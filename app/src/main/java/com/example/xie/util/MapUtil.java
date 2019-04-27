package com.example.xie.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.busline.BusLineResult;
import com.baidu.mapapi.search.core.VehicleInfo;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.utils.DistanceUtil;

import java.text.DecimalFormat;
import java.util.List;
/*
地图工具类
 */
public class MapUtil {
    // 获得两个坐标的距离
    public static String getDistance(LatLng start, LatLng end) {
        // 两点间距离 km，*1000
        double d = DistanceUtil.getDistance(start, end);
        if (d < 1000)
            return (int) (d) + "m";
        else
            return String.format("%.2f", d / 1000) + "km";

    }
    // 修改地图包大小的格式
    public static String formatDataSize(long size) {
        String ret = "";
        if (size < (1024 * 1024)) {
            ret = String.format("%dK", size / 1024);
        } else {
            ret = String.format("%.1fM", size / (1024 * 1024.0));
        }
        return ret;
    }

    // 判断是否有网络连接
    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    // 计算公交距离
    public static String getFriendlyLength(int lenMeter) {
        if (lenMeter > 10000) // 10 km
        {
            int dis = lenMeter / 1000;
            return dis + ChString.Kilometer;
        }

        if (lenMeter > 1000) {
            float dis = (float) lenMeter / 1000;
            DecimalFormat fnum = new DecimalFormat("##0.0");
            String dstr = fnum.format(dis);
            return dstr + ChString.Kilometer;
        }

        if (lenMeter > 100) {
            int dis = lenMeter / 50 * 50;
            return dis + ChString.Meter;
        }

        int dis = lenMeter / 10 * 10;
        if (dis == 0) {
            dis = 10;
        }

        return dis + ChString.Meter;
    }

    // 计算公交耗时
    public static String getFriendlyTime(int second) {
        if (second > 3600) {
            int hour = second / 3600;
            int miniate = (second % 3600) / 60;
            return hour + "小时" + miniate + "分钟";
        }
        if (second >= 60) {
            int miniate = second / 60;
            return miniate + "分钟";
        }
        return second + "秒";
    }

    // 得到公交路线名
    public static String getTransitPathTitle(TransitRouteLine transitRouteLine) {
        if (transitRouteLine == null) {
            return String.valueOf("");
        }
        List<TransitRouteLine.TransitStep> transitSteps = transitRouteLine.getAllStep();
        if (transitSteps == null) {
            return String.valueOf("");
        }
        StringBuffer sb = new StringBuffer();
        for (TransitRouteLine.TransitStep transitStep : transitSteps) {
            StringBuffer title = new StringBuffer();
            if (transitStep.getVehicleInfo() != null) {
                VehicleInfo vehicleInfo = transitStep.getVehicleInfo();
                sb.append(vehicleInfo.getTitle());
                sb.append(" > ");
            }
        }
        return sb.substring(0, sb.length() - 3);
    }

    // 得到公交路线的距离和时间
    public static String getTransitDes(TransitRouteLine transitRouteLine) {
        if (transitRouteLine == null) {
            return String.valueOf("");
        }
        long second = transitRouteLine.getDuration();
        String time = getFriendlyTime((int) second);
        float subDistance = transitRouteLine.getDistance();
        String subDis = getFriendlyLength((int) subDistance);
        return String.valueOf(time + " | " + subDis);
    }
}
