const express = require('express');
const pool = require('../db');
const { esUuid } = require('../validar');

const router = express.Router();

// Nunca se devuelven email ni password_hash
const COLUMNAS = 'id, nombre, telefono, rol, estado_verificacion, foto_perfil, activo';

const aUsuario = (u) => ({ ...u, id: String(u.id), telefono: u.telefono ?? '' });

// El registro y el login estan en /auth (routes/auth.js)
router.get('/:id', async (req, res) => {
  if (!esUuid(req.params.id)) return res.status(400).json({ error: 'Id invalido' });
  try {
    const r = await pool.query(`SELECT ${COLUMNAS} FROM usuarios WHERE id = $1`, [req.params.id]);
    if (r.rows.length === 0) return res.status(404).json({ error: 'Usuario no encontrado' });
    res.json(aUsuario(r.rows[0]));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al consultar el usuario' });
  }
});

module.exports = router;
