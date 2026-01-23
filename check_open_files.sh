#!/bin/bash

# Script para verificar archivos abiertos
# Uso: ./check_open_files.sh

echo "🔍 Verificando archivos abiertos en el sistema..."
echo ""

# 1. Límite del sistema
echo "📊 Límite del sistema:"
ulimit -n
echo ""

# 2. Archivos abiertos por Nginx
echo "📊 Archivos abiertos por Nginx:"
NGINX_PIDS=$(pgrep -f nginx)
if [ ! -z "$NGINX_PIDS" ]; then
    for pid in $NGINX_PIDS; do
        OPEN_FILES=$(sudo lsof -p $pid 2>/dev/null | wc -l)
        PROCESS_NAME=$(ps -p $pid -o comm= 2>/dev/null)
        echo "   PID $pid ($PROCESS_NAME): $OPEN_FILES archivos abiertos"
    done
else
    echo "   ⚠️  No se encontraron procesos de Nginx"
fi
echo ""

# 3. Archivos abiertos por Java (Spring Boot)
echo "📊 Archivos abiertos por Java (Spring Boot):"
JAVA_PIDS=$(pgrep -f java)
if [ ! -z "$JAVA_PIDS" ]; then
    for pid in $JAVA_PIDS; do
        OPEN_FILES=$(sudo lsof -p $pid 2>/dev/null | wc -l)
        PROCESS_CMD=$(ps -p $pid -o cmd= 2>/dev/null | head -c 60)
        echo "   PID $pid: $OPEN_FILES archivos abiertos"
        echo "      $PROCESS_CMD..."
    done
else
    echo "   ⚠️  No se encontraron procesos Java"
fi
echo ""

# 4. Conexiones WebSocket activas
echo "📊 Conexiones WebSocket activas (puerto 3030):"
if command -v netstat &> /dev/null; then
    netstat -an | grep :3030 | grep ESTABLISHED | wc -l | xargs echo "   Conexiones ESTABLISHED:"
elif command -v ss &> /dev/null; then
    ss -an | grep :3030 | grep ESTAB | wc -l | xargs echo "   Conexiones ESTABLISHED:"
fi
echo ""

# 5. Top 10 procesos con más archivos abiertos
echo "📊 Top 10 procesos con más archivos abiertos:"
sudo lsof 2>/dev/null | awk '{print $2}' | sort | uniq -c | sort -rn | head -10 | while read count pid; do
    if [ ! -z "$pid" ] && [ "$pid" != "PID" ]; then
        PROCESS_NAME=$(ps -p $pid -o comm= 2>/dev/null)
        echo "   PID $pid ($PROCESS_NAME): $count archivos"
    fi
done
echo ""

# 6. Verificar límites de procesos específicos
echo "📊 Límites de procesos:"
if [ ! -z "$NGINX_PIDS" ]; then
    FIRST_NGINX_PID=$(echo $NGINX_PIDS | awk '{print $1}')
    echo "   Nginx (PID $FIRST_NGINX_PID):"
    cat /proc/$FIRST_NGINX_PID/limits 2>/dev/null | grep "open files" || echo "      No se pudo leer"
fi
if [ ! -z "$JAVA_PIDS" ]; then
    FIRST_JAVA_PID=$(echo $JAVA_PIDS | awk '{print $1}')
    echo "   Java (PID $FIRST_JAVA_PID):"
    cat /proc/$FIRST_JAVA_PID/limits 2>/dev/null | grep "open files" || echo "      No se pudo leer"
fi

