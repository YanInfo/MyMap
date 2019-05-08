package com.example.xie.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.internal.NavigationMenu;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;


import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.example.xie.R;
import com.example.xie.util.MapUtil;
import com.example.xie.listener.MyOrientationListener;
import com.mancj.materialsearchbar.MaterialSearchBar;

import io.github.yavski.fabspeeddial.FabSpeedDial;

/**
 * 初始页面
 */
public class IndexActivity extends AppCompatActivity implements View.OnClickListener, FabSpeedDial.MenuListener, BaiduMap.OnMapClickListener, BaiduMap.OnMapLongClickListener, BaiduMap.OnMapTouchListener {

    public static final int DURATION_MS = 1000;
    public static final int BAIDU_READ_PHONE_STATE = 100;
    public static final int HIDE_UI = 200;
    public static final int APEAR_UI = 201;
    public static final int SHOW_ADDRESS = 300;
    public static final int LOCATION_NOMAL = 0;
    public static final int LOCATION_COMPASS_MODE_OFF = 1;
    public static final int LOCATION_COMPASS_MODE_ON = 2;
    private MapView mMapView = null;
    private FabSpeedDial fabSD_map_mode;
    private FloatingActionButton fab_check;
    private FloatingActionButton fab_location;
    private FloatingActionButton fab_to_there;
    private TextView textView_marker_information;
    private TextView textView_marker_name;
    private RelativeLayout layout_information;
    private boolean trafficEnabled = false;
    private boolean isHided = false;
    private BaiduMap mBaiduMap;
    private UiSettings mUiSettings;
    private LocationClient mLocationClient;
    private MyOrientationListener myOrientationListener;
    private float mCurrentX;
    private LatLng mStartPoint;
    private LatLng mEndPoint;
    private String PoiName;
    private String distance;
    private int locationState = 1;

    private Toast toast;
    private MyLocationListener mListener;
    private MapStatusUpdate msu;
    private MapStatus mMapStatus;
    private MapStatusUpdate mMapStatusUpdate;
    private MaterialSearchBar mSearchBar;
    private Marker marker;
    private Intent intent;

    private double mLatitude;
    private double mLongitude;
    private String mCurrentCityName;
    private BitmapDescriptor mIconNavigation;
    private BitmapDescriptor mIconMarker;

    private String addressInformation;
    private String myLocationInformation;

    private MyLocationConfiguration.LocationMode locationMode;
    MyLocationConfiguration configuration;
    public Handler handler = new Handler() {
        @SuppressLint("RestrictedApi")
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case HIDE_UI:
                    fab_check.setVisibility(View.GONE);
                    fab_location.setVisibility(View.GONE);
                    fabSD_map_mode.setVisibility(View.GONE);
                    mSearchBar.setVisibility(View.GONE);
                    mMapView.showZoomControls(false);
                    mBaiduMap.setCompassEnable(false);
                    break;
                case APEAR_UI:
                    fab_check.setVisibility(View.VISIBLE);
                    fab_location.setVisibility(View.VISIBLE);
                    fabSD_map_mode.setVisibility(View.VISIBLE);
                    mSearchBar.setVisibility(View.VISIBLE);
                    mMapView.showZoomControls(true);
                    mBaiduMap.setCompassEnable(true);
                    break;
                case SHOW_ADDRESS:
                    if (PoiName.isEmpty()) {
                        textView_marker_name.setText(addressInformation);
                        textView_marker_information.setText(distance);
                    } else {
                        textView_marker_name.setText(PoiName);
                        textView_marker_information.setText(distance + " " + addressInformation);
                    }
                    layout_information.setVisibility(View.VISIBLE);
                    fab_to_there.setVisibility(View.VISIBLE);
                    mMapView.setScaleControlPosition(new Point(20, 1580));
                    break;
                default:
            }
        }
    };
    private GeoCoder mCoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        mMapView = findViewById(R.id.bmapView);
        fab_location = findViewById(R.id.fab_location);
        fabSD_map_mode = findViewById(R.id.fabSD_map_mode);
        fab_check = findViewById(R.id.fab_check);
        mSearchBar = findViewById(R.id.search_bar);
        layout_information = findViewById(R.id.layout_information);
        textView_marker_information = findViewById(R.id.textview_marker_information);
        textView_marker_name = findViewById(R.id.textview_marker_name);
        fab_to_there = findViewById(R.id.fab_to_there);

    }

    private void initData() {
        //初始化图标,BitmapDescriptorFactory是bitmap 描述信息工厂类.
        mIconNavigation = BitmapDescriptorFactory.fromResource(R.mipmap.navigation);
        mIconMarker = BitmapDescriptorFactory.fromResource(R.mipmap.marker);
        locationMode = MyLocationConfiguration.LocationMode.NORMAL;
        toast = Toast.makeText(getApplicationContext(), null, Toast.LENGTH_SHORT);
        mBaiduMap = mMapView.getMap();
        configuration = new MyLocationConfiguration(locationMode, true, mIconNavigation);
        //设置定位图层配置信息，只有先允许定位图层后设置定位图层配置信息才会生效，参见 setMyLocationEnabled(boolean)
        mBaiduMap.setMyLocationConfiguration(configuration);
        // 设置指南针图标和位置
        android.graphics.Point point = new android.graphics.Point(100, 360);
        mMapView.getMap().setCompassPosition(point);
        mBaiduMap.setCompassIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.map_compass));
        // 隐藏logo
        View child = mMapView.getChildAt(1);
        if (child != null && (child instanceof ImageView || child instanceof ZoomControls)) {
            child.setVisibility(View.INVISIBLE);
        }
        // 设置缩放控件的位置
        mMapView.getMap().setOnMapLoadedCallback(new BaiduMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                mMapView.setZoomControlsPosition(new Point(960, 700));
                mMapView.setScaleControlPosition(new Point(20, 1720));
            }
        });
        // 设置地图缩放级别
        msu = MapStatusUpdateFactory.zoomTo(18.0f);
        mBaiduMap.setMapStatus(msu);
        mUiSettings = mBaiduMap.getUiSettings();
        mUiSettings.setScrollGesturesEnabled(true);
        mCoder = GeoCoder.newInstance();
    }

    /**
     * 初始化定位和相关监听事件
     */
    private void initEvent() {
        if (Build.VERSION.SDK_INT >= 23) {
            initLocation();
        } else {
            initLocation();
        }
        fabSD_map_mode.setMenuListener(this);
        fab_check.setOnClickListener(this);
        fab_location.setOnClickListener(this);
        mBaiduMap.setOnMapClickListener(this);
        mBaiduMap.setOnMapLongClickListener(this);
        mBaiduMap.setOnMapTouchListener(this);
        mSearchBar.setOnClickListener(this);
        fab_to_there.setOnClickListener(this);
        mCoder.setOnGetGeoCodeResultListener(mCoderListener);
    }

    /**
     * 启动定位sdk
     */
    @Override
    protected void onStart() {
        super.onStart();
        mBaiduMap.setMyLocationEnabled(true);
        if (!mLocationClient.isStarted()) {
            mLocationClient.start();
        }
        myOrientationListener.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        myOrientationListener.stop();
    }

    @Override
    protected void onDestroy() {
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        mCoder.destroy();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_to_there:
                intent = new Intent(IndexActivity.this, RoutePlanActivity.class);
                intent.putExtra("start_point", mStartPoint);
                intent.putExtra("end_point", mEndPoint);
                intent.putExtra("start_name", myLocationInformation);
                intent.putExtra("end_name", addressInformation);
                intent.putExtra("city_name", mCurrentCityName);
                startActivity(intent);
                overridePendingTransition(0, 0);
                break;
            case R.id.fab_location:
                if (locationState == LOCATION_NOMAL) {
                    getMyLocation();
                    locationState = LOCATION_COMPASS_MODE_OFF;
                    fab_location.setImageResource(R.mipmap.compass_mode_off);
                } else {
                    changeCompassMode();
                }
                break;
            case R.id.fab_check:
                changeTraffic();
                break;
            case R.id.search_bar:
                intent = new Intent(IndexActivity.this, SearchActivity.class);
                intent.putExtra("start_name", myLocationInformation);
                intent.putExtra("start_point", mStartPoint);
                intent.putExtra("city_name", mCurrentCityName);
                startActivity(intent);
                break;
            case R.id.layout_information:
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(mEndPoint);
                mBaiduMap.animateMapStatus(msu, DURATION_MS);
                msu = MapStatusUpdateFactory.zoomTo(18.0f);
                mBaiduMap.animateMapStatus(msu, DURATION_MS);
                break;
            default:
        }
    }

    /**
     * 开启实时定位，显示方向
     */
    private void changeCompassMode() {
        if (null == mMapView) {
            return;
        }
        if (locationState == LOCATION_COMPASS_MODE_OFF) {
            locationMode = MyLocationConfiguration.LocationMode.COMPASS;
            configuration = new MyLocationConfiguration(locationMode, true, mIconNavigation);
            //设置定位图层配置信息，只有先允许定位图层后设置定位图层配置信息才会生效，参见 setMyLocationEnabled(boolean)
            mBaiduMap.setMyLocationConfiguration(configuration);
            locationState = LOCATION_COMPASS_MODE_ON;
            fab_location.setImageResource(R.mipmap.compass_mode_on);
        } else {
            locationMode = MyLocationConfiguration.LocationMode.NORMAL;
            configuration = new MyLocationConfiguration(locationMode, true, mIconNavigation);
            //设置定位图层配置信息，只有先允许定位图层后设置定位图层配置信息才会生效，参见 setMyLocationEnabled(boolean)
            mBaiduMap.setMyLocationConfiguration(configuration);
            mMapStatus = new MapStatus.Builder().rotate(0).overlook(0).build();
            mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
            mBaiduMap.animateMapStatus(mMapStatusUpdate, DURATION_MS);
            locationState = LOCATION_COMPASS_MODE_OFF;
            fab_location.setImageResource(R.mipmap.compass_mode_off);
        }
    }

    /**
     * 实时路况
     */
    private void changeTraffic() {
        if (null == mMapView) {
            return;
        }
        if (trafficEnabled) {
            trafficEnabled = false;

            fab_check.setImageResource(R.mipmap.no_checking);
            fab_check.show();
            //关闭交通图
            mBaiduMap.setTrafficEnabled(false);
            toast.setText(getResources().getString(R.string.closeTraffic));
        } else {
            trafficEnabled = true;
            fab_check.setImageResource(R.mipmap.checking);
            //开启交通图
            mBaiduMap.setTrafficEnabled(true);
            mBaiduMap.setCustomTrafficColor(getResources().getString(R.string.severeCongestion), getResources().getString(R.string.congestion),
                    getResources().getString(R.string.slow), getResources().getString(R.string.smooth));
            //  对地图状态做更新，否则可能不会触发渲染，造成样式定义无法立即生效。
            /*MapStatusUpdate u = MapStatusUpdateFactory.zoomTo(13);
            mBaiduMap.animateMapStatus(u);*/
            toast.setText(getResources().getString(R.string.openTraffic));
        }
        toast.show();
    }


    @Override
    public boolean onPrepareMenu(NavigationMenu navigationMenu) {
        return true;
    }

    /**
     * 切换地图类型
     * @param menuItem
     * @return
     */
    @Override
    public boolean onMenuItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.map_standard:
                // 将地图修改为普通样式
                if (locationState != LOCATION_COMPASS_MODE_ON) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                    mMapStatus = new MapStatus.Builder().overlook(0).build();
                    mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                    mBaiduMap.animateMapStatus(mMapStatusUpdate, DURATION_MS);
                }
                mBaiduMap.setBuildingsEnabled(false);
                break;
            case R.id.map_satellite:
                // 将地图修改为卫星图样式
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.map_3d:
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                mMapStatus = new MapStatus.Builder().overlook(-45).build();
                mBaiduMap.setBuildingsEnabled(true);
                mMapStatusUpdate = MapStatusUpdateFactory.newMapStatus(mMapStatus);
                mBaiduMap.animateMapStatus(mMapStatusUpdate, DURATION_MS);
                break;
            default:
        }
        return true;
    }

    @Override
    public void onMenuClosed() {

    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onMapClick(LatLng latLng) {
        if (marker != null) {
            marker.remove();
            marker = null;
            layout_information.setVisibility(View.GONE);
            fab_to_there.setVisibility(View.GONE);
            mMapView.setScaleControlPosition(new Point(20, 1720));
        } else if (isHided) {
            isHided = false;
            handler.sendEmptyMessage(APEAR_UI);
        } else {
            isHided = true;
            handler.sendEmptyMessage(HIDE_UI);
        }
    }

    @Override
    public boolean onMapPoiClick(MapPoi mapPoi) {
        PoiName = mapPoi.getName();
        textView_marker_information.setText(getResources().getString(R.string.no_address));
        handler.sendEmptyMessage(APEAR_UI);
        isHided = false;
        mEndPoint = new LatLng(mapPoi.getPosition().latitude, mapPoi.getPosition().longitude);
        distance = MapUtil.getDistance(mEndPoint, new LatLng(mLatitude, mLongitude));
        if (marker != null) {
            marker.remove();
        }
        OverlayOptions options = new MarkerOptions().position(mEndPoint).icon(mIconMarker);
        // 在地图上添加Marker，并显示
        marker = (Marker) (mBaiduMap.addOverlay(options));

        mCoder.reverseGeoCode(new ReverseGeoCodeOption()
                .location(mEndPoint)
                // POI召回半径，允许设置区间为0-1000米，超过1000米按1000米召回。默认值为1000
                .radius(500));
        return true;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        PoiName = "";
        mEndPoint = latLng;
        textView_marker_information.setText(getResources().getString(R.string.no_address));
        handler.sendEmptyMessage(APEAR_UI);
        isHided = false;
        distance = MapUtil.getDistance(mEndPoint, new LatLng(mLatitude, mLongitude));
        if (marker != null) {
            marker.remove();
        }

        OverlayOptions options = new MarkerOptions().position(mEndPoint).icon(mIconMarker);
        // 在地图上添加Marker，并显示
        // mBaiduMap.addOverlay(options);
        marker = (Marker) (mBaiduMap.addOverlay(options));
        // textView_information.setText();
        // 设置额外的信息
        // 发起反地理编码请求(经纬度->地址信息)
        mCoder.reverseGeoCode(new ReverseGeoCodeOption()
                .location(mEndPoint)
                // POI召回半径，允许设置区间为0-1000米，超过1000米按1000米召回。默认值为1000
                .radius(500));
    }

    @Override
    public void onTouch(MotionEvent motionEvent) {
        locationState = LOCATION_NOMAL;
        locationMode = MyLocationConfiguration.LocationMode.NORMAL;
        configuration = new MyLocationConfiguration(locationMode, true, mIconNavigation);
        //设置定位图层配置信息，只有先允许定位图层后设置定位图层配置信息才会生效，参见 setMyLocationEnabled(boolean)
        mBaiduMap.setMyLocationConfiguration(configuration);
        fab_location.setImageResource(R.mipmap.mylocation);

    }

    /**
     * 实现定位监听接口
     */
    public class MyLocationListener extends BDAbstractLocationListener {
        private boolean isFirstIn = true;

        // 定位请求回调函数,这里面会得到定位信息
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            mLatitude = bdLocation.getLatitude();
            mLongitude = bdLocation.getLongitude();
            mStartPoint = new LatLng(mLatitude, mLongitude);
            mCurrentCityName = bdLocation.getCity();
            //BDLocation 回调的百度坐标类，内部封装了如经纬度、半径等属性信息
            //MyLocationData 定位数据,定位数据建造器
            if (bdLocation == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    // 设定图标方向
                    .direction(mCurrentX)
                    // getRadius 获取定位精度,默认值0.0f
                    .accuracy(bdLocation.getRadius())
                    // 百度纬度坐标
                    .latitude(mLatitude)
                    // 百度经度坐标
                    .longitude(mLongitude)
                    .build();
            mBaiduMap.setMyLocationData(locData);
            // 配置定位图层显示方式,三个参数的构造器
            /**
             * 1.定位图层显示模式
             * 2.是否允许显示方向信息
             * 3.用户自定义定位图标
             * */
            //判断是否为第一次定位,是的话需要定位到用户当前位置
            if (isFirstIn) {
                //地理坐标基本数据结构
                LatLng latLng = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
                //描述地图状态将要发生的变化,通过当前经纬度来使地图显示到该位置
                MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(latLng);
                //改变地图状态
                mBaiduMap.setMapStatus(msu);
                isFirstIn = false;
                if (bdLocation.getAddrStr() == null) {
                    toast.setText(getResources().getString(R.string.no_network));
                } else {
                    myLocationInformation = bdLocation.getAddrStr();
                    toast.setText(getResources().getString(R.string.current_location) + myLocationInformation);
                    Log.d("+++++++", bdLocation.getAddrStr());
                }
                toast.show();
            }
        }
    }


    /**
     * 定位方法和配置
     */
    private void initLocation() {
        //定位服务的客户端。宿主程序在客户端声明此类，并调用，目前只支持在主线程中启动
        mLocationClient = new LocationClient(this);
        mListener = new MyLocationListener();
        //注册监听器
        mLocationClient.registerLocationListener(mListener);
        //配置定位SDK各配置参数，比如定位模式、定位时间间隔、坐标系类型等
        LocationClientOption mOption = new LocationClientOption();
        //设置坐标类型
        mOption.setCoorType("bd09ll");
        //设置是否需要地址信息，默认为无地址
        mOption.setIsNeedAddress(true);
        //设置是否打开gps进行定位
        mOption.setOpenGps(true);
        //设置扫描间隔，单位是毫秒，当<1000(1s)时，定时定位无效
        int span = 1000;
        mOption.setScanSpan(span);
        //设置 LocationClientOption
        mLocationClient.setLocOption(mOption);
        myOrientationListener = new MyOrientationListener(getApplicationContext());
        //通过接口回调来实现实时方向的改变
        myOrientationListener.setOnOrientationListener(new MyOrientationListener.OnOrientationListener() {
            @Override
            public void onOrientationChanged(float x) {
                mCurrentX = x;
            }
        });
    }

    public void getMyLocation() {

        MapStatusUpdate msu = MapStatusUpdateFactory.newLatLng(mStartPoint);
        mBaiduMap.animateMapStatus(msu, DURATION_MS);
        msu = MapStatusUpdateFactory.zoomTo(18.0f);
        mBaiduMap.animateMapStatus(msu, DURATION_MS);
    }

    OnGetGeoCoderResultListener mCoderListener = new OnGetGeoCoderResultListener() {
        @Override
        public void onGetGeoCodeResult(GeoCodeResult geoCodeResult) {
        }

        @Override
        public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
            if (reverseGeoCodeResult == null || reverseGeoCodeResult.error != SearchResult.ERRORNO.NO_ERROR) {
                //没有找到检索结果
                return;
            } else {
                //详细地址
                addressInformation = reverseGeoCodeResult.getAddress();
                handler.sendEmptyMessage(SHOW_ADDRESS);
                //行政区号
                int adCode = reverseGeoCodeResult.getCityCode();
            }
        }
    };
}
