package com.ai.s3communication.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "aws")
public class S3Settings {

    private String region;
    private String accessKey;
    private String secretKey;
    private String bucketName;

}
