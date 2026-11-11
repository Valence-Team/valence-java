package br.com.valence.api.services;

import br.com.valence.api.exceptions.RecursoNaoEncontradoException;
import br.com.valence.api.exceptions.RegraNegocioException;
import br.com.valence.api.models.FotoOcorrenciaModel;
import br.com.valence.api.models.OcorrenciaModel;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.FotoOcorrenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class FotoService {

    private static final int LIMITE_FOTOS = 5;

    private final OcorrenciaService ocorrenciaService;
    private final FotoOcorrenciaRepository fotoOcorrenciaRepository;
    private final ArmazenamentoFotoService armazenamentoFotoService;

    @Transactional
    public FotoOcorrenciaModel adicionar(Long ocorrenciaId, byte[] conteudo, UsuarioModel usuario) {
        OcorrenciaModel ocorrencia = ocorrenciaService.carregar(ocorrenciaId);
        ocorrenciaService.validarDono(ocorrencia, usuario);
        if (ocorrencia.getStatus().finalizada()) {
            throw new RegraNegocioException("A ocorrência já tem laudo final e não recebe novas fotos.");
        }
        if (fotoOcorrenciaRepository.countByOcorrenciaId(ocorrenciaId) >= LIMITE_FOTOS) {
            throw new RegraNegocioException("Cada ocorrência aceita até 5 fotos.");
        }
        ArmazenamentoFotoService.FotoDetectada formato = armazenamentoFotoService.detectarFormato(conteudo);
        String caminho = armazenamentoFotoService.salvar(ocorrenciaId, conteudo, formato.extensao());
        FotoOcorrenciaModel foto = new FotoOcorrenciaModel();
        foto.setCaminhoArquivo(caminho);
        foto.setContentType(formato.contentType());
        foto.setTamanhoBytes((long) conteudo.length);
        foto.setEnviadaEm(Instant.now());
        foto.setOcorrencia(ocorrencia);
        ocorrencia.getFotos().add(foto);
        return fotoOcorrenciaRepository.save(foto);
    }

    @Transactional(readOnly = true)
    public FotoConteudo buscar(Long ocorrenciaId, Long fotoId, UsuarioModel usuario) {
        OcorrenciaModel ocorrencia = ocorrenciaService.carregar(ocorrenciaId);
        ocorrenciaService.validarLeitura(ocorrencia, usuario);
        FotoOcorrenciaModel foto = fotoOcorrenciaRepository.findByIdAndOcorrenciaId(fotoId, ocorrenciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Foto não encontrada."));
        return new FotoConteudo(armazenamentoFotoService.ler(foto.getCaminhoArquivo()), foto.getContentType());
    }

    public record FotoConteudo(byte[] conteudo, String contentType) {
    }
}
