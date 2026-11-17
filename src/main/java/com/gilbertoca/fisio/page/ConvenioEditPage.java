package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Convenio;
import com.gilbertoca.fisio.service.ConvenioService;
import org.apache.click.control.*;
import org.apache.click.util.Bindable;

public class ConvenioEditPage extends AuthenticatedPage {

    private static final long serialVersionUID = 1L;

    private final HiddenField idField = new HiddenField("id", Integer.class);
    @Bindable protected Integer id;
    private final Form form = new Form("form");
    private final TextField nome;
    private final TextField cnpj;
    private final TextField endereco;
    private final TextField bairro;
    private final TextField cep;
    private final TextField cidade;
    private final ConvenioService convenioService = new ConvenioService();

    public ConvenioEditPage() {
        form.setNoLayout(true);

        nome = new TextField("nome", "Nome do Convênio:", true);
        // Foco no primeiro campo ao abrir (Field.setFocus renderiza o
        // atributo nativo autofocus — sem JavaScript).
        nome.setFocus(true);
        cnpj = new TextField("cnpj", "CNPJ (Somente Números):", true);
        endereco = new TextField("endereco", "Endereço:", false);
        bairro = new TextField("bairro", "Bairro:", false);
        cep = new TextField("cep", "CEP (Somente Números):", false);
        cidade = new TextField("cidade", "Cidade:", false);

        form.add(idField);
        form.add(nome);
        form.add(cnpj);
        form.add(endereco);
        form.add(bairro);
        form.add(cep);
        form.add(cidade);
        form.add(new Submit("save", "Salvar", this, "onSaveClick"));
        form.add(new Submit("cancel", "Cancelar", this, "onCancelClick"));
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Convênio Edit");
    }

    @Override
    public void onGet() {
        if (id != null) {
            Convenio convenio = convenioService.get(id);
            if (convenio != null) {
                form.copyFrom(convenio);
            }
        }
    }

    public boolean onSaveClick() {
        if (form.isValid()) {
            if (id != null) {
                Convenio convenio = convenioService.get(id);
                if (convenio == null) {
                    form.setError("Convênio informado não existe mais.");
                    return true;
                }
                form.copyTo(convenio);
                convenioService.update(convenio);
            } else {
                Convenio convenio = new Convenio();
                form.copyTo(convenio);
                convenioService.create(convenio);
            }
            setRedirect(ConvenioViewPage.class);
            return false;
        }
        return true;
    }

    public boolean onCancelClick() {
        setRedirect(ConvenioViewPage.class);
        return false;
    }
}
