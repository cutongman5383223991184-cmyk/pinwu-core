package com.pinwu.app.service;

import com.pinwu.app.modules.ai.domain.vo.AiResultVo;
import com.pinwu.app.modules.ai.service.QwenService; // 确保导入了这个
import org.junit.Test; // 注意：这是 Junit 4 的注解
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner; // 若依默认用 Junit 4

/**
 * AI 服务单元测试
 * 1. 确保 src/test/resources/application.yml 中填了 apiKey
 * 2. 确保 pinwu-app/pom.xml 中引入了 hutool-all
 */
@RunWith(SpringRunner.class) // Junit 4 必须加这个
@SpringBootTest
public class QwenServiceTest {

    private static final Logger log = LoggerFactory.getLogger(QwenServiceTest.class);

    @Autowired
    private QwenService qwenService;

    // 测试用图
    private static final String TEST_IMAGE_URL = "https://java-test-with-ai.oss-cn-beijing.aliyuncs.com/pinwuzhi/items/2025/11/17/c148a4c0a8e34a01a6b9aeee45b2bbfa.png";

    @Test
    public void testAnalyzeImage() {
        System.out.println("\n====== 开始测试：图片分析 (HTTP版) ======");
        try {
            long start = System.currentTimeMillis();

            // 调用服务
            AiResultVo result = qwenService.analyzeImage(TEST_IMAGE_URL);

            long end = System.currentTimeMillis();

            // 打印结果
            System.out.println("耗时: " + (end - start) + "ms");
            printResult(result);

        } catch (Exception e) {
            log.error("测试失败", e);
        }
        System.out.println("====== 测试结束 ======\n");
    }

    @Test
    public void testGenerateByKeywords() {
        System.out.println("\n====== 开始测试：文本生成 (HTTP版) ======");
        String keywords = "95新 PS5 光驱版 双手柄";

        try {
            long start = System.currentTimeMillis();

            AiResultVo result = qwenService.generateByKeywords(keywords);

            long end = System.currentTimeMillis();

            System.out.println("输入关键词: " + keywords);
            System.out.println("耗时: " + (end - start) + "ms");
            printResult(result);

        } catch (Exception e) {
            log.error("测试失败", e);
        }
        System.out.println("====== 测试结束 ======\n");
    }

    // 辅助打印方法
    private void printResult(AiResultVo result) {
        if (result == null) {
            System.err.println("错误：返回结果为 NULL");
            return;
        }
        System.out.println("--------------------------------------");
        System.out.println("【标题】: " + result.getTitle());
        System.out.println("【分类】: " + result.getCategory());
        // System.out.println("【标签】: " + result.getTags()); // List直接打印可能乱码，这里先简化
        System.out.println("【描述】: " + result.getDescription());
        System.out.println("--------------------------------------");
    }
}