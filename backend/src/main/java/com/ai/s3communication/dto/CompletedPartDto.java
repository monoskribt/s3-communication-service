package com.ai.s3communication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CompletedPartDto {

    private int partNumber;
    private String eTag;

}
