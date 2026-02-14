package com.ai.s3communication.controller;

import com.ai.s3communication.dto.request.CompleteMultipartRequest;
import com.ai.s3communication.dto.request.MultipartUploadRequest;
import com.ai.s3communication.dto.response.MultipartUploadResponse;
import com.ai.s3communication.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service service;

    @PostMapping("/upload")
    public String upload(MultipartFile file) {
        return service.upload(file);
    }

    @PostMapping("/upload/multipart")
    public MultipartUploadResponse multipartUpload(@RequestBody MultipartUploadRequest request) {
        return service.multipartUpload(request);
    }

    @PostMapping("/upload/multipart/complete")
    public void completeMultipart(@RequestBody CompleteMultipartRequest request) {
        service.completeMultipart(request);
    }
}
