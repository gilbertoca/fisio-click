package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.service.ConectadosService;

/**
 * Encerra a sessão (logout) e volta para a tela de entrada. O menu do
 * BorderPage aponta para logout.htm quando há usuário autenticado.
 */
public class LogoutPage extends BorderPage {

    @Override
    public void onInit() {
        super.onInit();
        // Fora do registro de conectados ANTES da invalidação — a sessão
        // ainda tem o id de que o registro precisa como chave.
        ConectadosService.remover(getContext().getSession().getId());
        getContext().getSession().invalidate();
        setRedirect(LoginPage.class);
    }
}
