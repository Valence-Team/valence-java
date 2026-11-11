package br.com.valence.api.utils;

import java.util.List;
import java.util.regex.Pattern;

public final class DadosPessoaisUtils {

    private static final String SUBSTITUTO = "[removido]";

    private static final List<Pattern> PADROES = List.of(
            Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"),
            Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b"),
            Pattern.compile("\\b\\d{5}-\\d{3}\\b"),
            Pattern.compile("-?\\d{1,2}[.,]\\d{4,}\\s*[,;]\\s*-?\\d{1,3}[.,]\\d{4,}"),
            Pattern.compile("(\\+55\\s?)?\\(?\\d{2}\\)?\\s?9\\d{4}[-\\s]?\\d{4}\\b"
                    + "|\\(\\d{2}\\)\\s?\\d{4}[-\\s]?\\d{4}\\b"
                    + "|\\b9\\d{4}-\\d{4}\\b"));

    private DadosPessoaisUtils() {
    }

    public static String mascarar(String texto) {
        if (texto == null) {
            return null;
        }
        String resultado = texto;
        for (Pattern padrao : PADROES) {
            resultado = padrao.matcher(resultado).replaceAll(SUBSTITUTO);
        }
        return resultado;
    }

    public static boolean contemDadoPessoal(String texto) {
        if (texto == null) {
            return false;
        }
        return PADROES.stream().anyMatch(padrao -> padrao.matcher(texto).find());
    }

    public static String paraPrompt(String texto) {
        if (texto == null) {
            return "";
        }
        return mascarar(texto).replace('{', '(').replace('}', ')').strip();
    }

    public static String limitar(String texto, int tamanhoMaximo) {
        if (texto == null || texto.length() <= tamanhoMaximo) {
            return texto;
        }
        String cortado = texto.substring(0, tamanhoMaximo);
        int ultimoEspaco = cortado.lastIndexOf(' ');
        if (ultimoEspaco > tamanhoMaximo / 2) {
            cortado = cortado.substring(0, ultimoEspaco);
        }
        return cortado.strip();
    }
}
