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

package com.jimi.cpc.util.mq;

import java.util.ArrayList;
import java.util.List;

import com.jimi.cpc.util.SysConfigUtil;


/**
 * @FileName MqConstants.java
 * @Description:
 *
 * @Date 2017年5月12日 下午5:29:14
 * @author yaojianping
 * @version 1.0
 */
public class MqConstants {


	public static final String MQ_URL = SysConfigUtil.getString("activemq.url");

	public static final String MQ_USER = SysConfigUtil.getString("activemq.user");

	public static final String MQ_PWD = SysConfigUtil.getString("activemq.pwd");

	public static final String MQ_CONSUMER = "consumer";
	public static final String MQ_PRODUCER = "producer";
	public static final String MQ_CPC = "cpc";
	
	public static final String PROJECT_NAME = "CPC";
}
