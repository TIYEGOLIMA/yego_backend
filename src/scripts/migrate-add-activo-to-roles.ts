import { DataSource } from 'typeorm';
import { config } from 'dotenv';

// Cargar variables de entorno
config();

async function migrateAddActivoToRoles() {
  const dataSource = new DataSource({
    type: 'postgres',
    host: process.env.DB_HOST,
    port: parseInt(process.env.DB_PORT || '5432'),
    username: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    synchronize: false,
    logging: true,
  });

  try {
    await dataSource.initialize();
    console.log('✅ Conexión a la base de datos establecida');

    // Agregar columna activo si no existe
    await dataSource.query(`
      ALTER TABLE roles ADD COLUMN IF NOT EXISTS activo BOOLEAN DEFAULT true;
    `);
    console.log('✅ Columna activo agregada a la tabla roles');

    // Actualizar roles existentes para que estén activos
    await dataSource.query(`
      UPDATE roles SET activo = true WHERE activo IS NULL;
    `);
    console.log('✅ Roles existentes actualizados como activos');

    // Hacer la columna NOT NULL después de actualizar los valores
    await dataSource.query(`
      ALTER TABLE roles ALTER COLUMN activo SET NOT NULL;
    `);
    console.log('✅ Columna activo marcada como NOT NULL');

    // Crear índice para mejorar el rendimiento
    await dataSource.query(`
      CREATE INDEX IF NOT EXISTS idx_roles_activo ON roles(activo);
    `);
    console.log('✅ Índice creado en la columna activo');

    // Verificar la migración
    const result = await dataSource.query(`
      SELECT COUNT(*) as total_roles, 
             COUNT(CASE WHEN activo = true THEN 1 END) as roles_activos,
             COUNT(CASE WHEN activo = false THEN 1 END) as roles_inactivos
      FROM roles;
    `);
    
    console.log('📊 Estadísticas de roles:');
    console.log(`   Total de roles: ${result[0].total_roles}`);
    console.log(`   Roles activos: ${result[0].roles_activos}`);
    console.log(`   Roles inactivos: ${result[0].roles_inactivos}`);

    console.log('🎉 Migración completada exitosamente!');
  } catch (error) {
    console.error('❌ Error durante la migración:', error);
    throw error;
  } finally {
    await dataSource.destroy();
    console.log('🔌 Conexión a la base de datos cerrada');
  }
}

// Ejecutar la migración
migrateAddActivoToRoles()
  .then(() => {
    console.log('✅ Script de migración ejecutado correctamente');
    process.exit(0);
  })
  .catch((error) => {
    console.error('❌ Error en el script de migración:', error);
    process.exit(1);
  }); 