INSERT INTO public.caixa (
    empresa_junior_id,
    nome,
    saldo_inicial,
    limite_despesas
)
SELECT
    id,
    'Caixa Geral',
    0.00,
    NULL
FROM public.empresa_junior
WHERE nome = 'Hut8'
AND NOT EXISTS (
    SELECT 1
    FROM public.caixa c
    WHERE c.empresa_junior_id = empresa_junior.id
);

SELECT
    matricula,
    nome,
    email,
    role,
    ativo
FROM public.usuario;

SELECT
    id,
    empresa_junior_id,
    nome,
    saldo_inicial,
    limite_despesas
FROM public.caixa;

SELECT *
FROM public.vw_saldo_caixa;

DO $$
DECLARE
    objeto RECORD;
BEGIN
    -- 1. Remover a extensão pgcrypto
    DROP EXTENSION IF EXISTS pgcrypto CASCADE;

    -- 2. Remover todas as tabelas do schema public
    FOR objeto IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
    LOOP
        EXECUTE format(
            'DROP TABLE IF EXISTS public.%I CASCADE',
            objeto.tablename
        );
    END LOOP;

    -- 3. Remover funções e procedures restantes
    FOR objeto IN
        SELECT
            p.proname,
            pg_get_function_identity_arguments(p.oid) AS argumentos
        FROM pg_proc p
        JOIN pg_namespace n ON n.oid = p.pronamespace
        WHERE n.nspname = 'public'
          AND p.prokind IN ('f', 'p')
    LOOP
        EXECUTE format(
            'DROP ROUTINE IF EXISTS public.%I(%s) CASCADE',
            objeto.proname,
            objeto.argumentos
        );
    END LOOP;
END $$;