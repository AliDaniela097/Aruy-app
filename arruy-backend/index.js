require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const { rateLimit } = require('express-rate-limit');

if (!process.env.JWT_SECRET) {
  throw new Error('Falta JWT_SECRET en el archivo .env');
}

const app = express();
app.use(helmet());
app.use(express.json({ limit: '100kb' }));

// Limite general
app.use(
  rateLimit({
    windowMs: 15 * 60 * 1000,
    limit: 300,
    standardHeaders: 'draft-7',
    legacyHeaders: false,
  })
);

// Limite mas estricto para registro e inicio de sesion (frena adivinar contrasenas)
const limiteAuth = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 20,
  standardHeaders: 'draft-7',
  legacyHeaders: false,
  message: { error: 'Demasiados intentos, intenta mas tarde' },
});

app.get('/health', (req, res) => res.json({ estado: 'ok' }));
app.use('/auth', limiteAuth, require('./routes/auth'));
app.use('/servicios', require('./routes/servicios'));
app.use('/usuarios', require('./routes/usuarios'));
app.use('/perfiles', require('./routes/perfiles'));
app.use('/fotos', require('./routes/fotos'));

app.use((req, res) => res.status(404).json({ error: 'Ruta no encontrada' }));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Servidor en http://localhost:${PORT}`));
