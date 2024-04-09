package com.biglol.springsecuritypractice.jwt;

import com.biglol.springsecuritypractice.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import org.springframework.data.util.Pair;

import java.security.Key;
import java.util.Date;

public class JwtUtils {
    /**
     * 토큰에서 username 찾기
     *
     * @param token 토큰
     * @return username
     */
    public static String getUsername(String token) {
        // jwtToken에서 username을 찾음
        return Jwts.parserBuilder() // jwt를 만드는게 아니라 parse 해야 하기에
                .setSigningKeyResolver(SigningKeyResolver.instance)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject(); // username
    }

    /**
     * user로 토큰 생성
     * HEADER : alg (알고리즘 종류), kid (key의 id)
     * PAYLOAD : sub (subject = username), iat (토큰 발행시간), exp (토큰 만료시간)
     * SIGNATURE : JwtKey.getRandomKey로 구한 Secret Key로 HS512 해시
     *
     * @param user 유저
     * @return jwt token
     */
    public static String createToken(User user) { // User 엔터티를 넘기면 그 유저에 해당하는 토큰을 만들어냄
        Claims claims = Jwts.claims().setSubject(user.getUsername()); // subject
        Date now = new Date(); // current
        Pair<String, Key> key = JwtKey.getRandomKey(); // <kid, 비밀 키에 대한 Key 객체>

        // JWT Token 생성
        return Jwts.builder()
                .setClaims(claims) // 정보 저장
                .setIssuedAt(now) // 토큰 발행 시간 정보
                .setExpiration(new Date(now.getTime() + JwtProperties.EXPIRATION_TIME))
                .setHeaderParam(JwsHeader.KEY_ID, key.getFirst()) // kid
                .signWith(key.getSecond()) // signature 만드는거에 대한 값 넘김
                .compact(); // JWT token 생성
    }
}
