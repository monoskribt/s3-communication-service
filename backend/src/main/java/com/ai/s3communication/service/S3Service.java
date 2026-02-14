package com.ai.s3communication.service;

import com.ai.s3communication.config.S3Settings;
import com.ai.s3communication.dto.PresignedPart;
import com.ai.s3communication.dto.request.CompleteMultipartRequest;
import com.ai.s3communication.dto.request.MultipartUploadRequest;
import com.ai.s3communication.dto.response.MultipartUploadResponse;
import com.ai.s3communication.exception.FileValidationException;
import com.ai.s3communication.utils.S3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service {

    private final S3Settings settings;
    private final S3Client client;
    private final S3Presigner presigner;

    private static final long TEN_MB = 10 * 1024 * 1024;
    private static final long PART_SIZE = 5 * 1024 * 1024;

    public String upload(MultipartFile file) {
        if (file.getSize() > TEN_MB) {
            throw new FileValidationException("File > 10MB. Use multipart upload");
        }

        var key = generateS3Key(file.getName());

        try {
            client.putObject(PutObjectRequest.builder()
                            .bucket(settings.getBucketName())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return key;
    }

    public MultipartUploadResponse multipartUpload(MultipartUploadRequest request) {
        if (request.getFileSize() < TEN_MB) {
            throw new FileValidationException("File < 10MB. Use default upload");
        }

        var key = generateS3Key(request.getFileName());

        var createS3Request = CreateMultipartUploadRequest.builder()
                .bucket(settings.getBucketName())
                .key(key)
                .build();
        var S3response = client.createMultipartUpload(createS3Request);

        var uploadId = S3response.uploadId();
        var partCount = (int) Math.ceil((double) request.getFileSize() / PART_SIZE);
        var urls = new ArrayList<PresignedPart>();

        for (var i = 1; i <= partCount; i++) {
            var uploadPartRequest = UploadPartRequest.builder()
                    .bucket(settings.getBucketName())
                    .key(key)
                    .uploadId(uploadId)
                    .partNumber(i)
                    .build();
            var presignedRequest = presigner.presignUploadPart(pres -> pres.signatureDuration(Duration.ofMinutes(10))
                    .uploadPartRequest(uploadPartRequest));

            urls.add(new PresignedPart(i, presignedRequest.url().toString()));
        }

        return new MultipartUploadResponse(uploadId, key, PART_SIZE, urls);
    }

    public void completeMultipart(CompleteMultipartRequest request) {
        var completedParts = request.getParts().stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.getPartNumber())
                        .eTag(part.getETag())
                        .build())
                .toList();

        client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                .bucket(settings.getBucketName())
                .key(request.getKey())
                .uploadId(request.getUploadId())
                .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(completedParts)
                        .build())
                .build());
    }

    private String generateS3Key(String fileName) {
        return S3Utils.S3_BUCKET_FOLDER + fileName + "-" + LocalDateTime.now();
    }
}
