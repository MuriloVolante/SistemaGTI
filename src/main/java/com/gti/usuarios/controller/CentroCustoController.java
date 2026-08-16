package com.gti.usuarios.controller;

import com.gti.usuarios.model.CentroCusto;
import com.gti.usuarios.service.CentroCustoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/centros-custo")
public class CentroCustoController {

    private final CentroCustoService service;

    public CentroCustoController(CentroCustoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CentroCusto>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, String> dados) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dados.get("nome")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }
}