package com.gbhong.powersupplycontrol;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.ChartTouchListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DetailActivity extends AppCompatActivity {
    private final String TAG = "Raw Data";
    String mJsonString;
    String TAG_JSON="PS_Data";
    String[] TagName = {"DateTime", "channel", "sample", "mAmp_min", "mAmp_avg", "mAmp_max"};
    int[] readText = {0,0,0,0,0} ;
    float[] readValue = {0,0,0} ;
    ArrayList<HashMap<String, String>> mArrayList;
    private LineChart chart;

    private SimpleDateFormat timeformat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private SimpleDateFormat fileformat = new SimpleDateFormat("yyMMddhhmmss");
    ArrayList<DetailDataObject> dataItem=null;
    String urlList = "";
    private CustomProgressDialog customProgressDialog;


    String testTitle, testDesc, testPS, testCH, testTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rawdata);

        customProgressDialog = new CustomProgressDialog(DetailActivity.this);
        customProgressDialog .getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        Intent intent = getIntent();
        testTitle = intent.getStringExtra("Title");
        testDesc = intent.getStringExtra("Description");
        testPS = intent.getStringExtra("PS_ID");
        testCH = intent.getStringExtra("channel");
        testTime = intent.getStringExtra("TestTime");

        Log.d(TAG, testTitle + ":"+testDesc);

        Button file2xls = (Button) findViewById(R.id.button_to_xls) ;
        file2xls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileOut();
            }
        });


        getSupportActionBar().setTitle("측정데이터");
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        dialog = new ProgressDialog(DetailActivity.this);
//        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        dialog.setMessage("데이터를 확인하는 중입니다.");
//        dialog.show();

        String url_name = getResources().getString(R.string.url_root) + getResources().getString(R.string.php_datalist);

        GetData task = new GetData();
        task.execute(url_name, "power", "&PS_TTL=" + testTitle + "&PS_ID=" + testPS + "&PS_CHN="+testCH.substring(testCH.length()-1) + "&PS_DES="+testDesc);

        //        jsonData = (JsonOkhttpTask) new JsonOkhttpTask().execute(urlList);

        chart = (LineChart) findViewById(R.id.chart);
        chartSetting();

    }

    private final int[] colors = new int[] {
            Color.rgb(255, 208, 140),
            Color.rgb(255, 140, 157),
            Color.rgb(192, 255, 140),
            Color.rgb(140, 234, 255)
    };

    private final String[] dataName = new String[] {
            "min","avg","max"
    };

    void chartSetting() {
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setLabelCount(10);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.WHITE);
        //    xAxis.setLabelCount(2000);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(40f);
        leftAxis.setTextColor(Color.WHITE);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
        Legend legend = chart.getLegend();
        legend.setTextColor(Color.WHITE);
        legend.setTextSize(15);
        legend.setXEntrySpace(15);

        MyMarkerView marker = new MyMarkerView(this,R.layout.markerviewtext);
        marker.setChartView(chart);
        chart.setMarker(marker);


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
//        chart.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View view, boolean b) {
//                if(b==false) chart.setScaleY(1);
//            }
//        });
        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false);

        LineData data = new LineData();
        chart.setData(data);
        chart.invalidate();
    }

    private void addEntry() {
        LineData data = chart.getData();
        ILineDataSet[] dataset = {data.getDataSetByIndex(0),data.getDataSetByIndex(1),data.getDataSetByIndex(2)};
        if (data != null) {
            for(int z=0; z<3; z++) {
                if (dataset[z] == null) {
                    dataset[z] = createSet(z);
                    data.addDataSet(dataset[z]);
                }
                data.addEntry(new Entry(dataset[z].getEntryCount(), readValue[z]), z);
            }
            data.notifyDataChanged();

            chart.notifyDataSetChanged();
            chart.setVisibleXRangeMaximum(data.getEntryCount()+5);
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
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
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


    void fileOut() {
        FileOutputStream fos = null;
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File savefile = new File(dir,"PS_"+fileTime(testTime)+".csv");

        try {

            fos = new FileOutputStream(savefile);
            PrintWriter out = new PrintWriter(fos);

            String str = "DateTime,channel,sample,min,avg,max";
            out.println(str);
            if(mArrayList.size()>0) {
                for (int cnt = 0; cnt < mArrayList.size(); cnt++) {
                    str = mArrayList.get(cnt).get(TagName[0]);
                    str += "," + mArrayList.get(cnt).get(TagName[1]);
                    str += "," + mArrayList.get(cnt).get(TagName[2]);
                    str += "," + mArrayList.get(cnt).get(TagName[3]);
                    str += "," + mArrayList.get(cnt).get(TagName[4]);
                    str += "," + mArrayList.get(cnt).get(TagName[5]);
                    out.println(str);
                }
            }
            out.close();
            fos.close();
            Toast.makeText(this, "File saved:"+savefile.toString(), Toast.LENGTH_LONG).show();
            Log.d(TAG,"File save: " + savefile.toString());

        } catch (FileNotFoundException fnfe) {
            Toast.makeText(this, "File Not Found 2", Toast.LENGTH_LONG).show();
            return;
        } catch (Exception e) {
            Toast.makeText(this, "error", Toast.LENGTH_LONG).show();
        }


    }

    String fileTime(String testTime){
        String strR="";
        try {
            Date testDate = timeformat.parse(testTime);
            strR=fileformat.format(testDate);
        } catch (ParseException e) {
            strR=testTime.replace("-","").replace(":","").replace(" ","");
        }
        return strR;
    }


    private class GetData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(DetailActivity.this,
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
            String postParameters = "table=" + params[1] + params[2];
            Log.d(TAG, "posting  - " + postParameters);

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

            mArrayList = new ArrayList<HashMap<String, String>>();
            if (jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject item = jsonArray.getJSONObject(i);
                    HashMap<String, String> hashMap = new HashMap<String, String>();
                    for (int j = 0; j < TagName.length; j++) {
                        hashMap.put(TagName[j], item.getString(TagName[j]));
                    }
                    for (int j = 3; j < TagName.length; j++) {
                        readValue[j-3] = Float.parseFloat(item.getString(TagName[j]));
                    }
                    addEntry();
                    mArrayList.add(hashMap);
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
//            Log.d(TAG, "showResult : " + mJsonString);
        }

    }
}
