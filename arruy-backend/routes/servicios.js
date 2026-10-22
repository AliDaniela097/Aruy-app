const express = require('express');
const pool = require('../db');

const router = express.Router();

const COLUMNAS =
  'id, prestador_id, titulo, descripcion, categoria, precio_referencial, disponible';

function aServicio(f) {
  return {
    ...f,
    id: String(f.id),
    prestador_id: String(f.prestador_id),
    precio_referencial:
      f.precio_referencial === null ? null : Number(f.precio_referencial),
  };
}

router.get('/', async (req, res) => {
  try {
    const { prestador_id } = req.query;
    const r = prestador_id
      ? await pool.query(
          `SELECT ${COLUMNAS} FROM servicios WHERE prestador_id = $1 ORDER BY titulo`,
          [prestador_id]
        )
      : await pool.query(`SELECT ${COLUMNAS} FROM servicios ORDER BY titulo`);
    res.json(r.rows.map(aServicio));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al consultar servicios' });
  }
});

router.get('/:id', async (req, res) => {
  try {
    const r = await pool.query(
      `SELECT ${COLUMNAS} FROM servicios WHERE id = $1`,
      [req.params.id]
    );
    if (r.rows.length === 0) {
      return res.status(404).json({ error: 'Servicio no encontrado' });
    }
    res.json(aServicio(r.rows[0]));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al consultar el servicio' });
  }
});

module.exports = router;