ALTER TABLE public.transacao
    DROP CONSTRAINT ck_transacao_categoria_transacao,
    ADD CONSTRAINT ck_transacao_categoria_transacao
        CHECK (categoria_transacao IN (
            'VENDA',
            'EMPRESTIMO',
            'ALIMENTOS',
            'EQUIPAMENTO',
            'EVENTOS',
            'OUTROS'
        ));