package cn.itcast.hotel.controller;

import cn.itcast.hotel.pojo.PageResult;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Halo
 * @create 2021/09/12 下午 06:43
 * @description
 */
@RestController
@RequestMapping("/hotel")
public class HotelController {

    @Autowired
    private IHotelService hotelService;

    // 搜索酒店数据
    @PostMapping("/list")
    public PageResult search(@RequestBody RequestParams params) {
        return hotelService.search(params);
    }

    @PostMapping("filters")
    public Map<String, List<String>> getFilters(@RequestBody RequestParams params) {
        return hotelService.getFilters(params);
    }
}