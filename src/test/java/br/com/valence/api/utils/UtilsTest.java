package br.com.valence.api.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UtilsTest {

    @Test
    void distanciaEntreSaoPauloERioDeJaneiro() {
        double distancia = GeoUtils.distanciaKm(-23.5505, -46.6333, -22.9068, -43.1729);
        assertThat(distancia).isBetween(355.0, 365.0);
    }

    @Test
    void mascaraTelefoneEmailCpfECoordenada() {
        String texto = DadosPessoaisUtils.mascarar(
                "Fale com joao@email.com ou (16) 99999-1234, CPF 123.456.789-09, local -21.20501, -47.85502");
        assertThat(texto)
                .doesNotContain("joao@email.com")
                .doesNotContain("99999-1234")
                .doesNotContain("123.456.789-09")
                .doesNotContain("-21.20501")
                .contains("[removido]");
    }

    @Test
    void naoConfundeSafraComTelefone() {
        assertThat(DadosPessoaisUtils.contemDadoPessoal("Safra 2025-2026 de milho, área de 1200 hectares")).isFalse();
    }

    @Test
    void preparaTextoParaOPromptSemChaves() {
        assertThat(DadosPessoaisUtils.paraPrompt("texto {x} ok")).isEqualTo("texto (x) ok");
    }
}
