package com.gilbertoca.fisio.page;

/**
 * Encerra a sessão (logout) e volta para a tela de entrada. O menu do
 * BorderPage aponta para logout.htm quando há usuário autenticado.
 */
public class LogoutPage extends BorderPage {

    @Override
    public void onInit() {
        super.onInit();
        getContext().getSession().invalidate();
        setRedirect(LoginPage.class);
    }
}
