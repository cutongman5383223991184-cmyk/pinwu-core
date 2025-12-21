package com.pinwu.app.controller;

import com.pinwu.common.core.domain.AjaxResult;
import com.pinwu.common.utils.AwsS3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/app/common")
@Slf4j
public class AppCommonController {

    @Autowired
    private AwsS3Service awsS3Service;

    /**
     * APP 图片上传 (上传到 AWS S3)
     */
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            log.warn("S3上传请求被拦截 - 原因: 接收到的文件为空");
            return AjaxResult.error("文件不能为空");
        }
        try {
            long startTime = System.currentTimeMillis();
            String url = awsS3Service.uploadFile(file);
            long costTime = System.currentTimeMillis() - startTime;

            // 3. ★★★ 加上这行，控制台就有输出了 ★★★
            log.info("S3上传成功 - 文件名: {}, 耗时: {}ms, URL: {}", file.getOriginalFilename(), costTime, url);
            
            // 返回格式保持和若依一致，方便前端处理
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", file.getOriginalFilename());
            return ajax;
        } catch (Exception e) {
            log.error("S3上传失败:", e);
            return AjaxResult.error(e.getMessage());
        }
    }
}