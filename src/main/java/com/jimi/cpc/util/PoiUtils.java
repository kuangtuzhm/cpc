package com.jimi.cpc.util;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;

public class PoiUtils {
    private static final Logger log=Logger.getLogger(PoiUtils.class);
    private static final String url="http://poi.jimicloud.com/poi?_method_=geocoder";
    private static final String token="3500307a93c6fc335efa71f60438b465";
    /**
     * POI查询线程池
     */
    
    private static BlockingQueue<Runnable> poiBlockQueue= new LinkedBlockingQueue<Runnable>();
    private static ThreadPoolExecutor poiFixThreadPool=new ThreadPoolExecutor(50, 50, 0, TimeUnit.MILLISECONDS, poiBlockQueue);

    static{
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                poiFixThreadPool.shutdown();
            }
        });
    }
    /**
     * 
     * @author chengxuwei
     *
     */
    public static interface Future{
        
        public void complete(String address);
    }
    /**
     * 查询地址信息
     * @param lat
     * @param lng
     * @return
     */
    public static String queryPoi(final double lat,final double lng){
        String address=geocode(lat+","+lng,null);
        return address;
    }
    /**
     * 
     * @param lat
     * @param lng
     * @param future
     */
    public static void queryPoiAsync(final double lat,final double lng, final Future future){
           int maxSize=100000;
           //队列超限直接放弃
           if(poiBlockQueue.size()<maxSize){
               //异步回调
               poiFixThreadPool.submit(new Runnable() {
                
                @Override
                public void run() {
                    if(null != future){
//                        String address=BaiduApiUtils.getlocation(lng, lat);
                        String address=geocode(lat+","+lng,null);
                        if(StringUtils.isNoneBlank(address)){
                            future.complete(address);
                        }else{
                            log.info("lat,lng:"+lat+","+lng+"没有找到POI信息");
                        }
                    }
                }
            });
           }else{
               //放弃POI查询
               log.error("POI查询队列已满"+maxSize+"，放弃查询!");
           }
    }
    /**
     * 地址解析
     * @param latlng
     * @param language
     * @return
     */
    public static String geocode(String latlng,String language){
        String addr=null;
        Map<String, Object> params = Maps.newConcurrentMap();
        params.put("token", token);
        params.put("latlng", latlng);
        if(StringUtils.isNotBlank(language)){
            params.put("language", language);
        }else{
            params.put("language", "zh");
        }
      
       ApiHttpUtils.HttpResult result = ApiHttpUtils.doGet(url, params, 5000);
        if(HttpURLConnection.HTTP_OK==result.getCode()){
            String msg=result.getMsg();
            if(StringUtils.isNotBlank(msg)){
               JSONObject json = JSON.parseObject(msg);
               if(0==json.getInteger("code")){
                   addr=json.getString("msg");
               }
            }
        }
        
        return addr;
    }
    public static String geocode2(String url,String token,String latlng){
        String addr=null;
        Map<String, Object> params = Maps.newConcurrentMap();
        params.put("token", token);
        params.put("latlng", latlng);
        params.put("language", "zh");

        ApiHttpUtils.HttpResult result = ApiHttpUtils.doGet(url, params, 5000);
        if(HttpURLConnection.HTTP_OK==result.getCode()){
            String msg=result.getMsg();
            if(StringUtils.isNotBlank(msg)){
                JSONObject json = JSON.parseObject(msg);
                if(0==json.getInteger("code")){
                    addr=json.getString("msg");
                }
            }
        }

        return addr;
    }
    private static int count;
   
    public static void main(String[] args) {
//        long start=System.currentTimeMillis();
//        for (int i = 0; i <100; i++) {
//            PoiUtils.queryPoiAsync(22.576241666666668,113.91679166666667, new Future() {
//
//                @Override
//                public void complete(String address) {
//
//                    count++;
//                }
//            });
//        }
        String latlng="22.584071,113.879724";
        System.out.println(PoiUtils.geocode(latlng,null));
//        long dur=System.currentTimeMillis()-start;
//        System.out.println("用时(ms):"+dur);
//        String addr=geocode("22.576241666666668,113.91679166666667","zh");
//        System.out.println(addr);
    }
}
