package com.gti.usuarios.controller;

import com.gti.usuarios.dto.IntegracaoUsuarioDTO;
import com.gti.usuarios.model.Ativo;
import com.gti.usuarios.model.ValorCampo;
import com.gti.usuarios.service.AtivoService;
import com.gti.usuarios.service.ChamadoService;
import com.gti.usuarios.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/integracao")
public class IntegracaoController {

    private final AtivoService ativoService;
    private final UsuarioService usuarioService;
    private final ChamadoService chamadoService;
    private final DashboardController dashboardController;

    public IntegracaoController(AtivoService ativoService,
                                UsuarioService usuarioService,
                                ChamadoService chamadoService,
                                DashboardController dashboardController) {
        this.ativoService = ativoService;
        this.usuarioService = usuarioService;
        this.chamadoService = chamadoService;
        this.dashboardController = dashboardController;
    }

    // ── Endpoints públicos (autenticados via header X-API-Key) ────────────
    @GetMapping("/ativos")
    public ResponseEntity<List<Map<String, Object>>> ativos() {
        return ResponseEntity.ok(montarAtivos());
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<IntegracaoUsuarioDTO>> usuarios() {
        return ResponseEntity.ok(montarUsuarios());
    }

    @GetMapping("/chamados")
    public ResponseEntity<?> chamados() {
        return ResponseEntity.ok(chamadoService.listarTodos());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return dashboardController.dashboard(null, null, null);
    }

    // ── Endpoints de prévia (autenticados via JWT, somente TI) ─────────────
    // Usados pela tela de Configurações > Integração; mesmo formato dos
    // endpoints públicos, sem precisar expor a API key real no navegador.
    @GetMapping("/preview/ativos")
    public ResponseEntity<List<Map<String, Object>>> previewAtivos() {
        return ResponseEntity.ok(montarAtivos());
    }

    @GetMapping("/preview/usuarios")
    public ResponseEntity<List<IntegracaoUsuarioDTO>> previewUsuarios() {
        return ResponseEntity.ok(montarUsuarios());
    }

    @GetMapping("/preview/chamados")
    public ResponseEntity<?> previewChamados() {
        return ResponseEntity.ok(chamadoService.listarTodos());
    }

    @GetMapping("/preview/dashboard")
    public ResponseEntity<Map<String, Object>> previewDashboard() {
        return dashboardController.dashboard(null, null, null);
    }

    // ── Helpers ─────────────────────────────────────────────────────────
    private List<Map<String, Object>> montarAtivos() {
        List<Ativo> ativos = ativoService.listarTodos();
        return ativos.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("tipo", a.getTipo() != null ? a.getTipo().getNome() : null);
            m.put("patrimonio", a.getPatrimonio());
            m.put("marcaModelo", a.getMarcaModelo());
            m.put("responsavel", a.getResponsavel() != null ? a.getResponsavel().getNomeCompleto() : null);
            m.put("centroCusto", a.getCentroCusto());
            m.put("status", a.getStatus());
            m.put("dataCompra", a.getDataCompra());
            m.put("garantiaAte", a.getGarantiaAte());
            m.put("valorAquisicao", a.getValorAquisicao());
            m.put("vidaUtilMeses", a.getTipo() != null ? a.getTipo().getVidaUtilMeses() : null);
            m.put("percentualDepreciacao", a.getTipo() != null ? a.getTipo().getPercentualDepreciacao() : null);

            List<ValorCampo> valores = ativoService.buscarValores(a.getId());
            Map<String, String> camposDinamicos = valores.stream()
                    .collect(Collectors.toMap(
                            v -> v.getCampo().getNomeDoCampo(),
                            ValorCampo::getValor,
                            (a1, a2) -> a1,
                            LinkedHashMap::new));
            m.put("camposDinamicos", camposDinamicos);
            return m;
        }).collect(Collectors.toList());
    }

    private List<IntegracaoUsuarioDTO> montarUsuarios() {
        return usuarioService.listarTodos().stream()
                .map(IntegracaoUsuarioDTO::from)
                .collect(Collectors.toList());
    }
}