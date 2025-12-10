package com.pinwu.app.modules.product.controller;

import com.pinwu.app.modules.auth.domain.model.AppLoginUser;
import com.pinwu.app.modules.auth.service.AppTokenService;
import com.pinwu.app.modules.product.domain.doc.ProductDoc;
import com.pinwu.app.modules.product.domain.dto.ProductPublishDto;
import com.pinwu.app.modules.product.domain.dto.ProductSearchQuery;
import com.pinwu.app.modules.product.service.AppProductService;
import com.pinwu.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/app/product")
public class AppProductController {

    @Autowired
    private AppProductService productService;

    @Autowired
    private AppTokenService tokenService;

    /**
     * 发布商品
     */
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody @Validated ProductPublishDto dto, HttpServletRequest request) {
        AppLoginUser loginUser = tokenService.getLoginUser(request);
        if (loginUser == null) {
            return AjaxResult.error(401, "请先登录");
        }
        productService.publish(dto, loginUser.getPwUser().getId());
        return AjaxResult.success("发布成功");
    }

    /**
     * 首页搜索 (无需登录即可搜)
     */
    @PostMapping("/search")
    public AjaxResult search(@RequestBody ProductSearchQuery query) {
        // 如果是默认排序，必须传坐标，否则降级为按时间排序
        if (query.getSort() == 0 && (query.getLatitude() == null || query.getLongitude() == null)) {
            query.setSort(2); // 降级为最新发布
        }
        
        List<ProductDoc> list = productService.search(query);
        return AjaxResult.success(list);
    }
}