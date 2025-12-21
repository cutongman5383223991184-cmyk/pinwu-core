package com.pinwu.app.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.pinwu.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/app/location")
public class AppLocationController {

    @Value("${amap.key}")
    private String amapKey;

    @Value("${amap.url}")
    private String amapUrl;

    @GetMapping("/regeo")
    public AjaxResult regeo(@RequestParam Double lat, @RequestParam Double lng) {
        if (lat == null || lng == null) return AjaxResult.error("坐标不能为空");

        String location = lng + "," + lat;
        String url = String.format("%s?key=%s&location=%s&extensions=base", amapUrl, amapKey, location);

        try {
            String response = HttpUtil.get(url);
            JSONObject json = JSONUtil.parseObj(response);

            if ("1".equals(json.getStr("status"))) {
                JSONObject regeocode = json.getJSONObject("regeocode");
                JSONObject addressComponent = regeocode.getJSONObject("addressComponent");

                // ★★★ 核心修改：数据清洗 ★★★
                String province = cleanData(addressComponent.get("province"));
                String city = cleanData(addressComponent.get("city"));
                String district = cleanData(addressComponent.get("district"));

                // 1. 判断是否是“公海”或“无效区域” (即 province 解析出来是空的)
                if (province == null || province.isEmpty()) {
                    // 返回特定状态，告诉前端需要手动输入
                    return AjaxResult.success().put("msg", "未识别区域，请手动输入"); // data 为空
                }

                // 2. 判断是否只有“国家名”没有省份 (针对某些领海区域)
                if ("中华人民共和国".equals(province) && (city == null || city.isEmpty())) {
                    return AjaxResult.success().put("msg", "位置模糊，请手动输入");
                }

                // 3. 正常数据组装
                Map<String, Object> result = new HashMap<>();
                result.put("province", province);

                // 直辖市处理：如果是北京，city字段可能是空的，用province填充
                if (city == null || city.isEmpty()) {
                    result.put("city", province);
                } else {
                    result.put("city", city);
                }

                result.put("region", district);

                // 格式化地址：如果也是 [] 就返回空字符串
                Object fmtAddrObj = regeocode.get("formatted_address");
                String fmtAddr = (fmtAddrObj instanceof JSONArray) ? "" : fmtAddrObj.toString();
                result.put("formattedAddress", fmtAddr);

                return AjaxResult.success(result);
            } else {
                return AjaxResult.error("解析失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("服务异常");
        }
    }


    /**
     * 地理编码：根据详细地址获取经纬度
     * @param address 详细地址 (如: 江苏省南京市浦口区宁六路219号)
     */
    @GetMapping("/geo")
    public AjaxResult geo(@RequestParam String address) {
        if (address == null || address.trim().isEmpty()) {
            return AjaxResult.error("地址不能为空");
        }

        // 高德地理编码 API
        // 文档: https://lbs.amap.com/api/webservice/guide/api/geocoding
        String url = String.format("https://restapi.amap.com/v3/geocode/geo?key=%s&address=%s", amapKey, address);

        try {
            String response = HttpUtil.get(url);
            JSONObject json = JSONUtil.parseObj(response);

            if ("1".equals(json.getStr("status"))) {
                JSONArray geocodes = json.getJSONArray("geocodes");
                if (geocodes != null && !geocodes.isEmpty()) {
                    JSONObject resultObj = geocodes.getJSONObject(0);
                    // location 格式: "118.718388,32.206745" (经度,纬度)
                    String location = resultObj.getStr("location");
                    String[] split = location.split(",");

                    Map<String, Object> resMap = new HashMap<>();
                    resMap.put("longitude", Double.parseDouble(split[0]));
                    resMap.put("latitude", Double.parseDouble(split[1]));
                    resMap.put("formattedAddress", resultObj.getStr("formatted_address"));
                    resMap.put("province", resultObj.getStr("province"));
                    resMap.put("city", resultObj.getStr("city"));
                    resMap.put("district", resultObj.getStr("district"));

                    return AjaxResult.success(resMap);
                } else {
                    return AjaxResult.error("未找到该地址对应的坐标");
                }
            } else {
                return AjaxResult.error("地址解析失败: " + json.getStr("info"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return AjaxResult.error("位置服务异常");
        }
    }

    /**
     * 数据清洗工具方法
     * 高德对于空数据会返回 [] (JSONArray)，我们需要把它转成 null
     */
    private String cleanData(Object obj) {
        if (obj == null) return null;
        // 如果是空数组 []
        if (obj instanceof JSONArray) {
            return null;
        }
        // 如果是字符串 "[]" (Hutool有时候会转成字符串)
        String str = obj.toString();
        if ("[]".equals(str)) {
            return null;
        }
        return str;
    }
}