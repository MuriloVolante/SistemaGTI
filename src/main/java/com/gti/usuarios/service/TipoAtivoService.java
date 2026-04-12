package com.gti.usuarios.service;

import com.gti.usuarios.model.CampoDinamico;
import com.gti.usuarios.model.TipoAtivo;
import com.gti.usuarios.repository.CampoDinamicoRepository;
import com.gti.usuarios.repository.TipoAtivoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TipoAtivoService {

    private static final Logger log = LoggerFactory.getLogger(TipoAtivoService.class);

    private final TipoAtivoRepository tipoRepo;
    private final CampoDinamicoRepository campoRepo;

    public TipoAtivoService(TipoAtivoRepository tipoRepo, CampoDinamicoRepository campoRepo) {
        this.tipoRepo = tipoRepo;
        this.campoRepo = campoRepo;
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
        TipoAtivo salvo = tipoRepo.save(tipo);
        log.info("Tipo de ativo atualizado. ID: {} | nome: {}", salvo.getId(), salvo.getNome());
        return salvo;
    }

    public void excluirTipo(Long id) {
        log.debug("Excluindo tipo de ativo ID {}", id);
        buscarTipoPorId(id);
        tipoRepo.deleteById(id);
        log.info("Tipo de ativo ID {} excluido com sucesso", id);
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