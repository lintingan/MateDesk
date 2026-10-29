#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
PORT=8765
URL="http://127.0.0.1:${PORT}/MateDesk_Prototype_v0.2.html"

if ! command -v python3 >/dev/null 2>&1; then
  echo "Python 3 tidak ditemukan. Buka MateDesk_Prototype_v0.2.html langsung di browser."
  read -r -p "Tekan Enter untuk menutup..."
  exit 1
fi
if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  open "$URL"
  echo "MateDesk preview sudah berjalan di $URL"
  exit 0
fi
cd "$ROOT"
python3 -m http.server "$PORT" --bind 127.0.0.1 > /tmp/matedesk-preview-server.log 2>&1 &
SERVER_PID=$!
cleanup() { kill "$SERVER_PID" >/dev/null 2>&1 || true; }
trap cleanup EXIT INT TERM
sleep 1
open "$URL"
echo "MateDesk berjalan di $URL"
echo "Biarkan jendela Terminal ini terbuka. Tekan Control+C untuk menghentikan server."
wait "$SERVER_PID"
