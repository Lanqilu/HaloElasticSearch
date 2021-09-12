package cn.itcast.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author Halo
 * @create 2021/09/12 下午 10:27
 * @description 聚合测试类
 */
public class HotelAggregationTest {
    private RestHighLevelClient client;

    @Test
    void testAggregation() throws IOException {
        // 1. 准备 request
        SearchRequest request = new SearchRequest("hotel");

        // 2. 准备 DSL
        // 2.1 设置 size = 0
        request.source().size(0);
        // 2.2 聚合
        request.source().aggregation(AggregationBuilders
                .terms("brandAgg")
                .field("brand")
                .size(10));
        // 3. 发出请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // 4. 解析结果
        // 4.1 获取 aggregations
        Aggregations aggregations = response.getAggregations();
        // 4.2 根据名称获取聚合结果
        Terms brandTerms = aggregations.get("brandAgg");
        // 4.3 获取 buckets 并遍历
        for (Terms.Bucket bucket : brandTerms.getBuckets()) {
            // 获取 key
            String key = bucket.getKeyAsString();
            System.out.println(key);
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
