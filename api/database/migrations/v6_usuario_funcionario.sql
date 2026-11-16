-- Liga funcionario a usuario - pode rodar mais de uma vez
ALTER TABLE public.funcionario ADD COLUMN IF NOT EXISTS usuario_id uuid;

ALTER TABLE public.funcionario
    DROP CONSTRAINT IF EXISTS funcionario_usuario_id_key,
    DROP CONSTRAINT IF EXISTS funcionario_usuario_id_fkey,
    ADD CONSTRAINT funcionario_usuario_id_key UNIQUE (usuario_id),
    ADD CONSTRAINT funcionario_usuario_id_fkey
        FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);

-- Empresa júnior (só cria se não existir nenhuma)
INSERT INTO public.empresa_junior (nome)
SELECT 'Hut8' WHERE NOT EXISTS (SELECT 1 FROM public.empresa_junior);

-- Trigger: usuário novo -> liga ao funcionário de mesmo e-mail ou cria um
CREATE OR REPLACE FUNCTION public.vincular_funcionario_ao_usuario() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE public.funcionario
    SET usuario_id = NEW.id
    WHERE id = (SELECT f.id FROM public.funcionario f
                WHERE f.usuario_id IS NULL AND lower(f.email) = lower(NEW.email)
                LIMIT 1);

    IF NOT FOUND THEN
        INSERT INTO public.funcionario
            (empresa_junior_id, nome, email, matricula, curso, semestre_ingresso, papel, usuario_id)
        VALUES
            ((SELECT min(id) FROM public.empresa_junior),
             NEW.nome, NEW.email, NEW.matricula, NEW.curso, NEW.semestre_ingresso, 'DIRETOR', NEW.id);
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS vincular_funcionario_ao_usuario ON public.usuario;
CREATE TRIGGER vincular_funcionario_ao_usuario
AFTER INSERT ON public.usuario
FOR EACH ROW EXECUTE FUNCTION public.vincular_funcionario_ao_usuario();

-- Funcionário para os usuários que já existiam sem vínculo
UPDATE public.funcionario f SET usuario_id = u.id
FROM public.usuario u
WHERE f.usuario_id IS NULL AND lower(f.email) = lower(u.email);

INSERT INTO public.funcionario
    (empresa_junior_id, nome, email, matricula, curso, semestre_ingresso, papel, usuario_id)
SELECT (SELECT min(id) FROM public.empresa_junior),
       u.nome, u.email, u.matricula, u.curso, u.semestre_ingresso, 'DIRETOR', u.id
FROM public.usuario u
WHERE NOT EXISTS (SELECT 1 FROM public.funcionario f WHERE f.usuario_id = u.id);