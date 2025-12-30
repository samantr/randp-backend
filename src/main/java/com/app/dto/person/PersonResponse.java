package com.app.dto.person;

public record PersonResponse(
        Long id,
        String name,
        String lastName,
        String companyName,
        String address,
        String tel,
        boolean isLegal,
        String dsc
) {}
