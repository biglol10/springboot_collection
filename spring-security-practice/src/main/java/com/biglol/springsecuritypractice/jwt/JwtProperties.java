package com.biglol.springsecuritypractice.jwt;

/**
 * JWT 기본 설정값
 */
public class JwtProperties {
    public static final int EXPIRATION_TIME = 600000; // 10min
    public static final String COOKIE_NAME = "JWT-AUTHENTICATION";
}
