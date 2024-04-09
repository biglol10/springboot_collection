package com.biglol.springsecuritypractice.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;

import java.security.Key;

/**
 * JwsHeader를 통해 Signature 검증에 필요한 Key를 가져오는 코드 구현
 */
public class SigningKeyResolver extends SigningKeyResolverAdapter {
    public static SigningKeyResolver instance = new SigningKeyResolver();

    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        String kid = header.getKeyId(); // header에서 kid를 찾고
        if (kid == null) return null;
        return JwtKey.getKey(kid); // 그 kid로 비밀키를 가져옴
    }
}
