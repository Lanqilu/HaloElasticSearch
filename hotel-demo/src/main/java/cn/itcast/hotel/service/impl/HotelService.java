package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HotelService extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {

    @Autowired
    private RestHighLevelClient client;

    @Override
    public Map<String, List<String>> getFilters(RequestParams params) {
        try {
            // 1. 准备 request
            SearchRequest request = new SearchRequest("hotel");

            // 2. 准备 DSL
            // query
            FunctionScoreQueryBuilder query = getQueryBuilder(params);
            request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
            request.source().query(query);
            // 2.1 设置 size = 0
            request.source().size(0);
            // 2.2 聚合
            HashMap<String, String> items = new HashMap<>();
            items.put("brand", "品牌");
            items.put("city", "城市");
            items.put("starName", "星级");
            for (String item : items.keySet()) {
                request.source().aggregation(AggregationBuilders
                        .terms(item + "Agg")
                        .field(item)
                        .size(100));
            }
            // 3. 发出请求
            SearchResponse response = null;

            response = client.search(request, RequestOptions.DEFAULT);


            // 4. 解析结果
            // 4.1 获取 aggregations
            Aggregations aggregations = response.getAggregations();

            HashMap<String, List<String>> itemListHashMap = new HashMap<>();

            for (String item : items.keySet()) {
                // 4.2 根据名称获取聚合结果
                Terms brandTerms = aggregations.get(item + "Agg");
                // 4.3 获取 buckets 并遍历
                ArrayList<String> itemList = new ArrayList<>();
                for (Terms.Bucket bucket : brandTerms.getBuckets()) {
                    // 获取 key
                    itemList.add(bucket.getKeyAsString());
                }
                itemListHashMap.put(item, itemList);
            }
            return itemListHashMap;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PageResult search(RequestParams params) {
        try {
            // 1.准备Request
            SearchRequest request = new SearchRequest("hotel");
            // 2.准备DSL
            // 2.1.构建 query
            FunctionScoreQueryBuilder query = getQueryBuilder(params);
            request.source().highlighter(new HighlightBuilder().field("name").requireFieldMatch(false));
            request.source().query(query);

            // 2.2.分页
            int page = params.getPage();
            int size = params.getSize();
            request.source().from((page - 1) * size).size(size);

            // 排序
            String location = params.getLocation();
            if (location != null && !location.equals("")) {
                request.source().sort(SortBuilders
                        .geoDistanceSort("location", new GeoPoint(location))
                        .order(SortOrder.ASC)
                        .unit(DistanceUnit.KILOMETERS));
            }

            // 3.发送请求
            SearchResponse response = client.search(request, RequestOptions.DEFAULT);
            // 4.解析响应
            return handleResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private FunctionScoreQueryBuilder getQueryBuilder(RequestParams params) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        String key = params.getKey();
        if (key == null || "".equals(key)) {
            boolQuery.must(QueryBuilders.matchAllQuery());
        } else {
            boolQuery.must(QueryBuilders.matchQuery("all", key));
        }
        // 条件过滤
        // 城市条件
        if (params.getCity() != null && !params.getCity().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("city", params.getCity()));
        }
        // 品牌条件
        if (params.getBrand() != null && !params.getBrand().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("brand", params.getBrand()));
        }
        // 星级
        if (params.getStarName() != null && !params.getStarName().equals("")) {
            boolQuery.filter(QueryBuilders.termQuery("starName", params.getStarName()));
        }
        // 价格
        if (params.getMinPrice() != null && params.getMaxPrice() != null) {
            boolQuery.filter(QueryBuilders.rangeQuery("price")
                    .gte(params.getMinPrice()).lte(params.getMaxPrice()));
        }

        // 算分控制
        FunctionScoreQueryBuilder functionScoreQueryBuilder =
                QueryBuilders.functionScoreQuery(
                        // 原始查询，相关性算分
                        boolQuery,
                        // function score
                        new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
                                // 一个 function score 元素
                                new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                        // 过滤条件
                                        QueryBuilders.termQuery("isAD", true),
                                        // 算分函数
                                        ScoreFunctionBuilders.weightFactorFunction(10)
                                )
                        });

        return functionScoreQueryBuilder;
    }

    // 结果解析
    private PageResult handleResponse(SearchResponse response) {
        // 4.解析响应
        SearchHits searchHits = response.getHits();
        // 4.1.获取总条数
        long total = searchHits.getTotalHits().value;
        // 4.2.文档数组
        SearchHit[] hits = searchHits.getHits();
        // 4.3.遍历
        List<HotelDoc> hotels = new ArrayList<>();
        for (SearchHit hit : hits) {
            // 获取文档source
            String json = hit.getSourceAsString();
            // 反序列化
            HotelDoc hotelDoc = JSON.parseObject(json, HotelDoc.class);
            // 获取排序值 - location
            Object[] sortValues = hit.getSortValues();
            if (sortValues.length > 0) {
                Object sortValue = sortValues[0];
                hotelDoc.setDistance(sortValue);
            }

            // 处理高亮
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)) {
                // 获取高亮字段结果
                HighlightField highlightField = highlightFields.get("name");
                if (highlightField != null) {
                    // 取出高亮结果数组中的第一个，就是酒店名称
                    String name = highlightField.getFragments()[0].string();
                    hotelDoc.setName(name);
                }
            }

            // 放入集合
            hotels.add(hotelDoc);
        }
        // 4.4.封装返回
        return new PageResult(total, hotels);
    }
}
