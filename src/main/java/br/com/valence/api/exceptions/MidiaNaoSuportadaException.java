package br.com.valence.api.exceptions;

public class MidiaNaoSuportadaException extends RuntimeException {

    public MidiaNaoSuportadaException(String mensagem) {
        super(mensagem);
    }
}
