package com.example.xie.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.example.xie.R;
import com.example.xie.ui.OfflineActivity;
import com.example.xie.util.MapUtil;

import java.util.ArrayList;

public class LoadingMapAdapter extends BaseAdapter {
    private ArrayList<MKOLUpdateElement> loadingList = new ArrayList<MKOLUpdateElement>();   //正在下载列表
    private LayoutInflater mInflater;
    OfflineActivity context;
    private LoadingMapAdapter.OnClickLoadedListener listener;

    public LoadingMapAdapter(OfflineActivity context, ArrayList<MKOLUpdateElement> data) {
        this.context = context;
        loadingList = data;
        mInflater = LayoutInflater.from(context);
    }
    // 设置删除的监听回调
    public void setListener(LoadingMapAdapter.OnClickLoadedListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return loadingList.size();
    }

    @Override
    public Object getItem(int position) {
        return loadingList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_loading, null);
        }
        TextView name = (TextView) convertView.findViewById(R.id.city_name);
        TextView size = (TextView) convertView.findViewById(R.id.city_size);
        TextView ratio = (TextView) convertView.findViewById(R.id.down_ratio);
        ImageView manager = (ImageView) convertView.findViewById(R.id.down_manager);
        ImageView delete = (ImageView) convertView.findViewById(R.id.down_delete);
        final MKOLUpdateElement e = loadingList.get(position);
        name.setText(e.cityName);
        size.setText(MapUtil.formatDataSize(e.size));
        // 显示下载进度
        ratio.setText(e.ratio + "%");
        // 判断是否暂停
        if ("1".equals(context.downLoadingList.get(e.cityName))) {
            manager.setBackgroundResource(R.mipmap.loading_play);
        } else if ("2".equals(context.downLoadingList.get(e.cityName))) {
            manager.setBackgroundResource(R.mipmap.loading_pause);
        }
        // 暂停继续监听
        manager.setOnClickListener(new View.OnClickListener() {
            boolean flag = "1".equals(context.downLoadingList.get(e.cityName))?false:true;
            @Override
            public void onClick(View v) {
                if (flag) {
                    context.downLoadingList.put(e.cityName, "1");
                    OfflineActivity.mOffline.pause(e.cityID);
                    v.setBackgroundResource(R.mipmap.loading_play);
                    flag = false;
                } else {
                    context.downLoadingList.put(e.cityName, "2");
                    OfflineActivity.mOffline.start(e.cityID);
                    v.setBackgroundResource(R.mipmap.loading_pause);
                    flag = true;
                }
            }
        });
        // 删除监听
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (listener != null) {
                    listener.onRemoveClick(e, false);
                }
            }
        });
        return convertView;
    }
    // 删除监听回调接口
    public interface OnClickLoadedListener {
        void onRemoveClick(MKOLUpdateElement element, boolean flag);
        //void onDoUpdateClick(MKOLUpdateElement element);
    }
}
