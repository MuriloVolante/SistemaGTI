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
import java.time.YearMonth;
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

        BigDecimal valorTotalDepreciado = ativos.stream()
                .filter(a -> a.getValorAquisicao() != null)
                .map(this::calcularValorAtual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> porStatus = ativos.stream()
                .collect(Collectors.groupingBy(Ativo::getStatus, Collectors.counting()));

        Map<String, Long> porTipo = ativos.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getTipo() != null ? a.getTipo().getNome() : "Sem tipo",
                        Collectors.counting()));

        List<Long> ativoIds = ativos.stream().map(Ativo::getId).collect(Collectors.toList());

        List<Manutencao> manutencoesAtivos = manutencaoRepo.findAll().stream()
                .filter(m -> ativoIds.contains(m.getAtivoId()))
                .collect(Collectors.toList());

        BigDecimal custoManutencao = manutencoesAtivos.stream()
                .filter(m -> m.getCusto() != null)
                .map(Manutencao::getCusto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate daqui90dias = LocalDate.now().plusDays(90);
        long proximosFimVida = ativos.stream()
                .filter(a -> a.getTipo() != null && a.getTipo().getVidaUtilMeses() != null && a.getDataCompra() != null)
                .filter(a -> {
                    LocalDate fimVida = a.getDataCompra().plusMonths(a.getTipo().getVidaUtilMeses());
                    return !fimVida.isBefore(LocalDate.now()) && !fimVida.isAfter(daqui90dias);
                })
                .count();

        LocalDate inicio12m = LocalDate.now().minusMonths(12);

        BigDecimal custoManutencao12m = manutencoesAtivos.stream()
                .filter(m -> m.getCusto() != null && m.getDataManutencao() != null
                        && !m.getDataManutencao().isBefore(inicio12m))
                .map(Manutencao::getCusto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> custoManutencaoMensal = new LinkedHashMap<>();
        YearMonth mesAtual = YearMonth.now();
        for (int i = 11; i >= 0; i--)
            custoManutencaoMensal.put(mesAtual.minusMonths(i).toString(), BigDecimal.ZERO);
        manutencoesAtivos.stream()
                .filter(m -> m.getCusto() != null && m.getDataManutencao() != null)
                .forEach(m -> {
                    String chave = YearMonth.from(m.getDataManutencao()).toString();
                    if (custoManutencaoMensal.containsKey(chave))
                        custoManutencaoMensal.merge(chave, m.getCusto(), BigDecimal::add);
                });

        Map<String, BigDecimal> valorPorTipo = ativos.stream()
                .filter(a -> a.getValorAquisicao() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getTipo() != null ? a.getTipo().getNome() : "Sem tipo",
                        Collectors.reducing(BigDecimal.ZERO, Ativo::getValorAquisicao, BigDecimal::add)));

        Map<String, BigDecimal> valorPorCentroCusto = ativos.stream()
                .filter(a -> a.getValorAquisicao() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getCentroCusto() != null ? a.getCentroCusto() : "Sem centro",
                        Collectors.reducing(BigDecimal.ZERO, Ativo::getValorAquisicao, BigDecimal::add)));

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("total",                total);
        resultado.put("valorTotal",           valorTotal);
        resultado.put("valorTotalDepreciado", valorTotalDepreciado);
        resultado.put("porStatus",       porStatus);
        resultado.put("porTipo",         porTipo);
        resultado.put("custoManutencao", custoManutencao);
        resultado.put("proximosFimVida", proximosFimVida);
        Map<String, Long> porCentroCusto = ativos.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCentroCusto() != null ? a.getCentroCusto() : "Sem centro",
                        Collectors.counting()));
        resultado.put("porCentroCusto", porCentroCusto);

        resultado.put("custoManutencao12m",    custoManutencao12m);
        resultado.put("custoManutencaoMensal", custoManutencaoMensal);
        resultado.put("valorPorTipo",          valorPorTipo);
        resultado.put("valorPorCentroCusto",   valorPorCentroCusto);

        log.debug("Dashboard gerado. Total: {}", total);
        return ResponseEntity.ok(resultado);
    }

    // Depreciação linear: percentualDepreciacao é taxa anual, aplicada mês a mês
    // e capada em vidaUtilMeses. Sem tipo/config/data de compra -> valor cheio.
    private BigDecimal calcularValorAtual(Ativo a) {
        BigDecimal valor = a.getValorAquisicao();
        if (a.getTipo() == null || a.getTipo().getPercentualDepreciacao() == null
                || a.getTipo().getVidaUtilMeses() == null || a.getDataCompra() == null) {
            return valor;
        }

        long mesesDecorridos = java.time.temporal.ChronoUnit.MONTHS.between(a.getDataCompra(), LocalDate.now());
        if (mesesDecorridos <= 0) return valor;
        mesesDecorridos = Math.min(mesesDecorridos, a.getTipo().getVidaUtilMeses());

        BigDecimal percentualDepreciado = a.getTipo().getPercentualDepreciacao()
                .multiply(BigDecimal.valueOf(mesesDecorridos))
                .divide(BigDecimal.valueOf(12), 4, java.math.RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));

        BigDecimal fatorRestante = BigDecimal.ONE.subtract(
                percentualDepreciado.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
        if (fatorRestante.signum() < 0) fatorRestante = BigDecimal.ZERO;

        return valor.multiply(fatorRestante).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}