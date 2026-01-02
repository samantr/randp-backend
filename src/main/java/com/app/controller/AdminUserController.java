package com.app.controller;

import com.app.dto.admin.AdminUserDTO;
import com.app.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin-users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AdminUserDTO> createAdminUser(@Valid @RequestBody AdminUserDTO dto) {
        return ResponseEntity.status(201).body(adminUserService.create(dto));
    }
}
