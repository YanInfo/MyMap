package com.example.xie.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.example.xie.R;

import java.util.ArrayList;

public class LoadedMapAdapter extends BaseAdapter {
    private ArrayList<MKOLUpdateElement> loadedList = new ArrayList<MKOLUpdateElement>();  //已下载列表
    private LayoutInflater mInflater;
    private Context mContext;
    private LoadedMapAdapter.OnClickLoadedListener listener;
    public LoadedMapAdapter(Context context, ArrayList<MKOLUpdateElement> data) {
        mContext=context;
        loadedList = data;
        mInflater = LayoutInflater.from(context);
    }
    // 设置回调监听
    public void setListener(LoadedMapAdapter.OnClickLoadedListener listener) {
        this.listener = listener;
    }
    @Override
    public int getCount() {
        return loadedList.size();
    }

    @Override
    public Object getItem(int position) {
        return loadedList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final MKOLUpdateElement e = (MKOLUpdateElement) getItem(position);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_loaded, null);
        }
        Button remove = (Button) convertView.findViewById(R.id.remove);
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView update = (TextView) convertView.findViewById(R.id.update);
        //            TextView ratio = (TextView) view.findViewById(R.id.ratio);
        Button doUpdate = (Button) convertView.findViewById(R.id.exe_update);
        //            ratio.setText(e.ratio + "%");
        title.setText(e.cityName);
        // 判断是否可更新
        if (e.update) {
            update.setText("可更新");
        } else {
            update.setText("最新");
        }
        // 删除回调
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (listener != null) {
                    listener.onRemoveClick(e, false);
                }
            }
        });
        // 更新回调
        doUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onDoUpdateClick(e);
                }

            }
        });
        return convertView;
    }
    //回调接口
    public interface OnClickLoadedListener {
        void onRemoveClick(MKOLUpdateElement element, boolean flag);
        void onDoUpdateClick(MKOLUpdateElement element);
    }
}
