package br.com.valence.api.enums;

public enum StatusOcorrencia {
    RECEBIDA,
    TRIADA,
    EM_ANALISE,
    CONFIRMADA,
    DESCARTADA;

    public boolean permiteEdicao() {
        return this == RECEBIDA || this == TRIADA;
    }

    public boolean permiteTriagem() {
        return this == RECEBIDA || this == TRIADA || this == EM_ANALISE;
    }

    public boolean finalizada() {
        return this == CONFIRMADA || this == DESCARTADA;
    }
}
