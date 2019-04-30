/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package sdkdemo.oldif;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.navisdk.adapter.BNCommonSettingParam;
import com.baidu.navisdk.adapter.BNOuterTTSPlayerCallback;
import com.baidu.navisdk.adapter.BNRoutePlanNode;
import com.baidu.navisdk.adapter.BNRoutePlanNode.CoordinateType;
import com.baidu.navisdk.adapter.BNaviSettingManager;
import com.baidu.navisdk.adapter.BaiduNaviManager;
import com.baidu.navisdk.adapter.BaiduNaviManager.NaviInitListener;
import com.baidu.navisdk.adapter.BaiduNaviManager.RoutePlanListener;
import com.baidu.navisdk.adapter.BaiduNaviManagerFactory;
import com.example.xie.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import sdkdemo.EventHandler;
import sdkdemo.NormalUtils;

public class OldDemoMainActivity extends Activity {

    private static final String APP_FOLDER_NAME = "BNSDKSimpleDemo";

    private Button mWgsNaviBtn = null;
    private Button mGcjNaviBtn = null;
    private Button mBdmcNaviBtn = null;
    private Button mDb06ll = null;
    private Button mGotoSettingsBtn = null;
    private String mSDCardPath = null;

    static final String ROUTE_PLAN_NODE = "routePlanNode";

    private static final String[] authBaseArr = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private static final int authBaseRequestCode = 1;

    private boolean hasInitSuccess = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.demo_normal_activity_main);

        mWgsNaviBtn = (Button) findViewById(R.id.wgsNaviBtn);
        mGcjNaviBtn = (Button) findViewById(R.id.gcjNaviBtn);
        mBdmcNaviBtn = (Button) findViewById(R.id.bdmcNaviBtn);
        mDb06ll = (Button) findViewById(R.id.mDb06llNaviBtn);
        mGotoSettingsBtn = (Button) findViewById(R.id.mGotoSettingsBtn);
        initListener();
        if (initDirs()) {
            initNavi();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initListener() {
        if (mWgsNaviBtn != null) {
            mWgsNaviBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    if (BaiduNaviManager.isNaviInited()) {
                        routeplanToNavi(CoordinateType.WGS84);
                    }
                }

            });
        }
        if (mGcjNaviBtn != null) {
            mGcjNaviBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                    if (BaiduNaviManager.isNaviInited()) {
                        routeplanToNavi(CoordinateType.GCJ02);
                    }
                }

            });
        }
        if (mBdmcNaviBtn != null) {
            mBdmcNaviBtn.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {

                    if (BaiduNaviManager.isNaviInited()) {
                        routeplanToNavi(CoordinateType.BD09_MC);
                    }
                }
            });
        }

        if (mDb06ll != null) {
            mDb06ll.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (BaiduNaviManager.isNaviInited()) {
                        routeplanToNavi(CoordinateType.BD09LL);
                    }
                }
            });
        }

        if (mGotoSettingsBtn != null) {
            mGotoSettingsBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BaiduNaviManagerFactory.getBaiduNaviManager().isInited()) {
                        NormalUtils.gotoSettings(OldDemoMainActivity.this);
                    }
                }
            });
        }
    }

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

    /**
     * 内部TTS播报状态回传handler
     */
    private Handler ttsHandler = new Handler() {
        public void handleMessage(Message msg) {
            int type = msg.what;
            switch (type) {
                case BaiduNaviManager.TTSPlayMsgType.PLAY_START_MSG: {
                    Log.e("test_TTS", "PLAY_START_MSG");
                    break;
                }
                case BaiduNaviManager.TTSPlayMsgType.PLAY_END_MSG: {
                    Log.e("test_TTS", "PLAY_END_MSG");
                    break;
                }
                default:
                    break;
            }
        }
    };

    /**
     * 内部TTS播报状态回调接口
     */
    private BaiduNaviManager.TTSPlayStateListener ttsPlayStateListener = new BaiduNaviManager.TTSPlayStateListener() {

        @Override
        public void playEnd() {
            Log.e("test_TTS", "playEnd");
        }

        @Override
        public void playStart() {
            Log.e("test_TTS", "playStart");
        }
    };

    private boolean hasBasePhoneAuth() {
        // TODO Auto-generated method stub

        PackageManager pm = this.getPackageManager();
        for (String auth : authBaseArr) {
            if (pm.checkPermission(auth, this.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void initNavi() {
        // 申请权限
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            if (!hasBasePhoneAuth()) {
                this.requestPermissions(authBaseArr, authBaseRequestCode);
                return;
            }
        }

        BaiduNaviManager.getInstance().init(this, mSDCardPath, APP_FOLDER_NAME, new NaviInitListener() {
            @Override
            public void onAuthResult(int status, String msg) {
                String result;
                if (0 == status) {
                    result = "key校验成功!";
                } else {
                    result = "key校验失败, " + msg;
                }
                Toast.makeText(OldDemoMainActivity.this, result, Toast.LENGTH_LONG).show();
            }

            public void initSuccess() {
                Toast.makeText(OldDemoMainActivity.this, "百度导航引擎初始化成功", Toast.LENGTH_SHORT).show();
                hasInitSuccess = true;
                initSetting();
            }

            public void initStart() {
                Toast.makeText(OldDemoMainActivity.this, "百度导航引擎初始化开始", Toast.LENGTH_SHORT).show();
            }

            public void initFailed() {
                Toast.makeText(OldDemoMainActivity.this, "百度导航引擎初始化失败", Toast.LENGTH_SHORT).show();
            }

        }, null, ttsHandler, ttsPlayStateListener);

    }

    private String getSdcardDir() {
        if (Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().toString();
        }
        return null;
    }

    private void routeplanToNavi(int coType) {
        if (!hasInitSuccess) {
            Toast.makeText(OldDemoMainActivity.this, "还未初始化!", Toast.LENGTH_SHORT).show();
        }

        BNRoutePlanNode sNode = new BNRoutePlanNode(116.30142, 40.05087, "百度大厦", "百度大厦", coType);
        BNRoutePlanNode eNode = new BNRoutePlanNode(116.39750, 39.90882, "北京天安门", "北京天安门", coType);
        switch (coType) {
            case CoordinateType.GCJ02: {
                sNode = new BNRoutePlanNode(116.30142, 40.05087, "百度大厦", "百度大厦", coType);
                eNode = new BNRoutePlanNode(116.39750, 39.90882, "北京天安门", "北京天安门", coType);
                break;
            }
            case CoordinateType.WGS84: {
                sNode = new BNRoutePlanNode(116.300821, 40.050969, "百度大厦", "百度大厦", coType);
                eNode = new BNRoutePlanNode(116.397491, 39.908749, "北京天安门", "北京天安门", coType);
                break;
            }
            case CoordinateType.BD09_MC: {
                sNode = new BNRoutePlanNode(12947471, 4846474, "百度大厦", "百度大厦", coType);
                eNode = new BNRoutePlanNode(12958160, 4825947, "北京天安门", "北京天安门", coType);
                break;
            }
            case CoordinateType.BD09LL: {
                sNode = new BNRoutePlanNode(116.30784537597782, 40.057009624099436, "百度大厦", "百度大厦", coType);
                eNode = new BNRoutePlanNode(116.40386525193937, 39.915160800132085, "北京天安门", "北京天安门", coType);
                break;
            }
            default:
                break;
        }
        List<BNRoutePlanNode> list = new ArrayList<BNRoutePlanNode>();
        list.add(sNode);
        list.add(eNode);

        // 开发者可以使用旧的算路接口，也可以使用新的算路接口,可以接收诱导信息等
        BaiduNaviManager.getInstance().launchNavigator(this, list, 1, true, new DemoRoutePlanListener(sNode),
                eventListerner);
    }

    BaiduNaviManager.NavEventListener eventListerner = new BaiduNaviManager.NavEventListener() {

        @Override
        public void onCommonEventCall(int what, int arg1, int arg2, Bundle bundle) {
            EventHandler.getInstance().handleNaviEvent(what, arg1, arg2, bundle);
        }
    };

    public class DemoRoutePlanListener implements RoutePlanListener {

        private BNRoutePlanNode mBNRoutePlanNode = null;

        public DemoRoutePlanListener(BNRoutePlanNode node) {
            mBNRoutePlanNode = node;
        }

        @Override
        public void onJumpToNavigator() {
            Intent intent = new Intent(OldDemoMainActivity.this, OldDemoGuideActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable(ROUTE_PLAN_NODE, (BNRoutePlanNode) mBNRoutePlanNode);
            intent.putExtras(bundle);
            startActivity(intent);
        }

        @Override
        public void onRoutePlanFailed() {
            // TODO Auto-generated method stub
            Toast.makeText(OldDemoMainActivity.this, "算路失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void initSetting() {
        BNaviSettingManager
                .setShowTotalRoadConditionBar(BNaviSettingManager.PreViewRoadCondition.ROAD_CONDITION_BAR_SHOW_ON);
        BNaviSettingManager.setVoiceMode(BNaviSettingManager.VoiceMode.Veteran);
        BNaviSettingManager.setRealRoadCondition(BNaviSettingManager.RealRoadCondition.NAVI_ITS_ON);
        BNaviSettingManager.setIsAutoQuitWhenArrived(true);
        Bundle bundle = new Bundle();
        // 必须设置APPID，否则会静音
        bundle.putString(BNCommonSettingParam.TTS_APP_ID, NormalUtils.getTTSAppID());
        BNaviSettingManager.setNaviSdkParam(bundle);
    }

    private BNOuterTTSPlayerCallback mTTSCallback = new BNOuterTTSPlayerCallback() {

        @Override
        public void stopTTS() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "stopTTS");
        }

        @Override
        public void resumeTTS() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "resumeTTS");
        }

        @Override
        public void releaseTTSPlayer() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "releaseTTSPlayer");
        }

        @Override
        public int playTTSText(String speech, int bPreempt) {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "playTTSText" + "_" + speech + "_" + bPreempt);

            return 1;
        }

        @Override
        public void phoneHangUp() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "phoneHangUp");
        }

        @Override
        public void phoneCalling() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "phoneCalling");
        }

        @Override
        public void pauseTTS() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "pauseTTS");
        }

        @Override
        public void initTTSPlayer() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "initTTSPlayer");
        }

        @Override
        public int getTTSState() {
            // TODO Auto-generated method stub
            Log.e("test_TTS", "getTTSState");
            return 1;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == authBaseRequestCode) {
            for (int ret : grantResults) {
                if (ret == 0) {
                    continue;
                } else {
                    Toast.makeText(OldDemoMainActivity.this, "缺少导航基本的权限!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            initNavi();
        }
    }
}
