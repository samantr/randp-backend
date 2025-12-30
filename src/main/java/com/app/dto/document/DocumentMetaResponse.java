package com.app.dto.document;

public record DocumentMetaResponse(
        Long id,
        Long ownerId,
        long sizeBytes,
        String dsc
) {}
