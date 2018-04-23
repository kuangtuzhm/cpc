package com.jimi.cpc.test;

import com.jimi.cpc.service.DbScanService;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbScanTest {
    public  Connection getMyCatConn() {
        try {
            String driverName = "com.mysql.jdbc.Driver";
            String url = "jdbc:mysql://172.16.0.116:8066/his?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&autoReconnect=true&rewriteBatchedStatements=true";
            String user = "jimi";
            String password = "jimi";
            Class.forName(driverName);
            Connection conn = DriverManager.getConnection(url, user, password);
            return conn;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Test
    public void testDbscan()throws Exception{
        /*Connection conn= getMyCatConn();
        String sql = "select id,device_imei,macs,lat,lng from wifi_location where device_imei='867597010570558'";
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet res  = stmt.executeQuery();
        int count=0;
        int colCount=res.getMetaData().getColumnCount();
        String line=null;
        Map<String, List> dataMap=new HashMap<>();
        List<String> dataList=new ArrayList<>();
        String imei="867597010570558";
        while (res.next()) {
            count++;
            for (int k = 1; k <= colCount; k++) {
                if (k == 1) {
                    line = res.getString(k);
                } else {
                    line += "\t" + res.getString(k);
                }
            }
//            System.out.println(line);
            dataList.add(line);
        }
        res.close();
        stmt.close();
        conn.close();
        dataMap.put(imei,dataList);
        String config="E:\\IdeaProjects\\cpc\\src\\main\\resources\\param.properties";
        DbScanService servie=new DbScanService(config);
        servie.handle(dataMap);*/
    }
}
