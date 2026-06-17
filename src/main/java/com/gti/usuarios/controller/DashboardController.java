package com.gti.usuarios.controller;

import com.gti.usuarios.model.Ativo;
import com.gti.usuarios.model.Manutencao;
import com.gti.usuarios.repository.AtivoRepository;
import com.gti.usuarios.repository.ManutencaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final AtivoRepository ativoRepo;
    private final ManutencaoRepository manutencaoRepo;

    public DashboardController(AtivoRepository ativoRepo,
                               ManutencaoRepository manutencaoRepo) {
        this.ativoRepo      = ativoRepo;
        this.manutencaoRepo = manutencaoRepo;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> dashboard(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String centroCusto) {
        log.debug("Gerando dados do dashboard - filtros: tipo={}, status={}, centroCusto={}", tipo, status, centroCusto);

        List<Ativo> ativos = ativoRepo.findAll();

        if (tipo != null && !tipo.isBlank())
            ativos = ativos.stream().filter(a -> a.getTipo() != null &&
                    a.getTipo().getNome().equalsIgnoreCase(tipo)).collect(Collectors.toList());
        if (status != null && !status.isBlank())
            ativos = ativos.stream().filter(a -> a.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        if (centroCusto != null && !centroCusto.isBlank())
            ativos = ativos.stream().filter(a -> a.getCentroCusto() != null &&
                    a.getCentroCusto().equalsIgnoreCase(centroCusto)).collect(Collectors.toList());

        long total = ativos.size();

        BigDecimal valorTotal = ativos.stream()
                .filter(a -> a.getValorAquisicao() != null)
                .map(Ativo::getValorAquisicao)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> porStatus = ativos.stream()
                .collect(Collectors.groupingBy(Ativo::getStatus, Collectors.counting()));

        Map<String, Long> porTipo = ativos.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getTipo() != null ? a.getTipo().getNome() : "Sem tipo",
                        Collectors.counting()));

        List<Long> ativoIds = ativos.stream().map(Ativo::getId).collect(Collectors.toList());
        BigDecimal custoManutencao = manutencaoRepo.findAll().stream()
                .filter(m -> m.getCusto() != null && ativoIds.contains(m.getAtivoId()))
                .map(Manutencao::getCusto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate daqui90dias = LocalDate.now().plusDays(90);
        long proximosFimVida = ativos.stream()
                .filter(a -> a.getVidaUtilMeses() != null && a.getDataCompra() != null)
                .filter(a -> {
                    LocalDate fimVida = a.getDataCompra().plusMonths(a.getVidaUtilMeses());
                    return !fimVida.isBefore(LocalDate.now()) && !fimVida.isAfter(daqui90dias);
                })
                .count();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("total",           total);
        resultado.put("valorTotal",      valorTotal);
        resultado.put("porStatus",       porStatus);
        resultado.put("porTipo",         porTipo);
        resultado.put("custoManutencao", custoManutencao);
        resultado.put("proximosFimVida", proximosFimVida);
        Map<String, Long> porCentroCusto = ativos.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCentroCusto() != null ? a.getCentroCusto() : "Sem centro",
                        Collectors.counting()));
        resultado.put("porCentroCusto", porCentroCusto);

        log.debug("Dashboard gerado. Total: {}", total);
        return ResponseEntity.ok(resultado);
    }
}