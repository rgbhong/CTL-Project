package com.gbhong.TempHumiMonitor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public static final String WIFE_STATE = "WIFE";
    public static final String MOBILE_STATE = "MOBILE";
    public static final String NONE_STATE = "NONE";
    String TAG = "drybox Main";
    String BT_Default = "DryBox 온습도";

    private BluetoothSPP bt;
    TextView mDumpTextView, DBoxName, DBoxStep  ;
    ToggleButton tglTHP , tglPost;
    Switch swP , swT;
    ScrollView mScrollView;
    LinearLayout LayoutData, LayoutSetting;
    ActionBar actionbar;
    Context context;
    Timer timer;

    public float ScreenWidth, ScreenHeight;
    int[] readText = {0,0,0,0,0,0} ;
    int timerCount=0;
    float[] readValue = {0,0,0,0,0} ;
    String[] ConfigValue = {"", "", "", ""} ;
    String[] ConfigDbox = {"",""} ;
    String[] ConfigWifi = {"","","",""} ;
    String[] ConfigIP = {"","","","","","",""} ;

    private Button[] btnSetCtl = null;
    private ListView[] lstSetCtl = null;
    private LineChart chart;
    private Thread thread;

    ListView listViewV, listViewD, listViewW, listViewI0, listViewI1;
    ConfigList_Adapter Val_adapter, Dbox_adapter, Wifi_adapter, IP_adapter0,IP_adapter1;
    SimpleAdapter WiFi_adapter=null;
    ArrayList<HashMap<String, String>> PS_ReadingVlaue=null, PS_settingVlaue=null, wifi_list=null;

    WifiManager wifiManager ;
    ConnectivityManager connManager ;
    private List<ScanResult> scanResult;

    ArrayList<Config_Data> Val_Data = new ArrayList<>();
    ArrayList<Config_Data> Dbox_Data = new ArrayList<>();
    ArrayList<Config_Data> Wifi_Data = new ArrayList<>();
    ArrayList<Config_Data> IP_Data0 = new ArrayList<>();
    ArrayList<Config_Data> IP_Data1 = new ArrayList<>();

    private boolean fabMain_status = false, chartFocus=false, IP_fixed;
    private FloatingActionButton fabMain;
    GestureDetector mGestureDetector;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        Logger("onCreate");

        getSupportActionBar().setTitle("블루투스 연결중...");
        progressBar = (ProgressBar) findViewById(R.id.progressbar);

        swP = (Switch) findViewById(R.id.switchP);
        swP.setChecked(false); //초기 토글 버튼 체크 상태 지정
        swT = (Switch) findViewById(R.id.switchT);
        swT.setChecked(false); //초기 토글 버튼 체크 상태 지정
        DBoxName = (TextView)findViewById(R.id.textDry0);
        DBoxStep = (TextView)findViewById(R.id.textDry1);

        mDumpTextView = (TextView) findViewById(R.id.consoleText);

        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        LayoutData=findViewById((R.id.layoutD));
        LayoutSetting=findViewById((R.id.layoutS));

        readText[0]=(R.id.textDataTime);
        readText[1]=(R.id.textDataTemp1);
        readText[2]=(R.id.textDataTemp2);
        readText[3]=(R.id.textDataHumi1);
        readText[4]=(R.id.textDataHumi2);
        readText[5]=(R.id.textPressure);

        for(int i=0; i<ConfigValue.length; i++){
            Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
            Val_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
        }
        for(int i=0; i<ConfigDbox.length; i++){
            Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
            Dbox_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
        }
        for(int i=0; i<ConfigWifi.length; i++){
            Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
            Wifi_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
        }
        for(int i=0; i<4; i++){
            Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
            IP_Data0.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
        }
        for(int i=4; i<ConfigIP.length; i++){
            Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
            IP_Data1.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
        }

        Bluetooth_Initial();
        Buttons_Initial();
        Listview_Setting();
        Swife_setting();

        chart = (LineChart) findViewById(R.id.chart);
        chartSetting();

        wifi_list = new ArrayList< HashMap<String,String> >();
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE); //인스턴스 get
        connManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE); //인스턴스 get
        NetworkInfo wifiInfo =
                connManager.getNetworkInfo( ConnectivityManager.TYPE_WIFI); //wifi 타입의 정보를 얻오온다.

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
                Logger("DataReceived"+message);
                mDumpTextView.append(message+"\n");
                mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
                recieveDataDisplay(message);
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() { //연결됐을 때
            public void onDeviceConnected(String name, String address) {
                Logger("BluetoothConnection : "+name);
                Toast.makeText(getApplicationContext()
                        , "Connected to " + name + "\n" + address
                        , Toast.LENGTH_SHORT).show();
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

    void Buttons_Initial() {

        swP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sndMsg="PostMode ";
                if(swP.isChecked() == true){ //TODO 토글 버튼이 체크된경우 (ON)
                    sndMsg += "1";
                } else { //TODO 토글 버튼이 체크되지 않은 경우 (OFF)
                    sndMsg += "0";
                }
                BT_Send("[BTcomm]"+sndMsg);
            }
        });

        swT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sndMsg="TH_Mode ";
                if(swT.isChecked() == true){ //TODO 토글 버튼이 체크된경우 (ON)
                    sndMsg += "1";
                } else { //TODO 토글 버튼이 체크되지 않은 경우 (OFF)
                    sndMsg += "0";
                }
                BT_Send("[BTcomm]"+sndMsg);
            }
        });

        DBoxName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Alert_Mini(DBoxName.getText().toString(),0);
            }
        });

        DBoxStep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Alert_Mini(DBoxStep.getText().toString(),1);
            }
        });

        int[] btnID = {R.id.button0, R.id.button1, R.id.btn_chart, R.id.button3};
        int[] lstID = {R.id.ListValue, R.id.ListDbox, R.id.ListWifi, R.id.ListIP0};
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
                BT_Send("[BTcomm]SaveConfig");
                fabMain_status = false;
                fabMain.setVisibility(View.INVISIBLE);
            }
        });

    }

    void Listview_Setting() {
        listViewV = findViewById(R.id.ListValue);
        Val_adapter = new ConfigList_Adapter(this, Val_Data); //어댑터 클래스 객체 생성
        listViewV.setAdapter(Val_adapter); //리스트튜에 어댑터 장착
        listViewV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Alert_Popup(Val_adapter, Val_Data, position);
            }
        });

        listViewD = findViewById(R.id.ListDbox);
        Dbox_adapter = new ConfigList_Adapter(this, Dbox_Data); //어댑터 클래스 객체 생성
        listViewD.setAdapter(Dbox_adapter); //리스트튜에 어댑터 장착
        listViewD.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    Alert_Popup(Dbox_adapter, Dbox_Data, position);

            }
        });

        listViewW = findViewById(R.id.ListWifi);
        Wifi_adapter = new ConfigList_Adapter(this, Wifi_Data); //어댑터 클래스 객체 생성
        listViewW.setAdapter(Wifi_adapter); //리스트튜에 어댑터 장착
        listViewW.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Toast.makeText(getApplicationContext(), Wifi_Data.get(position).getName() + "\n" + Wifi_Data.get(position).getValue(), Toast.LENGTH_SHORT).show();

            }
        });

        listViewI0 = findViewById(R.id.ListIP0);
        IP_adapter0 = new ConfigList_Adapter(this, IP_Data0); //어댑터 클래스 객체 생성
        listViewI0.setAdapter(IP_adapter0); //리스트튜에 어댑터 장착
        listViewI0.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position==0){
                    Alert_WiFi(IP_adapter0, IP_Data0, position);
                } else {
                    Alert_Popup(IP_adapter0, IP_Data0, position);
                }
            }
        });
        listViewI1 = findViewById(R.id.ListIP1);
        IP_adapter1 = new ConfigList_Adapter(this, IP_Data1); //어댑터 클래스 객체 생성
        listViewI1.setAdapter(IP_adapter1); //리스트튜에 어댑터 장착
        listViewI1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Alert_Popup(IP_adapter1, IP_Data1, position);
            }
        });
    }

    void Swife_setting() {
        getDisplayMetrics();
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {
                if((e1.getY() > ScreenHeight * 0.5) && (Math.abs(e2.getX()-e1.getX()) > 2 * Math.abs(e2.getY()-e1.getY()))) {
                    if(e1.getX() > ScreenWidth * 0.75 && velocityX < -200 ) {
                        Logger("Swife_setting - right");
                        Read_Config();
                        LayoutSetting.setVisibility(View.VISIBLE);
                        LayoutData.setVisibility(View.GONE);
                    }
                    if(e1.getX() < ScreenWidth * 0.25 && velocityX > 200 ) {
                        Logger("Swife_setting - left");
                        LayoutData.setVisibility(View.VISIBLE);
                        LayoutSetting.setVisibility(View.GONE);
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
        if (rData.contains("[DATA]")) {
            String[] result = rData.split(",");
            TextView newData = (TextView) findViewById(readText[0]);
            newData.setText(result[1]);
            for (int i = 2; i < result.length; i++) {
                if (i < 7) {
                    newData = (TextView) findViewById(readText[i - 1]);
                    newData.setText(result[i]);
                    readValue[i - 2] = Float.parseFloat(result[i]);
                }
            }
            addEntry();
        } else if (rData.contains("[READ]")) {
            String[] result = rData.split(":");
            if (result.length > 3 && Integer.valueOf(result[1]) > -1) {
                Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
                save_data.setLabel(result[0].replace("[READ]", "")); //set 메소드로 데이터 설정
                save_data.setInx(result[1]);
                save_data.setName(result[2]);
                save_data.setType(0);
                save_data.setValue(result[3]);
                final int grp = Integer.valueOf(result[1].substring(0, 1));
                final int inx = Integer.valueOf(result[1].substring(1, 2));
                //               Logger("[READ]"+save_data.getName()+":"+save_data.getValue()+":"+save_data.getLabel());
                if (grp == 0) {
                    ConfigValue[inx] = result[3];
                    if (inx < 2) save_data.setType(1);
                    if (null == Val_Data.get(inx)) {
                        Val_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    } else {
                        Val_Data.set(inx, save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    }
                    Val_adapter.notifyDataSetChanged(); //리스트뷰 갱신
                } else if (grp == 1) {
                    ConfigDbox[inx] = result[3];
                    if (null == Dbox_Data.get(inx)) {
                        Dbox_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    } else {
                        Dbox_Data.set(inx, save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    }
                    Dbox_adapter.notifyDataSetChanged(); //리스트뷰 갱신
                } else if (grp == 2) {
                    ConfigWifi[inx] = result[3];
                    if (null == Wifi_Data.get(inx)) {
                        Wifi_Data.add(save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    } else {
                        Wifi_Data.set(inx, save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                    }
                    Wifi_adapter.notifyDataSetChanged(); //리스트뷰 갱신
                } else if (grp == 3) {
                    ConfigIP[inx] = result[3];
                    if (inx < 4) {
                        if (inx == 0) save_data.setType(3);
                        if (inx == 1) save_data.setType(2);
                        IP_Data0.set(inx, save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                        if (inx == 1) {
                            IP_fixed = save_data.getValue().equalsIgnoreCase("1");
                            IP_Config_Change();
                        }
                        IP_adapter0.notifyDataSetChanged(); //리스트뷰 갱신
                    } else {
                        IP_Data1.set(inx-4, save_data); //설정한 데이터를 아까 위에서 list를 ArrayList로 선언하였던 변수에 add 해준다.
                        IP_adapter1.notifyDataSetChanged(); //리스트뷰 갱신
                    }
                    Logger("[READ]" + result[1] + " " + save_data.getName() + ":" + save_data.getValue() + ":" + save_data.getLabel());
                }
            }
        } else if (rData.contains("[MINI]")) {
            getSupportActionBar().setTitle(bt.getConnectedDeviceName());
            String[] result = rData.split(":");
            if (result.length == 5 ) {
                swP.setChecked(result[1].contains("1"));
                swT.setChecked(result[2].contains("1"));
                DBoxName.setText(result[3]);
                DBoxStep.setText(result[4]);
                Logger("저장/센싱/dryBox/Step:" + result[1] + ":" + result[2] + ":" + result[3] + ":" + result[4]);
            }
        } else if (rData.startsWith("[SAVED]")) {
            fabMain.setVisibility(View.INVISIBLE);
        } else if (rData.startsWith("[Config]WIFINUM")) {
//            IP_Config_Change();
        }
        progressBar.setVisibility(View.INVISIBLE);
    }

    public void onDestroy() {
        Logger("onDestroy");
        bt.stopAutoConnect();
        bt.disconnect();
        bt.stopService(); //블루투스 중지

        super.onDestroy();
    }

    void Read_Config(){
        ConfigValue[0]="";
        BT_Send("[BTcomm]currconfig");
        Logger("Request Config");
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
        bt.autoConnect(BT_Default);
        timer4config();
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
                Intent intent = new Intent(this, DryboxListActivity.class);
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
                if(ConfigValue[0] == "" && timerCount<10) {
                    timerCount++;
                    Read_Config();
                } else {
                    timer.cancel();//타이머 종료
                }
            }
        };

        //타이머를 실행
        timer.schedule(timerTask, 0, 3000); //Timer 실행
    }

    void IP_Config_Change() {
        if(listViewI0.getVisibility() == View.VISIBLE) {
            if(IP_fixed) {
                listViewI1.setVisibility(View.VISIBLE);
            } else {
                listViewI1.setVisibility(View.GONE);
            }
        }
        Logger("IP_adapter :" + IP_fixed + ":" + listViewI1.getVisibility() );
    }

    void Alert_Mini(String oldVal, int pos){
        if(bt.getServiceState() != 3) return;
        String[] tTitle = {"이름","상태"};
        String[] tName = {"DryBox","opStep"};
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_dialog, null);
        alert.setTitle("DryBox " + tTitle[pos]);
        EditText input = (EditText) layout.findViewById(R.id.addboxdialog);
        input.setText(oldVal);
            input.setSelection(input.getText().length());
            alert.setView(layout);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString(); //value.toString();
                BT_Send("[BTcomm]" + tName[pos]+" "+value);
                BT_Send("[BTcomm]miniconfig");
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { // Canceled.
            }
        });
        AlertDialog dialog = alert.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alert.show();
    }

    void Alert_Popup(ConfigList_Adapter aAdapter, ArrayList<Config_Data> aData, int position){
        if(bt.getServiceState() != 3) return;
        Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
        save_data.setName(aData.get(position).getName());
        save_data.setInx(aData.get(position).getInx());
        save_data.setType(aData.get(position).getType());
        save_data.setValue(aData.get(position).getValue());
        save_data.setLabel(aData.get(position).getLabel());
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_dialog, null);
        alert.setTitle(aData.get(position).getLabel());
        EditText input = (EditText) layout.findViewById(R.id.addboxdialog);
        input.setText(aData.get(position).getValue());
        int aType=aData.get(position).getType();
        Logger("Alert_Popup - "+ position + " - " + aType + " : " + save_data.getValue());
        switch (aType){
            case 1: //on,off
                String[] Wsel = {"Off","On"};
                alert.setSingleChoiceItems(Wsel,Integer.valueOf(save_data.getValue()), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        input.setText(String.valueOf(i));
                        save_data.setValue(String.valueOf(i));
                        Logger("setSingleChoiceItems - "+save_data.getValue());
                    }
                });
                break;
            case 2:
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
                break;
            default: //text
                alert.setView(layout);
                input.setSelection(input.getText().length());
                Logger("type x - "+save_data.getValue());
                break;

        }
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString(); //value.toString();
                if(aType==0) save_data.setValue(value);
                aData.set(position,save_data);
                aAdapter.notifyDataSetChanged();
                int aInx = Integer.valueOf(aData.get(position).getInx());
                if(aType!=1) {
                    fabMain_status = true;
                    fabMain.setVisibility(View.VISIBLE);
                    Toast.makeText(getApplicationContext(), "하단 저장버튼을 실행해야 적용됩니다", Toast.LENGTH_LONG).show();
                }
                BT_Send("[BTcomm]" + save_data.getName()+" "+save_data.getValue());
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) { // Canceled.
            }
        });
        AlertDialog dialog = alert.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alert.show();
    }

    void Alert_WiFi(ConfigList_Adapter aAdapter, ArrayList<Config_Data> aData, int position){

        Config_Data save_data = new Config_Data(); //list 클래스는 아이템 항목 클래스이다.
        save_data.setInx(aData.get(position).getInx());
        save_data.setType(aData.get(position).getType());
        save_data.setName(aData.get(position).getName());
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
                wifi_list,
                android.R.layout.simple_list_item_2,
                new String[] {"ssid","level"},
                new int[]{android.R.id.text1, android.R.id.text2});
        lv.setAdapter(this.WiFi_adapter);
        refresh ();

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                input.setText(wifi_list.get(i).get("ssid"));
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
                BT_Send("[BTcomm]" + save_data.getName()+" "+save_data.getValue());
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


    private void refresh() {
        scanResult = wifiManager.getScanResults();
        wifi_list.clear();
        for(ScanResult result: scanResult) {
            HashMap<String,String> item = new HashMap<String,String>();
            item.put("ssid",result.SSID);
            item.put("level"," (" + result.frequency + "kHz,"  + result.level + ")" );
            wifi_list.add(item);
        }
        WiFi_adapter.notifyDataSetChanged();
    }

    public String getWiFiSSID(Context mContext)
    {
        WifiManager manager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        String sSSID = wifiInfo.getSSID();
        return sSSID.replaceAll("\"", "");
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
            listViewI1.setVisibility(View.GONE);
            for(int j = 0; j < 4; j++){
                if(view.getId() == btnSetCtl[j].getId()) {
                    lstSetCtl[j].setVisibility(View.VISIBLE);
                    btnSetCtl[j].setBackground(BtnColor(true));
                    btnSetCtl[j].setTextColor(BtnTextColor(true));
                    BT_Send("[BTcomm]config-" + j);
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

    private final int[] colors = new int[] {
            Color.rgb(255, 208, 140),
            Color.rgb(255, 140, 157),
            Color.rgb(192, 255, 140),
            Color.rgb(140, 234, 255),
            Color.rgb(255, 102, 255)
    };

    private final String[] dataName = new String[] {
            "온도1","온도2","습도1","습도2","압력"
    };

    void chartSetting() {
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setLabelCount(10);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(60f);
        leftAxis.setTextColor(Color.WHITE);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setAxisMinimum(98f);
        rightAxis.setAxisMaximum(120f);
        rightAxis.setTextColor(Color.WHITE);

        Legend legend = chart.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setTextSize(15);
        legend.setXEntrySpace(6);


        // enable touch gestures
        chart.setTouchEnabled(true);

        // enable scaling and dragging
        chart.setScaleEnabled(true);
        chart.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                chart.setScaleY(1);
                chart.setScaleX(1);
                return false;
            }
        });
        chart.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b==false) chart.setScaleY(1);
            }
        });
        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false);

        LineData data = new LineData();
        chart.setData(data);
        chart.invalidate();
    }

    private void addEntry() {
        LineData data = chart.getData();
        ILineDataSet[] dataset = {data.getDataSetByIndex(0),data.getDataSetByIndex(1),data.getDataSetByIndex(2),data.getDataSetByIndex(3),data.getDataSetByIndex(4)};
        if (data != null) {
            for(int z=0; z<5; z++) {
                if (dataset[z] == null) {
                    dataset[z] = createSet(z);
                    data.addDataSet(dataset[z]);
                }
                data.addEntry(new Entry(dataset[z].getEntryCount(), readValue[z]), z);
            }
            data.notifyDataChanged();

            chart.notifyDataSetChanged();
            if(dataset[0].getEntryCount()<10) {
                chart.setVisibleXRangeMaximum(6);
            } else if(dataset[0].getEntryCount()<50) {
                chart.setVisibleXRangeMaximum(46);
            } else {
                chart.setVisibleXRangeMaximum(100);
            }
            if(chart.getScaleY()==1)
            chart.moveViewToX(data.getEntryCount()+5);
        }
    }

    private LineDataSet createSet(int setInx) {

        LineDataSet set = new LineDataSet(null,  dataName[setInx]);
        set.setFillAlpha(110);
//        set.setFillColor(Color.parseColor("#d7e7fa"));
        set.setColor(colors[setInx]);
        set.setCircleColor(colors[setInx]);
        set.setCircleHoleColor(colors[setInx]);
        set.setValueTextColor(Color.GREEN);
        set.setDrawValues(false);
        set.setLineWidth(2);
        set.setCircleRadius(3);
        set.setDrawCircleHole(true);
        set.setDrawCircles(true);
        set.setValueTextSize(9f);
        set.setDrawFilled(false);
        if(setInx<4) {
            set.setAxisDependency(YAxis.AxisDependency.LEFT);
        }else{
            set.setAxisDependency(YAxis.AxisDependency.RIGHT);
        }
        set.setHighLightColor(Color.rgb(244, 117, 117));
        return set;
    }

    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {
        Log.d("Fling", "Chart fling. VelocityX: " + velocityX + ", VelocityY: " + velocityY);
    }

    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
        Log.d("Scale / Zoom", "ScaleX: " + scaleX + ", ScaleY: " + scaleY);
    }

    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        Log.d("Gesture", "START, x: " + me.getX() + ", y: " + me.getY());
    }


    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
        Log.d("Gesture", "END, lastGesture: " + lastPerformedGesture);

        // un-highlight values after the gesture is finished and no single-tap
        if(lastPerformedGesture != ChartTouchListener.ChartGesture.SINGLE_TAP)
            chart.highlightValues(null); // or highlightTouch(null) for callback to onNothingSelected(...)
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger("onActivityResult");
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            Logger("onActivityResult - REQUEST_CONNECT_DEVICE");
            if (resultCode == Activity.RESULT_OK) {
                Logger( "onActivityResult - RESULT_OK");
                BT_Default="";
                bt.stopAutoConnect();
                bt.connect(data);
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
        }
    }
}