require('dotenv').config();
const express = require('express');

const app = express();
app.use(express.json());

app.get('/health', (req, res) => res.json({ estado: 'ok' }));
app.use('/servicios', require('./routes/servicios'));
app.use('/usuarios', require('./routes/usuarios'));
app.use('/fotos', require('./routes/fotos'));

app.use((req, res) => res.status(404).json({ error: 'Ruta no encontrada' }));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Servidor en http://localhost:${PORT}`));