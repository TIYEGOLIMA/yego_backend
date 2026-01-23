#!/bin/bash

# Script para verificar el estado del servicio y puerto
# Uso: ./check_service.sh

echo "🔍 Verificando estado del servicio y puerto..."
echo ""

# 1. Verificar estado del servicio
echo "📊 Estado del servicio:"
sudo systemctl status yego_backend.service --no-pager -l | head -20
echo ""

# 2. Verificar si el puerto 3030 está escuchando
echo "🔌 Verificando puerto 3030:"
if sudo lsof -i:3030 2>/dev/null | grep -q LISTEN; then
    echo "✅ Puerto 3030 está escuchando:"
    sudo lsof -i:3030 | grep LISTEN
else
    echo "❌ Puerto 3030 NO está escuchando"
    echo "   La aplicación no está corriendo o no está en el puerto correcto"
fi
echo ""

# 3. Verificar procesos Java
echo "☕ Procesos Java relacionados:"
ps aux | grep -i "yego\|java.*3030" | grep -v grep || echo "   No se encontraron procesos"
echo ""

# 4. Verificar logs recientes
echo "📋 Últimos logs del servicio:"
sudo journalctl -u yego_backend.service -n 30 --no-pager | tail -20
echo ""

# 5. Intentar conectar al puerto
echo "🌐 Intentando conectar a localhost:3030:"
if timeout 2 bash -c "echo > /dev/tcp/localhost/3030" 2>/dev/null; then
    echo "✅ Conexión exitosa al puerto 3030"
else
    echo "❌ No se pudo conectar al puerto 3030"
fi
echo ""

# 6. Verificar configuración de Nginx
echo "⚙️  Verificando upstream de Nginx:"
if [ -f /etc/nginx/sites-available/api-int.yego.pro ]; then
    echo "   Archivo de configuración encontrado"
    echo "   Upstream configurado:"
    grep -A 5 "upstream\|proxy_pass.*localhost" /etc/nginx/sites-available/api-int.yego.pro 2>/dev/null | head -10
else
    echo "   ⚠️  No se encontró archivo de configuración en /etc/nginx/sites-available/api-int.yego.pro"
fi

