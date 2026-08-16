package com.gti.usuarios.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AtivoValidator {

    private static final Pattern MATRICULA_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern MARCA_MODELO_PATTERN = Pattern.compile("^[\\p{L}0-9 ]+$");
    private static final Set<String> STATUS_VALIDOS = Set.of("ATIVO", "MANUTENCAO", "ESTOQUE", "DESCARTADO");

    public void validarMatricula(String matricula) {
        if (!MATRICULA_PATTERN.matcher(matricula).matches())
            throw new RuntimeException("Matrícula deve conter apenas números.");
    }

    public void validarMarcaModelo(String marcaModelo) {
        String v = marcaModelo.trim();
        if (v.length() < 3)
            throw new RuntimeException("Marca/Modelo deve ter no mínimo 3 caracteres.");
        if (!MARCA_MODELO_PATTERN.matcher(v).matches())
            throw new RuntimeException("Marca/Modelo não pode conter símbolos.");
    }

    public void validarStatus(String status) {
        if (!STATUS_VALIDOS.contains(status))
            throw new RuntimeException("Status inválido. Use: ATIVO, MANUTENCAO, ESTOQUE ou DESCARTADO.");
    }

    public void validarGarantia(LocalDate dataCompra, LocalDate garantiaAte) {
        if (garantiaAte != null && dataCompra != null && garantiaAte.isBefore(dataCompra))
            throw new RuntimeException("Data de garantia não pode ser anterior à data de compra.");
    }

    public void validarValorAquisicao(BigDecimal valor) {
        if (valor == null) return;
        if (valor.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Valor de aquisição deve ser maior que zero.");
        if (valor.stripTrailingZeros().scale() > 2)
            throw new RuntimeException("Valor de aquisição deve ter no máximo 2 casas decimais.");
    }
}