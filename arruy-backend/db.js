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
    }
});

module.exports = pool;
