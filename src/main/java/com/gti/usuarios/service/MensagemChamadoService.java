package com.gti.usuarios.service;

import com.gti.usuarios.model.Chamado;
import com.gti.usuarios.model.MensagemChamado;
import com.gti.usuarios.model.Usuario;
import com.gti.usuarios.repository.ChamadoRepository;
import com.gti.usuarios.repository.MensagemChamadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MensagemChamadoService {

    private static final Logger log = LoggerFactory.getLogger(MensagemChamadoService.class);

    private final MensagemChamadoRepository mensagemRepo;
    private final ChamadoRepository chamadoRepo;

    public MensagemChamadoService(MensagemChamadoRepository mensagemRepo,
                                  ChamadoRepository chamadoRepo) {
        this.mensagemRepo = mensagemRepo;
        this.chamadoRepo  = chamadoRepo;
    }

    private static final int MAX_TAM_MENSAGEM = 2000;

    private static String mensagemValidada(String bruto) {
        if (bruto == null)
            throw new RuntimeException("Mensagem não pode ser vazia.");
        String limpa = bruto.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
        if (limpa.isEmpty())
            throw new RuntimeException("Mensagem não pode ser vazia.");
        if (limpa.length() > MAX_TAM_MENSAGEM)
            throw new RuntimeException("Mensagem deve ter no máximo " + MAX_TAM_MENSAGEM + " caracteres.");
        return limpa;
    }

    public List<MensagemChamado> listar(Long chamadoId) {
        if (!chamadoRepo.existsById(chamadoId))
            throw new RuntimeException("Chamado não encontrado. ID: " + chamadoId);
        return mensagemRepo.findByChamadoIdOrderByCriadoEmAsc(chamadoId);
    }

    @Transactional
    public MensagemChamado enviar(Long chamadoId, String texto, Usuario autor) {
        if (autor == null)
            throw new RuntimeException("Autor não identificado.");
        if (Boolean.TRUE.equals(autor.getBloqueado()))
            throw new RuntimeException("Conta bloqueada. Contate o administrador.");

        String mensagem = mensagemValidada(texto);

        Chamado c = chamadoRepo.findById(chamadoId)
                .orElseThrow(() -> new RuntimeException("Chamado não encontrado. ID: " + chamadoId));

        if ("CONCLUIDO".equals(c.getStatus()))
            throw new RuntimeException("Chamado concluído não aceita novas mensagens.");

        boolean ehSolicitante = c.getSolicitante() != null
                && c.getSolicitante().getId().equals(autor.getId());
        boolean ehTecnico = c.getTecnico() != null
                && c.getTecnico().getId().equals(autor.getId());
        if (!ehSolicitante && !ehTecnico)
            throw new RuntimeException("Apenas o solicitante ou o técnico que assumiu o chamados podem enviar mensagens.");

        MensagemChamado m = new MensagemChamado();
        m.setChamadoId(chamadoId);
        m.setAutor(autor);
        m.setMensagem(mensagem);
        m.setTipo("COMUM");

        MensagemChamado salva = mensagemRepo.save(m);
        log.info("Mensagem enviada. Chamado: {} | autor ID: {}", chamadoId, autor.getId());
        return salva;
    }

    @Transactional
    public MensagemChamado registrarTipada(Long chamadoId, String texto, Usuario autor, String tipo) {
        MensagemChamado m = new MensagemChamado();
        m.setChamadoId(chamadoId);
        m.setAutor(autor);
        m.setMensagem(mensagemValidada(texto));
        m.setTipo(tipo);
        return mensagemRepo.save(m);
    }
}