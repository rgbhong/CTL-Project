package com.gbhong.TempHumiMonitor;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class CustomListView extends ArrayAdapter {

    LayoutInflater layoutInflater;
    ArrayList<DryBoxObject> data;
    Context context;

    public CustomListView(Activity context, ArrayList<DryBoxObject> data) {
        super(context, R.layout.drybox_datalist);
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.data = data;
        this.context = context;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.drybox_list_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.tvDryboxName = (TextView) convertView.findViewById(R.id.tv_name);
            viewHolder.tvDryboxStatus = (TextView) convertView.findViewById(R.id.tv_status);
            viewHolder.tvTestStart = (TextView) convertView.findViewById(R.id.tv_start);
            viewHolder.tvTestEnd = (TextView) convertView.findViewById(R.id.tv_end);
            viewHolder.tvCount = (TextView) convertView.findViewById(R.id.tv_CNT);

            convertView.setTag(viewHolder); // 반드시 setTag를 해줘야 함
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.tvDryboxName.setText(data.get(position).getDryBox());
        viewHolder.tvDryboxStatus.setText(data.get(position).getStatus());
        viewHolder.tvTestStart.setText(data.get(position).getstart());
        viewHolder.tvTestEnd.setText(data.get(position).getend());
        viewHolder.tvCount.setText("데이터 갯수:" + data.get(position).getCNT());

        return convertView;
    }

    class ViewHolder {
        public TextView tvDryboxName, tvDryboxStatus, tvTestStart, tvTestEnd, tvCount;

    }

}

