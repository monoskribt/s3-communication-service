package com.ai.s3communication.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultipartUploadRequest {

    private String fileName;
    private Long fileSize;

}
