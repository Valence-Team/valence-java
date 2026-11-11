package br.com.valence.api.clients;

import br.com.valence.api.dtos.ia.RagConsultaDto;
import br.com.valence.api.dtos.ia.RagResultadoDto;
import br.com.valence.api.dtos.ia.VisaoResultadoDto;
import br.com.valence.api.enums.Cultura;
import br.com.valence.api.exceptions.IaIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class IaServiceClient {

    private final RestClient restClient;

    // Método construtor
    public IaServiceClient(RestClient.Builder builder,
                           @Value("${valence.ia.url}") String url,
                           @Value("${valence.ia.timeout-segundos}") int timeoutSegundos,
                           @Value("${valence.ia.api-key:}") String apiKey) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Math.min(timeoutSegundos, 5) * 1000);
        fabrica.setReadTimeout(timeoutSegundos * 1000);
        RestClient.Builder configurado = builder.baseUrl(url).requestFactory(fabrica);
        if (StringUtils.hasText(apiKey)) {
            configurado.defaultHeader("X-API-Key", apiKey);
        }
        this.restClient = configurado.build();
    }

    public VisaoResultadoDto classificar(byte[] imagem, String nomeArquivo, String contentType, Cultura cultura) {
        MultipartBodyBuilder corpo = new MultipartBodyBuilder();
        corpo.part("imagem", new ByteArrayResource(imagem) {
            @Override
            public String getFilename() {
                return nomeArquivo;
            }
        }).contentType(MediaType.parseMediaType(contentType));
        corpo.part("cultura", cultura.name());
        try {
            VisaoResultadoDto resultado = restClient.post()
                    .uri("/api/v1/visao/classificar")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(corpo.build())
                    .retrieve()
                    .body(VisaoResultadoDto.class);
            if (resultado == null) {
                throw new IaIndisponivelException("O serviço de IA respondeu sem corpo.", null);
            }
            return resultado;
        } catch (RestClientException | IllegalArgumentException ex) {
            throw new IaIndisponivelException("Serviço de IA indisponível na classificação da imagem.", ex);
        }
    }

    public RagResultadoDto consultarRag(Cultura cultura, String sintomas, List<String> classes) {
        try {
            RagResultadoDto resultado = restClient.post()
                    .uri("/api/v1/rag/consultar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RagConsultaDto(cultura.name(), sintomas, classes))
                    .retrieve()
                    .body(RagResultadoDto.class);
            if (resultado == null) {
                throw new IaIndisponivelException("O serviço de IA respondeu sem corpo.", null);
            }
            return resultado;
        } catch (RestClientException | IllegalArgumentException ex) {
            throw new IaIndisponivelException("Serviço de IA indisponível na consulta às fichas técnicas.", ex);
        }
    }
}
