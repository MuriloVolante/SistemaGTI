package com.gti.usuarios.controller;

import com.gti.usuarios.model.Ativo;
import com.gti.usuarios.model.Manutencao;
import com.gti.usuarios.repository.AtivoRepository;
import com.gti.usuarios.repository.ManutencaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ativos")
public class ManutencaoController {

    private static final Logger log = LoggerFactory.getLogger(ManutencaoController.class);

    private final ManutencaoRepository manutencaoRepo;
    private final AtivoRepository ativoRepo;

    public ManutencaoController(ManutencaoRepository manutencaoRepo,
                                AtivoRepository ativoRepo) {
        this.manutencaoRepo = manutencaoRepo;
        this.ativoRepo      = ativoRepo;
    }

    private static final int MAX_TAM_DESCRICAO = 1000;
    private static final int MAX_TAM_TECNICO = 150;
    private static final java.util.Set<String> TIPOS_MANUTENCAO =
            java.util.Set.of("CORRETIVA", "PREVENTIVA", "PREDITIVA");
    private static final BigDecimal MAX_CUSTO = new BigDecimal("9999999999.99");

    private static String textoDe(Object valor, String campo) {
        if (valor == null) return null;
        if (!(valor instanceof String s))
            throw new RuntimeException(campo + " inválido.");
        return s.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
    }

    @GetMapping("/{id}/manutencoes")
    public ResponseEntity<List<Manutencao>> listar(@PathVariable Long id) {
        log.debug("GET manutencoes ativo {}", id);
        return ResponseEntity.ok(manutencaoRepo.findByAtivoIdOrderByDataManutencaoDesc(id));
    }

    @PostMapping("/{id}/manutencoes")
    @Transactional
    public ResponseEntity<?> criar(@PathVariable Long id,
                                   @RequestBody(required = false) Map<String, Object> body) {
        log.info("POST manutencao ativo {}", id);
        try {
            if (body == null)
                return ResponseEntity.badRequest().body(Map.of("erro", "Dados da manutenção são obrigatórios."));

            Ativo ativo = ativoRepo.findById(id).orElse(null);
            if (ativo == null)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("erro", "Ativo não encontrado. ID: " + id));

            if ("DESCARTADO".equals(ativo.getStatus()))
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Não é possível registrar manutenção em um ativo descartado."));

            String descricao = textoDe(body.get("descricao"), "Descrição");
            if (descricao == null || descricao.isEmpty())
                return ResponseEntity.badRequest().body(Map.of("erro", "Descrição da manutenção é obrigatória."));
            if (descricao.length() < 5)
                return ResponseEntity.badRequest().body(Map.of("erro", "Descrição deve ter no mínimo 5 caracteres."));
            if (descricao.length() > MAX_TAM_DESCRICAO)
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Descrição deve ter no máximo " + MAX_TAM_DESCRICAO + " caracteres."));

            String tipo = textoDe(body.get("tipo"), "Tipo de manutenção");
            tipo = (tipo == null || tipo.isEmpty()) ? "CORRETIVA" : tipo.toUpperCase();
            if (!TIPOS_MANUTENCAO.contains(tipo))
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Tipo de manutenção inválido. Use: CORRETIVA, PREVENTIVA ou PREDITIVA."));

            String tecnico = textoDe(body.get("tecnico"), "Técnico");
            if (tecnico != null && tecnico.length() > MAX_TAM_TECNICO)
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Nome do técnico deve ter no máximo " + MAX_TAM_TECNICO + " caracteres."));
            if (tecnico != null && tecnico.isEmpty()) tecnico = null;

            Object dataBruta = body.get("dataManutencao");
            if (dataBruta == null || dataBruta.toString().isBlank())
                return ResponseEntity.badRequest().body(Map.of("erro", "Data da manutenção é obrigatória."));
            LocalDate dataManutencao;
            try {
                dataManutencao = LocalDate.parse(dataBruta.toString().trim());
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Data da manutenção inválida. Use o formato AAAA-MM-DD."));
            }
            if (dataManutencao.isAfter(LocalDate.now()))
                return ResponseEntity.badRequest().body(Map.of("erro", "Data da manutenção não pode ser futura."));
            if (ativo.getDataCompra() != null && dataManutencao.isBefore(ativo.getDataCompra()))
                return ResponseEntity.badRequest()
                        .body(Map.of("erro", "Data da manutenção não pode ser anterior à data de compra do ativo."));

            BigDecimal custo = null;
            Object custoBruto = body.get("custo");
            if (custoBruto != null && !custoBruto.toString().isBlank()) {
                try {
                    custo = new BigDecimal(custoBruto.toString().trim().replace(',', '.'));
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of("erro", "Custo inválido."));
                }
                if (custo.compareTo(BigDecimal.ZERO) < 0)
                    return ResponseEntity.badRequest().body(Map.of("erro", "Custo não pode ser negativo."));
                if (custo.compareTo(MAX_CUSTO) > 0)
                    return ResponseEntity.badRequest().body(Map.of("erro", "Custo excede o limite permitido."));
                if (custo.stripTrailingZeros().scale() > 2)
                    return ResponseEntity.badRequest().body(Map.of("erro", "Custo deve ter no máximo 2 casas decimais."));
            }

            Manutencao m = new Manutencao();
            m.setAtivoId(id);
            m.setDescricao(descricao);
            m.setTipo(tipo);
            m.setTecnico(tecnico);
            m.setGarantia(Boolean.TRUE.equals(body.get("garantia")));
            m.setCusto(custo);
            m.setDataManutencao(dataManutencao);

            manutencaoRepo.save(m);

            String statusAnterior = ativo.getStatus();
            ativo.setStatus("MANUTENCAO");
            ativoRepo.save(ativo);

            log.info("Manutencao registrada ativo {} - {} -> MANUTENCAO", id, statusAnterior);
            return ResponseEntity.status(HttpStatus.CREATED).body(m);
        } catch (RuntimeException e) {
            log.warn("Erro ao registrar manutencao do ativo {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}