#!/usr/bin/env bash
# Levanta la API de TeqRuta (Flask + SQLite) con un solo comando.
#
#   ./run.sh            API en el puerto 5000 (crea la base con datos de ejemplo si no existe)
#   ./run.sh --ngrok    además abre un túnel ngrok y muestra la URL pública para la app
#   ./run.sh --reset    borra la base de datos y la vuelve a crear
#
# La primera vez crea un entorno virtual (.venv) e instala Flask.
set -euo pipefail
cd "$(dirname "$0")"

PUERTO="${PORT:-5000}"
USAR_NGROK=0
ARGS_APP=()
for arg in "$@"; do
  case "$arg" in
    --ngrok) USAR_NGROK=1 ;;
    --reset) ARGS_APP+=("--reset") ;;
    -h|--help) sed -n '2,8p' "$0"; exit 0 ;;
    *) echo "Opción desconocida: $arg (usa --help)"; exit 1 ;;
  esac
done

if [ ! -x .venv/bin/python ]; then
  echo ">> Creando entorno virtual e instalando dependencias..."
  python3 -m venv .venv
  .venv/bin/pip install -q -r requirements.txt
fi

PIDS=()
limpiar() { for pid in "${PIDS[@]:-}"; do [ -n "$pid" ] && kill "$pid" 2>/dev/null || true; done; }
trap limpiar EXIT INT TERM

if [ "$USAR_NGROK" = 1 ]; then
  command -v ngrok >/dev/null || { echo "ngrok no está instalado: https://ngrok.com/download"; exit 1; }
  ngrok http "$PUERTO" --log=stdout >/tmp/teqruta-ngrok.log 2>&1 &
  PIDS+=($!)
  echo ">> Abriendo túnel ngrok..."
  URL=""
  for _ in $(seq 1 20); do
    URL=$(curl -s http://127.0.0.1:4040/api/tunnels 2>/dev/null | .venv/bin/python -c \
      'import sys, json; t = json.load(sys.stdin).get("tunnels", []); print(next((x["public_url"] for x in t if x["public_url"].startswith("https")), ""))' 2>/dev/null || true)
    [ -n "$URL" ] && break
    sleep 1
  done
  if [ -z "$URL" ]; then
    echo "No se pudo obtener la URL de ngrok. ¿Configuraste tu authtoken? (ngrok config add-authtoken <token>)"
    echo "Detalle en /tmp/teqruta-ngrok.log"
    exit 1
  fi
  echo
  echo "=============================================================="
  echo "  URL pública:  $URL"
  echo "  Pégala en API_URL (app/.../data/remote/ApiConfig.kt) y vuelve a ejecutar la app."
  echo "=============================================================="
  echo
fi

PORT="$PUERTO" .venv/bin/python app.py "${ARGS_APP[@]:-}"
