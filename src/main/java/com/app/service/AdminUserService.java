
package com.app.service;

import com.app.dto.admin.AdminUserDTO;
import com.app.model.AdminUser;
import com.app.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserDTO create(AdminUserDTO dto) {
        AdminUser user = new AdminUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        AdminUser saved = adminUserRepository.save(user);
        dto.setId(saved.getId());
        dto.setPassword(null); // Don't expose password
        return dto;
    }
}
