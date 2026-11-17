-- Seeds do módulo de segurança. IDs explícitos (PK "BY DEFAULT" aceita);
-- a permissão '*' concede tudo (wildcard resolvido no AuthorizationService).

-- PAPÉIS
INSERT INTO fisio.papel (papel_id, nome, descricao) VALUES (1, 'Administrador', 'Acesso total, incluindo usuários e papéis');
INSERT INTO fisio.papel (papel_id, nome, descricao) VALUES (2, 'Gerente', 'Gestão completa das áreas clínicas');
INSERT INTO fisio.papel (papel_id, nome, descricao) VALUES (3, 'Operador', 'Recepção: agenda, cadastros e atendimentos');
INSERT INTO fisio.papel (papel_id, nome, descricao) VALUES (4, 'Profissional', 'Atendimento clínico e consultas do próprio painel');
INSERT INTO fisio.papel (papel_id, nome, descricao) VALUES (5, 'Fatura Gerente', 'Gestão de faturamento (módulo em construção)');

-- PERMISSÕES (modulo:acao — a ação "excluir" segue o vocabulário do app)
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (1, '*');

-- cliente
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (2, 'cliente:adicionar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (3, 'cliente:editar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (4, 'cliente:excluir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (5, 'cliente:visualizar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (6, 'cliente:imprimir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (7, 'cliente:*');

-- convenio
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (8, 'convenio:adicionar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (9, 'convenio:editar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (10, 'convenio:excluir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (11, 'convenio:visualizar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (12, 'convenio:imprimir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (13, 'convenio:*');

-- modalidade
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (14, 'modalidade:adicionar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (15, 'modalidade:editar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (16, 'modalidade:excluir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (17, 'modalidade:visualizar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (18, 'modalidade:imprimir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (19, 'modalidade:*');

-- procedimento
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (20, 'procedimento:adicionar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (21, 'procedimento:editar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (22, 'procedimento:excluir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (23, 'procedimento:visualizar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (24, 'procedimento:imprimir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (25, 'procedimento:*');

-- profissional
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (26, 'profissional:adicionar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (27, 'profissional:editar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (28, 'profissional:excluir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (29, 'profissional:visualizar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (30, 'profissional:imprimir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (31, 'profissional:*');

-- sessao (a "ação" atender cobre a sala de atendimento/prontuário)
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (32, 'sessao:adicionar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (33, 'sessao:editar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (34, 'sessao:excluir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (35, 'sessao:visualizar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (36, 'sessao:atender');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (37, 'sessao:imprimir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (38, 'sessao:*');

-- usuario
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (39, 'usuario:adicionar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (40, 'usuario:editar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (41, 'usuario:excluir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (42, 'usuario:visualizar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (43, 'usuario:*');

-- faturamento (módulo futuro: tabelas no V2, telas ainda não existem)
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (44, 'faturamento:adicionar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (45, 'faturamento:editar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (46, 'faturamento:excluir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (47, 'faturamento:visualizar');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (48, 'faturamento:imprimir');
INSERT INTO fisio.permissao (permissao_id, nome) VALUES (49, 'faturamento:*');

-- PERMISSÃO↔PAPEL (a matriz nasce enxuta; afina-se na tela de papéis)
-- Administrador: '*' = tudo
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (1, 1);
-- Gerente: domínio * de cada módulo clínico + usuários + faturamento
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (2, 7);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (2, 13);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (2, 19);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (2, 25);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (2, 31);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (2, 38);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (2, 43);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (2, 49);
-- Operador (recepção): inclui tudo, nada de excluir; agenda e cadastros
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 2);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 3);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 5);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 6);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 8);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 9);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 11);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 12);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 14);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 15);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 17);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 18);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 20);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 21);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 23);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 24);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 32);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 33);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 35);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 36);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (3, 37);
-- Profissional: consulta o catálogo e a agenda, atende na sala
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (4, 5);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (4, 23);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (4, 35);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (4, 36);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (4, 33);
-- Fatura Gerente: faturamento + visão dos clientes e convênios
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (5, 49);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (5, 5);
INSERT INTO fisio.permissao_papel (papel_id, permissao_id) VALUES (5, 11);

-- USUÁRIO semente: admin/admin (dev). Senha PBKDF2WithHmacSHA256, 100.000
-- iterações (PasswordHasher no app); trocar a senha na primeira oportunidade.
INSERT INTO fisio.usuario (usuario_id, nome, username, email, senha, salt) VALUES (1, 'Administrador', 'admin', 'admin@fisioclin.com', '7523a5b833bbdf91a0790ec2c795adbb0b955ca15ce13d6c9b6c64f76e3bde2c', '3ec2a35cdcb56e5cf22e0f4a4d19b88b');
INSERT INTO fisio.usuario_papel (usuario_id, papel_id) VALUES (1, 1);
