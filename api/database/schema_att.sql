BEGIN;

CREATE TABLE public.empresa_junior (
    id integer GENERATED ALWAYS AS IDENTITY,
    nome varchar(50) NOT NULL
);

CREATE TABLE public.squad (
    id integer GENERATED ALWAYS AS IDENTITY,
    empresa_junior_id integer NOT NULL,
    nome varchar(10) NOT NULL
);

CREATE TABLE public.usuario (
    matricula varchar(8) NOT NULL,
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    senha_hash varchar(100) NOT NULL,
    ativo boolean NOT NULL DEFAULT true,
    criado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    role varchar(20) NOT NULL DEFAULT 'USER',
    nome varchar(50) NOT NULL,
    email varchar(50) NOT NULL,
    curso varchar(25) NOT NULL,
    semestre_ingresso varchar(5) NOT NULL,
	squad_id integer,
    papel_squad varchar(20),
    CONSTRAINT ck_usuario_role CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[]))),
    CONSTRAINT ck_usuario_papel_squad CHECK (papel_squad IS NULL OR papel_squad IN ('DIRETOR', 'MEMBRO')),
    CONSTRAINT ck_usuario_papel_exige_squad CHECK (papel_squad IS NULL OR squad_id IS NOT NULL)
);

CREATE TABLE public.caixa (
    id integer GENERATED ALWAYS AS IDENTITY,
    empresa_junior_id integer NOT NULL,
    nome character varying(50) NOT NULL,
    saldo_inicial numeric(9,2) NOT NULL DEFAULT 0,
    limite_despesas numeric(9,2),
    CONSTRAINT ck_limite_caixa CHECK (limite_despesas IS NULL OR limite_despesas >= 0)
);

CREATE TABLE public.categoria_orcamentaria (
    id integer GENERATED ALWAYS AS IDENTITY,
    empresa_junior_id integer NOT NULL,
    nome varchar(15) NOT NULL,
    descricao text,
    ativa boolean NOT NULL DEFAULT true
);

CREATE TABLE public.meta_financeira (
    id integer GENERATED ALWAYS AS IDENTITY,
    empresa_junior_id integer NOT NULL,
    nome varchar(120) NOT NULL,
    data_inicio date NOT NULL,
    data_fim date NOT NULL,
    valor_meta numeric(9,2) NOT NULL,
    CONSTRAINT ck_periodo_meta CHECK ((data_fim >= data_inicio)),
    CONSTRAINT meta_financeira_valor_meta_check CHECK ((valor_meta > (0)::numeric))
);

CREATE TABLE public.orcamento_periodo (
    id integer GENERATED ALWAYS AS IDENTITY,
    empresa_junior_id integer NOT NULL,
    nome varchar(25) NOT NULL,
    data_inicio date NOT NULL,
    data_fim date NOT NULL,
    valor_total numeric(9,2) NOT NULL,
    status varchar(15) NOT NULL DEFAULT 'RASCUNHO',
    CONSTRAINT ck_periodo_orcamento CHECK ((data_fim >= data_inicio)),
    CONSTRAINT orcamento_periodo_status_check CHECK (((status)::text = ANY ((ARRAY['RASCUNHO'::character varying, 'ATIVO'::character varying, 'ENCERRADO'::character varying])::text[]))),
    CONSTRAINT orcamento_periodo_valor_total_check CHECK ((valor_total > (0)::numeric))
);

CREATE TABLE public.distribuicao_orcamento (
    orcamento_periodo_id integer NOT NULL,
    categoria_id integer NOT NULL,
    percentual numeric(5,2) NOT NULL,
    CONSTRAINT distribuicao_orcamento_percentual_check CHECK (((percentual > (0)::numeric) AND (percentual <= (100)::numeric)))
);

CREATE TABLE public.subverba (
    id integer GENERATED ALWAYS AS IDENTITY,
    orcamento_periodo_id integer NOT NULL,
    categoria_id integer NOT NULL,
    nome varchar(80) NOT NULL,
    valor_reservado numeric(9,2) NOT NULL,
    CONSTRAINT subverba_nome_check CHECK (btrim(nome) <> ''),
    CONSTRAINT subverba_valor_reservado_check CHECK (valor_reservado > 0)
);

CREATE TABLE public.solicitacao_orcamento (
    id integer GENERATED ALWAYS AS IDENTITY,
    solicitante_id varchar(8) NOT NULL,  -- usuario.matricula
    orcamento_periodo_id  integer,
    categoria_id integer NOT NULL,
    titulo varchar(150) NOT NULL,
    descricao text NOT NULL,
    valor_solicitado numeric(9,2) NOT NULL,
    arquivo_ref text,
    status varchar(20)  NOT NULL DEFAULT 'PENDENTE',
    avaliado_por_id varchar(8), -- usuario.matricula
    justificativa_decisao text,
    data_decisao timestamptz,
    criado_em timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_solicitacao_decidida CHECK ((((status)::text <> ALL ((ARRAY['APROVADO'::character varying, 'REJEITADO'::character varying])::text[])) OR ((avaliado_por_id IS NOT NULL) AND (data_decisao IS NOT NULL)))),
    CONSTRAINT solicitacao_orcamento_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'EM_ANALISE'::character varying, 'APROVADO'::character varying, 'REJEITADO'::character varying])::text[]))),
    CONSTRAINT solicitacao_orcamento_valor_solicitado_check CHECK ((valor_solicitado > (0)::numeric))
);

CREATE TABLE public.projeto (
    id integer GENERATED ALWAYS AS IDENTITY,
    empresa_junior_id integer NOT NULL,
    squad_id integer,
    nome varchar(100) NOT NULL,
    descricao text,
    status varchar(20)  NOT NULL DEFAULT 'ATIVO',
    CONSTRAINT projeto_status_check CHECK (((status)::text = ANY ((ARRAY['ATIVO'::character varying, 'CONCLUIDO'::character varying, 'CANCELADO'::character varying])::text[])))
);

CREATE TABLE public.contrato (
    id integer GENERATED ALWAYS AS IDENTITY,
    projeto_id integer NOT NULL,
    cliente_nome varchar(35) NOT NULL,
    arquivo_ref text,
    valor_total numeric(9,2) NOT NULL,
    data_inicio date NOT NULL,
    data_fim date NOT NULL,
    status varchar(20)  NOT NULL DEFAULT 'RASCUNHO',
    CONSTRAINT ck_periodo_contrato CHECK ((data_fim >= data_inicio)),
    CONSTRAINT contrato_status_check CHECK (((status)::text = ANY ((ARRAY['RASCUNHO'::character varying, 'ATIVO'::character varying, 'ENCERRADO'::character varying, 'RESCINDIDO'::character varying])::text[]))),
    CONSTRAINT contrato_valor_total_check CHECK ((valor_total > (0)::numeric))
);

CREATE TABLE public.prestacao (
    id integer GENERATED ALWAYS AS IDENTITY,
    contrato_id integer NOT NULL,
    numero integer NOT NULL,
    valor numeric(9,2) NOT NULL,
    data_vencimento date NOT NULL,
    status varchar(15)  NOT NULL DEFAULT 'PENDENTE',
    CONSTRAINT prestacao_numero_check CHECK ((numero > 0)),
    CONSTRAINT prestacao_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'PAGA'::character varying, 'CANCELADA'::character varying])::text[]))),
    CONSTRAINT prestacao_valor_check CHECK ((valor > (0)::numeric))
);

CREATE TABLE public.transacao (
    id integer GENERATED ALWAYS AS IDENTITY,
    caixa_id integer NOT NULL,
    registrado_por_id varchar(8), -- usuario.matricula
    projeto_id integer,
    prestacao_id integer,
    categoria_id integer, -- verba
    subverba_id integer,
    descricao varchar(255) NOT NULL,
    tipo varchar(10) NOT NULL,
    categoria_transacao varchar(20) NOT NULL, -- natureza do lançamento
    valor numeric(9,2) NOT NULL,
    data_movimento date NOT NULL,
    criado_em timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_despesa_categorizada CHECK (tipo <> 'DESPESA' OR categoria_id IS NOT NULL),
    CONSTRAINT ck_prestacao_e_receita CHECK (prestacao_id IS NULL OR tipo = 'RECEITA'),
    CONSTRAINT ck_transacao_subverba_despesa CHECK (
        subverba_id IS NULL OR (tipo = 'DESPESA' AND categoria_id IS NOT NULL)
    ),
    CONSTRAINT ck_transacao_categoria_transacao CHECK (
        categoria_transacao IN ('VENDA', 'EMPRESTIMO', 'ALIMENTOS', 'OUTROS')
    ),
    CONSTRAINT transacao_tipo_check CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT transacao_valor_check CHECK (valor > 0)
);

CREATE TABLE public.comprovante (
    id integer GENERATED ALWAYS AS IDENTITY,
    prestacao_id integer,
    transacao_id integer,
    tipo varchar(25)  NOT NULL,
    arquivo_ref text NOT NULL,
    numero_nota_fiscal varchar(60),
    valor numeric(9,2),
    data_documento date,
    criado_em timestamptz  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_documento_vinculo_unico CHECK ((num_nonnulls(prestacao_id, transacao_id) = 1)),
    CONSTRAINT comprovante_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['RECIBO'::character varying, 'NOTA_FISCAL'::character varying, 'COMPROVANTE_PAGAMENTO'::character varying, 'OUTRO'::character varying])::text[]))),
    CONSTRAINT comprovante_valor_check CHECK (((valor IS NULL) OR (valor > (0)::numeric)))
);



ALTER TABLE public.empresa_junior
    ADD CONSTRAINT empresa_junior_pkey PRIMARY KEY (id),
    ADD CONSTRAINT empresa_junior_nome_key UNIQUE (nome);

ALTER TABLE public.squad
    ADD CONSTRAINT squad_pkey PRIMARY KEY (id),
    ADD CONSTRAINT squad_empresa_junior_id_nome_key UNIQUE (empresa_junior_id, nome);

ALTER TABLE public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (matricula),
    ADD CONSTRAINT usuario_id_key UNIQUE (id),
    ADD CONSTRAINT usuario_email_key UNIQUE (email);

ALTER TABLE public.caixa
    ADD CONSTRAINT caixa_pkey PRIMARY KEY (id),
    ADD CONSTRAINT caixa_empresa_junior_id_nome_key UNIQUE (empresa_junior_id, nome);

ALTER TABLE public.categoria_orcamentaria
    ADD CONSTRAINT categoria_orcamentaria_pkey PRIMARY KEY (id),
    ADD CONSTRAINT categoria_orcamentaria_empresa_junior_id_nome_key UNIQUE (empresa_junior_id, nome);

ALTER TABLE public.meta_financeira
    ADD CONSTRAINT meta_financeira_pkey PRIMARY KEY (id);

ALTER TABLE public.orcamento_periodo
    ADD CONSTRAINT orcamento_periodo_pkey PRIMARY KEY (id);

ALTER TABLE public.distribuicao_orcamento
    ADD CONSTRAINT distribuicao_orcamento_pkey PRIMARY KEY (orcamento_periodo_id, categoria_id);

ALTER TABLE public.subverba
    ADD CONSTRAINT subverba_pkey PRIMARY KEY (id),
    ADD CONSTRAINT subverba_orcamento_periodo_id_categoria_id_nome_key UNIQUE (orcamento_periodo_id, categoria_id, nome),
    ADD CONSTRAINT subverba_id_categoria_id_key UNIQUE (id, categoria_id);

ALTER TABLE public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_pkey PRIMARY KEY (id);

ALTER TABLE public.projeto
    ADD CONSTRAINT projeto_pkey PRIMARY KEY (id);

ALTER TABLE public.contrato
    ADD CONSTRAINT contrato_pkey PRIMARY KEY (id);

ALTER TABLE public.prestacao
    ADD CONSTRAINT prestacao_pkey PRIMARY KEY (id),
    ADD CONSTRAINT prestacao_contrato_id_numero_key UNIQUE (contrato_id, numero);

ALTER TABLE public.transacao
    ADD CONSTRAINT transacao_pkey PRIMARY KEY (id);

ALTER TABLE public.comprovante
    ADD CONSTRAINT comprovante_pkey PRIMARY KEY (id);



ALTER TABLE public.squad
    ADD CONSTRAINT squad_empresa_junior_id_fkey
        FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior (id);

ALTER TABLE public.caixa
    ADD CONSTRAINT caixa_empresa_junior_id_fkey
        FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior (id);

ALTER TABLE public.categoria_orcamentaria
    ADD CONSTRAINT categoria_orcamentaria_empresa_junior_id_fkey
        FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior (id);

ALTER TABLE public.meta_financeira
    ADD CONSTRAINT meta_financeira_empresa_junior_id_fkey
        FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior (id);

ALTER TABLE public.orcamento_periodo
    ADD CONSTRAINT orcamento_periodo_empresa_junior_id_fkey
        FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior (id);

ALTER TABLE public.distribuicao_orcamento
    ADD CONSTRAINT distribuicao_orcamento_orcamento_periodo_id_fkey
        FOREIGN KEY (orcamento_periodo_id) REFERENCES public.orcamento_periodo (id),
    ADD CONSTRAINT distribuicao_orcamento_categoria_id_fkey
        FOREIGN KEY (categoria_id) REFERENCES public.categoria_orcamentaria (id);

ALTER TABLE public.subverba
    ADD CONSTRAINT subverba_distribuicao_fkey
        FOREIGN KEY (orcamento_periodo_id, categoria_id)
        REFERENCES public.distribuicao_orcamento (orcamento_periodo_id, categoria_id);

-- Se uma matrícula for corrigida em usuario, as referências acompanham.
ALTER TABLE public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_solicitante_id_fkey
        FOREIGN KEY (solicitante_id) REFERENCES public.usuario (matricula) ON UPDATE CASCADE,
    ADD CONSTRAINT solicitacao_orcamento_avaliado_por_id_fkey
        FOREIGN KEY (avaliado_por_id) REFERENCES public.usuario (matricula) ON UPDATE CASCADE,
    ADD CONSTRAINT solicitacao_orcamento_categoria_id_fkey
        FOREIGN KEY (categoria_id) REFERENCES public.categoria_orcamentaria (id),
    ADD CONSTRAINT solicitacao_orcamento_orcamento_periodo_id_fkey
        FOREIGN KEY (orcamento_periodo_id) REFERENCES public.orcamento_periodo (id);

ALTER TABLE public.projeto
    ADD CONSTRAINT projeto_empresa_junior_id_fkey
        FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior (id),
    ADD CONSTRAINT projeto_squad_id_fkey
        FOREIGN KEY (squad_id) REFERENCES public.squad (id);

ALTER TABLE public.contrato
    ADD CONSTRAINT contrato_projeto_id_fkey
        FOREIGN KEY (projeto_id) REFERENCES public.projeto (id);

ALTER TABLE public.prestacao
    ADD CONSTRAINT prestacao_contrato_id_fkey
        FOREIGN KEY (contrato_id) REFERENCES public.contrato (id);

ALTER TABLE public.transacao
    ADD CONSTRAINT transacao_caixa_id_fkey
        FOREIGN KEY (caixa_id) REFERENCES public.caixa (id),
    ADD CONSTRAINT transacao_registrado_por_id_fkey
        FOREIGN KEY (registrado_por_id) REFERENCES public.usuario (matricula) ON UPDATE CASCADE,
    ADD CONSTRAINT transacao_projeto_id_fkey
        FOREIGN KEY (projeto_id) REFERENCES public.projeto (id),
    ADD CONSTRAINT transacao_prestacao_id_fkey
        FOREIGN KEY (prestacao_id) REFERENCES public.prestacao (id),
    ADD CONSTRAINT transacao_categoria_id_fkey
        FOREIGN KEY (categoria_id) REFERENCES public.categoria_orcamentaria (id),
    ADD CONSTRAINT transacao_subverba_categoria_fkey
        FOREIGN KEY (subverba_id, categoria_id) REFERENCES public.subverba (id, categoria_id);

ALTER TABLE public.comprovante
    ADD CONSTRAINT comprovante_prestacao_id_fkey
        FOREIGN KEY (prestacao_id) REFERENCES public.prestacao (id),
    ADD CONSTRAINT comprovante_transacao_id_fkey
        FOREIGN KEY (transacao_id) REFERENCES public.transacao (id);



CREATE INDEX idx_solicitacao_solicitante ON public.solicitacao_orcamento (solicitante_id);
CREATE INDEX idx_solicitacao_status ON public.solicitacao_orcamento (status);

CREATE INDEX idx_transacao_caixa_data ON public.transacao (caixa_id, data_movimento);
CREATE INDEX idx_transacao_categoria_data ON public.transacao (categoria_id, data_movimento);
CREATE INDEX idx_transacao_prestacao ON public.transacao (prestacao_id);
CREATE INDEX idx_transacao_subverba ON public.transacao (subverba_id)
    WHERE subverba_id IS NOT NULL;

CREATE INDEX idx_contrato_projeto_status ON public.contrato (projeto_id, status);
CREATE INDEX idx_contrato_status ON public.contrato (status);
CREATE INDEX idx_contrato_cliente_nome_lower ON public.contrato (lower(cliente_nome) text_pattern_ops);

CREATE INDEX idx_prestacao_contrato ON public.prestacao (contrato_id);
CREATE INDEX idx_prestacao_vencimento ON public.prestacao (data_vencimento);

CREATE INDEX idx_comprovante_prestacao ON public.comprovante (prestacao_id)
    WHERE prestacao_id IS NOT NULL;
CREATE INDEX idx_comprovante_transacao ON public.comprovante (transacao_id)
    WHERE transacao_id IS NOT NULL;


CREATE VIEW public.vw_saldo_caixa AS
SELECT c.id AS caixa_id,
       c.empresa_junior_id,
       c.nome,
       c.saldo_inicial,
       c.saldo_inicial + COALESCE(sum(
           CASE WHEN t.tipo = 'RECEITA' THEN t.valor ELSE -t.valor END
       ), 0) AS saldo_atual
FROM public.caixa c
LEFT JOIN public.transacao t ON t.caixa_id = c.id
GROUP BY c.id, c.empresa_junior_id, c.nome, c.saldo_inicial;

CREATE VIEW public.vw_consumo_subverba AS
SELECT s.id,
       s.orcamento_periodo_id,
       s.categoria_id,
       s.nome,
       s.valor_reservado,
       COALESCE(sum(t.valor), 0) AS valor_utilizado,
       s.valor_reservado - COALESCE(sum(t.valor), 0) AS saldo_subverba
FROM public.subverba s
LEFT JOIN public.transacao t ON t.subverba_id = s.id
GROUP BY s.id;

CREATE VIEW public.vw_consumo_orcamento AS
SELECT d.orcamento_periodo_id,
       d.categoria_id,
       cat.nome AS categoria,
       d.percentual,
       o.valor_total AS orcamento_base,
       round(o.valor_total * d.percentual / 100, 2) AS limite_categoria,
       COALESCE(sum(t.valor), 0) AS valor_utilizado,
       round(o.valor_total * d.percentual / 100, 2) - COALESCE(sum(t.valor), 0) AS saldo_categoria
FROM public.distribuicao_orcamento d
JOIN public.orcamento_periodo o ON o.id = d.orcamento_periodo_id
JOIN public.categoria_orcamentaria cat ON cat.id = d.categoria_id
LEFT JOIN public.transacao t ON t.categoria_id = d.categoria_id
    AND t.tipo = 'DESPESA'
    AND t.data_movimento BETWEEN o.data_inicio AND o.data_fim
    AND EXISTS (
        SELECT 1
        FROM public.caixa c
        WHERE c.id = t.caixa_id
          AND c.empresa_junior_id = o.empresa_junior_id
    )
    AND (t.subverba_id IS NULL OR EXISTS (
        SELECT 1
        FROM public.subverba s
        WHERE s.id = t.subverba_id
          AND s.orcamento_periodo_id = o.id
    ))
GROUP BY d.orcamento_periodo_id, d.categoria_id, cat.nome, d.percentual, o.valor_total;


CREATE FUNCTION public.validar_reserva_subverba() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    limite numeric;
    reservado numeric;
    id_anterior integer;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        id_anterior := OLD.id;
        IF (NEW.orcamento_periodo_id, NEW.categoria_id) <>
           (OLD.orcamento_periodo_id, OLD.categoria_id) THEN
            RAISE EXCEPTION 'Não é possível mover uma subverba para outra distribuição.'
                USING ERRCODE = '23514';
        END IF;
    END IF;

    UPDATE public.orcamento_periodo
    SET valor_total = valor_total
    WHERE id = NEW.orcamento_periodo_id;

    SELECT round(o.valor_total * d.percentual / 100, 2)
    INTO limite
    FROM public.distribuicao_orcamento d
    JOIN public.orcamento_periodo o ON o.id = d.orcamento_periodo_id
    WHERE d.orcamento_periodo_id = NEW.orcamento_periodo_id
      AND d.categoria_id = NEW.categoria_id;

    SELECT COALESCE(sum(s.valor_reservado), 0)
    INTO reservado
    FROM public.subverba s
    WHERE s.orcamento_periodo_id = NEW.orcamento_periodo_id
      AND s.categoria_id = NEW.categoria_id
      AND s.id IS DISTINCT FROM id_anterior;

    IF reservado + NEW.valor_reservado > limite THEN
        RAISE EXCEPTION 'A soma das reservas ultrapassa a verba principal.'
            USING ERRCODE = 'P5001';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER validar_reserva_subverba
BEFORE INSERT OR UPDATE ON public.subverba
FOR EACH ROW EXECUTE FUNCTION public.validar_reserva_subverba();


CREATE FUNCTION public.validar_percentual_subverbas() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    total numeric;
BEGIN
    UPDATE public.orcamento_periodo
    SET valor_total = valor_total
    WHERE id = NEW.orcamento_periodo_id
    RETURNING valor_total INTO total;

    IF (SELECT COALESCE(sum(s.valor_reservado), 0)
        FROM public.subverba s
        WHERE s.orcamento_periodo_id = NEW.orcamento_periodo_id
          AND s.categoria_id = NEW.categoria_id) > round(total * NEW.percentual / 100, 2) THEN
        RAISE EXCEPTION 'O novo percentual não cobre as subverbas já reservadas.'
            USING ERRCODE = 'P5001';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER validar_percentual_subverbas
BEFORE UPDATE OF percentual ON public.distribuicao_orcamento
FOR EACH ROW WHEN (OLD.percentual IS DISTINCT FROM NEW.percentual)
EXECUTE FUNCTION public.validar_percentual_subverbas();

-- Novo valor total precisa cobrir as subverbas já reservadas.
CREATE FUNCTION public.validar_total_subverbas() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.distribuicao_orcamento d
        JOIN public.subverba s ON s.orcamento_periodo_id = d.orcamento_periodo_id
                              AND s.categoria_id = d.categoria_id
        WHERE d.orcamento_periodo_id = NEW.id
        GROUP BY d.categoria_id, d.percentual
        HAVING sum(s.valor_reservado) > round(NEW.valor_total * d.percentual / 100, 2)
    ) THEN
        RAISE EXCEPTION 'O novo total não cobre as subverbas já reservadas.'
            USING ERRCODE = 'P5001';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER validar_total_subverbas
BEFORE UPDATE OF valor_total ON public.orcamento_periodo
FOR EACH ROW WHEN (OLD.valor_total IS DISTINCT FROM NEW.valor_total)
EXECUTE FUNCTION public.validar_total_subverbas();

COMMIT;
