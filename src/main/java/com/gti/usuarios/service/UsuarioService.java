package com.gti.usuarios.service;

import com.gti.usuarios.model.Usuario;
import com.gti.usuarios.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.gti.usuarios.repository.AtivoRepository;
import com.gti.usuarios.repository.ChamadoRepository;
import com.gti.usuarios.repository.MensagemChamadoRepository;
import com.gti.usuarios.model.Ativo;
import com.gti.usuarios.model.Chamado;
import com.gti.usuarios.security.LoginBloqueadoException;
import com.gti.usuarios.security.LoginRateLimiter;

import java.util.UUID;

import java.util.List;
import java.util.Map;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AtivoRepository ativoRepository;
    private final ChamadoRepository chamadoRepository;
    private final MensagemChamadoRepository mensagemRepository;
    private final ChamadoService chamadoService;
    private final LoginRateLimiter loginRateLimiter;

    private static final int MAX_TAM_USUARIO = 50;
    private static final int MAX_TAM_SENHA = 128;
    private static final int MIN_TAM_NOVA_SENHA = 8;
    private static final int MAX_TAM_NOVA_SENHA = 64;
    private static final String SENHA_PADRAO = "gti1234";

    private final String hashDummy;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                          AtivoRepository ativoRepository, ChamadoRepository chamadoRepository,
                          MensagemChamadoRepository mensagemRepository,
                          ChamadoService chamadoService,
                          LoginRateLimiter loginRateLimiter) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.ativoRepository = ativoRepository;
        this.chamadoRepository = chamadoRepository;
        this.mensagemRepository = mensagemRepository;
        this.chamadoService = chamadoService;
        this.loginRateLimiter = loginRateLimiter;
        this.hashDummy = passwordEncoder.encode("::sem-usuario::" + UUID.randomUUID());
    }

    private static String sanitizarLog(String valor) {
        if (valor == null) return "null";
        String limpo = valor.replaceAll("[\\r\\n\\t]", "_");
        return limpo.length() > 50 ? limpo.substring(0, 50) + "..." : limpo;
    }

    public Usuario login(String nomeUsuario, String senha) {
        if (nomeUsuario == null || nomeUsuario.isBlank() || senha == null || senha.isEmpty())
            throw new RuntimeException("Usuário e senha são obrigatórios.");

        String usuarioNormalizado = nomeUsuario.trim();

        if (usuarioNormalizado.length() > MAX_TAM_USUARIO || senha.length() > MAX_TAM_SENHA)
            throw new RuntimeException("Usuário ou senha inválidos.");

        long espera = loginRateLimiter.segundosRestantes(usuarioNormalizado);
        if (espera > 0) {
            log.warn("Login barrado por excesso de tentativas: {}", sanitizarLog(usuarioNormalizado));
            throw new LoginBloqueadoException(
                    "Muitas tentativas de login. Tente novamente em " + ((espera / 60) + 1) + " minuto(s).");
        }

        log.debug("Tentativa de login: {}", sanitizarLog(usuarioNormalizado));

        Usuario usuario = repository.findByNomeUsuario(usuarioNormalizado).orElse(null);

        boolean senhaConfere = (usuario != null)
                ? passwordEncoder.matches(senha, usuario.getSenha())
                : passwordEncoder.matches(senha, hashDummy);

        if (usuario == null || !senhaConfere || Boolean.TRUE.equals(usuario.getBloqueado())) {
            loginRateLimiter.registrarFalha(usuarioNormalizado);
            log.warn("Login falhou para: {}", sanitizarLog(usuarioNormalizado));
            throw new RuntimeException("Usuário ou senha inválidos.");
        }

        loginRateLimiter.registrarSucesso(usuarioNormalizado);
        log.info("Login bem-sucedido: {} | tipo: {}",
                sanitizarLog(usuario.getNomeUsuario()), usuario.getTipoAcesso());
        return usuario;
    }

    public List<Usuario> listarTodos() {
        log.debug("Listando todos os usuarios");
        List<Usuario> usuarios = repository.findAll();
        log.debug("Total encontrado: {}", usuarios.size());
        return usuarios;
    }

    public List<Usuario> listarAtivos() {
        log.debug("Listando usuarios ativos");
        return repository.findByBloqueadoFalse();
    }

    public Usuario buscarPorId(Long id) {
        log.debug("Buscando usuario ID {}", id);
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Usuario nao encontrado. ID: {}", id);
                    return new RuntimeException("Usuário não encontrado. ID: " + id);
                });
    }

    public Usuario criar(Usuario usuario) {
        log.debug("Criando usuario: {}", usuario.getNomeUsuario());
        if (usuario.getNomeUsuario() == null || usuario.getNomeUsuario().isBlank())
            throw new RuntimeException("Nome de usuário é obrigatório.");
        if (usuario.getNomeCompleto() == null || usuario.getNomeCompleto().isBlank())
            throw new RuntimeException("Nome completo é obrigatório.");
        if (usuario.getTipoAcesso() == null || usuario.getTipoAcesso().isBlank())
            usuario.setTipoAcesso("COMUM");
        usuario.setSenha(passwordEncoder.encode("gti1234"));
        usuario.setPrecisaTrocarSenha(true);
        Usuario salvo = repository.save(usuario);
        log.info("Usuario criado. ID: {} | usuario: {} | tipo: {}", salvo.getId(), salvo.getNomeUsuario(), salvo.getTipoAcesso());
        return salvo;
    }

    public Usuario trocarSenha(String nomeUsuario, String novaSenha) {
        log.debug("Trocando senha do usuario: {}", sanitizarLog(nomeUsuario));

        if (novaSenha == null || novaSenha.isBlank())
            throw new RuntimeException("A nova senha é obrigatória.");
        if (novaSenha.length() < MIN_TAM_NOVA_SENHA)
            throw new RuntimeException("A senha deve ter no mínimo " + MIN_TAM_NOVA_SENHA + " caracteres.");
        if (novaSenha.length() > MAX_TAM_NOVA_SENHA)
            throw new RuntimeException("A senha deve ter no máximo " + MAX_TAM_NOVA_SENHA + " caracteres.");
        if (SENHA_PADRAO.equals(novaSenha))
            throw new RuntimeException("A nova senha deve ser diferente da senha padrão.");

        Usuario usuario = repository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (Boolean.TRUE.equals(usuario.getBloqueado()))
            throw new RuntimeException("Conta bloqueada. Contate o administrador.");

        if (passwordEncoder.matches(novaSenha, usuario.getSenha()))
            throw new RuntimeException("A nova senha deve ser diferente da senha atual.");

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setPrecisaTrocarSenha(false);
        Usuario salvo = repository.save(usuario);
        log.info("Senha trocada com sucesso. Usuario: {}", sanitizarLog(nomeUsuario));
        return salvo;
    }

    public Usuario atualizar(Long id, Usuario dados) {
        log.debug("Atualizando usuario ID {}", id);
        Usuario usuario = buscarPorId(id);
        if (dados.getNomeUsuario() == null || dados.getNomeUsuario().isBlank())
            throw new RuntimeException("Nome de usuário é obrigatório.");
        if (dados.getNomeCompleto() == null || dados.getNomeCompleto().isBlank())
            throw new RuntimeException("Nome completo é obrigatório.");
        usuario.setNomeUsuario(dados.getNomeUsuario());
        usuario.setNomeCompleto(dados.getNomeCompleto());
        usuario.setDescricao(dados.getDescricao());
        usuario.setEmail(dados.getEmail());
        if (dados.getTipoAcesso() != null && !dados.getTipoAcesso().isBlank())
            usuario.setTipoAcesso(dados.getTipoAcesso());
        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            if (dados.getSenha().length() < 8)
                throw new RuntimeException("A senha deve ter no mínimo 8 caracteres.");
            log.debug("Senha atualizada para usuario ID {}", id);
            usuario.setSenha(passwordEncoder.encode(dados.getSenha()));
            // provisória (true) força troca no próximo login; definitiva (false) vale direto.
            // default: provisória, salvo se o TI marcar como definitiva.
            usuario.setPrecisaTrocarSenha(
                    dados.getPrecisaTrocarSenha() != null ? dados.getPrecisaTrocarSenha() : true);
        }
        Usuario salvo = repository.save(usuario);
        log.info("Usuario atualizado. ID: {}", salvo.getId());
        return salvo;
    }

    public Usuario alterarBloqueio(Long id, Boolean bloqueado) {
        log.debug("Alterando bloqueio do usuario ID {} para {}", id, bloqueado);
        Usuario usuario = buscarPorId(id);
        usuario.setBloqueado(bloqueado);
        Usuario salvo = repository.save(usuario);
        log.info("Usuario ID {} bloqueado={}", salvo.getId(), salvo.getBloqueado());
        return salvo;
    }

    @Transactional
    public void excluir(Long id) {
        log.debug("Excluindo usuario ID {}", id);
        buscarPorId(id);

        chamadoRepository.findBySolicitanteIdOrderByDataAberturaDesc(id)
                .forEach(c -> chamadoService.excluirFisico(c.getId()));

        mensagemRepository.deleteByAutorId(id);
        mensagemRepository.flush();

        for (Chamado c : chamadoRepository.findByTecnicoId(id)) {
            c.setTecnico(null);
            if ("EM_ANDAMENTO".equals(c.getStatus()))
                c.setStatus("ABERTO");
            chamadoRepository.save(c);
        }

        for (Ativo a : ativoRepository.findByResponsavelId(id)) {
            a.setResponsavel(null);
            ativoRepository.save(a);
        }

        chamadoRepository.flush();
        ativoRepository.flush();

        repository.deleteById(id);
        log.info("Usuario ID {} excluído fisicamente", id);
    }

    public Map<String, Object> impactoExclusao(Long id) {
        buscarPorId(id);

        List<Chamado> comoSolicitante = chamadoRepository.findBySolicitanteIdOrderByDataAberturaDesc(id);

        long mensagens = comoSolicitante.stream()
                .mapToLong(c -> mensagemRepository.countByChamadoId(c.getId())).sum();

        long mensagensEmChamadosAlheios = mensagemRepository.countByAutorId(id)
                - comoSolicitante.stream()
                .mapToLong(c -> mensagemRepository.countByAutorIdAndChamadoId(id, c.getId())).sum();
        mensagens += mensagensEmChamadosAlheios;

        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("usuarios", 1L);
        m.put("chamados", (long) comoSolicitante.size());
        m.put("mensagens", mensagens);
        m.put("chamadosDesvinculados", chamadoRepository.countByTecnicoId(id));
        m.put("ativosDesvinculados", ativoRepository.countByResponsavelId(id));
        return m;
    }
}