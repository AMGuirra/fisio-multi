#!/usr/bin/env bash
set -e

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD="$(command -v java || true)"
fi

if [ -z "$JAVA_CMD" ]; then
  echo "Erro: Java nao encontrado nem em JAVA_HOME nem no PATH."
  exit 1
fi

echo "Java encontrado em: $JAVA_CMD"
"$JAVA_CMD" -version

JAR_FILE=$(find target -maxdepth 1 -type f -name "*.jar" ! -name "original-*.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
  echo "Erro: nenhum .jar encontrado em target/"
  exit 1
fi

echo "Iniciando aplicacao com jar: $JAR_FILE"
exec "$JAVA_CMD" -Dserver.port="${PORT:-8083}" -jar "$JAR_FILE"
