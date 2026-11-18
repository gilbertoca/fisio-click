package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.model.Cliente;
import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.model.Profissional;
import com.gilbertoca.fisio.model.Sessao;
import org.junit.Test;
import org.sql2o.Connection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.gilbertoca.fisio.model.enums.PagamentoOrigem;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import com.gilbertoca.fisio.model.enums.SessaoTipo;
import org.junit.AfterClass;

import static org.junit.Assert.*;
import org.junit.BeforeClass;

public class SessaoServiceTest {

    private SessaoService sessaoService;

    // IDs de referência persistidos no cenário base
    private static Integer clienteId;
    private static Integer idProfissional;
    private static Integer idModalidade;
    private static Integer idProcedimentoSala;

    @BeforeClass
    public static void setUp() {
        // Inicializa banco em memória compatível com Postgres
        String jdbcUrl = "jdbc:h2:mem:fisio_sessao_service_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        seedCenarioOperacionalBase();
    }

    /**
     * Alimenta o banco com os Atores e Contextos obrigatórios para as FKs.
     */
    private static void seedCenarioOperacionalBase() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {

            // 1. Cadastra Cliente Ator
            clienteId = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, cliente_situacao) "
                    + "VALUES ('Roberto Miranda', '98765432100', '1988-10-05', '869994455', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            // 2. Cadastra Profissional Ator
            idProfissional = conn.createQuery(
                    "INSERT INTO fisio.profissional (nome, cpf, dt_nascimento, email, telefone, registro_numero, registro_orgao) "
                    + "VALUES ('Dr. Marcos Vinícius', '12345678900', '1980-01-01', 'marcos@fisioclin.com', '868882211', 'CREFITO-999-F', 'CREFITO-3')", true)
                    .executeUpdate().getKey(Integer.class);

            // 3. Cadastra Modalidade Descrição
            idModalidade = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, valor_base) VALUES ('RPG (Reeducação Postural)', 130.00)", true)
                    .executeUpdate().getKey(Integer.class);

            // 4. Conduta do catálogo para o roteiro da sala (teste do Finalizar)
            idProcedimentoSala = conn.createQuery(
                    "INSERT INTO fisio.procedimento (modalidade_id, nome, procedimento_situacao) "
                    + "VALUES (:modalidadeId, 'Conduta da Sala (teste)', 'ATIVO')", true)
                    .addParameter("modalidadeId", idModalidade)
                    .executeUpdate().getKey(Integer.class);

            conn.commit();
        }
    }

    @Test
    public void deveAgendarComSucessoERecuperarGrafoRicoDoBanco() {
        // Instancia o Serviço passando diretamente o Sql2o (Sem a camada Repository)
        sessaoService = new SessaoService();

        // Define um horário fixo livre de oscilações de milissegundos (Hoje às 14:00)
        LocalDateTime horarioDesejado = LocalDate.of(1993, 9, 22).atTime(14, 0);

        // Instancia os modelos ricos de domínio com seus respectivos IDs
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        Profissional profissional = new Profissional();
        profissional.setId(idProfissional);
        Modalidade modalidade = new Modalidade();
        modalidade.setId(idModalidade);

        // Monta o Grafo completo da Sessão
        Sessao sessao = new Sessao();
        sessao.setCliente(cliente);
        sessao.setProfissional(profissional);
        sessao.setModalidade(modalidade);
        sessao.setDataHoraInicio(horarioDesejado);
        sessao.setDataHoraFim(horarioDesejado.plusMinutes(50));
        sessao.setSessaoTipo(SessaoTipo.AVALIACAO);
        sessao.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
        sessao.setSituacao(SessaoSituacao.AGENDADA);
        sessao.setObservacoesRecepcao("Teste de serviço unificado.");

        // 1. Executa a gravação direta pela camada Service
        sessaoService.create(sessao);

        // 2. Recupera a agenda pela listagem (sem filtro) e isola o registro
        //    criado acima pelo horário planejado
        List<Sessao> agenda = sessaoService.listarAgenda(null).stream()
                .filter(s -> horarioDesejado.equals(s.getDataHoraInicio()))
                .toList();
        // 3. Asserts de Integridade
        assertEquals(1, agenda.size());
        Sessao sGravada = agenda.get(0);
        assertEquals(SessaoTipo.AVALIACAO, sGravada.getSessaoTipo());
        assertEquals(horarioDesejado, sGravada.getDataHoraInicio());

        // 4. Prova do Mapeamento de Grafo Rico (Múltiplos Níveis via dot-notation)
        assertNotNull(sGravada.getCliente());
        assertEquals("Roberto Miranda", sGravada.getCliente().getNome());

        assertNotNull(sGravada.getProfissional());
        assertEquals("Dr. Marcos Vinícius", sGravada.getProfissional().getNome());

        assertNotNull(sGravada.getModalidade());
        assertEquals("RPG (Reeducação Postural)", sGravada.getModalidade().getNome());
    }

    @Test
    public void deveBloquearAgendamentoSeHouverConflitoDeHorarioDoTerapeuta() {
        // Instancia o Serviço passando diretamente o Sql2o (Sem a camada Repository)
        sessaoService = new SessaoService();

        LocalDateTime horarioBase = LocalDate.of(1993, 9, 23).atTime(16, 0);

        Cliente c = new Cliente();
        c.setId(clienteId);
        Profissional p = new Profissional();
        p.setId(idProfissional);
        Modalidade m = new Modalidade();
        m.setId(idModalidade);

        // Primeiro agendamento legítimo (16:00 às 16:50)
        Sessao sessao1 = new Sessao();
        sessao1.setCliente(c);
        sessao1.setProfissional(p);
        sessao1.setModalidade(m);
        sessao1.setDataHoraInicio(horarioBase);
        sessao1.setDataHoraFim(horarioBase.plusMinutes(50));
        sessao1.setSessaoTipo(SessaoTipo.TRATAMENTO);
        sessao1.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
        sessao1.setSituacao(SessaoSituacao.AGENDADA);
        sessaoService.create(sessao1);

        // Segundo agendamento em conflito para o mesmo profissional (Interseção às 16:30)
        Sessao sessaoConflitante = new Sessao();
        sessaoConflitante.setCliente(c);
        sessaoConflitante.setProfissional(p);
        sessaoConflitante.setModalidade(m);
        sessaoConflitante.setDataHoraInicio(horarioBase.plusMinutes(30));
        sessaoConflitante.setDataHoraFim(horarioBase.plusMinutes(80));
        sessaoConflitante.setSessaoTipo(SessaoTipo.TRATAMENTO);
        sessaoConflitante.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
        sessaoConflitante.setSituacao(SessaoSituacao.AGENDADA);

        try {
            // A camada service deve interceptar o conflito ativamente
            sessaoService.create(sessaoConflitante);
            fail("Deveria ter lançado IllegalStateException devido à colisão de horários.");
        } catch (IllegalStateException ex) {
            assertEquals("O profissional já possui atendimento neste horário.", ex.getMessage());
        }
    }

    @Test
    public void deveRejeitarAgendamentoSeOClienteNaoExistirNoBanco() {
        // Instancia o Serviço passando diretamente o Sql2o (Sem a camada Repository)
        sessaoService = new SessaoService();
        
        Cliente clienteInexistente = new Cliente();
        clienteInexistente.setId(999); // ID fantasma

        Profissional p = new Profissional();
        p.setId(idProfissional);
        Modalidade m = new Modalidade();
        m.setId(idModalidade);

        Sessao sessaoInvalida = new Sessao();
        sessaoInvalida.setCliente(clienteInexistente);
        sessaoInvalida.setProfissional(p);
        sessaoInvalida.setModalidade(m);
        sessaoInvalida.setDataHoraInicio(LocalDateTime.now());
        sessaoInvalida.setDataHoraFim(LocalDateTime.now().plusMinutes(50));
        sessaoInvalida.setSessaoTipo(SessaoTipo.TRATAMENTO);
        sessaoInvalida.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
        sessaoInvalida.setSituacao(SessaoSituacao.AGENDADA);

        try {
            sessaoService.create(sessaoInvalida);
            fail("Deveria ter falhado devido à ausência do cliente no banco.");
        } catch (IllegalArgumentException ex) {
            assertEquals("Operação cancelada: O cliente informado não existe.", ex.getMessage());
        }
    }

    // =========================================================================
    // Fluxo da sala: transições de situação + registro do atendimento (1:1)
    // =========================================================================

    /** Sessão crua no banco (evita depender do id gerado do create via modelo). */
    private Integer novaSessaoDb(String situacao) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, dt_hora_inicio, dt_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao) "
                    + "VALUES (:cli, :prof, :mod, CURRENT_TIMESTAMP + INTERVAL '2' HOUR, CURRENT_TIMESTAMP + INTERVAL '3' HOUR, 'TRATAMENTO', 'PARTICULAR', :situacao)", true)
                    .addParameter("cli", clienteId)
                    .addParameter("prof", idProfissional)
                    .addParameter("mod", idModalidade)
                    .addParameter("situacao", situacao)
                    .executeUpdate().getKey(Integer.class);
        }
    }

    @Test
    public void deveExecutarFluxoDeAtendimentoComRegistroNaPropriaSessao() {
        sessaoService = new SessaoService();

        Integer sessaoId = novaSessaoDb("AGENDADA");

        // 1. Abrir o formulário é navegação: nada persistido, ainda AGENDADA
        //    e sem início real marcado
        assertNull(sessaoService.get(sessaoId).getRegistroDtInicio());

        // 2. A PRIMEIRA GRAVAÇÃO inicia o atendimento: AGENDADA → EM_ANDAMENTO,
        //    marcando o início real na mesma transação
        Sessao rascunho = new Sessao();
        rascunho.setId(sessaoId);
        rascunho.setRegistroRelato("Dor lombar 7/10 ao sentar.");
        Sessao emAndamento = sessaoService.salvarRegistro(rascunho);
        assertEquals(SessaoSituacao.EM_ANDAMENTO, emAndamento.getSituacao());
        assertNotNull(emAndamento.getRegistroDtInicio());
        assertNull(emAndamento.getRegistroConduta());
        assertFalse(sessaoService.get(sessaoId).podeFinalizar());

        // 3. Rascunho sem conduta: finalização bloqueada
        Sessao semConduta = new Sessao();
        semConduta.setId(sessaoId);
        semConduta.setRegistroRelato("Ainda sem conduta.");
        try {
            sessaoService.finalizarAtendimento(semConduta);
            fail("Deveria exigir a conduta registrada do paciente.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("conduta"));
        }
        assertFalse(sessaoService.get(sessaoId).podeFinalizar());

        // 4. Conduta registrada: a gravação seguinte NÃO re-marca o início real
        LocalDateTime inicioReal = sessaoService.get(sessaoId).getRegistroDtInicio();
        Sessao consolidado = new Sessao();
        consolidado.setId(sessaoId);
        consolidado.setRegistroConduta("Alongamento passivo, estabilização segmentar.");
        sessaoService.salvarRegistro(consolidado);
        assertTrue(sessaoService.get(sessaoId).podeFinalizar());

        Sessao finalizacao = new Sessao();
        finalizacao.setId(sessaoId);
        finalizacao.setRegistroConduta("Alongamento passivo, estabilização segmentar.");
        Sessao realizada = sessaoService.finalizarAtendimento(finalizacao);
        assertEquals(SessaoSituacao.REALIZADA, realizada.getSituacao());
        assertNotNull(realizada.getRegistroDtFim());

        // 5. O início real marcado na primeira gravação sobrevive ao ciclo
        assertEquals(inicioReal, sessaoService.get(sessaoId).getRegistroDtInicio());
    }

    /**
     * Regressão: o Finalizar da sala grava o estado ATUAL da tela — textos
     * clínicos digitados e roteiro marcado entram na MESMA transação da
     * transição, mesmo sem um Salvar anterior.
     */
    @Test
    public void deveGravarRegistroDaTelaAoFinalizar() {
        sessaoService = new SessaoService();

        Integer sessaoId = novaSessaoDb("EM_ANDAMENTO");

        Sessao daTela = new Sessao();
        daTela.setId(sessaoId);
        daTela.setRegistroRelato("Digitado direto no atendimento.");
        daTela.setRegistroAvaliacao("Teste positivo à palpação.");
        daTela.setRegistroConduta("Mobilização articular grau II.");
        daTela.setRegistroOrientacoes("Gelo 20 min e retorno em 48h.");
        // Roteiro marcado no picklist: coleção do próprio agregado.
        daTela.incluirProcedimento(idProcedimentoSala);

        Sessao realizada = sessaoService.finalizarAtendimento(daTela);

        assertEquals(SessaoSituacao.REALIZADA, realizada.getSituacao());
        assertEquals("O relato digitado antes do Finalizar não pode se perder.",
                "Digitado direto no atendimento.", realizada.getRegistroRelato());
        assertEquals("Teste positivo à palpação.", realizada.getRegistroAvaliacao());
        assertEquals("Mobilização articular grau II.", realizada.getRegistroConduta());
        assertEquals("Gelo 20 min e retorno em 48h.", realizada.getRegistroOrientacoes());
        assertEquals("O roteiro marcado na tela entra na mesma transação e volta no agregado.",
                1, realizada.getProcedimentos().size());
        assertEquals(idProcedimentoSala, realizada.getProcedimentos().get(0).getProcedimentoId());
        assertNotNull(realizada.getRegistroDtFim());
    }

    /**
     * O roteiro é estado do agregado: cada gravação reflete EXATAMENTE a
     * coleção do modelo — incluir, reduzir e limpar, sempre na transação do
     * registro e sem observação de execução (a execução é do
     * registro_conduta).
     */
    @Test
    public void deveSincronizarRoteiroComOEstadoDoAgregado() {
        sessaoService = new SessaoService();

        Integer sessaoId = novaSessaoDb("EM_ANDAMENTO");
        Integer segundaConduta;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            segundaConduta = conn.createQuery(
                    "INSERT INTO fisio.procedimento (modalidade_id, nome, procedimento_situacao) "
                    + "VALUES (:modalidadeId, 'Conduta Extra (sync)', 'ATIVO')", true)
                    .addParameter("modalidadeId", idModalidade)
                    .executeUpdate().getKey(Integer.class);
        }

        // 2 condutas no modelo → 2 no roteiro gravado
        Sessao comDuas = new Sessao();
        comDuas.setId(sessaoId);
        comDuas.incluirProcedimento(idProcedimentoSala);
        comDuas.incluirProcedimento(segundaConduta);
        Sessao salva = sessaoService.salvarRegistro(comDuas);
        assertEquals(2, salva.getProcedimentos().size());
        assertTrue("Conduta nova entra no roteiro sem observação de execução.",
                salva.getProcedimentos().stream().allMatch(p -> p.getObservacao() == null));

        // 1 conduta no modelo → o roteiro perde a que saiu
        Sessao comUma = new Sessao();
        comUma.setId(sessaoId);
        comUma.incluirProcedimento(segundaConduta);
        Sessao reduzida = sessaoService.salvarRegistro(comUma);
        assertEquals(1, reduzida.getProcedimentos().size());
        assertEquals(segundaConduta, reduzida.getProcedimentos().get(0).getProcedimentoId());

        // Lista vazia no modelo → roteiro limpo
        Sessao semNenhuma = new Sessao();
        semNenhuma.setId(sessaoId);
        assertTrue(sessaoService.salvarRegistro(semNenhuma).getProcedimentos().isEmpty());
    }

    @Test
    public void deveIniciarAtendimentoAoSalvarRegistroDeSessaoConfirmada() {
        sessaoService = new SessaoService();

        Integer sessaoId = novaSessaoDb("CONFIRMADA");

        Sessao registro = new Sessao();
        registro.setId(sessaoId);
        registro.setRegistroConduta("Técnica de liberação miofascial.");

        Sessao emAndamento = sessaoService.salvarRegistro(registro);
        assertEquals(SessaoSituacao.EM_ANDAMENTO, emAndamento.getSituacao());
        assertNotNull(emAndamento.getRegistroDtInicio());
    }

    @Test
    public void deveBloquearRegistroEmSessaoCancelada() {
        sessaoService = new SessaoService();

        Integer sessaoCanceladaId = novaSessaoDb("CANCELADA");

        Sessao registro = new Sessao();
        registro.setId(sessaoCanceladaId);
        registro.setRegistroConduta("Tentativa em sessão terminal.");

        try {
            sessaoService.salvarRegistro(registro);
            fail("Deveria bloquear registro do atendimento em sessão CANCELADA.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("Cancelada"));
        }
    }

    @Test
    public void deveListarUltimosRegistrosParaOCockpitDaHome() {
        sessaoService = new SessaoService();

        // Fila: uma AGENDADA e uma CONFIRMADA (recém-criada) + uma CANCELADA
        Integer agendadaId = novaSessaoDb("AGENDADA");
        Integer confirmadaId = novaSessaoDb("CONFIRMADA");
        Integer canceladaId = novaSessaoDb("CANCELADA");

        List<Sessao> fila = sessaoService.getUltimasNaFila(5);
        boolean agendadaNaFila = false;
        boolean confirmadaPrimeira = !fila.isEmpty() && fila.get(0).getId().equals(confirmadaId);
        boolean canceladaFora = true;
        for (Sessao linha : fila) {
            if (linha.getId().equals(agendadaId)) {
                agendadaNaFila = true;
            }
            if (linha.getId().equals(canceladaId)) {
                canceladaFora = false;
            }
        }
        assertTrue("AGENDADA deveria estar na fila do cockpit.", agendadaNaFila);
        assertTrue("A recém-criada deveria vir primeiro (ORDER BY sessao_id DESC).", confirmadaPrimeira);
        assertTrue("CANCELADA não deveria estar na fila do cockpit.", canceladaFora);

        // Atendimentos: só sessões com registro iniciado (transição marca
        // registro_dt_inicio). O banco é compartilhado entre os testes, então
        // outros atendimentos com registro aparecem — o critério é que TODAS
        // as linhas têm o registro e a nossa está presente.
        Integer emAndamentoId = novaSessaoDb("AGENDADA");
        Sessao inicio = new Sessao();
        inicio.setId(emAndamentoId);
        sessaoService.salvarRegistro(inicio);

        List<Sessao> atendimentos = sessaoService.getUltimosAtendimentos(5);
        boolean soComRegistro = !atendimentos.isEmpty();
        boolean nossoAtendimentoPresente = false;
        for (Sessao linha : atendimentos) {
            // A query recorta por registro_dt_inicio IS NOT NULL; cada linha
            // carregada pelo agregado deve comprovar o início real marcado.
            soComRegistro &= sessaoService.get(linha.getId()).getRegistroDtInicio() != null;
            nossoAtendimentoPresente |= linha.getId().equals(emAndamentoId);
        }
        assertTrue("Cockpit da sala deveria listar apenas atendimentos com registro iniciado.", soComRegistro);
        assertTrue("O atendimento recém-iniciado deveria estar no quadro da sala.", nossoAtendimentoPresente);
    }

    @Test
    public void devePermitirSessoesEmGrupoAteCapacidadeEBloquearExcedenteEOutraModalidade() {
        // Instancia o Serviço passando diretamente o Sql2o (Sem a camada Repository)
        sessaoService = new SessaoService();

        // Modalidade em grupo: pilates com capacidade para 2 pacientes na mesma aula
        Integer pilatesId;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            pilatesId = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, duracao_minutos, valor_base, capacidade_simultanea) "
                    + "VALUES ('Pilates Grupo', 50, 80.00, 2)", true)
                    .executeUpdate().getKey(Integer.class);
        }

        LocalDateTime slot = LocalDate.of(1993, 9, 24).atTime(10, 0);

        // Paciente 1 e 2 no mesmo slot: permitido (capacidade 2)
        Sessao primeiro = novaSessaoGrupo(clienteId, pilatesId, slot);
        sessaoService.create(primeiro);

        Integer clienteGrupo2Id;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            clienteGrupo2Id = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, cliente_situacao) "
                    + "VALUES ('Roberta Grupo', '77777777777', '1991-02-02', '8699001122', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);
        }
        Sessao segundo = novaSessaoGrupo(clienteGrupo2Id, pilatesId, slot);
        sessaoService.create(segundo);

        // Paciente 3 no mesmo slot: grupo cheio
        try {
            Sessao terceiro = novaSessaoGrupo(clienteId, pilatesId, slot);
            sessaoService.create(terceiro);
            fail("Deveria bloquear o terceiro paciente do grupo de capacidade 2.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("Grupo cheio"));
        }

        // Overlap com modalidade diferente (RPG, individual) no mesmo slot: conflito
        try {
            Sessao rpgSobreposto = novaSessaoGrupo(clienteId, idModalidade, slot);
            sessaoService.create(rpgSobreposto);
            fail("Deveria bloquear overlap com outra modalidade.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("outra modalidade"));
        }
    }

    private Sessao novaSessaoGrupo(Integer clienteIdRef, Integer modalidadeIdRef, LocalDateTime inicio) {
        Cliente c = new Cliente();
        c.setId(clienteIdRef);
        Profissional p = new Profissional();
        p.setId(idProfissional);
        Modalidade m = new Modalidade();
        m.setId(modalidadeIdRef);

        Sessao sessao = new Sessao();
        sessao.setCliente(c);
        sessao.setProfissional(p);
        sessao.setModalidade(m);
        sessao.setDataHoraInicio(inicio);
        sessao.setDataHoraFim(inicio.plusMinutes(50));
        sessao.setSessaoTipo(SessaoTipo.TRATAMENTO);
        sessao.setPagamentoOrigem(PagamentoOrigem.PARTICULAR);
        sessao.setSituacao(SessaoSituacao.AGENDADA);
        return sessao;
    }

    @AfterClass
    public static void tearDown() {
        DataSourceManager.shutdown();
    }
}
