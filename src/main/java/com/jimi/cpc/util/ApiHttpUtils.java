package com.jimi.cpc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;


public class ApiHttpUtils {
  private static final Logger log=Logger.getLogger(ApiHttpUtils.class);
  /**
   * 
   */
  private static String contentType="application/json;charset=UTF-8";
  public static class HttpResult {
	  /**
	   *状态码
	   */
	  private int code;
	  /**
	   * 消息
	   */
	  private String msg;
	  
	  public int getCode() {
	    return code;
	  }
	  public void setCode(int code) {
	    this.code = code;
	  }
	  public String getMsg() {
	    return msg;
	  }
	  public void setMsg(String msg) {
	    this.msg = msg;
	  }


	}

  /**
   * 生成URL串
   * @param params
   * @author chengxuwei
   */
  private  static String encodeUrl(Map<String,Object> params){
    StringBuffer sb=new StringBuffer();
    if(null!=params){
      Iterator<Entry<String, Object>> ite = params.entrySet().iterator();
      while(ite.hasNext()){
        Entry<String, Object> kv = ite.next();
        try {
          String key=kv.getKey();
          String value=URLEncoder.encode(kv.getValue().toString(), "UTF-8");
          sb.append("&");
          sb.append(key);
          sb.append("=");
          sb.append(value);
     
        } catch (UnsupportedEncodingException e) {
          log.error("encode url error", e);
        }
        
      }
    }
   return sb.toString();  
  }
  /**
   * 
   * 
   * @author chengxuwei
   */
  public  static void setContentType(String type){
    contentType=type;
  }
  /**
   * Get请求
   * @param url(可以直接接&)
   * @param params
   * @return
   * @author chengxuwei
   */
  public static  HttpResult  doGet(String httpUrl,Map<String,Object> params,int timeoutMs){
    HttpResult  result=new HttpResult ();
    // 创建url对象
    HttpURLConnection conn=null;
    InputStream is = null;
    BufferedReader reader=null;
    StringBuffer sb=new StringBuffer();
    try {
        String paramUrl=encodeUrl(params);
        URL url = new URL(httpUrl+paramUrl);
        log.debug("requst url:"+url);
        // 打开url连接
        conn = (HttpURLConnection) url.openConnection();
        // 设置url请求方式 ‘get’ 或者 ‘post’
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", contentType);
        conn.setConnectTimeout(timeoutMs);// 连接超时
        conn.setReadTimeout(timeoutMs);//读取超时
        conn.setUseCaches(false);
        //start read
        if(conn.getResponseCode()==HttpURLConnection.HTTP_OK){
          is = conn.getInputStream();
          reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
          String line=null;
          long start=System.currentTimeMillis();
          while ((line=reader.readLine())!=null&&(System.currentTimeMillis()-start)<timeoutMs) {
             sb.append(line);
          }
          result.setMsg(sb.toString());
        }
        result.setCode(conn.getResponseCode());
    }catch (IOException e) {
        result.setCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
        result.setMsg(e.toString());
        log.error("get respone error", e);
    }finally{
        if(null!=is){
            try {
                is.close();
            } catch (IOException e) {
                log.error("close InputStream", e);
            }
        }
        if(null!=reader){
            try {
                reader.close();
            } catch (IOException e) {
                log.error("close reader", e);
            }
        }
        if(null!=conn){
            conn.disconnect();
        } 
    }
    return result;
  }
  /**
   * Post请求
   * @param params
   * @return
   * @author chengxuwei
   */
  public static HttpResult doPost(String httpUrl,Map<String,Object> params,int timeoutMs){
    HttpResult  result=new HttpResult();
    // 创建url对象
    HttpURLConnection conn=null;
    InputStream is = null;
    OutputStream os = null;
    BufferedReader reader=null;
    StringBuffer sb=new StringBuffer();
    try {

        URL url = new URL(httpUrl);
        log.info("requst url:"+url);
        // 打开url连接
        conn = (HttpURLConnection) url.openConnection();
        // 设置url请求方式 ‘get’ 或者 ‘post’
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-type", contentType);
        conn.setConnectTimeout(timeoutMs);// 连接超时
        conn.setReadTimeout(timeoutMs);//读取超时
        conn.setUseCaches(false);//POST缓存无用
        conn.setDoOutput(true);//需要输出body
        os=conn.getOutputStream();
        //write data
//        String json=JSON.toJSONString(params);
        String paramUrl=encodeUrl(params);
        os.write(paramUrl.getBytes());
        os.flush();
        
        //start read
       // if(conn.getResponseCode()==HttpURLConnection.HTTP_OK){
          is = conn.getInputStream();
          reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
          String line=null;
          long start=System.currentTimeMillis();
          while ((line=reader.readLine())!=null&&(System.currentTimeMillis()-start)<timeoutMs) {
             sb.append(line);
          }
          result.setMsg(sb.toString());
      //  }
         result.setCode(conn.getResponseCode());
    }catch (IOException e) {
      result.setCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
      result.setMsg(e.toString());
      log.error("get respone error", e);
    }finally{
        if(null!=is){
            try {
                is.close();
            } catch (IOException e) {
                log.error("close InputStream", e);
            }
        }
        if(null!=os){
          try {
              os.close();
          } catch (IOException e) {
              log.error("close OutputStream", e);
          }
      }
        if(null!=reader){
            try {
                reader.close();
            } catch (IOException e) {
                log.error("close reader", e);
            }
        }
        if(null!=conn){
            conn.disconnect();
        } 
    }
    return result;
  }
  
  /**
   * Post请求
   * @param url
   * @param params
   * @return
   * @author chengxuwei
   */
  public static HttpResult doPostJson(String httpUrl,String json,int timeoutMs){
    HttpResult  result=new HttpResult();
    // 创建url对象
    HttpURLConnection conn=null;
    InputStream is = null;
    OutputStream os = null;
    BufferedReader reader=null;
    StringBuffer sb=new StringBuffer();
    try {

        URL url = new URL(httpUrl);
        log.info("requst url:"+url);
        // 打开url连接
        conn = (HttpURLConnection) url.openConnection();
        // 设置url请求方式 ‘get’ 或者 ‘post’
        conn.setRequestMethod("POST");
//        conn.setRequestProperty("Content-type", contentType);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(timeoutMs);// 连接超时
        conn.setReadTimeout(timeoutMs);//读取超时
        conn.setUseCaches(false);//POST缓存无用
        conn.setDoOutput(true);//需要输出body
        os=conn.getOutputStream();
        //write data
//        
        os.write(json.getBytes());
//        os.write(json.getBytes());
        os.flush();
        
        //start read
       // if(conn.getResponseCode()==HttpURLConnection.HTTP_OK){
          is = conn.getInputStream();
          reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
          String line=null;
          long start=System.currentTimeMillis();
          while ((line=reader.readLine())!=null&&(System.currentTimeMillis()-start)<timeoutMs) {
             sb.append(line);
          }
          result.setMsg(sb.toString());
      //  }
         result.setCode(conn.getResponseCode());
    }catch (IOException e) {
      result.setCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
      result.setMsg(e.toString());
      log.error("get respone error", e);
    }finally{
        if(null!=is){
            try {
                is.close();
            } catch (IOException e) {
                log.error("close InputStream", e);
            }
        }
        if(null!=os){
          try {
              os.close();
          } catch (IOException e) {
              log.error("close OutputStream", e);
          }
      }
        if(null!=reader){
            try {
                reader.close();
            } catch (IOException e) {
                log.error("close reader", e);
            }
        }
        if(null!=conn){
            conn.disconnect();
        } 
    }
    return result;
  }
  
 
  
 
  public static void testLocal(){
	  String url="http://loc.jimicloud.com/loc/query?";
	  Map<String,Object> params=Maps.newConcurrentMap();
	  params.put("token", "3500307a93c6fc335efa71f60438b465");
	  params.put("imei", "123451234512345");
	  params.put("lbs", "460,0,21908,8651,-70|460,0,21908,48091,-80|460,0,21915,6903,-10|460,0,21915,6901,-20");
	HttpResult result = ApiHttpUtils.doGet(url, params, 60000);
	System.out.println(JSON.toJSONString(result));
  }
  
  
  public static void main(String[] args) {
	  testLocal();
	  
  }
  
}
