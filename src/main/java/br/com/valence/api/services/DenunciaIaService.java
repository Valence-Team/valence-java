package br.com.valence.api.services;

import br.com.valence.api.dtos.ia.AnaliseDenunciaIaDto;
import br.com.valence.api.enums.AcaoModeracao;
import br.com.valence.api.enums.CategoriaDenuncia;
import br.com.valence.api.enums.GravidadeDenuncia;
import br.com.valence.api.utils.DadosPessoaisUtils;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class DenunciaIaService {

    static final String INSTRUCAO_SISTEMA = """
            Você apoia a moderação do mural do Valence, onde pequenos produtores rurais conversam sobre pragas \
            e doenças da lavoura na sua região. Analise o post denunciado e o motivo da denúncia.
            Categorias:
            - RECOMENDACAO_PERIGOSA: indica agrotóxico, dose ou prática que pode causar dano sem orientação técnica.
            - DESINFORMACAO: afirma algo falso ou sem base sobre pragas, doenças ou o manejo da lavoura.
            - OFENSA: ataca, humilha ou discrimina pessoas.
            - DADOS_PESSOAIS: expõe telefone, endereço, documento ou outro dado pessoal de alguém.
            - SPAM: propaganda ou conteúdo repetido sem relação com a lavoura.
            - SEM_PROBLEMA: nenhuma das anteriores.
            Regras:
            1. gravidade ALTA quando houver risco à saúde, à segurança ou exposição de dados pessoais.
            2. acao OCULTAR_E_REVISAR quando a gravidade for ALTA; nos demais casos, MANTER.
            3. Você nunca remove um post: a decisão final é sempre de um agrônomo ou gestor.
            4. justificativa curta, em português, sem repetir dados pessoais do texto.
            """;

    private final ChatClient chatClient;
    private final Validator validator;

    // Método construtor
    public DenunciaIaService(ChatClient.Builder chatClientBuilder, Validator validator) {
        this.chatClient = chatClientBuilder.build();
        this.validator = validator;
    }

    public Optional<AnaliseDenunciaIaDto> analisar(String textoPost, String motivo) {
        if (DadosPessoaisUtils.contemDadoPessoal(textoPost)) {
            return Optional.of(new AnaliseDenunciaIaDto(CategoriaDenuncia.DADOS_PESSOAIS, GravidadeDenuncia.ALTA,
                    AcaoModeracao.OCULTAR_E_REVISAR,
                    "O texto expõe dado pessoal, como telefone, e-mail ou documento."));
        }
        try {
            String prompt = "Post denunciado: " + DadosPessoaisUtils.paraPrompt(textoPost)
                    + "\nMotivo informado na denúncia: " + DadosPessoaisUtils.paraPrompt(motivo)
                    + "\n\nClassifique o post no formato pedido.";
            AnaliseDenunciaIaDto resposta = chatClient.prompt()
                    .system(INSTRUCAO_SISTEMA)
                    .user(prompt)
                    .call()
                    .entity(AnaliseDenunciaIaDto.class);
            return normalizar(resposta);
        } catch (Exception ex) {
            log.warn("Análise de denúncia por IA indisponível: {}.", ClassificadorFalhaIa.classificar(ex));
            return Optional.empty();
        }
    }

    Optional<AnaliseDenunciaIaDto> normalizar(AnaliseDenunciaIaDto resposta) {
        if (resposta == null || resposta.categoria() == null || resposta.gravidade() == null) {
            return Optional.empty();
        }
        GravidadeDenuncia gravidade = resposta.categoria() == CategoriaDenuncia.SEM_PROBLEMA
                ? GravidadeDenuncia.BAIXA
                : resposta.gravidade();
        AcaoModeracao acao = gravidade == GravidadeDenuncia.ALTA
                ? AcaoModeracao.OCULTAR_E_REVISAR
                : AcaoModeracao.MANTER;
        String justificativa = DadosPessoaisUtils.limitar(DadosPessoaisUtils.mascarar(resposta.justificativa()), 500);
        AnaliseDenunciaIaDto normalizada = new AnaliseDenunciaIaDto(resposta.categoria(), gravidade, acao,
                justificativa);
        if (!validator.validate(normalizada).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(normalizada);
    }
}
