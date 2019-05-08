package com.example.xie.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.example.xie.R;
import com.example.xie.adapter.BaseAdapter;
import com.example.xie.adapter.LoadMoreAdapterWrapper;
import com.example.xie.adapter.MySuggestionsAdapter;
import com.example.xie.adapter.SearchAdapter;
import com.example.xie.util.MapUtil;
import com.mancj.materialsearchbar.MaterialSearchBar;

import java.util.ArrayList;
import java.util.List;

import static com.mancj.materialsearchbar.MaterialSearchBar.BUTTON_BACK;

/**
 * 这里对应的是起始位置和终止位置输入框，输入内容时跳转
 */
public class RoutePlanSearchActivity extends FragmentActivity implements OnGetPoiSearchResultListener,
        MaterialSearchBar.OnSearchActionListener, OnGetSuggestionResultListener, MySuggestionsAdapter.OnItemViewClickListener {

    public static final int UPDATE_SEARCH = 400;
    public static final int DELAYED_TIME = 500;
    MaterialSearchBar mSearchBar;
    RecyclerView mRecyclerView;
    private List<PoiInfo> poiInfos;
    // POI搜索
    private PoiSearch mPoiSearch;
    private SuggestionSearch mSuggestionSearch;
    MySuggestionsAdapter suggestionsAdapter;
    BaseAdapter mAdapter;
    SearchAdapter searchAdapter;
    int loadCount;
    LoadMoreAdapterWrapper.ILoadCallback mCallback;

    private boolean isItemClicked;

    private String startName;
    private String endName;
    private LatLng startPoint;
    private LatLng endPoint;
    private String mCurrentCityName;
    private String mTag;

    ArrayList<String> suggestions = new ArrayList<String>();

    private InputMethodManager inputMethodManager;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_plan_search);
        initView();
        initData();
        initEvent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPoiSearch.destroy();
    }

    private void initView() {
        mSearchBar = findViewById(R.id.searchar_search);
        mRecyclerView = findViewById(R.id.recycler_result);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        suggestionsAdapter = new MySuggestionsAdapter(LayoutInflater.from(getApplicationContext()));
        mSearchBar.clearSuggestions();
        mSearchBar.enableSearch();
        loadCount = 0;
        if (null != getIntent()) {
            startPoint = getIntent().getParcelableExtra("start_point");
            endPoint = getIntent().getParcelableExtra("end_point");
            startName = getIntent().getStringExtra("start_name");
            endName = getIntent().getStringExtra("end_name");
            mCurrentCityName = getIntent().getStringExtra("city_name");
            mTag = getIntent().getStringExtra("tag");
        }
        if (null == inputMethodManager) {
            inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        poiInfos = new ArrayList<PoiInfo>();
        mPoiSearch = PoiSearch.newInstance();
        mSuggestionSearch = SuggestionSearch.newInstance();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        suggestions.add("我的位置");
        suggestionsAdapter.setSuggestions(suggestions);
        mSearchBar.setCustomSuggestionAdapter(suggestionsAdapter);
        isItemClicked = false;
    }

    private void initEvent() {
        mSuggestionSearch.setOnGetSuggestionResultListener(this);
        mPoiSearch.setOnGetPoiSearchResultListener(this);
        mSearchBar.setOnSearchActionListener(this);
        mSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isItemClicked) {
                    mRecyclerView.setVisibility(View.GONE);
                    if (!MapUtil.isNetworkConnected(RoutePlanSearchActivity.this)) {
                        Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_network), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!mSearchBar.getText().isEmpty()) {
                        mSuggestionSearch.requestSuggestion(new SuggestionSearchOption()
                                .city(mCurrentCityName)
                                .keyword(mSearchBar.getText()));
                    }
                }
                isItemClicked = false;
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        suggestionsAdapter.setListener(this);
    }

    @Override
    public void onGetPoiResult(PoiResult poiResult) {
        if (poiResult.getTotalPoiNum() != 0) {
            poiInfos = poiResult.getAllPoi();
            searchAdapter.appendData(poiInfos);
            loadCount++;
            mCallback.onSuccess();
            // 模拟加载到没有更多数据的情况，触发onFailure
            if (poiResult.getTotalPoiNum() < 10) {
                mCallback.onFailure();
            }
        } else if (loadCount == 0) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_result), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {

    }

    @Override
    public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {

    }

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

    }

    @Override
    public void onSearchStateChanged(boolean enabled) {

    }

    @Override
    public void onSearchConfirmed(final CharSequence text) {
        if (!MapUtil.isNetworkConnected(this)) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_network), Toast.LENGTH_SHORT).show();
            return;
        }
        mSearchBar.clearSuggestions();
        mRecyclerView.setVisibility(View.VISIBLE);
        loadCount = 0;
        if (null != inputMethodManager) {// 隐藏软件盘
            inputMethodManager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
        searchAdapter = new SearchAdapter(this);
        searchAdapter.setOnItemClickListener(new SearchAdapter.SearchItemOnClickListener() {
            @Override
            public void onClick(View view, PoiInfo poiInfo, int position) {
                Message message = Message.obtain();
                Bundle bundle = new Bundle();
                if (mTag.equals("start")) {
                    bundle.putString("start_name", poiInfo.getAddress());
                    bundle.putParcelable("start_point", (Parcelable) poiInfo.getLocation());
                    message.what = 1;
                } else if (mTag.equals("end")) {
                    bundle.putString("end_name", poiInfo.getAddress());
                    bundle.putParcelable("end_point", (Parcelable) poiInfo.getLocation());
                    message.what = 2;
                }
                message.setData(bundle);
                if (RoutePlanActivity.routePlanActivityInstance != null &&
                        RoutePlanActivity.routePlanActivityInstance.handler != null) {
                    RoutePlanActivity.routePlanActivityInstance.handler.sendMessage(message);
                }
                overridePendingTransition(0, 0);
                finish();
            }
        });
        //创建装饰者实例，并传入被装饰者和回调接口
        mAdapter = new LoadMoreAdapterWrapper(searchAdapter, new LoadMoreAdapterWrapper.OnLoad() {
            @Override
            public void load(int pagePosition, int pageSize, final LoadMoreAdapterWrapper.ILoadCallback callback) {
                mPoiSearch.searchInCity(new PoiCitySearchOption()
                        .city(mCurrentCityName) // 必填
                        .keyword(String.valueOf(text))// 必填
                        .pageNum(loadCount));
                //数据的处理最终还是交给被装饰的adapter来处理
                mCallback = callback;
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.scheduleLayoutAnimation();//执行Item进入动画
    }

    @Override
    public void onButtonClicked(int buttonCode) {
        Log.d("------", "" + buttonCode);
        mSearchBar.disableSearch();
        if (buttonCode == BUTTON_BACK) {
            finish();
        }
    }

    @Override
    public void onGetSuggestionResult(SuggestionResult suggestionResult) {
        mSearchBar.clearSuggestions();
        if (suggestionResult.getAllSuggestions() != null) {
            suggestions.add("我的位置");
            for (int i = 0; i < suggestionResult.getAllSuggestions().size(); i++) {
                suggestions.add(suggestionResult.getAllSuggestions().get(i).getKey());
                //mSearchBar.setLastSuggestions(suggestions);
                suggestionsAdapter.setSuggestions(suggestions);
                //suggestionsAdapter.notifyDataSetChanged();
                mSearchBar.setCustomSuggestionAdapter(suggestionsAdapter);
                mSearchBar.showSuggestionsList();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            finish();
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void OnItemClickListener(int position, View v) {
        if (position == 0) {
            //发送数据给activity
            Message message = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putString("mtag", mTag);
            message.setData(bundle);
            message.what = 0;
            if (RoutePlanActivity.routePlanActivityInstance != null &&
                    RoutePlanActivity.routePlanActivityInstance.handler != null) {
                RoutePlanActivity.routePlanActivityInstance.handler.sendMessage(message);
            }
            overridePendingTransition(0, 0);
            finish();
        } else {
            isItemClicked = true;
            mSearchBar.setText((String) v.getTag());
            onSearchConfirmed(mSearchBar.getText());
        }
    }
}
