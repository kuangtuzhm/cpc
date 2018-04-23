package com.jimi.cpc.test;

import com.jimi.cpc.util.DateUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class DataGenTest {
    public static void main(String[] args) {
        new DataGenTest().genData();
    }
    /**
     * 单个设备超过1万的
     * 模拟两，三个聚点
     *
     * @throws Exception
     */
    public void genData() {
        Connection conn = null;
        try {
            long beginTime=System.currentTimeMillis();
            DbScanTest test = new DbScanTest();
            conn = test.getMyCatConn();
            conn.setAutoCommit(false);
            String sql = "insert into wifi_location_10(id,device_imei,macs,lng,lat,gate_time,record_time) values(?,?,?,?,?,?,?)";
            PreparedStatement pstm = conn.prepareStatement(sql);
            int count = 0;
            long beginTime1 = System.currentTimeMillis();
            //
            long imei = 668897215570564l;
            String macs="005a136420e8|34ce202e09ca|b0d59d5bc22c|8825933a5c47";
            double lng=112.916737496981;
            double lat=21.5774370637507;
            double step=0.000001;
            double batchStep=0.01;
             int batchNum=10000;
            for (int j=0;j<100;j++){
                System.out.println(imei);
                imei+=1;
                lng+=1;
                lat+=1;
                for (int i = 0; i < 5000; i++) {
                    count++;
                    long id = System.currentTimeMillis() + count;
                    int col = 1;
                    pstm.setString(col++, id + "");
                    pstm.setString(col++, imei+"");
                    pstm.setString(col++, macs);
                    if (i%1000==0){
                        lng+=batchStep;
                        lat+=batchStep;
                    }else{
                        lng+=step;
                        lng+=step;
                    }
                    pstm.setDouble(col++, lng);
                    pstm.setDouble(col++, lat);
                    pstm.setString(col++, DateUtil.getDateBefore("yyyy-MM-dd HH:mm:ss",i%30));
                    pstm.setString(col++, DateUtil.getDateBefore("yyyy-MM-dd HH:mm:ss",i%30));
                    pstm.addBatch();
//                    if (count%batchNum==0){
//
//                    }
                }
                pstm.executeBatch();
                conn.commit();
                pstm.clearBatch();
                System.out.println(count+" this batch task:"+(System.currentTimeMillis()-beginTime1));
                beginTime1=System.currentTimeMillis();
            }
            pstm.executeBatch();
            conn.commit();
            pstm.close();
            System.out.println("task time:"+(System.currentTimeMillis()-beginTime));
        } catch (Exception e) {
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
     * 单个设备超过1万的
     * 模拟两，三个聚点
     *
     * @throws Exception
     */
    public void genData2() {
        Connection conn = null;
        try {
            long beginTime=System.currentTimeMillis();
            DbScanTest test = new DbScanTest();
            conn = test.getMyCatConn();
            conn.setAutoCommit(false);
            String sql1 = "insert into wifi_location_09(id,device_imei,macs,lng,lat,gate_time,record_time) values(?,?,?,?,?,?,?)";
//            String sql2 = "insert into wifi_location_09(id,device_imei,macs,lng,lat,gate_time,record_time) values(?,?,?,?,?,?,?)";
            PreparedStatement pstm = conn.prepareStatement(sql1);
            int count = 0;
            long beginTime1 = System.currentTimeMillis();
            //
            long imei = 687597010570564l;
            String macs="640980774e7e|54666c810d20|005a136410e8|34ce002e09ca|b0d59d5bc22c|8825933a5c47|206be70cb5d4|84dbacd9e82d";
            double lng=112.916537496981;
            double lat=21.5773370637507;
            double step=0.000001;
            double batchStep=0.01;
            int batchNum=10000;
            for (int j=0;j<10000;j++){
                System.out.println(imei);
                imei+=1;
                lng+=1;
                lat+=1;
                for (int i = 0; i < 5000; i++) {
                    count++;
                    long id = System.currentTimeMillis() + count;
                    int col = 1;
                    pstm.setString(col++, id + "");
                    pstm.setString(col++, imei+"");
                    pstm.setString(col++, macs);
                    if (i%1000==0){
                        lng+=batchStep;
                        lat+=batchStep;
                    }else{
                        lng+=step;
                        lng+=step;
                    }
                    pstm.setDouble(col++, lng);
                    pstm.setDouble(col++, lat);
                    pstm.setString(col++, DateUtil.getTimeNow());
                    pstm.setString(col++, DateUtil.getTimeNow());
                    pstm.addBatch();
//                    if (count%batchNum==0){
//
//                    }
                }
                pstm.executeBatch();
                conn.commit();
                pstm.clearBatch();
                System.out.println(count+" this batch task:"+(System.currentTimeMillis()-beginTime1));
                beginTime1=System.currentTimeMillis();
            }
            pstm.executeBatch();
            conn.commit();
            pstm.close();
            System.out.println("task time:"+(System.currentTimeMillis()-beginTime));
        } catch (Exception e) {
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
}
