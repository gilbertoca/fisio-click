package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.service.ConectadosService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.click.Context;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.Form;
import org.apache.click.control.Table;

/**
 * Tela "Usuários Conectados" do CredBem: as sessões abertas no momento —
 * Usuário, Papel, Host (IP), Último Acesso e Início da Sessão. O registro
 * vem do ConectadosService (em memória): entra no login, batido a cada
 * página autenticada, sai no logout.
 */
public class UsuarioConectadoViewPage extends AuthenticatedPage {

    private static final long serialVersionUID = 1L;

    /** Formato do CredBem: hora curta + data curta. */
    private static final DateTimeFormatter FORMATO
            = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final Form form = new Form("formTable");
    private final Table table = new Table("table");

    public UsuarioConectadoViewPage() {
        form.setNoLayout(true);
        table.setShowBanner(true);

        addColuna("username", "Usuário");
        addColuna("papel", "Papel");
        addColuna("host", "Host (IP)");
        addColunaData("ultimoAcesso", "Último Acesso");
        addColunaData("inicio", "Início da Sessão");

        table.setDataProvider(new org.apache.click.dataprovider.DataProvider<ConectadosService.Conectado>() {
            @Override
            public Iterable<ConectadosService.Conectado> getData() {
                return ConectadosService.getConectados();
            }
        });

        form.add(table);
        addControl(form);
    }

    private void addColuna(String nome, String titulo) {
        Column coluna = new Column(nome, titulo);
        // Registro Java: acesso por username()/papel() — decoro direto
        // (e nenhuma coluna ordena, o registro é volátil).
        coluna.setSortable(false);
        coluna.setDecorator(new Decorator() {
            @Override
            public String render(Object row, Context context) {
                return String.valueOf(chamar(row, nome));
            }
        });
        table.addColumn(coluna);
    }

    private void addColunaData(String nome, String titulo) {
        Column coluna = new Column(nome, titulo);
        coluna.setSortable(false);
        coluna.setDecorator(new Decorator() {
            @Override
            public String render(Object row, Context context) {
                return FORMATO.format((LocalDateTime) chamar(row, nome));
            }
        });
        table.addColumn(coluna);
    }

    /** Acesso ao accessor do record (username(), inicio(), ...). */
    private Object chamar(Object row, String accessor) {
        try {
            return row.getClass().getMethod(accessor).invoke(row);
        } catch (ReflectiveOperationException ex) {
            return "";
        }
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Usuários Conectados");
    }

    @Override
    protected String[] permissoesExigidas() {
        return new String[] { "usuario:visualizar" };
    }
}
