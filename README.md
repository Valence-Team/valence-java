# Valence API

API núcleo do **Valence**, uma rede pública e colaborativa de vigilância fitossanitária para pequenos produtores de milho, tomate e batata. Global Solution FIAP 2026/2, tema CitizenTech, disciplina Java Advanced.

| Integrante | RM |
| --- | --- |
| Henrique Martins | 563620 |
| Guilherme Macedo | 562396 |
| Pedro Henrique | 563405 |

- **Deploy (Azure App Service):** `https://app-valence-api-rm563620.azurewebsites.net/swagger-ui.html` *(confirmar após o primeiro deploy)*
- **Vídeo pitch:** *(link do YouTube)*
- **Arquitetura distribuída escolhida (requisito 5):** **opção A, RabbitMQ**, com evento publicado e consumido de forma assíncrona pela própria API.

## O problema e o cidadão beneficiado

Comunidades rurais isoladas descobrem pragas tarde, sem técnico por perto e sem canal rápido entre vizinhos e poder público. Segundo a FAO, cerca de 40% da produção agrícola global se perde para pragas; a Embrapa estima que a cigarrinha-do-milho causa US$ 6,5 bilhões de prejuízo por ano no Brasil.

Com o Valence, o **agricultor familiar** registra um dano na lavoura pelo app (foto e descrição). A IA faz uma triagem inicial, as propriedades vizinhas recebem um alerta de suspeita e um **agrônomo** confirma ou descarta o caso. Casos confirmados viram aviso no mural da região e dados abertos e anonimizados para o **poder público** (Observatório .NET).

ODS atendidos: 2 (fome zero e agricultura sustentável), 9 (inovação e infraestrutura), 10 (redução das desigualdades) e 13 (ação contra a mudança do clima).

## Requisitos da disciplina

| Requisito | Como foi atendido |
| --- | --- |
| 01 API REST do domínio | 11 entidades JPA relacionadas, CRUD completo de propriedade, ocorrência e publicação do mural, paginação em todas as listagens |
| 02 Validação e erros | Bean Validation em todos os DTOs de entrada e `@RestControllerAdvice` (`GlobalExceptionHandler`) com o formato de erro único |
| 03 Segurança | Spring Security stateless com JWT (HS256), senhas com BCrypt, papéis `PRODUTOR`, `AGRONOMO` e `GESTOR` e `@PreAuthorize` em cada endpoint |
| 04 Spring AI | Triagem da ocorrência e análise de denúncia com `ChatClient` e saída estruturada convertida em objeto Java |
| 05 Arquitetura distribuída | Opção A: publica `ocorrencia.triada` e `laudo.emitido` na exchange `valence.eventos` e consome `ocorrencia.triada` na fila `valence.java.alertas` para gerar alertas |
| 06 Documentação e integração | Swagger/OpenAPI, este README, coleção do Postman e contrato consumido pelo app Mobile |

## Arquitetura

![Arquitetura do Valence](docs/arquitetura.png)

1. **Usuários:** produtor, agrônomo e gestor público.
2. **App Mobile** (React Native): única interface; fala só com esta API, por REST e JWT.
3. **API Java** (este repositório): núcleo do domínio, segurança, triagem, alertas, laudos, mural e moderação.
4. **Azure SQL:** banco do perfil `prod`. Em desenvolvimento, H2 em arquivo.
5. **Serviço de IA** (Python): classifica a foto da folha e busca trechos de fichas técnicas (RAG).
6. **LLM** (Groq, compatível com a API da OpenAI): usado pelo Spring AI na triagem e na análise de denúncias.
7. **RabbitMQ** (CloudAMQP): exchange `valence.eventos` (topic) com a fila da própria API e a fila do Observatório.
8. **Observatório .NET:** consome os eventos e publica dados abertos anonimizados.
9. **Azure DevOps:** Boards, Repos e Pipelines (CI com testes JUnit e CD para o App Service).

Fonte do diagrama: [`docs/arquitetura.mmd`](docs/arquitetura.mmd).

## Tecnologias e versões

| Item | Versão |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.5.16 (Web, Data JPA, Validation, Security, AMQP, Actuator) |
| Spring AI | 1.1.8 (`spring-ai-starter-model-openai` apontando para a Groq) |
| JWT | `com.auth0:java-jwt` 4.5.0 |
| Documentação | springdoc-openapi 2.8.17 |
| Banco | H2 (dev e testes) e Azure SQL com `mssql-jdbc` (prod) |
| Mensageria | RabbitMQ 4 (Docker em dev, CloudAMQP em prod) |
| Build | Maven 3.9.11 pelo Maven Wrapper |
| Testes | JUnit 5, Spring Boot Test, MockMvc, Mockito |
| Outras | Lombok |

## Estrutura do projeto

```
src/main/java/br/com/valence/api
  clients/       cliente HTTP do serviço de IA (tempo limite e chave X-API-Key)
  configs/       segurança, RabbitMQ, OpenAPI e usuários de teste
  consumers/     consumidor RabbitMQ de ocorrencia.triada
  controllers/   endpoints REST
  dtos/          entrada (Bean Validation), saída, eventos e estruturas da IA
  enums/         papéis, culturas e status
  exceptions/    exceções de negócio e @RestControllerAdvice
  mappers/       conversão de entidade para resposta
  models/        entidades JPA (tabelas TB_*)
  producers/     publicação de eventos depois do commit
  repositories/  Spring Data JPA
  schedulers/    triagem das ocorrências que ficaram sem foto
  security/      JWT, filtro e respostas 401/403
  services/      regras de negócio e Spring AI
  utils/         distância geográfica e mascaramento de dados pessoais
scripts/         script-bd.sql e scripts Azure CLI
dockerfiles/     Dockerfile da API e docker-compose do RabbitMQ
postman/         coleção do Postman
azure-pipeline.yml
```

## Como rodar

### Pré-requisitos

- JDK 21 (`java -version`).
- Docker Desktop, para o RabbitMQ local. Sem ele a API sobe do mesmo jeito: os eventos não são publicados e o restante funciona.
- Não é preciso instalar Maven: o projeto usa o Maven Wrapper.

### Passo a passo (perfil `dev`)

1. Suba o RabbitMQ local:
   ```bash
   docker compose -f dockerfiles/docker-compose.yml up -d
   ```
   Painel: http://localhost:15672 (usuário e senha `guest`).
2. Crie o arquivo `.env` a partir do exemplo e preencha a chave da Groq (`LLM_API_KEY`). Sem chave a API sobe e a triagem cai no fallback (vai direto para o agrônomo).
   ```bash
   cp .env.example .env
   ```
3. Rode os testes e suba a API:
   ```bash
   ./mvnw test
   ./mvnw spring-boot:run
   ```
   No Windows (PowerShell ou CMD): `.\mvnw.cmd test` e `.\mvnw.cmd spring-boot:run`.
4. Acesse o Swagger em http://localhost:8080/swagger-ui.html, faça login em `POST /api/auth/login` com um usuário de teste, copie o `token` e clique em **Authorize**.

O perfil `dev` grava o H2 em `./data/valence-db` e as fotos em `./data/fotos`. Para começar do zero, apague a pasta `data`.

Para subir a API também em Docker: `docker compose -f dockerfiles/docker-compose.yml --profile completo up -d --build`.

### Perfil `prod`

Defina `SPRING_PROFILES_ACTIVE=prod` e as variáveis do Azure SQL, do CloudAMQP e do JWT. No Azure, quem faz isso é o script `scripts/infra-criar.sh` e a pipeline.

## Variáveis de ambiente

Os valores reais nunca vão para o Git; os nomes estão em [`.env.example`](.env.example).

| Variável | Uso | Padrão |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` (H2 + RabbitMQ local) ou `prod` (Azure SQL + CloudAMQP) | `dev` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Banco do perfil `prod` | |
| `JWT_SECRET` | Segredo HS256, mínimo de 32 caracteres; obrigatório em `prod` | gerado a cada início em `dev` |
| `JWT_EXPIRATION_MINUTES` | Validade do token | 120 |
| `RABBITMQ_URI` | URI `amqps` do CloudAMQP | `amqp://guest:guest@localhost:5672` |
| `IA_SERVICE_URL`, `IA_TIMEOUT_SECONDS`, `IA_API_KEY` | Serviço de IA, tempo limite e chave enviada no `X-API-Key` | `http://localhost:8000`, 10, vazio |
| `IA_CONFIANCA_MINIMA` | Abaixo disso, sem alerta automático | 0.6 |
| `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL` | Provedor do Spring AI | Groq, sem chave, `openai/gpt-oss-120b` |
| `LLM_COMPLETIONS_PATH`, `LLM_TIMEOUT_SECONDS` | Caminho do endpoint de chat e tempo limite | `/v1/chat/completions`, 20 |
| `ALERTA_RAIO_KM` | Raio dos alertas | 10 |
| `FOTOS_DIR` | Pasta das fotos | `./data/fotos` (`/home/data/fotos` no Azure) |
| `CORS_ORIGENS` | Origens de navegador liberadas | vazio |

## Usuários de teste

Criados automaticamente na primeira inicialização (as senhas ficam no código só como hash BCrypt).

| Papel | E-mail | Senha |
| --- | --- | --- |
| PRODUTOR | `produtor@valence.local` | `Produtor@2026` |
| PRODUTOR (vizinho, a cerca de 6 km) | `vizinho@valence.local` | `Vizinho@2026` |
| AGRONOMO | `agronomo@valence.local` | `Agronomo@2026` |
| GESTOR | `gestor@valence.local` | `Gestor@2026` |

O produtor e o vizinho já têm uma propriedade de milho cada, em Ribeirão Preto/SP, para a demonstração do alerta entre vizinhos.

## Endpoints principais

Listas aceitam `?page=0&size=10&sort=campo,desc` e respondem com `content`, `totalElements`, `totalPages`, `number`, `size`, `first` e `last`.

| Método | Rota | Papel | Descrição |
| --- | --- | --- | --- |
| POST | `/api/auth/registrar` | público | Cadastro de produtor; devolve o token |
| POST | `/api/auth/login` | público | Login |
| GET, PUT | `/api/usuarios/me` | autenticado | Perfil do usuário logado |
| GET, POST | `/api/propriedades` | PRODUTOR | Lista paginada e cadastro |
| GET, PUT, DELETE | `/api/propriedades/{id}` | PRODUTOR | Detalhe, edição e remoção |
| GET, POST | `/api/ocorrencias` | PRODUTOR | Lista (filtro `status`) e registro com `Idempotency-Key` |
| GET | `/api/ocorrencias/{id}` | dono, AGRONOMO, GESTOR | Detalhe com triagem e laudo |
| PUT, DELETE | `/api/ocorrencias/{id}` | dono | Edição e remoção enquanto `RECEBIDA` ou `TRIADA` |
| POST | `/api/ocorrencias/{id}/fotos` | dono | Envio da foto (parte `foto`) e triagem |
| GET | `/api/ocorrencias/{id}/fotos/{fotoId}` | dono, AGRONOMO, GESTOR | Bytes da foto |
| POST | `/api/ocorrencias/{id}/triagem` | dono | Reprocessa a triagem |
| GET | `/api/alertas` | PRODUTOR | Alertas das propriedades do usuário |
| PATCH | `/api/alertas/{id}/lido` | PRODUTOR | Marca como lido |
| GET | `/api/analises` | AGRONOMO | Fila `TRIADA` e `EM_ANALISE` por severidade |
| POST | `/api/ocorrencias/{id}/laudo` | AGRONOMO | Emite o laudo |
| GET, POST | `/api/mural` | autenticado | Publicações da região e nova publicação |
| GET, PUT, DELETE | `/api/mural/{id}` | autenticado, autor, autor ou moderador | Detalhe, edição e remoção |
| GET, POST | `/api/mural/{id}/comentarios` | autenticado | Comentários |
| POST | `/api/mural/{id}/denuncias` | autenticado | Denúncia analisada pelo Spring AI (409 se repetir) |
| GET | `/api/moderacao` | AGRONOMO, GESTOR | Publicações com denúncias pendentes |
| PATCH | `/api/moderacao/{postId}` | AGRONOMO, GESTOR | Decisão `MANTER` ou `REMOVER` |
| GET | `/actuator/health` | público | Saúde da API |
| GET | `/swagger-ui.html` | público | Documentação |

## Fluxo de autenticação JWT

1. `POST /api/auth/login` envia e-mail e senha ao `AuthenticationManager`, que usa o `AutenticacaoService` (`UserDetailsService`) e compara a senha com o hash BCrypt.
2. Com a senha certa, o `TokenService` assina um JWT HS256 com `sub` (id do usuário), `email`, `papel`, `iat`, `exp` e emissor `valence-api`.
3. O app envia `Authorization: Bearer <token>` em toda requisição.
4. O `SecurityFilter` valida assinatura, emissor e expiração, carrega o usuário e coloca no `SecurityContext` a autoridade `ROLE_<PAPEL>`. A sessão é stateless.
5. O `SecurityFilterChain` exige token em tudo que não é público, e cada controller declara o papel com `@PreAuthorize`. Sem token ou com token inválido: 401 com `WWW-Authenticate: Bearer`; papel errado: 403. Os dois no formato de erro do contrato.

## Spring AI

As duas funcionalidades de IA generativa estão dentro do fluxo do produto, não em um chat solto.

**Triagem da ocorrência** (`TriagemService` e `TriagemIaService`)
1. O produtor envia a foto. A API chama o serviço de IA para classificar a folha (visão computacional) e consultar as fichas técnicas (RAG), com tempo limite.
2. O `ChatClient` recebe uma instrução de sistema (papel, domínio, idioma e limites, como nunca citar agrotóxico ou dose) e os dados da ocorrência sem dados pessoais.
3. A resposta vem como JSON e o Spring AI converte para o record `TriagemIaDto` com `.entity(...)`. A API normaliza o resultado (remove cuidados com produto ou dose, limita o texto do alerta a 200 caracteres) e valida com Bean Validation.
4. Com confiança acima de `IA_CONFIANCA_MINIMA`, a ocorrência fica `TRIADA` e o evento `ocorrencia.triada` é publicado. Caso contrário, vai para `EM_ANALISE`, sem alerta automático.

**Análise de denúncia** (`DenunciaService` e `DenunciaIaService`)
1. Antes do LLM, uma regra local detecta telefone, e-mail, CPF ou coordenada no texto; nesse caso a denúncia já é `DADOS_PESSOAIS`, gravidade `ALTA`, sem enviar o texto ao modelo.
2. Nos demais casos, o `ChatClient` devolve `AnaliseDenunciaIaDto` (categoria, gravidade, ação e justificativa).
3. Gravidade `ALTA` oculta a publicação de forma provisória e ela entra na fila de moderação. A IA nunca remove: quem decide é o agrônomo ou o gestor.

**Falhas tratadas:** serviço de IA fora do ar (a triagem segue só com o texto), LLM sem chave, sem cota, com limite de requisições, fora do ar ou com resposta fora do formato (a ocorrência vai para o agrônomo e a denúncia fica pendente para moderação humana). O motivo vai para o log sem expor segredos. Nenhum nome, e-mail, telefone ou coordenada é enviado ao modelo.

## Arquitetura distribuída (opção A, RabbitMQ)

| Item | Valor |
| --- | --- |
| Exchange | `valence.eventos`, tipo topic, durável |
| Fila da API Java | `valence.java.alertas`, ligada a `ocorrencia.triada` |
| Fila do Observatório | `valence.observatorio` (declarada pelo .NET) |
| Publicados pela API | `ocorrencia.triada` (triagem confiável) e `laudo.emitido` (todo laudo) |
| Consumido pela API | `ocorrencia.triada`, no `OcorrenciaTriadaConsumer` |

- O `EventoProducer` publica só **depois do commit** da transação (`@TransactionalEventListener(AFTER_COMMIT)`), em JSON UTF-8 com `messageId` igual ao `eventoId`. Com o broker fora do ar, a requisição não falha: o erro vai para o log.
- O consumidor busca propriedades da mesma cultura no raio de `ALERTA_RAIO_KM` (fórmula de Haversine) e cria alertas `SUSPEITA` com a distância arredondada.
- **Idempotência:** cada `eventoId` processado fica em `TB_EVENTO_PROCESSADO`; uma mensagem repetida é ignorada. O par ocorrência e propriedade é único em `TB_ALERTA`.
- Mensagem com erro é tentada 3 vezes e depois descartada sem reenfileirar.

Para ver funcionando: suba a API, faça login como `produtor@valence.local`, registre uma ocorrência de milho na propriedade de teste e envie uma foto. Com a triagem confiável, o painel do RabbitMQ mostra a mensagem e `vizinho@valence.local` passa a ter um alerta em `GET /api/alertas`.

## Modelagem

```mermaid
erDiagram
    TB_USUARIO ||--o{ TB_PROPRIEDADE : possui
    TB_USUARIO ||--o{ TB_OCORRENCIA : registra
    TB_PROPRIEDADE ||--o{ TB_PROPRIEDADE_CULTURA : cultiva
    TB_PROPRIEDADE ||--o{ TB_OCORRENCIA : "local da"
    TB_OCORRENCIA ||--o{ TB_FOTO_OCORRENCIA : tem
    TB_OCORRENCIA ||--o| TB_TRIAGEM : recebe
    TB_OCORRENCIA ||--o{ TB_LAUDO : recebe
    TB_USUARIO ||--o{ TB_LAUDO : "emite (agrônomo)"
    TB_OCORRENCIA ||--o{ TB_ALERTA : gera
    TB_PROPRIEDADE ||--o{ TB_ALERTA : recebe
    TB_USUARIO ||--o{ TB_POST_MURAL : publica
    TB_POST_MURAL ||--o{ TB_COMENTARIO : tem
    TB_POST_MURAL ||--o{ TB_DENUNCIA : recebe
    TB_USUARIO ||--o{ TB_COMENTARIO : escreve
    TB_USUARIO ||--o{ TB_DENUNCIA : faz
```

`TB_EVENTO_PROCESSADO` guarda os eventos já consumidos. O DDL completo está em [`scripts/script-bd.sql`](scripts/script-bd.sql).

Status da ocorrência: `RECEBIDA` → `TRIADA` ou `EM_ANALISE` → `CONFIRMADA` ou `DESCARTADA`. Laudo `MAIS_INFORMACOES` mantém `EM_ANALISE`. Ocorrências sem foto há 10 minutos passam por triagem só com o texto.

## Formato de erro

```json
{
  "timestamp": "2026-11-10T14:32:00Z",
  "status": 400,
  "erro": "Requisição inválida",
  "mensagem": "Um ou mais campos são inválidos.",
  "caminho": "/api/ocorrencias",
  "campos": [ { "campo": "descricao", "mensagem": "A descrição é obrigatória." } ]
}
```

Códigos usados: 200, 201, 204, 400, 401, 403, 404, 405, 409, 413, 415, 422 e 500.

## Testes

```bash
./mvnw test
```

Os testes rodam sem rede, sem broker, sem LLM e sem o serviço de IA: o `ChatModel`, o cliente do serviço de IA e o `RabbitTemplate` são simulados com `@MockitoBean`, e o banco é H2 em memória. Cobrem cadastro e login, 401 e 403 por papel, validação e formato de erro, CRUD com paginação, `Idempotency-Key`, envio de foto, triagem com e sem falhas do modelo, consumo idempotente de eventos, alertas, laudo, mural, denúncia e moderação.

## Deploy no Azure

1. **Infraestrutura:** no Azure Cloud Shell (Bash), com o repositório clonado:
   ```bash
   export SQL_ADMIN_PASSWORD='senha-forte'
   export LLM_API_KEY='chave-da-groq'
   export RABBITMQ_URI='amqps://...'
   bash scripts/infra-criar.sh
   ```
   Cria o Resource Group `rg-valence`, o Azure SQL `sql-valence-rm563620` (banco `valence`, Basic), o plano B1 Linux e o Web App `app-valence-api-rm563620` com Java 21, Always On e as variáveis de ambiente. `scripts/infra-remover.sh` apaga tudo.
2. **Pipeline:** `azure-pipeline.yml` tem o estágio **Build** (Java 21, `mvn clean package`, testes JUnit publicados e artefato `.jar`) e o estágio **Release** (deploy no App Service e verificação de saúde), que roda automaticamente depois de um artefato novo. O gatilho é a branch `main`, protegida por política de PR.
3. **Configuração no Azure DevOps:** conexão de serviço `valence-azure` com a assinatura e grupo de variáveis `valence-segredos` com `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `RABBITMQ_URI`, `LLM_API_KEY`, `LLM_MODEL`, `IA_SERVICE_URL` e `IA_API_KEY` (marcar como secretas).

## JSON de CRUD

**Propriedade** (`/api/propriedades`, token de PRODUTOR)

```json
POST /api/propriedades
{
  "nome": "Sítio Boa Vista",
  "municipio": "Ribeirão Preto",
  "uf": "SP",
  "latitude": -21.21,
  "longitude": -47.84,
  "areaHectares": 15.5,
  "culturas": ["MILHO", "TOMATE"]
}

PUT /api/propriedades/{id}
{
  "nome": "Sítio Boa Vista II",
  "municipio": "Ribeirão Preto",
  "uf": "SP",
  "latitude": -21.21,
  "longitude": -47.84,
  "areaHectares": 18.0,
  "culturas": ["MILHO"]
}

GET /api/propriedades?page=0&size=10
GET /api/propriedades/{id}
DELETE /api/propriedades/{id}
```

**Ocorrência** (`/api/ocorrencias`, token de PRODUTOR)

```json
POST /api/ocorrencias
Idempotency-Key: 3f6c1e2a-8b4d-4e9a-9c1f-2d7e5a6b8c90
{
  "propriedadeId": 1,
  "cultura": "MILHO",
  "descricao": "Folhas com pústulas alaranjadas e plantas avermelhadas perto da cerca."
}

PUT /api/ocorrencias/{id}
{
  "propriedadeId": 1,
  "cultura": "MILHO",
  "descricao": "Pústulas alaranjadas nas duas faces da folha."
}

GET /api/ocorrencias?status=RECEBIDA&page=0&size=10&sort=dataRegistro,desc
GET /api/ocorrencias/{id}
DELETE /api/ocorrencias/{id}
```

Consultas para conferir no banco: `SELECT * FROM TB_PROPRIEDADE;` e `SELECT * FROM TB_OCORRENCIA;`.

## Coleção do Postman

[`postman/valence-api.postman_collection.json`](postman/valence-api.postman_collection.json): importe no Postman, ajuste `baseUrl` e rode as pastas na ordem. Os logins guardam os tokens nas variáveis da coleção.

## Uso de IA generativa

- **No produto:** LLM da Groq pelo Spring AI (triagem e análise de denúncias), como descrito acima.
- **No desenvolvimento:** o Claude (Anthropic) foi usado como apoio para estruturar o projeto, sugerir código e testes, revisar o README e explicar erros. Todo o código foi revisado, executado e entendido pelo integrante responsável, que responde por ele na apresentação.
