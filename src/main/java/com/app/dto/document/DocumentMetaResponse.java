package com.app.dto.document;

public record DocumentMetaResponse(
        Long id,
        Long ownerId,
        long sizeBytes,
        String fileName,
        String contentType,
        String createdAt,
        String dsc
) {}
