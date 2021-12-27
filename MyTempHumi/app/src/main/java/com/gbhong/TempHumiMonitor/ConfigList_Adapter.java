package com.gbhong.TempHumiMonitor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;

public class ConfigList_Adapter extends BaseAdapter {
    Context context = null;
    ArrayList<Config_Data> List_Data;

    TextView tv1, tv2;
    Switch sw2;

    ConfigList_Adapter(Context context, ArrayList<Config_Data> list){
        this.context = context;
        this.List_Data = list;
    }

    //아이템 항목(리스트뷰에 들어가는 View) 갯수 반환
    @Override
    public int getCount() {
        return List_Data.size();
    }

    //position에 해당하는 아이템 항목을 객체로 반환
    @Override
    public Object getItem(int position) {
        return List_Data.get(position);
    }

    //position에 해당하는 아이템 항목의 ID(순서) 반환
    @Override
    public long getItemId(int position) {
        return position;
    }

    public static boolean isNumericArray(String str) {
        if (str == null)
            return false;
        for (char c : str.toCharArray())
            if (c < '0' || c > '9')
                return false;
        return true;
    }
    //제일 중요한 부분
    //리스트뷰에 보여줄 View 반환한다.
    //아이템 항목을 구성할 View를 inflate해주고, 데이터를 가져와 셋팅해주고, View 반환
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.config_list, parent, false);

        tv1 = itemView.findViewById(R.id.textD1);
        tv2 = itemView.findViewById(R.id.textD2);

        tv1.setText(List_Data.get(position).getLabel());
        String dValue = List_Data.get(position).getValue();

        int inx = 0;
        if(isNumericArray(dValue)) inx=Integer.parseInt(dValue);

        switch (List_Data.get(position).getType()){
            case 1: //on,off
                String[] Wsel = {"Off","On"};
                tv2.setText(Wsel[inx]);
                break;
            case 2:
                String[] Wloc = {"유동 IP - 사무실","고정 IP - 현장"};
                tv2.setText(Wloc[inx]);
                break;
            default: //text
                tv2.setText(dValue);
                break;

        }

        return itemView;
    }
}
