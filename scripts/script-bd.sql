-- Valence API · Azure SQL (SQL Server)
-- DDL dos objetos usados pela API Java. Pode ser executado mais de uma vez.

-- Tabelas DDL
IF OBJECT_ID(N'dbo.TB_USUARIO', N'U') IS NULL
CREATE TABLE dbo.TB_USUARIO (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    nome        NVARCHAR(100)        NOT NULL,
    email       NVARCHAR(150)        NOT NULL,
    senha_hash  NVARCHAR(100)        NOT NULL,
    papel       NVARCHAR(20)         NOT NULL,
    telefone    NVARCHAR(20)         NULL,
    municipio   NVARCHAR(100)        NOT NULL,
    uf          NVARCHAR(2)          NOT NULL,
    criado_em   DATETIMEOFFSET(6)    NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_papel CHECK (papel IN ('PRODUTOR', 'AGRONOMO', 'GESTOR'))
);

IF OBJECT_ID(N'dbo.TB_PROPRIEDADE', N'U') IS NULL
CREATE TABLE dbo.TB_PROPRIEDADE (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    nome           NVARCHAR(100)        NOT NULL,
    municipio      NVARCHAR(100)        NOT NULL,
    uf             NVARCHAR(2)          NOT NULL,
    latitude       FLOAT(53)            NOT NULL,
    longitude      FLOAT(53)            NOT NULL,
    area_hectares  FLOAT(53)            NOT NULL,
    criado_em      DATETIMEOFFSET(6)    NOT NULL,
    usuario_id     BIGINT               NOT NULL,
    CONSTRAINT pk_propriedade PRIMARY KEY (id),
    CONSTRAINT fk_propriedade_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.TB_USUARIO (id)
);

IF OBJECT_ID(N'dbo.TB_PROPRIEDADE_CULTURA', N'U') IS NULL
CREATE TABLE dbo.TB_PROPRIEDADE_CULTURA (
    propriedade_id BIGINT       NOT NULL,
    cultura        NVARCHAR(20) NOT NULL,
    CONSTRAINT pk_propriedade_cultura PRIMARY KEY (propriedade_id, cultura),
    CONSTRAINT fk_cultura_propriedade FOREIGN KEY (propriedade_id) REFERENCES dbo.TB_PROPRIEDADE (id),
    CONSTRAINT ck_propriedade_cultura CHECK (cultura IN ('MILHO', 'TOMATE', 'BATATA'))
);

IF OBJECT_ID(N'dbo.TB_OCORRENCIA', N'U') IS NULL
CREATE TABLE dbo.TB_OCORRENCIA (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    cultura          NVARCHAR(20)         NOT NULL,
    descricao        NVARCHAR(1000)       NOT NULL,
    status           NVARCHAR(20)         NOT NULL,
    severidade       INT                  NULL,
    idempotency_key  NVARCHAR(36)         NOT NULL,
    data_registro    DATETIMEOFFSET(6)    NOT NULL,
    atualizado_em    DATETIMEOFFSET(6)    NOT NULL,
    versao           BIGINT               NULL,
    propriedade_id   BIGINT               NOT NULL,
    usuario_id       BIGINT               NOT NULL,
    CONSTRAINT pk_ocorrencia PRIMARY KEY (id),
    CONSTRAINT uk_ocorrencia_usuario_chave UNIQUE (usuario_id, idempotency_key),
    CONSTRAINT fk_ocorrencia_propriedade FOREIGN KEY (propriedade_id) REFERENCES dbo.TB_PROPRIEDADE (id),
    CONSTRAINT fk_ocorrencia_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.TB_USUARIO (id),
    CONSTRAINT ck_ocorrencia_cultura CHECK (cultura IN ('MILHO', 'TOMATE', 'BATATA')),
    CONSTRAINT ck_ocorrencia_status CHECK (status IN ('RECEBIDA', 'TRIADA', 'EM_ANALISE', 'CONFIRMADA', 'DESCARTADA')),
    CONSTRAINT ck_ocorrencia_severidade CHECK (severidade IS NULL OR severidade BETWEEN 1 AND 5)
);

IF OBJECT_ID(N'dbo.TB_FOTO_OCORRENCIA', N'U') IS NULL
CREATE TABLE dbo.TB_FOTO_OCORRENCIA (
    id               BIGINT IDENTITY(1,1) NOT NULL,
    caminho_arquivo  NVARCHAR(255)        NOT NULL,
    content_type     NVARCHAR(50)         NOT NULL,
    tamanho_bytes    BIGINT               NOT NULL,
    enviada_em       DATETIMEOFFSET(6)    NOT NULL,
    ocorrencia_id    BIGINT               NOT NULL,
    CONSTRAINT pk_foto_ocorrencia PRIMARY KEY (id),
    CONSTRAINT fk_foto_ocorrencia FOREIGN KEY (ocorrencia_id) REFERENCES dbo.TB_OCORRENCIA (id)
);

IF OBJECT_ID(N'dbo.TB_TRIAGEM', N'U') IS NULL
CREATE TABLE dbo.TB_TRIAGEM (
    id                   BIGINT IDENTITY(1,1) NOT NULL,
    hipoteses            NVARCHAR(2000)       NOT NULL,
    confianca            FLOAT(53)            NULL,
    severidade           INT                  NULL,
    cuidados_imediatos   NVARCHAR(2000)       NOT NULL,
    texto_alerta         NVARCHAR(255)        NULL,
    encaminhar_agronomo  BIT                  NOT NULL,
    gerada_em            DATETIMEOFFSET(6)    NOT NULL,
    explicacao           NVARCHAR(1500)       NULL,
    fontes               NVARCHAR(4000)       NOT NULL,
    motivo_imagem        NVARCHAR(40)         NULL,
    ocorrencia_id        BIGINT               NOT NULL,
    CONSTRAINT pk_triagem PRIMARY KEY (id),
    CONSTRAINT uk_triagem_ocorrencia UNIQUE (ocorrencia_id),
    CONSTRAINT fk_triagem_ocorrencia FOREIGN KEY (ocorrencia_id) REFERENCES dbo.TB_OCORRENCIA (id)
);

IF OBJECT_ID(N'dbo.TB_LAUDO', N'U') IS NULL
CREATE TABLE dbo.TB_LAUDO (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    decisao           NVARCHAR(20)         NOT NULL,
    praga_confirmada  NVARCHAR(150)        NULL,
    parecer           NVARCHAR(2000)       NOT NULL,
    emitido_em        DATETIMEOFFSET(6)    NOT NULL,
    ocorrencia_id     BIGINT               NOT NULL,
    agronomo_id       BIGINT               NOT NULL,
    CONSTRAINT pk_laudo PRIMARY KEY (id),
    CONSTRAINT fk_laudo_ocorrencia FOREIGN KEY (ocorrencia_id) REFERENCES dbo.TB_OCORRENCIA (id),
    CONSTRAINT fk_laudo_agronomo FOREIGN KEY (agronomo_id) REFERENCES dbo.TB_USUARIO (id),
    CONSTRAINT ck_laudo_decisao CHECK (decisao IN ('CONFIRMADA', 'DESCARTADA', 'MAIS_INFORMACOES'))
);

IF OBJECT_ID(N'dbo.TB_ALERTA', N'U') IS NULL
CREATE TABLE dbo.TB_ALERTA (
    id              BIGINT IDENTITY(1,1) NOT NULL,
    nivel           NVARCHAR(20)         NOT NULL,
    distancia_km    FLOAT(53)            NOT NULL,
    lido            BIT                  NOT NULL,
    criado_em       DATETIMEOFFSET(6)    NOT NULL,
    ocorrencia_id   BIGINT               NOT NULL,
    propriedade_id  BIGINT               NOT NULL,
    CONSTRAINT pk_alerta PRIMARY KEY (id),
    CONSTRAINT uk_alerta_ocorrencia_propriedade UNIQUE (ocorrencia_id, propriedade_id),
    CONSTRAINT fk_alerta_ocorrencia FOREIGN KEY (ocorrencia_id) REFERENCES dbo.TB_OCORRENCIA (id),
    CONSTRAINT fk_alerta_propriedade FOREIGN KEY (propriedade_id) REFERENCES dbo.TB_PROPRIEDADE (id),
    CONSTRAINT ck_alerta_nivel CHECK (nivel IN ('SUSPEITA', 'CONFIRMADO', 'ENCERRADO'))
);

IF OBJECT_ID(N'dbo.TB_POST_MURAL', N'U') IS NULL
CREATE TABLE dbo.TB_POST_MURAL (
    id             BIGINT IDENTITY(1,1) NOT NULL,
    municipio      NVARCHAR(100)        NOT NULL,
    uf             NVARCHAR(2)          NOT NULL,
    texto          NVARCHAR(1000)       NOT NULL,
    status         NVARCHAR(20)         NOT NULL,
    ocorrencia_id  BIGINT               NULL,
    criado_em      DATETIMEOFFSET(6)    NOT NULL,
    usuario_id     BIGINT               NOT NULL,
    CONSTRAINT pk_post_mural PRIMARY KEY (id),
    CONSTRAINT fk_post_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.TB_USUARIO (id),
    CONSTRAINT ck_post_status CHECK (status IN ('ATIVO', 'OCULTO', 'REMOVIDO'))
);

IF OBJECT_ID(N'dbo.TB_COMENTARIO', N'U') IS NULL
CREATE TABLE dbo.TB_COMENTARIO (
    id          BIGINT IDENTITY(1,1) NOT NULL,
    texto       NVARCHAR(500)        NOT NULL,
    criado_em   DATETIMEOFFSET(6)    NOT NULL,
    post_id     BIGINT               NOT NULL,
    usuario_id  BIGINT               NOT NULL,
    CONSTRAINT pk_comentario PRIMARY KEY (id),
    CONSTRAINT fk_comentario_post FOREIGN KEY (post_id) REFERENCES dbo.TB_POST_MURAL (id),
    CONSTRAINT fk_comentario_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.TB_USUARIO (id)
);

IF OBJECT_ID(N'dbo.TB_DENUNCIA', N'U') IS NULL
CREATE TABLE dbo.TB_DENUNCIA (
    id                BIGINT IDENTITY(1,1) NOT NULL,
    motivo            NVARCHAR(500)        NOT NULL,
    categoria_ia      NVARCHAR(30)         NULL,
    gravidade_ia      NVARCHAR(10)         NULL,
    acao_ia           NVARCHAR(20)         NULL,
    justificativa_ia  NVARCHAR(500)        NULL,
    decisao_final     NVARCHAR(20)         NULL,
    criada_em         DATETIMEOFFSET(6)    NOT NULL,
    decidida_em       DATETIMEOFFSET(6)    NULL,
    post_id           BIGINT               NOT NULL,
    usuario_id        BIGINT               NOT NULL,
    CONSTRAINT pk_denuncia PRIMARY KEY (id),
    CONSTRAINT uk_denuncia_post_usuario UNIQUE (post_id, usuario_id),
    CONSTRAINT fk_denuncia_post FOREIGN KEY (post_id) REFERENCES dbo.TB_POST_MURAL (id),
    CONSTRAINT fk_denuncia_usuario FOREIGN KEY (usuario_id) REFERENCES dbo.TB_USUARIO (id)
);

IF OBJECT_ID(N'dbo.TB_EVENTO_PROCESSADO', N'U') IS NULL
CREATE TABLE dbo.TB_EVENTO_PROCESSADO (
    evento_id      UNIQUEIDENTIFIER   NOT NULL,
    tipo           NVARCHAR(50)       NOT NULL,
    processado_em  DATETIMEOFFSET(6)  NOT NULL,
    CONSTRAINT pk_evento_processado PRIMARY KEY (evento_id)
);

-- Índices de consulta
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_propriedade_coordenadas')
CREATE INDEX ix_propriedade_coordenadas ON dbo.TB_PROPRIEDADE (latitude, longitude);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_ocorrencia_status')
CREATE INDEX ix_ocorrencia_status ON dbo.TB_OCORRENCIA (status, severidade);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = 'ix_post_regiao')
CREATE INDEX ix_post_regiao ON dbo.TB_POST_MURAL (uf, municipio, status);
