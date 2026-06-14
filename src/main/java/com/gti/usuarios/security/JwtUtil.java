package com.gti.usuarios.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    private static final long EXPIRATION_MS = 8 * 60 * 60 * 1000; // 8 horas

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String gerarToken(Long id, String nomeUsuario, String tipoAcesso, boolean precisaTrocarSenha) {
        String token = Jwts.builder()
                .setSubject(nomeUsuario)
                .claim("id", id)
                .claim("tipoAcesso", tipoAcesso)
                .claim("precisaTrocarSenha", precisaTrocarSenha)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
        log.debug("Token JWT gerado para usuario: {} | tipo: {} | precisaTrocarSenha: {}", nomeUsuario, tipoAcesso, precisaTrocarSenha);
        return token;
    }

    public Claims extrairClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validarToken(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado");
            return false;
        } catch (Exception e) {
            log.warn("Token JWT invalido: {}", e.getMessage());
            return false;
        }
    }

    public String extrairUsuario(String token) {
        return extrairClaims(token).getSubject();
    }
}