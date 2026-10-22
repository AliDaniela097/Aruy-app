const express = require('express');
const pool = require('../db');

const router = express.Router();

router.get('/:id', async (req, res) => {
  try {
    const r = await pool.query(
      `SELECT id, nombre, telefono, rol, estado_verificacion, foto_perfil, activo
       FROM usuarios WHERE id = $1`,
      [req.params.id]
    );
    if (r.rows.length === 0) {
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }
    const u = r.rows[0];
    res.json({ ...u, id: String(u.id), telefono: u.telefono ?? '' });
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al consultar el usuario' });
  }
});

module.exports = router;