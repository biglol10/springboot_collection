package com.biglol.getinline.service;

import java.util.List;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.biglol.getinline.domain.Admin;
import com.biglol.getinline.repository.AdminRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class AdminService implements UserDetailsService {

    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin =
                adminRepository
                        .findByEmail(username)
                        .orElseThrow(() -> new UsernameNotFoundException("Admin not found."));

        return User.builder()
                .username(admin.getEmail())
                .password(admin.getPassword())
                .authorities(List.of()) // 분리되어 있는 권한체계가 딱히 없으니 비어있는 객체
                .build();
    }
}
