package br.com.valence.api.dtos;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaDto<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size,
        boolean first,
        boolean last) {

    public static <T> PaginaDto<T> de(Page<T> pagina) {
        return new PaginaDto<>(pagina.getContent(), pagina.getTotalElements(), pagina.getTotalPages(),
                pagina.getNumber(), pagina.getSize(), pagina.isFirst(), pagina.isLast());
    }
}
