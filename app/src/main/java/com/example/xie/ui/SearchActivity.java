package com.example.xie.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
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
 * 输入目的地，由主页面第一个跳转到的
 */
public class SearchActivity extends FragmentActivity implements OnGetPoiSearchResultListener,
        MaterialSearchBar.OnSearchActionListener, OnGetSuggestionResultListener, MySuggestionsAdapter.OnItemViewClickListener {

    public static final int UPDATE_SEARCH = 400;
    public static final int DELAYED_TIME = 500;
    MaterialSearchBar mSearchBar;
    RecyclerView mRecyclerView;
    private List<PoiInfo> poiInfos;
    private String mCurrentCityName;
    private PoiSearch mPoiSearch;//POI搜索
    private SuggestionSearch mSuggestionSearch;
    MySuggestionsAdapter suggestionsAdapter;
    BaseAdapter mAdapter;
    SearchAdapter searchAdapter;
    int loadCount;
    LoadMoreAdapterWrapper.ILoadCallback mCallback;

    private LatLng startPoint;
    private String startName;
    private boolean isItemClicked;

    ArrayList<String> suggestions = new ArrayList<String>();

    private InputMethodManager inputMethodManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
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

    private void initData() {
        suggestionsAdapter = new MySuggestionsAdapter(LayoutInflater.from(getApplicationContext()));
        mSearchBar.clearSuggestions();
        mSearchBar.enableSearch();
        loadCount = 0;
        if (null != getIntent()) {
            mCurrentCityName = getIntent().getStringExtra("city_name");
            startPoint = getIntent().getParcelableExtra("start_point");
            startName = getIntent().getStringExtra("start_name");
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
                    if (!MapUtil.isNetworkConnected(SearchActivity.this)) {
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
        if (poiResult.error == SearchResult.ERRORNO.NO_ERROR&&poiResult.getTotalPoiNum() != 0) {
            poiInfos = poiResult.getAllPoi();
            searchAdapter.appendData(poiInfos);
            loadCount++;
            mCallback.onSuccess();
            //模拟加载到没有更多数据的情况，触发onFailure
            if (poiResult.getTotalPoiNum() < 10) {
                mCallback.onFailure();
            }

        } else if (poiResult.error == SearchResult.ERRORNO.AMBIGUOUS_KEYWORD) {
            // 当输入关键字在本市没有找到，但在其他城市找到时，返回包含该关键字信息的城市列表
            String strInfo = "在";

            for (CityInfo cityInfo : poiResult.getSuggestCityList()) {
                strInfo += cityInfo.city;
                strInfo += ",";
            }
            strInfo += "找到结果";
            Toast.makeText(SearchActivity.this, "本市内无结果,"+strInfo, Toast.LENGTH_LONG).show();
        }else{
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


    /**
     * 当用户请求调用时
     * @param text
     */
    @Override
    public void onSearchConfirmed(final CharSequence text) {

        mSearchBar.clearSuggestions();
        mRecyclerView.setVisibility(View.VISIBLE);
        loadCount = 0;
        // 隐藏软件盘
        if (null != inputMethodManager) {
            inputMethodManager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
        searchAdapter = new SearchAdapter(this);
        searchAdapter.setOnItemClickListener(new SearchAdapter.SearchItemOnClickListener() {
            @Override
            public void onClick(View view, PoiInfo poiInfo, int position) {
                Intent intent = new Intent(SearchActivity.this, RoutePlanActivity.class);
                intent.putExtra("start_point", startPoint);
                intent.putExtra("end_point", poiInfo.getLocation());
                intent.putExtra("start_name", startName);
                intent.putExtra("end_name", poiInfo.getAddress());
                intent.putExtra("city_name", mCurrentCityName);
                startActivity(intent);
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

    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            finish();
        }
        return true;
    }

    @Override
    public void OnItemClickListener(int position, View v) {
        isItemClicked = true;
        mSearchBar.setText((String) v.getTag());
        onSearchConfirmed(mSearchBar.getText());
    }
}
