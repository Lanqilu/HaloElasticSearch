package cn.itcast.hotel;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author Halo
 * @create 2021/09/11 下午 02:40
 * @description
 */

public class HotelIndexTest {

    private RestHighLevelClient client;

    @Test
    void testInit() {
        System.out.println("client = " + client);
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
