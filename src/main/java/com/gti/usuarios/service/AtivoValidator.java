package com.gti.usuarios.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AtivoValidator {

    private static final Pattern MATRICULA_PATTERN = Pattern.compile("^[0-9]+$");
    private static final Pattern MARCA_MODELO_PATTERN = Pattern.compile("^[\\p{L}0-9 .,/()+-]+$");
    private static final Set<String> STATUS_VALIDOS = Set.of("ATIVO", "MANUTENCAO", "ESTOQUE", "DESCARTADO");

    public void validarMatricula(String matricula) {
        if (!MATRICULA_PATTERN.matcher(matricula).matches())
            throw new RuntimeException("Patrimônio deve conter apenas números.");
        if (matricula.length() > 50)
            throw new RuntimeException("Patrimônio deve ter no máximo 50 dígitos.");
    }

    public void validarMarcaModelo(String marcaModelo) {
        String v = marcaModelo.trim();
        if (v.length() < 3)
            throw new RuntimeException("Marca/Modelo deve ter no mínimo 3 caracteres.");
        if (v.length() > 150)
            throw new RuntimeException("Marca/Modelo deve ter no máximo 150 caracteres.");
        if (!MARCA_MODELO_PATTERN.matcher(v).matches())
            throw new RuntimeException("Marca/Modelo aceita apenas letras, números, espaço e os símbolos . , / ( ) + -");
    }

    public void validarDataCompra(LocalDate dataCompra) {
        if (dataCompra == null) return;
        if (dataCompra.isAfter(LocalDate.now()))
            throw new RuntimeException("Data de compra não pode ser futura.");
        if (dataCompra.getYear() < 1990)
            throw new RuntimeException("Data de compra inválida.");
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
        if (valor.compareTo(new BigDecimal("9999999999.99")) > 0)
            throw new RuntimeException("Valor de aquisição excede o limite permitido.");
    }

    public void validarCampoDinamico(String nomeCampo, String tipoDado, String valor) {
        switch (tipoDado) {
            case "INT" -> {
                try {
                    Long.parseLong(valor);
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Campo '" + nomeCampo + "' deve ser um número inteiro.");
                }
            }
            case "DATE" -> {
                try {
                    LocalDate.parse(valor);
                } catch (DateTimeParseException e) {
                    throw new RuntimeException("Campo '" + nomeCampo + "' deve ser uma data válida.");
                }
            }
            case "BOOLEAN" -> {
                if (!valor.equals("true") && !valor.equals("false"))
                    throw new RuntimeException("Campo '" + nomeCampo + "' deve ser verdadeiro ou falso.");
            }
            default -> { }
        }
    }
}