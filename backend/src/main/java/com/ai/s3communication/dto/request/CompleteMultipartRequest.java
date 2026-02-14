package com.ai.s3communication.dto.request;

import com.ai.s3communication.dto.CompletedPartDto;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CompleteMultipartRequest {

    private String uploadId;
    private String key;
    private List<CompletedPartDto> parts;

}
