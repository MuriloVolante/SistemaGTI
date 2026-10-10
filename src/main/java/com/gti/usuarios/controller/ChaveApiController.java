package com.gti.usuarios.controller;

import com.gti.usuarios.dto.ChaveApiCriadaDTO;
import com.gti.usuarios.dto.ChaveApiCriarDTO;
import com.gti.usuarios.dto.ChaveApiDTO;
import com.gti.usuarios.dto.ChaveApiDetalheDTO;
import com.gti.usuarios.dto.ChaveApiUsoDTO;
import com.gti.usuarios.model.Usuario;
import org.springframework.data.domain.Page;
import com.gti.usuarios.repository.UsuarioRepository;
import com.gti.usuarios.service.ChaveApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/integracao/chaves")
public class ChaveApiController {

    private final ChaveApiService chaveApiService;
    private final UsuarioRepository usuarioRepository;

    public ChaveApiController(ChaveApiService chaveApiService,
                              UsuarioRepository usuarioRepository) {
        this.chaveApiService = chaveApiService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public ResponseEntity<List<ChaveApiDTO>> listar() {
        return ResponseEntity.ok(chaveApiService.listar());
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody ChaveApiCriarDTO dto) {
        try {
            Optional<Usuario> autor = usuarioAtual();
            ChaveApiCriadaDTO criada = chaveApiService.criar(
                    dto,
                    autor.map(Usuario::getId).orElse(null),
                    autor.map(Usuario::getNomeCompleto).orElse(nomeUsuarioAtual()));
            return ResponseEntity.ok(criada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detalhar(@PathVariable Long id) {
        try {
            ChaveApiDetalheDTO detalhe = chaveApiService.detalhar(id);
            return ResponseEntity.ok(detalhe);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/{id}/girar")
    public ResponseEntity<?> girar(@PathVariable Long id,
                                   @RequestBody(required = false) Map<String, Object> corpo) {
        try {
            Integer carencia = corpo != null && corpo.get("diasCarencia") instanceof Number n
                    ? n.intValue() : null;
            Optional<Usuario> autor = usuarioAtual();
            return ResponseEntity.ok(chaveApiService.girar(
                    id,
                    carencia,
                    autor.map(Usuario::getId).orElse(null),
                    autor.map(Usuario::getNomeCompleto).orElse(nomeUsuarioAtual())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    @GetMapping("/tentativas-negadas")
    public ResponseEntity<Page<ChaveApiUsoDTO>> tentativasNegadas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamanho) {
        return ResponseEntity.ok(chaveApiService.tentativasNegadas(pagina, tamanho));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> desligar(@PathVariable Long id) {
        try {
            Optional<Usuario> autor = usuarioAtual();
            return ResponseEntity.ok(chaveApiService.desligar(
                    id,
                    autor.map(Usuario::getNomeCompleto).orElse(nomeUsuarioAtual())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    private String nomeUsuarioAtual() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getPrincipal() == null) return "[desconhecido]";
        return String.valueOf(a.getPrincipal());
    }

    private Optional<Usuario> usuarioAtual() {
        String nome = nomeUsuarioAtual();
        if ("[desconhecido]".equals(nome)) return Optional.empty();
        return usuarioRepository.findByNomeUsuario(nome);
    }
}