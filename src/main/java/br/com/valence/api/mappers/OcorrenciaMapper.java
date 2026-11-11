package br.com.valence.api.mappers;

import br.com.valence.api.dtos.FonteDto;
import br.com.valence.api.dtos.FotoResponseDto;
import br.com.valence.api.dtos.HipoteseDto;
import br.com.valence.api.dtos.LaudoResponseDto;
import br.com.valence.api.dtos.OcorrenciaResponseDto;
import br.com.valence.api.dtos.PropriedadeResumoDto;
import br.com.valence.api.dtos.TriagemResponseDto;
import br.com.valence.api.models.FotoOcorrenciaModel;
import br.com.valence.api.models.LaudoModel;
import br.com.valence.api.models.OcorrenciaModel;
import br.com.valence.api.models.TriagemModel;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OcorrenciaMapper {

    public static final TypeReference<List<HipoteseDto>> TIPO_HIPOTESES = new TypeReference<>() {
    };
    public static final TypeReference<List<String>> TIPO_TEXTOS = new TypeReference<>() {
    };
    public static final TypeReference<List<FonteDto>> TIPO_FONTES = new TypeReference<>() {
    };

    private final ConversorJson conversorJson;

    public OcorrenciaResponseDto paraResposta(OcorrenciaModel ocorrencia) {
        TriagemResponseDto triagem = paraTriagem(ocorrencia.getTriagem());
        List<FotoResponseDto> fotos = ocorrencia.getFotos().stream()
                .map(foto -> paraFoto(ocorrencia.getId(), foto))
                .toList();
        LaudoResponseDto laudo = ocorrencia.getLaudos().stream()
                .max(Comparator.comparing(LaudoModel::getEmitidoEm))
                .map(LaudoResponseDto::de)
                .orElse(null);
        return new OcorrenciaResponseDto(ocorrencia.getId(), ocorrencia.getCultura(), ocorrencia.getDescricao(),
                ocorrencia.getStatus(), ocorrencia.getSeveridade(), pragaProvavel(ocorrencia.getTriagem()),
                ocorrencia.getDataRegistro(), ocorrencia.getAtualizadoEm(),
                PropriedadeResumoDto.de(ocorrencia.getPropriedade()), fotos, triagem, laudo);
    }

    public TriagemResponseDto paraTriagem(TriagemModel triagem) {
        if (triagem == null) {
            return null;
        }
        return new TriagemResponseDto(
                conversorJson.paraLista(triagem.getHipoteses(), TIPO_HIPOTESES),
                triagem.getSeveridade(),
                conversorJson.paraLista(triagem.getCuidadosImediatos(), TIPO_TEXTOS),
                triagem.getTextoAlerta(),
                triagem.isEncaminharAgronomo(),
                triagem.getConfianca(),
                triagem.getGeradaEm(),
                triagem.getExplicacao(),
                conversorJson.paraLista(triagem.getFontes(), TIPO_FONTES),
                triagem.getMotivoImagem());
    }

    public String pragaProvavel(TriagemModel triagem) {
        if (triagem == null) {
            return null;
        }
        return conversorJson.paraLista(triagem.getHipoteses(), TIPO_HIPOTESES).stream()
                .findFirst()
                .map(HipoteseDto::praga)
                .orElse(null);
    }

    private FotoResponseDto paraFoto(Long ocorrenciaId, FotoOcorrenciaModel foto) {
        String url = "/api/ocorrencias/" + ocorrenciaId + "/fotos/" + foto.getId();
        return new FotoResponseDto(foto.getId(), url, foto.getContentType());
    }
}
