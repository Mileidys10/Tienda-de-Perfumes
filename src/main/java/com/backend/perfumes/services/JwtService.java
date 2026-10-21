package com.backend.perfumes.services;

import com.backend.perfumes.model.TokenType;
import com.backend.perfumes.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expirationMs}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refreshExpirationMs:86400000}")
    private long refreshExpirationMs;

    private Key getSigninKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private static final String CLAIM_ID = "id";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "rol";
    private static final String CLAIM_TYPE = "type";

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private static final String DELETE_PREFIX = "del_";


    private String buildToken(Map<String, Object> claims, User user, long expiration) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiration))
                .signWith(getSigninKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSigninKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public String generateToken(Map<String, Object> extraClaims, User user) {
        Map<String, Object> claims = new HashMap<>();

        if (extraClaims != null) {
            claims.putAll(extraClaims);
        }


        claims.put(CLAIM_ID, user.getId());
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_ROLE, user.getRole().name());
        claims.put(CLAIM_TYPE, TokenType.ACCESS.name());

        return buildToken(claims, user, jwtExpirationMs);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();


        claims.put(CLAIM_ID, user.getId());
        claims.put(CLAIM_EMAIL, user.getEmail());
        claims.put(CLAIM_TYPE, TokenType.REFRESH.name());

        return buildToken(claims, user, refreshExpirationMs);
    }

    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }


    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "access".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

}















