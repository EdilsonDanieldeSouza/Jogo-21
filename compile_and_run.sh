#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Script de compilação e execução do Blackjack RMI
# Requer: JDK 11+ (javac + java)
# ─────────────────────────────────────────────────────────────────────────────

set -e

SRC_DIR="src"
OUT_DIR="out"
PKG="blackjack"

echo "=== Compilando o projeto ==="
mkdir -p "$OUT_DIR"
javac -d "$OUT_DIR" -sourcepath "$SRC_DIR" "$SRC_DIR/$PKG"/*.java
echo "Compilação concluída com sucesso."

echo ""
echo "=== Como executar ==="
echo ""
echo "1) Em um terminal, inicie o Servidor:"
echo "   java -cp out blackjack.Servidor"
echo ""
echo "2) Em outro terminal, inicie o Cliente:"
echo "   java -cp out blackjack.Cliente"
echo ""
echo "   (Para múltiplos jogadores, abra um terminal por cliente)"
