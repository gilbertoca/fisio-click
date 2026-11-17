-- 1. MODALIDADES (Base do agendamento e do faturamento da sessão)
INSERT INTO fisio.modalidade (modalidade_id, nome, duracao_minutos, valor_base, capacidade_simultanea) VALUES
(1, 'Fisioterapia Ortopédica', 50, 150.00, 1),
(2, 'Pilates Clínico',         50, 110.00, 4),
(3, 'RPG',                     50, 160.00, 1),
(4, 'Consulta Ortopédica',     30, 350.00, 1),
(5, 'Liberação Miofascial',    50, 140.00, 1),
(6, 'Massagem Desportiva',     50, 130.00, 1),
(7, 'Drenagem Linfática',      50, 120.00, 1);

-- 2. PROCEDIMENTOS / CONDUTAS (Técnicas aplicadas e registradas na evolução da sessão)
INSERT INTO fisio.procedimento (procedimento_id, modalidade_id, nome, codigo_tuss, procedimento_situacao) VALUES
(1, 1, 'Eletroterapia (TENS / FES)',                    NULL, 'ATIVO'),
(2, 1, 'Ultrassom Terapêutico',                         NULL, 'ATIVO'),
(3, 1, 'Liberação Miofascial Manual',                   NULL, 'ATIVO'),
(4, 1, 'Cinesioterapia Motora',                         NULL, 'ATIVO'),
(5, 2, 'Exercícios no Reformer',                        NULL, 'ATIVO'),
(6, 2, 'Exercícios no Cadillac',                        NULL, 'ATIVO'),
(7, 2, 'Fortalecimento Solo / Bola',                    NULL, 'ATIVO'),
(8, 3, 'Postura Rã no Chão',                            NULL, 'ATIVO'),
(9, 4, 'Infiltração Articular',                         NULL, 'ATIVO'),
(10, 5, 'Liberação Instrumental (IASTM / Crochetagem)', NULL, 'ATIVO'),
(11, 5, 'Ventosaterapia',                               NULL, 'ATIVO'),
(12, 6, 'Massagem Profunda / Deslizamento',             NULL, 'ATIVO'),
(13, 7, 'Drenagem Linfática Manual (Método Vodder)',    NULL, 'ATIVO'),
(14, NULL, 'Avaliação Fisioterapêutica',                NULL, 'ATIVO');

ALTER TABLE fisio.modalidade   ALTER COLUMN modalidade_id   RESTART WITH 8;
ALTER TABLE fisio.procedimento ALTER COLUMN procedimento_id RESTART WITH 15;
