require('dotenv').config();

const { Pool } = require('pg');

if (!process.env.DATABASE_URL) {
    throw new Error(
        'Falta configurar DATABASE_URL en el archivo .env'
    );
}

const pool = new Pool({
    connectionString: process.env.DATABASE_URL,
    ssl: {
        rejectUnauthorized: true
    },
    // Si la base no responde, fallar rapido (antes la peticion quedaba colgada ~20 s)
    connectionTimeoutMillis: 8000,
    query_timeout: 15000,
    idleTimeoutMillis: 30000,
    keepAlive: true
});

// Neon cierra las conexiones inactivas. Sin este manejador, ese error en una conexion
// sin uso puede tumbar todo el proceso de Node.
pool.on('error', (err) => {
    console.error('Conexion inactiva con la base cerrada:', err.code || err.message);
});

module.exports = pool;
