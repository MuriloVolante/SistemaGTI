package com.gti.usuarios.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class IntegracaoApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IntegracaoApiKeyFilter.class);

    @Value("${integracao.api-key}")
    private String apiKeyEsperada;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/integracao/") || uri.startsWith("/api/integracao/preview/")) {
            chain.doFilter(request, response);
            return;
        }

        String chaveRecebida = request.getHeader("X-API-Key");
        if (chaveRecebida == null ||
                !java.security.MessageDigest.isEqual(
                        chaveRecebida.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        apiKeyEsperada.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            log.warn("Tentativa de acesso a /api/integracao com API key inválida. IP: {}", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"erro\":\"API key inválida ou ausente.\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}