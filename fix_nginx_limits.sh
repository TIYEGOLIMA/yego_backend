#!/bin/bash

# Script para aumentar límites de archivos abiertos para Nginx
# Uso: sudo ./fix_nginx_limits.sh

echo "🔧 Aumentando límites de archivos abiertos para Nginx..."
echo ""

# 1. Verificar límite actual de Nginx
echo "📊 Límite actual de Nginx:"
NGINX_PID=$(pgrep -f "nginx: master" | head -1)
if [ ! -z "$NGINX_PID" ]; then
    echo "   PID: $NGINX_PID"
    cat /proc/$NGINX_PID/limits | grep "open files"
else
    echo "   ⚠️  No se encontró proceso master de Nginx"
fi
echo ""

# 2. Editar /etc/security/limits.conf
echo "📝 Editando /etc/security/limits.conf..."
if ! grep -q "nginx.*nofile" /etc/security/limits.conf 2>/dev/null; then
    echo "" | sudo tee -a /etc/security/limits.conf
    echo "# Límites para Nginx" | sudo tee -a /etc/security/limits.conf
    echo "nginx soft nofile 65536" | sudo tee -a /etc/security/limits.conf
    echo "nginx hard nofile 65536" | sudo tee -a /etc/security/limits.conf
    echo "www-data soft nofile 65536" | sudo tee -a /etc/security/limits.conf
    echo "www-data hard nofile 65536" | sudo tee -a /etc/security/limits.conf
    echo "✅ Límites agregados a /etc/security/limits.conf"
else
    echo "ℹ️  Los límites ya existen en /etc/security/limits.conf"
fi
echo ""

# 3. Editar /etc/systemd/system.conf (si usa systemd)
if [ -f /etc/systemd/system.conf ]; then
    echo "📝 Verificando /etc/systemd/system.conf..."
    if ! grep -q "^DefaultLimitNOFILE" /etc/systemd/system.conf; then
        echo "   Agregando DefaultLimitNOFILE..."
        sudo sed -i '/^\[Manager\]/a DefaultLimitNOFILE=65536' /etc/systemd/system.conf
        echo "✅ DefaultLimitNOFILE agregado"
    else
        echo "ℹ️  DefaultLimitNOFILE ya existe"
    fi
fi
echo ""

# 4. Crear override para Nginx (si usa systemd)
if systemctl list-units | grep -q nginx.service; then
    echo "📝 Creando override para nginx.service..."
    sudo mkdir -p /etc/systemd/system/nginx.service.d/
    echo "[Service]" | sudo tee /etc/systemd/system/nginx.service.d/override.conf
    echo "LimitNOFILE=65536" | sudo tee -a /etc/systemd/system/nginx.service.d/override.conf
    echo "✅ Override creado"
    
    echo "🔄 Recargando systemd..."
    sudo systemctl daemon-reload
fi
echo ""

# 5. Reiniciar Nginx
echo "🔄 Reiniciando Nginx para aplicar cambios..."
sudo systemctl restart nginx

# 6. Verificar nuevo límite
sleep 2
echo ""
echo "📊 Nuevo límite de Nginx:"
NGINX_PID=$(pgrep -f "nginx: master" | head -1)
if [ ! -z "$NGINX_PID" ]; then
    cat /proc/$NGINX_PID/limits | grep "open files"
fi
echo ""

# 7. Verificar archivos abiertos actuales
echo "📊 Archivos abiertos actualmente por Nginx:"
if [ ! -z "$NGINX_PID" ]; then
    OPEN_FILES=$(sudo lsof -p $NGINX_PID 2>/dev/null | wc -l)
    echo "   Archivos abiertos: $OPEN_FILES"
fi
echo ""

echo "✅ Proceso completado!"
echo "💡 Si el problema persiste, verifica que no haya conexiones WebSocket colgadas"

