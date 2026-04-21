#!/bin/bash
set -euo pipefail

OUT_PLAIN="dispositivos_tokens_plain.txt"
OUT_SQL="apply_tokens_and_cleanup.sql"

cat > "$OUT_PLAIN" <<HEADER
============================================================
TOKENS PLANOS DE DISPOSITIVOS - GUARDA ESTE ARCHIVO SEGURO
Generados: $(date -Iseconds)
Sólo se ven UNA vez. La BD almacena el hash BCrypt.
Formato: <sede> | <dispositivo> | <token plano>
============================================================

HEADER

cat > "$OUT_SQL" <<HEADER
-- ============================================================================
-- V3: Aplicación final
--   1) Reemplaza access_token de los 12 dispositivos con hashes BCrypt reales
--   2) Pone Tablet Principal con module_id = NULL (separa generación/atención)
--   3) Desactiva los 4 usuarios-tablet legacy de Lince
-- ============================================================================
BEGIN;

HEADER

# Mapeo: db_id | sede | dispositivo
DEVICES=(
  "1|Lince|Tablet Principal"
  "4|Lince|Tablet 1"
  "7|Lince|Tablet 2"
  "10|Lince|TV"
  "2|San Miguel|Tablet Principal"
  "5|San Miguel|Tablet 1"
  "8|San Miguel|Tablet 2"
  "11|San Miguel|TV"
  "3|Trujillo|Tablet Principal"
  "6|Trujillo|Tablet 1"
  "9|Trujillo|Tablet 2"
  "12|Trujillo|TV"
)

for entry in "${DEVICES[@]}"; do
  IFS='|' read -r DB_ID SEDE DISPO <<<"$entry"
  RAND=$(openssl rand -hex 24)
  PLAIN="tkn_${RAND}"
  HASH=$(htpasswd -bnBC 10 "" "$PLAIN" | tr -d ':\n')
  printf "%-12s | %-18s | %s\n" "$SEDE" "$DISPO" "$PLAIN" >> "$OUT_PLAIN"
  printf "UPDATE dispositivos SET access_token = '%s', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = %s; -- %s | %s\n" \
    "$HASH" "$DB_ID" "$SEDE" "$DISPO" >> "$OUT_SQL"
done

cat >> "$OUT_SQL" <<FOOTER

-- 2) Tablet Principal sin module_id (la TP solo crea tickets, no atiende)
UPDATE dispositivos SET module_id = NULL, updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE type = 'TABLET_PRINCIPAL';

-- 3) Desactivar usuarios-tablet legacy
UPDATE users SET active = FALSE WHERE id IN (1, 4, 5, 6); -- ttablet1, ttablet2, pprincipal, tv

COMMIT;

-- VERIFICACIÓN
-- SELECT s.name AS sede, d.name AS disp, d.type, m.name AS modulo, LENGTH(d.access_token) AS hash_len
--   FROM dispositivos d JOIN sedes s ON s.id=d.sede_id LEFT JOIN yego_modules m ON m.id=d.module_id
--  ORDER BY s.id, d.type, d.name;
-- SELECT id, username, active FROM users WHERE id IN (1,4,5,6);
FOOTER

chmod 600 "$OUT_PLAIN"
echo "Tokens generados:"
echo "  $OUT_PLAIN (modo 600)"
echo "  $OUT_SQL"
