package com.example.xie.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ZoomControls;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;
import com.example.xie.R;
import com.example.xie.adapter.TransitSegmentListAdapter;
import com.example.xie.routeoverlay.MyTransitRouteOverlay;
import com.example.xie.util.ChString;
import com.example.xie.util.MapUtil;
import com.example.xie.util.RevealAnimatorUtil;
import com.example.xie.util.SPUtil;

/**
 * 公交换乘路线
 */
public class TransitRouteDetailActivity extends AppCompatActivity implements BaiduMap.OnMapLoadedCallback, TransitSegmentListAdapter.OnClickNaviListener {
    TransitRouteLine transitRouteLine;
    TransitRouteResult transitRouteResult;
    MapView mMapView;
    private BaiduMap mBaiduMap;
    private UiSettings mUiSettings;
    private android.support.v7.widget.Toolbar toolbar;
    private TextView mTvRouteInfo;
    private String duration;
    private String distinct;
    private ListView mListView;
    private boolean isClicked;
    private MyTransitRouteOverlay overlay;
    private TransitSegmentListAdapter transitSegmentListAdapter;
    private String mCurrentCityName;
    private LatLng locationPoint;
    private LocationClient mLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transit_route_detail);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        toolbar = findViewById(R.id.toolbar);
        mMapView = findViewById(R.id.mapview);
        mTvRouteInfo = findViewById(R.id.tv_route_info);
        mListView = findViewById(R.id.listview);

    }

    private void initData() {
        setSupportActionBar(toolbar);
        if (null != getIntent()) {
            transitRouteLine = getIntent().getParcelableExtra("transit_route_line");
            transitRouteResult = getIntent().getParcelableExtra("transit_route_result");
            mCurrentCityName = getIntent().getStringExtra("city_name");
            locationPoint = getIntent().getParcelableExtra("start_point");
        }
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setCompassEnable(false);
        mUiSettings = mBaiduMap.getUiSettings();
        mUiSettings.setOverlookingGesturesEnabled(false);
        mUiSettings.setRotateGesturesEnabled(false);
        // 隐藏logo
        View child = mMapView.getChildAt(1);
        if (child != null && (child instanceof ImageView || child instanceof ZoomControls)) {
            child.setVisibility(View.INVISIBLE);
        }
        duration = MapUtil.getFriendlyTime((int) transitRouteLine.getDuration());
        distinct = MapUtil.getFriendlyLength((int) transitRouteLine.getDistance());
        if (null != duration && null != distinct) {
            mTvRouteInfo.setText("坐公交耗时" + duration + ",路程" + distinct);
        } else {
            mTvRouteInfo.setText("公交路线详情");
        }
        transitSegmentListAdapter = new TransitSegmentListAdapter(TransitRouteDetailActivity.this, transitRouteLine.getAllStep(), mCurrentCityName);
        mListView.setAdapter(transitSegmentListAdapter);

    }

    private void initEvent() {
        mBaiduMap.setMyLocationEnabled(true);
        initLocation();
        mBaiduMap.setOnMapLoadedCallback(this);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setOnMenuItemClickListener(new android.support.v7.widget.Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.route_line_mode && !isClicked) {
                    mListView.setVisibility(View.GONE);
                    mMapView.setVisibility(View.VISIBLE);
                    mBaiduMap.clear();
                    overlay = new MyTransitRouteOverlay(mBaiduMap);
                    overlay.setData(transitRouteLine);
                    //在地图上绘制TransitRouteOverlay
                    overlay.addToMap();
                    isClicked = true;
                } else if (isClicked) {
                    mMapView.setVisibility(View.GONE);
                    mListView.setVisibility(View.VISIBLE);
                    isClicked = false;
                }
                return true;
            }
        });
        transitSegmentListAdapter.setListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bus_detail, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onMapLoaded() {
        if (overlay != null) {
            overlay.addToMap();
            overlay.zoomToSpan();
        }
    }

    @Override
    public void onNaviClick(LatLng endPoint) {
        WalkNaviLaunchParam walkParam = new WalkNaviLaunchParam().stPt(locationPoint).endPt(endPoint);
        startWalkNavi(walkParam);
    }

    private void startWalkNavi(final WalkNaviLaunchParam walkParam) {
        Log.d("navi", "startBikeNavi");
        try {
            WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d("navi", "BikeNavi engineInitSuccess");
                    routePlanWithWalkParam(walkParam);
                }

                @Override
                public void engineInitFail() {
                    Log.d("navi", "BikeNavi engineInitFail");
                    //mBikeNaviHelper.initNaviEngine();
                }
            });
        } catch (Exception e) {
            Log.d("navi", "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    /**
     * 发起步行导航算路
     */
    private void routePlanWithWalkParam(WalkNaviLaunchParam walkParam) {
        WalkNavigateHelper.getInstance().routePlanWithParams(walkParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d("navi", "BikeNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d("navi", "BikeNavi onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(TransitRouteDetailActivity.this, WNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError walkRoutePlanError) {

            }
        });
    }

    private void initLocation() {
        //定位初始化
        mLocationClient = new LocationClient(this);
        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        //设置locationClientOption
        mLocationClient.setLocOption(option);
        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            mCurrentCityName = location.getCity();
            //locationName = location.getAddrStr();
            locationPoint = new LatLng(location.getLatitude(), location.getLongitude());
        }
    }
}