package com.jimi.cpc.dao;

import com.jimi.cpc.util.DateUtil;
import com.jimi.cpc.util.PropertiesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TransferQueue;

/**
 * mysql数据存取
 * @author  yuanshao
 */
public class CopyOfMysqlDao {
    private static final Logger log = LoggerFactory.getLogger(CopyOfMysqlDao.class);
    private PropertiesUtils propertiesUtils = null;

    public CopyOfMysqlDao(PropertiesUtils propertiesUtils) {
        this.propertiesUtils = propertiesUtils;
    }

    public Connection getMySqlConn() {
        try {
            String driverName = propertiesUtils.get("mysql.jdbc.driverName");
            String url = propertiesUtils.get("mysql.jdbc.url");
            String user = propertiesUtils.get("mysql.jdbc.user");
            String password = propertiesUtils.get("mysql.jdbc.password");
            Class.forName(driverName);
            Connection conn = DriverManager.getConnection(url, user, password);
            return conn;
        } catch (Exception e) {
            log.info("连接异常：" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    public Connection getMyCatConn() {
        try {
            String driverName = propertiesUtils.get("mycat.jdbc.driverName");
            String url = propertiesUtils.get("mycat.jdbc.url");
            String user = propertiesUtils.get("mycat.jdbc.user");
            String password = propertiesUtils.get("mycat.jdbc.password");
            Class.forName(driverName);
            Connection conn = DriverManager.getConnection(url, user, password);
            return conn;
        } catch (Exception e) {
            log.info("连接异常：" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param queue     队列
     * @param threadNum 处理线程数
     * @param day_num   查询前几天
     */
    public void search(TransferQueue<Map<String, List>> queue, int threadNum, int day_num) {
        Connection conn = null;
        try {
            long beginTime = System.currentTimeMillis();
            conn = getMySqlConn();
            int batchNum = 10000; //每批查询数据量
            PreparedStatement stmt = null;
            ResultSet res = null;
            String beginDate = DateUtil.getDateBefore("yyyy-MM-dd", day_num);
            String beferMonth = beginDate.substring(5, 7);
            String currentMonth = DateUtil.getTimeNow().substring(5, 7);
            String table_name1 = null;
            String table_name2 = "'wifi_location_" + currentMonth + "'";
            if (!beferMonth.equals(currentMonth)) { //判断是否需要跨表查询
                table_name1 = "wifi_location_" + beferMonth;
            }
            List<String> meta_infos = getMySqlMeta(conn, table_name2);
            System.out.println("database.size:" + meta_infos.size());
            Map<String, List> imeiData = new HashMap<String, List>();
            List<String> list1 = new ArrayList<String>(); //1都表示上个月的
            List<String> list2 = new ArrayList<String>(); //2都表示当前月的
            int table1_sum = 0;
            int table2_sum = 0;
            //循环表的所有库
            for (String database_table : meta_infos) {
                long beginTime2 = System.currentTimeMillis();
                String[] databaseTableArray = database_table.split("#");
                String database = databaseTableArray[0];
                String table2 = "`" + database + "`." + databaseTableArray[1];
                String table1 = null;
                if (table_name1 != null) {
                    table1 = "`" + database + "`." + table_name1;
                }
                //----查询表数据量
                String sql2 = "select count(*) from " + table2 + " where gate_time>='" + beginDate + "'";
                String sql1 = null;
                stmt = conn.prepareStatement(sql2);
                res = stmt.executeQuery();
                int totalNum = 0;
                int count1 = 0;
                int count2 = 0;
                if (res.next()) {
                    totalNum = res.getInt(1);
                }
                if (totalNum > 0) {
                    String lastImei2 = null;
                    String line = null;
                    while (count2 < totalNum) {
                        long startTime = System.currentTimeMillis();
                        //--第一批，先查询设备号
                        if (lastImei2 == null) {
                            sql2 = "select device_imei from " + table2 + " where gate_time>='" + beginDate + "' order by device_imei limit " + batchNum;
                            System.out.println(sql2);
                            stmt = conn.prepareStatement(sql2);
                            res = stmt.executeQuery();
                            while (res.next()) {
                                lastImei2 = res.getString(1); //找到最大的设备号
                            }
                            res.close();
                            stmt.close();
//                            System.out.println("1 task time:" + (System.currentTimeMillis() - startTime));
//                            startTime = System.currentTimeMillis();
                            sql2 = "select id,device_imei,macs,lat,lng , gate_time from " + table2 + " where gate_time>='" + beginDate + "'and device_imei<='" + lastImei2 + "' order by device_imei";
                            if (table1 != null) {
                                sql1 = "select id,device_imei,macs,lat,lng , gate_time from " + table1 + " where gate_time>='" + beginDate + "' and device_imei<='" + lastImei2 + "' order by device_imei";
                            }
                        } else {//第二批后的数据
                            sql2 = "select device_imei from " + table2 + " where gate_time>='" + beginDate + "'and device_imei>'" + lastImei2 + "' order by device_imei limit " + batchNum;
                            System.out.println(sql2);
                            stmt = conn.prepareStatement(sql2);
                            res = stmt.executeQuery();
                            String maxImei = null;
                            while (res.next()) {
                                maxImei = res.getString(1);//找到最大的设备号
                            }
                            res.close();
                            stmt.close();
//                            System.out.println("1 task time:" + (System.currentTimeMillis() - startTime));
//                            startTime = System.currentTimeMillis();
                            sql2 = "select id,device_imei,macs,lat,lng , gate_time from " + table2 + " where device_imei>'" + lastImei2 + "' and device_imei<='" + maxImei + "' order by device_imei";
                            if (table1 != null) {
                                sql1 = "select id,device_imei,macs,lat,lng , gate_time from " + table1 + " where device_imei>'" + lastImei2 + "' and device_imei<='" + maxImei + "' order by device_imei";
                            }
                            lastImei2 = maxImei;
                        }
                        System.out.println(sql2);
                        stmt = conn.prepareStatement(sql2);
                        res = stmt.executeQuery();
//                        System.out.println("2 task time:" + (System.currentTimeMillis() - startTime));
//                        startTime = System.currentTimeMillis();
                        int colCount = res.getMetaData().getColumnCount();
                        //---组装成一行行数据
                        String currentImei2 = null;
                        boolean haveData = false;
                        while (res.next()) {
                            haveData = true;
                            count2++;
                            table2_sum++;
                            for (int k = 1; k <= colCount; k++) {
                                if (k == 1) {
                                    line = res.getString(k);
                                } else {
                                    line += "\t" + res.getString(k);
                                }
                            }
                            
                            currentImei2 = res.getString("device_imei");
                            List<String> dataList1 = imeiData.get(currentImei2);
                            if (dataList1 == null) {
                            	dataList1 = new ArrayList<String>();
                            	imeiData.put(currentImei2, dataList1);
                            } 
                            dataList1.add(line);

                        }

                        res.close();
                        stmt.close();
                        //---------合并上一个月------------
                        String currentImei1 = null;
                        String lastImei1 = null;
                        if (sql1 != null) {
                            System.out.println(sql1);
                            stmt = conn.prepareStatement(sql1);
                            res = stmt.executeQuery();
                            colCount = res.getMetaData().getColumnCount();
                            //---组装成一行行数据
                            while (res.next()) {
                                count1++;
                                table1_sum++;
                                for (int k = 1; k <= colCount; k++) {
                                    if (k == 1) {
                                        line = res.getString(k);
                                    } else {
                                        line += "\t" + res.getString(k);
                                    }
                                }
                                currentImei1 = res.getString("device_imei");
                                List<String> dataList2 = imeiData.get(currentImei1);
                                if (dataList2 == null) {
                                	dataList2 = new ArrayList<String>();
                                	imeiData.put(currentImei1, dataList2);
                                } 
                                dataList2.add(line);
                            }
                            res.close();
                            stmt.close();
                        }
                        //已按imei分组好数据，入队进行计算
                       queue.transfer(imeiData);
                       imeiData = new HashMap<String, List>();
                        //---数据查询异常
                        if (!haveData) {
                            log.error("数据查询异常:count: " + count1 + ",totalNum: " + totalNum);
                            break;
                        }
                        log.info(" count: " + count2 + " totalNum: " + totalNum + " this batch tasks:" + (System.currentTimeMillis() - startTime) + " lastImei:" + lastImei2);
                    }
                }
                log.info(table2 + " task time:" + (System.currentTimeMillis() - beginTime2) + " count:" + count2 + " totalNum:" + totalNum + " " + (count2 == totalNum));
            }
            //查询数据，发送完成指令，让各线程善后(如释放资源)
            for (int i = 0; i < threadNum; i++) {
                imeiData.put("finish", list2);
                queue.transfer(imeiData);
            }
            //----验证数据条数
            Connection myConn=getMyCatConn();
            String sql = "select count(*) from " + table_name2.replace("'","")+ " where gate_time>='" + beginDate + "'";
            stmt = myConn.prepareStatement(sql);
            res = stmt.executeQuery();
            int totalNum2=0;
            int totalNum1=0;
            if (res.next()) {
                totalNum2 = res.getInt(1);
            }

            if (table_name1!=null){
                sql = "select count(*) from " + table_name1+ " where gate_time>='" + beginDate + "'";
                stmt = myConn.prepareStatement(sql);
                res = stmt.executeQuery();
                if (res.next()) {
                    totalNum1 = res.getInt(1);
                }
            }
            log.info("all data load finish task time:" + (System.currentTimeMillis() - beginTime) + " mycat table1 sum:" + totalNum1+" mysql table1 sum:"+table1_sum+" table1_sum=totalNum1?"+(table1_sum==totalNum1)+ " mycat table2 sum:" + totalNum2+" mysql table2 sum:"+table2_sum+" table2_sum=totalNum2?"+(table2_sum==totalNum2));
        } catch (Exception e) {
            log.error("mysql数据查询异常:" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 根据表名，获取表所在的所有库
     *
     * @param conn
     * @param tableName
     * @return
     */
    private List<String> getMySqlMeta(Connection conn, String tableName) {
        try {
            long beginTime = System.currentTimeMillis();
            String sql = "select table_schema,table_name from information_schema.tables where table_name=" + tableName + " order by table_name";
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet res = stmt.executeQuery();
            List<String> table_schema = new ArrayList<>();
            System.out.println(sql);
            while (res.next()) {
                String database = res.getString(1);
                String table_name = res.getString(2);
                table_schema.add(database + "#" + table_name);
            }
            res.close();
            stmt.close();
            log.info("get mysql meta-tables tasks:" + (System.currentTimeMillis() - beginTime));
            return table_schema;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
