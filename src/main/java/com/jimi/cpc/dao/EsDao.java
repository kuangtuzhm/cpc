package com.jimi.cpc.dao;

import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jimi.cpc.util.SysConfigUtil;

/**
 * es 数据存取
 * @author  yuanshao
 */
public class EsDao {
    private static final Logger log = LoggerFactory.getLogger(EsDao.class);
    private Client client = null;

    public EsDao() {
        try {
            String host = SysConfigUtil.getString("es.mater.ip");
            int port = SysConfigUtil.getInt("es.mater.port");
            String cluster = SysConfigUtil.getString("es.cluster.name");
            Settings settings = Settings.settingsBuilder()
                    .put("cluster.name", cluster).build();
            this.client = TransportClient.builder().settings(settings).build()
                    .addTransportAddress(new InetSocketTransportAddress(new InetSocketAddress(host, port)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        EsDao esDao = new EsDao();
        String index = "wifi_group";
        String type = "wifi";
        String imei = "688897215570568";
//        List<Map<String, Object>> datalist=esDao.search(client,index,type,imei);
//        for (Map<String, Object> data:datalist){
//            System.out.println(data);
//            String _id=data.get("_id").toString();
//            data.remove("_id");
//            data.put("is_valid","1");
//            esDao.update(client,index,type,_id,data);
//        }
//        List<Map<String, Object>> datalist=new ArrayList<>();
//        for (int i=0;i<5;i++){
//            Map<String, Object> data=new HashMap<>();
//            data.put("update_time", DateUtil.getTimeNow());
//            data.put("gid", i);
//            data.put("create_time",  DateUtil.getTimeNow());
//            data.put("lng",  "11.654587");
//            data.put("lat",  "22.5773370637507");
//            data.put("counts",  100);
//            data.put("macs",  "a2b3c4 a3b4c5");
//            data.put("imei",  "imei2");
//            data.put("model",  "0");
//            datalist.add(data);
//        }
//        esDao.index(index,type, datalist);
        List<Map<String, Object>>  dataList= esDao.search(index,type,imei,0);
//        List<Map<String, Object>> dataList = esDao.search(index, type, imei,null,null);
        for (Map<String, Object> data:dataList){
            System.out.println(data);
        }
//        System.out.println();
        esDao.close();
    }

    public List<Map<String, Object>> search(String index, String type, String imei, String startTime, String endTime) {
        SearchRequestBuilder searchRequest = client.prepareSearch(index);
        searchRequest.setTypes(type);
//                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        if (imei!=null){
            searchRequest.setQuery(QueryBuilders.termQuery("imei", imei));
        }
//        searchRequest.setFrom(0).setSize(10000).setExplain(true);
        if (startTime != null && endTime != null) {
            searchRequest.setPostFilter(QueryBuilders.rangeQuery("update_time").from(startTime).to(endTime));    // Filter
        }
//        System.out.println(searchRequest.toString());
        SearchResponse response = searchRequest.get();
        SearchHit[] hits = response.getHits().getHits();
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (SearchHit hit : hits) {
//            System.out.println(hit.getSource());
            Map<String, Object> source = hit.getSource();
            source.put("_id", hit.getId());
            dataList.add(source);
        }
        return dataList;
    }

    public void index(String index, String type, List<Map<String, Object>> dataList) {
        try {
            BulkRequestBuilder bulkRequest = client.prepareBulk();
            for (Map<String, Object> data : dataList) {
                IndexRequestBuilder response = client.prepareIndex(index, type);
                response.setSource(data);
                response.setId(data.get("id").toString());
                bulkRequest.add(response);
            }
            BulkResponse bulkResponse = bulkRequest.get();
            if (bulkResponse.hasFailures()) {
                log.error("存在错误", bulkResponse.buildFailureMessage());

            }
            for (BulkItemResponse resp : bulkResponse) {
                if (resp.isFailed()) {
                    log.error("错误详情：", bulkResponse.buildFailureMessage());
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.warn(e.getMessage());
        }
    }

    public void update(String index, String type, Map<String, Object> data) {
        try {
            UpdateRequest updateRequest = new UpdateRequest();
            updateRequest.index(index);
            updateRequest.type(type);
            String id = data.get("_id").toString();
            updateRequest.id(id);
            data.remove("_id");
            updateRequest.doc(data);
            UpdateResponse response = client.update(updateRequest).get();
        } catch (Exception e) {
            e.printStackTrace();
            log.warn(e.getMessage());
        }
    }

    public List<Map<String, Object>> search(String index, String type,String imei,int model) {
        QueryBuilder qb = QueryBuilders
                .boolQuery()
                .must(QueryBuilders.termQuery("model", model))
                .must(QueryBuilders.termQuery("imei", imei));
        SearchRequestBuilder searchRequest = client.prepareSearch(index)
                .setTypes(type)
//                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setQuery(qb);
//        searchRequest.setFrom(0).setSize(10000);
//        System.out.println(searchRequest.toString());
        SearchResponse response = searchRequest.get();
        SearchHit[] hits = response.getHits().getHits();
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (SearchHit hit : hits) {
//            System.out.println(hit.getSource());
            Map<String, Object> source = hit.getSource();
            source.put("_id", hit.getId());
            dataList.add(source);
        }
        return dataList;
    }


    public void close() {
        if (client != null)
            client.close();
    }

    @Deprecated
    public void delete(String index, String type, String imei) {
        List<Map<String, Object>> dataList = search(index, type, imei,null,null);
        for (Map<String, Object> data : dataList) {
            String id = data.get("_id").toString();
            DeleteResponse response = client.prepareDelete(index, type, id).get();
        }
    }
    /**
     * ES保存ACC分段
     * @param acc
     */
    public  void insertWifiLocation() {
		try {
			BulkRequestBuilder bulkRequest = client.prepareBulk();

			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String dateStr = null;
		    try {
		    	 dateStr = format.format(new Date());
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
			JSONObject wifiLocation = new JSONObject();
			wifiLocation.put("update_time", dateStr);
			wifiLocation.put("gid"   , "86759701056979320171107145756dUcpUTZCb6e");
			wifiLocation.put("create_time" , dateStr);
			wifiLocation.put("lng"   	, 113.916521507787);
			wifiLocation.put("counts"   , 1);
			wifiLocation.put("macs"     , "005a136410e8");
			wifiLocation.put("is_valid" , 1);
			wifiLocation.put("imei"		, "867597010569793");
			wifiLocation.put("model"    , 1); // 用户Id
			wifiLocation.put("radius"   ,500.0); //全父id
			wifiLocation.put("gname"    ,"手动点"); //全父id
			wifiLocation.put("lat"      ,22.577214090036); //全父id

	    	wifiLocation.put("index_name","wifi_group"); //按月创建索引
			
			bulkRequest.add(client.prepareIndex("wifi_group", "wifi", wifiLocation.getString("gid")).setSource(JSON.toJSONString(wifiLocation)));
			bulkRequest.execute().actionGet();
			bulkRequest = client.prepareBulk();
			
		} catch (Exception e) {
			
		} finally {
			client.close();
		}
	}
}
