package br.com.valence.api.dtos.ia;

import java.util.List;

public record RagConsultaDto(String cultura, String sintomas, List<String> classes) {
}
