
package com.app.controller;

import com.app.dto.admin.AdminUserDTO;
import com.app.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin-users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PostMapping
    public AdminUserDTO createAdminUser(@Valid @RequestBody AdminUserDTO dto) {
        return adminUserService.create(dto);
    }
}
