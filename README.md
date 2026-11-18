# fisioclin-system-click-sql2o-flyway

Sistema de clínica (fisioterapia, pilates e massagem) em Java 17 com Apache
Click, Sql2o, Flyway, H2 e Jetty 12 embarcado.

## Build

```bash
mvn clean package
```

Gera **um único arquivo**: `target/fisio-system-sql2o.jar`, com a webapp
embutida em `/webapp/`. Não há pasta `webapp/`, zip ou script de distribuição.

## Execução

```bash
# Sempre do diretório que contém o jar (ou informe APP_HOME)
java -jar fisio-system-sql2o.jar
```

A pasta `data/` (banco H2) fica ao lado do jar e **não é criada
automaticamente** — crie-a na primeira execução ou defina `DB_URL` apontando
para outro banco/pasta:

```bash
mkdir data
java -jar fisio-system-sql2o.jar
```

As migrações do Flyway rodam no startup. A pasta é resolvida pela localização
do jar, não pelo diretório corrente, então atalhos, serviços do Windows e
systemd funcionam sem `cd`.

Para começar do zero, pare a aplicação e apague `data/`.

## Configuração

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `APP_HOME` | pasta do jar | base de `data/` e dos caminhos relativos |
| `PORT` | `8080` | porta HTTP (ou HTTPS, quando `TLS_KEYSTORE` está definido) |
| `DB_URL` | `jdbc:h2:file:<appHome>/data/fisio;...` | URL JDBC (dispensa a pasta `data/`) |
| `DB_USER` / `DB_PASS` | `sa` / `sa` | credenciais (troque em produção) |
| `DB_DRIVER` | `org.h2.Driver` | driver JDBC |
| `TLS_KEYSTORE` | — | keystore PKCS12; definido = HTTPS |
| `TLS_PASSWORD` | — | senha do keystore |

`PORT`, `DB_URL`, `DB_USER`, `DB_PASS`, `DB_DRIVER`, `TLS_KEYSTORE` e
`TLS_PASSWORD` equivalem a `server.port`, `db.url`, `db.user`, `db.password`,
`db.driver`, `tls.keystore` e `tls.password` no arquivo abaixo.

## Configuração em arquivo

Em vez de variáveis de ambiente, use `data/application.properties` (a mesma
pasta do banco, ignorada pelo git — segredos e certificado não vão para o
repositório). A precedência é **ambiente > arquivo > padrões do jar**:

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
  java -jar fisio-system-sql2o.jar
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
