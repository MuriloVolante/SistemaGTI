package com.gti.usuarios.controller;

import com.gti.usuarios.model.HistoricoAtivo;
import com.gti.usuarios.repository.HistoricoAtivoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ativos")
public class HistoricoAtivoController {

    private static final Logger log = LoggerFactory.getLogger(HistoricoAtivoController.class);

    private final HistoricoAtivoRepository historicoRepo;

    public HistoricoAtivoController(HistoricoAtivoRepository historicoRepo) {
        this.historicoRepo = historicoRepo;
    }

    @GetMapping("/{id}/historico")
    public ResponseEntity<List<HistoricoAtivo>> listar(@PathVariable Long id) {
        log.debug("GET historico ativo {}", id);
        return ResponseEntity.ok(historicoRepo.findByAtivoIdOrderByCriadoEmDesc(id));
    }
}