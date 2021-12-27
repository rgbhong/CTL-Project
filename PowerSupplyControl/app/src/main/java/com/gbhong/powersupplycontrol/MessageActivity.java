package com.gbhong.powersupplycontrol;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class MessageActivity extends AppCompatActivity {
    public static final String WIFE_STATE = "WIFE";
    public static final String MOBILE_STATE = "MOBILE";
    public static final String NONE_STATE = "NONE";
    private final String TAG = "PS Alarm List";

    String TAG_JSON="PS_Alarm";
    ArrayList<HashMap<String, String>> mArrayList;
    ListView mlistView;
    SimpleAdapter adapter;
    View footer;

    String url_name , table_name , mJsonString;
    String[] TagName ;
    int[] list_tv ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_main);

        Log.d(TAG, "onCreate");
        //아이템객체를 담을 리스형 배열을 만든다. 여기에 담아서 화면에 꺼내서 출력한다.

        getSupportActionBar().setTitle("Power Supply 알람");

        url_name = getResources().getString(R.string.url_root) + getResources().getString(R.string.php_datalist);
        table_name = getResources().getString(R.string.table_alarm);
        TagName = new String[]{"PS_ID","event_type","description","DateTime"};
        list_tv = new int[]{R.id.tv_psid, R.id.tv_event, R.id.tv_desc, R.id.tv_time};

        if (getWhatKindOfNetwork(MessageActivity.this).equalsIgnoreCase("NONE")) {
            Log.d(TAG, " getWhatKindOfNetwork : " + "none");
            Toast.makeText(getApplicationContext(), "네트웍에 연결되지 않았습니다", Toast.LENGTH_LONG).show();
        } else {
            layerInitial();
            //AsyncTask 실행
            startQueryData("", "", 0);
        }
        Log.d(TAG, "onCreate");
    }

    void layerInitial(){

        mArrayList = new ArrayList<HashMap<String, String>>();
        mlistView = findViewById(R.id.message_list);

        //Adapter setting
        adapter = new SimpleAdapter(
                this, mArrayList, R.layout.alarm_row, TagName, list_tv
        );

        footer = getLayoutInflater().inflate(R.layout.message_footer, null, false);
        mlistView.addFooterView(footer);
        Button NextBtn = (Button) footer.findViewById(R.id.btn_foot);
        NextBtn.setOnClickListener(nextList);

        mlistView.setAdapter(adapter);

    }

    View.OnClickListener nextList = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            startQueryData("","",mArrayList.size());
        }
    };

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

    private void startQueryData(String sqlItem, String sqlCondition, int pageStart){
        footer.setVisibility(View.GONE);
    //    sqlItem = "&selectitem=PS_ID,event_type,description,DateTime";
    //    sqlCondition += "&PS_ID=aaa" , "PS_CHN=P1";
        Log.d(TAG," : " + url_name +" : " + table_name  +" : " + sqlCondition  +" : " + sqlItem   );
        GetData task = new GetData();
        task.execute(url_name, table_name, sqlCondition + "&page_size=8&page_start="+pageStart );
    }

    private class GetData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(MessageActivity.this,
                    "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            Log.d(TAG, "response  - " + result);
            if (result == null){
                //       mTextViewResult.setText(errorString);
            }
            else {
                mJsonString = result;
                showResult();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];
            String postParameters = "table=" + params[1]  + params[2];
//            Log.d(TAG, "posting  - " + postParameters);

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                //httpURLConnection.setRequestProperty("content-type", "application/json");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "POST response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder strbld = new StringBuilder();
                String line = null;
                while((line = bufferedReader.readLine()) != null){
                    strbld.append(line);
                }
                bufferedReader.close();
                return strbld.toString();
            } catch (Exception e) {
                Log.d(TAG, "InsertData: Error ", e);
                return new String("Error: " + e.getMessage());
            }
        }
    }

    private void showResult(){
        try {
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            Log.d(TAG, "showResult : "+jsonArray.length());
            if (jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    for (int j = 0; j < TagName.length; j++) {
                        hashMap.put(TagName[j], item.getString(TagName[j]));
                    }
                    mArrayList.add(hashMap);
                }
                //Adapter setting
                adapter.notifyDataSetChanged();
                footer.setVisibility(View.VISIBLE);
                Log.d(TAG, "showResult : "+mlistView.getCount());
            }
//            if(customProgressDialog.isShowing()) customProgressDialog.dismiss();
        } catch ( JSONException e) {
            Log.d(TAG, "show Error : ", e);
            //            Log.d(TAG, "showResult : " + mJsonString);
        }
        Toast.makeText(getApplicationContext(), "기존 데이터 확인 ", Toast.LENGTH_SHORT).show();
    }

}
