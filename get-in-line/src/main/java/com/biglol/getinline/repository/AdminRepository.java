package com.biglol.getinline.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.biglol.getinline.domain.Admin;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);
}
