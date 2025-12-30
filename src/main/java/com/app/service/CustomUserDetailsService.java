package com.app.service;


import com.app.model.AdminUser;
import com.app.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminUserRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser user = repository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        System.out.println("Loaded user: " + user.getUsername());
        System.out.println("Encoded password in DB: " + user.getPassword());


        return User.withUsername(user.getUsername())
                   .password(user.getPassword())
                   .roles("ADMIN")
                   .build();
    }
}
