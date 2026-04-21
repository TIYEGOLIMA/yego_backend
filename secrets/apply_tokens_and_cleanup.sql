-- ============================================================================
-- V3: Aplicación final
--   1) Reemplaza access_token de los 12 dispositivos con hashes BCrypt reales
--   2) Pone Tablet Principal con module_id = NULL (separa generación/atención)
--   3) Desactiva los 4 usuarios-tablet legacy de Lince
-- ============================================================================
BEGIN;

UPDATE dispositivos SET access_token = '$2y$10$1GUF.vmjBHUoelml9SdQcO/uURN7yDCDOysV1OgWi/tRCqbAnsFfK', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 1; -- Lince | Tablet Principal
UPDATE dispositivos SET access_token = '$2y$10$Us6l95krMi3rza/9NTq1NuE1vSP90C/uf.UJ.TxhqeadBBmz.89s6', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 4; -- Lince | Tablet 1
UPDATE dispositivos SET access_token = '$2y$10$Bjsg7VYu8Exj3u1aVfKQPeM29gP4loV2z95ifNTA3/iWbqi4uizwu', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 7; -- Lince | Tablet 2
UPDATE dispositivos SET access_token = '$2y$10$yB7hatcLc2vmAOC4mYI9CubnObX0Kro4mN5A/7T/yMaRydkK3XBQK', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 10; -- Lince | TV
UPDATE dispositivos SET access_token = '$2y$10$Xa7Wys./y.lZJbUoXPlIkuNRGmRCg0CZY65V94ibdbsqG0Mc5RlF.', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 2; -- San Miguel | Tablet Principal
UPDATE dispositivos SET access_token = '$2y$10$ZjhFV9nzC2wu8PWgLNhSreu4HwkGEv.tPFxwvtL5v949RTpuhwS6S', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 5; -- San Miguel | Tablet 1
UPDATE dispositivos SET access_token = '$2y$10$UZr5RE44qrDVzr7xmcv0CuWYKFuyHOOGXy/PBRbLyz/MUYOwrA4Ja', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 8; -- San Miguel | Tablet 2
UPDATE dispositivos SET access_token = '$2y$10$fYOawKoRg60w9fmHS/q1tu7slLPZvCbRnTSErANY4LBv974qMu3.q', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 11; -- San Miguel | TV
UPDATE dispositivos SET access_token = '$2y$10$bucg6Uom3vZAnNaEDNGNP.NGHhcBn8zBUSveIvEeG4ctY6gO5Ajf2', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 3; -- Trujillo | Tablet Principal
UPDATE dispositivos SET access_token = '$2y$10$WZCwz.ZYQylfc1oS.qipH.0j30IO./lxVfs3ktQT0RfzAuv818o2O', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 6; -- Trujillo | Tablet 1
UPDATE dispositivos SET access_token = '$2y$10$MJ/aHGeXZvrdGG4N4hbqbOE.AJTUHMIpT79TyIn.JhLpBxmmEa1cy', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 9; -- Trujillo | Tablet 2
UPDATE dispositivos SET access_token = '$2y$10$ZT5vytpnvaLYhPOtcu5zB.2Ukw/MGUme2UAGg8CFvQK/IccoDPbgy', updated_at = NOW() AT TIME ZONE 'America/Lima' WHERE id = 12; -- Trujillo | TV

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
