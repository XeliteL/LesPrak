#!/bin/sh
# Экспорт диаграмм draw.io в PNG для отчёта. Требуется draw.io desktop (пакет drawio-desktop).
# Запуск из корня репозитория: sh tools/diagrams/export.sh
set -e
cd "$(dirname "$0")/../../diagrams"
mkdir -p png
ex() { drawio -x -f png -s 2 -b 10 -p "$2" -o "png/$3" "$1" >/dev/null 2>&1; echo "png/$3"; }
for i in 1 2 3; do ex bpmn-prodazha-kvartiry.drawio "$i" "bpmn-$i.png"; done
ex er-logicheskaya-model.drawio 1 er.png
for i in 1 2 3 4 5; do ex algoritmy.drawio "$i" "algo-$i.png"; done
