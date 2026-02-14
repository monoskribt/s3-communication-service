package com.ai.s3communication;

import com.ai.s3communication.config.S3Settings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(value = {S3Settings.class})
public class S3CommunicationApplication {

    public static void main(String[] args) {
        SpringApplication.run(S3CommunicationApplication.class, args);
    }

}
