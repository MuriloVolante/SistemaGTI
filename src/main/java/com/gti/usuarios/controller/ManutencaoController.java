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

    @GetMapping("/{id}/manutencoes")
    public ResponseEntity<List<Manutencao>> listar(@PathVariable Long id) {
        log.debug("GET manutencoes ativo {}", id);
        return ResponseEntity.ok(manutencaoRepo.findByAtivoIdOrderByDataManutencaoDesc(id));
    }

    @PostMapping("/{id}/manutencoes")
    public ResponseEntity<?> criar(@PathVariable Long id,
                                   @RequestBody Map<String, Object> body) {
        log.info("POST manutencao ativo {}", id);

        Ativo ativo = ativoRepo.findById(id).orElse(null);
        if (ativo == null) return ResponseEntity.notFound().build();

        Manutencao m = new Manutencao();
        m.setAtivoId(id);
        m.setDescricao((String) body.get("descricao"));
        m.setTipo((String) body.get("tipo"));
        m.setTecnico((String) body.get("tecnico"));
        m.setGarantia(Boolean.TRUE.equals(body.get("garantia")));

        if (body.get("custo") != null)
            m.setCusto(new BigDecimal(body.get("custo").toString()));
        if (body.get("dataManutencao") != null)
            m.setDataManutencao(LocalDate.parse(body.get("dataManutencao").toString()));

        manutencaoRepo.save(m);

        String statusAnterior = ativo.getStatus();
        ativo.setStatus("MANUTENCAO");
        ativoRepo.save(ativo);

        log.info("Manutencao registrada ativo {} — {} -> MANUTENCAO", id, statusAnterior);
        return ResponseEntity.ok(m);
    }
}