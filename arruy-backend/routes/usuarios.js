const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { esUuid, textoValido, ROLES } = require('../validar');

const router = express.Router();

const COLUMNAS = 'id, nombre, telefono, rol, estado_verificacion, foto_perfil, activo';

const aUsuario = (u) => ({ ...u, id: String(u.id), telefono: u.telefono ?? '' });

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

router.post('/', async (req, res) => {
  const { nombre, telefono, rol, foto_perfil } = req.body || {};
  if (!textoValido(nombre, 100)) return res.status(400).json({ error: 'nombre es obligatorio' });
  if (!textoValido(telefono, 20)) return res.status(400).json({ error: 'telefono es obligatorio' });
  if (!ROLES.includes(rol)) return res.status(400).json({ error: 'rol debe ser PRESTADOR o CONTRATANTE' });
  try {
    const r = await pool.query(
      `INSERT INTO usuarios (id, nombre, telefono, rol, estado_verificacion, foto_perfil, activo, fecha_registro)
       VALUES ($1, $2, $3, $4, 'NO_SOLICITADA', $5, true, NOW())
       RETURNING ${COLUMNAS}`,
      [crypto.randomUUID(), nombre.trim(), telefono.trim(), rol, foto_perfil ?? null]
    );
    res.status(201).json(aUsuario(r.rows[0]));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al crear el usuario' });
  }
});

module.exports = router;