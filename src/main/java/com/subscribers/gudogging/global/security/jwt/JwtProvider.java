package com.subscribers.gudogging.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";

    private final SecretKey secretKey;
    private final long accessExpirationSeconds;
    @Getter
    private final long refreshExpirationSeconds;

    public JwtProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-expiration-seconds}") long accessExpirationSeconds,
        @Value("${jwt.refresh-expiration-seconds}") long refreshExpirationSeconds
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpirationSeconds = accessExpirationSeconds;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    public String createAccessToken(UUID userId) {
        return createToken(userId, TokenType.ACCESS, accessExpirationSeconds);
    }

    public String createRefreshToken(UUID userId) {
        return createToken(userId, TokenType.REFRESH, refreshExpirationSeconds);
    }

    /**
     * Access Token을 검증하고 userId를 반환한다. 만료 시 ExpiredJwtException, 서명·형식·type 오류 시 JwtException 계열 예외를 던진다.
     */
    public UUID parseAccessToken(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        if (!TokenType.ACCESS.name().equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
            throw new MalformedJwtException("Access Token이 아닙니다.");
        }
        return UUID.fromString(claims.getSubject());
    }

    private String createToken(UUID userId, TokenType tokenType, long expirationSeconds) {
        Date now = new Date();
        return Jwts.builder()
            .subject(userId.toString())
            .claim(TOKEN_TYPE_CLAIM, tokenType.name())
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationSeconds * 1000L))
            .signWith(secretKey)
            .compact();
    }
}
