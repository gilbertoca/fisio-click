-- Catálogo inicial revisado conforme as normas do COFFITO (RBPF 2025,
-- Res. 404/2011, Res. 380/2010 e Acórdão 735/2024) e do CONFEF
-- (Res. 201/2010 e 643/2026).
--
-- codigo_tuss guarda o código de referência do procedimento na RBPF
-- (série 131069xxx). Fica NULL quando a RBPF não tem item equivalente
-- (PICS e massoterapia, por exemplo). A RBPF cobra por procedimento/sessão:
-- em uma sessão com várias técnicas, use o código do procedimento principal,
-- não a soma dos códigos de cada técnica.

-- 1. MODALIDADES (Base do agendamento e do faturamento da sessão)
INSERT INTO fisio.modalidade (modalidade_id, nome, duracao_minutos, valor_base, capacidade_simultanea) VALUES
(1, 'Fisioterapia Traumato-Ortopédica', 50, 150.00, 1),
(2, 'Pilates Clínico',                  50, 110.00, 4), -- capacidade 4 = turma (organização), prática sempre individualizada
(3, 'Reeducação Postural Global (RPG)',  50, 160.00, 1),
(4, 'Consulta Fisioterapêutica',        30, 350.00, 1),
(5, 'Terapia Manual Ortopédica',        50, 140.00, 1),
(6, 'Massoterapia',                     50, 130.00, 1),
(7, 'Drenagem Linfática Manual',        50, 120.00, 1);

-- 2. PROCEDIMENTOS / CONDUTAS (Técnicas aplicadas e registradas na evolução da sessão)
-- Pilates: a RBPF separa apenas o formato de atendimento (grupo x individual);
-- a prescrição é individualizada nos dois. Turma de até 4 = 131069403.
INSERT INTO fisio.procedimento (procedimento_id, modalidade_id, nome, codigo_tuss, procedimento_situacao) VALUES
(1,  1,    'Eletroterapia (TENS / FES)',                     '131069414', 'ATIVO'),
(2,  1,    'Ultrassom Terapêutico',                          '131069417', 'ATIVO'),
(3,  1,    'Liberação Miofascial Manual',                    '131069413', 'ATIVO'),
(4,  1,    'Cinesioterapia Motora',                          '131069304', 'ATIVO'),
(5,  2,    'Exercícios no Reformer',                         '131069403', 'ATIVO'),
(6,  2,    'Exercícios no Cadillac',                         '131069403', 'ATIVO'),
(7,  2,    'Fortalecimento Solo / Bola',                     '131069403', 'ATIVO'),
(8,  3,    'Postura Rã no Chão',                             '131069409', 'ATIVO'),
(9,  5,    'Liberação Instrumental (IASTM / Crochetagem)',   '131069413', 'ATIVO'),
(10, 5,    'Ventosaterapia',                                 NULL,        'ATIVO'),
(11, 6,    'Massagem Profunda / Deslizamento',               NULL,        'ATIVO'),
(12, 7,    'Drenagem Linfática Manual (Método Vodder)',      '131069319', 'ATIVO'),
(13, NULL, 'Avaliação Fisioterapêutica',                     '131069100', 'ATIVO');

ALTER TABLE fisio.modalidade   ALTER COLUMN modalidade_id   RESTART WITH 8;
ALTER TABLE fisio.procedimento ALTER COLUMN procedimento_id RESTART WITH 14;
