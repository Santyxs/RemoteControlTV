#!/usr/bin/env bash
# Descomprime TVRemote-proyecto.zip dentro de un repo de GitHub (Codespace) y hace commit + push.
#
# Uso:
#   ./unzip_to_repo.sh [ruta_al_zip] [carpeta_destino]
#
# Por defecto:
#   ruta_al_zip     -> ~/TVRemote-proyecto.zip
#   carpeta_destino -> directorio actual (asume que ya estás dentro del repo)
#
# El zip trae una carpeta raíz "tvremote/" con todo el proyecto Android
# (app/, build.gradle.kts, settings.gradle.kts, etc). Este script mueve
# ese contenido directamente a la carpeta destino, sin dejar la carpeta
# "tvremote" de por medio, para que el proyecto quede en la raíz del repo.

set -e

ZIP_PATH="${1:-$HOME/TVRemote-proyecto.zip}"
DEST_DIR="${2:-$(pwd)}"

if [ ! -f "$ZIP_PATH" ]; then
  echo "No encuentro el zip en: $ZIP_PATH"
  echo "Subilo primero al Codespace (arrastralo al explorador de archivos de VS Code"
  echo "o descargalo con curl/wget si tenés un link directo), y volvé a correr el script."
  exit 1
fi

if [ ! -d "$DEST_DIR/.git" ]; then
  echo "ADVERTENCIA: $DEST_DIR no parece ser la raíz de un repo git (no hay carpeta .git)."
  echo "Corré el script desde la raíz de tu repo, o pasá la ruta correcta como segundo argumento."
  read -p "¿Continuar de todas formas? (s/n) " confirm
  if [ "$confirm" != "s" ]; then
    exit 1
  fi
fi

TMP_DIR=$(mktemp -d)
echo "Descomprimiendo $ZIP_PATH..."
unzip -q "$ZIP_PATH" -d "$TMP_DIR"

# Mueve el contenido de la carpeta raíz del zip (tvremote/) al destino,
# sea cual sea su nombre exacto, sin dejar esa carpeta contenedora.
shopt -s dotglob nullglob
INNER_DIR=$(find "$TMP_DIR" -mindepth 1 -maxdepth 1 -type d | head -n 1)

if [ -n "$INNER_DIR" ]; then
  mv "$INNER_DIR"/* "$DEST_DIR"/
else
  mv "$TMP_DIR"/* "$DEST_DIR"/
fi

rm -rf "$TMP_DIR"
echo "Archivos copiados a $DEST_DIR"

cd "$DEST_DIR"
git add -A
git commit -m "Agrega proyecto TV Remote (Android/Kotlin)"
git push

echo "Listo: proyecto descomprimido y subido al repo."
