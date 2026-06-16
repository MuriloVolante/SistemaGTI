package com.gti.usuarios.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtUtil jwtUtil;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validarToken(token)) {
                Claims claims = jwtUtil.extrairClaims(token);
                log.debug("Token valido para usuario: {}", claims.getSubject());
                String tipoAcesso = claims.get("tipoAcesso", String.class);
                var authorities = tipoAcesso != null
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + tipoAcesso))
                        : List.<SimpleGrantedAuthority>of();
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                // Gating: força troca de senha antes de liberar a API
                Boolean precisaTrocar = claims.get("precisaTrocarSenha", Boolean.class);
                String uri = request.getRequestURI();
                if (Boolean.TRUE.equals(precisaTrocar)
                        && uri.startsWith("/api/")
                        && !uri.startsWith("/api/auth/")) {
                    log.warn("Acesso bloqueado — troca de senha pendente: {}", uri);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"erro\":\"Troca de senha obrigatória.\"}");
                    return;
                }
            } else {
                log.warn("Token invalido recebido em: {}", request.getRequestURI());
            }
        }

        chain.doFilter(request, response);
    }
}