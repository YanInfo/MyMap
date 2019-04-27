/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package sdkdemo.oldif;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;


import com.baidu.navisdk.adapter.BNRouteGuideManager;
import com.baidu.navisdk.adapter.BNRouteGuideManager.OnNavigationListener;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNRoutePlanNode.CoordinateType;
import com.baidu.navisdk.adapter.map.BNItemizedOverlay;
import com.baidu.navisdk.adapter.map.BNOverlayItem;
import com.example.xie.R;

import sdkdemo.EventHandler;

/**
 * 诱导界面
 * 
 * @author sunhao04
 *
 */
public class OldDemoGuideActivity extends Activity {

    private static final String TAG = OldDemoGuideActivity.class.getName();

    private BNRoutePlanNode mBNRoutePlanNode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createHandler();

        View view = BNRouteGuideManager.getInstance().onCreate(this, mOnNavigationListener);

        if (view != null) {
            setContentView(view);
        }

        Intent intent = getIntent();
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                mBNRoutePlanNode = (BNRoutePlanNode)
                        bundle.getSerializable(OldDemoMainActivity.ROUTE_PLAN_NODE);
            }
        }
        // 显示自定义图标
        if (hd != null) {
            hd.sendEmptyMessageAtTime(MSG_SHOW, 5000);
        }

        EventHandler.getInstance().getDialog(this);
        EventHandler.getInstance().showDialog();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BNRouteGuideManager.getInstance().onResume();

    }

    protected void onPause() {
        super.onPause();
        BNRouteGuideManager.getInstance().onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BNRouteGuideManager.getInstance().onDestroy();
        EventHandler.getInstance().disposeDialog();
    }

    @Override
    protected void onStop() {
        super.onStop();
        BNRouteGuideManager.getInstance().onStop();
    }

    /*/
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     * 此处onBackPressed传递false表示强制退出，true表示返回上一级，非强制退出
     */
    @Override
    public void onBackPressed() {
        BNRouteGuideManager.getInstance().onBackPressed(false);
    }

    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        BNRouteGuideManager.getInstance().onConfigurationChanged(newConfig);
    }
    
    
    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        BNRouteGuideManager.getInstance().onStart();
    }

    private void addCustomizedLayerItems() {
        // 需要使用新方法，旧方法不再支持
        BNOverlayItem item =
                new BNOverlayItem(2563047.686035, 1.2695675172607E7, BNOverlayItem.CoordinateType.BD09_MC);
        BNItemizedOverlay overlay = new BNItemizedOverlay(
                getResources().getDrawable(R.drawable.navi_guide_turn));
        overlay.addItem(item);
        overlay.show();
    }

    private static final int MSG_SHOW = 1;
    private static final int MSG_HIDE = 2;
    private static final int MSG_RESET_NODE = 3;
    private Handler hd = null;

    private void createHandler() {
        if (hd == null) {
            hd = new Handler(getMainLooper()) {
                public void handleMessage(android.os.Message msg) {
                    if (msg.what == MSG_SHOW) {
                        addCustomizedLayerItems();
                    } else if (msg.what == MSG_HIDE) {
                        BNRouteGuideManager.getInstance().showCustomizedLayer(false);
                    } else if (msg.what == MSG_RESET_NODE) {
                        BNRouteGuideManager.getInstance().resetEndNodeInNavi(
                                new BNRoutePlanNode(116.21142, 40.85087, "百度大厦11", null, CoordinateType.GCJ02));
                    }
                }
            };
        }
    }

    private OnNavigationListener mOnNavigationListener = new OnNavigationListener() {

        @Override
        public void onNaviGuideEnd() {
            // 退出导航
            finish();
        }

        @Override
        public void notifyOtherAction(int actionType, int arg1, int arg2, Object obj) {
            
            if (actionType == 0) {
                // 导航到达目的地 自动退出
                Log.i(TAG, "notifyOtherAction actionType = " + actionType + ",导航到达目的地！");
                BNRouteGuideManager.getInstance().forceQuitNaviWithoutDialog();
            }

            Log.i(TAG, "actionType:" + actionType + "arg1:" + arg1 + "arg2:" + arg2);
        }
    };
}
