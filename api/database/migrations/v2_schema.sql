--
-- PostgreSQL database dump
--

\restrict 4ayJe2ZKsSgk3EdMJBlxlWasy1k9C7Li3l9huGu2R7GD8uflCsKfmC8h45RB1cT

-- Dumped from database version 18.1
-- Dumped by pg_dump version 18.1

-- Started on 2026-11-07 14:44:46

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 228 (class 1259 OID 16706)
-- Name: caixa; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.caixa (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    nome character varying(50) NOT NULL,
    saldo_inicial numeric(9,2) DEFAULT 0 NOT NULL,
    limite_despesas numeric(9,2),
    CONSTRAINT ck_limite_caixa CHECK (((limite_despesas IS NULL) OR (limite_despesas >= (0)::numeric)))
);


ALTER TABLE public.caixa OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 16705)
-- Name: caixa_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.caixa ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.caixa_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 232 (class 1259 OID 16744)
-- Name: categoria_orcamentaria; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.categoria_orcamentaria (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    nome character varying(15) NOT NULL,
    descricao text,
    ativa boolean DEFAULT true NOT NULL
);


ALTER TABLE public.categoria_orcamentaria OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 16743)
-- Name: categoria_orcamentaria_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.categoria_orcamentaria ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.categoria_orcamentaria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 245 (class 1259 OID 16919)
-- Name: comprovante; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.comprovante OWNER TO postgres;

--
-- TOC entry 244 (class 1259 OID 16918)
-- Name: comprovante_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.comprovante ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.comprovante_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 239 (class 1259 OID 16829)
-- Name: contrato; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.contrato OWNER TO postgres;

--
-- TOC entry 238 (class 1259 OID 16828)
-- Name: contrato_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.contrato ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.contrato_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 235 (class 1259 OID 16785)
-- Name: distribuicao_orcamento; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.distribuicao_orcamento (
    orcamento_periodo_id integer NOT NULL,
    categoria_id integer NOT NULL,
    percentual numeric(5,2) NOT NULL,
    CONSTRAINT distribuicao_orcamento_percentual_check CHECK (((percentual > (0)::numeric) AND (percentual <= (100)::numeric)))
);


ALTER TABLE public.distribuicao_orcamento OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 16626)
-- Name: empresa_junior; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.empresa_junior (
    id integer NOT NULL,
    nome character varying(50) NOT NULL
);


ALTER TABLE public.empresa_junior OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 16625)
-- Name: empresa_junior_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.empresa_junior ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.empresa_junior_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 224 (class 1259 OID 16652)
-- Name: funcionario; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.funcionario OWNER TO postgres;

--
-- TOC entry 223 (class 1259 OID 16651)
-- Name: funcionario_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.funcionario ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.funcionario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 230 (class 1259 OID 16725)
-- Name: meta_financeira; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.meta_financeira OWNER TO postgres;

--
-- TOC entry 229 (class 1259 OID 16724)
-- Name: meta_financeira_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.meta_financeira ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.meta_financeira_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 234 (class 1259 OID 16764)
-- Name: orcamento_periodo; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.orcamento_periodo OWNER TO postgres;

--
-- TOC entry 233 (class 1259 OID 16763)
-- Name: orcamento_periodo_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.orcamento_periodo ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.orcamento_periodo_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 241 (class 1259 OID 16853)
-- Name: prestacao; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.prestacao OWNER TO postgres;

--
-- TOC entry 240 (class 1259 OID 16852)
-- Name: prestacao_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.prestacao ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.prestacao_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 237 (class 1259 OID 16805)
-- Name: projeto; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.projeto (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    squad_id integer,
    nome character varying(100) NOT NULL,
    descricao text,
    status character varying(20) DEFAULT 'ATIVO'::character varying NOT NULL,
    CONSTRAINT projeto_status_check CHECK (((status)::text = ANY ((ARRAY['ATIVO'::character varying, 'CONCLUIDO'::character varying, 'CANCELADO'::character varying])::text[])))
);


ALTER TABLE public.projeto OWNER TO postgres;

--
-- TOC entry 236 (class 1259 OID 16804)
-- Name: projeto_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.projeto ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.projeto_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 247 (class 1259 OID 16945)
-- Name: solicitacao_orcamento; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.solicitacao_orcamento OWNER TO postgres;

--
-- TOC entry 246 (class 1259 OID 16944)
-- Name: solicitacao_orcamento_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.solicitacao_orcamento ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.solicitacao_orcamento_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 222 (class 1259 OID 16636)
-- Name: squad; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.squad (
    id integer NOT NULL,
    empresa_junior_id integer NOT NULL,
    nome character varying(10) NOT NULL
);


ALTER TABLE public.squad OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 16635)
-- Name: squad_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.squad ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.squad_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 243 (class 1259 OID 16876)
-- Name: transacao; Type: TABLE; Schema: public; Owner: postgres
--

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


ALTER TABLE public.transacao OWNER TO postgres;

--
-- TOC entry 242 (class 1259 OID 16875)
-- Name: transacao_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.transacao ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.transacao_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 226 (class 1259 OID 16686)
-- Name: usuario; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.usuario (
    id integer NOT NULL,
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


ALTER TABLE public.usuario OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 16685)
-- Name: usuario_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.usuario ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.usuario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 249 (class 1259 OID 16997)
-- Name: vw_consumo_orcamento; Type: VIEW; Schema: public; Owner: postgres
--

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


ALTER VIEW public.vw_consumo_orcamento OWNER TO postgres;

--
-- TOC entry 248 (class 1259 OID 16992)
-- Name: vw_saldo_caixa; Type: VIEW; Schema: public; Owner: postgres
--

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


ALTER VIEW public.vw_saldo_caixa OWNER TO postgres;

--
-- TOC entry 5211 (class 0 OID 16706)
-- Dependencies: 228
-- Data for Name: caixa; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.caixa (id, empresa_junior_id, nome, saldo_inicial, limite_despesas) FROM stdin;
\.


--
-- TOC entry 5215 (class 0 OID 16744)
-- Dependencies: 232
-- Data for Name: categoria_orcamentaria; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.categoria_orcamentaria (id, empresa_junior_id, nome, descricao, ativa) FROM stdin;
\.


--
-- TOC entry 5228 (class 0 OID 16919)
-- Dependencies: 245
-- Data for Name: comprovante; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.comprovante (id, prestacao_id, transacao_id, tipo, arquivo_ref, numero_nota_fiscal, valor, data_documento, criado_em) FROM stdin;
\.


--
-- TOC entry 5222 (class 0 OID 16829)
-- Dependencies: 239
-- Data for Name: contrato; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.contrato (id, projeto_id, cliente_nome, arquivo_ref, valor_total, data_inicio, data_fim, status) FROM stdin;
\.


--
-- TOC entry 5218 (class 0 OID 16785)
-- Dependencies: 235
-- Data for Name: distribuicao_orcamento; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.distribuicao_orcamento (orcamento_periodo_id, categoria_id, percentual) FROM stdin;
\.


--
-- TOC entry 5203 (class 0 OID 16626)
-- Dependencies: 220
-- Data for Name: empresa_junior; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.empresa_junior (id, nome) FROM stdin;
\.


--
-- TOC entry 5207 (class 0 OID 16652)
-- Dependencies: 224
-- Data for Name: funcionario; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.funcionario (id, empresa_junior_id, squad_id, nome, email, matricula, curso, semestre_ingresso, papel, diretoria, avaliacao, ativo) FROM stdin;
\.


--
-- TOC entry 5213 (class 0 OID 16725)
-- Dependencies: 230
-- Data for Name: meta_financeira; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.meta_financeira (id, empresa_junior_id, nome, data_inicio, data_fim, valor_meta) FROM stdin;
\.


--
-- TOC entry 5217 (class 0 OID 16764)
-- Dependencies: 234
-- Data for Name: orcamento_periodo; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.orcamento_periodo (id, empresa_junior_id, nome, data_inicio, data_fim, valor_total, status) FROM stdin;
\.


--
-- TOC entry 5224 (class 0 OID 16853)
-- Dependencies: 241
-- Data for Name: prestacao; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.prestacao (id, contrato_id, numero, valor, data_vencimento, status) FROM stdin;
\.


--
-- TOC entry 5220 (class 0 OID 16805)
-- Dependencies: 237
-- Data for Name: projeto; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.projeto (id, empresa_junior_id, squad_id, nome, descricao, status) FROM stdin;
\.


--
-- TOC entry 5230 (class 0 OID 16945)
-- Dependencies: 247
-- Data for Name: solicitacao_orcamento; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.solicitacao_orcamento (id, solicitante_id, orcamento_periodo_id, categoria_id, titulo, descricao, valor_solicitado, arquivo_ref, status, avaliado_por_id, justificativa_decisao, data_decisao, criado_em) FROM stdin;
\.


--
-- TOC entry 5205 (class 0 OID 16636)
-- Dependencies: 222
-- Data for Name: squad; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.squad (id, empresa_junior_id, nome) FROM stdin;
\.


--
-- TOC entry 5226 (class 0 OID 16876)
-- Dependencies: 243
-- Data for Name: transacao; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.transacao (id, caixa_id, registrado_por_id, projeto_id, prestacao_id, categoria_id, descricao, tipo, valor, data_movimento, criado_em) FROM stdin;
\.


--
-- TOC entry 5209 (class 0 OID 16686)
-- Dependencies: 226
-- Data for Name: usuario; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.usuario (id, senha_hash, ativo, criado_em, role, nome, email, matricula, curso, semestre_ingresso) FROM stdin;
1	$2a$12$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTUVWXYZ012	t	2026-11-07 13:27:26.769946-03	USER	Usuario Teste	teste@email.com	20240001	CCOMP	20241
\.


--
-- TOC entry 5236 (class 0 OID 0)
-- Dependencies: 227
-- Name: caixa_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.caixa_id_seq', 1, false);


--
-- TOC entry 5237 (class 0 OID 0)
-- Dependencies: 231
-- Name: categoria_orcamentaria_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.categoria_orcamentaria_id_seq', 1, false);


--
-- TOC entry 5238 (class 0 OID 0)
-- Dependencies: 244
-- Name: comprovante_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.comprovante_id_seq', 1, false);


--
-- TOC entry 5239 (class 0 OID 0)
-- Dependencies: 238
-- Name: contrato_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.contrato_id_seq', 1, false);


--
-- TOC entry 5240 (class 0 OID 0)
-- Dependencies: 219
-- Name: empresa_junior_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.empresa_junior_id_seq', 1, false);


--
-- TOC entry 5241 (class 0 OID 0)
-- Dependencies: 223
-- Name: funcionario_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.funcionario_id_seq', 1, false);


--
-- TOC entry 5242 (class 0 OID 0)
-- Dependencies: 229
-- Name: meta_financeira_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.meta_financeira_id_seq', 1, false);


--
-- TOC entry 5243 (class 0 OID 0)
-- Dependencies: 233
-- Name: orcamento_periodo_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.orcamento_periodo_id_seq', 1, false);


--
-- TOC entry 5244 (class 0 OID 0)
-- Dependencies: 240
-- Name: prestacao_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.prestacao_id_seq', 1, false);


--
-- TOC entry 5245 (class 0 OID 0)
-- Dependencies: 236
-- Name: projeto_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.projeto_id_seq', 1, false);


--
-- TOC entry 5246 (class 0 OID 0)
-- Dependencies: 246
-- Name: solicitacao_orcamento_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.solicitacao_orcamento_id_seq', 1, false);


--
-- TOC entry 5247 (class 0 OID 0)
-- Dependencies: 221
-- Name: squad_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.squad_id_seq', 1, false);


--
-- TOC entry 5248 (class 0 OID 0)
-- Dependencies: 242
-- Name: transacao_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.transacao_id_seq', 1, false);


--
-- TOC entry 5249 (class 0 OID 0)
-- Dependencies: 225
-- Name: usuario_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.usuario_id_seq', 1, true);


--
-- TOC entry 4996 (class 2606 OID 16718)
-- Name: caixa caixa_empresa_junior_id_nome_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.caixa
    ADD CONSTRAINT caixa_empresa_junior_id_nome_key UNIQUE (empresa_junior_id, nome);


--
-- TOC entry 4998 (class 2606 OID 16716)
-- Name: caixa caixa_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.caixa
    ADD CONSTRAINT caixa_pkey PRIMARY KEY (id);


--
-- TOC entry 5002 (class 2606 OID 16757)
-- Name: categoria_orcamentaria categoria_orcamentaria_empresa_junior_id_nome_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categoria_orcamentaria
    ADD CONSTRAINT categoria_orcamentaria_empresa_junior_id_nome_key UNIQUE (empresa_junior_id, nome);


--
-- TOC entry 5004 (class 2606 OID 16755)
-- Name: categoria_orcamentaria categoria_orcamentaria_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categoria_orcamentaria
    ADD CONSTRAINT categoria_orcamentaria_pkey PRIMARY KEY (id);


--
-- TOC entry 5024 (class 2606 OID 16933)
-- Name: comprovante comprovante_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comprovante
    ADD CONSTRAINT comprovante_pkey PRIMARY KEY (id);


--
-- TOC entry 5012 (class 2606 OID 16846)
-- Name: contrato contrato_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato
    ADD CONSTRAINT contrato_pkey PRIMARY KEY (id);


--
-- TOC entry 5008 (class 2606 OID 16793)
-- Name: distribuicao_orcamento distribuicao_orcamento_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.distribuicao_orcamento
    ADD CONSTRAINT distribuicao_orcamento_pkey PRIMARY KEY (orcamento_periodo_id, categoria_id);


--
-- TOC entry 4975 (class 2606 OID 16634)
-- Name: empresa_junior empresa_junior_nome_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.empresa_junior
    ADD CONSTRAINT empresa_junior_nome_key UNIQUE (nome);


--
-- TOC entry 4977 (class 2606 OID 16632)
-- Name: empresa_junior empresa_junior_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.empresa_junior
    ADD CONSTRAINT empresa_junior_pkey PRIMARY KEY (id);


--
-- TOC entry 4983 (class 2606 OID 16672)
-- Name: funcionario funcionario_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_email_key UNIQUE (email);


--
-- TOC entry 4985 (class 2606 OID 16674)
-- Name: funcionario funcionario_matricula_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_matricula_key UNIQUE (matricula);


--
-- TOC entry 4987 (class 2606 OID 16670)
-- Name: funcionario funcionario_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_pkey PRIMARY KEY (id);


--
-- TOC entry 5000 (class 2606 OID 16737)
-- Name: meta_financeira meta_financeira_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meta_financeira
    ADD CONSTRAINT meta_financeira_pkey PRIMARY KEY (id);


--
-- TOC entry 5006 (class 2606 OID 16779)
-- Name: orcamento_periodo orcamento_periodo_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orcamento_periodo
    ADD CONSTRAINT orcamento_periodo_pkey PRIMARY KEY (id);


--
-- TOC entry 5015 (class 2606 OID 16869)
-- Name: prestacao prestacao_contrato_id_numero_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prestacao
    ADD CONSTRAINT prestacao_contrato_id_numero_key UNIQUE (contrato_id, numero);


--
-- TOC entry 5017 (class 2606 OID 16867)
-- Name: prestacao prestacao_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prestacao
    ADD CONSTRAINT prestacao_pkey PRIMARY KEY (id);


--
-- TOC entry 5010 (class 2606 OID 16817)
-- Name: projeto projeto_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.projeto
    ADD CONSTRAINT projeto_pkey PRIMARY KEY (id);


--
-- TOC entry 5028 (class 2606 OID 16964)
-- Name: solicitacao_orcamento solicitacao_orcamento_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_pkey PRIMARY KEY (id);


--
-- TOC entry 4979 (class 2606 OID 16645)
-- Name: squad squad_empresa_junior_id_nome_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.squad
    ADD CONSTRAINT squad_empresa_junior_id_nome_key UNIQUE (empresa_junior_id, nome);


--
-- TOC entry 4981 (class 2606 OID 16643)
-- Name: squad squad_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.squad
    ADD CONSTRAINT squad_pkey PRIMARY KEY (id);


--
-- TOC entry 5022 (class 2606 OID 16892)
-- Name: transacao transacao_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_pkey PRIMARY KEY (id);


--
-- TOC entry 4990 (class 2606 OID 17014)
-- Name: usuario usuario_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_email_key UNIQUE (email);


--
-- TOC entry 4992 (class 2606 OID 17016)
-- Name: usuario usuario_matricula_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_matricula_key UNIQUE (matricula);


--
-- TOC entry 4994 (class 2606 OID 16697)
-- Name: usuario usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuario
    ADD CONSTRAINT usuario_pkey PRIMARY KEY (id);


--
-- TOC entry 4988 (class 1259 OID 16985)
-- Name: idx_funcionario_squad; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_funcionario_squad ON public.funcionario USING btree (squad_id);


--
-- TOC entry 5013 (class 1259 OID 16986)
-- Name: idx_prestacao_vencimento; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_prestacao_vencimento ON public.prestacao USING btree (data_vencimento);


--
-- TOC entry 5025 (class 1259 OID 16991)
-- Name: idx_solicitacao_solicitante; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_solicitacao_solicitante ON public.solicitacao_orcamento USING btree (solicitante_id);


--
-- TOC entry 5026 (class 1259 OID 16990)
-- Name: idx_solicitacao_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_solicitacao_status ON public.solicitacao_orcamento USING btree (status);


--
-- TOC entry 5018 (class 1259 OID 16987)
-- Name: idx_transacao_caixa_data; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_transacao_caixa_data ON public.transacao USING btree (caixa_id, data_movimento);


--
-- TOC entry 5019 (class 1259 OID 16988)
-- Name: idx_transacao_categoria_data; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_transacao_categoria_data ON public.transacao USING btree (categoria_id, data_movimento);


--
-- TOC entry 5020 (class 1259 OID 16989)
-- Name: idx_transacao_prestacao; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_transacao_prestacao ON public.transacao USING btree (prestacao_id);


--
-- TOC entry 5032 (class 2606 OID 16719)
-- Name: caixa caixa_empresa_junior_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.caixa
    ADD CONSTRAINT caixa_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);


--
-- TOC entry 5034 (class 2606 OID 16758)
-- Name: categoria_orcamentaria categoria_orcamentaria_empresa_junior_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.categoria_orcamentaria
    ADD CONSTRAINT categoria_orcamentaria_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);


--
-- TOC entry 5047 (class 2606 OID 16934)
-- Name: comprovante comprovante_prestacao_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comprovante
    ADD CONSTRAINT comprovante_prestacao_id_fkey FOREIGN KEY (prestacao_id) REFERENCES public.prestacao(id);


--
-- TOC entry 5048 (class 2606 OID 16939)
-- Name: comprovante comprovante_transacao_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.comprovante
    ADD CONSTRAINT comprovante_transacao_id_fkey FOREIGN KEY (transacao_id) REFERENCES public.transacao(id);


--
-- TOC entry 5040 (class 2606 OID 16847)
-- Name: contrato contrato_projeto_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.contrato
    ADD CONSTRAINT contrato_projeto_id_fkey FOREIGN KEY (projeto_id) REFERENCES public.projeto(id);


--
-- TOC entry 5036 (class 2606 OID 16799)
-- Name: distribuicao_orcamento distribuicao_orcamento_categoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.distribuicao_orcamento
    ADD CONSTRAINT distribuicao_orcamento_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categoria_orcamentaria(id);


--
-- TOC entry 5037 (class 2606 OID 16794)
-- Name: distribuicao_orcamento distribuicao_orcamento_orcamento_periodo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.distribuicao_orcamento
    ADD CONSTRAINT distribuicao_orcamento_orcamento_periodo_id_fkey FOREIGN KEY (orcamento_periodo_id) REFERENCES public.orcamento_periodo(id);


--
-- TOC entry 5030 (class 2606 OID 16675)
-- Name: funcionario funcionario_empresa_junior_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);


--
-- TOC entry 5031 (class 2606 OID 16680)
-- Name: funcionario funcionario_squad_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.funcionario
    ADD CONSTRAINT funcionario_squad_id_fkey FOREIGN KEY (squad_id) REFERENCES public.squad(id);


--
-- TOC entry 5033 (class 2606 OID 16738)
-- Name: meta_financeira meta_financeira_empresa_junior_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.meta_financeira
    ADD CONSTRAINT meta_financeira_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);


--
-- TOC entry 5035 (class 2606 OID 16780)
-- Name: orcamento_periodo orcamento_periodo_empresa_junior_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orcamento_periodo
    ADD CONSTRAINT orcamento_periodo_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);


--
-- TOC entry 5041 (class 2606 OID 16870)
-- Name: prestacao prestacao_contrato_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.prestacao
    ADD CONSTRAINT prestacao_contrato_id_fkey FOREIGN KEY (contrato_id) REFERENCES public.contrato(id);


--
-- TOC entry 5038 (class 2606 OID 16818)
-- Name: projeto projeto_empresa_junior_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.projeto
    ADD CONSTRAINT projeto_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);


--
-- TOC entry 5039 (class 2606 OID 16823)
-- Name: projeto projeto_squad_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.projeto
    ADD CONSTRAINT projeto_squad_id_fkey FOREIGN KEY (squad_id) REFERENCES public.squad(id);


--
-- TOC entry 5049 (class 2606 OID 16980)
-- Name: solicitacao_orcamento solicitacao_orcamento_avaliado_por_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_avaliado_por_id_fkey FOREIGN KEY (avaliado_por_id) REFERENCES public.funcionario(id);


--
-- TOC entry 5050 (class 2606 OID 16975)
-- Name: solicitacao_orcamento solicitacao_orcamento_categoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categoria_orcamentaria(id);


--
-- TOC entry 5051 (class 2606 OID 16970)
-- Name: solicitacao_orcamento solicitacao_orcamento_orcamento_periodo_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_orcamento_periodo_id_fkey FOREIGN KEY (orcamento_periodo_id) REFERENCES public.orcamento_periodo(id);


--
-- TOC entry 5052 (class 2606 OID 16965)
-- Name: solicitacao_orcamento solicitacao_orcamento_solicitante_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.solicitacao_orcamento
    ADD CONSTRAINT solicitacao_orcamento_solicitante_id_fkey FOREIGN KEY (solicitante_id) REFERENCES public.funcionario(id);


--
-- TOC entry 5029 (class 2606 OID 16646)
-- Name: squad squad_empresa_junior_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.squad
    ADD CONSTRAINT squad_empresa_junior_id_fkey FOREIGN KEY (empresa_junior_id) REFERENCES public.empresa_junior(id);


--
-- TOC entry 5042 (class 2606 OID 16893)
-- Name: transacao transacao_caixa_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_caixa_id_fkey FOREIGN KEY (caixa_id) REFERENCES public.caixa(id);


--
-- TOC entry 5043 (class 2606 OID 16913)
-- Name: transacao transacao_categoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categoria_orcamentaria(id);


--
-- TOC entry 5044 (class 2606 OID 16908)
-- Name: transacao transacao_prestacao_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_prestacao_id_fkey FOREIGN KEY (prestacao_id) REFERENCES public.prestacao(id);


--
-- TOC entry 5045 (class 2606 OID 16903)
-- Name: transacao transacao_projeto_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_projeto_id_fkey FOREIGN KEY (projeto_id) REFERENCES public.projeto(id);


--
-- TOC entry 5046 (class 2606 OID 16898)
-- Name: transacao transacao_registrado_por_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transacao
    ADD CONSTRAINT transacao_registrado_por_id_fkey FOREIGN KEY (registrado_por_id) REFERENCES public.funcionario(id);


-- Completed on 2026-11-07 14:44:46

--
-- PostgreSQL database dump complete
--

\unrestrict 4ayJe2ZKsSgk3EdMJBlxlWasy1k9C7Li3l9huGu2R7GD8uflCsKfmC8h45RB1cT

