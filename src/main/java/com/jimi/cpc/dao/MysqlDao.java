package com.jimi.cpc.dao;

import com.jimi.cpc.util.DateUtil;
import com.jimi.cpc.util.SysConfigUtil;

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
public class MysqlDao {
	
    private static final Logger log = LoggerFactory.getLogger(MysqlDao.class);

    public Connection getMySqlConn() {
        try {
            String driverName = SysConfigUtil.getString("mysql.jdbc.driverName");
            String url = SysConfigUtil.getString("mysql.jdbc.url");
            String user = SysConfigUtil.getString("mysql.jdbc.user");
            String password = SysConfigUtil.getString("mysql.jdbc.password");
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
            String driverName = SysConfigUtil.getString("mycat.jdbc.driverName");
            String url = SysConfigUtil.getString("mycat.jdbc.url");
            String user = SysConfigUtil.getString("mycat.jdbc.user");
            String password = SysConfigUtil.getString("mycat.jdbc.password");
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
     * @param batchNum 每次查询的数量
     */
    public void search(TransferQueue<Map<String, List<String>>> queue, int threadNum, int day_num, int batchNum) {
        Connection conn = null;
        try {
            long beginTime = System.currentTimeMillis();
            conn = getMySqlConn();
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
            log.debug("database.size:" + meta_infos.size());
            Map<String, List<String>> imeiData = new HashMap<>();
            int data_sum = 0;
            //循环表的所有库
            for (String database_table : meta_infos) {
            	int table_data_sum = 0;
                long beginTime2 = System.currentTimeMillis();
                String[] databaseTableArray = database_table.split("#");
                String database = databaseTableArray[0];
                String table2 = "`" + database + "`." + databaseTableArray[1];
                String table1 = null;
                if (table_name1 != null) {
                    table1 = "`" + database + "`." + table_name1;
                }
                String sqlMaxImei ="";
                String sqlQuery="";
                String lastImei = null;
                String maxImei = null;
                String line = null;
                while(true)
                {
                	//获取最小imei
                	if(table1 == null)
                	{
		                if(lastImei != null)
		                {
		                	sqlMaxImei = "select max(device_imei) from " + table2 + " where gate_time>='" + beginDate +"' and device_imei>'"+lastImei+"' order by device_imei limit " + batchNum;
		                }
		                else
		                {
		                	sqlMaxImei = "select max(device_imei) from " + table2 + " where gate_time>='" + beginDate +"' order by device_imei limit " + batchNum;
		                }
                	}
	                else
	                {
	                	if(lastImei != null)
	                	{
	                		sqlMaxImei="SELECT max(device_imei) FROM (SELECT device_imei FROM "+table2+" where gate_time>='"+ beginDate +"' and device_imei>'"+lastImei+"'"
		                			  +" UNION ALL "
		                			  +"SELECT device_imei FROM "+table1+" where gate_time>='"+ beginDate +"' and device_imei>'"+lastImei+"' order by device_imei limit " + batchNum+") AS a";
	                	}
	                	else
	                	{
		                	sqlMaxImei="SELECT max(device_imei) FROM (SELECT device_imei FROM "+table2+" where gate_time>='"+ beginDate +"'"
		                			  +" UNION ALL "
		                			  +"SELECT device_imei FROM "+table1+" where gate_time>='"+ beginDate +"' order by device_imei limit " + batchNum+") AS a";
	                	}
	                }
                	log.info("sqlMaxImei="+sqlMaxImei);
	                stmt = conn.prepareStatement(sqlMaxImei);
	                res = stmt.executeQuery();
	                if (res.next()) {
	                	maxImei = res.getString(1);
	                }
	                if(maxImei != null)
	                {
		                //获取表数据
		                if(table1 == null)
		                {
		                	if(lastImei != null)
		                	{
		                		sqlQuery = "select id,device_imei,macs,lat,lng , gate_time from " + table2 + " where gate_time>='" + beginDate + "'and device_imei<='" + maxImei+ "' and device_imei>'"+lastImei+"'";
		                	}
		                	else
		                	{
		                		sqlQuery = "select id,device_imei,macs,lat,lng , gate_time from " + table2 + " where gate_time>='" + beginDate + "'and device_imei<='" + maxImei+ "'";
		                	}
		                	
		                }
		                else
		                {
		                	if(lastImei != null)
		                	{
		                		sqlQuery = "select id,device_imei,macs,lat,lng , gate_time from " + table2 + " where gate_time>='" + beginDate + "'and device_imei<='" + maxImei+"' and device_imei>'"+lastImei+"'"
			                			 + " UNION ALL "
			                			 + "select id,device_imei,macs,lat,lng , gate_time from " + table1 + " where gate_time>='" + beginDate + "'and device_imei<='" + maxImei+ "' and device_imei>'"+lastImei+"'";
		                	}
		                	else
		                	{
		                		sqlQuery = "select id,device_imei,macs,lat,lng , gate_time from " + table2 + " where gate_time>='" + beginDate + "'and device_imei<='" + maxImei+ "'"
			                			 + " UNION ALL "
			                			 + "select id,device_imei,macs,lat,lng , gate_time from " + table1 + " where gate_time>='" + beginDate + "'and device_imei<='" + maxImei+ "'";
		                	}
		                }
		                lastImei = maxImei;
		                log.info("sqlQuery="+sqlQuery);
		                stmt = conn.prepareStatement(sqlQuery);
                        res = stmt.executeQuery();
                        int colCount = res.getMetaData().getColumnCount();
                        //---组装成一行行数据
                        String currentImei2 = null;
                        while (res.next()) {
                            data_sum++;
                            table_data_sum++;
                            for (int k = 1; k <= colCount; k++) {
                                if (k == 1) {
                                    line = res.getString(k);
                                } else {
                                    line += "\t" + res.getString(k);
                                }
                            }
                            
                            currentImei2 = res.getString("device_imei");
                            List<String> dataList2 = imeiData.get(currentImei2);
                            if (dataList2 == null) {
                            	dataList2 = new ArrayList<String>();
                            	imeiData.put(currentImei2, dataList2);
                            } 
                            dataList2.add(line);
                        }
                        res.close();
                        stmt.close();
                        //已按imei分组好数据，入队进行计算
                        queue.transfer(imeiData);
                        imeiData = new HashMap<String, List<String>>();
	                }
	                else
	                {
	                	break;
	                }
                }
                if(table1 == null)
                {
                	log.info("数据表="+table2+"数据总量table_data_sum="+table_data_sum+";task 耗时"+(System.currentTimeMillis() - beginTime2));
                }
                else
                {
                	log.info("数据表="+table1+","+table2+"数据总量table_data_sum="+table_data_sum+";task 耗时"+(System.currentTimeMillis() - beginTime2));
                }
                /*
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
                            log.debug("sql2="+sql2);
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
                            log.debug(sql2);
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
                        log.debug(sql2);
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
                            List<String> dataList2 = imeiData.get(currentImei2);
                            if (dataList2 == null) {
                            	dataList2 = new ArrayList<String>();
                            	imeiData.put(currentImei2, dataList2);
                            } 
                            dataList2.add(line);

                        }

                        res.close();
                        stmt.close();
                        //---------合并上一个月------------
                        String currentImei1 = null;
                        if (sql1 != null) {
                        	log.debug(sql1);
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
                                List<String> dataList1 = imeiData.get(currentImei1);
                                if (dataList1 == null) {
                                	dataList1 = new ArrayList<String>();
                                	imeiData.put(currentImei1, dataList1);
                                } 
                                dataList1.add(line);
                            }
                            res.close();
                            stmt.close();
                        }
                        //已按imei分组好数据，入队进行计算
                       queue.transfer(imeiData);
                       imeiData = new HashMap<String, List<String>>();
                        //---数据查询异常
                        if (!haveData) {
                            log.error("数据查询异常:count: " + count1 + ",totalNum: " + totalNum);
                            break;
                        }
                        log.info(" count: " + count2 + " totalNum: " + totalNum + " this batch tasks:" + (System.currentTimeMillis() - startTime) + " lastImei:" + lastImei2);
                    }
                }
                
                log.info(table2 + " task time:" + (System.currentTimeMillis() - beginTime2) + " count:" + count2 + " totalNum:" + totalNum + " " + (count2 == totalNum));
            	*/
            }
            
            //查询数据，发送完成指令，让各线程善后(如释放资源)
            for (int i = 0; i < threadNum; i++) {
                imeiData.put("finish", new ArrayList<String>());
                queue.transfer(imeiData);
            }
            //----验证数据条数
			Connection myConn = null;
			int totalNum2 = 0;
			int totalNum1 = 0;
			try {
				myConn = getMyCatConn();
				String sql = "select count(*) from " + table_name2.replace("'","")+ " where gate_time>='" + beginDate + "'";
				stmt = myConn.prepareStatement(sql);
				res = stmt.executeQuery();
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
			} catch (Exception e) {
				log.error("验证数据条数数据库查询异常:" + e.getMessage());
			} finally
			{
				res.close();
	            stmt.close();
	            myConn.close();
			}
            
            log.info("all data load finish task time:" + (System.currentTimeMillis() - beginTime) + " mycat table sum:" + (totalNum1+totalNum2)+" mysql table sum:"+data_sum);
        } catch (Exception e) {
            log.error("mysql数据查询异常:" + e.getMessage());
            Map<String, List<String>> imeiData = new HashMap<>();
            imeiData.put("finish", new ArrayList<String>());
            try {
				queue.transfer(imeiData);
			} catch (InterruptedException e1) {
				log.error("发送异常结束信号异常:" + e.getMessage());
			}
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                	log.error("数据库关闭连接异常:" + e.getMessage());
                	
                	Map<String, List<String>> imeiData = new HashMap<>();
                    imeiData.put("finish", new ArrayList<String>());
                    try {
        				queue.transfer(imeiData);
        			} catch (InterruptedException e1) {
        				log.error("发送异常结束信号异常:" + e.getMessage());
        			}
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
            log.debug(sql);
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
