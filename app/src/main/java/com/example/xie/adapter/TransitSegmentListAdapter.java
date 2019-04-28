package com.example.xie.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.busline.BusLineResult;
import com.baidu.mapapi.search.busline.BusLineSearch;
import com.baidu.mapapi.search.busline.BusLineSearchOption;
import com.baidu.mapapi.search.busline.OnGetBusLineSearchResultListener;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.example.xie.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 公交路线详情适配器
 */
public class TransitSegmentListAdapter extends BaseAdapter {
    private Context mContext;
    private List<TransitRouteLine.TransitStep> transitStepList = new ArrayList<TransitRouteLine.TransitStep>();
    private String mCurrentCityName;
    private OnClickNaviListener listener;

    public TransitSegmentListAdapter(Context context, List<TransitRouteLine.TransitStep> list,String cityName) {
        this.mContext = context;
        transitStepList = list;
        mCurrentCityName = cityName;
    }

    public void setListener(OnClickNaviListener listener) {
        this.listener = listener;
    }
    @Override
    public int getCount() {
        return transitStepList.size();
    }

    @Override
    public Object getItem(int position) {
        return transitStepList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.item_transit_segment, null);
            holder.parent = (RelativeLayout) convertView
                    .findViewById(R.id.transit_item);
            holder.transitLineName = (TextView) convertView
                    .findViewById(R.id.transit_line_name);
            holder.transitDirIcon = (ImageView) convertView
                    .findViewById(R.id.transit_dir_icon);
            holder.transitStationNum = (TextView) convertView
                    .findViewById(R.id.transit_station_num);
            holder.transitExpandImage = (ImageView) convertView
                    .findViewById(R.id.transit_expand_image);
            holder.transitDirUp = (ImageView) convertView
                    .findViewById(R.id.transit_dir_icon_up);
            holder.transitDirDown = (ImageView) convertView
                    .findViewById(R.id.transit_dir_icon_down);
            holder.expandContent = (LinearLayout) convertView
                    .findViewById(R.id.expand_content);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final TransitRouteLine.TransitStep item = transitStepList.get(position);
        if (position == 0) {
            holder.transitDirIcon.setImageResource(R.mipmap.dir_start);
            holder.transitLineName.setText("出发");
            holder.transitDirUp.setVisibility(View.INVISIBLE);
            holder.transitDirDown.setVisibility(View.VISIBLE);
            holder.transitStationNum.setVisibility(View.GONE);
            holder.transitExpandImage.setVisibility(View.GONE);
            return convertView;
        } else if (position == transitStepList.size() - 1) {
            holder.transitDirIcon.setImageResource(R.mipmap.dir_end);
            holder.transitLineName.setText("到达终点");
            holder.transitDirUp.setVisibility(View.VISIBLE);
            holder.transitDirDown.setVisibility(View.INVISIBLE);
            holder.transitStationNum.setVisibility(View.INVISIBLE);
            holder.transitExpandImage.setVisibility(View.INVISIBLE);
            return convertView;
        } else {
            if (item.getStepType() == TransitRouteLine.TransitStep.TransitRouteStepType.WAKLING && item.getDistance() > 0) {
                holder.transitDirIcon.setImageResource(R.mipmap.dir13);
                holder.transitDirUp.setVisibility(View.VISIBLE);
                holder.transitDirDown.setVisibility(View.VISIBLE);
                holder.transitLineName.setText("步行" + (int) item.getDistance() + "米");
                holder.transitStationNum.setVisibility(View.VISIBLE);
                holder.transitStationNum.setText("导航");
                holder.transitStationNum.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (listener != null) {
                            listener.onNaviClick(item.getExit().getLocation());
                        }
                    }
                });
                holder.transitExpandImage.setImageResource(R.mipmap.arrow_navi);
                holder.transitExpandImage.setVisibility(View.VISIBLE);
                return convertView;
            } else if (item.getStepType() == TransitRouteLine.TransitStep.TransitRouteStepType.SUBWAY) {
                holder.transitDirIcon.setImageResource(R.mipmap.dir16);
                holder.transitDirUp.setVisibility(View.VISIBLE);
                holder.transitDirDown.setVisibility(View.VISIBLE);
                holder.transitLineName.setText(item.getVehicleInfo().getTitle());
                holder.transitStationNum.setVisibility(View.VISIBLE);
                holder.transitStationNum.setText((item.getVehicleInfo().getPassStationNum() + 1) + "站");
                holder.transitExpandImage.setVisibility(View.VISIBLE);
                ArrowClick arrowClick = new ArrowClick(holder, item);
                holder.parent.setTag(position);
                holder.parent.setOnClickListener(arrowClick);
                return convertView;
            } else if (item.getStepType() == TransitRouteLine.TransitStep.TransitRouteStepType.BUSLINE) {
                holder.transitDirIcon.setImageResource(R.mipmap.dir14);
                holder.transitDirUp.setVisibility(View.VISIBLE);
                holder.transitDirDown.setVisibility(View.VISIBLE);
                holder.transitLineName.setText(item.getVehicleInfo().getTitle());
                holder.transitStationNum.setVisibility(View.VISIBLE);
                holder.transitStationNum
                        .setText((item.getVehicleInfo().getPassStationNum() + 1) + "站");
                holder.transitExpandImage.setVisibility(View.VISIBLE);
                ArrowClick arrowClick = new ArrowClick(holder, item);
                holder.parent.setTag(position);
                holder.parent.setOnClickListener(arrowClick);
                return convertView;
            }
        }
        return convertView;
    }

    private class ViewHolder {
        public RelativeLayout parent;
        TextView transitLineName;
        ImageView transitDirIcon;
        TextView transitStationNum;
        ImageView transitExpandImage;
        ImageView transitDirUp;
        ImageView transitDirDown;
        LinearLayout expandContent;
        boolean arrowExpend = false;
    }

    private class ArrowClick implements View.OnClickListener, OnGetBusLineSearchResultListener {
        private ViewHolder mHolder;
        private TransitRouteLine.TransitStep mItem;
        BusLineSearch mBusSearch;
        String startUid;
        String endUid;

        public ArrowClick(final ViewHolder holder, final TransitRouteLine.TransitStep item) {
            startUid = item.getEntrance().getUid();
            endUid = item.getExit().getUid();
            mHolder = holder;
            mItem = item;
            mBusSearch = BusLineSearch.newInstance();
            // 设置公交线路搜索监听
            mBusSearch.setOnGetBusLineSearchResultListener(this);
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            int position = Integer.parseInt(String.valueOf(v.getTag()));
            mItem = transitStepList.get(position);
            if (mHolder.arrowExpend == false) {
                mHolder.arrowExpend = true;
                mHolder.transitExpandImage
                        .setImageResource(R.mipmap.moreitems_arrow);
                // 发起公交线路搜索
                mBusSearch.searchBusLine(new BusLineSearchOption()
                        .city(mCurrentCityName)
                        .uid(mItem.getVehicleInfo().getUid()));
            } else {
                mHolder.arrowExpend = false;
                mHolder.transitExpandImage
                        .setImageResource(R.mipmap.moreitems_arrow_down);
                mHolder.expandContent.removeAllViews();
            }
        }

        @Override
        public void onGetBusLineResult(BusLineResult busLineResult) {
            if (busLineResult == null || busLineResult.error != SearchResult.ERRORNO.NO_ERROR) {
                return;
            }
            boolean flag = false;
            /*
            由于uid搜索的是一整条公交线路，将需要的线路从起点和终点显示
             */
            for (BusLineResult.BusStation busStation : busLineResult.getStations()) {
                if (busStation.getUid().equals(startUid)) {
                    flag = true;
                }
                if (flag) {
                    LinearLayout ll = (LinearLayout) View.inflate(mContext,
                            R.layout.item_bus_segment_ex, null);
                    TextView tv = (TextView) ll
                            .findViewById(R.id.bus_line_station_name);
                    tv.setText(busStation.getTitle());
                    mHolder.expandContent.addView(ll);
                }
                if (busStation.getUid().equals(endUid)) {
                    flag = false;
                }
            }
        }
    }
    // 公交线路中的步行导航
    public interface OnClickNaviListener {
        void onNaviClick(LatLng endPoint);
    }
}
