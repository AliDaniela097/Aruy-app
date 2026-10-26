const express = require('express');
const pool = require('../db');
const { exigirToken } = require('../middleware/auth');
const { esUuid, textoValido, esUrlHttps } = require('../validar');

const router = express.Router();

// Nunca se devuelve password_hash. El correo solo se devuelve en "mis datos" (/yo).
const COLUMNAS = 'id, nombre, telefono, rol, estado_verificacion, foto_perfil, activo';

const aUsuario = (u) => ({ ...u, id: String(u.id), telefono: u.telefono ?? '' });

// El registro y el login estan en /auth (routes/auth.js)

// Mis datos (incluye mi correo)
router.get('/yo', exigirToken, async (req, res) => {
  try {
    const r = await pool.query(`SELECT ${COLUMNAS}, email FROM usuarios WHERE id = $1`, [req.usuario.id]);
    if (r.rows.length === 0) return res.status(404).json({ error: 'Usuario no encontrado' });
    res.json(aUsuario(r.rows[0]));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al consultar tus datos' });
  }
});

// Editar mis datos: solo se cambian los campos que se envian
router.patch('/yo', exigirToken, async (req, res) => {
  const b = req.body || {};
  const sets = [];
  const vals = [];
  const poner = (columna, valor) => { vals.push(valor); sets.push(`${columna} = $${vals.length}`); };

  if (b.nombre !== undefined) {
    if (!textoValido(b.nombre, 100)) return res.status(400).json({ error: 'nombre invalido' });
    poner('nombre', b.nombre.trim());
  }
  if (b.telefono !== undefined) {
    if (!textoValido(b.telefono, 20)) return res.status(400).json({ error: 'telefono invalido' });
    poner('telefono', b.telefono.trim());
  }
  if (b.foto_perfil !== undefined) {
    if (b.foto_perfil !== null && !esUrlHttps(b.foto_perfil)) {
      return res.status(400).json({ error: 'foto_perfil debe ser una direccion https, o null' });
    }
    poner('foto_perfil', b.foto_perfil === null ? null : b.foto_perfil.trim());
  }
  if (sets.length === 0) return res.status(400).json({ error: 'No enviaste ningun campo para cambiar' });

  try {
    vals.push(req.usuario.id);
    const r = await pool.query(
      `UPDATE usuarios SET ${sets.join(', ')} WHERE id = $${vals.length} RETURNING ${COLUMNAS}`,
      vals
    );
    if (r.rows.length === 0) return res.status(404).json({ error: 'Usuario no encontrado' });
    res.json(aUsuario(r.rows[0]));
  } catch (e) {
    if (e.code === '23505') return res.status(409).json({ error: 'Ese telefono ya esta registrado' });
    console.error(e.message);
    res.status(500).json({ error: 'Error al editar tus datos' });
  }
});

// Darme de baja: NO se borra (la base lo impide si tengo perfil o calificaciones), se marca activo = false
router.delete('/yo', exigirToken, async (req, res) => {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    await client.query('UPDATE usuarios SET activo = false WHERE id = $1', [req.usuario.id]);
    // Si era prestador, su perfil deja de aparecer como disponible
    await client.query('UPDATE perfiles_prestadores SET disponible = false WHERE usuario_id = $1', [req.usuario.id]);
    await client.query('COMMIT');
    res.status(204).end();
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    console.error(e.message);
    res.status(500).json({ error: 'Error al darte de baja' });
  } finally {
    client.release();
  }
});

// Consultar un usuario (publico, sin correo)
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
