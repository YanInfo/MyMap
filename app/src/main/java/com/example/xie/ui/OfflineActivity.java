package com.example.xie.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;
import com.example.xie.R;
import com.example.xie.adapter.CityExpandableListAdapter;
import com.example.xie.adapter.HotcityListAdapter;
import com.example.xie.adapter.LoadingMapAdapter;
import com.example.xie.adapter.LoadedMapAdapter;
import com.example.xie.util.ChString;
import com.example.xie.util.MapUtil;
import com.example.xie.util.RevealAnimatorUtil;
import com.example.xie.util.SPUtil;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 无网络时
 */
public class OfflineActivity extends AppCompatActivity implements View.OnClickListener, MKOfflineMapListener, ExpandableListView.OnGroupClickListener, ExpandableListView.OnChildClickListener, AdapterView.OnItemClickListener, LoadedMapAdapter.OnClickLoadedListener, LoadingMapAdapter.OnClickLoadedListener {

    public static final int UPDATE_VIEW = 100;

    private Button button_city_list;
    private Button button_downloaded_manage;
    private TextView current_name;
    private TextView current_size;
    ListView listView_hot_city;
    RelativeLayout currentItem;
    ImageView imageView_back;
    private LoadedMapAdapter lAdapter;
    private LoadingMapAdapter dAdapter = null;
    public static HashMap<String, String> downLoadingList;
    public static HashMap<String, String> isPauseList;

    LinearLayout mRootLayout;
    String mCurrentCityName;

    private int animatorX, animatorY;//动画开始和结束的坐标
    private RevealAnimatorUtil revealAnimatorUtil;//揭露动画工具类

    ExpandableListView allCityList;
    private CityExpandableListAdapter adapter;
    private HotcityListAdapter hAdapter;

    private MKOLSearchRecord currentRecord;

    private HashMap<String, Boolean> hashMap = new HashMap<String, Boolean>(); //是否已下载;

    ArrayList<MKOLSearchRecord> records1;
    ArrayList<MKOLSearchRecord> records2;

    public static MKOfflineMap mOffline;
    public ArrayList<MKOLUpdateElement> loadedMapList = null;
    private ArrayList<MKOLUpdateElement> loadedList = new ArrayList<MKOLUpdateElement>();
    private ArrayList<MKOLUpdateElement> loadingList = new ArrayList<MKOLUpdateElement>();
    Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case UPDATE_VIEW:
                    dAdapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);
        initView();
        initData();
        initEvent();
    }

    private void initView() {
        current_name = findViewById(R.id.current_name);
        current_size = findViewById(R.id.current_size);
        button_city_list = findViewById(R.id.button_city_list);
        button_downloaded_manage = findViewById(R.id.button_downloaded_manage);
        imageView_back = findViewById(R.id.imageView_back);
        currentItem = (RelativeLayout) findViewById(R.id.current_item);
        mRootLayout = findViewById(R.id.container);
    }

    private void initData() {
        animatorX = (int) SPUtil.get(OfflineActivity.this, ChString.REVEAL_CENTER_X, this.getWindowManager().getDefaultDisplay().getWidth());//默认值是屏幕宽度
        animatorY = (int) SPUtil.get(OfflineActivity.this, ChString.REVEAL_CENTER_Y, this.getWindowManager().getDefaultDisplay().getHeight());//默认值是屏幕高度
        revealAnimatorUtil = new RevealAnimatorUtil(mRootLayout,this);
        mRootLayout.post(new Runnable() {
            @Override
            public void run() {
                revealAnimatorUtil.startRevealAnimator(false,animatorX,animatorY);
            }
        });
        mOffline = new MKOfflineMap();
        mOffline.init(this);
        downLoadingList = new HashMap<String, String>();
        // 获取已下过的离线地图信息
        ArrayList<MKOLUpdateElement> localMapList = mOffline.getAllUpdateInfo();

        if(localMapList!=null){
            for (MKOLUpdateElement r : localMapList) {
                if (r.ratio == 100) {
                    loadedList.add(r);
                } else {
                    loadingList.add(r);
                    downLoadingList.put(r.cityName, "1");
                }
            }
        }
        ListView listView_local_map = (ListView) findViewById(R.id.loadedMapList);
        lAdapter = new LoadedMapAdapter(this, loadedList);
        listView_local_map.setAdapter(lAdapter);

        loadedMapList = new ArrayList<MKOLUpdateElement>();
        ListView listView_loading_list = (ListView) findViewById(R.id.lodinglist);
        dAdapter = new LoadingMapAdapter(this, loadingList);
        listView_loading_list.setAdapter(dAdapter);

        listView_hot_city = (ListView) findViewById(R.id.hotcitylist);
        final ArrayList<Integer> hotCityList = new ArrayList<Integer>();
        // 获取热门城市列表
        records1 = mOffline.getHotCityList();
        if (records1 != null) {
            for (MKOLSearchRecord r : records1) {
                hotCityList.add(r.cityID);
            }
        }
        hAdapter = new HotcityListAdapter(this, records1, hashMap);

        listView_hot_city.setAdapter(hAdapter);

        allCityList = (ExpandableListView) findViewById(R.id.allcitylist);
        // 获取所有支持离线地图的城市
        records2 = mOffline.getOfflineCityList();
        if (records1 != null) {
            for (MKOLSearchRecord r : records2) {
                //                allCities.add(r.cityName+"--" + this.formatDataSize(r.size));
                //                allCitiyIds.add(r.cityID);
                hashMap.put(r.cityName, downList(r.cityName));
                if (!"1".equals(downLoadingList.get(r.cityName))) {
                    downLoadingList.put(r.cityName, "0");
                }
                if (r.childCities != null && r.childCities.size() != 0) {
                    ArrayList<MKOLSearchRecord> childrecord = r.childCities;
                    //
                    for (MKOLSearchRecord cr : childrecord) {
                        hashMap.put(cr.cityName, downList(cr.cityName));
                        if (!"1".equals(downLoadingList.get(cr.cityName))) {
                            downLoadingList.put(cr.cityName, "0");
                        }
                    }
                }
            }
        }

        adapter = new CityExpandableListAdapter(this, records2, hashMap);

        allCityList.setAdapter(adapter);
        allCityList.setGroupIndicator(null);
        if (null != getIntent()) {
            mCurrentCityName = getIntent().getStringExtra("city_name");
            setCurrentLocation(mCurrentCityName);
        }
    }

    private void initEvent() {
        lAdapter.setListener(this);
        dAdapter.setListener(this);
        allCityList.setOnGroupClickListener(this);
        allCityList.setOnChildClickListener(this);
        listView_hot_city.setOnItemClickListener(this);
        button_city_list.setOnClickListener(this);
        button_downloaded_manage.setOnClickListener(this);
        imageView_back.setOnClickListener(this);
        currentItem.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imageView_back:
                revealAnimatorUtil.startRevealAnimator(true,animatorX,animatorY);

                //finish();
            case R.id.button_city_list:
                clickCityListButton();
                break;
            case R.id.button_downloaded_manage:
                clickDownloadedManageButton();
                break;
            case R.id.current_item:
                if (hashMap.get(currentRecord.cityName)) {
                    Toast.makeText(OfflineActivity.this, "离线地图已下载", Toast.LENGTH_LONG).show();
                } else {
                    current_size.setText("下载中");
                    downLoadingList.put(currentRecord.cityName, "2");
                    start(currentRecord.cityID);
                }
                break;
            default:
        }
    }

    /**
     * 切换至城市列表
     *
     * @param
     */
    public void clickCityListButton() {
        LinearLayout cl = (LinearLayout) findViewById(R.id.layout_city_list);
        LinearLayout lm = (LinearLayout) findViewById(R.id.layout_local_map);
        lm.setVisibility(View.GONE);
        cl.setVisibility(View.VISIBLE);
        Button button_city_list = (Button) findViewById(R.id.button_city_list);
        button_city_list.setBackgroundResource(R.drawable.offline_left_checked);
        button_city_list.setTextColor(Color.parseColor("#ffffff"));
        Button button_downloaded_manage = (Button) findViewById(R.id.button_downloaded_manage);
        button_downloaded_manage.setBackgroundResource(R.drawable.offline_right_normal);
        button_downloaded_manage.setTextColor(Color.parseColor("#05C1F1"));
    }

    public void clickDownloadedManageButton() {
        LinearLayout cl = (LinearLayout) findViewById(R.id.layout_city_list);
        LinearLayout lm = (LinearLayout) findViewById(R.id.layout_local_map);
        lm.setVisibility(View.VISIBLE);
        cl.setVisibility(View.GONE);
        Button button_city_list = (Button) findViewById(R.id.button_city_list);
        button_city_list.setBackgroundResource(R.drawable.offline_left_normal);
        button_city_list.setTextColor(Color.parseColor("#05C1F1"));
        Button button_downloaded_manage = (Button) findViewById(R.id.button_downloaded_manage);
        button_downloaded_manage.setBackgroundResource(R.drawable.offline_right_checked);
        button_downloaded_manage.setTextColor(Color.parseColor("#ffffff"));
        updateView(null, false);
    }

    @Override
    public void onGetOfflineMapState(int i, int i1) {
        switch (i) {
            case MKOfflineMap.TYPE_DOWNLOAD_UPDATE:
                /*final MKOLUpdateElement update = mOffline.getUpdateInfo(i1);
                // 处理下载进度更新提示
                if (update != null) {
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            while (update.ratio!=100) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    break;
                                }
                                handler.sendEmptyMessage(UPDATE_VIEW);
                            }
                        }
                    };
                    thread.start();
                }*/
                MKOLUpdateElement update = mOffline.getUpdateInfo(i1);
                // 处理下载进度更新提示
                if (update != null) {
                    if (update.ratio == 100) {
                        updateView(update, true);
                    } else if (update.ratio % 5 == 0) {
                        updateView(null, false);
                    }
                }
                break;
            case MKOfflineMap.TYPE_NEW_OFFLINE:
                // 有新离线地图安装
                Log.d("OfflineDemo", String.format("add offlinemap num:%d", i1));
                break;
            case MKOfflineMap.TYPE_VER_UPDATE:
                // 版本更新提示
                // MKOLUpdateElement e = mOffline.getUpdateInfo(state);
                break;
            default:
                break;
        }

    }

    public boolean downList(String cityName) {
        Boolean flag = false;
        if (loadedList != null) {
            for (int i = 0; i < loadedList.size(); i++) {
                MKOLUpdateElement element = loadedList.get(i);
                if (cityName.equals(element.cityName)) {
                    flag = true;
                    break;
                } else {
                    flag = false;
                }
            }
        }
        return flag;
    }

    public void setCurrentLocation(String currentCityName) {
        current_name.setText(currentCityName);
        currentRecord = search(currentCityName);
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        if (records2 != null) {
            MKOLSearchRecord record = records2.get(groupPosition);
            if (record.childCities == null) {
                if (hashMap.get(record.cityName)) {
                    Toast.makeText(OfflineActivity.this, "离线地图已下载", Toast.LENGTH_LONG).show();
                } else {
                    downLoadingList.put(record.cityName, "2");
                    start(record.cityID);
                    if (mCurrentCityName.equals(record.cityName)) {
                        current_size.setText("下载中");
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                int childPosition, long id) {
        if (records2 != null) {
            MKOLSearchRecord record = records2.get(groupPosition);
            MKOLSearchRecord cldred = record.childCities.get(childPosition);
            if (hashMap.get(cldred.cityName)) {
                Toast.makeText(OfflineActivity.this, "离线地图已下载", Toast.LENGTH_LONG).show();
            } else {
                TextView childSize = (TextView) v.findViewById(R.id.child_size);

                childSize.setText("下载中");
                downLoadingList.put(cldred.cityName, "2");
                start(cldred.cityID);
                if (mCurrentCityName.equals(cldred.cityName)) {
                    current_size.setText("下载中");
                }
            }

        }
        return false;
    }

    /**
     * 开始下载
     *
     * @param
     */
    public void start(int cityid) {
        mOffline.start(cityid);
        clickDownloadedManageButton();
        updateView(null, false);
    }

    /**
     * 更新状态显示
     */
    public void updateView(MKOLUpdateElement element, boolean flag) {

        loadedMapList = mOffline.getAllUpdateInfo();
        if (loadedMapList == null) {
            loadedMapList = new ArrayList<MKOLUpdateElement>();
        }
        loadingList.clear();
        loadedList.clear();
        for (MKOLUpdateElement element1 : loadedMapList) {
            if (element1.ratio != 100) {
                loadingList.add(element1);
            } else {
                loadedList.add(element1);
            }
        }
        if (element != null) {
            hashMap.put(element.cityName, flag);
            if (currentRecord.cityID == element.cityID) {
                if (flag) {
                    current_size.setText("已下载");
                } else {
                    current_size.setText(MapUtil.formatDataSize(currentRecord.dataSize));
                }

            } else {
                adapter.notifyDataSetInvalidated();
                hAdapter.notifyDataSetChanged();
            }
        }
        lAdapter.notifyDataSetChanged();
        dAdapter.notifyDataSetChanged();
    }

    /**
     * 搜索离线需市
     *
     * @param
     */
    public MKOLSearchRecord search(String currentCityName) {
        ArrayList<MKOLSearchRecord> records = mOffline.searchCity(currentCityName);

        if (records == null || records.size() != 1) {
            return null;
        }

        if (hashMap.get(records.get(0).cityName)) {
            current_size.setText("已下载");
        } else {
            if ("0".equals(downLoadingList.get(records.get(0).cityName))) {
                current_size.setText(MapUtil.formatDataSize(records.get(0).dataSize));

            } else {
                current_size.setText("下载中");
            }
        }
        return records.get(0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MKOLSearchRecord mkolSearchRecord = records1.get(position);
        if (hashMap.get(mkolSearchRecord.cityName)) {
            Toast.makeText(OfflineActivity.this, "离线地图已下载", Toast.LENGTH_LONG).show();
        } else {
            TextView textView_city_size = (TextView) view.findViewById(R.id.textView_city_size);
            textView_city_size.setText("下载中");
            downLoadingList.put(mkolSearchRecord.cityName, "2");
            start(mkolSearchRecord.cityID);
            if (mCurrentCityName.equals(mkolSearchRecord.cityName)) {
                current_size.setText("下载中");
            }

        }
    }

    @Override
    public void onRemoveClick(MKOLUpdateElement element, boolean flag) {
        mOffline.remove(element.cityID);
        downLoadingList.put(element.cityName, "0");
        updateView(element, false);
    }

    @Override
    public void onDoUpdateClick(MKOLUpdateElement element) {
        if (element.update) {
            Toast.makeText(OfflineActivity.this, "开始更新", Toast.LENGTH_SHORT).show();
            mOffline.update(element.cityID);
        } else {
            Toast.makeText(OfflineActivity.this, "当前已是最新", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        mOffline.destroy();

        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        revealAnimatorUtil.startRevealAnimator(true,animatorX,animatorY);
    }
}
