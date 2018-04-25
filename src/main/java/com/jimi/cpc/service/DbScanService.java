package com.jimi.cpc.service;

import com.jimi.cpc.dao.EsDao;
import com.jimi.cpc.dao.MysqlDao;
import com.jimi.cpc.dbscan.Cluster;
import com.jimi.cpc.dbscan.DBScan;
import com.jimi.cpc.dbscan.Point;
import com.jimi.cpc.util.DateUtil;
import com.jimi.cpc.util.GpsUtils;
import com.jimi.cpc.util.PoiUtils;
import com.jimi.cpc.util.SysConfigUtil;
import com.jimi.cpc.util.mq.ActiveMQProducer;
import com.jimi.cpc.util.mq.JMSMode;
import com.jimi.cpc.util.mq.MqConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class DbScanService {
	
    private static final Logger log = LoggerFactory.getLogger(DbScanService.class);
    
    
    private static ActiveMQProducer activation_producer = ActiveMQProducer.getInstance(MqConstants.MQ_CPC, MqConstants.MQ_PRODUCER,
			JMSMode.QUEUE, true);
    
    private DBScan dbScan;
    private EsDao esDao;
    private String index = null;
    private String type = null;
    private int threshold;
    private String compareStartTime = null;
    private String compareEndTime = null;
    private String geoUrl = null;
    private String geoToken = null;
    private int radius;
    private int batchNum = 2000;
    
    //    private DecimalFormat df=new DecimalFormat(".#######");
    public DbScanService() {
        this.radius = SysConfigUtil.getInt("dbscan.radius");
        int minPts = SysConfigUtil.getInt("dbscan.minPts");
        String month = SysConfigUtil.getString("compare_range_month");
        if (month != null) {
            int compareMonth = Integer.parseInt(month.trim());
            this.compareStartTime = DateUtil.getMonthBefore(compareMonth);
            this.compareEndTime = DateUtil.getTimeNow();
        }
        this.threshold = SysConfigUtil.getInt("gps.center.threshold");
        this.dbScan = new DBScan(radius, minPts);
        this.esDao = new EsDao();
        this.index = SysConfigUtil.getString("es.index.name");
        this.type = SysConfigUtil.getString("es.index.type");
        this.geoUrl = SysConfigUtil.getString("geocoder.url");
        this.geoToken = SysConfigUtil.getString("geocoder.token");
        this.batchNum = SysConfigUtil.getInt("manual.cpc.batchNum");
    }

    public static void main(String[] args) throws Exception {
        DbScanService dbScanServie = new DbScanService();
        dbScanServie.task();
    }

    public void task() throws Exception {
        long beginTime = System.currentTimeMillis();
        TransferQueue<Map<String, List<String>>> queue = new LinkedTransferQueue<>();
        int threadNum = SysConfigUtil.getInt("dbscan.handleThread.num");
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        CountDownLatch latch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++) {
            executor.execute(new HandleData(queue, latch));
        }
        MysqlDao dao = new MysqlDao();
        int day_num = SysConfigUtil.getInt("seach_day_num");
        dao.search(queue, threadNum,day_num,batchNum);
        latch.await();
        esDao.close();
        executor.shutdown();
        activation_producer.shutdown();
        activation_producer = null;
        log.info("-----dbscan all hadle finish-----task time: " + (System.currentTimeMillis() - beginTime));
    }

    class HandleData implements Runnable {
        private TransferQueue<Map<String, List<String>>> queue;
        private CountDownLatch latch;

        public HandleData(TransferQueue<Map<String, List<String>>> queue, CountDownLatch latch) {
            this.queue = queue;
            this.latch = latch;
        }

        @Override
        public void run() {
            try {
                long beginTime = System.currentTimeMillis();
                while (true) {
                    Map<String, List<String>> line = queue.poll(5, TimeUnit.SECONDS);
                    if (line != null) {
                        if (line.keySet().contains("finish")) {
                            break;
                        }
                        handle(line);
                    }
                }
                latch.countDown();
                log.info(Thread.currentThread() + "--finish---task time:" + (System.currentTimeMillis() - beginTime));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param mysqlData 是一个imei的包括mysql数据
     */
    public void handle(Map<String, List<String>> mysqlData) {
        for (String imei : mysqlData.keySet()) {
            List<String> dataList = mysqlData.get(imei);
            System.out.println(imei + ":" + dataList.size());
            List<Point> pointList = new ArrayList<>();
            for (String line : dataList) {
                String[] row = line.split("\t");
                int index = 0;
                String id = row[index++];
                String device_imei = row[index++];
                String macs = row[index++];
                double lat = Double.parseDouble(row[index++]);
                double lng = Double.parseDouble(row[index++]); 
                int hour   = Integer.parseInt(row[index++].substring(11, 13));
                //id,device_imei,macs,lat,lng
                Point point = new Point(device_imei, macs, lng, lat , hour);
                pointList.add(point);
            }
            dbScan.process(pointList);
            List<Cluster> clusters = dbScan.getCluster(pointList);
            Iterator<Cluster> clusterIt = clusters.iterator();
            //合并各聚类macs
            while(clusterIt.hasNext()){
            	Cluster cluster = clusterIt.next();
           /* }
            for (Cluster cluster : clusters) {*/
                List<Point> points = cluster.getPlist();
                String macs = "";
                int belongMon = 0;  //属于早上的点
                int belongNig = 0;	//属于晚上的点
                for (Point point : points) {
                    String tmp_mac = point.getMacs();
                    macs += "|" + tmp_mac;
                    int hour = point.getHour();
                    if(8 <= hour && 12 >= hour){
                    	belongMon ++;
                    }else if(18 <= hour && 21 >= hour){
                    	belongNig ++;
                    }
                }
                float monPer = (float)belongMon / points.size();
                float nigPer = (float)belongNig / points.size();
                /*if(monPer < 0.6 && nigPer < 0.6){
                	clusterIt.remove();
                	continue;
                }*/
                cluster.setType(nigPer > monPer ? 0 :1);  //0 家 1 学校
                log.info("早上百分比："+monPer+" 晚上百分比："+nigPer);
                cluster.setCombineMacs(transMacs(macs.substring(1)));
            }
            if (clusters.size() > 0) {
                //---验证是否存在考勤点，如果存在更新，否则新加
                List<Map<String, Object>> esDataList = new ArrayList<>();
                List<Map<String, Object>> historyDatas = esDao.search(index, type, imei, compareStartTime, compareEndTime);
                for (Cluster cluster : clusters) {
                    Point mean = cluster.getMean();
                    Map<String, Object> similarityData = null;
                    
                    int exists_similar_point = 0;//0不存在聚类点
                    //验证是否存在相似聚类点
                    for (Map<String, Object> row : historyDatas) {
                        double lng = Double.parseDouble(row.get("lng").toString());
                        double lat = Double.parseDouble(row.get("lat").toString());
                        double distance = GpsUtils.distanceGoogle(mean.getX(), mean.getY(), lng, lat);
                        //说明已存在聚类点
                        if (distance < threshold) {
                            similarityData = row;
                            //break;//delete by wg.he 11/01/2017,只更新一个考勤点的mac地址
                            
                            
                            //add by wg.he 11/01/2017,imei的每个考勤点的MAC地址(es)都要被append update
                            exists_similar_point = 1;
                            if (similarityData.get("is_valid").toString().equals("0")) { 
                                similarityData.put("lng", mean.getX());
                                similarityData.put("lat", mean.getY());
                                String latlng = mean.getY() + "," + mean.getX();
                                String gname = PoiUtils.geocode2(geoUrl, geoToken, latlng);
                                if (gname != null){
                                    similarityData.put("gname", gname);
                                }
                                similarityData.put("type", cluster.getType());

                            }
                            similarityData.put("counts", cluster.getPlist().size());
                            similarityData.put("update_time", DateUtil.getTimeNow());
                            String macs = cluster.getCombineMacs();
                            String es_macs = similarityData.get("macs").toString();
                            similarityData.put("macs", mergeMacs(macs, es_macs));
                            esDao.update(index, type, similarityData);
                        }
                    }
                    
                    
                    /*
                    if (similarityData != null) {
                        if (similarityData.get("is_valid").toString().equals("0")) {
                            similarityData.put("lng", mean.getX());
                            similarityData.put("lat", mean.getY());
                            String latlng = mean.getY() + "," + mean.getX();
                            String gname = PoiUtils.geocode2(geoUrl, geoToken, latlng);
                            if (gname != null)
                                similarityData.put("gname", gname);

                        }
                        similarityData.put("counts", cluster.getPlist().size());
                        similarityData.put("update_time", DateUtil.getTimeNow());
                        String macs = cluster.getCombineMacs();
                        String es_macs = similarityData.get("macs").toString();
                        similarityData.put("macs", mergeMacs(macs, es_macs));
                        esDao.update(index, type, similarityData);
                    } else {
                        Map<String, Object> es_data = new HashMap<>();
                        es_data.put("id", UUID.randomUUID().toString());
                        es_data.put("imei", imei);
                        es_data.put("lng", mean.getX());
                        es_data.put("lat", mean.getY());
                        es_data.put("macs", cluster.getCombineMacs());
                        String gid = uuid();
//                        es_data.put("gid", cluster.getNum());
                        es_data.put("gid", gid);
                        es_data.put("is_valid", 0);
                        es_data.put("model", 0);
                        es_data.put("counts", cluster.getPlist().size());
                        es_data.put("update_time", DateUtil.getTimeNow());
                        es_data.put("create_time", DateUtil.getTimeNow());
                        es_data.put("radius",radius);
                        String latlng = mean.getY() + "," + mean.getX();
                        String gname = PoiUtils.geocode2(geoUrl, geoToken, latlng);
                        if (gname != null)
                            es_data.put("gname", gname);
                        esDataList.add(es_data);
                    }
                    */
                    
                    
                    //add by wg.he 11/01/2017
                    if(exists_similar_point == 0){
                    	Map<String, Object> es_data = new HashMap<>();
                        es_data.put("id", UUID.randomUUID().toString());
                        es_data.put("imei", imei);
                        es_data.put("lng", mean.getX());
                        es_data.put("lat", mean.getY());
                        es_data.put("macs", cluster.getCombineMacs());
                        String gid = uuid();
//                        es_data.put("gid", cluster.getNum());
                        es_data.put("gid", gid);
                        es_data.put("is_valid", 0);
                        es_data.put("model", 0);
                        es_data.put("counts", cluster.getPlist().size());
                        es_data.put("update_time", DateUtil.getTimeNow());
                        es_data.put("create_time", DateUtil.getTimeNow());
                        es_data.put("radius",radius);
                        es_data.put("type"  , cluster.getType());
                        String latlng = mean.getY() + "," + mean.getX();
                        String gname = PoiUtils.geocode2(geoUrl, geoToken, latlng);
                        if (gname != null)
                            es_data.put("gname", gname);
                        esDataList.add(es_data);
                    }
                }
                if (esDataList.size() > 0) {
                    esDao.index(index, type, esDataList);
                }
            }
        }
    }

    /**
     * 转换mac由原来|转为空格分隔
     *
     * @param macs
     * @return
     */
    public static String transMacs(String macs) {
        String[] mac_array = macs.split("\\|");
        Set<Object> oneSet = new HashSet<Object>(Arrays.asList(mac_array));
        Object[] newMacs = oneSet.toArray();
        String mac = "";
        for (int i = 0; i < newMacs.length; i++) {
            if (i == 0) {
                mac = newMacs[i].toString();
            } else {
                mac += " " + newMacs[i].toString();
            }
        }
        return mac;
    }

    /**
     * 合并去重复
     *
     * @param macs1
     * @param macs2
     * @return
     */
    public static String mergeMacs(String macs1, String macs2) {
        String[] macs_1 = macs1.split(" ");
        String[] macs_2 = macs2.split(" ");
        Set<String> set = new HashSet<String>();
        for (String mac : macs_1) {
            set.add(mac);
        }
        for (String mac : macs_2) {
            set.add(mac);
        }
        String new_macs = "";
        for (String mac : set) {
            new_macs += " " + mac;
        }
        return new_macs.trim();
    }

    public String uuid() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replace("-", "");
    }

}
