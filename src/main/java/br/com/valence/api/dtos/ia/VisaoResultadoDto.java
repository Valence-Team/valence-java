package br.com.valence.api.dtos.ia;

import java.util.List;

public record VisaoResultadoDto(
        boolean imagemValida,
        String motivo,
        String culturaDetectada,
        List<ClasseVisaoDto> classes) {

    public List<ClasseVisaoDto> classesOuVazio() {
        return classes == null ? List.of() : classes;
    }
}
