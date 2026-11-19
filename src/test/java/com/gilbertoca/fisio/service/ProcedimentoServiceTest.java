package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.model.Procedimento;
import com.gilbertoca.fisio.model.enums.ProcedimentoSituacao;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sql2o.Connection;

import static org.junit.Assert.*;

public class ProcedimentoServiceTest {

    private ProcedimentoService procedimentoService;

    // IDs de referência persistidos no cenário base
    private static Integer idModalidade;
    private static Integer idSessao;

    @BeforeClass
    public static void setUp() {
        // Inicializa banco em memória compatível com Postgres
        String jdbcUrl = "jdbc:h2:mem:fisio_procedimento_service_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        seedCenarioOperacionalBase();
    }

    @AfterClass
    public static void tearDown() {
        DataSourceManager.shutdown();
    }

    /**
     * Alimenta o banco com modalidade, os atores da sessão e uma sessão
     * agendada — base para o roteiro (sessao_procedimento).
     */
    private static void seedCenarioOperacionalBase() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {

            idModalidade = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, duracao_minutos, valor_base, capacidade_simultanea) "
                    + "VALUES ('Pilates', 50, 120.00, 4)", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idCliente = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, cliente_situacao) "
                    + "VALUES ('Ana Beatriz', '11122233300', '1990-05-12', '86999112233', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idProfissional = conn.createQuery(
                    "INSERT INTO fisio.profissional (nome, cpf, dt_nascimento, email, telefone, registro_numero, registro_orgao) "
                    + "VALUES ('Dra. Helena Costa', '22233344400', '1985-03-20', 'helena@fisioclin.com', '86888445566', 'CREFITO-777-F', 'CREFITO-3')", true)
                    .executeUpdate().getKey(Integer.class);

            idSessao = conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, dt_hora_inicio, dt_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao) "
                    + "VALUES (:clienteId, :profissionalId, :modalidadeId, '2026-01-10 08:00:00', '2026-01-10 08:50:00', 'TRATAMENTO', 'PARTICULAR', 'AGENDADA')", true)
                    .addParameter("clienteId", idCliente)
                    .addParameter("profissionalId", idProfissional)
                    .addParameter("modalidadeId", idModalidade)
                    .executeUpdate().getKey(Integer.class);

            conn.commit();
        }
    }

    @Test
    public void deveCriarERecuperarProcedimentoComGrafo() {
        procedimentoService = new ProcedimentoService();

        Procedimento procedimento = new Procedimento();
        procedimento.setNome("Reformer Teste");
        procedimento.setCodigoTuss("20101234");
        Modalidade modalidade = new Modalidade();
        modalidade.setId(idModalidade);
        procedimento.setModalidade(modalidade);
        procedimentoService.create(procedimento);

        Procedimento recuperado = procedimentoService.getAllLikeNome("Reformer Teste").get(0);
        assertNotNull("Procedimento recém-criado deveria ser recuperável.", recuperado.getId());
        assertEquals("Reformer Teste", recuperado.getNome());
        assertEquals("20101234", recuperado.getCodigoTuss());
        assertEquals(ProcedimentoSituacao.ATIVO, recuperado.getSituacao());
        assertNotNull("Grafo da modalidade deveria vir na mesma ida ao banco.", recuperado.getModalidade());
        assertEquals(idModalidade, recuperado.getModalidade().getId());
    }

    @Test
    public void deveCriarProcedimentoSemModalidade() {
        procedimentoService = new ProcedimentoService();

        Procedimento procedimento = new Procedimento();
        procedimento.setNome("TENS Teste");
        procedimentoService.create(procedimento);

        Procedimento recuperado = procedimentoService.getAllLikeNome("TENS Teste").get(0);
        assertEquals("TENS Teste", recuperado.getNome());
        assertEquals("Procedimento de uso geral fica sem amarra (modalidade opcional).",
                null, recuperado.getModalidade());
    }

    @Test
    public void deveAtualizarProcedimento() {
        procedimentoService = new ProcedimentoService();

        Procedimento procedimento = new Procedimento();
        procedimento.setNome("Liberação Teste");
        procedimentoService.create(procedimento);
        Integer id = procedimentoService.getAllLikeNome("Liberação Teste").get(0).getId();

        Procedimento carregado = procedimentoService.get(id);
        carregado.setCodigoTuss("40102345");
        carregado.setSituacao(ProcedimentoSituacao.INATIVO);
        Modalidade modalidade = new Modalidade();
        modalidade.setId(idModalidade);
        carregado.setModalidade(modalidade);
        procedimentoService.update(carregado);

        Procedimento atualizado = procedimentoService.get(id);
        assertEquals("40102345", atualizado.getCodigoTuss());
        assertEquals(ProcedimentoSituacao.INATIVO, atualizado.getSituacao());
        assertEquals(idModalidade, atualizado.getModalidade().getId());
    }

    @Test
    public void deveExcluirProcedimentoSemUso() {
        procedimentoService = new ProcedimentoService();

        Procedimento procedimento = new Procedimento();
        procedimento.setNome("Cadillac Teste");
        procedimentoService.create(procedimento);
        Integer id = procedimentoService.getAllLikeNome("Cadillac Teste").get(0).getId();

        procedimentoService.delete(id);
        assertNull("Procedimento sem uso deveria ser excluído.", procedimentoService.get(id));
    }

    @Test
    public void deveBloquearExclusaoDeProcedimentoUsadoEmRoteiro() {
        procedimentoService = new ProcedimentoService();

        Procedimento procedimento = new Procedimento();
        procedimento.setNome("Bola Suíça");
        procedimentoService.create(procedimento);
        Integer id = procedimentoService.getAllLikeNome("Bola Suíça").get(0).getId();

        // Roteiro semeado direto: a escrita do roteiro é do agregado Sessao
        // (SessaoService); o catálogo só precisa enxergar o uso.
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(
                    "INSERT INTO fisio.sessao_procedimento (sessao_id, procedimento_id, observacao) "
                    + "VALUES (:sessaoId, :procedimentoId, '3x10, apoio total')")
                    .addParameter("sessaoId", idSessao)
                    .addParameter("procedimentoId", id)
                    .executeUpdate();
        }

        try {
            procedimentoService.delete(id);
            fail("Deveria bloquear exclusão de procedimento usado em roteiro clínico.");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("Inative-o"));
        }
        assertNotNull("Procedimento usado permanece no catálogo.", procedimentoService.get(id));
    }

    @Test
    public void deveSugerirCondutasDaModalidadeEUsoGeral() {
        procedimentoService = new ProcedimentoService();
        ModalidadeService modalidadeService = new ModalidadeService();

        // Uso geral: sem amarra de modalidade
        Procedimento usoGeral = new Procedimento();
        usoGeral.setNome("Fita Elástica");
        procedimentoService.create(usoGeral);

        // Amarrados à modalidade do cenário (Pilates): um ATIVO e um INATIVO
        Procedimento daModalidade = new Procedimento();
        daModalidade.setNome("Cadillac Barra");
        Modalidade modalidade = new Modalidade();
        modalidade.setId(idModalidade);
        daModalidade.setModalidade(modalidade);
        procedimentoService.create(daModalidade);

        Procedimento inativoDaModalidade = new Procedimento();
        inativoDaModalidade.setNome("Pé de Galinha");
        inativoDaModalidade.setModalidade(modalidade);
        inativoDaModalidade.setSituacao(ProcedimentoSituacao.INATIVO);
        procedimentoService.create(inativoDaModalidade);

        // Amarrado a OUTRA modalidade: não deve ser sugerido para Pilates
        Modalidade outra = new Modalidade();
        outra.setNome("Massagem Teste");
        outra.setValorBase(new java.math.BigDecimal("90.00"));
        outra.setCapacidadeSimultanea(1);
        modalidadeService.create(outra);
        Integer outraId = modalidadeService.getAllLikeNome("Massagem Teste").get(0).getId();

        Procedimento daOutra = new Procedimento();
        daOutra.setNome("Pedra Quente");
        Modalidade outraStub = new Modalidade();
        outraStub.setId(outraId);
        daOutra.setModalidade(outraStub);
        procedimentoService.create(daOutra);

        // Sugestões para a modalidade da sessão em atendimento
        List<Procedimento> sugestoes = procedimentoService.getSugestoesPorModalidade(idModalidade);
        assertTrue("Uso geral deve ser sugerido para qualquer modalidade.",
                sugestoes.stream().anyMatch(p -> "Fita Elástica".equals(p.getNome())));
        assertTrue("Procedimento ATIVO da modalidade deve ser sugerido.",
                sugestoes.stream().anyMatch(p -> "Cadillac Barra".equals(p.getNome())));
        assertFalse("Procedimento INATIVO não deve ser sugerido.",
                sugestoes.stream().anyMatch(p -> "Pé de Galinha".equals(p.getNome())));
        assertFalse("Procedimento de OUTRA modalidade não deve ser sugerido.",
                sugestoes.stream().anyMatch(p -> "Pedra Quente".equals(p.getNome())));

        // Uso geral também aparece nas sugestões da outra modalidade
        List<Procedimento> sugestoesOutra = procedimentoService.getSugestoesPorModalidade(outraId);
        assertTrue(sugestoesOutra.stream().anyMatch(p -> "Fita Elástica".equals(p.getNome())));
        assertFalse(sugestoesOutra.stream().anyMatch(p -> "Cadillac Barra".equals(p.getNome())));
    }

    @Test
    public void deveFiltrarPorCodigoEModalidade() {
        procedimentoService = new ProcedimentoService();

        Integer modalidadeA = novaModalidade("Filtro Modalidade A");
        Integer modalidadeB = novaModalidade("Filtro Modalidade B");

        criarProcedimento("Filtro Alfa", "131069999", modalidadeA);
        criarProcedimento("Filtro Beta", "20101234", modalidadeB);

        Integer idAlfa = procedimentoService.getAllLikeNome("Filtro Alfa").get(0).getId();
        Integer idBeta = procedimentoService.getAllLikeNome("Filtro Beta").get(0).getId();

        List<Procedimento> porCodigo = procedimentoService.getAllFiltrado(null, "131069", null);
        assertTrue("Código parcial deve achar o procedimento Alfa.",
                porCodigo.stream().anyMatch(p -> idAlfa.equals(p.getId())));
        assertTrue("Código de outra faixa não deve trazer o Beta.",
                porCodigo.stream().noneMatch(p -> idBeta.equals(p.getId())));

        List<Procedimento> porModalidade = procedimentoService.getAllFiltrado(null, null, "modalidade a");
        assertEquals(1, porModalidade.size());
        assertEquals(idAlfa, porModalidade.get(0).getId());

        List<Procedimento> combinado = procedimentoService.getAllFiltrado("alfa", "131069", "filtro modalidade a");
        assertEquals(1, combinado.size());
        assertEquals(idAlfa, combinado.get(0).getId());

        assertTrue(procedimentoService.getAllFiltrado(null, "000000", null).isEmpty());
    }

    private Integer novaModalidade(String nome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, duracao_minutos, valor_base, capacidade_simultanea) "
                    + "VALUES (:nome, 50, 100.00, 1)", true)
                    .addParameter("nome", nome)
                    .executeUpdate().getKey(Integer.class);
        }
    }

    private void criarProcedimento(String nome, String codigo, Integer modalidadeId) {
        Procedimento p = new Procedimento();
        p.setNome(nome);
        p.setCodigoTuss(codigo);
        Modalidade m = new Modalidade();
        m.setId(modalidadeId);
        p.setModalidade(m);
        procedimentoService.create(p);
    }
}
