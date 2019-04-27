package com.example.xie.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.example.xie.R;
import com.example.xie.ui.OfflineActivity;
import com.example.xie.util.MapUtil;

import java.util.ArrayList;
import java.util.HashMap;
/*
 城市列表适配器
 */

public class CityExpandableListAdapter extends BaseExpandableListAdapter {
    private OfflineActivity context;  //上下文
    private ArrayList<MKOLSearchRecord> records;  //省级列表
    private HashMap<String, Boolean> hashMap;    //下载状况

    public CityExpandableListAdapter(OfflineActivity context, ArrayList<MKOLSearchRecord> records, HashMap<String, Boolean> hashMap)
    {
        this.context = context;
        this.records = records;
        this.hashMap = hashMap;
    }

    @Override
    public int getGroupCount() {
        return records.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (records.get(groupPosition).childCities != null)
        {
            return records.get(groupPosition).childCities.size();
        }
        return 0;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return records.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if (records.get(groupPosition).childCities != null)
        {
            return records.get(groupPosition).childCities.get(childPosition);
        }
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupHolder groupHolder = null;
        if (convertView == null)
        {
            convertView = LayoutInflater.from(context).inflate(R.layout.expandlist_group, parent, false);
            groupHolder = new GroupHolder();
            groupHolder.txt = (TextView)convertView.findViewById(R.id.names);
            groupHolder.size = (TextView)convertView.findViewById(R.id.map_size);
            groupHolder.img = (ImageView)convertView.findViewById(R.id.indicator_arrow);
            convertView.setTag(groupHolder);
        }
        else
        {
            groupHolder = (GroupHolder)convertView.getTag();
        }
        String cityName = records.get(groupPosition).cityName;
        groupHolder.txt.setText(cityName);
        // 判断省级行政单位是否有子城市
        if (records.get(groupPosition).childCities == null)
        {
            groupHolder.img.setVisibility(View.GONE);
            groupHolder.size.setVisibility(View.VISIBLE);
            //判断下载状况
            if (hashMap.get(cityName))
            {
                groupHolder.size.setText("已下载");
            }else {
                if (!"0".equals(context.downLoadingList.get(cityName)))
                {
                    groupHolder.size.setText("下载中");
                }else {
                    groupHolder.size.setText(MapUtil.formatDataSize(records.get(groupPosition).dataSize));
                }
            }
        }else {
            groupHolder.size.setVisibility(View.GONE);
            groupHolder.img.setVisibility(View.VISIBLE);
        }

        //判断isExpanded就可以控制是按下还是关闭，同时更换图片
        if(isExpanded){
            groupHolder.img.setBackgroundResource(R.mipmap.moreitems_arrow);
        }else{
            groupHolder.img.setBackgroundResource(R.mipmap.moreitems_arrow_down); }
        return convertView;
    }

    // 显示子listview，省内城市列表
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ItemHolder itemHolder = null;
        if (convertView == null)
        {
            convertView = LayoutInflater.from(context).inflate(R.layout.expandlist_item, parent,false);
            itemHolder = new ItemHolder();
            itemHolder.txt = (TextView)convertView.findViewById(R.id.child_names);
            itemHolder.size  = (TextView)convertView.findViewById(R.id.child_size);
            convertView.setTag(itemHolder);
        }
        else
        {
            itemHolder = (ItemHolder)convertView.getTag();
        }
        // 判断下载状况
        if (records.get(groupPosition).childCities != null)
        {
            ArrayList<MKOLSearchRecord> list = records.get(groupPosition).childCities;
            if (list.size()>0)
            {
                MKOLSearchRecord info = list.get(childPosition);
                String cityName  = info.cityName;
                itemHolder.txt.setText(cityName);
                if (hashMap.get(cityName))
                {
                    itemHolder.size.setText("已下载");
                }else {
                    if (!"0".equals(context.downLoadingList.get(cityName)))
                    {
                        itemHolder.size.setText("下载中");
                    }else {
                        itemHolder.size.setText(MapUtil.formatDataSize(info.dataSize));
                    }
                }
            }
        }
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    class GroupHolder
    {
        public TextView txt;
        public TextView size;
        public ImageView img;

    }
    class ItemHolder
    {
        public ImageView img;
        public TextView size;
        public TextView txt;
    }
}
