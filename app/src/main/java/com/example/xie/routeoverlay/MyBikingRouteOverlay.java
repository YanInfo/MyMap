package com.example.xie.routeoverlay;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.example.xie.R;

/**
 * 骑行Overlay，这里没有用到
 */
public class MyBikingRouteOverlay extends BikingRouteOverlay{
    public MyBikingRouteOverlay(BaiduMap baiduMap) {
        super(baiduMap);
    }
    @Override
    public BitmapDescriptor getStartMarker() {
        return BitmapDescriptorFactory.fromResource(R.mipmap.start_location);

    }

    @Override
    public BitmapDescriptor getTerminalMarker() {
        return BitmapDescriptorFactory.fromResource(R.mipmap.end_location);
    }
}
