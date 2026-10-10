package com.gti.usuarios.config;

import com.gti.usuarios.model.ChaveApiUsoDiario;
import com.gti.usuarios.repository.ChaveApiUsoDiarioRepository;
import com.gti.usuarios.repository.ChaveApiUsoRepository;
import com.gti.usuarios.security.ChaveApiRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class ChaveApiManutencaoJob {

    private static final Logger log = LoggerFactory.getLogger(ChaveApiManutencaoJob.class);

    @Value("${integracao.dias-retencao-uso:90}")
    private int diasRetencao;

    private final ChaveApiUsoRepository usoRepo;
    private final ChaveApiUsoDiarioRepository diarioRepo;
    private final ChaveApiRateLimiter rateLimiter;

    public ChaveApiManutencaoJob(ChaveApiUsoRepository usoRepo,
                                 ChaveApiUsoDiarioRepository diarioRepo,
                                 ChaveApiRateLimiter rateLimiter) {
        this.usoRepo = usoRepo;
        this.diarioRepo = diarioRepo;
        this.rateLimiter = rateLimiter;
    }

    @Scheduled(cron = "0 10 3 * * *")
    @Transactional
    public void executar() {
        consolidarOntem();
        expurgar();
        rateLimiter.limparAntigas();
    }

    private void consolidarOntem() {
        LocalDate ontem = LocalDate.now().minusDays(1);
        LocalDateTime inicio = ontem.atStartOfDay();
        LocalDateTime fim = ontem.plusDays(1).atStartOfDay();

        List<Object[]> linhas = usoRepo.consolidarPeriodo(inicio, fim);
        for (Object[] l : linhas) {
            Long chaveId = ((Number) l[0]).longValue();
            long total = ((Number) l[1]).longValue();
            long negadas = l[2] == null ? 0L : ((Number) l[2]).longValue();

            ChaveApiUsoDiario d = diarioRepo.findByChaveIdAndDia(chaveId, ontem)
                    .orElseGet(ChaveApiUsoDiario::new);
            d.setChaveId(chaveId);
            d.setDia(ontem);
            d.setTotalChamadas(total);
            d.setTotalNegadas(negadas);
            diarioRepo.save(d);
        }
        log.info("Consolidacao de uso da API concluida para {} - {} chaves", ontem, linhas.size());
    }

    private void expurgar() {
        if (diasRetencao <= 0) return;
        LocalDateTime limite = LocalDateTime.now().minusDays(diasRetencao);
        int removidos = usoRepo.apagarAnterioresA(limite);
        if (removidos > 0)
            log.info("Expurgo de registros de uso da API: {} linhas removidas", removidos);
    }
}