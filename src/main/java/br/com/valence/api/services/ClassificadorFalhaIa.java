package br.com.valence.api.services;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

public final class ClassificadorFalhaIa {

    private ClassificadorFalhaIa() {
    }

    public static String classificar(Throwable falha) {
        for (Throwable atual = falha; atual != null; atual = atual.getCause()) {
            String mensagem = atual.getMessage() == null ? "" : atual.getMessage();
            if (atual instanceof SocketTimeoutException || atual instanceof HttpTimeoutException) {
                return "tempo esgotado";
            }
            if (atual instanceof ConnectException || atual instanceof ResourceAccessException) {
                return "falha de conexão com o provedor";
            }
            if (atual instanceof NonTransientAiException) {
                if (mensagem.contains("401") || mensagem.contains("403")) {
                    return "chave do provedor ausente ou inválida";
                }
                if (mensagem.contains("429")) {
                    return "limite de requisições ou cota do provedor esgotada";
                }
                return "requisição recusada pelo provedor";
            }
            if (atual instanceof TransientAiException) {
                return "provedor indisponível no momento";
            }
        }
        return "resposta inválida ou erro inesperado do modelo";
    }
}
