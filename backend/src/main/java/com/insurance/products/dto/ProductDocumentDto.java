package com.insurance.products.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocumentDto {

    private Long id;
    private Long productId;
    private Long categoryId;
    private String filename;
    private String storagePath;
    private String storageUrl;
    private Long fileSize;
    private String contentType;
    private String extractedText;
    private LocalDateTime createdAt;
}
