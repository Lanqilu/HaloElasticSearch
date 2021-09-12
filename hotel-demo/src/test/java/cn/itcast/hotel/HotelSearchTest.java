package cn.itcast.hotel;



import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author Halo
 * @create 2021/09/11 下午 02:40
 * @description Rest Client Search 测试
 */

public class HotelSearchTest {

    private RestHighLevelClient client;

    @Test
    void testMatchAll() throws IOException {
        // 1.准备 Request
        SearchRequest request = new SearchRequest("hotel");
        // 2.组织 DSL 参数
        request.source().query(QueryBuilders.matchAllQuery());
        // 3.发送请求，得到响应结果
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        // 4.解析结果
        SearchHits searchHits = response.getHits();
        // 4.1.查询的总条数
        long total = searchHits.getTotalHits().value;
        System.err.println("total = " + total);
        // 4.2.查询的结果数组
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            // 4.3.得到source
            String json = hit.getSourceAsString();
            // 反序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            // 4.4.打印
            System.out.println(hotelDoc);
        }
    }



    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(HttpHost.create("http://halo:9200")));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }
}
