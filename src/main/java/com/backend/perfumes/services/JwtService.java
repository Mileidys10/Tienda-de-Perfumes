package com.backend.perfumes.services;

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

    @Value("${app.jwt.refreshExpirationMs:86400000}") // 24 horas por defecto
    private long refreshExpirationMs;

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSignInKey())
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
        if (extraClaims == null) {
            extraClaims = new HashMap<>();
        }

        extraClaims.putIfAbsent("id", user.getId());
        extraClaims.putIfAbsent("email", user.getEmail());
        extraClaims.putIfAbsent("rol", user.getRole().name());
        extraClaims.put("type", "access"); // Tipo de token

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Nuevo método para generar refresh token
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("email", user.getEmail());
        claims.put("type", "refresh"); // Tipo de token

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Método para validar si un token es un refresh token
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "refresh".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    // Método para validar si un token es un access token
    public boolean isAccessToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return "access".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public String generateVerificationToken(User user) {
        return UUID.randomUUID().toString();
    }

    public String generateDeleteAccountToken(User user) {
        return "del_" + UUID.randomUUID().toString();
    }

    public boolean isDeleteToken(String token) {
        return token != null && token.startsWith("del_");
    }

    public boolean isVerificationToken(String token) {
        return token != null && !token.startsWith("del_");
    }

    public boolean isValidUUID(String token) {
        try {
            if (token.startsWith("del_")) {
                UUID.fromString(token.substring(4));
            } else {
                UUID.fromString(token);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}