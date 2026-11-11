package br.com.valence.api.services;

import br.com.valence.api.exceptions.MidiaNaoSuportadaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Slf4j
@Service
public class ArmazenamentoFotoService {

    private final Path diretorioBase;

    public ArmazenamentoFotoService(@Value("${valence.fotos.diretorio}") String diretorio) {
        this.diretorioBase = Path.of(diretorio).toAbsolutePath().normalize();
    }

    public FotoDetectada detectarFormato(byte[] conteudo) {
        if (conteudo.length > 3 && (conteudo[0] & 0xFF) == 0xFF && (conteudo[1] & 0xFF) == 0xD8
                && (conteudo[2] & 0xFF) == 0xFF) {
            return new FotoDetectada("image/jpeg", "jpg");
        }
        if (conteudo.length > 8 && (conteudo[0] & 0xFF) == 0x89 && conteudo[1] == 'P' && conteudo[2] == 'N'
                && conteudo[3] == 'G') {
            return new FotoDetectada("image/png", "png");
        }
        if (conteudo.length > 12 && conteudo[0] == 'R' && conteudo[1] == 'I' && conteudo[2] == 'F'
                && conteudo[3] == 'F' && conteudo[8] == 'W' && conteudo[9] == 'E' && conteudo[10] == 'B'
                && conteudo[11] == 'P') {
            return new FotoDetectada("image/webp", "webp");
        }
        throw new MidiaNaoSuportadaException("Envie a foto em JPEG, PNG ou WebP.");
    }

    public String salvar(Long ocorrenciaId, byte[] conteudo, String extensao) {
        String caminhoRelativo = ocorrenciaId + "/" + UUID.randomUUID() + "." + extensao;
        Path destino = resolver(caminhoRelativo);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, conteudo);
            return caminhoRelativo;
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível salvar a foto.", ex);
        }
    }

    public byte[] ler(String caminhoRelativo) {
        try {
            return Files.readAllBytes(resolver(caminhoRelativo));
        } catch (IOException ex) {
            throw new UncheckedIOException("Não foi possível ler a foto.", ex);
        }
    }

    public void remover(String caminhoRelativo) {
        try {
            Files.deleteIfExists(resolver(caminhoRelativo));
        } catch (IOException ex) {
            log.warn("Não foi possível remover o arquivo de foto {}", caminhoRelativo);
        }
    }

    private Path resolver(String caminhoRelativo) {
        Path caminho = diretorioBase.resolve(caminhoRelativo).normalize();
        if (!caminho.startsWith(diretorioBase)) {
            throw new IllegalArgumentException("Caminho de foto inválido.");
        }
        return caminho;
    }

    public record FotoDetectada(String contentType, String extensao) {
    }
}
