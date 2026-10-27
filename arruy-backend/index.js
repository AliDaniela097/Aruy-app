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

// Limite para subir imagenes (cada una puede pesar hasta 5 MB)
const limiteImagenes = rateLimit({
  windowMs: 15 * 60 * 1000,
  limit: 60,
  standardHeaders: 'draft-7',
  legacyHeaders: false,
  message: { error: 'Demasiadas imagenes en poco tiempo, intenta mas tarde' },
});

app.get('/health', (req, res) => res.json({ estado: 'ok' }));
app.use('/auth', limiteAuth, require('./routes/auth'));
// Subir fotos (se procesan con sharp): limite de subidas por IP
app.post('/servicios/:id/fotos', limiteImagenes);
app.post('/usuarios/yo/foto', limiteImagenes);
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

// Errores que no atrapo ninguna ruta: siempre JSON, nunca el detalle interno
app.use((err, req, res, next) => {
  if (res.headersSent) return next(err);
  if (err.type === 'entity.too.large') return res.status(413).json({ error: 'El cuerpo de la peticion es demasiado grande' });
  if (err.type === 'entity.parse.failed') return res.status(400).json({ error: 'El cuerpo no es un JSON valido' });
  console.error(err.message);
  res.status(500).json({ error: 'Error interno del servidor' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Servidor en http://localhost:${PORT}`));
