package br.com.valence.api.exceptions;

import lombok.Getter;

@Getter
public class ValidacaoException extends RuntimeException {

    private final String campo;

    public ValidacaoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }
}
