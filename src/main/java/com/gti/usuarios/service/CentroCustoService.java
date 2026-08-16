package com.gti.usuarios.service;

import com.gti.usuarios.model.CentroCusto;
import com.gti.usuarios.repository.CentroCustoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CentroCustoService {

    private static final Logger log = LoggerFactory.getLogger(CentroCustoService.class);

    private final CentroCustoRepository repo;

    public CentroCustoService(CentroCustoRepository repo) {
        this.repo = repo;
    }

    public static String normalizar(String nome) {
        return nome == null ? null : nome.trim().toUpperCase();
    }

    public List<CentroCusto> listar() {
        return repo.findAllByOrderByNomeAsc();
    }

    public boolean existe(String nomeNormalizado) {
        return repo.existsByNome(nomeNormalizado);
    }

    public CentroCusto criar(String nome) {
        String normalizado = normalizar(nome);
        if (normalizado == null || normalizado.isBlank())
            throw new RuntimeException("Nome do centro de custo é obrigatório.");
        if (repo.existsByNome(normalizado))
            throw new RuntimeException("Centro de custo já cadastrado.");
        CentroCusto cc = new CentroCusto();
        cc.setNome(normalizado);
        CentroCusto salvo = repo.save(cc);
        log.info("Centro de custo criado. ID: {} | nome: {}", salvo.getId(), salvo.getNome());
        return salvo;
    }
}