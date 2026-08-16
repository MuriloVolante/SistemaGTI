package com.gti.usuarios.config;

import com.gti.usuarios.model.Ativo;
import com.gti.usuarios.repository.AtivoRepository;
import com.gti.usuarios.service.CentroCustoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class CentroCustoMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CentroCustoMigrationRunner.class);

    private final AtivoRepository ativoRepo;
    private final CentroCustoService centroCustoService;

    public CentroCustoMigrationRunner(AtivoRepository ativoRepo, CentroCustoService centroCustoService) {
        this.ativoRepo = ativoRepo;
        this.centroCustoService = centroCustoService;
    }

    @Override
    public void run(String... args) {
        List<Ativo> ativos = ativoRepo.findAll();
        Set<String> nomesCatalogo = new HashSet<>();
        int ativosNormalizados = 0;

        for (Ativo a : ativos) {
            String bruto = a.getCentroCusto();
            if (bruto == null || bruto.isBlank()) continue;

            String normalizado = CentroCustoService.normalizar(bruto);
            if (!normalizado.equals(bruto)) {
                a.setCentroCusto(normalizado);
                ativoRepo.save(a);
                ativosNormalizados++;
            }
            nomesCatalogo.add(normalizado);
        }

        int criados = 0;
        for (String nome : nomesCatalogo) {
            if (!centroCustoService.existe(nome)) {
                centroCustoService.criar(nome);
                criados++;
            }
        }

        if (criados > 0 || ativosNormalizados > 0)
            log.info("Migração de centros de custo: {} ativo(s) normalizado(s), {} centro(s) de custo criado(s) no catálogo.",
                    ativosNormalizados, criados);
    }
}