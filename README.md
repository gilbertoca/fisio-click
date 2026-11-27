# Fisio

Controle de Agenda/Atendimento para fisioterapia.

Sistema de clínica (fisioterapia, pilates e massagem) em Java 17 com Apache
Click, Sql2o, Flyway, H2 e Jetty 12 embarcado.

## Componentes principais

A aplicação depende diretamente destes três componentes — sem eles o sistema
não sobe:

| Componente | Versão | Papel |
| --- | --- | --- |
| **Apache Click** | 2.8.1-SNAPSHOT (fork) | Framework web: páginas, controles (`Table`, `Form`), navegação e templates Velocity. |
| **Sql2o** | 1.9.2 | Acesso a dados: executa o SQL de `queries.properties` e mapeia o resultado nos modelos. |
| **Flyway** | 10.10.0 | Versionamento do schema e das cargas iniciais em `src/main/resources/db/migration`, aplicadas no startup. |

Completam a distribuição o **H2 2.1.214** (banco embarcado, modo PostgreSQL) e
o **Jetty 12.0.37** (HTTP/HTTPS embarcado), ambos empacotados no mesmo jar.

## Build

```bash
mvn clean package
```

Gera a pasta **`shade/`**, pronta para copiar para a clínica:

```
shade/
├── fisio.jar               # webapp embutida em /webapp/
├── run.bat
└── data/
    ├── application.properties
    ├── fisio.mv.db         # banco inicial (schema + catálogo)
    └── fisio.p12           # certificado de desenvolvimento
```

O jar é um arquivo único; o pacote acrescenta o launcher, a configuração e a
base inicial. A origem dessa pasta é `src/main/config/`; `mvn clean` apaga
`target/`, `shade/` e o `data/` de desenvolvimento.

## Execução

```bash
# Na pasta shade/ (ou informe APP_HOME)
java -jar fisio.jar
```

A pasta `data/` (banco H2, configuração e certificado) fica ao lado do jar e
**não é criada automaticamente** — o pacote já a traz; se faltar, a aplicação
para e avisa. Para usar outro banco, defina `DB_URL` com a URL JDBC completa.

As migrações do Flyway rodam no startup. A pasta é resolvida pela localização
do jar, não pelo diretório corrente, então atalhos, serviços do Windows e
systemd funcionam sem `cd`.

Para começar do zero, pare a aplicação e apague `data/fisio.mv.db`; o Flyway
recria o schema e o catálogo na próxima execução.

### Rodando a partir do projeto

O build já deixa o pacote pronto em `shade/` (jar + `data/`):

```bash
java -jar shade/fisio.jar
```

No Windows, `shade\run.bat` faz o mesmo e localiza o JDK na pasta.

### Desenvolvimento (IDE / Maven)

Rodando pela IDE ou por Maven, o `appHome` é a **raiz do projeto**, então o
app usa `data/` na raiz. O build cria essa pasta a partir do seed em
`src/main/config/data` na fase `process-resources`, **sem sobrescrever** o que
já existe; o seed permanece limpo para o pacote. `data/` é ignorado pelo git.

```bash
mvn compile exec:java
```

O `compile` passa pelo `process-resources` (cria o `data/` se faltar) e o
`exec:java` sobe a classe main configurada no pom. Para recriar o banco de
desenvolvimento a partir do seed, rode `mvn clean` (apaga `data/` e `shade/`)
e depois `mvn compile exec:java`. Para desenvolver em HTTP, comente
`tls.keystore`/`tls.password` em `data/application.properties`.

### `-D` não é variável de ambiente

A configuração vem de **variáveis de ambiente** ou do
`application.properties`, **não** de propriedades do sistema. Portanto
`-DDB_URL=...` (antes ou depois de `-jar`) é ignorado:

```bash
# Correto: variável de ambiente
DB_URL="jdbc:h2:file:/dados/fisio;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE" \
  java -jar fisio.jar

# Correto: arquivo <appHome>/data/application.properties
# db.url=jdbc:h2:file:/dados/fisio;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```

`DB_URL` aceita **somente uma URL JDBC** (começando com `jdbc:`). Uma pasta
(ex.: `./data`) não é aceita — para mudar a pasta do banco use `APP_HOME`, ou
informe a URL JDBC completa apontando para o arquivo `fisio`.

## Configuração

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `APP_HOME` | pasta do jar | base de `data/` e dos caminhos relativos |
| `PORT` | `8080` | porta HTTP (ou HTTPS, quando `TLS_KEYSTORE` está definido) |
| `DB_URL` | `jdbc:h2:file:<appHome>/data/fisio;...` | URL JDBC completa (dispensa a pasta `data/`; não aceita caminho de pasta) |
| `DB_USER` / `DB_PASS` | `sa` / `sa` | credenciais (troque em produção) |
| `DB_DRIVER` | `org.h2.Driver` | driver JDBC |
| `TLS_KEYSTORE` | — | keystore PKCS12; definido = HTTPS |
| `TLS_PASSWORD` | — | senha do keystore |

`PORT`, `DB_URL`, `DB_USER`, `DB_PASS`, `DB_DRIVER`, `TLS_KEYSTORE` e
`TLS_PASSWORD` equivalem a `server.port`, `db.url`, `db.user`, `db.password`,
`db.driver`, `tls.keystore` e `tls.password` no arquivo abaixo.

## Configuração em arquivo

Em vez de variáveis de ambiente, edite `data/application.properties` no
pacote (o modelo versionado fica em
`src/main/config/data/application.properties` e é copiado no build). A
precedência é **ambiente > arquivo > padrões do jar**:

```properties
server.port=8080
db.url=jdbc:h2:file:/dados/fisio;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
db.user=fisio
db.password=senha
tls.keystore=data/fisio.p12
tls.password=senha
```

Use permissão `600` (o arquivo contém senha). Sem `db.url`, o banco é H2 em
`<appHome>/data/fisio`, e a pasta `data/` precisa existir. Caminho relativo do
keystore é resolvido a partir da pasta do jar.

## HTTPS opcional

O pacote traz um certificado de **desenvolvimento** em `data/fisio.p12`
(senha `changeit`, SAN `fisio.local`/`localhost`/`127.0.0.1`). Para produção,
gere um por instalação e substitua o arquivo — nunca reaproveite o mesmo
certificado entre clínicas. Com `tls.keystore` (ou `TLS_KEYSTORE`) definido, o
Jetty sobe **HTTPS na mesma `PORT`** (sem segunda porta e sem
redirecionamento):

```bash
keytool -genkeypair -alias fisio -keyalg RSA -keysize 2048 -validity 3650 \
  -storetype PKCS12 -keystore data/fisio.p12 -storepass "SENHA" \
  -dname "CN=fisio.local" -ext "SAN=dns:fisio.local,ip:192.168.0.10"

TLS_KEYSTORE=data/fisio.p12 TLS_PASSWORD="SENHA" \
  java -jar fisio.jar
```

Acesse `https://fisio.local:8080`. Caminho relativo do keystore é resolvido a
partir da pasta do jar. O nome digitado no navegador precisa constar no `SAN`
do certificado — o Jetty 12 valida SNI e responde `400 Invalid SNI` quando não
consta. Ajuste `CN`/`SAN` para todos os nomes e IPs usados pelas estações e
instale o certificado (exporte o `.p12` como `.cer`) nos navegadores da
clínica. Para voltar a HTTP, comente `tls.keystore`/`tls.password` no
`data/application.properties`.

### Desenvolvimento

O `data/fisio.p12` que acompanha o pacote é só para desenvolvimento. Para
gerar outro autoassinado, use o comando acima com
`-ext "SAN=dns:fisio.local,dns:localhost,ip:127.0.0.1"`.

## Backend de banco

O SQL é compatível com PostgreSQL (sufixo `MODE=PostgreSQL` no H2), mas a
troca não é plug-and-play: é preciso adicionar o driver JDBC e o módulo
`flyway-database-*` correspondente e validar as migrações. MariaDB usa
`MODE=MariaDB` e tem as mesmas exigências.
