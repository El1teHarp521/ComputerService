#!/usr/bin/env bash
# Запуск JavaFX-приложения (Linux / macOS). База данных PostgreSQL должна быть уже запущена.
set -euo pipefail
cd "$(dirname "$0")"

command -v mvn >/dev/null 2>&1 || { echo "Maven не найден. Нужен Maven 3.9+ и Java 21."; exit 1; }
command -v java >/dev/null 2>&1 || { echo "Java не найдена. Нужна Java 21."; exit 1; }

mvn -q javafx:run
