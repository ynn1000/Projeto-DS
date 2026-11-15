-- Aplicar uma vez após v4_categoria_transacao.sql.
CREATE TABLE public.subverba (
    id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    orcamento_periodo_id integer NOT NULL,
    categoria_id integer NOT NULL,
    nome varchar(80) NOT NULL CHECK (btrim(nome) <> ''),
    valor_reservado numeric(9,2) NOT NULL CHECK (valor_reservado > 0),
    FOREIGN KEY (orcamento_periodo_id, categoria_id)
        REFERENCES public.distribuicao_orcamento (orcamento_periodo_id, categoria_id),
    UNIQUE (orcamento_periodo_id, categoria_id, nome),
    UNIQUE (id, categoria_id)
);

ALTER TABLE public.transacao
    ADD COLUMN subverba_id integer,
    ADD CONSTRAINT transacao_subverba_categoria_fkey
        FOREIGN KEY (subverba_id, categoria_id) REFERENCES public.subverba (id, categoria_id),
    ADD CONSTRAINT ck_transacao_subverba_despesa
        CHECK (subverba_id IS NULL OR (tipo = 'DESPESA' AND categoria_id IS NOT NULL));

CREATE INDEX idx_transacao_subverba ON public.transacao (subverba_id)
    WHERE subverba_id IS NOT NULL;

-- Uma única transação alimenta as duas consultas de orçamento e o Caixa.
CREATE VIEW public.vw_consumo_subverba AS
SELECT s.id, s.orcamento_periodo_id, s.categoria_id, s.nome, s.valor_reservado,
       COALESCE(sum(t.valor), 0) AS valor_utilizado,
       s.valor_reservado - COALESCE(sum(t.valor), 0) AS saldo_subverba
FROM public.subverba s
LEFT JOIN public.transacao t ON t.subverba_id = s.id
GROUP BY s.id;

-- Despesas sem subverba seguem o critério de datas do v3. Para uma despesa
-- vinculada, respeite também o período da reserva se dois períodos se sobrepõem.
CREATE OR REPLACE VIEW public.vw_consumo_orcamento AS
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
        SELECT 1 FROM public.caixa c WHERE c.id = t.caixa_id AND c.empresa_junior_id = o.empresa_junior_id
    )
    AND (t.subverba_id IS NULL OR EXISTS (
        SELECT 1 FROM public.subverba s
        WHERE s.id = t.subverba_id AND s.orcamento_periodo_id = o.id
    ))
GROUP BY d.orcamento_periodo_id, d.categoria_id, cat.nome, d.percentual, o.valor_total;

-- O orçamento do período serve de ponto de sincronização para mudanças nas
-- reservas, nos percentuais e no valor total. Uma gravação sem alteração de valor
-- também força conflito se houver uma escrita concorrente em REPEATABLE READ.
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
