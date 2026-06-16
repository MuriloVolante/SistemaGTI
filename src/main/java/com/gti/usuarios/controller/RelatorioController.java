package com.gti.usuarios.controller;

import com.gti.usuarios.model.Ativo;
import com.gti.usuarios.repository.AtivoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import com.gti.usuarios.model.ValorCampo;
import com.gti.usuarios.repository.ValorCampoRepository;

@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private static final Logger log = LoggerFactory.getLogger(RelatorioController.class);

    private final AtivoRepository ativoRepo;
    private final ValorCampoRepository valorCampoRepo;

    public RelatorioController(AtivoRepository ativoRepo,
                               ValorCampoRepository valorCampoRepo) {
        this.ativoRepo      = ativoRepo;
        this.valorCampoRepo = valorCampoRepo;
    }

    // ── Tabela ────────────────────────────────────────────────────────────────
    @GetMapping("/ativos")
    public ResponseEntity<List<Map<String, Object>>> tabelaAtivos(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String centroCusto,
            @RequestParam(required = false) String responsavel) {

        log.debug("Gerando tabela de ativos — filtros: tipo={}, status={}, centroCusto={}, responsavel={}",
                tipo, status, centroCusto, responsavel);

        List<Ativo> ativos = ativoRepo.findAll();

        if (tipo != null && !tipo.isBlank())
            ativos = ativos.stream().filter(a -> a.getTipo() != null &&
                    a.getTipo().getNome().equalsIgnoreCase(tipo)).collect(Collectors.toList());
        if (status != null && !status.isBlank())
            ativos = ativos.stream().filter(a -> a.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        if (centroCusto != null && !centroCusto.isBlank())
            ativos = ativos.stream().filter(a -> a.getCentroCusto().equalsIgnoreCase(centroCusto))
                    .collect(Collectors.toList());
        if (responsavel != null && !responsavel.isBlank())
            ativos = ativos.stream().filter(a -> a.getResponsavel() != null &&
                            a.getResponsavel().getNomeCompleto().equalsIgnoreCase(responsavel))
                    .collect(Collectors.toList());

        List<Map<String, Object>> resultado = ativos.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",            a.getId());
            m.put("patrimonio",    a.getPatrimonio());
            m.put("tipo",          a.getTipo() != null ? a.getTipo().getNome() : null);
            m.put("tipoId",        a.getTipo() != null ? a.getTipo().getId() : null);
            m.put("marcaModelo",   a.getMarcaModelo());
            m.put("responsavel",   a.getResponsavel() != null ? a.getResponsavel().getNomeCompleto() : null);
            m.put("centroCusto",   a.getCentroCusto());
            m.put("status",        a.getStatus());
            m.put("dataCompra",    a.getDataCompra());
            m.put("garantiaAte",   a.getGarantiaAte());
            m.put("vidaUtilAnos",  a.getVidaUtilMeses() != null ? Math.round(a.getVidaUtilMeses() / 12.0) : null);
            m.put("valorAquisicao",a.getValorAquisicao());

            // Campos dinamicos
            List<ValorCampo> valores = valorCampoRepo.findByAtivoId(a.getId());
            Map<String, String> dinamicos = new LinkedHashMap<>();
            for (ValorCampo vc : valores) {
                dinamicos.put(vc.getCampo().getNomeDoCampo(), vc.getValor());
            }
            m.put("camposDinamicos", dinamicos);

            return m;
        }).collect(Collectors.toList());

        log.debug("Tabela gerada. Total de registros: {}", resultado.size());
        return ResponseEntity.ok(resultado);
    }

    // ── Exportação CSV ────────────────────────────────────────────────────────
    @GetMapping("/ativos/export")
    public ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String centroCusto,
            @RequestParam(required = false) String responsavel) {

        log.debug("Exportando CSV de ativos");

        List<Ativo> ativos = ativoRepo.findAll();

        if (tipo != null && !tipo.isBlank())
            ativos = ativos.stream().filter(a -> a.getTipo() != null &&
                    a.getTipo().getNome().equalsIgnoreCase(tipo)).collect(Collectors.toList());
        if (status != null && !status.isBlank())
            ativos = ativos.stream().filter(a -> a.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        if (centroCusto != null && !centroCusto.isBlank())
            ativos = ativos.stream().filter(a -> a.getCentroCusto().equalsIgnoreCase(centroCusto))
                    .collect(Collectors.toList());
        if (responsavel != null && !responsavel.isBlank())
            ativos = ativos.stream().filter(a -> a.getResponsavel() != null &&
                            a.getResponsavel().getNomeCompleto().toLowerCase().contains(responsavel.toLowerCase()))
                    .collect(Collectors.toList());

        String[] headers = {
                "ID", "Patrimônio", "Tipo", "Marca/Modelo", "Responsável",
                "Centro de Custo", "Status", "Data de Compra", "Garantia até",
                "Vida útil (anos)", "Valor de Aquisição (R$)"
        };

        StringBuilder csv = new StringBuilder();
        // BOM UTF-8 para Excel BR
        csv.append('\uFEFF');
        csv.append(String.join(";", headers)).append("\n");

        for (Ativo a : ativos) {
            csv.append(csvVal(a.getId()))
                    .append(";").append(csvVal(a.getPatrimonio()))
                    .append(";").append(csvVal(a.getTipo() != null ? a.getTipo().getNome() : ""))
                    .append(";").append(csvVal(a.getMarcaModelo()))
                    .append(";").append(csvVal(a.getResponsavel() != null ? a.getResponsavel().getNomeCompleto() : ""))
                    .append(";").append(csvVal(a.getCentroCusto()))
                    .append(";").append(csvVal(a.getStatus()))
                    .append(";").append(csvVal(a.getDataCompra()))
                    .append(";").append(csvVal(a.getGarantiaAte()))
                    .append(";").append(csvVal(a.getVidaUtilMeses() != null ? Math.round(a.getVidaUtilMeses() / 12.0) : ""))
                    .append(";").append(csvVal(a.getValorAquisicao()))
                    .append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);

        String filename = "ativos_" + java.time.LocalDate.now() + ".csv";

        log.info("CSV exportado. Registros: {} | arquivo: {}", ativos.size(), filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(bytes);
    }

    private String csvVal(Object val) {
        if (val == null) return "";
        String s = val.toString();
        if (s.contains(";") || s.contains("\"") || s.contains("\n"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}