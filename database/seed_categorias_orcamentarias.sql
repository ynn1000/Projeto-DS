-- Rodar após cadastrar a empresa. Reexecutar não duplica verbas nem reativa as inativas.
INSERT INTO public.categoria_orcamentaria (empresa_junior_id, nome)
SELECT e.id, verba.nome
FROM public.empresa_junior e
CROSS JOIN (VALUES ('Eventos'), ('Escritório'), ('Servidores')) AS verba(nome)
ON CONFLICT (empresa_junior_id, nome) DO NOTHING;
