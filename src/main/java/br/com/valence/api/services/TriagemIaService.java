package br.com.valence.api.services;

import br.com.valence.api.dtos.HipoteseDto;
import br.com.valence.api.dtos.ia.ClasseVisaoDto;
import br.com.valence.api.dtos.ia.EntradaTriagemDto;
import br.com.valence.api.dtos.ia.TrechoRagDto;
import br.com.valence.api.dtos.ia.TriagemIaDto;
import br.com.valence.api.utils.DadosPessoaisUtils;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
public class TriagemIaService {

    static final String INSTRUCAO_SISTEMA = """
            Você é o assistente de triagem fitossanitária do Valence, uma rede pública que apoia pequenos \
            produtores de milho, tomate e batata no Brasil. Você organiza uma triagem inicial de um dano \
            relatado na lavoura. Você não faz diagnóstico: um agrônomo sempre confirma ou descarta o caso.
            Regras:
            1. Responda em português do Brasil, com linguagem simples para o produtor rural.
            2. Liste de 1 a 3 hipóteses de pragas ou doenças compatíveis com a cultura informada, com \
            probabilidade entre 0 e 1. Quando houver resultado do modelo de visão, use-o como evidência principal.
            3. Severidade de 1 (leve) a 5 (muito grave), considerando o risco de espalhar para as lavouras vizinhas.
            4. Cuidados imediatos: até 4 ações práticas e seguras, como isolar a área, observar plantas vizinhas \
            e fotografar folhas. Nunca cite agrotóxico, defensivo, produto comercial, princípio ativo ou dose.
            5. textoAlerta: uma frase de até 200 caracteres para avisar produtores vizinhos, sem nome, endereço, \
            telefone ou qualquer dado pessoal.
            6. encaminharAgronomo: true quando houver dúvida, severidade 4 ou 5, ou pouca informação.
            7. Use somente as informações recebidas. Sem evidência suficiente, use probabilidades baixas.
            """;

    private static final Pattern TERMOS_PROIBIDOS = Pattern.compile(
            "agrot[oó]xic|defensiv|inseticid|fungicid|herbicid|acaricid|nematicid|pesticid|veneno|"
                    + "princ[ií]pio ativo|\\bdose|\\bml\\b|mililitr|litro|calda|pulveriz",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final ChatClient chatClient;
    private final Validator validator;

    // Método construtor
    public TriagemIaService(ChatClient.Builder chatClientBuilder, Validator validator) {
        this.chatClient = chatClientBuilder.build();
        this.validator = validator;
    }

    public Optional<TriagemIaDto> gerar(EntradaTriagemDto entrada) {
        try {
            TriagemIaDto resposta = chatClient.prompt()
                    .system(INSTRUCAO_SISTEMA)
                    .user(montarPrompt(entrada))
                    .call()
                    .entity(TriagemIaDto.class);
            Optional<TriagemIaDto> normalizada = normalizar(resposta, entrada);
            if (normalizada.isEmpty()) {
                log.warn("Triagem por IA descartada: a resposta do modelo não seguiu o formato esperado.");
            }
            return normalizada;
        } catch (Exception ex) {
            log.warn("Triagem por IA indisponível: {}.", ClassificadorFalhaIa.classificar(ex));
            return Optional.empty();
        }
    }

    String montarPrompt(EntradaTriagemDto entrada) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Cultura: ").append(entrada.cultura().name()).append('\n');
        prompt.append("Relato do produtor: ")
                .append(DadosPessoaisUtils.paraPrompt(DadosPessoaisUtils.limitar(entrada.descricao(), 1000)))
                .append("\n\n");
        prompt.append("Resultado do modelo de visão:\n");
        if (entrada.classes() == null || entrada.classes().isEmpty()) {
            prompt.append("Sem foto analisada.\n");
        } else {
            for (ClasseVisaoDto classe : entrada.classes()) {
                prompt.append("- ")
                        .append(DadosPessoaisUtils.paraPrompt(classe.nome() != null ? classe.nome() : classe.rotulo()))
                        .append(" (").append(DadosPessoaisUtils.paraPrompt(classe.rotulo())).append("): confiança ")
                        .append(String.format(Locale.ROOT, "%.2f", classe.confianca() == null ? 0.0 : classe.confianca()))
                        .append('\n');
            }
        }
        prompt.append("\nTrechos das fichas técnicas:\n");
        if (entrada.trechos() == null || entrada.trechos().isEmpty()) {
            prompt.append("Nenhum trecho disponível.\n");
        } else {
            int numero = 1;
            for (TrechoRagDto trecho : entrada.trechos()) {
                prompt.append(numero++).append(". ")
                        .append(DadosPessoaisUtils.paraPrompt(trecho.fonte())).append(" - ")
                        .append(DadosPessoaisUtils.paraPrompt(trecho.titulo())).append(": ")
                        .append(DadosPessoaisUtils.paraPrompt(DadosPessoaisUtils.limitar(trecho.texto(), 600)))
                        .append('\n');
            }
        }
        prompt.append("\nGere a triagem inicial no formato pedido.");
        return prompt.toString();
    }

    Optional<TriagemIaDto> normalizar(TriagemIaDto resposta, EntradaTriagemDto entrada) {
        if (resposta == null || resposta.hipoteses() == null) {
            return Optional.empty();
        }
        List<HipoteseDto> hipoteses = resposta.hipoteses().stream()
                .filter(hipotese -> hipotese != null && StringUtils.hasText(hipotese.praga()))
                .map(hipotese -> new HipoteseDto(DadosPessoaisUtils.limitar(hipotese.praga().strip(), 150),
                        hipotese.probabilidade()))
                .sorted(Comparator.comparing(HipoteseDto::probabilidade,
                        Comparator.nullsLast(Comparator.<Double>reverseOrder())))
                .limit(3)
                .toList();
        List<String> cuidados = resposta.cuidadosImediatos() == null ? List.of() : resposta.cuidadosImediatos().stream()
                .filter(StringUtils::hasText)
                .map(String::strip)
                .filter(cuidado -> !TERMOS_PROIBIDOS.matcher(cuidado).find())
                .map(DadosPessoaisUtils::mascarar)
                .limit(4)
                .toList();
        String textoAlerta = resposta.textoAlerta();
        if (!StringUtils.hasText(textoAlerta) || TERMOS_PROIBIDOS.matcher(textoAlerta).find()) {
            String praga = hipoteses.isEmpty() ? "praga" : hipoteses.get(0).praga();
            textoAlerta = "Possível caso de " + praga + " em lavoura de " + entrada.cultura().name().toLowerCase(Locale.ROOT)
                    + " na região. Observe suas plantas.";
        }
        textoAlerta = DadosPessoaisUtils.limitar(DadosPessoaisUtils.mascarar(textoAlerta.strip()), 200);
        TriagemIaDto normalizada = new TriagemIaDto(hipoteses, resposta.severidade(), cuidados, textoAlerta,
                resposta.encaminharAgronomo());
        if (!validator.validate(normalizada).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(normalizada);
    }
}
