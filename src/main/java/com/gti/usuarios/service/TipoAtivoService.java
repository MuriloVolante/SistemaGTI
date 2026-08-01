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

    public TipoAtivo criarTipo(TipoAtivo tipo) {
        log.debug("Criando tipo de ativo: {}", tipo.getNome());
        if (tipo.getNome() == null || tipo.getNome().isBlank())
            throw new RuntimeException("O nome do tipo é obrigatório.");
        TipoAtivo salvo = tipoRepo.save(tipo);
        log.info("Tipo de ativo criado. ID: {} | nome: {}", salvo.getId(), salvo.getNome());
        return salvo;
    }

    public TipoAtivo atualizarTipo(Long id, TipoAtivo dados) {
        log.debug("Atualizando tipo de ativo ID {}", id);
        TipoAtivo tipo = buscarTipoPorId(id);
        if (dados.getNome() == null || dados.getNome().isBlank())
            throw new RuntimeException("O nome do tipo é obrigatório.");
        tipo.setNome(dados.getNome());
        tipo.setVidaUtilMeses(dados.getVidaUtilMeses());
        tipo.setPercentualDepreciacao(dados.getPercentualDepreciacao());
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

    public CampoDinamico adicionarCampo(Long tipoId, CampoDinamico campo) {
        log.debug("Adicionando campo '{}' ao tipo ID {}", campo.getNomeDoCampo(), tipoId);
        TipoAtivo tipo = buscarTipoPorId(tipoId);
        if (campo.getNomeDoCampo() == null || campo.getNomeDoCampo().isBlank())
            throw new RuntimeException("O nome do campo é obrigatório.");
        List<String> tiposValidos = List.of("VARCHAR", "INT", "DATE", "BOOLEAN");
        if (!tiposValidos.contains(campo.getTipoDado()))
            throw new RuntimeException("Tipo de dado inválido. Use: VARCHAR, INT, DATE ou BOOLEAN.");
        campo.setTipoAtivo(tipo);
        if (campo.getObrigatorio() == null) campo.setObrigatorio(false);
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