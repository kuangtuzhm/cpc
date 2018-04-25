package com.jimi.cpc.util.mq;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.Maps;

public class ActiveMQProducer {

	private static final Logger logger = Logger.getLogger(ActiveMQProducer.class);

	/**
	 * 创建Map
	 */
	private static final Map<String, ActiveMQProducer> instMap = Maps.newConcurrentMap();
	/**
     * 
     */
	protected ActiveMQConnectionFactory factory;
	/**
     * 
     */
	private Connection connection;
	/**
	 * 
	 */
	private Session session;
	/**
    * 
    */
	private MessageProducer producer;
	/**
	 * 
	 */
	private String myTarget;

	/**
	 * 发送消息
	 * 
	 * @param data
	 * @return
	 */
	public boolean send(byte[] data) {
		boolean success = false;
		// 如果有生产者
		if (null != producer) {
			try {
				BytesMessage msg = session.createBytesMessage();
				msg.writeBytes(data);
				producer.send(msg);
				success = true;
			} catch (Exception e) {
				logger.error(myTarget + "发送消息失败", e);
			}
		} else {
			logger.error("Producer为空，是否忘记调用startAsync!");
		}

		return success;
	}
	
	/**
	 * 发送消息
	 * 
	 * @param data
	 * @return
	 */
	public boolean send(String msg) {
		boolean success = false;
		// 如果有生产者
		if (null != producer) {
			try {
				TextMessage textMessage = session.createTextMessage(msg);   
				producer.send(textMessage);  
				success = true;
			} catch (Exception e) {
				logger.error(myTarget + "发送消息失败", e);
			}
		} else {
			logger.error("Producer为空，是否忘记调用startAsync!");
		}

		return success;
	}
	
	private static String getMqKey(JMSMode mode, String target) {
		if (StringUtils.isBlank(target)) {
			return mode.getName() + "." + MqConstants.PROJECT_NAME;
		} else {
			return mode.getName() + "." + MqConstants.PROJECT_NAME + "." + target;
		}
	}

	/**
	 * 获取实例 给cpc使用
	 * 
	 * @param target
	 * @param mode
	 * @return
	 */
	public static ActiveMQProducer getInstance(String target, String clientID, JMSMode mode, boolean persistent) {
		if (null != mode && null != target && null != clientID) {
			String mqKey = getMqKey(mode, target);
			String key = mqKey + clientID + mode;
			if (!instMap.containsKey(key)) {
				synchronized (instMap) {
					if (!instMap.containsKey(key)) {
						ActiveMQProducer producer = new ActiveMQProducer();
						producer.init(clientID);
						producer.createProducer(mode, mqKey, persistent);
						instMap.put(key, producer);
					}
				}
			}
			return instMap.get(key);
		} else {
			logger.error("参数错误");
			return null;
		}
	}

	/**
	 * 创建生产者
	 */
	private void createProducer(JMSMode jmsMode, String target, boolean persistent) {
		this.myTarget = jmsMode.toString() + "." + target;

		if (null != session) {
			try {
				Destination dest = null;
				switch (jmsMode) {
				case TOPIC:
					dest = session.createTopic(target);
					break;
				case QUEUE:
					dest = session.createQueue(target);
					break;
				}
				producer = session.createProducer(dest);
				// 持久
				if (persistent) {
					producer.setDeliveryMode(DeliveryMode.PERSISTENT);
				}

			} catch (JMSException e) {
				logger.error(myTarget + "创建失败", e);
			}
		} else {
			logger.error("Session 为空创建生产者失败");
		}
	}

	/**
	 * 初始化
	 * 
	 * @param clientID
	 */
	private void init(String clientID) {

		if (null == connection) {
			try {
				// failover:(tcp://primary:61616)?timeout=3000
				String brokerURL = "failover:(" + MqConstants.MQ_URL + ")";
				logger.info(" ready connect to activemq: " + brokerURL);
				factory = new ActiveMQConnectionFactory(brokerURL);
				factory.setUserName(MqConstants.MQ_USER);
				factory.setPassword(MqConstants.MQ_PWD);
				// ACK配置
				// factory.setOptimizeAcknowledge(true);
				// factory.setUseAsyncSend(true);
				// factory.setUseCompression(true);
				// 预取配置
				Properties props = new Properties();
				props.setProperty("prefetchPolicy.queuePrefetch", "1");
				props.setProperty("prefetchPolicy.queueBrowserPrefetch", "1");
				props.setProperty("prefetchPolicy.durableTopicPrefetch", "100");
				props.setProperty("prefetchPolicy.topicPrefetch", "32766");
				// factory.setProperties(props);
				// // factory.setAlwaysSyncSend(true);
				// // //
				// // factory.setNonBlockingRedelivery(true);
				// // factory.setSendAcksAsync(true);
				// // // 预读
				// // ActiveMQPrefetchPolicy prefetchPolicy = new
				// ActiveMQPrefetchPolicy();
				// // prefetchPolicy.setQueuePrefetch(15000);
				// // factory.setPrefetchPolicy(prefetchPolicy);
				// 重发策略
				RedeliveryPolicy queuePolicy = new RedeliveryPolicy();
				queuePolicy.setInitialRedeliveryDelay(0);
				queuePolicy.setRedeliveryDelay(1000);// 重发延时
				queuePolicy.setUseExponentialBackOff(false);
				queuePolicy.setMaximumRedeliveries(3);// 重发次数
				factory.setRedeliveryPolicy(queuePolicy);
				// 创建连接
				connection = factory.createConnection();
				if (StringUtils.isNotBlank(clientID)) {
					try {
						clientID = getClientIDString(clientID);
						logger.info("connect activemq user ClientID:" + clientID);
						connection.setClientID(clientID);
					} catch (JMSException e) {
						logger.error("削费者客户端ID：" + clientID + "失败");
					}
				}
			} catch (JMSException e) {
				logger.error("ActiveMq创建连接失败", e);
			}
		}
		if (null != connection) {
			if (null == session) {
				try {
					session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				} catch (JMSException e) {
					logger.error("ActiveMq创建会话失败", e);
				}
			}
		} else {
			logger.error("连接为空，无法创建Session");
		}

	}

	/**
	 * 获取客户端ID
	 * 
	 * @return
	 */
	private String getClientIDString(String clientID) {
		String ip = "UnknowHost";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.error("获取本机IP失败", e);
		}
		return clientID + "@" + ip + "-" + System.currentTimeMillis();
	}

	/**
	 * 关闭连接
	 */
	public static void shutdown() {
		Iterator<ActiveMQProducer> ite = instMap.values().iterator();
		ActiveMQProducer inst = null;
		while (ite.hasNext()) {

			inst = ite.next();
			if (null != inst.producer) {
				try {
					inst.producer.close();
				} catch (JMSException e) {
					logger.error(inst.myTarget + "关闭失败", e);
				}
			}
		}
		if (inst == null) {
			return;
		}
		if (null != inst.session) {
			try {
				inst.session.close();
			} catch (JMSException e) {
				logger.error("关闭Session异常", e);
			}
		}
		if (null != inst.connection) {
			try {
				inst.connection.close();
			} catch (JMSException e) {
				logger.error("关闭Connect异常", e);
			}
		}
	}
}
