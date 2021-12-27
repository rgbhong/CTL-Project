
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <ESP8266mDNS.h>
#include <WiFiUdp.h>
#include <time.h>
#include <TimeLib.h> 
#include "FS.h"

#include "DHT.h"
#include "DFRobot_BMP388_I2C.h"
#include "DFRobot_BMP388.h"
#include "Wire.h"
#include "SPI.h"
#include "bmp3_defs.h"
#include <SoftwareSerial.h>

int led = LED_BUILTIN;
int dataPin = D2;
int clockPin = D1;
int DHT21PINB = D5;     // Digital pin connected to the DHT sensor
int DHT21PINA = D4;     // Digital pin connected to the DHT sensor
int rxPin = D7;
int txPin = D8;

bool    Wifi_Ready, ST_Ready, BT_Ready, TH_Mode, PostMode ;
String  postAgent = "/engr/post_drybox.php" ;
String  time2string[5] = {""};
unsigned long prevW , prevM , prevP , intervalP=10000 , durationP=0;
int     countP=0, countTH=0 , sts=0;
float   valTempHumi[2][2]={0}, valPress=0, pressure, temp, humi, tempV=0, humiV=0;

String  configVName[]={"데이터 저장 모드","데이터 센싱 모드","데이터 기록 시간(분)","데이터 기록 간격(초)"};
String  configDName[]={"드라이박스 이름","테스트 ID"};
String  configWName[]={"와이파이 이름","센서 IP Address","WiFi Status","WiFi AP 신호강도"};
String  configIName[]={"Wifi 이름","Wifi Type","Wifi패스워드","서버 IP Address","고정 IP Address","Gate IP Address","Subnet IP Address"};

String  configVSet[]={"PostMode","TH_Mode","Duration","Interval"};
String  configDSet[]={"DryBox","opStep"};
String  configWSet[]={"WifiSSID","LocalIP","Wifi","WifiRSSI"};
String  configISet[]={"WifiName","WifiNum","Password","ServerIP","LineIP","gateway","subnet"};

int     configVData[]={ 0,  1,  7200, 30 };
String  configDData[]={"DryBox","standby"};
String  configWData[]={".?.",".?.",".?.",".?."};
String  configIData[]={"office2ctl","0","ctlinc5300","220.120.155.254","220.120.155.186","220.120.155.1","255.255.255.0"};
int     sizeV=4, sizeD=2, sizeW=4, sizeI=7;

String  WifiStatus[7]={"WL_IDLE_STATUS","WL_NO_SSID_AVAIL","WL_SCAN_COMPLETED","WL_CONNECTED","WL_CONNECT_FAILED","WL_CONNECTION_LOST","WL_DISCONNECTED"};
String  rcvUSB, rcvBT;


DHT dht21A(DHT21PINA, DHT21);
DHT dht21B(DHT21PINB, DHT21);
/* Create a bmp388 object to communicate with IIC.*/
DFRobot_BMP388_I2C bmp388;

/* Don't hardwire the IP address or we won't get the benefits of the pool.
    Lookup the IP address for the host name instead */
//IPAddress timeServer(129, 6, 15, 28); // time.nist.gov NTP server
IPAddress timeServerIP; // time.nist.gov NTP server address
const char* ntpServerName = "time.nist.gov";
const int NTP_PACKET_SIZE = 48; // NTP time stamp is in the first 48 bytes of the message
byte packetBuffer[ NTP_PACKET_SIZE]; //buffer to hold incoming and outgoing packets

IPAddress PHPserver, lineIP, gateway, subnet, accessIP ;

// A UDP instance to let us send and receive packets over UDP
WiFiUDP udp;
ESP8266WebServer server(80);

int serverport = 80;
WiFiClient Wificlient;

SoftwareSerial BTserial(rxPin, txPin); // RX, TX

void setup() {
  Serial.begin(115200);
  BTserial.begin(9600);  
  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite( LED_BUILTIN, HIGH );
  Serial.println();

  if (SPIFFS.begin()) {
    FSInfo fsInfo;
    SPIFFS.info(fsInfo);
    listDir("/");
    readFile();
  }
  
  setFixedIP();
  setLogger() ;  
  connectWifi();
  
  dht21A.begin();
  dht21B.begin();
  bmp388.set_iic_addr(BMP3_I2C_ADDR_SEC);
  bmp388.begin();

  readTime();
  configVData[0]=0;
  configDData[0]="Dry_" + time2string[4];
  PostMode = false;
  TH_Mode = true;
  dispConfig(8);
}

void connectWifi() {
  String ssid = configIData[0] ;             // your network SSID (name)
  String sspw = configIData[2];        // your network password
  unsigned int localPort = 2390;      // local port to listen for UDP packets
  IPAddress officeAdd(192,168,0,17);
  // We start by connecting to a WiFi network
  displayResponse("Connecting to :" + ssid);
  WiFi.disconnect();
  if(configIData[1]=="0") {
    WiFi.config(officeAdd, gateway, subnet);
  } else {
    WiFi.config(lineIP, gateway, subnet);
  }
  WiFi.begin(ssid, sspw);
  WiFi.setAutoConnect(true); //자동 접속 설정
  WiFi.setAutoReconnect(true); //자동 재접속 설정 
  WiFi.waitForConnectResult();
  WiFi.mode(WIFI_STA);
  Wifi_Ready = false;
  ST_Ready = false;
  
  if (WiFi.status() == WL_NO_SHIELD) {
    displayResponse("WiFi shield not present");
    return;
  }

  prevW = millis() + 30000 ;
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    if(prevW < millis()) break;
  }
    Serial.println("");

  if (WiFi.status() == WL_CONNECTED) {
    displayResponse("WiFi connected");
    Serial.println("IP address: ");
    accessIP = WiFi.localIP() ;
    configWData[0] = WiFi.SSID();
    configWData[1] = IP2String(accessIP);
    displayResponse(configWData[1]);

    Serial.printf("MAC address = %s ", WiFi.softAPmacAddress().c_str());
    Serial.println("\nStarting UDP");
    udp.begin(localPort);
    Serial.print("Local port: ");
    Serial.println(udp.localPort());
    
    Wificlient.connect(PHPserver, serverport);
    getTimeFromNTP();
  } else {
    displayResponse("WiFi connection fail");
  }

}

void setFixedIP(){
  PHPserver=Str2IP(configIData[3]);
  lineIP= Str2IP(configIData[4]);
  gateway=Str2IP(configIData[5]); // 게이트웨이 주소
  subnet=Str2IP(configIData[6]); // 서브넷 주소
}

void setLogger(){
  TH_Mode = configVData[1]==1;
  if(TH_Mode==false) configVData[1]==0;
  PostMode = configVData[0]==1;
  durationP = millis() + configVData[2] * 60000 ;
  intervalP = configVData[3] * 1000;
}

void loop() {

    if ( BTserial.available() )    {
      BT_reading();
      return;
    }
    
    if ( Serial.available() )    {
      usb_reading();
      return;
    }
    
    Wifi_Ready = (WiFi.status() == WL_CONNECTED) ;
    
    while(Wificlient.available()){
        String rcv=Wificlient.readStringUntil('\r');
        if(rcv.indexOf("HTTP/1.1") >= 0) {
            displayResponse("[FromServer]" + rcv);
          Serial.flush();
        }
        if(rcv.indexOf("<BR>") >= 0) {
          int st_index = rcv.indexOf("<BR>",0);
          int val_index = rcv.indexOf("<BR>", st_index+1);
//            displayResponse("[FromServer]" + rcv.substring(st_index , val_index+4));
          Serial.flush();
        }
    }

   if ( millis() < prevM )  prevM = millis() ;
   if ( millis() - prevM > 5000) timerLoop();

   if( TH_Mode == true && Wifi_Ready == true && PostMode == true) {
     if ( millis() < prevP )  prevP = millis() ;
     if ( millis() - prevP > intervalP) timerPost();
   }
    if(Wifi_Ready == true && ST_Ready==false) getTimeFromNTP();
}

void usb_reading() {
    while(Serial.available()) {
      int byteSend;
      byteSend = Serial.read();
      if (byteSend == 10 || byteSend == 13)   {
        if(rcvUSB.length()>1) {
          Serial.println("[USB_comm]" + rcvUSB);
          Serial.flush();
          if(rcvUSB.startsWith("AT")) {
            setBT(rcvUSB);
          } else {
            Configuration(rcvUSB);
          }
        }
         rcvUSB = "";
      } else {
        rcvUSB += char(byteSend);
      }      
  }
}

void BT_reading() {
    while (BTserial.available())    {
      int byteSend;
      byteSend = BTserial.read();
      if (byteSend == 10 || byteSend == 13)   {
        if(rcvBT.length()>1) {
          if(rcvBT.startsWith("[BTcomm]")){
            Configuration(rcvBT.substring(8));
          } else {
            Serial.println("[fromBT]" + rcvBT);
            Serial.flush();
            if(rcvBT=="+DISC:SUCCESS") {
              BT_Ready=true;              
              Configuration("miniconfig");
            }else if(rcvBT=="ERROR:(0)") {
              BT_Ready=false;
            }
          }
        }
         rcvBT = "";
      } else {
        rcvBT += char(byteSend);
      }      
  }
}

void timerLoop() {
  prevM = millis() ;
    digitalWrite( LED_BUILTIN, digitalRead( LED_BUILTIN ) ^ 1 );
  if(TH_Mode == true) {
    readTime();
    readBMP3();
    readDHT21A();
    readDHT21B();
    
    SendBT();
  }
  
  if(Wifi_Ready == true && year() < 2020) {
    getTimeFromNTP() ;
    return;
  }  
}

void timerPost() {
    char buf[80]; 
    prevP = millis() ;
  String PostVal = "table=drybox&act=ADD" ; 
    PostVal += "&StrNew=(DryBox, Status, DateTime, Temperature, Humidity, Pressure, press0, temp1, temp2, humi1, humi2 ) VALUES " ;
    PostVal += "('" + configDData[0] + "','" + configDData[1] + "','" + time2string[2] + "'" ;
    sprintf(buf , ",%.1f,%.1f" , tempV/countTH , humiV/countTH); 
    PostVal += buf;
    sprintf(buf , ",%.1f,%.1f" , valPress/countP , pressure); 
    PostVal += buf;
    sprintf(buf , ",%.1f,%.1f,%.1f,%.1f)" , valTempHumi[0][0] ,valTempHumi[1][0] , valTempHumi[0][1], valTempHumi[1][1]); 
    PostVal += buf;
    PostVal += "&Key_Val=" +  configDData[0] + "," + configDData[1] + "," + time2string[2] ;
    countTH=0; countP=0; tempV=0; humiV=0; valPress=0;
    writePost(PostVal,configDData[0] + ":" + time2string[2]) ;
  if ( millis() > durationP ) PostMode == false ;
}

String SplitValue(String data, char separator, int index ) {
  int found = 0, strIndex[]={0,-1}, maxIndex = data.length()-1 ;
  for(int i=0; i<=maxIndex && found<=index; i++)  {
    if(data.charAt(i) == separator || i==maxIndex ) {
      found++;
      strIndex[0] = strIndex[1]+1 ;
      strIndex[1] = (i==maxIndex) ? i+1 : i ;
    }
  }
  return found>index ? data.substring(strIndex[0], strIndex[1]) : "" ;
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

void readBMP3() {
  // Read values from the sensor
      pressure = bmp388.readPressure();

    if (!isnan(pressure)) {
      pressure /= 1000;
      valPress += pressure;
      countP++;
    } else {
      pressure = 0;
    }
      Serial.printf("BMP388\t pressure\t %.1f kPa " , pressure);
      Serial.println();
}

void readDHT21A() {
  // Read values from the sensor
  temp = dht21A.readTemperature();
  humi = dht21A.readHumidity();
      valTempHumi[0][0] = 0;
      valTempHumi[0][1] = 0;
    if (!isnan(temp) && !isnan(humi)) {
      valTempHumi[0][0] = temp;
      valTempHumi[0][1] = humi;
      tempV += temp;
      humiV += humi;
      countTH++;
    }
      Serial.printf("CM2122(A)\t Temperature \t %.1f℃ \t Humidity \t %.1f%%" , temp, humi);
      Serial.println();
}

void readDHT21B() {
  // Read values from the sensor
  temp = dht21B.readTemperature();
  humi = dht21B.readHumidity();
      valTempHumi[1][0] = 0;
      valTempHumi[1][1] = 0;
    if (!isnan(temp) && !isnan(humi)) {
      valTempHumi[1][0] = temp;
      valTempHumi[1][1] = humi;
      tempV += temp;
      humiV += humi;
      countTH++;
    }
      Serial.printf("CM2122(B)\t Temperature \t %.1f℃ \t Humidity \t %.1f%%" , temp, humi);
      Serial.println();
}

void writePost(String PostData, String keyData){
    Wificlient.connect(PHPserver, serverport);
    Wificlient.setTimeout(5000);
    if(Wificlient.connected()){
        Wificlient.println("POST " + postAgent +" HTTP/1.1");
        Wificlient.println("Host:  " + IP2String(PHPserver) );
        Wificlient.println("Content-Type: application/x-www-form-urlencoded;");
        Wificlient.print("Content-Length: ");
        Wificlient.println(PostData.length());
        Wificlient.println();
        Wificlient.println(PostData);
        displayResponse("[ToServer]=>" + keyData );
    } else {
        displayResponse("[ToServer]데이터 전송오류");
        Wificlient.connect(PHPserver, serverport);
    }    
}

void Configuration(String configAction){
    String value="",action=configAction ;
//    if(configAction.startsWith("error")) return;
    if(configAction.indexOf(" ",1)>0) {
       value=configAction.substring(configAction.indexOf(" ",1)+1);
       action=configAction.substring(0,configAction.indexOf(" ",1));
    }
    action.toUpperCase();
    displayResponse("[Config]" + action + ">" + value);
    action.toLowerCase();
    action.replace(" ","");
      
    setConfig(action, value);
}

void setConfig(String action, String value){
    String actMode[2]={"정지","동작"};
    String setAction="";
    int aType=-1;
    int aInx=-1;
    String valFB = " : ";
    String actFB = "";
    if(value.length()>0) valFB = " 변경 => ";
    for(int i=0; i<sizeV; i++) {
      actFB = configVSet[i] ;
      actFB.toLowerCase();
      if(action==actFB) {
        setAction=action ;
        aType=0;
        aInx=i;
      }
    }
    for(int i=0; i<sizeD; i++) {
      actFB = configDSet[i] ;
      actFB.toLowerCase();
      if(action==actFB) {
        setAction=action ;
        aType=1;
        aInx=i;
      }
    }
    for(int i=0; i<sizeW; i++) {
      actFB = configWSet[i] ;
      actFB.toLowerCase();
      if(action==actFB) {
        setAction=action ;
        aType=2;
        aInx=i;
      }
    }
    for(int i=0; i<sizeI; i++) {
      actFB = configISet[i] ;
      actFB.toLowerCase();
      if(action==actFB) {
        setAction=action ;
        aType=3;
        aInx=i;
      }
    }
    
    if(aType==0) {
      if(value.length()>0) {
        configVData[aInx]=value.toInt();
        setLogger() ;
      }
      if(aInx==0) {
        valFB += configWData[0];
      } else if(aInx==1 || aInx==2) {
        valFB += actMode[configVData[aInx]];
      } else {
        valFB += String(configVData[aInx]);  
      }
        displayResponse(configVName[aInx] + valFB);
    }
    if(aType==1) {
      if(value.length()>0) {
        configDData[aInx]=value;
      }
        valFB += configDData[aInx];  
        displayResponse(configDName[aInx] + valFB);
    }
    if(aType==2) {
      if(value.length()>0) {
        configWData[aInx]=value;
      }
        valFB += configWData[aInx];  
        displayResponse(configWName[aInx] + valFB);
    }
    if(aType==3) {
      if(value.length()>0) {
        configIData[aInx]=value;
      }
        valFB += configIData[aInx];  
        displayResponse(configIName[aInx] + valFB);
    }
    if(aType<0 ) {
      if( action == "wifirestart") {
        connectWifi();
      } else if( action == "wifiscan") {
        Wifi_Scan();
      } else if( action == "readconfig") {
        readFile();
      } else if( action == "saveconfig") {
        writeFile();
      } else if( action == "currconfig") {
        dispConfig(9);
      } else if( action == "miniconfig") {
        dispConfig(8);
      } else if( action.startsWith("config-")) {
        dispConfig(action.substring(7,8).toInt());
      } else if( action == "??") {
          for (int i=0; i<sizeV; i++) {
            displayResponse(configVSet[i] + ":" + configVName[i]);
          }
          for (int i=0; i<sizeD; i++) {
            displayResponse(configDSet[i] + ":" + configDName[i]);
          }
          for (int i=0; i<sizeW; i++) {
            displayResponse(configWSet[i] + ":" + configWName[i]);
          }
          for (int i=0; i<sizeI; i++) {
            displayResponse(configISet[i] + ":" + configIName[i]);
          }
        displayResponse("WifiRestart : Wifi접속 start");
        displayResponse("ReadConfig : 설정치 re-load");
        displayResponse("SaveConfig : 설정치 저장");
      } else {
       displayResponse("Unknown set command : " + action + valFB );
      }
    }
}

void setBT(String action){
  char printBT[100];
    if( action == "AT") {
        Serial.print("블루투스 통신 상태 : ");
    } else if( action.startsWith("AT+NAME")) {
        Serial.print("블루투스 이름 변경 : ");
    }
//    action += "\r\n"  ;
//    action.toCharArray(printBT, action.length()+1);
    BTserial.println(action);
    Serial.println();
}

void Wifi_Scan() {
  int n = WiFi.scanNetworks();
  int order = 30;
  if (n == 0) {
    displayResponse("no networks found!");
  } else {
    displayResponse("networks found!");
    for (int i = 0; i < n; ++i) {
      displayResponse( WiFi.SSID(i) + " [" + String(WiFi.RSSI(i)) + "] " +  (WiFi.encryptionType(i) == ENC_TYPE_NONE) ? " " : "*");
    }
  }
}

void displayResponse(String AlertStr){
    Serial.println(AlertStr);
    BTserial.println(AlertStr);
}

void listDir(const char * dirname){
  Serial.printf("Listing directory: %s\r\n", dirname);
  Dir dir = SPIFFS.openDir(dirname);
  while (dir.next()) {
    Serial.print("File Name: "); Serial.print(dir.fileName());
    if(dir.fileSize()) {
      File f = dir.openFile("r");
      Serial.print(", Size: "); Serial.println(f.size());
    }
  }
}

void readFile(){
  char * path = "/foo.txt" ;
  File file = SPIFFS.open(path, "r");
  if(!file || file.isDirectory()){
    displayResponse("- failed to open file for reading");
    return;
  }
  displayResponse("read from file:");
  while(file.available()){
    String line = file.readStringUntil('\n'); 
    displayResponse(line);
    if(line.length()>3 ) {
        int grp= line.substring(0,1).toInt();
        int inx= line.substring(1,2).toInt();
        String value=line.substring(3) ;
        value.replace("\n","");
        value.replace("\r","");
        if(grp==0 && inx<sizeV) {
          displayResponse("[CONFIG]" + configVName[inx] + ":" + configVSet[inx] + "=>" + value );
          if(configVData[inx] != value.toInt()) Serial.println(value+":"+String(value.length()));
          configVData[inx] = value.toInt();
        }
        if(grp==1 && inx<sizeD) {
          displayResponse("[CONFIG]" + configDName[inx] + ":" + configDSet[inx] + "=>" + value);
          if(configDData[inx] != value ) Serial.println(value+":"+String(value.length()));
          configDData[inx] = value;
        }
        if(grp==2 && inx<sizeW) {
          displayResponse("[CONFIG]" + configWName[inx] + ":" + configWSet[inx] + "=>" + line.substring(3));
          if(configWData[inx] != value ) Serial.println(value+":"+String(value.length()));
          configWData[inx] = value;
        }
        if(grp==3 && inx<sizeI) {
          displayResponse("[CONFIG]" + configIName[inx] + ":" + configISet[inx] + "=>" + line.substring(3));
          if(configIData[inx] != value ) Serial.println(value+":"+String(value.length()));
          configIData[inx] = value;
        }
    }
  }
  file.close();
  displayResponse("저장매체로부터 Configuration 정보를 읽었습니다.");
}

void writeFile(){
  char * path = "/foo.txt" ;
  File file = SPIFFS.open(path, "w");
  if(!file){
    displayResponse("failed to open file for writing");
    return;
  }
    for (int i=0; i<sizeV; i++) {
      if(file.println("0" + String(i) + ":" + String(configVData[i])) ) {
        displayResponse("file written  0" + String(i) + ":" + String(configVData[i]));
      } else {
        displayResponse("write failed 0" + String(i));
      }
    }
    for (int i=0; i<sizeD; i++) {
      if(file.println("1" + String(i) + ":" + String(configDData[i])) ) {
        displayResponse("file written  1" + String(i) + ":" + String(configDData[i]));
      } else {
        displayResponse("write failed 1" + String(i));
      }
    }
    for (int i=0; i<sizeW; i++) {
      if(file.println("2" + String(i) + ":" + String(configWData[i])) ) {
        displayResponse("file written  2" + String(i) + ":" + String(configWData[i]));
      } else {
        displayResponse("write failed 2" + String(i));
      }
    }
    for (int i=0; i<sizeI; i++) {
      if(file.println("3" + String(i) + ":" + String(configIData[i])) ) {
        displayResponse("file written  3" + String(i) + ":" + String(configIData[i]));
      } else {
        displayResponse("write failed 3" + String(i));
      }
    }
    file.close();
  displayResponse("저장매체에 Configuration 정보를 입력했습니다.");
}

void dispConfig(int conType){
  String miniC = "[MINI]" ;
  for (int i=0; i<2; i++) {
    miniC += ":" + String(configVData[i]);
  }
  for (int i=0; i<2; i++) {
    miniC += ":" + configDData[i];
  }
  displayResponse(miniC);
  
  if(conType==0 || conType==9){
  for (int i=0; i<sizeV; i++) {
    displayResponse("[READ]"+ configVName[i] + ":0" + String(i) + ":" + configVSet[i] + ":" + String(configVData[i]));
  }
  }
  
  if(conType==1 || conType==9){
  for (int i=0; i<sizeD; i++) {
    displayResponse("[READ]"+ configDName[i] + ":1" + String(i) + ":" + configDSet[i] + ":" + configDData[i]);
  }
  }

  if(conType==2 || conType==9){

    displayResponse("[READ]"+ configWName[0] + ":20:" + configWSet[0] + ":" + String(WiFi.SSID()));
    displayResponse("[READ]"+ configWName[1] + ":21:" + configWSet[1] + ":" + IP2String(accessIP) );
    displayResponse("[READ]"+ configWName[2] + ":22:" + configWSet[2] + ":" + WifiStatus[WiFi.status()] );
    displayResponse("[READ]"+ configWName[3] + ":23:" + configWSet[3] + ":" + String(WiFi.RSSI())+" dBm");
  }

  if(conType==3 || conType==9){
    for (int i=0; i<sizeI; i++) {
      displayResponse("[READ]"+ configIName[i] + ":3" + String(i) + ":" + configISet[i] + ":" + configIData[i]);
    }
  }
}


void SendBT(){
  char printBT[100], h02[100];
//    String cTime = "[DATA]," + time2string[1]  ;
//    cTime.toCharArray(printBT, cTime.length()+1);
//    Serial.write(printBT);
//    BTserial.write(printBT);
    
    sprintf(printBT, "[DATA], %s, %3.1f, %3.1f, %3.1f, %3.1f, %.1f, \r\n", time2string[1].c_str(), valTempHumi[0][0], valTempHumi[1][0], valTempHumi[0][1], valTempHumi[1][1], pressure);
    BTserial.write(printBT);
//    sprintf(printBT, " %3.1f, %3.1f, \r\n", valTempHumi[0][1], valTempHumi[1][1]);
//    BTserial.write(printBT);
}

void getTimeFromNTP() {

  //get a random server from the pool
  WiFi.hostByName(ntpServerName, timeServerIP);

  sendNTPpacket(timeServerIP); // send an NTP packet to a time server
  // wait to see if a reply is available
  delay(1000);

  int cb = udp.parsePacket();
  if (!cb) {
    Serial.println("no packet yet");
  } else {
    Serial.print("packet received, length=");
    Serial.println(cb);
    // We've received a packet, read the data from it
    udp.read(packetBuffer, NTP_PACKET_SIZE); // read the packet into the buffer

    //the timestamp starts at byte 40 of the received packet and is four bytes,
    // or two words, long. First, esxtract the two words:

    unsigned long highWord = word(packetBuffer[40], packetBuffer[41]);
    unsigned long lowWord = word(packetBuffer[42], packetBuffer[43]);
    // combine the four bytes (two words) into a long integer
    // this is NTP time (seconds since Jan 1 1900):
    unsigned long secsSince1900 = highWord << 16 | lowWord;
    Serial.print("Seconds since Jan 1 1900 = ");
    Serial.println(secsSince1900);

    // now convert NTP time into everyday time:
    Serial.print("Unix time = ");
    // Unix time starts on Jan 1 1970. In seconds, that's 2208988800:
    const unsigned long seventyYears = 2208988800UL;
    // subtract seventy years:
     unsigned long epoch = secsSince1900 - seventyYears;
     // print Unix time:
     Serial.println(epoch);

      epoch = epoch + 9 * 3600; // 한국시    +9
      setTime((time_t)epoch);

     // print the hour, minute and second:
      Serial.println();             // UTC is the time at Greenwich Meridian (GMT)
      Serial.print("Controller Time : ");
      readTime();
      displayResponse(time2string[2]); 
      ST_Ready = true;
  }
}

// send an NTP request to the time server at the given address
void sendNTPpacket(IPAddress& address) {
  Serial.println("sending NTP packet...");
  // set all bytes in the buffer to 0
  memset(packetBuffer, 0, NTP_PACKET_SIZE);
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
  udp.beginPacket(address, 123); //NTP requests are to port 123
  udp.write(packetBuffer, NTP_PACKET_SIZE);
  udp.endPacket();
}

void readTime() {
      String bufStr, strDate, strTime, strYY, strMM, strDD, strHour, strMin, strSec ;   

    strYY = String( year());
    bufStr = "0" + String( month() ) ;
    strMM =  bufStr.substring(bufStr.length()-2 ) ;
    bufStr = "0" + String( day() ) ;
    strDD =  bufStr.substring(bufStr.length()-2 ) ;

    bufStr = "0" + String( hour() ) ;
    strHour =  bufStr.substring(bufStr.length()-2 ) ;
    bufStr = "0" + String( minute() ) ;
    strMin =  bufStr.substring(bufStr.length()-2 ) ;
    bufStr = "0" + String( second() ) ;
    strSec =  bufStr.substring(bufStr.length()-2 ) ;

    time2string[0] = strYY + "-" + strMM + "-" + strDD ;
    time2string[1] = strHour + ":" + strMin + ":" + strSec ;
    time2string[2] = time2string[0] + " " + time2string[1] ;
    time2string[3] = strYY + strMM + strDD + strHour + strMin + strSec ;
    time2string[4] = strYY.substring(2) + strMM + strDD + strHour + strMin + strSec ;
}
