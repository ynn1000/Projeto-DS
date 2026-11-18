INSERT INTO public.empresa_junior (nome)
SELECT 'Hut8' WHERE NOT EXISTS (SELECT 1 FROM public.empresa_junior);
