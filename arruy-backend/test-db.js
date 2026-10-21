const pool = require('./db');

async function probarConexion() {
    try {
        const resultado = await pool.query(
            'SELECT NOW() AS fecha_servidor, current_database() AS base_datos'
        );

        console.log('Conexión exitosa con Neon PostgreSQL');
        console.log('Base de datos:', resultado.rows[0].base_datos);
        console.log('Fecha del servidor:', resultado.rows[0].fecha_servidor);

    } catch (error) {
        console.error('Error al conectar con Neon:', error.message);
        process.exitCode = 1;

    } finally {
        await pool.end();
    }
}

probarConexion();
