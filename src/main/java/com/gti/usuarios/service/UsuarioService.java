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
import com.gti.usuarios.model.Chamado;

import java.util.List;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AtivoRepository ativoRepository;
    private final ChamadoRepository chamadoRepository;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                          AtivoRepository ativoRepository, ChamadoRepository chamadoRepository) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.ativoRepository = ativoRepository;
        this.chamadoRepository = chamadoRepository;
    }

    public Usuario login(String nomeUsuario, String senha) {
        log.debug("Tentativa de login: {}", nomeUsuario);
        if (nomeUsuario == null || senha == null)
            throw new RuntimeException("Usuário e senha são obrigatórios.");
        Usuario usuario = repository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> {
                    log.warn("Login falhou - usuario nao encontrado: {}", nomeUsuario);
                    return new RuntimeException("Usuário ou senha inválidos.");
                });
        if (usuario.getBloqueado()) {
            log.warn("Login bloqueado para usuario: {}", nomeUsuario);
            throw new RuntimeException("Conta bloqueada. Contate o administrador.");
        }
        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            log.warn("Login falhou - senha incorreta para: {}", nomeUsuario);
            throw new RuntimeException("Usuário ou senha inválidos.");
        }
        log.info("Login bem-sucedido: {} | tipo: {}", nomeUsuario, usuario.getTipoAcesso());
        return usuario;
    }

    public List<Usuario> listarTodos() {
        log.debug("Listando todos os usuarios");
        List<Usuario> usuarios = repository.findAll().stream()
                .filter(u -> !u.getNomeUsuario().startsWith("removido_"))
                .toList();
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
        log.debug("Trocando senha do usuario: {}", nomeUsuario);
        if (novaSenha == null || novaSenha.isBlank())
            throw new RuntimeException("A nova senha é obrigatória.");
        if (novaSenha.length() < 8)
            throw new RuntimeException("A senha deve ter no mínimo 8 caracteres.");
        if ("gti1234".equals(novaSenha))
            throw new RuntimeException("A nova senha deve ser diferente da senha padrão.");
        Usuario usuario = repository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setPrecisaTrocarSenha(false);
        Usuario salvo = repository.save(usuario);
        log.info("Senha trocada com sucesso. Usuario: {}", nomeUsuario);
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
        Usuario usuario = buscarPorId(id);

        long ativos = ativoRepository.countByResponsavelId(id);
        if (ativos > 0)
            throw new RuntimeException("Não é possível excluir: o usuário é responsável por " + ativos + " ativo(s). Reatribua esses ativos a outro usuário antes de excluir.");

        long chamadosAbertos = chamadoRepository.findBySolicitanteIdOrderByDataAberturaDesc(id).stream()
                .filter(c -> !"CONCLUIDO".equals(c.getStatus())).count();
        chamadosAbertos += chamadoRepository.findAll().stream()
                .filter(c -> c.getTecnico() != null && c.getTecnico().getId().equals(id))
                .filter(c -> !"CONCLUIDO".equals(c.getStatus())).count();
        if (chamadosAbertos > 0)
            throw new RuntimeException("Não é possível excluir: o usuário tem " + chamadosAbertos + " chamado(s) em aberto ou em andamento. Conclua esses chamados antes de excluir.");

        usuario.setNomeUsuario("removido_" + id);
        usuario.setEmail(null);
        usuario.setDescricao(null);
        usuario.setSenha(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        usuario.setBloqueado(true);
        repository.save(usuario);
        log.info("Usuario ID {} anonimizado (remoção lógica)", id);
    }
}