package com.example.xie.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.example.xie.R;
import com.example.xie.util.MapUtil;

import java.util.List;

/**
 * 公交路线适配器
 */
public class TransitRouteAdapter extends RecyclerView.Adapter<TransitRouteAdapter.ViewHolder> {
    private List<TransitRouteLine> mTransitRouteLineList;  //公交线路列表
    private ItemBusRouteOnClickListener mListener;
    private TransitRouteResult mTransitRouteResult;
    Context mContext;

    public TransitRouteAdapter(Context context, TransitRouteResult transitRouteResult) {
        this.mTransitRouteResult = transitRouteResult;
        this.mTransitRouteLineList = transitRouteResult.getRouteLines();
        this.mContext = context;
    }

    public void setOnItemClickListener(ItemBusRouteOnClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.item_route_plan_transit, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.bindData(mTransitRouteLineList.get(position));
        // 点击item监听
        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (null != mListener) {
                    mListener.onClick(holder.root, mTransitRouteLineList.get(position), mTransitRouteResult, position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return null == mTransitRouteLineList ? 0 : mTransitRouteLineList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, des;
        RelativeLayout root;

        public ViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.transit_line_title);
            des = (TextView) itemView.findViewById(R.id.transi_line_des);
            root = (RelativeLayout) itemView.findViewById(R.id.root);
        }

        private void bindData(TransitRouteLine transitRouteLine) {
            title.setText(MapUtil.getTransitPathTitle(transitRouteLine));
            des.setText(MapUtil.getTransitDes(transitRouteLine));
        }
    }

    public interface ItemBusRouteOnClickListener {
        void onClick(View view, TransitRouteLine transitRouteLine, TransitRouteResult transitRouteResult, int position);
    }
}
