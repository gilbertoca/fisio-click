# 🏥 Sistema Fisio — Guia Rápido para Tablet Samsung (Android)

Este guia mostra como instalar e usar o sistema **Fisio** no seu tablet (ex: Galaxy Tab S6 Lite) sem precisar de internet no dia a dia.

---

## 📋 Antes de começar
Baixe o arquivo **`fisio.zip`** no tablet (ele ficará salvo na pasta **Downloads**).

---

## 1. Instalação (Feita apenas uma vez)

### Passo 1: Instalar o Termux
⚠️ **Importante:** Não instale pela Play Store (a versão de lá não funciona).

1. Abra o navegador do tablet e acesse: **[f-droid.org](https://f-droid.org)**
2. Pesquise por **Termux**, baixe e instale o aplicativo *(aceite a permissão de fontes desconhecidas se o tablet pedir)*.

### Passo 2: Instalar o Java e configurar o sistema
1. Abra o aplicativo **Termux**.
2. Copie o bloco de comandos abaixo, cole na tela preta do Termux e aperte **Enter**:

```bash
pkg update && pkg upgrade -y && pkg install openjdk-17 unzip -y && termux-setup-storage
```

3. Quando aparecer uma janela pedindo permissão para acessar os arquivos do tablet, toque em **Permitir**.
4. Depois, cole este segundo bloco e aperte **Enter** para extrair o sistema:

```bash
mkdir -p ~/fisio && unzip -o ~/storage/downloads/fisio.zip -d ~/fisio/ && chmod +x ~/fisio/run.sh
```

---

## 2. Como Usar no Dia a Dia

### ▶️ Ligar o Sistema
1. Abra o **Termux** e digite:
   ```bash
   ~/fisio/run.sh
   ```
2. Aguarde aparecer a mensagem de que o servidor iniciou (ex: `Started Server`).
3. **Não feche o Termux** (apenas minimize voltando para a tela inicial).
4. Abra o navegador (**Chrome** ou **Samsung Internet**) e acesse:
   ```text
   https://localhost:8080
   ```
   *(Se aparecer um aviso de segurança sobre certificado, toque em **Avançado** e depois em **Ir para localhost**).*

### ⏹️ Desligar o Sistema (Importante!)
Para não corromper os dados dos pacientes ao fechar:
1. Volte para a tela do **Termux**.
2. Toque no botão **CTRL** (acima do teclado) e depois na letra **C**.
3. Digite `exit` e aperte **Enter**.

---

## 💡 Dicas para Facilitar o Uso
* **Evitar que o tablet feche o sistema sozinho:** Puxe a barra de notificações do topo da tela, toque na notificação do **Termux** e selecione **"Acquire wakelock"**.
* **Atalho rápido:** No navegador, toque nos três pontinhos do menu e escolha **"Adicionar à tela inicial"** para criar um ícone de acesso direto ao sistema.
