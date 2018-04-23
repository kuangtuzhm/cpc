package com.jimi.cpc.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author  yuanshao
 * 此类不能直接作为公共工具类，需优化
 */
public class PropertiesUtils {

	private static final Logger logger = LoggerFactory.getLogger(PropertiesUtils.class);
	private Properties properties;
	public String config_file = "/param.properties";
	public PropertiesUtils(String path){
		super();
		InputStream configIn =null;
		try {
			if (path==null||path.trim().length()==0){
				configIn = PropertiesUtils.class.getResourceAsStream(config_file);
			}else{
				File configFile = new File(path);
				if (configFile.exists()) {
					configIn= new FileInputStream(configFile);
				}
			}
			properties = new Properties();
			if (configIn!=null)
				properties.load(configIn);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("error=", e);
		} finally {
			if (configIn != null) {
				try {
					configIn.close();
				} catch (IOException e) {
					logger.error("error", e);
				}
			}
		}
	}

	/**
	 * 
	 * 根据key获取value 根据key获取config.properties里的值
	 * 
	 * @param key
	 *            键值
	 * @return value
	 * @see [PropertiesUtils、PropertiesUtils#getValue]
	 */
	public  String get(String key) {
		return properties.getProperty(key);
	}
}
