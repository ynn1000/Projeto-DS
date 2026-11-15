-- Aplicar uma vez sobre o schema v3, dentro de uma transação.
-- A verba continua em categoria_id. Lançamentos antigos não tinham natureza
-- registrada, então são classificados como OUTROS sem inferir pela descrição.
ALTER TABLE public.transacao
    ADD COLUMN categoria_transacao VARCHAR(20) NOT NULL DEFAULT 'OUTROS';

ALTER TABLE public.transacao
    ALTER COLUMN categoria_transacao DROP DEFAULT,
    ADD CONSTRAINT ck_transacao_categoria_transacao
        CHECK (categoria_transacao IN ('VENDA', 'EMPRESTIMO', 'ALIMENTOS', 'OUTROS'));
