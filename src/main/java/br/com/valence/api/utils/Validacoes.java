package br.com.valence.api.utils;

public final class Validacoes {

    public static final String UF_REGEX =
            "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO";

    public static final String TELEFONE_REGEX = "^[0-9()+\\-\\s]*$";

    private Validacoes() {
    }
}
