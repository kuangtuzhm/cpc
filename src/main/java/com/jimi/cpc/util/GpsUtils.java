package com.jimi.cpc.util;

import java.util.Random;

public class GpsUtils {
  private static final double EARTH_RADIUS = 6378137;// 赤道半径(单位m)

  /**
   * 转化为弧度(rad)
   * */
  private static double rad(double d) {
    return d * Math.PI / 180.0;
  }
  
  private static double pi = 3.1415926535897932384626;  //元周率
  private static double a = 6378245.0;  //卫星椭球坐标投影到平面地图坐标系的投影因子。
  private static double ee = 0.00669342162296594323;   //ee: 椭球的偏心率。


  /**
   * 基于余弦定理求两经纬度距离
   * 
   * @param lon1 第一点的精度
   * @param lat1 第一点的纬度
   * @param lon2 第二点的精度
   * @param lat2 第二点的纬度
   * @return 返回的距离，单位m
   * */
  public static double distanceCos(double lon1, double lat1, double lon2, double lat2) {
    double radLat1 = rad(lat1);
    double radLat2 = rad(lat2);

    double radLon1 = rad(lon1);
    double radLon2 = rad(lon2);

    if (radLat1 < 0) radLat1 = Math.PI / 2 + Math.abs(radLat1);// south
    if (radLat1 > 0) radLat1 = Math.PI / 2 - Math.abs(radLat1);// north
    if (radLon1 < 0) radLon1 = Math.PI * 2 - Math.abs(radLon1);// west
    if (radLat2 < 0) radLat2 = Math.PI / 2 + Math.abs(radLat2);// south
    if (radLat2 > 0) radLat2 = Math.PI / 2 - Math.abs(radLat2);// north
    if (radLon2 < 0) radLon2 = Math.PI * 2 - Math.abs(radLon2);// west
    double x1 = EARTH_RADIUS * Math.cos(radLon1) * Math.sin(radLat1);
    double y1 = EARTH_RADIUS * Math.sin(radLon1) * Math.sin(radLat1);
    double z1 = EARTH_RADIUS * Math.cos(radLat1);

    double x2 = EARTH_RADIUS * Math.cos(radLon2) * Math.sin(radLat2);
    double y2 = EARTH_RADIUS * Math.sin(radLon2) * Math.sin(radLat2);
    double z2 = EARTH_RADIUS * Math.cos(radLat2);

    double d = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2));
    // 余弦定理求夹角
    double theta =
        Math.acos((EARTH_RADIUS * EARTH_RADIUS + EARTH_RADIUS * EARTH_RADIUS - d * d)
            / (2 * EARTH_RADIUS * EARTH_RADIUS));
    double dist = theta * EARTH_RADIUS;
    return dist;
  }
  /**
   * 计算两经纬度的距离 
   * @param p1
   * @param p2
   * @return
   * @author chengxuwei
   */
  public static double distanceGoogle(Point p1,Point p2) {
     return distanceGoogle(p1.getLng(),p1.getLat(),p2.getLng(),p2.getLat());
  }

  /**
   * 基于googleMap中的算法得到两经纬度之间的距离,计算精度与谷歌地图的距离精度差不多，相差范围在0.2米以下
   * 
   * @param lon1 第一点的精度
   * @param lat1 第一点的纬度
   * @param lon2 第二点的精度
   * @param lat2 第二点的纬度
   * @return 返回的距离，单位M
   * */
  public static double distanceGoogle(double lon1, double lat1, double lon2, double lat2) {
    double radLat1 = rad(lat1);
    double radLat2 = rad(lat2);
    double a = radLat1 - radLat2;
    double b = rad(lon1) - rad(lon2);
    double s =
        2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1)
            * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
    	s = s * EARTH_RADIUS;
    // s = Math.round(s * 10000) / 10000; //返回千米
    return s;
  }
  /**
   * 计算两经纬度的距离 
   * @param p1
   * @param p2
   * @return
   * @author chengxuwei
   */
  public static double distanceCos(Point p1,Point p2) {
    
    return distanceCos(p1.getLng(),p1.getLat(),p2.getLng(),p2.getLat());
  }

  /** 
   * 84 to 火星坐标系 (GCJ-02) World Geodetic System ==> Mars Geodetic System 
   *  
   * @param lat 
   * @param lon 
   * @return 
   */  
  public static Point gps84_To_gcj02(double lat, double lon) {  
      if (outOfChina(lat, lon)) {  
          return null;  
      }  
      double dLat = transformLat(lon - 105.0, lat - 35.0);  
      double dLon = transformLon(lon - 105.0, lat - 35.0);  
      double radLat = lat / 180.0 * pi;  
      double magic = Math.sin(radLat);  
      magic = 1 - ee * magic * magic;  
      double sqrtMagic = Math.sqrt(magic);  
      dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);  
      dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);  
      double mgLat = lat + dLat;  
      double mgLon = lon + dLon;  
      return new Point(mgLon,mgLat);  
  }  

  /** 
   *  火星坐标系 (GCJ-02) to 84 
   * 
   *  @param lon 
   *  @param lat 
   *  @return 
   * 
   * */  
  private static Point gcj02_To_gps84(double lat, double lon) {  
      Point gps = transform(lat, lon);  
      double lontitude = lon * 2 - gps.getLng();  
      double latitude = lat * 2 - gps.getLat();  
      return new Point(lontitude,latitude);  
  }  
  

  private static Point delta (double lat,double lon) {
      // Krasovsky 1940
      //
      // a = 6378245.0, 1/f = 298.3
      // b = a * (1 - f)
      // ee = (a^2 - b^2) / a^2;
      double a = 6378245.0; //  a: 卫星椭球坐标投影到平面地图坐标系的投影因子。
      double ee = 0.00669342162296594323; //  ee: 椭球的偏心率。
      double dLat = transformLat(lon - 105.0, lat - 35.0);
      double dLon = transformLon(lon - 105.0, lat - 35.0);
      double radLat = lat / 180.0 * pi;
      double magic = Math.sin(radLat);
      magic = 1 - ee * magic * magic;
      double sqrtMagic = Math.sqrt(magic);
      dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);
      dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);
//      return {'lat': dLat, 'lon': dLon};
      return new Point(dLon,dLat);
  }
//  GCJ-02 to WGS-84
  private static Point gcj02_wgs84 (double gcjLat,double gcjLon) {
      if (outOfChina(gcjLat, gcjLon))
          return new Point(gcjLon,gcjLat);
      Point d = delta(gcjLat, gcjLon);
      return new Point(gcjLon - d.getLng(),gcjLat - d.getLat());
  }
  //GCJ-02 to WGS-84 exactly
  public static Point  gcj02_decrypt_exact  (double gcjLat, double gcjLon) {
      double initDelta = 0.01;
      double threshold = 0.000000001;
      double dLat = initDelta, dLon = initDelta;
      double mLat = gcjLat - dLat, mLon = gcjLon - dLon;
      double pLat = gcjLat + dLat, pLon = gcjLon + dLon;
      double wgsLat, wgsLon, i = 0;
      while (true) {
          wgsLat = (mLat + pLat) / 2;
          wgsLon = (mLon + pLon) / 2;
          Point tmp = gcj02_wgs84(wgsLat, wgsLon);
          dLat = tmp.getLat() - gcjLat;
          dLon = tmp.getLng() - gcjLon;
          if ((Math.abs(dLat) < threshold) && (Math.abs(dLon) < threshold))
              break;

          if (dLat > 0) pLat = wgsLat; else mLat = wgsLat;
          if (dLon > 0) pLon = wgsLon; else mLon = wgsLon;

          if (++i > 10000) break;
      }
      //console.log(i);
      return new Point(wgsLon,wgsLat);
//      return {'lat': wgsLat, 'lon': wgsLon};
  }

  /** 
   * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 将 GCJ-02 坐标转换成 BD-09 坐标 
   *  
   * @param gg_lat 
   * @param gg_lon 
   */  
  public static Point gcj02_to_bd09(double gg_lat, double gg_lon) {  
      double x = gg_lon, y = gg_lat;  
      double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * pi);  
      double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * pi);  
      double bd_lon = z * Math.cos(theta) + 0.0065;  
      double bd_lat = z * Math.sin(theta) + 0.006;  
      return new Point(bd_lon,bd_lat );  
  }  

  /** 
   *   火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法 
   *   将 BD-09 坐标转换成GCJ-02 坐标
   *  @param  bd_lat 
   *  @param bd_lon 
   *  @return 
   */  
  public static Point bd09_to_gcj02(double bd_lat, double bd_lon) {  
      double x = bd_lon - 0.0065, y = bd_lat - 0.006;  
      double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * pi);  
      double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * pi);  
      double gg_lon = z * Math.cos(theta);  
      double gg_lat = z * Math.sin(theta);  
      return new Point(gg_lon,gg_lat );  
  }  

//  /** 
//   * (BD-09)-->84 
//   * @param bd_lat 
//   * @param bd_lon 
//   * @return 
//   */  
//  public static Point bd09_to_gps84(double bd_lat, double bd_lon) {  
//
//      Point gcj02 = bd09_to_gcj02(bd_lat, bd_lon);  
//      Point map84 = gcj02_To_gps84(gcj02.getLat(), gcj02.getLng());  
//      return map84;  
//
//  } 
  /**
   * GPS坐标转百度坐标 
   * @param gps
   * @return
   * @author chengxuwei
   * 2015年1月20日
   */
  public static Point gpsToBaidu(Point gps){
      Point gcj = gps84_To_gcj02(gps.getLat(), gps.getLng());  
      Point bd= gcj02_to_bd09(gcj.getLat(), gcj.getLng());  
      return bd;
  }
  /**
   * 百度坐标 转GPS坐标
   * @param bd
   * @return
   * @author chengxuwei
   * 2015年1月20日
   */
  public static Point baiduToGps(Point bd){
      Point gcj02 = bd09_to_gcj02(bd.getLat(), bd.getLng());  
      Point map84 = gcj02_To_gps84(gcj02.getLat(), gcj02.getLng());  
      return map84;
  }
  /**
   * Google to Gps点
   * @return
   */
  public static Point googleToGps(Point google){
     Point gps=    gcj02_decrypt_exact(google.getLat(),google.getLng());
     return gps;
  }
  /**
   * Google to Gps点
   * @return
   */
  public static Point gpsToGoogle(Point gps){
      Point gcj = gps84_To_gcj02(gps.getLat(), gps.getLng());  
     return gcj;
  } 
  
  /**
   * 
   * @param lat
   * @param lon
   * @return
   */
  private static boolean outOfChina(double lat, double lon) {  
      if (lon < 72.004 || lon > 137.8347)  
          return true;  
      if (lat < 0.8293 || lat > 55.8271)  
          return true;  
      return false;  
  }  
 /**
  * 
  * @param lat
  * @param lon
  * @return
  */
  private static Point transform(double lat, double lon) {  
      if (outOfChina(lat, lon)) {  
          return new Point(lon,lat);  
      }  
      double dLat = transformLat(lon - 105.0, lat - 35.0);  
      double dLon = transformLon(lon - 105.0, lat - 35.0);  
      double radLat = lat / 180.0 * pi;  
      double magic = Math.sin(radLat);  
      magic = 1 - ee * magic * magic;  
      double sqrtMagic = Math.sqrt(magic);  
      dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * pi);  
      dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * pi);  
      double mgLat = lat + dLat;  
      double mgLon = lon + dLon;  
      return new Point(mgLat, mgLon);  
  }  
 /**
  * 
  * @param x
  * @param y
  * @return
  */
  private static double transformLat(double x, double y) {  
      double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));  
      ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;  
      ret += (20.0 * Math.sin(y * pi) + 40.0 * Math.sin(y / 3.0 * pi)) * 2.0 / 3.0;  
      ret += (160.0 * Math.sin(y / 12.0 * pi) + 320 * Math.sin(y * pi / 30.0)) * 2.0 / 3.0;  
      return ret;  
  }  
  /**
   * 
   * @param x
   * @param y
   * @return
   */
  private static double transformLon(double x, double y) {  
      double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));  
      ret += (20.0 * Math.sin(6.0 * x * pi) + 20.0 * Math.sin(2.0 * x * pi)) * 2.0 / 3.0;  
      ret += (20.0 * Math.sin(x * pi) + 40.0 * Math.sin(x / 3.0 * pi)) * 2.0 / 3.0;  
      ret += (150.0 * Math.sin(x / 12.0 * pi) + 300.0 * Math.sin(x / 30.0 * pi)) * 2.0 / 3.0;  
      return ret;  
  }  
  /**
   * 测试距离计算
   * 
   * @author chengxuwei
   */
  public static void testCal(){
    Random r=new Random();
    double lat = r.nextInt(90);
    double lng=r.nextInt(180);
    long start=System.currentTimeMillis();
    Point p1=new Point(lng,lat);
    for (int i = 0; i < 50000; i++) {
      Point p2=new Point(r.nextInt(90), r.nextInt(180));
      distanceGoogle(p1,p2);
    }
    long dur=System.currentTimeMillis()-start;
    System.out.println("dur :"+dur);
    
  }
  public static void main(String[] args) {

  }
}
