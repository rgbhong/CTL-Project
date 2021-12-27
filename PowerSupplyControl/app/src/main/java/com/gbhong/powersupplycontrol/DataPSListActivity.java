package com.gbhong.powersupplycontrol;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
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

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class DataPSListActivity extends AppCompatActivity {
    private final String TAG = "PS Data List Activity";

    String TAG_JSON = "PS_testlist";
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
        setContentView(R.layout.test_datalist);

        Log.d(TAG, "onCreate");
        //아이템객체를 담을 리스형 배열을 만든다. 여기에 담아서 화면에 꺼내서 출력한다.

        getSupportActionBar().setTitle("Power Supply 데이터 목록");


        url_name = getResources().getString(R.string.url_root) + getResources().getString(R.string.php_datalist);
        table_name = getResources().getString(R.string.table_list);
        TagName = new String[]{"Title", "Description", "PS_ID", "channel", "start", "end", "CNT"};
        list_tv = new int[]{R.id.tv_psid, R.id.tv_status, R.id.tv_desc, R.id.tv_CH, R.id.tv_time, R.id.tv_event, R.id.tv_CNT};

        Log.d(TAG, "onCreate");
        layerInitial();

        //AsyncTask 실행
        startQueryData("","",0);
        Log.d(TAG, "onCreate");

    }


    void layerInitial(){
        mArrayList = new ArrayList<HashMap<String, String>>();
        mlistView = findViewById(R.id.list_testdata);
        adapter = new SimpleAdapter(
                this, mArrayList, R.layout.test_list_item, TagName, list_tv
        );

        mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dataItemClick(i);
            }
        });

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

    public void dataItemClick(int position) {
        HashMap<String, String> hashMap  = mArrayList.get(position);
        //Toast.makeText(context, item.getWikipedia(), Toast.LENGTH_SHORT).show();
        //해당 주소의 웹페이지를 열도록 작업 할것.
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("Title", hashMap.get("Title"));
        intent.putExtra("Description", hashMap.get("Description"));
        intent.putExtra("PS_ID", hashMap.get("PS_ID"));
        intent.putExtra("channel", hashMap.get("channel"));
        intent.putExtra("TestTime", hashMap.get("start"));
        this.startActivity(intent);

    }


    private void startQueryData(String sqlItem, String sqlCondition, int pageStart){
        footer.setVisibility(View.VISIBLE);
        Log.d(TAG," : " + url_name +" : " + table_name  +" : " + sqlCondition  +" : " + sqlItem   );
        GetData task = new GetData();
        task.execute(url_name, table_name, sqlCondition + "&page_start="+pageStart  + "&page_size=8" );

    }

    private class GetData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(DataPSListActivity.this,
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
            Log.d(TAG, "posting  - " + postParameters);

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(120000);
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
                    hashMap.put(TagName[3], "P"+item.getString(TagName[3]));
                    hashMap.put(TagName[6], item.getString(TagName[6])+"개");
                    mArrayList.add(hashMap);
                }
                //Adapter setting
                adapter.notifyDataSetChanged();
                footer.setVisibility(View.VISIBLE);

                Log.d(TAG, "showResult : "+mlistView.getCount());
            } else {
                footer.setVisibility(View.GONE);
            }
//            if(customProgressDialog.isShowing()) customProgressDialog.dismiss();
        } catch ( JSONException e) {
            Log.d(TAG, "showResult : ", e);
    //            Log.d(TAG, "showResult : " + mJsonString);
        }
        Toast.makeText(getApplicationContext(), "기존 데이터 확인 ", Toast.LENGTH_SHORT).show();
    }

}
