-- Modelo físico do banco (PostgreSQL)
-- Inclui tabelas, identidades, constraints, índices e views.
-- Não inclui dados nem comandos específicos do psql.

-- Tabelas

CREATE TABLE public.caixa (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    nome character varying(50) NOT NULL,
    saldo_inicial numeric(9,2) DEFAULT 0 NOT NULL,
    limite_despesas numeric(9,2),
    CONSTRAINT ck_limite_caixa CHECK (((limite_despesas IS NULL) OR (limite_despesas >= (0)::numeric)))
);

CREATE TABLE public.categoria_orcamentaria (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    nome character varying(15) NOT NULL,
    descricao text,
    ativa boolean DEFAULT true NOT NULL
);

CREATE TABLE public.comprovante (
    id integer NOT NULL,
    prestacao_id integer,
    transacao_id integer,
    tipo character varying(25) NOT NULL,
    arquivo_ref text NOT NULL,
    numero_nota_fiscal character varying(60),
    valor numeric(9,2),
    data_documento date,
    criado_em timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_documento_vinculo_unico CHECK ((num_nonnulls(prestacao_id, transacao_id) = 1)),
    CONSTRAINT comprovante_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['RECIBO'::character varying, 'NOTA_FISCAL'::character varying, 'COMPROVANTE_PAGAMENTO'::character varying, 'OUTRO'::character varying])::text[]))),
    CONSTRAINT comprovante_valor_check CHECK (((valor IS NULL) OR (valor > (0)::numeric)))
);

CREATE TABLE public.contrato (
    id integer NOT NULL,
    projeto_id integer NOT NULL,
    cliente_nome character varying(35) NOT NULL,
    arquivo_ref text,
    valor_total numeric(9,2) NOT NULL,
    data_inicio date NOT NULL,
    data_fim date NOT NULL,
    status character varying(20) DEFAULT 'ATIVO'::character varying NOT NULL,
    CONSTRAINT ck_periodo_contrato CHECK ((data_fim >= data_inicio)),
    CONSTRAINT contrato_status_check CHECK (((status)::text = ANY ((ARRAY['RASCUNHO'::character varying, 'ATIVO'::character varying, 'ENCERRADO'::character varying, 'RESCINDIDO'::character varying])::text[]))),
    CONSTRAINT contrato_valor_total_check CHECK ((valor_total > (0)::numeric))
);

CREATE TABLE public.distribuicao_orcamento (
    orcamento_periodo_id integer NOT NULL,
    categoria_id integer NOT NULL,
    percentual numeric(5,2) NOT NULL,
    CONSTRAINT distribuicao_orcamento_percentual_check CHECK (((percentual > (0)::numeric) AND (percentual <= (100)::numeric)))
);

CREATE TABLE public.empresa_junior (
    id integer NOT NULL,
    nome character varying(50) NOT NULL
);

CREATE TABLE public.funcionario (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    squad_id integer,
    nome character varying(50) NOT NULL,
    email character varying(50) NOT NULL,
    matricula character varying(8) NOT NULL,
    curso character varying(25) NOT NULL,
    semestre_ingresso character varying(5) NOT NULL,
    papel character varying(20) NOT NULL,
    diretoria character varying(10),
    avaliacao text,
    ativo boolean DEFAULT true NOT NULL,
    CONSTRAINT ck_colaborador_tem_squad CHECK ((((papel)::text <> 'COLABORADOR'::text) OR (squad_id IS NOT NULL))),
    CONSTRAINT funcionario_papel_check CHECK (((papel)::text = ANY ((ARRAY['DIRETOR'::character varying, 'COLABORADOR'::character varying])::text[])))
);

CREATE TABLE public.meta_financeira (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    nome character varying(120) NOT NULL,
    data_inicio date NOT NULL,
    data_fim date NOT NULL,
    valor_meta numeric(9,2) NOT NULL,
    CONSTRAINT ck_periodo_meta CHECK ((data_fim >= data_inicio)),
    CONSTRAINT meta_financeira_valor_meta_check CHECK ((valor_meta > (0)::numeric))
);

CREATE TABLE public.orcamento_periodo (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    nome character varying(25) NOT NULL,
    data_inicio date NOT NULL,
    data_fim date NOT NULL,
    valor_total numeric(9,2) NOT NULL,
    status character varying(15) DEFAULT 'RASCUNHO'::character varying NOT NULL,
    CONSTRAINT ck_periodo_orcamento CHECK ((data_fim >= data_inicio)),
    CONSTRAINT orcamento_periodo_status_check CHECK (((status)::text = ANY ((ARRAY['RASCUNHO'::character varying, 'ATIVO'::character varying, 'ENCERRADO'::character varying])::text[]))),
    CONSTRAINT orcamento_periodo_valor_total_check CHECK ((valor_total > (0)::numeric))
);

CREATE TABLE public.prestacao (
    id integer NOT NULL,
    contrato_id integer NOT NULL,
    numero integer NOT NULL,
    valor numeric(9,2) NOT NULL,
    data_vencimento date NOT NULL,
    status character varying(15) DEFAULT 'PENDENTE'::character varying NOT NULL,
    CONSTRAINT prestacao_numero_check CHECK ((numero > 0)),
    CONSTRAINT prestacao_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'PAGA'::character varying, 'CANCELADA'::character varying])::text[]))),
    CONSTRAINT prestacao_valor_check CHECK ((valor > (0)::numeric))
);

CREATE TABLE public.projeto (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    squad_id integer,
    nome character varying(100) NOT NULL,
    descricao text,
    status character varying(20) DEFAULT 'ATIVO'::character varying NOT NULL,
    CONSTRAINT projeto_status_check CHECK (((status)::text = ANY ((ARRAY['ATIVO'::character varying, 'CONCLUIDO'::character varying, 'CANCELADO'::character varying])::text[])))
);

CREATE TABLE public.solicitacao_orcamento (
    id integer NOT NULL,
    solicitante_id integer NOT NULL,
    orcamento_periodo_id integer,
    categoria_id integer NOT NULL,
    titulo character varying(150) NOT NULL,
    descricao text NOT NULL,
    valor_solicitado numeric(9,2) NOT NULL,
    arquivo_ref text,
    status character varying(20) DEFAULT 'PENDENTE'::character varying NOT NULL,
    avaliado_por_id integer,
    justificativa_decisao text,
    data_decisao timestamp with time zone,
    criado_em timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_solicitacao_decidida CHECK ((((status)::text <> ALL ((ARRAY['APROVADO'::character varying, 'REJEITADO'::character varying])::text[])) OR ((avaliado_por_id IS NOT NULL) AND (data_decisao IS NOT NULL)))),
    CONSTRAINT solicitacao_orcamento_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'EM_ANALISE'::character varying, 'APROVADO'::character varying, 'REJEITADO'::character varying])::text[]))),
    CONSTRAINT solicitacao_orcamento_valor_solicitado_check CHECK ((valor_solicitado > (0)::numeric))
);

CREATE TABLE public.squad (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    nome character varying(10) NOT NULL
);

CREATE TABLE public.transacao (
    id integer NOT NULL,
    caixa_id integer NOT NULL,
    registrado_por_id integer,
    projeto_id integer,
    prestacao_id integer,
    categoria_id integer,
    descricao character varying(255) NOT NULL,
    tipo character varying(10) NOT NULL,
    valor numeric(9,2) NOT NULL,
    data_movimento date NOT NULL,
    criado_em timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_despesa_categorizada CHECK ((((tipo)::text <> 'DESPESA'::text) OR (categoria_id IS NOT NULL))),
    CONSTRAINT ck_prestacao_e_receita CHECK (((prestacao_id IS NULL) OR ((tipo)::text = 'RECEITA'::text))),
    CONSTRAINT transacao_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['RECEITA'::character varying, 'DESPESA'::character varying])::text[]))),
    CONSTRAINT transacao_valor_check CHECK ((valor > (0)::numeric))
);

CREATE TABLE public.usuario (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    senha_hash character varying(100) NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    criado_em timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    role character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    nome character varying(50) NOT NULL,
    email character varying(50) NOT NULL,
    matricula character varying(8) NOT NULL,
    curso character varying(25) NOT NULL,
    semestre_ingresso character varying(5) NOT NULL,
    CONSTRAINT ck_usuario_role CHECK (((role)::text = ANY ((ARRAY['USER'::character varying, 'ADMIN'::character varying])::text[])))
);


-- Colunas identity

ALTER TABLE public.caixa ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.caixa_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.categoria_orcamentaria ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.categoria_orcamentaria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.comprovante ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.comprovante_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.contrato ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.contrato_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.empresa_junior ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.empresa_junior_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.funcionario ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.funcionario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.meta_financeira ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.meta_financeira_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.orcamento_periodo ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.orcamento_periodo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.prestacao ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.prestacao_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.projeto ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.projeto_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.solicitacao_orcamento ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.solicitacao_orcamento_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.squad ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.squad_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE public.transacao ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.transacao_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


-- Chaves e constraints

ALTER TABLE ONLY public.caixa
    ADD CONSTRAINT caixa_empresa_junior_id_nome_key UNIQUE (empresa_junior_id, nome);

ALTER TABLE ONLY public.caixa
    ADD CONSTRAINT caixa_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.categoria_orcamentaria
    ADD CONSTRAINT categoria_orcamentaria_empresa_junior_id_nome_key UNIQUE (empresa_junior_id, nome);

ALTER TABLE ONLY public.categoria_orcamentaria
    ADD CONSTRAINT categoria_orcamentaria_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.comprovante
    ADD CONSTRAINT comprovante_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.contrato
    ADD CONSTRAINT contrato_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.distribuicao_orcamento
    ADD CONSTRAINT distribuicao_orcamento_pkey PRIMARY KEY (orcamento_periodo_id, categoria_id);

ALTER TABLE ONLY public.empresa_junior
    ADD CONSTRAINT empresa_junior_nome_key UNIQUE (nome);

ALTER TABLE ONLY public.empresa_junior
    ADD CONSTRAINT empresa_junior_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_email_key UNIQUE (email);

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_matricula_key UNIQUE (matricula);

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.meta_financeira
    ADD CONSTRAINT meta_financeira_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.orcamento_periodo
    ADD CONSTRAINT orcamento_periodo_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.prestacao
    ADD CONSTRAINT prestacao_contrato_id_numero_key UNIQUE (contrato_id, numero);

ALTER TABLE ONLY public.prestacao
    ADD CONSTRAINT prestacao_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.projeto
    ADD CONSTRAINT projeto_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.squad
    ADD CONSTRAINT squad_empresa_junior_id_nome_key UNIQUE (empresa_junior_id, nome);

ALTER TABLE ONLY public.squad
    ADD CONSTRAINT squad_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_email_key UNIQUE (email);

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_matricula_key UNIQUE (matricula);

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.caixa
    ADD CONSTRAINT caixa_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);

ALTER TABLE ONLY public.categoria_orcamentaria
    ADD CONSTRAINT categoria_orcamentaria_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);

ALTER TABLE ONLY public.comprovante
    ADD CONSTRAINT comprovante_prestacao_id_fkey FOREIGN KEY (prestacao_id) REFERENCES public.prestacao(id);

ALTER TABLE ONLY public.comprovante
    ADD CONSTRAINT comprovante_transacao_id_fkey FOREIGN KEY (transacao_id) REFERENCES public.transacao(id);

ALTER TABLE ONLY public.contrato
    ADD CONSTRAINT contrato_projeto_id_fkey FOREIGN KEY (projeto_id) REFERENCES public.projeto(id);

ALTER TABLE ONLY public.distribuicao_orcamento
    ADD CONSTRAINT distribuicao_orcamento_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categoria_orcamentaria(id);

ALTER TABLE ONLY public.distribuicao_orcamento
    ADD CONSTRAINT distribuicao_orcamento_orcamento_periodo_id_fkey FOREIGN KEY (orcamento_periodo_id) REFERENCES public.orcamento_periodo(id);

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_squad_id_fkey FOREIGN KEY (squad_id) REFERENCES public.squad(id);

ALTER TABLE ONLY public.meta_financeira
    ADD CONSTRAINT meta_financeira_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);

ALTER TABLE ONLY public.orcamento_periodo
    ADD CONSTRAINT orcamento_periodo_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);

ALTER TABLE ONLY public.prestacao
    ADD CONSTRAINT prestacao_contrato_id_fkey FOREIGN KEY (contrato_id) REFERENCES public.contrato(id);

ALTER TABLE ONLY public.projeto
    ADD CONSTRAINT projeto_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);

ALTER TABLE ONLY public.projeto
    ADD CONSTRAINT projeto_squad_id_fkey FOREIGN KEY (squad_id) REFERENCES public.squad(id);

ALTER TABLE ONLY public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_avaliado_por_id_fkey FOREIGN KEY (avaliado_por_id) REFERENCES public.funcionario(id);

ALTER TABLE ONLY public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categoria_orcamentaria(id);

ALTER TABLE ONLY public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_orcamento_periodo_id_fkey FOREIGN KEY (orcamento_periodo_id) REFERENCES public.orcamento_periodo(id);

ALTER TABLE ONLY public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_solicitante_id_fkey FOREIGN KEY (solicitante_id) REFERENCES public.funcionario(id);

ALTER TABLE ONLY public.squad
    ADD CONSTRAINT squad_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_caixa_id_fkey FOREIGN KEY (caixa_id) REFERENCES public.caixa(id);

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categoria_orcamentaria(id);

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_prestacao_id_fkey FOREIGN KEY (prestacao_id) REFERENCES public.prestacao(id);

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_projeto_id_fkey FOREIGN KEY (projeto_id) REFERENCES public.projeto(id);

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_registrado_por_id_fkey FOREIGN KEY (registrado_por_id) REFERENCES public.funcionario(id);


-- Índices

CREATE INDEX idx_funcionario_squad ON public.funcionario USING btree (squad_id);

CREATE INDEX idx_prestacao_vencimento ON public.prestacao USING btree (data_vencimento);

CREATE INDEX idx_solicitacao_solicitante ON public.solicitacao_orcamento USING btree (solicitante_id);

CREATE INDEX idx_solicitacao_status ON public.solicitacao_orcamento USING btree (status);

CREATE INDEX idx_transacao_caixa_data ON public.transacao USING btree (caixa_id, data_movimento);

CREATE INDEX idx_transacao_categoria_data ON public.transacao USING btree (categoria_id, data_movimento);

CREATE INDEX idx_transacao_prestacao ON public.transacao USING btree (prestacao_id);


-- Views

CREATE VIEW public.vw_consumo_orcamento AS
 SELECT d.orcamento_periodo_id,
    d.categoria_id,
    cat.nome AS categoria,
    d.percentual,
    o.valor_total AS orcamento_base,
    round(((o.valor_total * d.percentual) / (100)::numeric), 2) AS limite_categoria,
    COALESCE(sum(t.valor), (0)::numeric) AS valor_utilizado,
    (round(((o.valor_total * d.percentual) / (100)::numeric), 2) - COALESCE(sum(t.valor), (0)::numeric)) AS saldo_categoria
   FROM (((public.distribuicao_orcamento d
     JOIN public.orcamento_periodo o ON ((o.id = d.orcamento_periodo_id)))
     JOIN public.categoria_orcamentaria cat ON ((cat.id = d.categoria_id)))
     LEFT JOIN public.transacao t ON (((t.categoria_id = d.categoria_id) AND ((t.tipo)::text = 'DESPESA'::text) AND ((t.data_movimento >= o.data_inicio) AND (t.data_movimento <= o.data_fim)) AND (EXISTS ( SELECT 1
           FROM public.caixa c
          WHERE ((c.id = t.caixa_id) AND (c.empresa_junior_id = o.empresa_junior_id)))))))
  GROUP BY d.orcamento_periodo_id, d.categoria_id, cat.nome, d.percentual, o.valor_total;

CREATE VIEW public.vw_saldo_caixa AS
 SELECT c.id AS caixa_id,
    c.empresa_junior_id,
    c.nome,
    c.saldo_inicial,
    (c.saldo_inicial + COALESCE(sum(
        CASE
            WHEN ((t.tipo)::text = 'RECEITA'::text) THEN t.valor
            ELSE (- t.valor)
        END), (0)::numeric)) AS saldo_atual
   FROM (public.caixa c
     LEFT JOIN public.transacao t ON ((t.caixa_id = c.id)))
  GROUP BY c.id, c.empresa_junior_id, c.nome, c.saldo_inicial;
