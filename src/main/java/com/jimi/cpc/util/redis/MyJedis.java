package com.jimi.cpc.util.redis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.jimi.cpc.util.SysConfigUtil;

import redis.clients.jedis.AdvancedJedisCommands;
import redis.clients.jedis.BasicCommands;
import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.BitOP;
import redis.clients.jedis.ClusterCommands;
import redis.clients.jedis.DebugParams;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.MultiKeyCommands;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.ScriptingCommands;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;
import redis.clients.util.Slowlog;

public class MyJedis implements JedisCommands,
MultiKeyCommands, AdvancedJedisCommands, ScriptingCommands,
BasicCommands, ClusterCommands, MethodInterceptor{
  private static final Logger log=Logger.getLogger(MyJedis.class);

  public static interface Key{
    
    /**
     * 服务中心
     */
    public static final String SERVER_CENTER=SysConfigUtil.getString("ServerCenterKey");
    
  }
  /**
  * 连接主机
  */
  private   String host=SysConfigUtil.getString("redis.host","172.16.10.105");
 
  /**
  * 连接端口
  */
  private   Integer port=SysConfigUtil.getInt("redis.port",6379);
  /**
  * 连接密码
  */
  private  String password=SysConfigUtil.getString("redis.password");
 
 /**
  * 
  */
 private static Map<String,MyJedis> instMap=new ConcurrentHashMap<String,MyJedis>();
 
 
 private  JedisPool pool;
 
  private  Map<String,Method> methodMap=new HashMap<String,Method>();

  MyJedis() {
  }
  /**
   * 
   * @return
   */
  public static MyJedis getInstance(){
	  return getInstance(Protocol.DEFAULT_DATABASE);
  }
  /**
   * 
   * @param database
   * @return
   */
  public static MyJedis getInstance(int database){
	  //直接读取配置文件
	  return getInstance(database, null, 0,null);
  }
  /**
   *
   */
  public static MyJedis getInstance(int database,String host,int port,String password){
	    
	  //
	  StringBuilder sb=new StringBuilder();
	  if(StringUtils.isNoneBlank(host)){
		  sb.append(host);
	  }
	  if(port>0){
		  sb.append("-");
		  sb.append(port);
	  }
	  sb.append("-");
	  sb.append(database);
	  
	  String key=sb.toString();
	  MyJedis inst=instMap.get(key); 
	  //
      if(null==inst){
        Enhancer enhancer = new Enhancer();  
        enhancer.setSuperclass(MyJedis.class);  
        MyJedis call = new MyJedis();
        //初始化正真的连接池
	  	  if(port>0){
	 		call.port=port;
	 	  }
	 	  if(StringUtils.isNoneBlank(host)){
	  		  call.host=host;
	  	  }
	  	  if(StringUtils.isNoneBlank(password)){
	  		  call.password=password;
	  	  }
        call.init(database);
        enhancer.setCallback(call);  
        inst = (MyJedis) enhancer.create();  
     
        instMap.put(key	, inst);
      }
    return  inst;
  }
  /**
   * 关闭连接�?
   * 
   * @author chengxuwei
   */
  public static void destory(){
 
    	if(instMap.size()>0){
    		for (MyJedis jedis : instMap.values()) {
				jedis.destoryPool();
			}
    	}
	 
   
  }

  private void destoryPool(){
	  pool.destroy();
  }
  /**
   * 初始
   * 
   * @author chengxuwei
   */
  private  void init(int database){
	 
	      JedisPoolConfig config = new JedisPoolConfig();
	      config.setMaxTotal(50); // 最大连接数
	      config.setMaxIdle(10);// 空闲连接数
	      config.setMaxWaitMillis(1000*10);//获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常, 小于零:阻塞不确定的时间,  默认-1
	      config.setTestOnBorrow(false); //在获取连接的时候检查有效性
	      config.setTestOnReturn(false);//返回连接时检查有效性
	      config.setTestWhileIdle(true);//空闲时检查有效性
	      if(StringUtils.isNoneBlank(password)){
	    	  this.pool = new JedisPool(config, host, port,8000,password,database);
	      }else{
	    	  this.pool = new JedisPool(config, host, port,8000,null,database);
	      }
	     log.info("连接到Redis"+host+":"+port);
	      ///redis method 
	      Method[] methods = Jedis.class.getDeclaredMethods();
	      for (Method method : methods) {
	        String name=method.toGenericString().replace(Jedis.class.getName(),MyJedis.class.getName());
	       methodMap.put(name, method);
	     }
  }
  /**
   * 代理方法，调�?Jedis对应的言�?
   * 
   * Jedis没有无参构�?方法无法直接代理 
   */
  @Override
  public Object intercept(Object targetObject, Method method, Object[] args, MethodProxy proxy)  throws Throwable {
	  Object result=null;
      Jedis jedis=null; 
      try {
        jedis = pool.getResource();
        if(!method.getName().equals("finalize")&&!method.getName().equals("time")){
          Method jedisMethod=methodMap.get(method.toGenericString());
        
          if(null!=jedisMethod){
            result = jedisMethod.invoke(jedis, args);
          }else{
            log.error("un implements method:"+method.toGenericString());
          }
          
        }else if(method.getName().equals("time")){
         result=  time();
        }
       
      
      } catch (Exception e) {
        log.error("proxy call jedis error", e);
         //关闭这个连接并释放
        pool.returnBrokenResource(jedis);
         jedis=null;//防止再次释放异常
      } finally {
    	  pool.returnResource(jedis);
      }
    
    return result;  
  }
  /**
   * 
   * @param args
   * @author chengxuwei
   */
  static AtomicInteger count=new AtomicInteger();
  
//  public static void batch(){
//    final  String gps="{''acc':'0','deviceImei':'829153247180000','direction':'318','gateTime':'2015-07-01 18:07:54','gpsInfo':'9','gpsMode':'1','gpsSpeed':'0','gpsTime':'2015-07-01 18:29:58','latitude':23.322552281843645,'longitude':114.33809845958888,'posMethod':'3'}";
//    final ExecutorService threadPool = Executors.newFixedThreadPool(100);
//    ScheduledExecutorService schedulePool = Executors.newScheduledThreadPool(1);
//    for (int i = 0; i < 50;i++) {
//      final int n=i;
//      threadPool.submit(new Runnable() {
//        @Override
//        public void run() {
//       
//          while(true){
////            threadPool.submit(new Runnable() {
////              @Override
////              public void run() {
////                // TODO Auto-generated method stub
////                MyJedis.getInstance().hset("tracker_redis_test_imei", "123451234512345", gps);
////                count.incrementAndGet();
////              }
////            });
//            Jedis jedis = pool.getResource();
//            jedis.hset("tracker_redis_test_imei","12345678901234"+n,gps);
//            pool.returnResource(jedis);
//            count.incrementAndGet();
//          }
//
//         
//        }
//      });
//    }
//    schedulePool.scheduleAtFixedRate(new Runnable() {
//      
//      @Override
//      public void run() {
//        System.out.println("count:"+count.getAndSet(0));
//      }
//    }, 1, 1, TimeUnit.SECONDS);
//    
//  }
  
  

  
  public static void main(String[] args) throws NoSuchMethodException, SecurityException, IOException {

//	  Long n=MyJedis.getInstance(1).hincrBy("test", "abc", 1);
//	  System.out.println(">>>>>>>>"+n);
//    MyJedis.getInstance().del("_test");
//    MyJedis.getInstance().incrBy("_test", 10);
//    System.out.println(  "测试"+ MyJedis.getInstance(1).get("_test"));
//    MyJedis jedis = MyJedis.getInstance();
//    System.out.println(jedis.time());
//    System.out.println(System.currentTimeMillis());
    
//     MyJedis.getInstance().set("test", "abc");
//     System.out.println(   MyJedis.getInstance().get("test"));
//     long time = MyJedis.time();
//     System.out.println(time);
//     Date date=new Date(MyJedis.time());
//     SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//     System.out.println(fmt.format(date));
//     
//     System.out.println(System.currentTimeMillis());
//    batch();
//      back();
//      recover();
  }
  public static void recover() throws IOException{
    
      FileReader fr=new FileReader("d://updateinfo.txt");
      BufferedReader br=new BufferedReader(fr);
      for (String line = br.readLine();  null!=line; line=br.readLine()) {
        String[] kv=line.split("#");
        MyJedis.getInstance(0).hset("SWMS_UPDATE_INFO", kv[0], kv[1]);
        System.out.println("导入"+kv[0]);
     }
      br.close();
      fr.close();
  }
  public static void back() throws IOException{
      Map<String, String> redis = MyJedis.getInstance(0).hgetAll("SWMS_UPDATE_INFO");
      Iterator<Entry<String, String>> ite = redis.entrySet().iterator();
      FileWriter fr=new FileWriter("d://updateinfo.txt");
      BufferedWriter br=new BufferedWriter(fr);
      while(ite.hasNext()){
         Entry<String, String> kv = ite.next();
         String line=kv.getKey()+"#"+kv.getValue()+"\n";
         br.write(line);
      }
      br.close();
      fr.close();
  }
  /**
   * Redis服务器当前时间毫�?
   * 用于并发不高的情况下，不是特别精准的服务器的时间同步
   * @return
   * @author chengxuwei
   */
  public  long time(){
    List<String> list=null;
    Jedis jedis = pool.getResource();
    try {
      if (null != jedis) {
        list = jedis.time();
      }
    } catch (Exception e) {
      log.error("get redis time", e);
    } finally {
    	pool.returnResource(jedis);
    }
    if (null != list&&list.size()>0) {
      // 算成毫秒
      return Long.valueOf(list.get(0));
    }
    return 0;
  }
 

  /**
   * 
   * #############################################################################
   * 只做方法签名，被动态代理到Jedis，如果JedisCommand中有Jedis里没有的的方法，直接把Jedis的方法Copy到当类
   * #############################################################################
   */

  @Override
  public String clusterNodes() {
    
    return null;
  }

  @Override
  public String clusterMeet(String ip, int port) {
    
    return null;
  }

  @Override
  public String clusterAddSlots(int... slots) {
    
    return null;
  }

  @Override
  public String clusterDelSlots(int... slots) {
    
    return null;
  }

  @Override
  public String clusterInfo() {
    
    return null;
  }

  @Override
  public List<String> clusterGetKeysInSlot(int slot, int count) {
    
    return null;
  }

  @Override
  public String clusterSetSlotNode(int slot, String nodeId) {
    
    return null;
  }

  @Override
  public String clusterSetSlotMigrating(int slot, String nodeId) {
    
    return null;
  }

  @Override
  public String clusterSetSlotImporting(int slot, String nodeId) {
    
    return null;
  }

  @Override
  public String clusterSetSlotStable(int slot) {
    
    return null;
  }

  @Override
  public String clusterForget(String nodeId) {
    
    return null;
  }

  @Override
  public String clusterFlushSlots() {
    
    return null;
  }

  @Override
  public Long clusterKeySlot(String key) {
    
    return null;
  }

  @Override
  public Long clusterCountKeysInSlot(int slot) {
    
    return null;
  }

  @Override
  public String clusterSaveConfig() {
    
    return null;
  }

  @Override
  public String clusterReplicate(String nodeId) {
    
    return null;
  }

  @Override
  public List<String> clusterSlaves(String nodeId) {
    
    return null;
  }

  @Override
  public String clusterFailover() {
    
    return null;
  }

  @Override
  public String ping() {
    
    return null;
  }

  @Override
  public String quit() {
    
    return null;
  }

  @Override
  public String flushDB() {
    
    return null;
  }

  @Override
  public Long dbSize() {
    
    return null;
  }

  @Override
  public String select(int index) {
    
    return null;
  }

  @Override
  public String flushAll() {
    
    return null;
  }

  @Override
  public String auth(String password) {
    
    return null;
  }

  @Override
  public String save() {
    
    return null;
  }

  @Override
  public String bgsave() {
    
    return null;
  }

  @Override
  public String bgrewriteaof() {
    
    return null;
  }

  @Override
  public Long lastsave() {
    
    return null;
  }

  @Override
  public String shutdown() {
    
    return null;
  }

  @Override
  public String info() {
    
    return null;
  }

  @Override
  public String info(String section) {
    
    return null;
  }

  @Override
  public String slaveof(String host, int port) {
    
    return null;
  }

  @Override
  public String slaveofNoOne() {
    
    return null;
  }

  @Override
  public Long getDB() {
    
    return null;
  }

  @Override
  public String debug(DebugParams params) {
    
    return null;
  }

  @Override
  public String configResetStat() {
    
    return null;
  }

  @Override
  public Long waitReplicas(int replicas, long timeout) {
    
    return null;
  }

  @Override
  public Object eval(String script, int keyCount, String... params) {
    
    return null;
  }

  @Override
  public Object eval(String script, List<String> keys, List<String> args) {
    
    return null;
  }

  @Override
  public Object eval(String script) {
    
    return null;
  }

  @Override
  public Object evalsha(String script) {
    
    return null;
  }

  @Override
  public Object evalsha(String sha1, List<String> keys, List<String> args) {
    
    return null;
  }

  @Override
  public Object evalsha(String sha1, int keyCount, String... params) {
    
    return null;
  }

  @Override
  public Boolean scriptExists(String sha1) {
    
    return null;
  }

  @Override
  public List<Boolean> scriptExists(String... sha1) {
    
    return null;
  }

  @Override
  public String scriptLoad(String script) {
    
    return null;
  }

  @Override
  public List<String> configGet(String pattern) {
    
    return null;
  }

  @Override
  public String configSet(String parameter, String value) {
    
    return null;
  }

  @Override
  public String slowlogReset() {
    
    return null;
  }

  @Override
  public Long slowlogLen() {
    
    return null;
  }

  @Override
  public List<Slowlog> slowlogGet() {
    
    return null;
  }

  @Override
  public List<Slowlog> slowlogGet(long entries) {
    
    return null;
  }

  @Override
  public Long objectRefcount(String string) {
    
    return null;
  }

  @Override
  public String objectEncoding(String string) {
    
    return null;
  }

  @Override
  public Long objectIdletime(String string) {
    
    return null;
  }

  @Override
  public Long del(String... keys) {
    
    return null;
  }

  @Override
  public List<String> blpop(int timeout, String... keys) {
    
    return null;
  }

  @Override
  public List<String> brpop(int timeout, String... keys) {
    
    return null;
  }

  @Override
  public List<String> blpop(String... args) {
    
    return null;
  }

  @Override
  public List<String> brpop(String... args) {
    
    return null;
  }

  @Override
  public Set<String> keys(String pattern) {
    
    return null;
  }

  @Override
  public List<String> mget(String... keys) {
    
    return null;
  }

  @Override
  public String mset(String... keysvalues) {
    
    return null;
  }

  @Override
  public Long msetnx(String... keysvalues) {
    
    return null;
  }

  @Override
  public String rename(String oldkey, String newkey) {
    
    return null;
  }

  @Override
  public Long renamenx(String oldkey, String newkey) {
    
    return null;
  }

  @Override
  public String rpoplpush(String srckey, String dstkey) {
    
    return null;
  }

  @Override
  public Set<String> sdiff(String... keys) {
    
    return null;
  }

  @Override
  public Long sdiffstore(String dstkey, String... keys) {
    
    return null;
  }

  @Override
  public Set<String> sinter(String... keys) {
    
    return null;
  }

  @Override
  public Long sinterstore(String dstkey, String... keys) {
    
    return null;
  }

  @Override
  public Long smove(String srckey, String dstkey, String member) {
    
    return null;
  }

  @Override
  public Long sort(String key, SortingParams sortingParameters, String dstkey) {
    
    return null;
  }

  @Override
  public Long sort(String key, String dstkey) {
    
    return null;
  }

  @Override
  public Set<String> sunion(String... keys) {
    
    return null;
  }

  @Override
  public Long sunionstore(String dstkey, String... keys) {
    
    return null;
  }

  @Override
  public String watch(String... keys) {
    
    return null;
  }

  @Override
  public String unwatch() {
    
    return null;
  }

  @Override
  public Long zinterstore(String dstkey, String... sets) {
    
    return null;
  }

  @Override
  public Long zinterstore(String dstkey, ZParams params, String... sets) {
    
    return null;
  }

  @Override
  public Long zunionstore(String dstkey, String... sets) {
    
    return null;
  }

  @Override
  public Long zunionstore(String dstkey, ZParams params, String... sets) {
    
    return null;
  }

  @Override
  public String brpoplpush(String source, String destination, int timeout) {
    
    return null;
  }

  @Override
  public Long publish(String channel, String message) {
    
    return null;
  }

  @Override
  public void subscribe(JedisPubSub jedisPubSub, String... channels) {
    
    
  }

  @Override
  public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
    
    
  }

  @Override
  public String randomKey() {
    
    return null;
  }

  @Override
  public Long bitop(BitOP op, String destKey, String... srcKeys) {
    
    return null;
  }

  @Override
  public ScanResult<String> scan(int cursor) {
    
    return null;
  }

  @Override
  public ScanResult<String> scan(String cursor) {
    
    return null;
  }

  @Override
  public String pfmerge(String destkey, String... sourcekeys) {
    
    return null;
  }

  @Override
  public long pfcount(String... keys) {
    
    return 0;
  }

  @Override
  public String set(String key, String value) {
    
    return null;
  }

  @Override
  public String set(String key, String value, String nxxx, String expx, long time) {
    
    return null;
  }

  @Override
  public String get(String key) {
    
    return null;
  }

  @Override
  public Boolean exists(String key) {
    
    return null;
  }

  @Override
  public Long persist(String key) {
    
    return null;
  }

  @Override
  public String type(String key) {
    
    return null;
  }

  @Override
  public Long expire(String key, int seconds) {
    
    return null;
  }

  @Override
  public Long expireAt(String key, long unixTime) {
    
    return null;
  }

  @Override
  public Long ttl(String key) {
    
    return null;
  }

  @Override
  public Boolean setbit(String key, long offset, boolean value) {
    
    return null;
  }

  @Override
  public Boolean setbit(String key, long offset, String value) {
    
    return null;
  }

  @Override
  public Boolean getbit(String key, long offset) {
    
    return null;
  }

  @Override
  public Long setrange(String key, long offset, String value) {
    
    return null;
  }

  @Override
  public String getrange(String key, long startOffset, long endOffset) {
    
    return null;
  }

  @Override
  public String getSet(String key, String value) {
    
    return null;
  }

  @Override
  public Long setnx(String key, String value) {
    
    return null;
  }

  @Override
  public String setex(String key, int seconds, String value) {
    
    return null;
  }

  @Override
  public Long decrBy(String key, long integer) {
    
    return null;
  }

  @Override
  public Long decr(String key) {
    
    return null;
  }

  @Override
  public Long incrBy(String key, long integer) {
    
    return null;
  }

  @Override
  public Long incr(String key) {
    
    return null;
  }

  @Override
  public Long append(String key, String value) {
    
    return null;
  }

  @Override
  public String substr(String key, int start, int end) {
    
    return null;
  }

  @Override
  public Long hset(String key, String field, String value) {
    
    return null;
  }

  @Override
  public String hget(String key, String field) {
    
    return null;
  }

  @Override
  public Long hsetnx(String key, String field, String value) {
    
    return null;
  }

  @Override
  public String hmset(String key, Map<String, String> hash) {
    
    return null;
  }

  @Override
  public List<String> hmget(String key, String... fields) {
    
    return null;
  }

  @Override
  public Long hincrBy(String key, String field, long value) {
    
    return null;
  }

  @Override
  public Boolean hexists(String key, String field) {
    
    return null;
  }

  @Override
  public Long hdel(String key, String... field) {
    
    return null;
  }

  @Override
  public Long hlen(String key) {
    
    return null;
  }

  @Override
  public Set<String> hkeys(String key) {
    
    return null;
  }

  @Override
  public List<String> hvals(String key) {
    
    return null;
  }

  @Override
  public Map<String, String> hgetAll(String key) {
    
    return null;
  }

  @Override
  public Long rpush(String key, String... string) {
    
    return null;
  }

  @Override
  public Long lpush(String key, String... string) {
    
    return null;
  }

  @Override
  public Long llen(String key) {
    
    return null;
  }

  @Override
  public List<String> lrange(String key, long start, long end) {
    
    return null;
  }

  @Override
  public String ltrim(String key, long start, long end) {
    
    return null;
  }

  @Override
  public String lindex(String key, long index) {
    
    return null;
  }

  @Override
  public String lset(String key, long index, String value) {
    
    return null;
  }

  @Override
  public Long lrem(String key, long count, String value) {
    
    return null;
  }

  @Override
  public String lpop(String key) {
    
    return null;
  }

  @Override
  public String rpop(String key) {
    
    return null;
  }

  @Override
  public Long sadd(String key, String... member) {
    
    return null;
  }

  @Override
  public Set<String> smembers(String key) {
    
    return null;
  }

  @Override
  public Long srem(String key, String... member) {
    
    return null;
  }

  @Override
  public String spop(String key) {
    
    return null;
  }

  @Override
  public Long scard(String key) {
    
    return null;
  }

  @Override
  public Boolean sismember(String key, String member) {
    
    return null;
  }

  @Override
  public String srandmember(String key) {
    
    return null;
  }

  @Override
  public Long strlen(String key) {
    
    return null;
  }

  @Override
  public Long zadd(String key, double score, String member) {
    
    return null;
  }

  @Override
  public Long zadd(String key, Map<String, Double> scoreMembers) {
    
    return null;
  }

  @Override
  public Set<String> zrange(String key, long start, long end) {
    
    return null;
  }

  @Override
  public Long zrem(String key, String... member) {
    
    return null;
  }

  @Override
  public Double zincrby(String key, double score, String member) {
    
    return null;
  }

  @Override
  public Long zrank(String key, String member) {
    
    return null;
  }

  @Override
  public Long zrevrank(String key, String member) {
    
    return null;
  }

  @Override
  public Set<String> zrevrange(String key, long start, long end) {
    
    return null;
  }

  @Override
  public Set<Tuple> zrangeWithScores(String key, long start, long end) {
    
    return null;
  }

  @Override
  public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
    
    return null;
  }

  @Override
  public Long zcard(String key) {
    
    return null;
  }

  @Override
  public Double zscore(String key, String member) {
    
    return null;
  }

  @Override
  public List<String> sort(String key) {
    
    return null;
  }

  @Override
  public List<String> sort(String key, SortingParams sortingParameters) {
    
    return null;
  }

  @Override
  public Long zcount(String key, double min, double max) {
    
    return null;
  }

  @Override
  public Long zcount(String key, String min, String max) {
    
    return null;
  }

  @Override
  public Set<String> zrangeByScore(String key, double min, double max) {
    
    return null;
  }

  @Override
  public Set<String> zrangeByScore(String key, String min, String max) {
    
    return null;
  }

  @Override
  public Set<String> zrevrangeByScore(String key, double max, double min) {
    
    return null;
  }

  @Override
  public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
    
    return null;
  }

  @Override
  public Set<String> zrevrangeByScore(String key, String max, String min) {
    
    return null;
  }

  @Override
  public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
    
    return null;
  }

  @Override
  public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
    
    return null;
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
    
    return null;
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
    
    return null;
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset,
      int count) {
    
    return null;
  }

  @Override
  public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
    
    return null;
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
    
    return null;
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
    
    return null;
  }

  @Override
  public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset,
      int count) {
    
    return null;
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset,
      int count) {
    
    return null;
  }

  @Override
  public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset,
      int count) {
    
    return null;
  }

  @Override
  public Long zremrangeByRank(String key, long start, long end) {
    
    return null;
  }

  @Override
  public Long zremrangeByScore(String key, double start, double end) {
    
    return null;
  }

  @Override
  public Long zremrangeByScore(String key, String start, String end) {
    
    return null;
  }

  @Override
  public Long linsert(String key, LIST_POSITION where, String pivot, String value) {
    
    return null;
  }

  @Override
  public Long lpushx(String key, String... string) {
    
    return null;
  }

  @Override
  public Long rpushx(String key, String... string) {
    
    return null;
  }

  @Override
  public List<String> blpop(String arg) {
    
    return null;
  }

  @Override
  public List<String> brpop(String arg) {
    
    return null;
  }

  @Override
  public Long del(String key) {
    
    return null;
  }

  @Override
  public String echo(String string) {
    
    return null;
  }

  @Override
  public Long move(String key, int dbIndex) {
    
    return null;
  }

  @Override
  public Long bitcount(String key) {
    
    return null;
  }

  @Override
  public Long bitcount(String key, long start, long end) {
    
    return null;
  }

  @Override
  public ScanResult<Entry<String, String>> hscan(String key, int cursor) {
    
    return null;
  }

  @Override
  public ScanResult<String> sscan(String key, int cursor) {
    
    return null;
  }

  @Override
  public ScanResult<Tuple> zscan(String key, int cursor) {
    
    return null;
  }

  @Override
  public ScanResult<Entry<String, String>> hscan(String key, String cursor) {
    
    return null;
  }

  @Override
  public ScanResult<String> sscan(String key, String cursor) {
    
    return null;
  }

  @Override
  public ScanResult<Tuple> zscan(String key, String cursor) {
    
    return null;
  }

  @Override
  public Long pfadd(String key, String... elements) {
    
    return null;
  }

  @Override
  public long pfcount(String key) {
    
    return 0;
  }
  



}
