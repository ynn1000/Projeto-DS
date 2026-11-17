-- Ajustes aditivos para o módulo de contratos. Esta migration não remove
-- tabelas, colunas ou registros existentes.
-- Aplicar uma única vez após a estrutura que contém contrato, prestacao,
-- transacao e comprovante.

BEGIN;

-- Novos contratos devem iniciar em RASCUNHO, conforme o fluxo aprovado.
-- A alteração do DEFAULT não modifica contratos já cadastrados.
ALTER TABLE public.contrato
    ALTER COLUMN status SET DEFAULT 'RASCUNHO';

-- Filtros frequentes de contratos por projeto e situação.
CREATE INDEX IF NOT EXISTS idx_contrato_projeto_status
    ON public.contrato (projeto_id, status);

CREATE INDEX IF NOT EXISTS idx_contrato_status
    ON public.contrato (status);

-- A FK prestacao.contrato_id é usada para carregar parcelas e verificar
-- se um contrato ainda possui dependências antes da exclusão.
CREATE INDEX IF NOT EXISTS idx_prestacao_contrato
    ON public.prestacao (contrato_id);

-- As duas FKs de comprovante não possuíam índices no schema base.
-- Índices parciais evitam indexar valores NULL, já que exatamente um vínculo
-- deve estar preenchido por causa de ck_documento_vinculo_unico.
CREATE INDEX IF NOT EXISTS idx_comprovante_prestacao
    ON public.comprovante (prestacao_id)
    WHERE prestacao_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_comprovante_transacao
    ON public.comprovante (transacao_id)
    WHERE transacao_id IS NOT NULL;

-- Pesquisa por prefixo de nome, usando lower(cliente_nome) no Repository.
CREATE INDEX IF NOT EXISTS idx_contrato_cliente_nome_lower
    ON public.contrato (lower(cliente_nome) text_pattern_ops);

COMMIT;
