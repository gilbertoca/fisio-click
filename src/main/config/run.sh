#!/usr/bin/env bash

# Executa o sistema a partir desta pasta.
# Estrutura esperada:
#   fisio.jar
#   application.properties
#   jdk-17.../bin/java       (ou JAVA_HOME/PATH)
#   data/                    (banco e certificado iniciais, ja vem no pacote)
#
# Para outra porta, descomente a linha abaixo:
# export PORT=8081

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

if [ ! -f "fisio.jar" ]; then
    echo "[ERRO] fisio.jar nao encontrado em \"$SCRIPT_DIR\"."
    read -rp "Pressione Enter para sair..."
    exit 1
fi

JAVA_BIN=""

# 1. Busca por pasta local comecando com jdk*
for d in "$SCRIPT_DIR"/jdk*; do
    if [ -x "$d/bin/java" ]; then
        JAVA_BIN="$d/bin/java"
        break
    fi
done

# 2. Busca no JAVA_HOME
if [ -z "$JAVA_BIN" ] && [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
fi

# 3. Busca no PATH do sistema (ex: Termux / Linux)
if [ -z "$JAVA_BIN" ] && command -v java >/dev/null 2>&1; then
    JAVA_BIN="java"
fi

if [ -z "$JAVA_BIN" ]; then
    echo "[ERRO] Java nao encontrado. Extraia o JDK nesta pasta (jdk-17) ou instale o Java 17 (no Termux: pkg install openjdk-17)."
    read -rp "Pressione Enter para sair..."
    exit 1
fi

# Mantem o padrao UTF-8 e aplica limite de memoria seguro se nao definido
if [ -z "$JAVA_OPTS" ]; then
    JAVA_OPTS="-Dfile.encoding=UTF-8 -Xmx512m"
fi

echo "Iniciando Fisio com: \"$JAVA_BIN\""
"$JAVA_BIN" $JAVA_OPTS -jar "fisio.jar"

read -rp "Servidor encerrado. Pressione Enter para fechar..."