/*
 * COPYRIGHT. ShenZhen JiMi Technology Co., Ltd. 2017.
 * ALL RIGHTS RESERVED.
 *
 * No part of this publication may be reproduced, stored in a retrieval system, or transmitted,
 * on any form or by any means, electronic, mechanical, photocopying, recording, 
 * or otherwise, without the prior written permission of ShenZhen JiMi Network Technology Co., Ltd.
 *
 * Amendment History:
 * 
 * Date                   By              Description
 * -------------------    -----------     -------------------------------------------
 * 2017年5月12日    yaojianping         Create the class
 * http://www.jimilab.com/
 */

package com.jimi.cpc.util;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;

/**
 * @FileName ConfigUtil.java
 * @Description:
 *
 * @Date 2017年5月12日 下午2:52:39
 * @author yaojianping
 * @version 1.0
 */
public class SysConfigUtil {

	private static final Logger log = Logger.getLogger(SysConfigUtil.class);
	
	private static Properties props = null;
	
	static {
		// 加载properties配置
		try {
			log.info("load config from:" + SysConfigUtil.class.getClassLoader().getResource("").getPath() + "config.properties");
			InputStream propsStream = SysConfigUtil.class.getClassLoader().getResourceAsStream("config.properties");
			props = new Properties();
			props.load(propsStream);
		} catch (Exception e) {
			log.error("加载config.properties出错，系统错误：" + e);
		}
	}

	private SysConfigUtil() {
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public static String getString(String name) {
		return props.getProperty(name).trim();
	}
	
	public static boolean getBoolean(String name) {
		return Boolean.valueOf(props.getProperty(name));
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public static String getString(String name, String def) {
		String val = props.getProperty(name);
		if (StringUtils.isBlank(val)) {
			val = def;
		}
		return val;
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public static int getInt(String name) {
		String strValue = getString(name);
		return NumberUtils.toInt(strValue, 0);
	}

	/**
	 * 
	 * @param name
	 * @return
	 */
	public static int getInt(String name, int def) {
		String strValue = getString(name);
		return NumberUtils.toInt(strValue, def);
	}
}
