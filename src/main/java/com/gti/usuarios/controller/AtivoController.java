package com.gti.usuarios.controller;

import com.gti.usuarios.model.Ativo;
import com.gti.usuarios.model.AnexoAtivo;
import com.gti.usuarios.model.ValorCampo;
import com.gti.usuarios.service.AtivoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ativos")
public class AtivoController {

    private static final Logger log = LoggerFactory.getLogger(AtivoController.class);

    private final AtivoService service;

    public AtivoController(AtivoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Ativo>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/{id}/valores")
    public ResponseEntity<?> buscarValores(@PathVariable Long id) {
        try {
            List<ValorCampo> valores = service.buscarValores(id);
            return ResponseEntity.ok(valores);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> dados) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(dados));
        } catch (RuntimeException e) {
            log.warn("Erro ao criar ativo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id,
                                       @RequestBody Map<String, Object> dados) {
        try {
            return ResponseEntity.ok(service.atualizar(id, dados));
        } catch (RuntimeException e) {
            log.warn("Erro ao atualizar ativo ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/anexos")
    public ResponseEntity<?> uploadAnexo(@PathVariable Long id,
                                         @RequestParam("arquivo") MultipartFile arquivo) {
        try {
            AnexoAtivo a = service.salvarAnexo(id, arquivo);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", a.getId(),
                    "nome", a.getNome(),
                    "tamanho", a.getTamanho()));
        } catch (RuntimeException e) {
            log.warn("Erro ao salvar anexo do ativo {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/{id}/anexos")
    public ResponseEntity<?> listarAnexos(@PathVariable Long id) {
        return ResponseEntity.ok(service.listarAnexos(id));
    }

    @GetMapping("/anexos/{anexoId}")
    public ResponseEntity<?> baixarAnexo(@PathVariable Long anexoId) {
        try {
            AnexoAtivo a = service.buscarAnexo(anexoId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + a.getNome() + "\"")
                    .body(a.getConteudo());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/anexos/{anexoId}")
    public ResponseEntity<?> excluirAnexo(@PathVariable Long anexoId) {
        try {
            service.excluirAnexo(anexoId);
            return ResponseEntity.ok(Map.of("mensagem", "Anexo removido."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/{id}/impacto-exclusao")
    public ResponseEntity<?> impactoExclusao(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.impactoExclusao(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        try {
            service.excluirFisico(id);
            return ResponseEntity.ok(Map.of("mensagem", "Ativo excluído permanentemente."));
        } catch (RuntimeException e) {
            log.warn("Erro ao excluir ativo {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }
}