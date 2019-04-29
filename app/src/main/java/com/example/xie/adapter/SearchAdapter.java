package com.example.xie.adapter;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.baidu.mapapi.search.core.PoiInfo;
import com.example.xie.R;

/**
 * 搜索结果适配器
 */
public class SearchAdapter extends BaseAdapter<PoiInfo>{
    private Context mContext;
    private SearchAdapter.SearchItemOnClickListener mListener;

    public SearchAdapter(Context context) {
        mContext = context;
    }
    public void setOnItemClickListener(SearchAdapter.SearchItemOnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.item_search_result, parent, false);
        return new SearchViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        ((SearchViewHolder) holder).bind(getDataSet().get(position));
        // 点击item监听
        ((SearchViewHolder)holder).root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener ){
                    mListener.onClick(((SearchViewHolder)holder).root,getDataSet().get(position),position);
                }
            }
        });
    }


    static class SearchViewHolder extends RecyclerView.ViewHolder {
        CardView root;
        TextView textView_name, textView_address;

        public SearchViewHolder(View itemView) {
            super(itemView);
            textView_name = (TextView) itemView.findViewById(R.id.textView_name);
            textView_address = (TextView) itemView.findViewById(R.id.textView_address);
            root = (CardView) itemView.findViewById(R.id.root);
        }
        public void bind(PoiInfo poiInfo) {
            if (null != poiInfo) {
                textView_name.setText(poiInfo.getName());
                textView_address.setText(poiInfo.getAddress());
            }
        }
    }
    //回调接口
    public interface SearchItemOnClickListener {
        void onClick(View view, PoiInfo poiInfo, int position);
    }
}
