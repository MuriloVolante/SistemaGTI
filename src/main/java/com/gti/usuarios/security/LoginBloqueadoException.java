package com.gti.usuarios.security;

public class LoginBloqueadoException extends RuntimeException {
    public LoginBloqueadoException(String mensagem) {
        super(mensagem);
    }
}