package com.example.xie.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.example.xie.R;
import com.example.xie.ui.OfflineActivity;
import com.example.xie.util.MapUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HotcityListAdapter extends BaseAdapter {
    private List<MKOLSearchRecord> hotCityList = new ArrayList<>();  //热门城市列表
    private LayoutInflater mInflater;
    private HashMap<String, Boolean> hashMap;   //下载状态
    OfflineActivity context;

    public HotcityListAdapter(OfflineActivity context, List<MKOLSearchRecord> data, HashMap<String, Boolean> hashMap) {
        hotCityList = data;
        this.hashMap = hashMap;
        this.context = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        //return 5;
        return hotCityList.size();
    }

    @Override
    public Object getItem(int position) {
        return hotCityList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        final MKOLSearchRecord mkolSearchRecord = hotCityList.get(position);
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_hotcity, null);
            //把convertView中的控件保存到viewHolder中
            holder.textView_city_name = (TextView) convertView.findViewById(R.id.textView_city_name);
            holder.textView_city_size = (TextView) convertView.findViewById(R.id.textView_city_size);
            //通过setTag将ViewHolder与convertView绑定
            convertView.setTag(holder);
        } else {
            //通过调用缓冲视图convertView，然后就可以调用到viewHolder,viewHolder中已经绑定了各个控件，省去了findViewById的步骤
            holder = (ViewHolder) convertView.getTag();
        }
        if (hotCityList.size() > 0) {
            String cityName = mkolSearchRecord.cityName;
            holder.textView_city_name.setText(cityName);
            //判断下载状况
            if (hashMap.get(cityName)) {
                holder.textView_city_size.setText("已下载");
            } else {
                if ("0".equals(context.downLoadingList.get(cityName))) {
                    holder.textView_city_size.setText(MapUtil.formatDataSize(mkolSearchRecord.dataSize));
                } else {
                    holder.textView_city_size.setText("下载中");
                }
            }
        }
        return convertView;
    }

    static class ViewHolder {
        TextView textView_city_name;
        TextView textView_city_size;
    }
}
