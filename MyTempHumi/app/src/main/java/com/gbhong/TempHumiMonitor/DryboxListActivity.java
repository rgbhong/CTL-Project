package com.gbhong.TempHumiMonitor;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

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
public class DryboxListActivity extends AppCompatActivity {
    private final String TAG = "DryboxListActivity";

    ArrayList<HashMap<String, String>> mArrayList;
    ListView mlistView;

    String urlList = "";
    String mJsonString;
    String[] TagName = {"DryBox", "Status", "start", "end", "CNT"};

//    JsonFromServerTask jsonFromServerTask;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drybox_datalist);

        Log.d(TAG, "onCreate");
//        customProgressDialog = new CustomProgressDialog(DryboxListActivity.this);
//        customProgressDialog .getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        Log.d(TAG, "onCreate");
        //아이템객체를 담을 리스형 배열을 만든다. 여기에 담아서 화면에 꺼내서 출력한다.
        mArrayList = new ArrayList<>(); //List와 ArrayList의 차이점은???

        getSupportActionBar().setTitle("Drybox 온습도 데이터 목록");
        //AsyncTask 실행
        Log.d(TAG, "onCreate");

        mlistView = findViewById(R.id.list_drybox);

        mlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dataItemClick(i);
            }
        });

        Log.d(TAG, "onCreate");
        urlList = getResources().getString(R.string.php_address);

        GetData task = new GetData();
        task.execute(urlList, "dryboxlist");

//    private CustomProgressDialog customProgressDialog;

//        jsonFromServerTask = (JsonFromServerTask) new JsonFromServerTask().execute(urlList);
        Log.d(TAG, "onCreate");

    }

    public void dataItemClick(int position) {
        HashMap<String, String> hashMap  = mArrayList.get(position);
        //Toast.makeText(context, item.getWikipedia(), Toast.LENGTH_SHORT).show();
        //해당 주소의 웹페이지를 열도록 작업 할것.
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("DryBox", hashMap.get("DryBox"));
        intent.putExtra("Status", hashMap.get("Status"));
        intent.putExtra("TestTime", hashMap.get("start"));
        this.startActivity(intent);

    }

    private class GetData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(DryboxListActivity.this,
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
            String postParameters = "table=" + params[1];
            Log.d(TAG, "posting  - " + postParameters);

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(120000);
                httpURLConnection.setConnectTimeout(60000);
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
            JSONArray jsonArray = jsonObject.getJSONArray("dryboxlist");

            Log.d(TAG, "showResult : "+jsonArray.length());
            mArrayList = new ArrayList<HashMap<String, String>>();
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
                ListAdapter adapter = new SimpleAdapter(
                        this, mArrayList, R.layout.drybox_list_item,
                        TagName,
                        new int[]{R.id.tv_name, R.id.tv_status, R.id.tv_start, R.id.tv_end, R.id.tv_CNT}
                );
                mlistView.setAdapter(adapter);
                Log.d(TAG, "showResult : "+mlistView.getCount());
            }
//            if(customProgressDialog.isShowing()) customProgressDialog.dismiss();
        } catch ( JSONException e) {
            Log.d(TAG, "showResult : ", e);
    //            Log.d(TAG, "showResult : " + mJsonString);
        }

    }

}
