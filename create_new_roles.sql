-- Script para crear los nuevos roles: TV, TABLET1, TABLET2, PRINCIPAL
-- Ejecutar este script en la base de datos PostgreSQL

-- Insertar rol TV
INSERT INTO roles (name, description, permissions, active, created_at, updated_at)
VALUES (
    'TV', 
    'Rol para dispositivos de TV del sistema', 
    '{"tickets": ["read"], "modules": ["read"]}', 
    true, 
    NOW(), 
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Insertar rol TABLET1
INSERT INTO roles (name, description, permissions, active, created_at, updated_at)
VALUES (
    'TABLET1', 
    'Rol para tablet 1 del sistema', 
    '{"tickets": ["read", "write"], "modules": ["read"]}', 
    true, 
    NOW(), 
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Insertar rol TABLET2
INSERT INTO roles (name, description, permissions, active, created_at, updated_at)
VALUES (
    'TABLET2', 
    'Rol para tablet 2 del sistema', 
    '{"tickets": ["read", "write"], "modules": ["read"]}', 
    true, 
    NOW(), 
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Insertar rol PRINCIPAL
INSERT INTO roles (name, description, permissions, active, created_at, updated_at)
VALUES (
    'PRINCIPAL', 
    'Rol principal del sistema', 
    '{"tickets": ["read", "write"], "modules": ["read", "write"], "users": ["read"]}', 
    true, 
    NOW(), 
    NOW()
) ON CONFLICT (name) DO NOTHING;

-- Verificar que los roles se crearon correctamente
SELECT name, description, active, created_at 
FROM roles 
WHERE name IN ('TV', 'TABLET1', 'TABLET2', 'PRINCIPAL')
ORDER BY name;
