//package com.pinwu.app.controller;
//
//import com.pinwu.common.utils.AwsS3Service;
//import org.junit.Test; // ★ 改动1: 使用 JUnit 4 的 Test
//import org.junit.runner.RunWith; // ★ 改动2: 引入运行器
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.context.junit4.SpringRunner; // ★ 改动3: Spring 运行器
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * 单元测试 (JUnit 4 版本)
// */
//@RunWith(SpringRunner.class) // ★ 改动4: 必须加这个，告诉 JUnit 4 启动 Spring 容器
//@SpringBootTest
//@AutoConfigureMockMvc(addFilters = false) // 关闭 Security 过滤器
//public class AppCommonControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private AwsS3Service awsS3Service;
//
//    // --- Case 1: 正常系 ---
//    @Test
//    public void testUpload_Success() throws Exception {
//        // 1. Given
//        MockMultipartFile file = new MockMultipartFile(
//                "file",
//                "test.jpg",
//                MediaType.IMAGE_JPEG_VALUE,
//                "fake image content".getBytes()
//        );
//        String mockUrl = "http://img.pinwuzhi.xyz/items/mock-uuid.jpg";
//
//        // 2. When (Mock)
//        when(awsS3Service.uploadFile(any())).thenReturn(mockUrl);
//
//        // 3. Then
//        mockMvc.perform(multipart("/app/common/upload").file(file))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.msg").value("操作成功")) // 这里的 msg 要和你 Controller 返回的一致，若依默认是 "操作成功"
//                .andExpect(jsonPath("$.url").value(mockUrl));
//    }
//
//    // --- Case 2: 异常系 (文件为空) ---
//    @Test
//    public void testUpload_EmptyFile() throws Exception {
//        MockMultipartFile emptyFile = new MockMultipartFile(
//                "file",
//                "empty.jpg",
//                MediaType.IMAGE_JPEG_VALUE,
//                new byte[0]
//        );
//
//        mockMvc.perform(multipart("/app/common/upload").file(emptyFile))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(500))
//                .andExpect(jsonPath("$.msg").value("文件不能为空"));
//    }
//
//    // --- Case 3: 异常系 (S3报错) ---
//    @Test
//    public void testUpload_S3Error() throws Exception {
//        MockMultipartFile file = new MockMultipartFile(
//                "file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
//        );
//
//        when(awsS3Service.uploadFile(any())).thenThrow(new RuntimeException("AWS Connection Refused"));
//
//        mockMvc.perform(multipart("/app/common/upload").file(file))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(500))
//                .andExpect(jsonPath("$.msg").value("AWS Connection Refused"));
//    }
//}