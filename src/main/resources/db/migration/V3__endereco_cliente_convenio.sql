-- Endereço no cadastro:
--   Cliente: recibo/nota fiscal do pagador e atendimento domiciliar.
--   Convênio: envio de guias, cobrança e notas (faturamento).
--   Profissional NÃO recebe endereço: trabalha na clínica — sem uso
--   operacional visível (entra quando contrato/repasse justificar).
-- Todos nullable: endereço é opcional no cadastro.
-- H2 (modo PostgreSQL) não aceita ADD COLUMN múltiplo no mesmo ALTER:
-- uma cláusula por instrução, como no Postgres clássico.

ALTER TABLE fisio.cliente ADD COLUMN endereco VARCHAR(150);
ALTER TABLE fisio.cliente ADD COLUMN bairro   VARCHAR(60);
ALTER TABLE fisio.cliente ADD COLUMN cep      VARCHAR(10);
ALTER TABLE fisio.cliente ADD COLUMN cidade   VARCHAR(60);

ALTER TABLE fisio.convenio ADD COLUMN endereco VARCHAR(150);
ALTER TABLE fisio.convenio ADD COLUMN bairro   VARCHAR(60);
ALTER TABLE fisio.convenio ADD COLUMN cep      VARCHAR(10);
ALTER TABLE fisio.convenio ADD COLUMN cidade   VARCHAR(60);
