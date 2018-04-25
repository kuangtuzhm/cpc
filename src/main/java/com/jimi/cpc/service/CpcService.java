package com.jimi.cpc.service;

import com.jimi.cpc.dao.EsDao;
import com.jimi.cpc.dao.MysqlDao;
import com.jimi.cpc.util.DateUtil;
import com.jimi.cpc.util.GpsUtils;
import com.jimi.cpc.util.PropertiesUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

public class CpcService {
    private static final Logger log = LoggerFactory.getLogger(CpcService.class);
    private EsDao esDao;
    private String index = null;
    private String type = null;
    private PropertiesUtils propertiesUtils = null;
    private int batchNum = 2000;
    private int radius;

    //    private DecimalFormat df=new DecimalFormat(".#######");
    public CpcService(String configPath) {
        this.propertiesUtils = new PropertiesUtils(configPath);
        this.radius = Integer.parseInt(propertiesUtils.get("manual.cpc.radius"));
        this.esDao = new EsDao(propertiesUtils);
        this.index = propertiesUtils.get("es.index.name").trim();
        this.type = propertiesUtils.get("es.index.type").trim();
        this.batchNum = Integer.parseInt(propertiesUtils.get("manual.cpc.batchNum"));
    }

    public static void main(String[] args) throws Exception {
    	String path = null;
    	if(args != null && args.length > 0)
    	{
    		path = args[0];
    	}
        CpcService cpcService = new CpcService(path);
        /*cpcService.esDao.insertWifiLocation();*/
        cpcService.task();
    }
    
    public void task() throws Exception {
        long beginTime = System.currentTimeMillis();
        TransferQueue<Map<String, List<String>>> queue = new LinkedTransferQueue<>();
        int threadNum = Integer.parseInt(propertiesUtils.get("dbscan.handleThread.num"));
        ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        CountDownLatch latch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++) {
            executor.execute(new CpcService.HandleData(queue, latch));
        }
        MysqlDao dao = new MysqlDao(propertiesUtils);
        int day_num = Integer.parseInt(propertiesUtils.get("manual.cpc.day"));
        dao.search(queue, threadNum,day_num,batchNum);
        latch.await();
        esDao.close();
        executor.shutdown();
        log.info("-----cpc all hadle finish-----task time: " + (System.currentTimeMillis() - beginTime));
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
        long beginTime=System.currentTimeMillis();
        for (String imei : mysqlData.keySet()) {
            long beginTime2=System.currentTimeMillis();
            List<String> dataList = mysqlData.get(imei);
            System.out.println(imei + ":" + dataList.size());
            List<Map<String, Object>> list = esDao.search(index, type,imei,1);
            for (Map<String, Object> esRow:list){
                double centerLat=Double.parseDouble(esRow.get("lat").toString());
                double centerLng=Double.parseDouble(esRow.get("lng").toString());
                String centerMacs=esRow.get("macs").toString();
                String s="";
                for (String line : dataList) {
                    String[] row = line.split("\t");
                    int index = 0;
                    String id = row[index++];
                    String device_imei = row[index++];
                    String macs = row[index++];
                    double lat = Double.parseDouble(row[index++]);
                    double lng = Double.parseDouble(row[index++]);
                    //逐一计算是否在中心范围
                    double distance= GpsUtils.distanceGoogle(centerLng,centerLat,lng,lat);
                    if (distance<=radius){
                        s+="|"+macs;
                    }
                }
                if (s!=null&&s.length()>0){
                    String newMacs=DbScanService.mergeMacs(centerMacs,DbScanService.transMacs(s.substring(1)));
                    if (!centerMacs.equals(newMacs)){
                        esRow.put("macs",newMacs);
                        esRow.put("update_time", DateUtil.getTimeNow());
                        esDao.update(index, type, esRow);
                    }
                }
            }
            System.out.println(imei+" cpc handle task:"+(System.currentTimeMillis()-beginTime2));
        }
        System.out.println("cpc handle task :"+(System.currentTimeMillis()-beginTime));
    }
    

}
