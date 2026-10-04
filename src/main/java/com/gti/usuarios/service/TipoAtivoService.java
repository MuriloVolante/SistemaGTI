package com.gti.usuarios.service;

import com.gti.usuarios.model.Ativo;
import com.gti.usuarios.model.CampoDinamico;
import com.gti.usuarios.model.TipoAtivo;
import com.gti.usuarios.repository.AtivoRepository;
import com.gti.usuarios.repository.CampoDinamicoRepository;
import com.gti.usuarios.repository.TipoAtivoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gti.usuarios.dto.TipoAtivoRequestDTO;
import com.gti.usuarios.dto.CampoDinamicoRequestDTO;

import java.util.List;
import java.util.Map;

@Service
public class TipoAtivoService {

    private static final Logger log = LoggerFactory.getLogger(TipoAtivoService.class);

    private final TipoAtivoRepository tipoRepo;
    private final CampoDinamicoRepository campoRepo;
    private final AtivoRepository ativoRepo;
    private final AtivoService ativoService;

    public TipoAtivoService(TipoAtivoRepository tipoRepo,
                            CampoDinamicoRepository campoRepo,
                            AtivoRepository ativoRepo,
                            AtivoService ativoService) {
        this.tipoRepo = tipoRepo;
        this.campoRepo = campoRepo;
        this.ativoRepo = ativoRepo;
        this.ativoService = ativoService;
    }

    public List<TipoAtivo> listarTipos() {
        log.debug("Listando todos os tipos de ativo");
        List<TipoAtivo> tipos = tipoRepo.findAll();
        log.debug("Total de tipos encontrados: {}", tipos.size());
        return tipos;
    }

    public TipoAtivo buscarTipoPorId(Long id) {
        log.debug("Buscando tipo de ativo ID {}", id);
        return tipoRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Tipo de ativo nao encontrado. ID: {}", id);
                    return new RuntimeException("Tipo de ativo não encontrado. ID: " + id);
                });
    }

    private static final int MAX_TAM_NOME_TIPO = 100;
    private static final int MAX_VIDA_UTIL_MESES = 600;
    private static final java.math.BigDecimal MAX_PERCENTUAL = new java.math.BigDecimal("100.00");

    private String nomeValidado(String bruto, Long idAtual) {
        if (bruto == null || bruto.isBlank())
            throw new RuntimeException("O nome do tipo é obrigatório.");
        String nome = bruto.replaceAll("\\p{Cntrl}", " ").trim().replaceAll("[ \\t]{2,}", " ");
        if (nome.length() < 2)
            throw new RuntimeException("O nome do tipo deve ter no mínimo 2 caracteres.");
        if (nome.length() > MAX_TAM_NOME_TIPO)
            throw new RuntimeException("O nome do tipo deve ter no máximo " + MAX_TAM_NOME_TIPO + " caracteres.");
        tipoRepo.findByNomeIgnoreCase(nome).ifPresent(existente -> {
            if (idAtual == null || !existente.getId().equals(idAtual))
                throw new RuntimeException("Já existe um tipo de ativo com esse nome.");
        });
        return nome;
    }

    private Integer vidaUtilValidada(String bruto) {
        if (bruto == null || bruto.isBlank()) return null;
        int meses;
        try {
            meses = Integer.parseInt(bruto.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Vida útil deve ser um número inteiro de meses.");
        }
        if (meses <= 0)
            throw new RuntimeException("Vida útil deve ser maior que zero.");
        if (meses > MAX_VIDA_UTIL_MESES)
            throw new RuntimeException("Vida útil deve ser no máximo " + MAX_VIDA_UTIL_MESES + " meses.");
        return meses;
    }

    private java.math.BigDecimal percentualValidado(String bruto) {
        if (bruto == null || bruto.isBlank()) return null;
        java.math.BigDecimal p;
        try {
            p = new java.math.BigDecimal(bruto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Percentual de depreciação inválido.");
        }
        if (p.compareTo(java.math.BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Percentual de depreciação deve ser maior que zero.");
        if (p.compareTo(MAX_PERCENTUAL) > 0)
            throw new RuntimeException("Percentual de depreciação não pode ser maior que 100.");
        if (p.stripTrailingZeros().scale() > 2)
            throw new RuntimeException("Percentual de depreciação deve ter no máximo 2 casas decimais.");
        return p;
    }

    public TipoAtivo criarTipo(TipoAtivoRequestDTO dados) {
        TipoAtivo tipo = new TipoAtivo();
        tipo.setNome(nomeValidado(dados.nome(), null));
        tipo.setVidaUtilMeses(vidaUtilValidada(dados.vidaUtilMeses()));
        tipo.setPercentualDepreciacao(percentualValidado(dados.percentualDepreciacao()));

        TipoAtivo salvo = tipoRepo.save(tipo);
        log.info("Tipo de ativo criado. ID: {} | nome: {}", salvo.getId(), salvo.getNome());
        return salvo;
    }

    public TipoAtivo atualizarTipo(Long id, TipoAtivoRequestDTO dados) {
        log.debug("Atualizando tipo de ativo ID {}", id);
        TipoAtivo tipo = buscarTipoPorId(id);
        tipo.setNome(nomeValidado(dados.nome(), id));
        tipo.setVidaUtilMeses(vidaUtilValidada(dados.vidaUtilMeses()));
        tipo.setPercentualDepreciacao(percentualValidado(dados.percentualDepreciacao()));

        TipoAtivo salvo = tipoRepo.save(tipo);
        log.info("Tipo de ativo atualizado. ID: {} | nome: {}", salvo.getId(), salvo.getNome());
        return salvo;
    }

    @Transactional
    public void excluirTipo(Long id) {
        log.debug("Excluindo tipo de ativo ID {}", id);
        buscarTipoPorId(id);

        ativoRepo.findByTipoId(id)
                .forEach(a -> ativoService.excluirFisico(a.getId()));

        tipoRepo.deleteById(id);
        log.info("Tipo de ativo ID {} excluído fisicamente", id);
    }

    public Map<String, Object> impactoExclusao(Long id) {
        buscarTipoPorId(id);

        List<Ativo> ativos = ativoRepo.findByTipoId(id);

        Map<String, Object> total = new java.util.LinkedHashMap<>();
        total.put("tipos", 1L);
        total.put("campos", (long) campoRepo.findByTipoAtivoId(id).size());
        total.put("ativos", (long) ativos.size());
        total.put("chamados", 0L);
        total.put("mensagens", 0L);
        total.put("camposPreenchidos", 0L);
        total.put("anexos", 0L);
        total.put("historico", 0L);
        total.put("manutencoes", 0L);

        for (Ativo a : ativos) {
            Map<String, Object> i = ativoService.impactoExclusao(a.getId());
            for (String k : List.of("chamados", "mensagens", "camposPreenchidos",
                    "anexos", "historico", "manutencoes")) {
                total.put(k, (Long) total.get(k) + (Long) i.get(k));
            }
        }
        return total;
    }

    public List<CampoDinamico> listarCampos(Long tipoId) {
        log.debug("Listando campos do tipo ID {}", tipoId);
        buscarTipoPorId(tipoId);
        List<CampoDinamico> campos = campoRepo.findByTipoAtivoId(tipoId);
        log.debug("Total de campos encontrados: {}", campos.size());
        return campos;
    }

    private static final int MAX_TAM_NOME_CAMPO = 100;
    private static final java.util.Set<String> TIPOS_DADO =
            java.util.Set.of("VARCHAR", "INT", "DATE", "BOOLEAN");
    private static final java.util.regex.Pattern RE_NOME_CAMPO =
            java.util.regex.Pattern.compile("^[\\p{L}0-9][\\p{L}0-9 ._/()-]*$");

    private String nomeCampoValidado(Long tipoId, String bruto) {
        if (bruto == null || bruto.isBlank())
            throw new RuntimeException("O nome do campo é obrigatório.");
        String nome = bruto.replaceAll("\\p{Cntrl}", " ").trim().replaceAll("[ \\t]{2,}", " ");
        if (nome.length() < 2)
            throw new RuntimeException("O nome do campo deve ter no mínimo 2 caracteres.");
        if (nome.length() > MAX_TAM_NOME_CAMPO)
            throw new RuntimeException("O nome do campo deve ter no máximo " + MAX_TAM_NOME_CAMPO + " caracteres.");
        if (!RE_NOME_CAMPO.matcher(nome).matches())
            throw new RuntimeException("O nome do campo aceita apenas letras, números, espaço e os símbolos . _ / ( ) -");
        if (campoRepo.existsByTipoAtivoIdAndNomeDoCampoIgnoreCase(tipoId, nome))
            throw new RuntimeException("Já existe um campo com esse nome neste tipo de ativo.");
        return nome;
    }

    private String tipoDadoValidado(String bruto) {
        if (bruto == null || bruto.isBlank())
            throw new RuntimeException("O tipo de dado é obrigatório.");
        String tipo = bruto.trim().toUpperCase();
        if (!TIPOS_DADO.contains(tipo))
            throw new RuntimeException("Tipo de dado inválido. Use: VARCHAR, INT, DATE ou BOOLEAN.");
        return tipo;
    }

    public CampoDinamico adicionarCampo(Long tipoId, CampoDinamicoRequestDTO dados) {
        log.debug("Adicionando campo ao tipo ID {}", tipoId);
        TipoAtivo tipo = buscarTipoPorId(tipoId);

        CampoDinamico campo = new CampoDinamico();
        campo.setTipoAtivo(tipo);
        campo.setNomeDoCampo(nomeCampoValidado(tipoId, dados.nomeDoCampo()));
        campo.setTipoDado(tipoDadoValidado(dados.tipoDado()));
        campo.setObrigatorio(Boolean.TRUE.equals(dados.obrigatorio()));

        CampoDinamico salvo = campoRepo.save(campo);
        log.info("Campo adicionado. ID: {} | nome: {} | tipo: {}", salvo.getId(), salvo.getNomeDoCampo(), salvo.getTipoDado());
        return salvo;
    }

    public void excluirCampo(Long campoId) {
        log.debug("Excluindo campo ID {}", campoId);
        campoRepo.findById(campoId)
                .orElseThrow(() -> {
                    log.warn("Campo nao encontrado. ID: {}", campoId);
                    return new RuntimeException("Campo não encontrado. ID: " + campoId);
                });
        campoRepo.deleteById(campoId);
        log.info("Campo ID {} excluido com sucesso", campoId);
    }
}