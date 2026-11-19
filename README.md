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

Gera **um único arquivo**: `target/fisio.jar`, com a webapp
embutida em `/webapp/`. Não há pasta `webapp/`, zip ou script de distribuição.

## Execução

```bash
# Sempre do diretório que contém o jar (ou informe APP_HOME)
java -jar fisio.jar
```

A pasta `data/` (banco H2) fica ao lado do jar e **não é criada
automaticamente** — crie-a na primeira execução ou defina `DB_URL` com a URL
JDBC completa de outro banco:

```bash
mkdir data
java -jar fisio.jar
```

As migrações do Flyway rodam no startup. A pasta é resolvida pela localização
do jar, não pelo diretório corrente, então atalhos, serviços do Windows e
systemd funcionam sem `cd`.

Para começar do zero, pare a aplicação e apague `data/`.

### Rodando a partir do projeto

Durante o desenvolvimento o jar fica em `target/`, mas o banco e o
`application.properties` estão na raiz do projeto. Aponte `APP_HOME` para a
raiz — é dela que saem `<appHome>/data` e os caminhos relativos:

```bash
APP_HOME="$PWD" java -jar target/fisio.jar
```

No Windows, o equivalente é `set APP_HOME=%CD%` antes de `java -jar`.

No startup o banner mostra `Application home`, `Config file` e
`Database URL`; use-o para conferir de onde a aplicação está lendo.

### `-D` não é variável de ambiente

A configuração vem de **variáveis de ambiente** ou do
`application.properties`, **não** de propriedades do sistema. Portanto
`-DDB_URL=...` (antes ou depois de `-jar`) é ignorado:

```bash
# Correto: variável de ambiente
APP_HOME="$PWD" java -jar target/fisio.jar

# Correto: arquivo <appHome>/data/application.properties
# db.url=jdbc:h2:file:/dados/fisio;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
```

`DB_URL` aceita **somente uma URL JDBC** (começando com `jdbc:`). Uma pasta
(ex.: `./data`) não é aceita — para mudar a pasta do banco use `APP_HOME`, ou
informe a URL JDBC completa apontando para o arquivo `fisio`.

O `application.properties` é procurado em `<appHome>/data`. Com o jar em
`target/` e sem `APP_HOME`, ele não é encontrado (e o TLS configurado nele
também não vale).

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

Em vez de variáveis de ambiente, use
`<appHome>/data/application.properties` (a mesma pasta do banco, ignorada pelo
git — segredos e certificado não vão para o repositório). A precedência é
**ambiente > arquivo > padrões do jar**:

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

O certificado fica **fora** do jar, em `config/fisio.p12`, gerado uma vez por
instalação — nunca reaproveite o mesmo arquivo entre clínicas. Com
`TLS_KEYSTORE` definido, o Jetty sobe **HTTPS na mesma `PORT`** (sem segunda
porta e sem redirecionamento):

```bash
mkdir -p config
keytool -genkeypair -alias fisio -keyalg RSA -keysize 2048 -validity 3650 \
  -storetype PKCS12 -keystore config/fisio.p12 -storepass "SENHA" \
  -dname "CN=fisio.local" -ext "SAN=dns:fisio.local,ip:192.168.0.10"

TLS_KEYSTORE=config/fisio.p12 TLS_PASSWORD="SENHA" \
  java -jar fisio.jar
```

Acesse `https://fisio.local:8080`. Caminho relativo do keystore é resolvido a
partir da pasta do jar. O nome digitado no navegador precisa constar no `SAN`
do certificado — o Jetty 12 valida SNI e responde `400 Invalid SNI` quando não
consta. Ajuste `CN`/`SAN` para todos os nomes e IPs usados pelas estações e
instale o certificado (exporte o `.p12` como `.cer`) nos navegadores da
clínica. Sem `TLS_KEYSTORE` (ou `tls.keystore` no arquivo), a aplicação sobe
em HTTP.

### Desenvolvimento

Para testar sem certificado de produção, gere um autoassinado em `data/`
(não versionado) e aponte o arquivo de configuração para ele:

```bash
keytool -genkeypair -alias fisio -keyalg RSA -keysize 2048 -validity 3650 \
  -storetype PKCS12 -keystore data/fisio.p12 -storepass "SENHA" \
  -dname "CN=fisio.local" -ext "SAN=dns:fisio.local,dns:localhost,ip:127.0.0.1"
```

```properties
tls.keystore=data/fisio.p12
tls.password=SENHA
```

## Backend de banco

O SQL é compatível com PostgreSQL (sufixo `MODE=PostgreSQL` no H2), mas a
troca não é plug-and-play: é preciso adicionar o driver JDBC e o módulo
`flyway-database-*` correspondente e validar as migrações. MariaDB usa
`MODE=MariaDB` e tem as mesmas exigências.
