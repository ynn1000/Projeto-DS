INSERT INTO public.empresa_junior (nome)
SELECT 'Hut8' WHERE NOT EXISTS (SELECT 1 FROM public.empresa_junior);

INSERT INTO USUARIO(nome, email, senha_hash, role, curso, matricula, semestreIngresso)
VALUES('gabriel', 'gabriel@email.com', '$2a$10$OxLzB6imdzchejcNxYxm3eL8dkYtYdV/Tfy0AEUiuf3NswG4kVCtu', 'ADMIN', 'CCOMP', '20260001', '20261')

INSERT INTO public.categoria_orcamentaria (empresa_junior_id, nome)
SELECT e.id, verba.nome
FROM public.empresa_junior e
CROSS JOIN (VALUES ('Eventos'), ('Escritório'), ('Servidores')) AS verba(nome)
ON CONFLICT (empresa_junior_id, nome) DO NOTHING;

INSERT INTO public.caixa (empresa_junior_id, nome, saldo_inicial, limite_despesas)
VALUES (1, 'Caixa Geral', 1000.00, 500.00);

INSERT INTO public.projeto (
    empresa_junior_id,
    nome,
    descricao,
    status
)
SELECT
    e.id,
    p.nome,
    p.descricao,
    'ATIVO'
FROM public.empresa_junior e
CROSS JOIN (
    VALUES
        ('Projeto Alfa', 'Desenvolvimento de sistema web'),
        ('Projeto Beta', 'Desenvolvimento de landing page'),
        ('Projeto Gama', 'Consultoria em tecnologia'),
        ('Projeto Delta', 'Infraestrutura e hospedagem')
) AS p(nome, descricao)
WHERE e.nome = 'Hut8'
AND NOT EXISTS (
    SELECT 1
    FROM public.projeto existente
    WHERE existente.empresa_junior_id = e.id
      AND existente.nome = p.nome
);
