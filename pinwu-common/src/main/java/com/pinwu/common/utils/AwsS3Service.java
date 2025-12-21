package com.pinwu.common.utils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import com.pinwu.common.utils.uuid.IdUtils; // 若依自带的UUID工具
import com.pinwu.common.utils.DateUtils;   // 若依自带的日期工具

import javax.annotation.PostConstruct;
import java.io.IOException;

@Component
public class AwsS3Service {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.domain}")
    private String domain;

    private AmazonS3 s3Client;

    /**
     * 初始化 S3 客户端
     */
    @PostConstruct
    public void init() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }

    /**
     * 上传文件
     * @param file 前端传来的文件
     * @return 完整访问URL
     */
    public String uploadFile(MultipartFile file) {
        // 1. 生成唯一文件名: 2025/12/17/uuid.jpg
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = DateUtils.datePath() + "/" + IdUtils.fastUUID() + suffix;
        
        // S3 的 Key (文件路径)
        String objectKey = "items/" + fileName; 

        try {
            // 2. 准备元数据 (告诉S3这是什么类型的文件)
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            // 3. 执行上传
            s3Client.putObject(new PutObjectRequest(bucketName, objectKey, file.getInputStream(), metadata));

            // 4. 返回 URL
            return domain + "/" + objectKey;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }
}