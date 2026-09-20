package com.gti.usuarios.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RedirectController {

    @GetMapping({
            "/",
            "/login",
            "/home",
            "/consulta",
            "/usuarios", "/usuarios/**",
            "/ativos", "/ativos/**",
            "/chamados", "/chamados/**",
            "/meus-chamados", "/meus-chamados/**",
            "/configuracoes", "/configuracoes/**"
    })
    public String spa() {
        return "forward:/index.html";
    }
}