package com.jimi.cpc.util.redis;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.jimi.cpc.model.ImeiApp;
import com.jimi.cpc.util.Constants;

public class RedisTest {

	public static void main(String[] args) {
		//868111111111100   868111111111111  868111111111112  868111111111110
		RedisTest t = new RedisTest();
		String imei="868111111111110";
		String imeiAppId = t.get(imei);
		System.out.println(imeiAppId);
		if(StringUtils.isEmpty(imeiAppId))
		{
			t.set(imei);
		}
	}

	public String get(String imei)
	{
		String imeiAppId = MyJedis.getInstance(0).hget(Constants.DC_IMEI_APPID, imei);
		return imeiAppId;
	}
	
	public void set(String imei)
	{
		ImeiApp info = new ImeiApp();
		info.setAppId("TUQIANG");
		info.setMcType("ET200");
		info.setUserId("75354");
		String value = JSONObject.toJSONString(info);
		MyJedis.getInstance(0).hset(Constants.DC_IMEI_APPID, imei, value);
	}
}
