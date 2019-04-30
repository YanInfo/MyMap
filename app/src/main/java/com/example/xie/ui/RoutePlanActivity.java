package com.example.xie.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.bikenavi.BikeNavigateHelper;
import com.baidu.mapapi.bikenavi.adapter.IBEngineInitListener;
import com.baidu.mapapi.bikenavi.adapter.IBRoutePlanListener;
import com.baidu.mapapi.bikenavi.model.BikeRoutePlanError;
import com.baidu.mapapi.bikenavi.params.BikeNaviLaunchParam;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.baidu.navisdk.adapter.IBNRoutePlanManager;
import com.baidu.navisdk.adapter.IBNTTSManager;
import com.baidu.navisdk.adapter.IBaiduNaviManager;
import com.baidu.tts.chainofresponsibility.logger.LoggerProxy;
import com.example.xie.R;
import com.example.xie.adapter.TransitRouteAdapter;
import com.example.xie.routeoverlay.MyDrivingRouteOverlay;
import com.example.xie.util.ChString;
import com.example.xie.util.MapUtil;
import com.example.xie.util.RevealAnimatorUtil;
import com.example.xie.util.SPUtil;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sdkdemo.NormalUtils;
import sdkdemo.newif.DemoGuideActivity;

/**
 * 导航，输入起始位置和终点位置
 */
public class RoutePlanActivity extends AppCompatActivity implements TabLayout.OnTabSelectedListener, OnGetRoutePlanResultListener, View.OnClickListener {

    public static final int DRIVE_ROUTE = 0;
    public static final int BUS_ROUTE = 1;
    // 步行和骑行，没有用到
    public static final int WALK_ROUTE = 2;
    public static final int RIDE_ROUTE = 3;
    static final String ROUTE_PLAN_NODE = "routePlanNode";
    // 导航相关
    private static final String APP_FOLDER_NAME = "掌上地图";
    private static final String[] authBaseArr = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int authBaseRequestCode = 1;
    public static RoutePlanActivity routePlanActivityInstance = null;
    TabLayout mTabLayout;
    MapView mMapView;
    TextView mFirstLine;
    TextView mSecondLine;
    ImageView img_back;
    RecyclerView recycler_bus_list;
    RelativeLayout mBottomInfo;
    RelativeLayout layout_no_information;
    MaterialEditText edit_end;
    MaterialEditText edit_start;
    ImageView img_return;
    ImageView img_setting;
    String mTag = "";
    private ProgressBar progressBar;
    private RelativeLayout mRootLayout;
    private BaiduMap mBaiduMap;
    private UiSettings mUiSettings;
    private RoutePlanSearch mSearch;
    private LocationClient mLocationClient;
    // 动画开始和结束的坐标
    private int animatorX, animatorY;
    // 揭露动画工具类
    private RevealAnimatorUtil revealAnimatorUtil;
    private String startName;
    private String endName;
    private String locationName;
    private LatLng startPoint;
    private LatLng endPoint;
    private LatLng locationPoint;
    private String mCurrentCityName;
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            if (msg.what == 0) {
                mTag = data.getString("mtag");
            } else if (msg.what == 1) {
                startName = data.getString("start_name");
                startPoint = (LatLng) data.getParcelable("start_point");
                edit_start.setText(startName, TextView.BufferType.NORMAL);
                searchRouteResult(mTabLayout.getSelectedTabPosition());
            } else if (msg.what == 2) {
                endName = data.getString("end_name");
                endPoint = (LatLng) data.getParcelable("end_point");
                edit_end.setText(endName, TextView.BufferType.NORMAL);
                searchRouteResult(mTabLayout.getSelectedTabPosition());
            }
        }
    };
    private String mSDCardPath = null;
    private boolean hasInitSuccess = false;
    private BNRoutePlanNode mStartNode = null;
    // public String[] ways = new String[]{"驾车", "公交", "步行", "骑行"};
    private String[] ways = new String[]{"驾车", "公交"};

    //坐标转换
    public static BDLocation bd2gcj(BDLocation loc) {
        return LocationClient.getBDLocationInCoorType(loc, BDLocation.BDLOCATION_BD09LL_TO_GCJ02);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_plan);
        initView();
        initData();
        initEvent();
    }

    /**
     * 初始化布局
     */
    private void initView() {
        mTabLayout = findViewById(R.id.tab_layout);
        mMapView = findViewById(R.id.route_map);
        edit_end = findViewById(R.id.edit_end);
        edit_start = findViewById(R.id.edit_start);
        progressBar = findViewById(R.id.progressbar);
        mFirstLine = findViewById(R.id.firstline);
        mSecondLine = findViewById(R.id.secondline);
        mBottomInfo = findViewById(R.id.bottom_info);
        recycler_bus_list = findViewById(R.id.recycler_bus_list);
        img_back = findViewById(R.id.img_back);
        img_return = findViewById(R.id.img_return);
        img_setting = findViewById(R.id.img_setting);
        mRootLayout = findViewById(R.id.root_layout);
        layout_no_information = findViewById(R.id.layout_no_information);
    }

    private void initData() {
        routePlanActivityInstance = this;
        if (null != getIntent()) {
            startPoint = getIntent().getParcelableExtra("start_point");
            endPoint = getIntent().getParcelableExtra("end_point");
            startName = getIntent().getStringExtra("start_name");
            endName = getIntent().getStringExtra("end_name");
            mCurrentCityName = getIntent().getStringExtra("city_name");
        }
        // 默认值是屏幕宽度
        animatorX = (int) SPUtil.get(RoutePlanActivity.this, ChString.REVEAL_CENTER_X, this.getWindowManager().getDefaultDisplay().getWidth());
        // 默认值是屏幕高度
        animatorY = (int) SPUtil.get(RoutePlanActivity.this, ChString.REVEAL_CENTER_Y, this.getWindowManager().getDefaultDisplay().getHeight());
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(startPoint));
        mSearch = RoutePlanSearch.newInstance();
        mSearch.setOnGetRoutePlanResultListener(this);
        for (int i = 0; i < ways.length; i++) {
            mTabLayout.addTab(mTabLayout.newTab().setText(ways[i]));
        }
        mBaiduMap.setCompassEnable(false);
        mUiSettings = mBaiduMap.getUiSettings();
        mUiSettings.setOverlookingGesturesEnabled(false);
        mUiSettings.setRotateGesturesEnabled(false);
        // 隐藏logo
        View child = mMapView.getChildAt(1);
        if (child != null && (child instanceof ImageView || child instanceof ZoomControls)) {
            child.setVisibility(View.INVISIBLE);
        }
        edit_end.setText(endName, TextView.BufferType.NORMAL);
        edit_start.setText(startName, TextView.BufferType.NORMAL);
        edit_start.setFocusable(false);
        edit_end.setFocusable(false);
        recycler_bus_list.setLayoutManager(new LinearLayoutManager(this));
        recycler_bus_list.setHasFixedSize(true);
    }

    /**
     * 初始化定位和监听
     */
    private void initEvent() {
        mBaiduMap.setMyLocationEnabled(true);
        initLocation();
        revealAnimatorUtil = new RevealAnimatorUtil(mRootLayout, this);
        mRootLayout.post(new Runnable() {
            @Override
            public void run() {
                revealAnimatorUtil.startRevealAnimator(false, animatorX, animatorY);
            }
        });
        img_back.setOnClickListener(this);
        img_return.setOnClickListener(this);
        img_setting.setOnClickListener(this);
        mTabLayout.addOnTabSelectedListener(this);
        // 默认选择的路线
        mTabLayout.getTabAt(BUS_ROUTE).select();
        edit_start.setOnClickListener(this);
        edit_end.setOnClickListener(this);
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        //定位初始化
        mLocationClient = new LocationClient(this);
        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        // 打开gps
        option.setOpenGps(true);
        // 设置坐标类型
        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        //设置locationClientOption
        mLocationClient.setLocOption(option);
        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        switch (tab.getPosition()) {
            case BUS_ROUTE:
                mMapView.setVisibility(View.GONE);
                mBottomInfo.setVisibility(View.GONE);
                searchRouteResult(BUS_ROUTE);
                break;
            case DRIVE_ROUTE:
                checkStartAndEndPoint();
                recycler_bus_list.setVisibility(View.GONE);
                layout_no_information.setVisibility(View.GONE);
                mMapView.setVisibility(View.VISIBLE);
                mBottomInfo.setVisibility(View.VISIBLE);
                searchRouteResult(DRIVE_ROUTE);
                break;
                 /*  case WALK_ROUTE:
                checkStartAndEndPoint();
                recycler_bus_list.setVisibility(View.GONE);
                layout_no_information.setVisibility(View.GONE);
                mMapView.setVisibility(View.VISIBLE);
                mBottomInfo.setVisibility(View.VISIBLE);
                searchRouteResult(WALK_ROUTE);
                break;
            case RIDE_ROUTE:
                checkStartAndEndPoint();
                recycler_bus_list.setVisibility(View.GONE);
                layout_no_information.setVisibility(View.GONE);
                mMapView.setVisibility(View.VISIBLE);
                mBottomInfo.setVisibility(View.VISIBLE);
                searchRouteResult(RIDE_ROUTE);
                break;*/
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        mSearch.destroy();
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();
    }

    /**
     * 搜索路线
     *
     * @param routeType
     */
    private void searchRouteResult(int routeType) {
        checkStartAndEndPoint();
        PlanNode stNode = PlanNode.withLocation(startPoint);
        PlanNode enNode = PlanNode.withLocation(endPoint);
        progressBar.setVisibility(View.VISIBLE);
        switch (routeType) {
            case DRIVE_ROUTE:
                //第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
                mSearch.drivingSearch((new DrivingRoutePlanOption())
                        .from(stNode)
                        .to(enNode));
                break;
            case BUS_ROUTE:
                mSearch.transitSearch((new TransitRoutePlanOption())
                        .from(stNode)
                        .to(enNode)
                        .city(mCurrentCityName));
                break;
       /*     case WALK_ROUTE:
                mSearch.walkingSearch((new WalkingRoutePlanOption())
                        .from(stNode)
                        .to(enNode));
                break;
            case RIDE_ROUTE:
                mSearch.bikingSearch((new BikingRoutePlanOption())
                        .from(stNode)
                        .to(enNode)
                        // ridingType  0 普通骑行，1 电动车骑行
                        // 默认普通骑行
                        .ridingType(0));
                break;*/
        }
    }

    /**
     * 检查起始位置
     */
    private void checkStartAndEndPoint() {
        if (null == startPoint) {
            Toast toast = Toast.makeText(getApplicationContext(), "未设置起点", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        if (null == endPoint) {
            Toast toast = Toast.makeText(getApplicationContext(), "未设置终点", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
    }

    /**
     * 步行路线结果回调
     *
     * @param walkingRouteResult
     */
    @Override
    public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {
        Log.d("onGetWalkingRouteResult", "walking start");
        /*mBaiduMap.clear();
        if (walkingRouteResult != null && walkingRouteResult.getRouteLines() != null) {
            if (walkingRouteResult.getRouteLines().size() > 0) {
                progressBar.setVisibility(View.GONE);
                MyWalkingRouteOverlay overlay = new MyWalkingRouteOverlay(mBaiduMap);
                if (walkingRouteResult.getRouteLines().size() > 0) {
                    //获取路径规划数据,(以返回的第一条路线为例）
                    //为DrivingRouteOverlay实例设置数据
                    overlay.setData(walkingRouteResult.getRouteLines().get(0));
                    //在地图上绘制DrivingRouteOverlay
                    overlay.removeFromMap();
                    overlay.addToMap();
                    overlay.zoomToSpan();
                    int dis = walkingRouteResult.getRouteLines().get(0).getDistance();
                    int dur = walkingRouteResult.getRouteLines().get(0).getDuration();
                    String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                    mFirstLine.setText(des);
                    mSecondLine.setVisibility(View.GONE);
                    mBottomInfo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            WalkNaviLaunchParam walkParam = new WalkNaviLaunchParam().stPt(locationPoint).endPt(endPoint);
                            startWalkNavi(walkParam);
                        }
                    });
                    if (dis > 10000) {
                        Toast.makeText(getApplicationContext(), "距离过远，不建议步行", Toast.LENGTH_SHORT).show();
                    }
                } else if (walkingRouteResult != null && walkingRouteResult.getRouteLines() == null) {
                    Toast.makeText(getApplicationContext(), "对不起，没有搜索到相关数据", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "对不起，没有搜索到相关数据", Toast.LENGTH_SHORT).show();
            }
        }*/
    }

    /**
     * 换乘路线结果回调
     *
     * @param transitRouteResult
     */
    @Override
    public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {
        mBaiduMap.clear();
        progressBar.setVisibility(View.GONE);
        mBottomInfo.setVisibility(View.GONE);
        if (transitRouteResult != null && transitRouteResult.getRouteLines() != null) {
            if (transitRouteResult.getRouteLines().size() > 0) {
                TransitRouteAdapter mBusAdapter = new TransitRouteAdapter(RoutePlanActivity.this, transitRouteResult);
                //公交车详线路情
                mBusAdapter.setOnItemClickListener(new TransitRouteAdapter.ItemBusRouteOnClickListener() {
                    @Override
                    public void onClick(View view, TransitRouteLine transitRouteLine, TransitRouteResult transitRouteResult, int position) {
                        Intent intent = new Intent(RoutePlanActivity.this, TransitRouteDetailActivity.class);
                        intent.putExtra("transit_route_line", transitRouteLine);
                        intent.putExtra("transit_route_result", transitRouteResult);
                        intent.putExtra("start_point", locationPoint);
                        intent.putExtra("city_name", mCurrentCityName);
                        startActivity(intent);
                    }
                });
                recycler_bus_list.setVisibility(View.VISIBLE);
                recycler_bus_list.setAdapter(mBusAdapter);
            }
        } else {
            layout_no_information.setVisibility(View.VISIBLE);
            Toast.makeText(getApplicationContext(), "对不起，没有搜索到相关数据", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

    }

    /**
     * 驾车路线结果回调
     *
     * @param drivingRouteResult
     */
    @Override
    public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {
        if (initDirs()) {
            initNavi();
        }
        //创建DrivingRouteOverlay实例
        mBaiduMap.clear();
        if (drivingRouteResult != null && drivingRouteResult.getRouteLines() != null) {
            if (drivingRouteResult.getRouteLines().size() > 0) {
                progressBar.setVisibility(View.GONE);
                // 绘制驾驶路线
                MyDrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaiduMap);
                if (drivingRouteResult.getRouteLines().size() > 0) {
                    // 获取路径规划数据,以返回的第一条路线为例，这里只展示第一条路线
                    // 为DrivingRouteOverlay实例设置数据
                    overlay.setData(drivingRouteResult.getRouteLines().get(0));
                    // 在地图上绘制DrivingRouteOverlay
                    overlay.removeFromMap();
                    overlay.addToMap();
                    overlay.zoomToSpan();
                    int firstPrice = 0;
                    int dis = drivingRouteResult.getRouteLines().get(0).getDistance();
                    int dur = drivingRouteResult.getRouteLines().get(0).getDuration();
                    String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                    mFirstLine.setText(des);
                    if (firstPrice != 0) {
                        mSecondLine.setVisibility(View.VISIBLE);
                        mSecondLine.setText("打车约" + firstPrice + "元");
                    }
                    mBottomInfo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            routeplanToNavi();
                            Log.d("routeplanToNavi()", "###########");
                        }
                    });
                } else if (drivingRouteResult != null && drivingRouteResult.getRouteLines() == null) {
                    Toast.makeText(getApplicationContext(), "对不起，没有搜索到相关数据", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "对不起，没有搜索到相关数据", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

    }

    /**
     * 骑行路线结果回调
     *
     * @param bikingRouteResult
     */
    @Override
    public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {
        Log.d("onGetBikingRouteResult", "biking start");
        /*if (initDirs()) {
            initNavi();
        }
        mBaiduMap.clear();
        if (bikingRouteResult != null && bikingRouteResult.getRouteLines() != null) {
            if (bikingRouteResult.getRouteLines().size() > 0) {
                progressBar.setVisibility(View.GONE);
                MyBikingRouteOverlay overlay = new MyBikingRouteOverlay(mBaiduMap);
                if (bikingRouteResult.getRouteLines().size() > 0) {
                    //获取路径规划数据,(以返回的第一条路线为例）
                    //为DrivingRouteOverlay实例设置数据
                    overlay.setData(bikingRouteResult.getRouteLines().get(0));
                    //在地图上绘制DrivingRouteOverlay
                    overlay.removeFromMap();
                    overlay.addToMap();
                    overlay.zoomToSpan();
                    int dis = (int) bikingRouteResult.getRouteLines().get(0).getDistance();
                    int dur = (int) bikingRouteResult.getRouteLines().get(0).getDuration();
                    String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                    mFirstLine.setText(des);
                    mSecondLine.setVisibility(View.GONE);
                    mBottomInfo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            BikeNaviLaunchParam bikeParam = new BikeNaviLaunchParam().stPt(locationPoint).endPt(endPoint);
                            startBikeNavi(bikeParam);
                        }
                    });
                    if (dis > 50000) {
                        Toast.makeText(getApplicationContext(), "距离过远，不建议骑行", Toast.LENGTH_SHORT).show();
                    }
                } else if (bikingRouteResult != null && bikingRouteResult.getRouteLines() == null) {
                    Toast.makeText(getApplicationContext(), "对不起，没有搜索到相关数据", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "对不起，没有搜索到相关数据", Toast.LENGTH_SHORT).show();
            }
        }*/
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.img_back:
                revealAnimatorUtil.startRevealAnimator(true, animatorX, animatorY);
                break;
            case R.id.img_return:
                changeStartandEnd();
                break;
            case R.id.img_setting:
                if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                    NormalUtils.gotoSettings(RoutePlanActivity.this);
                }
                break;
            case R.id.edit_start:
                intent = new Intent();
                intent.setClass(RoutePlanActivity.this, RoutePlanSearchActivity.class);
                intent.putExtra("start_point", startPoint);
                intent.putExtra("end_name", endName);
                intent.putExtra("start_name", startName);
                intent.putExtra("end_point", endPoint);
                intent.putExtra("city_name", mCurrentCityName);
                intent.putExtra("tag", "start");
                startActivity(intent);
                //finish();
                break;
            case R.id.edit_end:
                intent = new Intent();
                intent.setClass(RoutePlanActivity.this, RoutePlanSearchActivity.class);
                intent.putExtra("start_point", startPoint);
                intent.putExtra("end_name", endName);
                intent.putExtra("start_name", startName);
                intent.putExtra("end_point", endPoint);
                intent.putExtra("city_name", mCurrentCityName);
                intent.putExtra("tag", "end");
                startActivity(intent);
                //finish();
                break;
            default:
        }
    }

    /**
     * 起点终点切换
     */
    private void changeStartandEnd() {
        String name = startName;
        startName = endName;
        endName = name;
        LatLng point = startPoint;
        startPoint = endPoint;
        endPoint = point;
        edit_end.setText(endName, TextView.BufferType.NORMAL);
        edit_start.setText(startName, TextView.BufferType.NORMAL);
        searchRouteResult(mTabLayout.getSelectedTabPosition());
    }

    /**
     * 驾驶导航算路
     */
    private void routeplanToNavi() {
        final int coType = BNRoutePlanNode.CoordinateType.GCJ02;
        if (!hasInitSuccess) {
            Toast.makeText(RoutePlanActivity.this, "还未初始化!", Toast.LENGTH_SHORT).show();
        }

        BDLocation srcBdLocation = new BDLocation();
        srcBdLocation.setLatitude(locationPoint.latitude);
        srcBdLocation.setLongitude(locationPoint.longitude);
        BDLocation srcGcj = bd2gcj(srcBdLocation);
        BDLocation destBdLocation = new BDLocation();
        destBdLocation.setLatitude(endPoint.latitude);
        destBdLocation.setLongitude(endPoint.longitude);
        BDLocation destGcj = bd2gcj(destBdLocation);

        BNRoutePlanNode sNode = new BNRoutePlanNode(srcGcj.getLongitude(), srcGcj.getLatitude(), "我的地点", null, coType);
        BNRoutePlanNode eNode = new BNRoutePlanNode(destGcj.getLongitude(), destGcj.getLatitude(), "目标地点", null, coType);

        mStartNode = sNode;

        List<BNRoutePlanNode> list = new ArrayList<>();
        list.add(sNode);
        list.add(eNode);

        // 发起导航
        BaiduNaviManagerFactory.getRoutePlanManager().routeplanToNavi(
                list,
                IBNRoutePlanManager.RoutePlanPreference.ROUTE_PLAN_PREFERENCE_DEFAULT,
                null,
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_START:
                                Toast.makeText(RoutePlanActivity.this, "导航:算路开始", Toast.LENGTH_SHORT)
                                        .show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_SUCCESS:
                                Toast.makeText(RoutePlanActivity.this, "导航:算路成功", Toast.LENGTH_SHORT)
                                        .show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_FAILED:
                                Toast.makeText(RoutePlanActivity.this, "导航:算路失败", Toast.LENGTH_SHORT)
                                        .show();
                                break;
                            case IBNRoutePlanManager.MSG_NAVI_ROUTE_PLAN_TO_NAVI:
                                Toast.makeText(RoutePlanActivity.this, "导航:算路成功准备进入导航", Toast.LENGTH_SHORT)
                                        .show();
                                // 跳转到诱导页面
                                Intent intent = new Intent(RoutePlanActivity.this,
                                        DemoGuideActivity.class);
                                Bundle bundle = new Bundle();
                                bundle.putSerializable(ROUTE_PLAN_NODE, mStartNode);
                                intent.putExtras(bundle);
                                startActivity(intent);
                                break;
                            default:
                                // nothing
                                break;
                        }
                    }
                });
    }

    /**
     * 步行导航初始化，并监听
     *
     * @param walkParam
     */
    private void startWalkNavi(final WalkNaviLaunchParam walkParam) {
        try {
            WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d("navi", "WalkNavi engineInitSuccess");
                    routePlanWithWalkParam(walkParam);
                }

                @Override
                public void engineInitFail() {
                    Log.d("navi", "WalkNavi engineInitFail");
                    //mBikeNaviHelper.initNaviEngine();
                }
            });
        } catch (Exception e) {
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
                Log.d("navi", "WalkNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d("navi", "WalkNavi onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(RoutePlanActivity.this, WNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError walkRoutePlanError) {

            }
        });
    }

    /**
     * 骑行导航初始化，并监听
     *
     * @param bikeParam
     */
    private void startBikeNavi(final BikeNaviLaunchParam bikeParam) {
        try {
            // 初始化导航引擎
            BikeNavigateHelper.getInstance().initNaviEngine(this, new IBEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d("navi", "BikeNavi engineInitSuccess");
                    routePlanWithBikeParam(bikeParam);
                }

                @Override
                public void engineInitFail() {
                    Log.d("navi", "BikeNavi engineInitFail");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发起骑行导航算路
     */
    private void routePlanWithBikeParam(BikeNaviLaunchParam bikeParam) {
        BikeNavigateHelper.getInstance().routePlanWithParams(bikeParam, new IBRoutePlanListener() {

            @Override
            public void onRoutePlanStart() {
                Log.d("navi", "BikeNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d("navi", "BikeNavi onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(RoutePlanActivity.this, BNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(BikeRoutePlanError error) {
                Log.d("navi", "BikeNavi onRoutePlanFail");
            }
        });
    }

    /**
     * 获取Sdcard目录
     *
     * @return
     */
    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    /**
     * 初始化导航目录，生成对应的文件夹
     *
     * @return
     */
    private boolean initDirs() {
        mSDCardPath = getSdcardDir();
        if (mSDCardPath == null) {
            return false;
        }
        File f = new File(mSDCardPath, APP_FOLDER_NAME);
        if (!f.exists()) {
            try {
                f.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private boolean hasBasePhoneAuth() {
        PackageManager pm = this.getPackageManager();
        for (String auth : authBaseArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 初始化导航
     */
    private void initNavi() {
        // 申请权限
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (!hasBasePhoneAuth()) {
                this.requestPermissions(authBaseArr, authBaseRequestCode);
                return;
            }
        }

        /**
         * 导航地图sdk初始化
         * @param context          建议是应用的context
         * @param sdcardRootPath   系统SD卡根目录路径
         * @param appFolderName    应用在SD卡中的目录名
         * @param naviInitListener 百度导航初始化监听器
         */
        BaiduNaviManagerFactory.getBaiduNaviManager().init(this,
                mSDCardPath, APP_FOLDER_NAME, new IBaiduNaviManager.INaviInitListener() {

                    @Override
                    public void onAuthResult(int status, String msg) {
                        String result;
                        if (0 == status) {
                            result = "key校验成功!";
                        } else {
                            result = "key校验失败, " + msg;
                        }
                        Toast.makeText(RoutePlanActivity.this, result, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void initStart() {
                        Toast.makeText(RoutePlanActivity.this, "导航引擎初始化开始", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void initSuccess() {
                        Toast.makeText(RoutePlanActivity.this, "导航引擎初始化成功", Toast.LENGTH_SHORT).show();
                        hasInitSuccess = true;
                        // 初始化tts
                        if (Build.VERSION.SDK_INT >= 23) {
                            if (ContextCompat.checkSelfPermission(RoutePlanActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                                    PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(RoutePlanActivity.this, new String[]{Manifest.permission
                                        .WRITE_EXTERNAL_STORAGE}, 1);
                            } else {
                                initTTS();
                            }
                        } else {
                            initTTS();
                        }
                    }

                    @Override
                    public void initFailed() {
                        Toast.makeText(RoutePlanActivity.this, "导航引擎初始化失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 初始化语音播报
     */
    private void initTTS() {
        // 使用内置TTS
        BaiduNaviManagerFactory.getTTSManager().initTTS(getApplicationContext(),
                getSdcardDir(), APP_FOLDER_NAME, NormalUtils.getTTSAppID());

        // 注册同步内置tts状态回调
        BaiduNaviManagerFactory.getTTSManager().setOnTTSStateChangedListener(
                new IBNTTSManager.IOnTTSPlayStateChangedListener() {
                    @Override
                    public void onPlayStart() {
                        Log.e("BNSDKDemo", "ttsCallback.onPlayStart");
                    }

                    @Override
                    public void onPlayEnd(String speechId) {
                        Log.e("BNSDKDemo", "ttsCallback.onPlayEnd");
                    }

                    @Override
                    public void onPlayError(int code, String message) {
                        LoggerProxy.printable(true);
                        Log.e("BNSDKDemo", "ttsCallback.onPlayError" + code + message);
                    }
                }
        );

        // 注册内置tts 异步状态消息
        BaiduNaviManagerFactory.getTTSManager().setOnTTSStateChangedHandler(
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        Log.e("BNSDKDemo", "ttsHandler.msg.what=" + msg.what);
                    }
                }
        );
    }

    @Override
    public void onBackPressed() {
        revealAnimatorUtil.startRevealAnimator(true, animatorX, animatorY);
    }

    /**
     * 注册定位监听
     */
    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // mapView 销毁后不在处理新接收的位置
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
            locationName = location.getAddrStr();
            locationPoint = new LatLng(location.getLatitude(), location.getLongitude());
            if (locationName != null) {
                if (mTag.equals("start")) {
                    startName = locationName;
                    mTag = "";
                    startPoint = new LatLng(location.getLatitude(), location.getLongitude());
                } else if (mTag.equals("end")) {
                    endName = locationName;
                    endPoint = new LatLng(location.getLatitude(), location.getLongitude());
                    mTag = "";
                }
            }
            if (startName.equals(locationName)) {
                edit_start.setText("我的位置");
            }
            if (endName.equals(locationName)) {
                edit_end.setText("我的位置");
            }
        }
    }
}
