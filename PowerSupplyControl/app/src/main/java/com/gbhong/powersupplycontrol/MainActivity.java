package com.gbhong.powersupplycontrol;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public static final String WIFE_STATE = "WIFE";
    public static final String MOBILE_STATE = "MOBILE";
    public static final String NONE_STATE = "NONE";
    String TAG = "PS Main",fabStr="";
    String BT_New="", BT_Default = "";

    private BluetoothSPP bt;
    TextView mDumpTextView  ;
    Switch swP, swT ;
    ScrollView mScrollView;
    LinearLayout LayoutData, LayoutSetting;
    ActionBar actionbar;
    Context context;
    Timer timer;

    public float ScreenWidth, ScreenHeight;
    int[] readText = {0,0,0,0,0} ;
    int timerCount=0;
    float[] readValue = {0,0,0,0} ;
    final String[] onoff = {"Off","On","OnOff"};

    private Button[] btnSetCtl = null;
    private ListView[] lstSetCtl = null;

    ListView ps_dataList, listViewD, listViewV, listViewW, listViewI, listViewP;
    ArrayList<HashMap<String, String>> PS_ReadingVlaue=null, PS_settingVlaue=null, AP_list=null;
    SimpleAdapter PS_adapter=null,mAdapter=null, WiFi_adapter=null;
    ConfigList_Adapter  Val_adapter, Wifi_adapter, I_adapter, P_adapter;
    ArrayList<Config_Data> PS_Data = new ArrayList<>();
    ArrayList<Config_Data> Val_Data = new ArrayList<>();
    ArrayList<Config_Data> Wifi_Data = new ArrayList<>();
    ArrayList<Config_Data> I_Data = new ArrayList<>();
    ArrayList<Config_Data> P_Data = new ArrayList<>();

    private boolean textIn_status = false, IP_fixed=false;
    private FloatingActionButton fabMain;
    GestureDetector mGestureDetector;
    InputMethodManager imm;
    WifiManager wifiManager ;
    ConnectivityManager connManager ;
    private List<ScanResult> scanResult;
    private ArrayAdapter<HashMap<String, String>> adapter;
    ProgressBar progressBar;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        Logger("onCreate");

        getSupportActionBar().setTitle("블루투스 연결 확인");

        BT_Default = ReadToProperty("BT_Default");
        Logger(BT_Default + ":BT_Default ");
        swP = (Switch) findViewById(R.id.switchP);
        swP.setChecked(false); //초기 토글 버튼 체크 상태 지정
        swT = (Switch) findViewById(R.id.switchT);
        swT.setChecked(false); //초기 토글 버튼 체크 상태 지정
        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mDumpTextView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                textIn_status=true;
                switch( motionEvent.getActionMasked() ) {

                    case MotionEvent.ACTION_DOWN:   // 화면에 손가락이 닿음 // 모든 이벤트의 출발점
                        textIn_status=true; // true
                        break;
                    case MotionEvent.ACTION_UP: // 화면에서 손가락을 땜 // 사실상 이벤트의 끝
                        textIn_status=false;
                        break;
                    case MotionEvent.ACTION_MOVE:   // 화면에 손가락이 닿은 채로 움직이고 있음(움직일때마다 호출됨)
                        textIn_status=true;
                    break;
                }
                return false;
            }
        });
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);

        LayoutData=findViewById((R.id.layoutD));
        LayoutSetting=findViewById((R.id.layoutS));

        Bluetooth_Initial();
        Buttons_Initial();
        Listview_Setting();
        Swife_setting();

        AP_list = new ArrayList< HashMap<String,String> >();
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE); //인스턴스 get
        connManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE); //인스턴스 get
        NetworkInfo wifiInfo =
                connManager.getNetworkInfo( ConnectivityManager.TYPE_WIFI); //wifi 타입의 정보를 얻오온다.

        imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

        FirebaseMessaging.getInstance().subscribeToTopic(getString(R.string.default_notification_channel_name ));

    }

    public void checkPermission(){
        String[] permission_list = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_MEDIA_LOCATION,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };

        //현재 안드로이드 버전이 6.0미만이면 메서드를 종료한다.
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;

        for(String permission : permission_list){
            //권한 허용 여부를 확인한다.
            int chk = checkCallingOrSelfPermission(permission);
            if(chk == PackageManager.PERMISSION_DENIED){
                //권한 허용을여부를 확인하는 창을 띄운다
                requestPermissions(permission_list,0);
            }
        }
    }

    private void refresh() {
        scanResult = wifiManager.getScanResults();
        AP_list.clear();
        for(ScanResult result: scanResult) {
            HashMap<String,String> item = new HashMap<String,String>();
            item.put("ssid",result.SSID);
            item.put("level",result.SSID + " (" + result.frequency + " kHz , " + result.level + " )" );
            AP_list.add(item);
        }
        WiFi_adapter.notifyDataSetChanged();
    }

    void Bluetooth_Initial(){
        bt = new BluetoothSPP(this); //Initializing

        if (!bt.isBluetoothAvailable()) { //블루투스 사용 불가
            Logger("!bt.isBluetoothAvailable");
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() { //데이터 수신
            public void onDataReceived(byte[] data, String message) {
//                Logger("DataReceived"+message);
                recieveDataDisplay(message);
                addDumpText(mDumpTextView, message);
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() { //연결됐을 때
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(getApplicationContext()
                        , "Connected to " + name + "\n" + address
                        , Toast.LENGTH_SHORT).show();
                Logger("BluetoothConnection : "+name);
                timer4config();
            }

            public void onDeviceDisconnected() { //연결해제
                Logger("BluetoothConnection - onDeviceDisconnected");
                Toast.makeText(getApplicationContext()
                        , "Connection lost", Toast.LENGTH_SHORT).show();
                getSupportActionBar().setTitle("블루투스 연결 ..");
            }

            public void onDeviceConnectionFailed() { //연결실패
                Logger("BluetoothConnection - onDeviceConnectionFailed");
                Toast.makeText(getApplicationContext()
                        , "Unable to connect", Toast.LENGTH_SHORT).show();
                getSupportActionBar().setTitle("블루투스 연결 안됨");
            }
        });
    }

    void addDumpText(TextView textview, String newMessage) {
        if(!newMessage.startsWith("DW:")) {
            textview.append(newMessage+"\n");
            String str = textview.getText().toString();
            int strn = str.length() - str.replace(String.valueOf("\n"), "").length();
            if(strn > 500) {
                textview.setText(str.substring(str.indexOf("\n") + 1));
            }
            mScrollView.smoothScrollTo(0,textview.getBottom());
        }
    }

    void Buttons_Initial() {

        swP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sndMsg="0";
                if(swP.isChecked() == true){ //TODO 토글 버튼이 체크된경우 (ON)
                        sndMsg = "1";
                }
                BT_Send("[BTcomm]BT:ID:00:" + sndMsg);
            }
        });

        swT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sndMsg="0";
                if(swT.isChecked() == true){ //TODO 토글 버튼이 체크된경우 (ON)
                    sndMsg = "1";
                }
                BT_Send("[BTcomm]BT:ID:01:" + sndMsg);
            }
        });

        int[] btnID = {R.id.button0, R.id.button1, R.id.btn_chart, R.id.button3};
        int[] lstID = {R.id.ListDbox, R.id.ListValue, R.id.ListWifi, R.id.ListIP0};
        btnSetCtl = new Button[4];
        lstSetCtl = new ListView[4];
        for(int i=0; i<4; i++) {
            this.btnSetCtl[i] = (Button) findViewById(btnID[i]);
            this.lstSetCtl[i] = (ListView) findViewById(lstID[i]);
            this.btnSetCtl[i].setOnClickListener(setControler);
        }

        fabMain = findViewById(R.id.fabMain);
        fabMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Read_Config("SAVECONFIG");

            }
        });

    }

    void Listview_Setting() {
        ps_dataList = findViewById(R.id.ps_data);
        View header = getLayoutInflater().inflate(R.layout.ps_data_item, null, false);
        header.setBackgroundColor(ContextCompat.getColor(MainActivity.this,R.color.ListHeader));

        ps_dataList.addHeaderView(header);

        PS_ReadingVlaue = new ArrayList<HashMap<String, String>>();
        for(int i=0; i<4; i++) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("key00", "P"+(i+1));
            hashMap.put("key01", "");
            hashMap.put("key02", "");
            hashMap.put("key03", "");
            hashMap.put("key04", "");
            hashMap.put("key05", "");
            hashMap.put("key06", "");
            hashMap.put("title", "");
            hashMap.put("desc", "");
            PS_ReadingVlaue.add(hashMap);
        }
        String[] TagName = new String[] {"key00", "key01", "key02", "key03", "key04", "key05", "key06"};
        int[] textValue = new int[] {R.id.text1, R.id.text2, R.id.text3, R.id.text4, R.id.text5, R.id.text6, R.id.text7};
        mAdapter = new SimpleAdapter(this, PS_ReadingVlaue, R.layout.ps_data_item, TagName, textValue){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view=super.getView(position, convertView, parent);
                TextView outp = (TextView)view.findViewById(R.id.text2);
                TextView count = (TextView)view.findViewById(R.id.text7);
                if(outp.getText().toString()=="On"){
                    outp.setTextColor(ContextCompat.getColor(MainActivity.this,R.color.textColor));
                }else{
                    outp.setTextColor(ContextCompat.getColor(MainActivity.this,R.color.textFrame));
                }
                if(Integer.parseInt("0"+count.getText().toString())%2==0){
                    count.setTextColor(ContextCompat.getColor(MainActivity.this,R.color.textFrame));
                }else{
                    count.setTextColor(ContextCompat.getColor(MainActivity.this,R.color.textColor));
                }
                return view;
            }
        } ;
        ps_dataList.setAdapter(mAdapter);
        ps_dataList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if(position>0 && position<5) {
                    Toast.makeText(getApplicationContext(),
                            "P" + position + ":" + PS_ReadingVlaue.get(position-1).get("title") + "\n" + PS_ReadingVlaue.get(position-1).get("desc"),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        PS_settingVlaue = new ArrayList<HashMap<String, String>>();
        for(int i=0; i<4; i++) {
            HashMap<String, String> hashMap = new HashMap<String, String>();
            hashMap.put("set00", "P"+(i+1));
            hashMap.put("set01", "");
            hashMap.put("set02", "");
            hashMap.put("set03", "");
            hashMap.put("set04", "");
            hashMap.put("set05", "");
            PS_settingVlaue.add(hashMap);
        }
        String[] keyName = new String[] {"set00", "set01", "set02", "set03", "set04", "set05"};
        int[] chValue = new int[] {R.id.text1, R.id.text2, R.id.text3, R.id.text4, R.id.text5, R.id.text6};
        PS_adapter = new SimpleAdapter(this, PS_settingVlaue, R.layout.ps_channel_item, keyName, chValue) {
            @Override
            public View getView (int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView t1 = (TextView) view.findViewById(R.id.text1);
                TextView t2 = (TextView) view.findViewById(R.id.text2);
                t1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Alert_Mini(position);
                    }
                });
                t2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Alert_Mini(position);
                    }
                });
                TextView t3 = (TextView) view.findViewById(R.id.text3);
                TextView t4 = (TextView) view.findViewById(R.id.text4);
                TextView t5 = (TextView) view.findViewById(R.id.text5);
                TextView t6 = (TextView) view.findViewById(R.id.text6);
                t3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        channelDialog(position);
                    }
                });
                t4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        channelDialog(position);
                    }
                });
                t5.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        channelDialog(position);
                    }
                });
                t6.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        channelDialog(position);
                    }
                });

                return view;
            }
        };

        listViewD = findViewById(R.id.ListDbox);
        header = getLayoutInflater().inflate(R.layout.ps_channel_item, null, false);
        header.setBackgroundColor(ContextCompat.getColor(MainActivity.this,R.color.ListHeader));
        listViewD.addHeaderView(header);
        listViewD.setAdapter(PS_adapter); //리스트튜에 어댑터 장착

        listViewV = findViewById(R.id.ListValue);
        Val_adapter = new ConfigList_Adapter(this, Val_Data); //어댑터 클래스 객체 생성
        listViewV.setAdapter(Val_adapter); //리스트튜에 어댑터 장착
        listViewV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Alert_Popup(Val_adapter, Val_Data, position);
            }
        });

        listViewW = findViewById(R.id.ListWifi);
        Wifi_adapter = new ConfigList_Adapter(this, Wifi_Data); //어댑터 클래스 객체 생성
        listViewW.setAdapter(Wifi_adapter); //리스트튜에 어댑터 장착
        listViewW.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Toast.makeText(getApplicationContext(), Wifi_Data.get(position).getLabel() + "\n" + Wifi_Data.get(position).getValue(), Toast.LENGTH_SHORT).show();
            }
        });

        listViewI = findViewById(R.id.ListIP0);
        I_adapter = new ConfigList_Adapter(this, I_Data); //어댑터 클래스 객체 생성
        listViewI.setAdapter(I_adapter); //리스트튜에 어댑터 장착
        listViewI.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position==2) {
                    Alert_WiFi(I_adapter, I_Data, position);
                } else {
                    Alert_Popup(I_adapter, I_Data, position);
                }
            }
        });

        listViewP = findViewById(R.id.ListIP1);
        P_adapter = new ConfigList_Adapter(this, P_Data); //어댑터 클래스 객체 생성
        listViewP.setAdapter(P_adapter); //리스트튜에 어댑터 장착
        listViewP.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Alert_Popup(P_adapter, P_Data, position);
            }
        });
    }

    void Swife_setting() {
        getDisplayMetrics();
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {
                if(bt.isBluetoothAvailable() && Val_Data.size()>0){
                if((e1.getY() > ScreenHeight * 0.5) && (Math.abs(e2.getX()-e1.getX()) > 2 * Math.abs(e2.getY()-e1.getY()))) {
                    if (e1.getX() > ScreenWidth * 0.75 && velocityX < -200) {
                        Logger("Swife_setting - right");
                        Read_Config("PS_CONFIG");
                        LayoutSetting.setVisibility(View.VISIBLE);
                        LayoutData.setVisibility(View.GONE);
                    }
                    if (e1.getX() < ScreenWidth * 0.25 && velocityX > 200) {
                        Logger("Swife_setting - left");
                        Read_Config("TESTMODE");
                        LayoutData.setVisibility(View.VISIBLE);
                        LayoutSetting.setVisibility(View.GONE);
                    }
                }
                }
                return false;
            }
        });
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if(mGestureDetector.onTouchEvent(ev)){
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    void getDisplayMetrics()    {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        float screenHeight = displaymetrics.heightPixels;
        float screenWidth = displaymetrics.widthPixels;
        float density = displaymetrics.density;
        //    Toast.makeText(this, String.format("screenHeight:%d screenWidth:%d density:%f displaymetrics.xdpi:%f"), Toast.LENGTH_SHORT).show();
        ScreenWidth=screenWidth;
        ScreenHeight=screenHeight;
        //    tV_Left_TeamName.setText(String.valueOf(screenHeight));
    }

    void Logger(String LogMsg) {
        Log.d(TAG,LogMsg);
    }

    void recieveDataDisplay(String rData) {
        if(rData.startsWith("DW:")) {
            String[] readingParm=rData.substring(4).split(",");
 //           Logger(rData.substring(4));
            int position = Integer.valueOf(readingParm[0]);
            float outp=Float.valueOf(readingParm[1]);
            String currOnoff="";
            if(outp<2) currOnoff=onoff[(int) outp];
            if(position>0 && position<5) {
                if (readingParm.length > 6) {
                    PS_ReadingVlaue.get(position - 1).put("key01" , currOnoff);
                    for (int i = 2; i < 7; i++) {
                        PS_ReadingVlaue.get(position - 1).put("key0" + String.valueOf(i), readingParm[i]);
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
        }

        if(rData.startsWith("[ID]")) {
            String[] result = rData.split(":",-1);
            Logger(rData + ":" + result.length );
            if (result.length > 2) {
                swP.setChecked(Integer.parseInt(result[1])>0);
                swT.setChecked(Integer.parseInt(result[2])>0);
                Logger("저장/name/desc:" + result[1] + ":" + result[2]);
            }
            if (result.length > 10) {
                for(int i=0;i<4;i++){
                    PS_ReadingVlaue.get(i).put("title", result[i*2+3]);
                    PS_ReadingVlaue.get(i).put("desc", result[i*2+4]);
                }
            }
            getSupportActionBar().setTitle(bt.getConnectedDeviceName());
            BT_New=bt.getConnectedDeviceAddress();

        }
        if (rData.startsWith("ConfigDataSize")) {
            String[] result = rData.split(":");
            if (result.length > 4) {
                Val_Data.clear();
                Wifi_Data.clear();
                I_Data.clear();
                P_Data.clear();
//                for(int i=0; i<4; i++){
//                    Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
//                    PS_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
//                }
                for(int i=0; i<Integer.parseInt(result[1]); i++){
                    Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
                    Val_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                }
                for(int i=0; i<Integer.parseInt(result[2]); i++){
                    Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
                    Wifi_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                }
                for(int i=0; i<Integer.parseInt(result[3]); i++){
                    Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
                    I_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                }
                for(int i=0; i<Integer.parseInt(result[4]); i++){
                    Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
                    P_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                }
            }
        }

        if (rData.startsWith("[PSSET]")) {
            String[] readingParm = rData.substring(8).split(",");
            if (readingParm.length > 6) {
                int position = Integer.valueOf(readingParm[0]);
                int outp=Integer.valueOf(readingParm[6]);
                String currOnoff="";
                if(outp<0){
                    currOnoff="-";
                } else if(outp<3) {
                    currOnoff=onoff[(int) outp];
                }
                HashMap<String, String> hashMap = PS_settingVlaue.get(position-1);
                if(position>0 && position<5) {
                    hashMap.put("set01" , currOnoff);
                    for (int i = 2; i < 6; i++) {
                        hashMap.put("set0" + String.valueOf(i), readingParm[i]);
                    }
                    PS_settingVlaue.set(position-1,hashMap);
                    PS_adapter.notifyDataSetChanged();
                }
            }
        }
        if (rData.startsWith("[CONFIG]")) {
            String[] result = rData.split(":");
            if (result.length > 3 && Integer.parseInt(result[1]) > -1) {
                final int grp = Integer.parseInt(result[1].substring(0, 1));
                final int inx = Integer.parseInt(result[1].substring(1, 2));
                Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
                save_data.setLabel(result[2]); //set 메소드로 데이터 설정
                save_data.setInx(inx);
                save_data.setGrp(grp);
                save_data.setType(0);
                save_data.setValue(result[3]);
                Logger("[READ]"+result[1]+":"+save_data.getValue()+":"+save_data.getLabel());
                if (grp == 0) {
                    if (null == Val_Data.get(inx)) {
                        Val_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    } else {
                        Val_Data.set(inx, save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    }
                    Val_adapter.notifyDataSetChanged(); //리스트뷰 갱신
                } else if (grp == 1) {
                    if (null == Wifi_Data.get(inx)) {
                        Wifi_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    } else {
                        Wifi_Data.set(inx, save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    }
                    Wifi_adapter.notifyDataSetChanged(); //리스트뷰 갱신
                } else if (grp == 2) {
                    if (null == I_Data.get(inx)) {
                        I_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    } else {
                        I_Data.set(inx, save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    }
                    if (inx == 3) {
                        save_data.setType(2);
                        IP_fixed = save_data.getValue().equalsIgnoreCase("1");
                        IP_Config_Change();
                    }
                    I_adapter.notifyDataSetChanged(); //리스트뷰 갱신
                } else if (grp == 3) {
                    if (null == P_Data.get(inx)) {
                        P_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    } else {
                        P_Data.set(inx, save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    }
                    P_adapter.notifyDataSetChanged(); //리스트뷰 갱신
                }
                Logger("[READ]" + result[1] + " " + save_data.getLabel() + ":" + save_data.getValue() + ":" );
            }
        }
        if (rData.startsWith("[SAVED]")) {
            fabMain.setVisibility(View.INVISIBLE);
        }
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void onDestroy() {
        Logger("onDestroy");
//        bt.stopAutoConnect();
        bt.disconnect();
        bt.stopService(); //블루투스 중지

        super.onDestroy();
    }

    void Read_Config(String reqStr){
        BT_Send("[BTcomm]BT:"+reqStr);
        Logger("Request Config -> "+reqStr);
    }

    public static String getWhatKindOfNetwork(Context context){
        ConnectivityManager cm = (ConnectivityManager)     context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return WIFE_STATE;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return MOBILE_STATE;
            }
        }
        return NONE_STATE;
    }

    @Override
    public void onBackPressed() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("앱을 종료하시겠습니까?");
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) { // Canceled.
                dialog.cancel();
            }
        });
        AlertDialog dialog=builder.create();
        builder.show();
    }

    public void onStart() {
        super.onStart();
        Logger("onStart");
        if (!bt.isBluetoothEnabled()) { //
            Logger("onStart-!bt.isBluetoothEnabled");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                Logger("onStart-!bt.isServiceAvailable");
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER); //DEVICE_ANDROID는 안드로이드 기기 끼리
            }
        }
        if(BT_Default !="") {
            Logger("onStart:" + BT_Default);
            bt.autoConnect(BT_Default);
            timer4config();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detailmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_ble){
            Logger("btnConnect");
            if (bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
                bt.disconnect();
            }
            Intent intent = new Intent(getApplicationContext(), BLDeviceListActivity.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }
        if (id == R.id.menu_data){
            if (getWhatKindOfNetwork(MainActivity.this).equalsIgnoreCase(NONE_STATE)){
                Toast.makeText(getApplicationContext(), "네트웍에 연결되지 않았습니다", Toast.LENGTH_LONG).show();
                Logger("네트웍에 연결되지 않았습니다");
            }else {
                Logger("Raw Data 검색");
                Intent intent = new Intent(this, DataPSListActivity.class);
                this.startActivity(intent);
            }
        }
        if (id == R.id.menu_event){
            if (getWhatKindOfNetwork(MainActivity.this).equalsIgnoreCase(NONE_STATE)){
                Toast.makeText(getApplicationContext(), "네트웍에 연결되지 않았습니다", Toast.LENGTH_LONG).show();
                Logger("네트웍에 연결되지 않았습니다");
            }else {
                Logger("Raw Data 검색");
                Intent intent = new Intent(this, MessageActivity.class);
                this.startActivity(intent);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    void timer4config(){
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if(getSupportActionBar().getTitle().toString() !=bt.getConnectedDeviceName() && timerCount<10) {
                    timerCount++;
                    Read_Config("TESTMODE");
                } else {
                    timer.cancel();//타이머 종료
                }
            }
        };

        //타이머를 실행
        timer.schedule(timerTask, 0, 3000); //Timer 실행
    }

    void IP_Config_Change() {
        if(listViewI.getVisibility() == View.VISIBLE) {
            if(IP_fixed) {
                listViewP.setVisibility(View.VISIBLE);
            } else {
                listViewP.setVisibility(View.GONE);
            }
        }
        Logger("IP_adapter :" + IP_fixed + ":" + listViewP.getVisibility() );
    }

    void Alert_Mini(int pos){
        if(bt.getServiceState() != 3) return;
        final HashMap<String, String> hashMap;
        hashMap = PS_settingVlaue.get(pos);
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.channel_onoff);
        dialog.setTitle("Channel On/Off");
        final TextView newCH = dialog.findViewById(R.id.channelName);
        newCH.setText(hashMap.get("set00"));
        final Switch newOnOff = (Switch) dialog.findViewById(R.id.switch1);
        final String channelStr = newCH.getText().toString().substring(1) + ",";

        newOnOff.setChecked(PS_ReadingVlaue.get(pos).get("key01").contains("On")==true);
        newOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                String RSset="";
                if(b==true){
                    RSset = "1";
                }else {
                    RSset = "0";
                }
                BT_Send("[BTcomm]SV:"+channelStr+RSset);
                PS_settingVlaue.get(pos).put("set01",onoff[Integer.parseInt(RSset)]);
                PS_adapter.notifyDataSetChanged();
            }
        });
        dialog.show();
    }

    void Alert_Popup(ConfigList_Adapter aAdapter, ArrayList<Config_Data> aData, int position){
        if(bt.getServiceState() != 3) return;
        Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
        save_data.setInx(aData.get(position).getInx());
        save_data.setGrp(aData.get(position).getGrp());
        save_data.setType(aData.get(position).getType());
        save_data.setValue(aData.get(position).getValue());
        save_data.setLabel(aData.get(position).getLabel());
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_dialog, null);
        alert.setTitle(aData.get(position).getLabel());
        EditText input = (EditText) layout.findViewById(R.id.addboxdialog);
        input.setText(aData.get(position).getValue());
        input.setSelection(0, input.getText().length() );
        int aType=aData.get(position).getType();
        int aInx=aData.get(position).getInx();
        if(aType==2 ) {
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            String[] Wloc = {"유동 IP - 사무실","고정 IP - 현장"};
            alert.setSingleChoiceItems(Wloc,Integer.valueOf(save_data.getValue()), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    input.setText(String.valueOf(i));
                    save_data.setValue(String.valueOf(i));
                    IP_fixed = i==1;
                    IP_Config_Change();
                    Logger("setSingleChoiceItems - "+save_data.getValue());
                }
            });

        }else{
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            alert.setView(layout);
            input.setSelection(input.getText().length());
        }
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString(); //value.toString();
                save_data.setValue(value);
                aData.set(position,save_data);
                aAdapter.notifyDataSetChanged();
                fabMain.setVisibility(View.VISIBLE);
                BT_Send("[BTcomm]EE:" + (save_data.getGrp()) + (save_data.getInx()) +":"+save_data.getValue());
                Toast.makeText(getApplicationContext(), "하단 저장버튼을 실행해야 적용됩니다", Toast.LENGTH_SHORT).show();
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { // Canceled.
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
        });
        AlertDialog dialog = alert.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alert.show();
    }

    void Alert_WiFi(ConfigList_Adapter aAdapter, ArrayList<Config_Data> aData, int position){

        Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
        save_data.setInx(aData.get(position).getInx());
        save_data.setGrp(aData.get(position).getGrp());
        save_data.setType(aData.get(position).getType());
        save_data.setValue(aData.get(position).getValue());
        save_data.setLabel(aData.get(position).getLabel());
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.wifi_list, null);
        alert.setTitle(aData.get(position).getLabel());

        EditText input = (EditText) layout.findViewById(R.id.wifi_ssid);
        ListView lv = (ListView) layout.findViewById(R.id.wifilist);
        Button reScan = (Button) layout.findViewById(R.id.scan);

        input.setText(getWiFiSSID(this));

        reScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh ();
            }
        });
        this.WiFi_adapter =  new SimpleAdapter(this,
                AP_list,
                            android.R.layout.simple_list_item_2,
                            new String[] {"ssid","level"},
                            new int[]{android.R.id.text1,android.R.id.text2});
        lv.setAdapter(this.WiFi_adapter);
        //refresh ();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                input.setText(AP_list.get(i).get("ssid"));
            }
        });

        alert.setView(layout);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString(); //value.toString();
                save_data.setValue(value);
                aData.set(position,save_data);
                aAdapter.notifyDataSetChanged();
                fabMain.setVisibility(View.VISIBLE);
                BT_Send("[BTcomm]EE:" + save_data.getGrp() + save_data.getInx() +":"+save_data.getValue());
                Toast.makeText(getApplicationContext(), "하단 저장버튼을 실행해야 적용됩니다", Toast.LENGTH_SHORT).show();
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { // Canceled.

            }
        });
        AlertDialog dialog = alert.create();
        alert.show();
    }

    void channelDialog(final int inx) {
        final HashMap<String, String> hashMap;
        hashMap = PS_settingVlaue.get(inx);
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.channel_set);
        dialog.setTitle("채널 설정");
        final EditText newVolt = (EditText) dialog.findViewById(R.id.channelVolt);
        final EditText newAmp = (EditText) dialog.findViewById(R.id.channelAmp);
        final EditText newOff = (EditText) dialog.findViewById(R.id.offTime);
        final EditText newOn = (EditText) dialog.findViewById(R.id.onTime);
        final TextView newCH = dialog.findViewById(R.id.channelName);
        newCH.setText(hashMap.get("set00"));
        newVolt.setText(hashMap.get("set02"));
        newAmp.setText(hashMap.get("set03"));
        if(newVolt.getText().toString().equalsIgnoreCase("0")) newAmp.setText("1");
        newOff.setText(hashMap.get("set04"));
        newOn.setText(hashMap.get("set05"));
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        newVolt.setSelection(0, newVolt.getText().length() );
        newAmp.setSelection(0, newAmp.getText().length() );
        newOn.setSelection(0, newOn.getText().length() );
        newOff.setSelection(0, newOn.getText().length() );
        final String channelStr = newCH.getText().toString().substring(1) + ",";
        dialog.findViewById(R.id.channelSet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String RSset="";
                if(isNumeric(newVolt.getText().toString()) &&
                        isNumeric(newVolt.getText().toString()) &&
                        isNumeric(newAmp.getText().toString()) &&
                        isNumeric(newOn.getText().toString()) &&
                        isNumeric(newOff.getText().toString())  ) {
                    if (Integer.parseInt(newOn.getText().toString()) > 0) {
                        if (newVolt.getText().toString() == "") newVolt.setText("0");
                        if (newAmp.getText().toString() == "") newAmp.setText("0");
                        if (newOn.getText().toString() == "") newOn.setText("0");
                        if (newOff.getText().toString() == "") newOff.setText("0");
                        RSset = channelStr + "1,";
                        RSset += (newVolt.getText().toString()) + ",";
                        RSset += (newAmp.getText().toString()) + ",";
                        RSset += (newOff.getText().toString()) + ",";
                        RSset += (newOn.getText().toString()) + ",";
                        if(Integer.parseInt(newOff.getText().toString())>0) {
                            RSset += "2";
                        }else{
                            RSset += "1";
                        }

                    } else {
                        RSset = channelStr + "0,0,0,0.0";
                    }
                    BT_Send("[BTcomm]SV:" + RSset);
                    dialog.dismiss();
                } else {
                    Toast.makeText(getApplicationContext()
                            , "숫자만 입력해 주세요"
                            , Toast.LENGTH_SHORT).show();
                }
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            }
        });
        dialog.show();
    }

    public String getWiFiSSID(Context mContext)
    {
        WifiManager manager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        String sSSID = wifiInfo.getSSID();
        return sSSID.replaceAll("\"", "");
    }

    public static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    void BT_Send(String sendMsg){
        if(bt.isServiceAvailable() ) {
            String cBtName=String.valueOf(bt.getServiceState());
            Logger("bt.getServiceState="+cBtName);
            if(bt.getServiceState() != 3) return;
            Logger("BT_Send:" + sendMsg);
            bt.send(sendMsg, true);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    View.OnClickListener setControler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String[] getConfig={"SETVALUE","CONFIG_V","CONFIG_W","CONFIG_I"};
            listViewP.setVisibility(View.GONE);
            for(int j = 0; j < 4; j++){
                if(view.getId() == btnSetCtl[j].getId()) {
                    lstSetCtl[j].setVisibility(View.VISIBLE);
                    btnSetCtl[j].setBackground(BtnColor(true));
                    btnSetCtl[j].setTextColor(BtnTextColor(true));
                    Read_Config(getConfig[j]);
                } else {
                    lstSetCtl[j].setVisibility(View.GONE);
                    btnSetCtl[j].setBackground(BtnColor(false));
                    btnSetCtl[j].setTextColor(BtnTextColor(false));
                }
            }
        }
    };

    Drawable BtnColor(boolean visible){
        Drawable d;
        if(visible) {
            d=ContextCompat.getDrawable(this,R.drawable.button_select);
        } else {
            d=ContextCompat.getDrawable(this,R.drawable.button_deselect);
        }
        return d;
    }

    int BtnTextColor(boolean visible){
        int c;
        if(visible) {
            c = getResources().getColor(R.color.textColor);
        } else {
            c=getResources().getColor(R.color.white);
        }
        return c;
    }

    //Property 파일 쓰기
    public void WriteToProperty(String key, String value){
        //property 파일
        File file = new File(Environment.getDataDirectory()+"/data/"+getPackageName(), "PropTest.properties");

        if(value=="") return;
        FileOutputStream fos = null;
        try{
            //property 파일이 없으면 생성
            if(!file.exists()){
                file.createNewFile();
            }

            fos = new FileOutputStream(file);

            //Property 데이터 저장
            Properties props = new Properties();
            props.setProperty(key , value);   //(key , value) 로 저장
            props.store(fos, "Property Test");
            Log.d("prop", "write success");
        }catch (NullPointerException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //Property 파일 읽기
    public String ReadToProperty(String key){
        //property 파일
        File file = new File(Environment.getDataDirectory()+"/data/"+getPackageName(), "PropTest.properties");

        if(!file.exists()){ return ""; }
        FileInputStream fis = null;
        String data = "";
        try{
            fis = new FileInputStream(file);
            //Property 데이터 읽기
            Properties props = new Properties();
            props.load(fis);
            data = props.getProperty(key, "");  //(key , default value)

            Log.d("prop", "read success");
        }catch(IOException e){
            e.printStackTrace();
        }
        return data;
    }

    void BT_Change() {
        WriteToProperty("BT_Default", BT_New);
        Logger("BT_Default :  " + BT_Default + "=>" + BT_New);
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        startActivity(mainIntent);
        System.exit(0);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger("onActivityResult");
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            Logger("onActivityResult - REQUEST_CONNECT_DEVICE");
            if (resultCode == Activity.RESULT_OK) {
                Logger( "onActivityResult - RESULT_OK");
                BT_New = data.getExtras().getString("device_address");
                BT_Change();
            } else {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
            }
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            Logger("onActivityResult - REQUEST_ENABLE_BT");
            if (resultCode == Activity.RESULT_OK) {
                Logger("onActivityResult - RESULT_OK");
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
//                setup();
            } else {
                Logger("REQUEST_DISABLE_BT");
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
//                finish();
            }
        } else {
//            finish();
        }
    }


}