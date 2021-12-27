
/*
 WiFiEsp example: WebClient

 This sketch connects to google website using an ESP8266 module to
 perform a simple web search.

 For more details see: http://yaab-arduino.blogspot.com/p/wifiesp-example-client.html
*/

#include <OneWire.h>
#include <LiquidCrystal_I2C.h>
#include <Wire.h> 
#include <time.h>
#include <TimeLib.h> 
#include <EEPROM.h>
#include <SPI.h>
#include <TimerOne.h>
#include "SdFat.h"
#include "sdios.h"
#include "WiFiEsp.h"
#include "WiFiEspUdp.h"
#include <avr/pgmspace.h>
#include "DHT.h"

int   rCount=-1, cCount=0, ampNo = 6, lCount=0 ;
int   boardPin = 41 , wifiPin = 39 , sdPin = 37 , espPin = 35 , rsPin = 33 , holdPin = 31 , tempPin = 3 ;  
int   sizeV, sizeW, sizeI, sizeP;    

float     currReading[4][5] = {0};            // [chno]{onoff,Svolt,Samp,Mvolt,Mamp
float     ReadingSum[4][11];            // [chno]{onoff,Svolt,Samp,Vcnt,Vsum,Vmax,Vmin,Icnt,Isum,Imax,Imin}
float     setChannel[4][7] = {0};          // [chno]{OUTP,VOLT,CURR,tOn,tOff,onoff}

bool    PS_Ready, RS_FB, SV_FB, SD_FB, Wifi_Ready, ST_Ready, Card_Ready, cntrHold, HW_Hold ;

//String  readSet[] = {"INST OUT1","INST OUT2","INST OUT3","INST OUT4","*IDN?","OUTP?","VOLT?","CURR?","MEAS:VOLT?","MEAS:CURR?"};
  const char readStr_0[] PROGMEM = "INST OUT1\n";
  const char readStr_1[] PROGMEM = "INST OUT2\n";
  const char readStr_2[] PROGMEM = "INST OUT3\n";
  const char readStr_3[] PROGMEM = "INST OUT4\n";
  const char readStr_4[] PROGMEM = "*IDN?\n";
  const char readStr_5[] PROGMEM = "OUTP?\n";
  const char readStr_6[] PROGMEM = "VOLT?\n";
  const char readStr_7[] PROGMEM = "CURR?\n";
  const char readStr_8[] PROGMEM = "MEAS:VOLT?\n";
  const char readStr_9[] PROGMEM = "MEAS:CURR?\n";
const char *const readSetP[] PROGMEM = {readStr_0, readStr_1, readStr_2, readStr_3, readStr_4, readStr_5, readStr_6, readStr_7, readStr_8, readStr_9};

  const char WifiStatus_255[] PROGMEM = "WL_NO_SHIELD";
  const char WifiStatus_0[] PROGMEM = "WL_IDLE_STATUS";
  const char WifiStatus_1[] PROGMEM = "WL_CONNECTED";
  const char WifiStatus_2[] PROGMEM = "WL_CONNECT_FAILED";
  const char WifiStatus_3[] PROGMEM = "WL_DISCONNECTED";
  const char WifiStatus_other[] PROGMEM = "Unknown";
const char *const WifiStatusP[] PROGMEM = {WifiStatus_255, WifiStatus_0, WifiStatus_1, WifiStatus_2,WifiStatus_3,WifiStatus_other};

//String  configVName[]={"WiFi 선택","데이터 기록 간격(분)","데이터 전송 간격(분)","LCD OFF Delay(분)"};
  const char VNameStr_0[] PROGMEM = "SDCard 기록 간격(초)";
  const char VNameStr_1[] PROGMEM = "데이터 전송 간격(분)";
  const char VNameStr_2[] PROGMEM = "LCD OFF Delay(분)";
const char *const configVName[] PROGMEM = {VNameStr_0, VNameStr_1, VNameStr_2};

//String  configWName[]={"와이파이 이름","IP Address","WiFi Status","WiFi AP 신호강도","SD CARD space"};
  const char WNameStr_0[] PROGMEM = "와이파이 이름";
  const char WNameStr_1[] PROGMEM = "IP Address";
  const char WNameStr_2[] PROGMEM = "WiFi Status";
  const char WNameStr_3[] PROGMEM = "WiFi AP 신호강도";
  const char WNameStr_4[] PROGMEM = "SD CARD space";
  const char WNameStr_5[] PROGMEM = "Contoller Temp";
  const char WNameStr_6[] PROGMEM = "Serial port";
const char *const configWName[] PROGMEM = {WNameStr_0, WNameStr_1, WNameStr_2, WNameStr_3, WNameStr_4, WNameStr_5, WNameStr_6};


//String  configIName[]={"사무실 Wifi 이름","현장 Wifi 이름","Wifi패스워드","현장 IP Address","서버 IP Address","Gate IP Address","Subnet IP Address","Powersupply ID","Powersupply SN"};
  const char INameStr_0[] PROGMEM = "Powersupply ID";
  const char INameStr_1[] PROGMEM = "Powersupply SN";
  const char INameStr_2[] PROGMEM = "Wifi 이름";
  const char INameStr_3[] PROGMEM = "WiFi TYPE";
  const char INameStr_4[] PROGMEM = "Wifi패스워드";
  const char INameStr_5[] PROGMEM = "서버 IP Address";
const char *const configIName[] PROGMEM = {INameStr_0, INameStr_1, INameStr_2, INameStr_3, INameStr_4, INameStr_5 };

  const char INameStr_6[] PROGMEM = "Gate IP Address";
  const char INameStr_7[] PROGMEM = "Subnet IP Address";
  const char INameStr_8[] PROGMEM = "고정 IP Address";  
const char *const configPName[] PROGMEM = { INameStr_6, INameStr_7, INameStr_8 };

    const char logPath[] PROGMEM = "/process/ps_event_log.php";
    const char writePath[] PROGMEM = "/process/post_ps_data.php";
    const char formTextSum[] PROGMEM = "table=power" ;
    const char formTextEvent[] PROGMEM = "table=pSupply" ;
    const char amps[] PROGMEM = "&";
    const char comma[] PROGMEM = "_";
    const char ddaom[] PROGMEM = "'";
    const char psid[] PROGMEM =  "PS_ID=";
    const char pssn[] PROGMEM =  "PS_SN=";
    const char chTitle[] PROGMEM =  "TEST_ID=";
    const char chDesc[] PROGMEM =  "TEST_DS=";
    const char colon[] PROGMEM = ":";
const char *const psParam[] PROGMEM = {logPath, writePath, formTextEvent, formTextSum, amps, comma, ddaom, psid, pssn, chTitle, chDesc};

    const char setMsg00[] PROGMEM = "[ERROR]형식이 맞지않는 정보입니다";
    const char setMsg01[] PROGMEM = "[RS_ERROR]채널no가 범위를 벗어났습니다";
    const char setMsg02[] PROGMEM = "[RS_ERROR]설정값이 실수 범위를 벗어났습니다";
    const char setMsg03[] PROGMEM = "[RS_ERROR]설정할 값이 없습니다";
    const char setMsg04[] PROGMEM = "[RS_ERROR]On/Off 설정 범위를 벗어났습니다";
    const char setMsg05[] PROGMEM = "RS_ERROR]Voltage가 범위를 벗어났습니다";
    const char setMsg06[] PROGMEM = "[RS_ERROR]Current가 범위를 벗어났습니다";
const char *const setMessage[] PROGMEM = { setMsg00, setMsg01, setMsg02, setMsg03, setMsg04, setMsg05, setMsg06 };
    
  char ComBuffer[32];  

String  configName[4][2]={"", ""};   
String  configDData[]={"0", "1"};   
String  configVData[]={"10",  "5",  "5"};   
String  configWData[]={".?.",".?.",".?.",".?.",".?.",".?.",".?."};
String  configIData[]={"CA-TE-0xx","ODA-04-0923-05xxx", ".?.", "0", "ctlinc5300","220.120.155.254"};
String  configPData[]={"220.120.155.1","255.255.255.0","220.120.155.186"};

String  strMon, strDate, strMD;
String  lcdMessage[3] = "";
String  RsQueue[4] = "", logQueue[4]="";
String  evtQueue="", CTRLno="11";
String  rcvRS="", rcvUSB="", rcvBT="", rcvSV="";

unsigned long   prevM , LcdOff ;
unsigned long   nextLog[4][2] = {0};       // [chno][write,log] timer
unsigned long   readInterval = 400 ;                    // inreval 단위 60초  //progmen
unsigned long   warnQueue[4][4] = {0};       // [chno][flag,time,set,read ] 

unsigned long   sdFree , sdSize;
int status = WL_IDLE_STATUS;     // the Wifi radio's status

byte packetBuffer[48]; // buffer to hold incoming and outgoing packets

LiquidCrystal_I2C lcd(0x27,16,2);
OneWire ds(tempPin);
DHT dht22(tempPin, DHT22);
// A UDP instance to let us send and receive packets over UDP
WiFiEspUDP Udp;
// Initialize the Ethernet client object
WiFiEspServer server(80);
WiFiEspClient phpclient    ;

// use a ring buffer to increase speed and reduce memory allocation
IPAddress PHPserver(220,120,155,254) ;
IPAddress officeIP(192, 168, 1, 18) ; // 사용할 IP 주소
IPAddress lineIP(220, 120, 155, 186) ; // 사용할 IP 주소
IPAddress gateway(220, 120, 155, 1); // 게이트웨이 주소
IPAddress subnet(255, 255, 255, 0); // 서브넷 주소
IPAddress accessIP ;

// SDCARD_SS_PIN is defined for the built-in SD on some boards.
#ifndef SDCARD_SS_PIN
const uint8_t SD_CS_PIN = 53;
#else  // SDCARD_SS_PIN
// Assume built-in SD is used.
const uint8_t SD_CS_PIN = SDCARD_SS_PIN;
#endif  // SDCARD_SS_PIN

// Try to select the best SD card configuration.
#if HAS_SDIO_CLASS
#define SD_CONFIG SdioConfig(FIFO_SDIO)
#elif ENABLE_DEDICATED_SPI
#define SD_CONFIG SdSpiConfig(SD_CS_PIN, DEDICATED_SPI)
#else  // HAS_SDIO_CLASS
#define SD_CONFIG SdSpiConfig(SD_CS_PIN, SHARED_SPI)
#endif  // HAS_SDIO_CLASS
//------------------------------------------------------------------------------

#if SD_FAT_TYPE == 0
SdFat sd;
File file, root;
#elif SD_FAT_TYPE == 1
SdFat32 sd;
File32 file, root;
#elif SD_FAT_TYPE == 2
SdExFat sd;
ExFile file, root;
#elif SD_FAT_TYPE == 3
SdFs sd;
FsFile file, root;
#endif  // SD_FAT_TYPE
    
void setup()
{
  // Initialize the digital pin as an output.
  // Pin 13 has an LED connected on most Arduino boards
   pinMode(LED_BUILTIN, OUTPUT);    
   pinMode(boardPin, OUTPUT);    
   pinMode(espPin, OUTPUT);
   pinMode(rsPin, OUTPUT);
   pinMode(sdPin, OUTPUT);
   pinMode(wifiPin, OUTPUT);
   pinMode(holdPin, OUTPUT);

   digitalWrite( boardPin, HIGH );
   digitalWrite( espPin, HIGH );
   digitalWrite( rsPin, HIGH );
   digitalWrite( sdPin, HIGH );
   digitalWrite( wifiPin, HIGH );
   digitalWrite( holdPin, HIGH );
   
   // initialize serial for debugging
   
  Serial.begin(115200);
  Serial3.begin(9600);        // power supply com port
  // initialize serial for ESP module
  Serial2.begin(9600);        // Bluetooth module
  Serial1.begin(115200);      // ESP-01 wifi module
  dht22.begin();
  
//  cntrHold =true;
  
  sizeV = sizeof(configVData)/sizeof(configVData[0]);
  sizeW = sizeof(configWData)/sizeof(configWData[0]);
  sizeI = sizeof(configIData)/sizeof(configIData[0]);
  sizeP = sizeof(configPData)/sizeof(configPData[0]);
  
  LCDInit(); // initialize the lcd 
  mySDBegin();
  printFreeSpace();
  
  EEPread();
  setIP();     
  CTRLno=configIData[0].substring(configIData[0].length()-2);

  eventQueue("system initializing", "initial", "01");
  ReadingTemperature() ;
  WiFi.init(&Serial1);
  // check for the presence of the shield
  if (WiFi.status() == WL_NO_SHIELD) {
    displayResponse("WiFi shield not present");
    // don't continue
  } else {
    connectWifi() ;
  }
  strcpy_P(ComBuffer, (char *)pgm_read_word(&(readSetP[4])));
  Serial3.print(ComBuffer);
  delay(1000);
  long currM = millis();
  for(int chno=0; chno<4; chno++) {
//    setChannel[chno][0]=1;
//    setChannel[chno][1]=150;
//    setChannel[chno][2]=1;
//    setChannel[chno][3]=5;
//    setChannel[chno][4]=5;
    setChannel[chno][5]=-1;
    setLogger(chno);
    initReading(chno);
  }
  displayResponse(lcdMessage[2]);
//  cntrHold =false;
  printTestMode();
  printSettingName();
  digitalWrite( rsPin, LOW );
  digitalWrite( holdPin, LOW );
  if(Card_Ready == true) digitalWrite( sdPin, LOW );
}

void setIP(){
  PHPserver=Str2IP(configIData[5]); //php서버
  gateway  =Str2IP(configPData[0]); // 게이트웨이 주소
  subnet   =Str2IP(configPData[1]); // 서브넷 주소
  lineIP   =Str2IP(configPData[2]);   // 고정ip
}

void setLogger(int chno){
  long currM = millis();
  if(setChannel[chno][5]==2){
    nextLog[chno][0] = currM + (setChannel[chno][3+String(setChannel[chno][0]).toInt()]) * 60000 ;
  }else{
    nextLog[chno][0] = currM + configVData[1].toInt() * 60000 ;
  }
    nextLog[chno][1] = currM + configVData[0].toInt() * 60000 ;
}

String IP2String(IPAddress ipnum) {
  char chr[100];
  sprintf(chr, "%d.%d.%d.%d", ipnum[0], ipnum[1], ipnum[2], ipnum[3]);
  return String(chr);
}

IPAddress Str2IP(String sData) {   
    IPAddress ipnum;
    String sCopy = sData+"..";
    for (int count=0; count<4; count++) {
        int nGetIndex = sCopy.indexOf(".");
        if(-1 != nGetIndex) {
          ipnum[count]=sCopy.substring(0,nGetIndex).toInt();
          sCopy = sCopy.substring(nGetIndex + 1);
        }else {
            break;
        }
    }
    return ipnum;
}

void loop() {

  cntrHold = configDData[1] == "0";
  HW_Hold = digitalRead( holdPin ) == HIGH ;
   if ( rCount+cCount == 0 && year() < 2020 )   ST_Ready=false ;
   if ( Serial.available() )   usb_reading();
   if ( Serial2.available() )  BT_reading();
   if ( Serial3.available() )  Serial3_reading();
   if ( phpclient.available() )  client_reading();
   
   if ( millis() < prevM )  prevM = millis() ;
   if ( millis() - prevM > readInterval) timerLoop();
}

  // ================================================================================================================== //
  // ===========================================    Communication      ============================================== //
  // ================================================================================================================== //

/// --------------------------
/// Message Display
/// --------------------------
void displayResponse(String AlertStr){
    Serial.println(AlertStr);
    Serial.flush();
    Serial2.println(AlertStr);
    Serial2.flush();
}

/// --------------------------
/// Message Display (memory)
/// --------------------------
void displayMessage(int msg_index, String AlertStr){
    strcpy_P(ComBuffer, (char *)pgm_read_word(&(setMessage[msg_index])));
    Serial.println(String(ComBuffer) + AlertStr);
    Serial.flush();
    Serial2.println(String(ComBuffer) + AlertStr);
    Serial2.flush();
}

/// --------------------------
/// USB Serial 통신
/// --------------------------
void usb_reading() {
    while(Serial.available()) {
      int byteSend;
      byteSend = Serial.read();
      if (byteSend == 10 || byteSend == 13)   {
        if (rcvUSB.length() > 3) {
          displayResponse("[USB_comm]" + rcvUSB);
           commandSetting(2,rcvUSB);
        }
         rcvUSB = "";
      } else {
        rcvUSB += char(byteSend);
      }      
    }
}

/// --------------------------
/// Bluetooth Serial 통신
/// --------------------------
void BT_reading() {
    while (Serial2.available())    {
      int byteSend;
      byteSend = Serial2.read();
      if (byteSend == 10 || byteSend == 13)   {
        if(rcvBT.length()>1) {
          if(rcvBT.startsWith("[BTcomm]")){
            commandSetting(3,rcvBT.substring(8));
          } else {
            Serial.println("[fromBT]" + rcvBT);
            Serial.flush();
          }
        }
         rcvBT = "";
      } else {
        rcvBT += char(byteSend);
      }      
  }
}

/// --------------------------
/// Com.port Serial 통신
/// --------------------------
void Serial3_reading() {
   while(Serial3.available()) {
      int byteSend;
       RS_FB = true;
       PS_Ready = true ;
      byteSend = Serial3.read();
      if (byteSend == 10 || byteSend == 13)   {
            if( rCount > 0 && rCount < 6)  {             // 데이터 항목은 계산으로
                addReading(rCount, cCount, rcvRS);
            } else {
              displayResponse("[fromPS]" + rcvRS);
              configWData[6] = rcvRS;
              configWData[6].replace(","," ");
            }
            if ( rCount == 5)  {                           // 마지막 데이터 받고 채널단위로 보여주고 보내고
              checkTimeAndLog(cCount) ;
            }
          rcvRS = "";
      } else {
          rcvRS += char(byteSend);
      }
  }

  if( SV_FB == true ) digitalWrite( espPin, LOW );
  if( RS_FB == true ) digitalWrite( rsPin, LOW );
  if( SD_FB == true ) digitalWrite( sdPin, LOW );
  
}

/// --------------------------
/// Com.port Serial 통신
/// --------------------------
void client_reading() {
  while (phpclient.available()) {
    char c = phpclient.read();
    Serial.write(c);
  }
  SV_FB = true;
}
  // ================================================================================================================== //
  // ===========================================    Timer 반복 작업      ============================================== //
  // ================================================================================================================== //

/// --------------------------
/// Com.port에 Signal 전송
/// --------------------------
void timerLoop() {
    digitalWrite( boardPin, digitalRead( boardPin ) ^ 1 );
    prevM = millis() ;
  if(Wifi_Ready == false || configDData[0] == "0" ) {
    digitalWrite( wifiPin, LOW );
  } else  {
    digitalWrite( wifiPin, HIGH );
  }
  if(Wifi_Ready == false && WiFi.status() != WL_NO_SHIELD) {
//      connectWifi();    
//      displayResponse("[timerIsr] Wifi connection retry : " );        //[timerIsr] Wifi connection retry : 
  }
  
  if(ST_Ready == false && Wifi_Ready == true) {
      gettimeFromNTP() ;
  }  

  displayToggle();
  if (HW_Hold == true || cntrHold == true ) {
    digitalWrite( sdPin, digitalRead( sdPin ) ^ 1 );
    digitalWrite( wifiPin, digitalRead( sdPin ) ^ 1 );
    return;
  }
  
      rCount++;                   // rCount 0=> channel변경, 1=>on/off check, 2:read set Volt, 3:read set mAmp, 4:read volt, 5:read mAmp, 6: write data
      if(rCount > ampNo ){
        RS_FB = false;
        rCount=0;
        cCount++;                 // cCount 0,1,2,3 => 각 channel 
        if(cCount > 3 ){
          rCount=0;
          cCount=0;  
        }
      }

//    printMessage(0, String(cCount) + String(rCount) + " .");
      if ( cCount < 0 ) return;
      if ( rCount < 0 || rCount > 5) return;
      if ( rCount == 0 ) {
        strcpy_P(ComBuffer, (char *)pgm_read_word(&(readSetP[cCount])));
        ReadingTemperature();
      } if ( rCount > 0 && rCount < 6) {
        strcpy_P(ComBuffer, (char *)pgm_read_word(&(readSetP[rCount + 4])));
      }
      Serial3.print(ComBuffer);
      Serial3.flush();
      digitalWrite( rsPin, HIGH );
    digitalWrite( LED_BUILTIN, digitalRead( LED_BUILTIN ) ^ 1 );
    if ( rCount > 3 && RS_FB==false ) {
//      Serial.print(String(rCount) + " " + String(PS_Ready) + " " + String(RS_FB) + ": ");
      if ( PS_Ready==true ) {
        PS_Ready=false;
        eventQueue("PowerSupply not responding", "warning", "03");
        eventPush();
      }
    }
}

/// --------------------------
/// 수신 데이터, Output변경 ,이벤트 , 서버전송 등 순차 처리
/// --------------------------
void checkTimeAndLog(int chno) {
      unsigned long currM = millis();
      String OnOffStr[]={"off","on"};
      
    displayResponse("DW: "+ LastReading(chno) );
    dataDisplay( );  
    
    if( configName[chno][0].length() == 0 ) newDescription(chno) ;
    
    // onoff동작에서 on-off 전환 
    if(setChannel[chno][5]==2 && currReading[chno][0] != setChannel[chno][0] ){            
        String msg="[CHECK]Channel "+String(chno+1);
        msg += "\tOutput: "+ OnOffStr[(int)currReading[chno][0]] + "=>" + OnOffStr[(int)setChannel[chno][0]] ;
        displayResponse(msg);
        changeOnOff(chno, setChannel[chno][0]);               // on/off전환
    }    

    // setChannel 값이 변경되면 
    if( setChannel[chno][6] >0 ) {
      String msg="[CHECK]Channel "+String(chno+1);
      if( (String)currReading[chno][1] != (String)setChannel[chno][1] ) msg += "\n\tVoltage: "+ String(currReading[chno][1]) + "!=" + String(setChannel[chno][1]);
      if( (String)currReading[chno][2] != (String)setChannel[chno][2] ) msg += "\n\tCurrent: "+ String(currReading[chno][2]) + "!=" + String(setChannel[chno][2]);
      displayResponse(msg);
      setChannel[chno][6] = 0;
      newSetPower(chno);                                    // power set
    }
      
    // Log 시간 경과하면 
    if(currM > nextLog[chno][0]) {
      if(setChannel[chno][5]==2){
        setChannel[chno][0] = (String(currReading[chno][0]+1).toInt())%2;
        nextLog[chno][0] += (setChannel[chno][3+String(setChannel[chno][0]).toInt()]) * 60000 ;
      } else if(setChannel[chno][5]>0){
        nextLog[chno][0] += configVData[0].toInt() * 60000;
        sendSummary(chno);            // sum저장
      }        
    }

    if(configVData[1].toInt()==0) {
      sendLog(chno);
    } else if( (configVData[1].toInt()>0) && (currM > nextLog[chno][1])) {
      nextLog[chno][1] += configVData[1].toInt() * 1000;
      sendLog(chno);                             // save reading
    }

    // output On 이고 read/set 5%이상 차이나면 
    if(setChannel[chno][0] > 0 ){            
//      Serial.println("DW+ " + String(chno+1) + " read/set = " + currReading[chno][4] + " / " + currReading[chno][2]);
      if( ( currReading[chno][4] / currReading[chno][2] ) < 0.95 || ( currReading[chno][4] / currReading[chno][2] ) > 1.05 ) {
        warnAdd( chno , currReading[chno][2], currReading[chno][4]);
      } else {
        warnQueue[chno][0] = 0 ;
      }
    }

    if(RsQueue[chno].length()>0){
      sendSummary(chno);            // sum저장
      QueueRead(chno);                                        // power supply setting
      return;
    }
    if(RsQueue[0].length()+RsQueue[1].length()+RsQueue[2].length()+RsQueue[3].length()>0) return;

    if(evtQueue.length()>0){
      eventPush();
      return;
    }
    if(logQueue[chno].length()>0){
      saveReading(chno);
      return;
    }
      warnPush();
    
}

  // ================================================================================================================== //
  // ===========================================    측정 데이터 수집     ============================================== //
  // ================================================================================================================== //

/// --------------------------
/// ReadingSum write
/// --------------------------
String writeReading( int chno) {
    int mCount = ReadingSum[chno][7];
    char psUbar[]="_";
    String  strVal = "&P"+String(chno+1)+"="+String(String(ReadingSum[chno][0]).toInt()) ;
      strVal += String(psUbar) + String(ReadingSum[chno][1]);
      strVal += String(psUbar) + String(ReadingSum[chno][2]);
      strVal += String(psUbar) + String(mCount);
      for(int j=0;j<2;j++){      
        strVal += String(psUbar) + String(ReadingSum[chno][j*4+4]/ReadingSum[chno][j*4+3]);
        strVal += String(psUbar) + String(ReadingSum[chno][j*4+5]);
        strVal += String(psUbar) + String(ReadingSum[chno][j*4+6]);            
      }
      return strVal ;
}
 
/// --------------------------
/// ReadingSum Add
/// --------------------------
void addReading( int rct ,int cct , String ReadBack){
    float ReadVal=ReadBack.toFloat();
    char bufStr[18], bufData[10], Cha[10];     

    if (rct==3 || rct==5) ReadVal *= 1000;
    if (rct<6) {
      currReading[cct][rct-1]=ReadVal ;
    
      //  1:on/off  2:Svolt? 3:Scurr? 4:Mvolt 5:Mcurr
      if (rct > 3 ) {
          ReadingSum[cct][(rct-4)*4+3]++;
          ReadingSum[cct][(rct-4)*4+4] += ReadVal;
          if(ReadingSum[cct][(rct-4)*4+5] < ReadVal) ReadingSum[cct][(rct-4)*4+5] = ReadVal;
          if(ReadingSum[cct][(rct-4)*4+6] > ReadVal) ReadingSum[cct][(rct-4)*4+6] = ReadVal;
      }  
    }
}

/// --------------------------
/// ReadingSum 초기화
/// --------------------------
void initReading( int chno){
      ReadingSum[chno][3]=0;
      ReadingSum[chno][4]=0;
      ReadingSum[chno][5]=0;
      ReadingSum[chno][6]=99999;
      ReadingSum[chno][7]=0;
      ReadingSum[chno][8]=0;
      ReadingSum[chno][9]=0;
      ReadingSum[chno][10]=99999;
}

  // ================================================================================================================== //
  // ===========================================    Power Supply Setting     ============================================== //
  // ================================================================================================================== //

void QueueRead(int chno){
    int startAt , endAt ;
    startAt=0;
    endAt = RsQueue[chno].indexOf('|',startAt);
    while (endAt>0) {  
      String readStr = RsQueue[chno].substring(startAt, endAt);
      rsWriting(String(chno+1),readStr);
      startAt = endAt+1;
      endAt = RsQueue[chno].indexOf('|',startAt);
    }
  RsQueue[chno]="";
}

void newSetPower(int chno) {
     char mAmps[20]; 
     String POoutput="0,";
     if(setChannel[chno][0]==1)  POoutput="1,";
     float volt=(setChannel[chno][1]);
     float amp=setChannel[chno][2];
     amp=amp/1000 ;
     dtostrf(amp, 6, 4, mAmps);
//     rsSetting("RS:SET=0" + String(chno+1) + POoutput + String(volt) + "," + String(mAmps));
//     rsWriting(String(chno+1) , POoutput + String(volt) + "," + String(mAmps) ); // 1,1,120.00,0.010
     RsQueue[chno] += POoutput + String(volt) + "," + String(mAmps) + "|";
      displayResponse("[SET] Channel = " + String(chno+1) + " newSetPower = " + RsQueue[chno] );
}

void changeOnOff(int chno, int chOut) {
      RsQueue[chno] += String(chOut) + "|";
      displayResponse("[SET] Channel = " + String(chno+1) + " OUTPUT = " + RsQueue[chno] );
}

void rsSetting(String msg) {
    boolean currRS = cntrHold ;
    if (msg.substring(0,7) != "RS:SET=" || !isDigit(msg.charAt(8)) ){ //  RS:SET=11,1,100,0.010   (,0,0)
      rsWriting("0",msg);
      return;                
    }
    if (msg == "RS:OFF") {
      configDData[1] == "0";
    }else if (msg == "RS:ON") {
      configDData[1] == "1";
    } else {
    Serial3.print(msg.substring(3) + "\n" );  
    }
}

void rsWriting(String chstr, String svalue) {
  String msg="";
  if (chstr.toInt()==0) {
      Serial3.print(svalue+"\n" );
      Serial3.flush();
    delay(readInterval);
    return;
  }
  String cTime = currTime();
  if (isDigit(svalue.charAt(0))) {
    if(svalue.length()>2) {                   //전압전류변경
      Serial3.print("APPL "+svalue.substring(2)+"\n" );
      Serial3.flush();
      msg = "P" + chstr + " : APPL " + svalue.substring(2) + " : "+ cTime + " " ;
      eventQueue(msg, "ChannelSet", chstr + "1");
    } else {                                  //outp변경
      Serial3.print("OUTP "+svalue.substring(0,1)+"\n" );
      Serial3.flush();
      msg = "P" + chstr + " : OUTP " + svalue.substring(0,1) + " : " + cTime + " " ;
      eventQueue(msg, "OutputSet", chstr + "2");
    }
    delay(readInterval);
  }  
}

void newDescription(int chno) {
      filedate();
      configName[chno][0]=strDate;
      configName[chno][1]= "(-)" + String(setChannel[chno][1]) + "V " + String(setChannel[chno][2]) + "mA";  
}
  // ================================================================================================================== //
  // ===========================================    데이터 기록 저장      ============================================== //
  // ================================================================================================================== //

void sendSummary(int chno) {
      if(ReadingSum[chno][7]>1) writeToDB(chno);
      initReading( chno );     // summary data rest
}

void sendLog(int chno) {
    String strTimes =filedate() ;     
    logQueue[chno] += strTimes + "," + LastReading(chno)+"|" ;
}

void eventQueue(String eventMsg, String eventType, String eventCh) {
    evtQueue += eventMsg + "|" + eventType + "|" + CTRLno + eventCh + "||" ;
}

void warnAdd(int chno , float setA, float readA){
    
    warnQueue[chno][2] = (long)setA;
    warnQueue[chno][3] = (long)readA;
    warnQueue[chno][0]++ ;
    if(warnQueue[chno][0]==1) warnQueue[chno][1] = millis();
//            Serial.println("warnAdd ch " + String(chno+1)) + " -> " + warnQueue[chno][0];

}

  // ================================================================================================================== //
  // ===========================================   외부(시리얼) 요청사항에 대한 대응     ============================== //
  // ================================================================================================================== //

void commandSetting(int owner, String setStr) {
//    displayResponse("[commandSetting]"  +setStr);
    if (setStr.length()<3) return;
        if (setStr.substring(0, 3) == "US:") {
            responseToUSB(setStr.substring(3));
        } else if (setStr.substring(0, 3) == "BT:") {
            responseToUSB(setStr.substring(3));
        } else if (setStr.substring(0, 3) == "RS:") {
            rsSetting(setStr);
        } else if (setStr.substring(0, 3) == "EE:") {
            eeSetting(setStr);
        } else if (setStr.substring(0, 3) == "SV:") {
            SetValueChange(setStr.substring(3));
        } else {
          displayResponse("Unknown Command :" + setStr);
        }  
}

void responseToUSB(String msg) {

  if (msg.indexOf("SDFREE") > -1) {
    printFreeSpace();
  }
  if (msg.indexOf("LASTDATA") > -1) {
    printLastValue();
  }
  if (msg.indexOf("READ_CONFIG") > -1) {
    EEPread();
    printSettingName();
  }
  if (msg.indexOf("PS_CONFIG") > -1) {
    printSettingName();
  }
  if (msg.indexOf("CONFIG_V") > -1) {
    printConfigV();
  }
  if (msg.indexOf("CONFIG_W") > -1) {
    printConfigW();
  }
  if (msg.indexOf("CONFIG_I") > -1) {
    printConfigI();
    printConfigP();
  }
  if (msg.indexOf("SETVALUE") > -1) {
    printSetValue();
  }
  if (msg.indexOf("TESTMODE") > -1) {
    printTestMode();
  }
  if (msg.indexOf("SAVECONFIG") > -1) {
    EEPwrite();
  }
  if (msg.startsWith("ID:") ) {     //BT:ID:00:test1
    int grp=msg.substring(3,4).toInt();
    int inx=msg.substring(4,5).toInt();
    if (grp==0)    configDData[inx]=msg.substring(6);
    displayResponse( "[Test_SET]" + msg.substring(3,5) + "=>" + msg.substring(6) );
    printTestMode();
  }
  if (msg.startsWith("AT") ) {     //BT:AT, BT:AT+NAME=Power Supply 01
    btSetting(msg.substring(2));
  }
}

void SetValueChange(String getStr) {
    int startAt=0, valCnt=0;
    int endAt = getStr.indexOf(',',0);
    float readVal[6]={-1};
    
    if (endAt<0) {                                        //SV:01,1,130,7.5,5,3,2
      displayMessage( 0,"" ); //setMsg00  [ERROR]형식이 맞지않는 정보입니다
      return;
    }
    int chno = getStr.substring(startAt, endAt).toInt();
    if (chno<1 || chno>4) {
      displayMessage( 1,"");  //setMsg01  [RS_ERROR]채널no가 범위를 벗어났습니다
      return;
    }

    while (endAt < getStr.length() && valCnt<6) {
      startAt = endAt+1;
      endAt = getStr.indexOf(',',endAt+1);
      if (endAt<0) endAt = getStr.length();
      for(int inx=startAt; inx<endAt; inx++) {
         if(!isDigit(getStr.charAt(inx)) && String(getStr.charAt(inx))!=".") {
          displayMessage( 2,"" );  //setMsg02  [RS_ERROR]설정값이 실수 범위를 벗어났습니다
          return;
         }
      }
      readVal[valCnt] = getStr.substring(startAt, endAt).toFloat();
      valCnt++ ;
    }
    filedate();
    if(valCnt==0){
      displayMessage( 3,"" ); //setMsg03  [RS_ERROR]설정할 값이 없습니다
      return;
    }else if(valCnt==1){
      if(readVal[0]<0 || readVal[0]>1) {
        displayMessage( 4,"" );  //setMsg04  [RS_ERROR]On/Off 설정 범위를 벗어났습니다
        return;
      }
      setChannel[chno-1][0]=readVal[0];
      setChannel[chno-1][5]=readVal[0];
      setChannel[chno-1][6]=1;
      setLogger(chno-1);
      initReading(chno-1);
      configName[chno-1][0]=strDate;
      configName[chno-1][1]= "("+String(String(setChannel[chno-1][5]).toInt()) +")" + String(setChannel[chno-1][1]) + "V " + String(setChannel[chno-1][2]) + "mA";
      return;
    }else if(valCnt>4){
      if(readVal[0]<0 || readVal[0]>1) {
        displayMessage( 4,"" );  //setMsg04  [RS_ERROR]On/Off 설정 범위를 벗어났습니다
        return;
      }
      if(readVal[1]<0 || readVal[1]>150) {
        displayMessage( 5,"" ); //setMsg05  RS_ERROR]Voltage가 범위를 벗어났습니다
        return;
      }
      if(readVal[2]<0 || readVal[2]>1000) {
        displayMessage( 6,"" ); //setMsg06  [RS_ERROR]Current가 범위를 벗어났습니다
        return;
      }
      setChannel[chno-1][0]=readVal[0];
      setChannel[chno-1][1]=readVal[1];
      setChannel[chno-1][2]=readVal[2];
      setChannel[chno-1][3]=readVal[3];
      setChannel[chno-1][4]=readVal[4];
      setChannel[chno-1][5]=readVal[5];
      setChannel[chno-1][6]=1;
      setLogger(chno-1);
      initReading(chno-1);
      configName[chno-1][0]=strDate;
      configName[chno-1][1]= "("+String(String(setChannel[chno-1][5]).toInt()) +")" + String(setChannel[chno-1][1]) + "V " + String(setChannel[chno-1][2]) + "mA";
    }
}

void eeSetting(String msg) {

      displayResponse("[EE_SEND]("+msg+")") ;   // EE:00:ctrlID 01:serverIP 02:AP 03:pass 04:myIP
  if (isDigit(msg.charAt(3)) && isDigit(msg.charAt(4)) ) {
    int grp = msg.substring(3,4).toInt();
    int pos = msg.substring(4,5).toInt();
    if(grp<3) {
      if(grp==0 && pos<sizeV) { //sizeof(ConfigV)
        configVData[pos]=msg.substring(6);
      }
      if(grp==1 && pos<sizeW) {
        configWData[pos]=msg.substring(6);
      }
      if(grp==2 && pos<sizeI) {
        configIData[pos]=msg.substring(6);
      }
      if(grp==3 && pos<sizeP) {
        configPData[pos]=msg.substring(6);
      }
    }
  }
}

void btSetting(String msg) {
      Serial.println("[BT_SEND]("+msg+")") ;
         Serial.flush();
    Serial2.println(msg);
}


  // ================================================================================================================== //
  // ===========================================    LCD에 정보 Display  =============================================== //
  // ================================================================================================================== //
void LCDInit() {
  lcd.init(); // initialize the lcd 

 // Print a message to the LCD. 
  lcd.backlight(); 
  lcd.cursor();
  lcd.clear();
  lcd.setCursor(2,0);
  lcd.print("Initilizing!"); 
 
}

void lcdDisplay() {
  lcd.clear();
  lcd.setCursor(0,0);
  lcd.print(lcdMessage[0]);   
  lcd.setCursor(0,1);
  lcd.print(lcdMessage[1]);   
}

void dataDisplay() {
    char bufStr[20], Cha[8], Chb[8], Ch0[8];     
    float ReadingVal;
    String zero=" -off- ";
    zero.toCharArray(Ch0, zero.length()+1);
    
    ReadingVal =  currReading[cCount][3] ;
    
    if(ReadingVal>0){
      dtostrf( ReadingVal    ,  7, 2, Cha );
    }else{
      zero.toCharArray(Cha, zero.length()+1);
    }
    ReadingVal =  currReading[cCount][4] ;
    if(ReadingVal>0){
      dtostrf( ReadingVal    ,  7, 2, Chb );
    }else{
      zero.toCharArray(Chb, zero.length());
    }
    sprintf(bufStr, "P%d%s%s", cCount+1, Cha, Chb);
    lcdMessage[0] = lcdMessage[1] ;
    lcdMessage[1] = bufStr ;

    lcdDisplay();
}

void cfgDisplay() {

    lcdMessage[0] = lcdMessage[1] ;
    lcdMessage[1] = configWData[lCount/6] ;

    lcdDisplay();
}

void displayToggle() {
   if ( millis() < LcdOff ) LcdOff = millis(); 
   if( HW_Hold == true ) {
      LcdOff = millis();
      lcd.backlight();
      lCount ++;
      if(lCount>sizeW*6+5) lCount=0;
      if(lCount % 6 == 0 ) cfgDisplay();
   } else if ( millis() - LcdOff > configVData[2].toInt()*60000 ) {
      lcd.noBacklight();
      return;
   }
   if(PS_Ready == false) {
    digitalWrite( sdPin, digitalRead( sdPin ) ^ 1 );
    digitalWrite( wifiPin, digitalRead( sdPin ) ^ 1 );    
   }
}

  // ================================================================================================================== //
  // ===========================================    Wifi로 Access Point에 접속      =================================== //
  // ================================================================================================================== //

void connectWifi() {
  int len;
  len=configIData[2].length()+1;
  char ssid[len];
  configIData[2].toCharArray(ssid, len);
  
  len=configIData[4].length()+1;
  char pass[len];  
  configIData[4].toCharArray(pass, len);
//char ssid[] = "office2ctl";            // your network SSID - configIData[2]
//char pass[] = "ctlinc5300";        // your network password - configIData[3]
  
  unsigned long timeout = millis();
// attempt to connect to WiFi network

  digitalWrite( espPin, HIGH );
  Wifi_Ready = false;
  
  while ( status != WL_CONNECTED) {
    displayResponse("Attempting to connect to WPA SSID: "+String(ssid));
    // Connect to WPA/WPA2 network  
    WiFi.disconnect();
    if(configIData[3]!="0")  WiFi.config(lineIP);

    status = WiFi.begin(ssid, pass);

    if (millis() - timeout > 10000) {
      displayResponse(">>> WPA SSID connection time out !");
      return;
    }

  }

  if ( status == WL_CONNECTED ) {
  // you're connected now, so print out the data
    displayResponse("You're connected to the network");

    printWifiStatus();
    
    Wifi_Ready = true;
    eventQueue("wifi connected : " + configWData[0] , "initial" , "02");
    eventPush();
    // start the web phpserver on port 80
    server.begin();
    Serial.println();  
    digitalWrite( espPin, LOW );    
  }
  
}

void printWifiStatus() {
  // print the SSID of the network you're attached to
  displayResponse("SSID: " + String(WiFi.SSID()));

  // print your WiFi shield's IP address
  IPAddress ip = WiFi.localIP();
    configWData[0] = WiFi.SSID();
    configWData[1] = IP2String(ip);
//  displayResponse("IP Address: " + configWData[1]);
    configWData[2] = WlStatusToStr(WiFi.status());
  
  // print the received signal strength
  long rssi = WiFi.RSSI();
  displayResponse("Signal strength (RSSI):" + String(rssi) + " dBm");
  configWData[3] = String(rssi) + " dBm";
}

String WlStatusToStr(wl_status_t wlStatus){
  int wsinx;
  switch (wlStatus)  {
  case WL_NO_SHIELD: wsinx=0; break;
  case WL_IDLE_STATUS: wsinx=1; break;
//  case WL_NO_SSID_AVAIL: return "WL_NO_SSID_AVAIL";
//  case WL_SCAN_COMPLETED: return "WL_SCAN_COMPLETED";
  case WL_CONNECTED: wsinx=2; break;
  case WL_CONNECT_FAILED: wsinx=3; break;
//  case WL_CONNECTION_LOST: return "WL_CONNECTION_LOST";
  case WL_DISCONNECTED: wsinx=4; break;
//  case WL_AP_LISTENING: return "WL_AP_LISTENING";
//  case WL_AP_CONNECTED: return "WL_AP_CONNECTED";
//  case WL_AP_FAILED: return "WL_AP_FAILED";
//  case WL_PROVISIONING: return "WL_PROVISIONING";
//  case WL_PROVISIONING_FAILED: return "WL_PROVISIONING_FAILED";
  default: wsinx=5; break;
  }
  strcpy_P(ComBuffer, (char*)pgm_read_word(&(WifiStatusP[wsinx])));
  displayResponse("WifiStatus:" + String(wlStatus)+":"+String(ComBuffer));
  return String(wlStatus)+";"+String(ComBuffer);
}

  // ================================================================================================================== //
  // ===========================================    서버에 데이터 전송      =========================================== //
  // ================================================================================================================== //

void writeToDB(int chno) {
    String strVal, postValue;
      char psAmp[]="&";

    strVal = "table=power" + String(psAmp) + "TEST_ID=" + configName[chno][0];
    strVal += String(psAmp) +  "TEST_DS=" + configName[chno][1];
    strVal += String(psAmp) + "PS_ID=" + configIData[0] ;
    strVal += String(psAmp) + "PS_SN=" + configIData[1] ;
    postValue = writeReading(chno);
//    displayResponse("[write]" + strVal );
    if(configDData[0]=="1") {
      displayResponse("[writeToDB]" + strVal );
      displayResponse("[summary]" + postValue );
      posting(strVal + postValue , 1 ) ;
    }else{
      displayResponse("[summary]" + strVal );
      displayResponse("[summary]" + postValue );
    }

}

void posting(String postData, int pathNo ){
  int len;
  String phprtn;
  len=configIData[5].length()+1;
  if ( status != WL_CONNECTED ) {
    Wifi_Ready = false;
    return;
  }
  char phpserver[len];

    configIData[5].toCharArray(phpserver, len);

      phpclient.setTimeout(500);
    phpclient.stop();
    if (phpclient.connect(phpserver, 80)) {
      digitalWrite( espPin, HIGH );
      strcpy_P(ComBuffer, (char *)pgm_read_word(&(psParam[pathNo])));
      String cmd = "POST " + String(ComBuffer) + " HTTP/1.1";
      
      // Make a HTTP request
      phpclient.println(cmd);
      phpclient.println("Host: "+String(phpserver));
      phpclient.println("Content-Type: application/x-www-form-urlencoded;");
      phpclient.print("Content-Length: ");
      phpclient.println(postData.length());
      phpclient.println();
      phpclient.println(postData);
    }else {
      displayResponse("[WifiESP]lost server connection : " + String(phpserver));
      return;
    } 
    SV_FB = false;
}

void eventPush() {
      char psAmp[]="&";

    String strFormData =  "table=pSupply" + String(psAmp) + "psid=" + configIData[0] + String(psAmp) + "psno=" + configIData[1];
    strFormData += String(psAmp) + "message=" + evtQueue ;
    displayResponse( strFormData );
    posting(strFormData, 0) ;
    evtQueue="";
}

void warnPush() {
      char psAmp[]="&";
      String OverUnder[] = {"Low ","High "};
      String cTime = currTime();

    for(int chno=0; chno<4; chno++) {
      if(warnQueue[chno][0]>2){
        if(warnQueue[chno][0] < 100){
          String strFormData =  "table=pSupply" + String(psAmp) + "psid=" + configIData[0] + String(psAmp) + "psno=" + configIData[1];
          strFormData += String(psAmp) + "message=P" + String(chno+1) + " : Current " + OverUnder[warnQueue[chno][2] < warnQueue[chno][3]]  + String(warnQueue[chno][2]) + " -> " + warnQueue[chno][3] + " : " + cTime + " |warning|" + CTRLno + String(chno+1) + "3||" ;
          displayResponse( strFormData );
          posting(strFormData, 0) ;
          warnQueue[chno][0] = 100;
//                Serial.println("warnPush :" + String(warnQueue[chno][0]));
        } else if(millis()-warnQueue[chno][1] > 120 * 60000){
            warnQueue[chno][0] = 0;
        }
      }
    }
}


  // ================================================================================================================== //
  // ===========================================     EEPROM 작업      ================================================= //
  // ================================================================================================================== //

void EEPread() {
    int startAt,endAt,dataAt,len, grp, pos ;
    String readStr, eepData = "";
    char temp;
    for (int i = 0; i < 512; ++i)    {
        temp = char(EEPROM.read(i));
        if(temp == '!')
            break;
        eepData += temp;
    }
//    Serial.println(eepData);
    startAt=0;
    endAt = eepData.indexOf('|',startAt);
    while (endAt>0) {  
      if (endAt<0) return;
      readStr = eepData.substring(startAt, endAt);
      dataAt = readStr.indexOf('=',0);
//      Serial.println(readStr);
      if (dataAt==2){
        grp=readStr.substring(0,1).toInt();
        pos=readStr.substring(1,2).toInt();
        if(grp==0){
          if(pos<sizeV){
            configVData[pos]=readStr.substring(3);
            displayResponse("[CONFIG]:0" + String(pos) + ":" + configVData[pos]);
          }
        }
        if(grp==2){
          if(pos<sizeI){
            configIData[pos]=readStr.substring(3);
            displayResponse("[CONFIG]:2" + String(pos) + ":" + configIData[pos]);
          }
        }
        if(grp==3){
          if(pos<sizeP){
            configPData[pos]=readStr.substring(3);
            displayResponse("[CONFIG]:3" + String(pos) + ":" + configPData[pos]);
          }
        }
      }
      startAt=endAt+1;
      endAt = eepData.indexOf('|',startAt);
    }
}
 
void EEPwrite() {
    String writeData = "";
    for (int j = 0; j < sizeV; j++) {
      writeData += "0" + String(j) + "=" + configVData[j] + "|" ;
    }
    for (int j = 0; j < sizeI; j++) {
      writeData += "2" + String(j) + "=" + configIData[j] + "|" ;
    }
    for (int j = 0; j < sizeP; j++) {
      writeData += "3" + String(j) + "=" + configPData[j] + "|" ;
    }
      writeData += "!";

    for (int i = 0; i < writeData.length(); ++i)    {
        EEPROM.write(i, writeData[i]);
    }
    Serial.println(writeData);
    displayResponse("[SAVED] configuration data saved");
    EEPread();
    printSettingName();
}

  // ================================================================================================================== //
  // ===========================================     앱에 전송될 데이터      ================================================= //
  // ================================================================================================================== //

void printSettingName(){
    displayResponse("Current Configured Name");
    printSetValue();
    printConfigV();
    printConfigI();
    printConfigP();
    printConfigW();
    displayResponse(configMode()) ;      
}

void printLastValue(){
    displayResponse("Last Reading");
    for (int k=0;k<4;k++) {
      String msg = "[LASTDATA]:0" + LastReading(k);
      displayResponse(msg) ;
    }
}

void printTestMode(){
    displayResponse("Test ID Reading");
    displayResponse(configMode()) ;      
    displayResponse(configSize()) ;
}
String configMode(){
    String msg = "[ID]:";
    if(configDData[0]!="1") configDData[0]="0";
    if(configDData[1]!="0") configDData[1]="1";
    for (int k=0;k<2;k++) {
       msg += configDData[k] + ":";
    }
    for (int k=0;k<4;k++) {
       msg += configName[k][0] + ":";
       msg += configName[k][1] + ":";
    }    
    return msg ;  
}
String configSize(){
   String   msg="ConfigDataSize:";
      msg += String(sizeV) + ":";
      msg += String(sizeW) + ":";
      msg += String(sizeI) + ":";      
      msg += String(sizeP) + ":";      
   return msg ;
}
void printConfigV() {
    String msg = "Test condition setting value - "+String(sizeV);
    displayResponse(msg);

    for (int pos = 0; pos < sizeV; pos++) {
      strcpy_P(ComBuffer, (char *)pgm_read_word(&(configVName[pos])));
      msg = "[CONFIG]:0";
      msg += String(pos) + ":";
      msg += String(ComBuffer) + ":";
      msg += configVData[pos] + ":";
    displayResponse(msg);
    }
}
void printConfigW() {
    String msg = "Wifi condition reading - "+String(sizeW);
    displayResponse(msg);
    if ( status == WL_CONNECTED )  printWifiStatus();
    if ( Card_Ready == true )    volDmp();
    ReadingTemperature();
    for (int pos = 0; pos < sizeW; pos++) {
      strcpy_P(ComBuffer, (char *)pgm_read_word(&(configWName[pos])));
      msg = "[CONFIG]:1";
      msg += String(pos) + ":";
      msg += String(ComBuffer) + ":";
      msg += configWData[pos] + ":";
    displayResponse(msg);
    }
}
void  printConfigI() {
    String msg = "Internet setting - "+String(sizeI);
    displayResponse(msg);

    for (int pos = 0; pos < sizeI; pos++) {
      strcpy_P(ComBuffer, (char *)pgm_read_word(&(configIName[pos])));
      msg = "[CONFIG]:2";
      msg += String(pos) + ":";
      msg += String(ComBuffer) + ":";
      msg += configIData[pos] + ":";
    displayResponse(msg);
    }
}
void  printConfigP() {
    String msg = "고정 IP setting - "+String(sizeP);
    displayResponse(msg);

    for (int pos = 0; pos < sizeP; pos++) {
      strcpy_P(ComBuffer, (char *)pgm_read_word(&(configPName[pos])));
      msg = "[CONFIG]:3";
      msg += String(pos) + ":";
      msg += String(ComBuffer) + ":";
      msg += configPData[pos] + ":";
    displayResponse(msg);
    }
}

String LastReading(int chno) {
    float value=0;
    long remainS = nextLog[chno][0] - millis();
    String Lread = String(chno+1) + "," ;
    for (int j=0;j<5;j++) {
      value = currReading[chno][j];
      Lread += String(value) + "," ;
    }
    Lread += String(ReadingSum[chno][7]) + "," ;
    Lread += String(remainS/1000) + "," ;
    Lread.replace(".00,",",");
    return Lread;  
}
void printSetValue(){
    String msg = "Set Value Reading";
    displayResponse(msg);
      float value=0;
    for (int k=0;k<4;k++) {
      msg = "[PSSET]:0" + String(k+1) + "," ;
      if(setChannel[k][5]<0) {
        msg += "1,";
        msg += String(currReading[k][1]) + "," ;
        msg += String(currReading[k][2]) + "," ;
        msg += "5,5,-1" ;
      } else {
        for (int j=0;j<6;j++) {
          value = setChannel[k][j];
          msg += String(value) + "," ;
        }
      }
      msg.replace(".00","");
      displayResponse(msg);
    }
}
// ================================================================================================================== //
  // ===================================     타임 서버에 NTP 시간 요청      =========================================== //
  // ================================================================================================================== //

void gettimeFromNTP() {
  int ntpTry = 25;
  char timeServer[] = "kr.pool.ntp.org";  // NTP server  
    Udp.begin(2390);
    
  while (ntpTry > 0){
    sendNTPpacket(timeServer); // send an NTP packet to a time server
    
    // wait for a reply for UDP_TIMEOUT miliseconds
    unsigned long startMs = millis();
    while (!Udp.available() && (millis() - startMs) < 2000) {}
  
    Serial.println(Udp.parsePacket());
    if (Udp.parsePacket()) {
//      Serial.println("packet received");
      // We've received a packet, read the data from it into the buffer
      Udp.read(packetBuffer, 48);
  
      // the timestamp starts at byte 40 of the received packet and is four bytes,
      // or two words, long. First, esxtract the two words:
  
      unsigned long highWord = word(packetBuffer[40], packetBuffer[41]);
      unsigned long lowWord = word(packetBuffer[42], packetBuffer[43]);
      // combine the four bytes (two words) into a long integer
      // this is NTP time (seconds since Jan 1 1900):
      unsigned long secsSince1900 = highWord << 16 | lowWord;
//      Serial.print("Seconds since Jan 1 1900 = ");
//      Serial.println(secsSince1900);
    // now convert NTP time into everyday time:
//     Serial.print("Unix time = ");
     // Unix time starts on Jan 1 1970. In seconds, that's 2208988800:
     const unsigned long seventyYears = 2208988800UL;
     // subtract seventy years:
     unsigned long epoch = secsSince1900 - seventyYears;
     // print Unix time:
//     Serial.println(epoch);

      epoch = epoch + 9 * 3600; // 한국시    +9
      setTime((time_t)epoch);
  String timeStr = "DT:" + String((time_t)epoch);
//      Serial3.print( timeStr  + "\n");
//      Serial3.flush();
//      Serial2.print( timeStr  + "\n");
//      Serial2.flush();

     // print the hour, minute and second:
//      Serial.println();             // UTC is the time at Greenwich Meridian (GMT)
      displayResponse("Controller Time : " + filedate()); 
    ST_Ready = true;

  // set date time callback function
  SdFile::dateTimeCallback(dateTime);

      break;
    }
    // wait ten seconds before asking for the time again
    delay(10000);
    ntpTry--;
//    Serial.println(ntpTry);
  }
}

// send an NTP request to the time server at the given address
void sendNTPpacket(char *ntpSrv)
{
  // set all bytes in the buffer to 0
  memset(packetBuffer, 0, 48);
  // Initialize values needed to form NTP request
  // (see URL above for details on the packets)

  packetBuffer[0] = 0b11100011;   // LI, Version, Mode
  packetBuffer[1] = 0;     // Stratum, or type of clock
  packetBuffer[2] = 6;     // Polling Interval
  packetBuffer[3] = 0xEC;  // Peer Clock Precision
  // 8 bytes of zero for Root Delay & Root Dispersion
  packetBuffer[12]  = 49;
  packetBuffer[13]  = 0x4E;
  packetBuffer[14]  = 49;
  packetBuffer[15]  = 52;

  // all NTP fields have been given values, now
  // you can send a packet requesting a timestamp:
  Udp.beginPacket(ntpSrv, 123); //NTP requests are to port 123

  Udp.write(packetBuffer, 48);

  Udp.endPacket();
}

  // ================================================================================================================== //
  // ===========================================    SD CARD 작업      ================================================= //
  // ================================================================================================================== //

String currTime() {
    String bufStr, strTime ;   
    
    bufStr = "0" + String( hour() ) ;
    strTime +=  " " + bufStr.substring(bufStr.length()-2 ) ;
    bufStr = "0" + String( minute() ) ;
    strTime +=  bufStr.substring(bufStr.length()-2 ) ;
    bufStr = "0" + String( second() ) ;
    strTime +=  bufStr.substring(bufStr.length()-2 ) ;
    return strTime ;     
}

String filedate() {
      String bufStr, strTime, strMM, strDD ;   

    bufStr = "0" + String( month() ) ;
    strMM =  bufStr.substring(bufStr.length()-2 ) ;
    bufStr = "0" + String( day() ) ;
    strDD =  bufStr.substring(bufStr.length()-2 ) ;

    strTime = String( year()) + "-" + strMM + "-" + strDD ;
    strDate = String( year()) + strMM + strDD ;
    strMon = String( year()) + strMM  ;
    strMD  = strMM + strDD  ;
    
    bufStr = "0" + String( hour() ) ;
    strTime +=  " " + bufStr.substring(bufStr.length()-2 ) ;
    bufStr = "0" + String( minute() ) ;
    strTime +=  ":" + bufStr.substring(bufStr.length()-2 ) ;
    bufStr = "0" + String( second() ) ;
    strTime +=  ":" + bufStr.substring(bufStr.length()-2 ) ;
    return strTime ;     
}

void folderReady() {
    if (!sd.begin(SD_CONFIG)) {
      displayResponse("SDCard not ready") ;
      Card_Ready = false;
      return ;
    }
      Card_Ready = true;
      digitalWrite( sdPin, HIGH );
    char newFolder[32] = {} ;
      strMon.toCharArray(newFolder, strMon.length()+1);
    
    if (!sd.exists(newFolder)) {
      if(sd.mkdir(newFolder)) { 
          displayResponse("Folder created : " + String(newFolder)) ;
      } else {
          displayResponse("Folder creation failed : " + String(newFolder)) ;
      }
    }

    if (!sd.exists(newFolder)) {
      displayResponse("folderReady = No Folder : " + String(newFolder)) ;
      return ;
    } 

//    displayResponse("folder ready : " + String(newFolder)) ;

}

void saveReading(int chno) {    

    String strTimes =filedate() ;     
    SD_FB = false;
    folderReady();

    String filename = strMon + "/P"+String(chno+1)+"_" + strMD + ".txt" ;

    File dataFile;
    char logFile[32] = {} ;
      filename.toCharArray(logFile, filename.length()+1);

    if (!sd.exists(logFile)) {
       String dataHeader = "DateTime,Channel,On/Off,set_Volt,set_mA,act_Volt,act_mA,count,time";
       dataFile = sd.open( logFile , FILE_WRITE);
       if (dataFile) {
          dataFile.println(dataHeader) ;
          Serial.println("file created : " + String(logFile)) ;
          Serial.flush();
       }
          dataFile.close();
          volDmp();
    }
    if (!sd.exists(logFile)) {
      Serial.println("No File : " + String(logFile)) ;
      Serial.flush();
      return;
    }
    
    dataFile = sd.open( logFile , FILE_WRITE);
    if (dataFile) {
      int startAt , endAt ;
      startAt=0;
      endAt = logQueue[chno].indexOf('|',startAt);
      while (endAt>0) {  
        String readStr = logQueue[chno].substring(startAt, endAt);
        if(readStr.length()>4) dataFile.println(readStr) ;
        startAt = endAt+1;
        endAt = logQueue[chno].indexOf('|',startAt);
      }
      dataFile.close();
      Serial.println("[WRITE]" + String(logFile) + " : " + logQueue[chno] ) ;
      SD_FB = true;
    } else {
      Serial.println("error opening " + String(logFile)) ;
    }
    Serial.flush();
    logQueue[chno]="";
}


void saveEvent(String eventType , String eventStr) {    
    String strTimes =filedate() ;     
    SD_FB = false;
    folderReady();
    
    char newFolder[32] = {} ;
      strMon.toCharArray(newFolder, strMon.length()+1);
    
    if (!sd.exists(newFolder)) {
      Serial.println("No Folder : " + String(strMon)) ;
    }
    
      String filename = strMon + "/" + strDate + ".log" ;
      String dataContent = strTimes + "," + eventStr ;

    File dataFile;
      char logFile[32] = {} ;
      filename.toCharArray(logFile, filename.length()+1);

    if (!sd.exists(strMon)) {
      Serial.println("No Folder1 : " + String(strMon)) ;
      Serial.flush();
      return;
    }
    
    if (!sd.exists(filename)) {
       dataFile = sd.open( filename , FILE_WRITE);
       if (dataFile) {
          Serial.println("file created : " + String(filename)) ;
       }
          dataFile.close();
    }
    if (!sd.exists(filename)) {
      Serial.println("No File2 : " + String(filename)) ;
      Serial.flush();
      return;
    }
    
    dataFile = sd.open( filename , FILE_WRITE);
    if (dataFile) {
      dataFile.println(dataContent) ;
      dataFile.close();
      Serial.println("[EVENT]" + String(filename) + " : " + dataContent ) ;
    SD_FB = true;
    } else {
      Serial.println("error opening " + String(filename)) ;
    }
}

void mySDBegin() {
    // Initialize the SD card.
  if (!sd.begin(SD_CONFIG)) {
    Serial.println("initialization failed. Things to check:");
    Card_Ready = false;
    return;
  }
    Serial.println("Wiring is correct and a card is present.");
  Card_Ready = true;
  volDmp();
}
//------------------------------------------------------------------------------
/*
 * User provided date time callback function.
 * See SdFile::dateTimeCallback() for usage.
 */
void dateTime(uint16_t* date, uint16_t* time) {
  // User gets date and time from GPS or real-time
  // clock in real callback function

  // return date using FAT_DATE macro to format fields
  *date = FAT_DATE(year(), month(), day());

  // return time using FAT_TIME macro to format fields
  *time = FAT_TIME(hour(), minute(), second());
}
//------------------------------------------------------------------------------
void printFreeSpace(){
  configWData[4]=String(sdFree) + " MB";
  displayResponse("[SDCARD] Card Space :" + String(sdSize) + " MB");
  displayResponse("[SDCARD] Free Space :" + String(sdFree) + " MB");
}
void volDmp() {
  uint32_t cardSize;
    cardSize = sd.card()->cardSize();
  if (cardSize == 0) {
    Serial.println("cardSize failed");
    return;
  }
  sdSize = cardSize/2/1024;
  uint32_t volFree = sd.vol()->freeClusterCount();
  sdFree = 0.000488 * volFree * (sd.vol()->blocksPerCluster());
  configWData[4]=String(sdFree) + " MB";
}

  // ================================================================================================================== //
  // ===========================================     콘트롤러 온도      ================================================= //
  // ================================================================================================================== //
void ReadingTemperature() {
  float temperature;
  temperature = dht22.readTemperature();

  if(isnan(temperature)) {
    temperature  = getTemp();
  } 

  char bufStr[26],  Cha[10];
  sprintf(bufStr, "Temp. n/a",0);
  if ( temperature > -100 && temperature < 100) {
        dtostrf( temperature    ,  6, 1, Cha );
        sprintf(bufStr, "Temp. %s", Cha);
  }
  lcdMessage[2] = String(bufStr);
  configWData[5] = String(bufStr) + "°C";
}

float getTemp() {
  byte data[12];
  byte addr[8];
  if ( !ds.search(addr)) {
    ds.reset_search();
    return -1000;
  }
  if ( OneWire::crc8( addr, 7) != addr[7]) {
    Serial.println("CRC is not valid!");
    return -1000;
  }
  if ( addr[0] != 0x10 && addr[0] != 0x28) {
    Serial.print("Device is not recognized");
    return -1000;
  }
  ds.reset();
  ds.select(addr);
  ds.write(0x44, 1);
  byte present = ds.reset();
  ds.select(addr);
  ds.write(0xBE);
  for (int i = 0; i < 9; i++)  {
    data[i] = ds.read();
  }
  ds.reset_search();
  byte MSB = data[1];
  byte LSB = data[0];
  float tempRead = ((MSB << 8) | LSB);
  float TemperatureSum = tempRead / 16;
  return TemperatureSum;
}
//------------------------------------------------------------------------------
