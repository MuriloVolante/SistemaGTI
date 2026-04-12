package com.gti.usuarios.service;

import com.gti.usuarios.model.Usuario;
import com.gti.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado. ID: " + id));
    }

    public Usuario criar(Usuario usuario) {
        if (usuario.getNomeUsuario() == null || usuario.getNomeUsuario().isBlank())
            throw new RuntimeException("Nome de usuário é obrigatório.");
        if (usuario.getNomeCompleto() == null || usuario.getNomeCompleto().isBlank())
            throw new RuntimeException("Nome completo é obrigatório.");
        if (usuario.getSenha() == null || usuario.getSenha().isBlank())
            throw new RuntimeException("Senha é obrigatória.");
        return repository.save(usuario);
    }

    public Usuario atualizar(Long id, Usuario dados) {
        Usuario usuario = buscarPorId(id);

        if (dados.getNomeUsuario() == null || dados.getNomeUsuario().isBlank())
            throw new RuntimeException("Nome de usuário é obrigatório.");
        if (dados.getNomeCompleto() == null || dados.getNomeCompleto().isBlank())
            throw new RuntimeException("Nome completo é obrigatório.");

        usuario.setNomeUsuario(dados.getNomeUsuario());
        usuario.setNomeCompleto(dados.getNomeCompleto());
        usuario.setDescricao(dados.getDescricao());
        usuario.setEmail(dados.getEmail());

        // Só altera a senha se vier preenchida
        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            usuario.setSenha(dados.getSenha());
        }

        return repository.save(usuario);
    }

    public Usuario alterarBloqueio(Long id, Boolean bloqueado) {
        Usuario usuario = buscarPorId(id);
        usuario.setBloqueado(bloqueado);
        return repository.save(usuario);
    }

    public void excluir(Long id) {
        buscarPorId(id);
        repository.deleteById(id);
    }
}