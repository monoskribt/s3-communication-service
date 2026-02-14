package com.ai.s3communication.dto.response;

import com.ai.s3communication.dto.PresignedPart;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MultipartUploadResponse {

    private String uploadId;
    private String key;
    private Long partSize;
    private List<PresignedPart> presignedUrls;

}
