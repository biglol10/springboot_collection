package com.biglol.getinline.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.biglol.getinline.domain.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByEmail(String email);
}
