package com.gti.usuarios.service;

import com.gti.usuarios.model.Usuario;
import com.gti.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario login(String nomeUsuario, String senha) {
        log.debug("Tentativa de login: {}", nomeUsuario);
        if (nomeUsuario == null || senha == null)
            throw new RuntimeException("Usuário e senha são obrigatórios.");
        Usuario usuario = repository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> {
                    log.warn("Login falhou — usuario nao encontrado: {}", nomeUsuario);
                    return new RuntimeException("Usuário ou senha inválidos.");
                });
        if (usuario.getBloqueado()) {
            log.warn("Login bloqueado para usuario: {}", nomeUsuario);
            throw new RuntimeException("Conta bloqueada. Contate o administrador.");
        }
        if (!passwordEncoder.matches(senha, usuario.getSenha())) {
            log.warn("Login falhou — senha incorreta para: {}", nomeUsuario);
            throw new RuntimeException("Usuário ou senha inválidos.");
        }
        log.info("Login bem-sucedido: {} | tipo: {}", nomeUsuario, usuario.getTipoAcesso());
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
        if (usuario.getSenha() == null || usuario.getSenha().isBlank())
            throw new RuntimeException("Senha é obrigatória.");
        if (usuario.getSenha().length() < 6)
            throw new RuntimeException("A senha deve ter no mínimo 6 caracteres.");
        if (usuario.getTipoAcesso() == null || usuario.getTipoAcesso().isBlank())
            usuario.setTipoAcesso("COMUM");
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        Usuario salvo = repository.save(usuario);
        log.info("Usuario criado. ID: {} | usuario: {} | tipo: {}", salvo.getId(), salvo.getNomeUsuario(), salvo.getTipoAcesso());
        return salvo;
    }

    public Usuario trocarSenha(String nomeUsuario, String novaSenha) {
        log.debug("Trocando senha do usuario: {}", nomeUsuario);
        if (novaSenha == null || novaSenha.isBlank())
            throw new RuntimeException("A nova senha é obrigatória.");
        if (novaSenha.length() < 6)
            throw new RuntimeException("A senha deve ter no mínimo 6 caracteres.");
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
            if (dados.getSenha().length() < 6)
                throw new RuntimeException("A senha deve ter no mínimo 6 caracteres.");
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
}