
BEGIN;

-- 1. Organizacao e seus membros
CREATE TABLE empresa_junior (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE squad (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_junior_id INT NOT NULL REFERENCES empresa_junior(id),
    nome VARCHAR(10) NOT NULL,
    UNIQUE (empresa_junior_id, nome)
);

CREATE TABLE funcionario (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_junior_id INT NOT NULL REFERENCES empresa_junior(id),
    squad_id INT REFERENCES squad(id),
    nome VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL UNIQUE,
    matricula VARCHAR(8) NOT NULL UNIQUE,
    curso VARCHAR(25) NOT NULL, 
    semestre_ingresso VARCHAR(5) NOT NULL,
    papel VARCHAR(20) NOT NULL CHECK (papel IN ('DIRETOR', 'COLABORADOR')),
    diretoria VARCHAR(10),
    avaliacao TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_colaborador_tem_squad
        CHECK (papel <> 'COLABORADOR' OR squad_id IS NOT NULL)
);

-- Credenciais separadas dos dados de negocio. Nunca salvar senha em texto puro.
CREATE TABLE usuario (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    funcionario_id INT NOT NULL UNIQUE REFERENCES funcionario(id),
    senha_hash VARCHAR(100) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Estrutura financeira e metas
CREATE TABLE caixa (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_junior_id INT NOT NULL REFERENCES empresa_junior(id),
    nome VARCHAR(50) NOT NULL,
    saldo_inicial NUMERIC(9,2) NOT NULL DEFAULT 0,
    limite_despesas NUMERIC(9,2),
    CONSTRAINT ck_limite_caixa CHECK (limite_despesas IS NULL OR limite_despesas >= 0),
    UNIQUE (empresa_junior_id, nome)
);

CREATE TABLE meta_financeira (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_junior_id INT NOT NULL REFERENCES empresa_junior(id),
    nome VARCHAR(120) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    valor_meta NUMERIC(9,2) NOT NULL CHECK (valor_meta > 0),
    CONSTRAINT ck_periodo_meta CHECK (data_fim >= data_inicio)
);

CREATE TABLE categoria_orcamentaria (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_junior_id INT NOT NULL REFERENCES empresa_junior(id),
    nome VARCHAR(15) NOT NULL,
    descricao TEXT,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_junior_id, nome)
);

-- Orçamento total reservado em determinado periodo (diferente de meta de receita).
CREATE TABLE orcamento_periodo (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_junior_id INT NOT NULL REFERENCES empresa_junior(id),
    nome VARCHAR(25) NOT NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    valor_total NUMERIC(9,2) NOT NULL CHECK (valor_total > 0),
    status VARCHAR(15) NOT NULL DEFAULT 'RASCUNHO'
        CHECK (status IN ('RASCUNHO', 'ATIVO', 'ENCERRADO')),
    CONSTRAINT ck_periodo_orcamento CHECK (data_fim >= data_inicio)
);

CREATE TABLE distribuicao_orcamento (
    orcamento_periodo_id INT NOT NULL REFERENCES orcamento_periodo(id),
    categoria_id INT NOT NULL REFERENCES categoria_orcamentaria(id),
    percentual NUMERIC(5,2) NOT NULL CHECK (percentual > 0 AND percentual <= 100),
    PRIMARY KEY (orcamento_periodo_id, categoria_id)
);

-- 3. Projetos, contratos e parcelas
CREATE TABLE projeto (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    empresa_junior_id INT NOT NULL REFERENCES empresa_junior(id),
    squad_id INT REFERENCES squad(id),
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO'
        CHECK (status IN ('ATIVO', 'CONCLUIDO', 'CANCELADO'))
);

CREATE TABLE contrato (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    projeto_id INT NOT NULL REFERENCES projeto(id),
    cliente_nome VARCHAR(35) NOT NULL,
    arquivo_ref TEXT,
    valor_total NUMERIC(9,2) NOT NULL CHECK (valor_total > 0),
    data_inicio DATE NOT NULL,
    data_fim DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO'
        CHECK (status IN ('RASCUNHO', 'ATIVO', 'ENCERRADO', 'RESCINDIDO')),
    CONSTRAINT ck_periodo_contrato CHECK (data_fim >= data_inicio)
);

CREATE TABLE prestacao (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    contrato_id INT NOT NULL REFERENCES contrato(id),
    numero INTEGER NOT NULL CHECK (numero > 0),
    valor NUMERIC(9,2) NOT NULL CHECK (valor > 0),
    data_vencimento DATE NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE', 'PAGA', 'CANCELADA')),
    UNIQUE (contrato_id, numero)
);

-- 4. Entradas e saidas efetivamente realizadas
CREATE TABLE transacao (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    caixa_id INT NOT NULL REFERENCES caixa(id),
    registrado_por_id INT REFERENCES funcionario(id),
    projeto_id INT REFERENCES projeto(id),
    prestacao_id INT REFERENCES prestacao(id),
    categoria_id INT REFERENCES categoria_orcamentaria(id),
    descricao VARCHAR(255) NOT NULL,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('RECEITA', 'DESPESA')),
    valor NUMERIC(9,2) NOT NULL CHECK (valor > 0),
    data_movimento DATE NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_despesa_categorizada
        CHECK (tipo <> 'DESPESA' OR categoria_id IS NOT NULL),
    CONSTRAINT ck_prestacao_e_receita
        CHECK (prestacao_id IS NULL OR tipo = 'RECEITA')
);

-- Um documento pode estar ligado a uma prestacao OU a uma transacao.
CREATE TABLE comprovante (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prestacao_id INT REFERENCES prestacao(id),
    transacao_id INT REFERENCES transacao(id),
    tipo VARCHAR(25) NOT NULL
        CHECK (tipo IN ('RECIBO', 'NOTA_FISCAL', 'COMPROVANTE_PAGAMENTO', 'OUTRO')),
    arquivo_ref TEXT NOT NULL,
    numero_nota_fiscal VARCHAR(60),
    valor NUMERIC(9,2) CHECK (valor IS NULL OR valor > 0),
    data_documento DATE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_documento_vinculo_unico
        CHECK (num_nonnulls(prestacao_id, transacao_id) = 1)
);

-- 5. Pedidos de orcamento dos colaboradores
CREATE TABLE solicitacao_orcamento (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    solicitante_id INT NOT NULL REFERENCES funcionario(id),
    orcamento_periodo_id INT REFERENCES orcamento_periodo(id),
    categoria_id INT NOT NULL REFERENCES categoria_orcamentaria(id),
    titulo VARCHAR(150) NOT NULL,
    descricao TEXT NOT NULL,
    valor_solicitado NUMERIC(9,2) NOT NULL CHECK (valor_solicitado > 0),
    arquivo_ref TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE', 'EM_ANALISE', 'APROVADO', 'REJEITADO')),
    avaliado_por_id INT REFERENCES funcionario(id),
    justificativa_decisao TEXT,
    data_decisao TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_solicitacao_decidida
        CHECK (status NOT IN ('APROVADO', 'REJEITADO')
               OR (avaliado_por_id IS NOT NULL AND data_decisao IS NOT NULL))
);

-- Indices que auxiliam consultas comuns
CREATE INDEX idx_funcionario_squad ON funcionario(squad_id);
CREATE INDEX idx_prestacao_vencimento ON prestacao(data_vencimento);
CREATE INDEX idx_transacao_caixa_data ON transacao(caixa_id, data_movimento);
CREATE INDEX idx_transacao_categoria_data ON transacao(categoria_id, data_movimento);
CREATE INDEX idx_transacao_prestacao ON transacao(prestacao_id);
CREATE INDEX idx_solicitacao_status ON solicitacao_orcamento(status);
CREATE INDEX idx_solicitacao_solicitante ON solicitacao_orcamento(solicitante_id);

-- Saldo atual e derivado das transacoes: evita inconsistencias ao manter
-- um campo 'saldo' manualmente na tabela caixa.
CREATE VIEW vw_saldo_caixa AS
SELECT
    c.id AS caixa_id,
    c.empresa_junior_id,
    c.nome,
    c.saldo_inicial,
    c.saldo_inicial + COALESCE(
        SUM(CASE WHEN t.tipo = 'RECEITA' THEN t.valor ELSE -t.valor END),
        0
    ) AS saldo_atual
FROM caixa c
LEFT JOIN transacao t ON t.caixa_id = c.id
GROUP BY c.id, c.empresa_junior_id, c.nome, c.saldo_inicial;

-- Consumo de cada categoria dentro do periodo do orcamento.
-- Considera somente transacoes realizadas, nao solicitacoes pendentes.
CREATE VIEW vw_consumo_orcamento AS
SELECT
    d.orcamento_periodo_id,
    d.categoria_id,
    cat.nome AS categoria,
    d.percentual,
    o.valor_total AS orcamento_base,
    ROUND(o.valor_total * d.percentual / 100, 2) AS limite_categoria,
    COALESCE(SUM(t.valor), 0) AS valor_utilizado,
    ROUND(o.valor_total * d.percentual / 100, 2)
        - COALESCE(SUM(t.valor), 0) AS saldo_categoria
FROM distribuicao_orcamento d
JOIN orcamento_periodo o ON o.id = d.orcamento_periodo_id
JOIN categoria_orcamentaria cat ON cat.id = d.categoria_id
LEFT JOIN transacao t ON t.categoria_id = d.categoria_id
    AND t.tipo = 'DESPESA'
    AND t.data_movimento BETWEEN o.data_inicio AND o.data_fim
    AND EXISTS (
        SELECT 1
        FROM caixa c
        WHERE c.id = t.caixa_id AND c.empresa_junior_id = o.empresa_junior_id
    )
GROUP BY d.orcamento_periodo_id, d.categoria_id,
         cat.nome, d.percentual, o.valor_total;

COMMIT;