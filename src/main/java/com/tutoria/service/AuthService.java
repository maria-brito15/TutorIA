package com.tutoria.service;

import com.tutoria.util.ConfigLoader;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class AuthService {

    private final Key chave;
    private final long expiracaoMillis = 1000L * 60 * 60 * 24;

    public AuthService() {
        String segredo = ConfigLoader.getJwtSecret();
        
        if (segredo == null || segredo.length() < 32) {
            throw new IllegalArgumentException("jwt.secret deve ter pelo menos 32 caracteres");
        }

        this.chave = Keys.hmacShaKeyFor(segredo.getBytes());
    }

    public String gerarToken(int userId, String email) {
        Date agora = new Date();
        Date expira = new Date(agora.getTime() + expiracaoMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("email", email)
                .setIssuedAt(agora)
                .setExpiration(expira)
                .signWith(chave, SignatureAlgorithm.HS256)
                .compact();
    }


    public Jws<Claims> validarToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(chave)
                .build()
                .parseClaimsJws(token);
    }

    public int getUserId(String token) {
        return Integer.parseInt(validarToken(token).getBody().getSubject());
    }

    public String getUserEmail(String token) {
        return validarToken(token).getBody().get("email", String.class);
    }
}
