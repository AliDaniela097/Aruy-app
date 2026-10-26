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
app.use('/calificaciones', require('./routes/calificaciones'));
app.use('/suscripciones', require('./routes/suscripciones'));

// Documentación interactiva (Swagger) en /docs
try {
  const swaggerUi = require('swagger-ui-express');
  const especificacion = require('./swagger');
  app.use(
    '/docs',
    (req, res, next) => {
      // La pagina de Swagger usa scripts propios que el CSP de helmet bloquearia
      res.removeHeader('Content-Security-Policy');
      next();
    },
    swaggerUi.serve,
    swaggerUi.setup(especificacion, { customSiteTitle: 'Aruy API' })
  );
  app.get('/docs.json', (req, res) => res.json(especificacion));
} catch (e) {
  if (e.code !== 'MODULE_NOT_FOUND') throw e;
  console.warn('Swagger desactivado: ejecuta "npm install swagger-ui-express" en arruy-backend');
}

app.use((req, res) => res.status(404).json({ error: 'Ruta no encontrada' }));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Servidor en http://localhost:${PORT}`));
