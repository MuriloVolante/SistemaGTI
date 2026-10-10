package com.gti.usuarios.security;

import com.gti.usuarios.model.ChaveApi;
import com.gti.usuarios.service.ChaveApiService;
import com.gti.usuarios.service.ChaveApiUsoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class IntegracaoApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IntegracaoApiKeyFilter.class);

    private static final String BASE = "/api/integracao/";

    private static final Map<String, String> ESCOPO_POR_RECURSO = Map.of(
            "ativos", "ATIVOS",
            "usuarios", "USUARIOS",
            "chamados", "CHAMADOS",
            "dashboard", "DASHBOARD"
    );

    @Value("${integracao.confiar-proxy:false}")
    private boolean confiarProxy;

    private final ChaveApiService chaveApiService;
    private final ChaveApiUsoService usoService;
    private final ChaveApiRateLimiter rateLimiter;

    public IntegracaoApiKeyFilter(@Lazy ChaveApiService chaveApiService,
                                  @Lazy ChaveApiUsoService usoService,
                                  ChaveApiRateLimiter rateLimiter) {
        this.chaveApiService = chaveApiService;
        this.usoService = usoService;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (!uri.startsWith(BASE)
                || uri.startsWith(BASE + "preview/")
                || uri.startsWith(BASE + "chaves")) {
            chain.doFilter(request, response);
            return;
        }

        String ip = ipDe(request);
        String metodo = request.getMethod();
        String userAgent = request.getHeader("User-Agent");
        String valor = request.getHeader("X-API-Key");

        if (valor == null || valor.isBlank()) {
            negar(response, 401, "Chave de API ausente.", null, null, uri, metodo, ip, userAgent, "SEM_CHAVE");
            return;
        }

        String prefixoTentado = chaveApiService.prefixoDe(valor);
        Optional<ChaveApi> encontrada = chaveApiService.buscarPorValor(valor);

        if (encontrada.isEmpty()) {
            negar(response, 401, "Chave de API inválida.", null, prefixoTentado, uri, metodo, ip, userAgent, "CHAVE_INVALIDA");
            return;
        }

        ChaveApi chave = encontrada.get();

        if (!chave.isAtiva()) {
            negar(response, 401, "Chave de API desligada.", chave, prefixoTentado, uri, metodo, ip, userAgent, "CHAVE_DESLIGADA");
            return;
        }

        if (chave.isExpirada()) {
            negar(response, 401, "Chave de API expirada.", chave, prefixoTentado, uri, metodo, ip, userAgent, "CHAVE_EXPIRADA");
            return;
        }

        if (!ipPermitido(chave.getIpsLista(), ip)) {
            negar(response, 403, "Endereço de rede não autorizado para esta chave.", chave, prefixoTentado, uri, metodo, ip, userAgent, "IP_NAO_AUTORIZADO");
            return;
        }

        String escopoNecessario = escopoDe(uri);
        if (escopoNecessario != null && !chave.getEscoposLista().contains(escopoNecessario)) {
            negar(response, 403, "Esta chave não tem acesso a este conjunto de dados.", chave, prefixoTentado, uri, metodo, ip, userAgent, "ESCOPO_NEGADO");
            return;
        }

        if (!rateLimiter.permitir(chave.getId())) {
            negar(response, 429, "Limite de chamadas por minuto excedido para esta chave.",
                    chave, prefixoTentado, uri, metodo, ip, userAgent, "LIMITE_EXCEDIDO");
            return;
        }

        chain.doFilter(request, response);

        usoService.registrar(chave, prefixoTentado, uri, metodo, ip, userAgent,
                response.getStatus(), response.getStatus() < 400, null);
    }

    private void negar(HttpServletResponse response, int status, String mensagem,
                       ChaveApi chave, String prefixoTentado, String uri, String metodo,
                       String ip, String userAgent, String motivo) throws IOException {
        log.warn("Acesso negado a {} - motivo: {} - ip: {}", uri, motivo, ip);
        usoService.registrar(chave, prefixoTentado, uri, metodo, ip, userAgent, status, false, motivo);
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"erro\":\"" + mensagem + "\",\"motivo\":\"" + motivo + "\"}");
    }

    private String escopoDe(String uri) {
        String resto = uri.substring(BASE.length());
        int barra = resto.indexOf('/');
        String recurso = barra >= 0 ? resto.substring(0, barra) : resto;
        return ESCOPO_POR_RECURSO.get(recurso);
    }

    private String ipDe(HttpServletRequest request) {
        if (confiarProxy) {
            String encaminhado = request.getHeader("X-Forwarded-For");
            if (encaminhado != null && !encaminhado.isBlank()) {
                return encaminhado.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private boolean ipPermitido(List<String> permitidos, String ip) {
        if (permitidos == null || permitidos.isEmpty()) return true;
        if (ip == null) return false;
        for (String p : permitidos) {
            if (p.equals(ip)) return true;
            if (p.contains("/") && dentroDaFaixa(p, ip)) return true;
        }
        return false;
    }

    private boolean dentroDaFaixa(String cidr, String ip) {
        try {
            String[] partes = cidr.split("/");
            int bits = Integer.parseInt(partes[1]);
            long rede = paraLong(partes[0]);
            long alvo = paraLong(ip);
            if (rede < 0 || alvo < 0) return false;
            long mascara = bits == 0 ? 0 : (-1L << (32 - bits)) & 0xFFFFFFFFL;
            return (rede & mascara) == (alvo & mascara);
        } catch (Exception e) {
            return false;
        }
    }

    private long paraLong(String ip) {
        String[] o = ip.split("\\.");
        if (o.length != 4) return -1;
        long r = 0;
        for (String parte : o) {
            int v = Integer.parseInt(parte);
            if (v < 0 || v > 255) return -1;
            r = (r << 8) | v;
        }
        return r;
    }
}