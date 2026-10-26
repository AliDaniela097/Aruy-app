const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { exigirToken, exigirRol } = require('../middleware/auth');
const { textoValido } = require('../validar');

const router = express.Router();

const COLUMNAS =
  'id, usuario_id, oficio_principal, descripcion_profesional, zona_cobertura, anios_experiencia, disponible';

const aPerfil = (p) => ({ ...p, id: String(p.id), usuario_id: String(p.usuario_id) });
const aniosValidos = (v) => Number.isInteger(v) && v >= 0 && v <= 80;

// Mi propio perfil de prestador
router.get('/yo', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  try {
    const r = await pool.query(`SELECT ${COLUMNAS} FROM perfiles_prestadores WHERE usuario_id = $1`, [req.usuario.id]);
    if (r.rows.length === 0) return res.status(404).json({ error: 'Todavia no tienes perfil de prestador' });
    res.json(aPerfil(r.rows[0]));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al consultar el perfil' });
  }
});

// Editar mi perfil: solo se cambian los campos que se envian (el perfil no se borra)
router.patch('/yo', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const b = req.body || {};
  const sets = [];
  const vals = [];
  const poner = (columna, valor) => { vals.push(valor); sets.push(`${columna} = $${vals.length}`); };

  if (b.oficio_principal !== undefined) {
    if (!textoValido(b.oficio_principal, 100)) return res.status(400).json({ error: 'oficio_principal invalido' });
    poner('oficio_principal', b.oficio_principal.trim());
  }
  if (b.zona_cobertura !== undefined) {
    if (!textoValido(b.zona_cobertura, 100)) return res.status(400).json({ error: 'zona_cobertura invalida' });
    poner('zona_cobertura', b.zona_cobertura.trim());
  }
  if (b.descripcion_profesional !== undefined) {
    if (b.descripcion_profesional !== null && !textoValido(b.descripcion_profesional, 2000)) {
      return res.status(400).json({ error: 'descripcion_profesional invalida' });
    }
    poner('descripcion_profesional', b.descripcion_profesional === null ? null : b.descripcion_profesional.trim());
  }
  if (b.anios_experiencia !== undefined) {
    if (b.anios_experiencia !== null && !aniosValidos(b.anios_experiencia)) {
      return res.status(400).json({ error: 'anios_experiencia debe ser un entero entre 0 y 80' });
    }
    poner('anios_experiencia', b.anios_experiencia);
  }
  if (b.disponible !== undefined) {
    if (typeof b.disponible !== 'boolean') return res.status(400).json({ error: 'disponible debe ser true o false' });
    poner('disponible', b.disponible);
  }
  if (sets.length === 0) return res.status(400).json({ error: 'No enviaste ningun campo para cambiar' });

  try {
    vals.push(req.usuario.id);
    const r = await pool.query(
      `UPDATE perfiles_prestadores SET ${sets.join(', ')} WHERE usuario_id = $${vals.length} RETURNING ${COLUMNAS}`,
      vals
    );
    if (r.rows.length === 0) return res.status(404).json({ error: 'Todavia no tienes perfil de prestador' });
    res.json(aPerfil(r.rows[0]));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al editar el perfil' });
  }
});

// Crear mi perfil (para prestadores que se registraron sin perfil)
router.post('/', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const { oficio_principal, zona_cobertura, descripcion_profesional, anios_experiencia } = req.body || {};
  if (!textoValido(oficio_principal, 100)) return res.status(400).json({ error: 'oficio_principal es obligatorio' });
  if (!textoValido(zona_cobertura, 100)) return res.status(400).json({ error: 'zona_cobertura es obligatoria' });
  if (descripcion_profesional != null && !textoValido(descripcion_profesional, 2000)) {
    return res.status(400).json({ error: 'descripcion_profesional invalida' });
  }
  if (anios_experiencia != null && !aniosValidos(anios_experiencia)) {
    return res.status(400).json({ error: 'anios_experiencia debe ser un entero entre 0 y 80' });
  }
  try {
    const r = await pool.query(
      `INSERT INTO perfiles_prestadores
         (id, usuario_id, oficio_principal, descripcion_profesional, zona_cobertura, anios_experiencia, disponible)
       VALUES ($1, $2, $3, $4, $5, $6, true)
       RETURNING ${COLUMNAS}`,
      [
        crypto.randomUUID(), req.usuario.id, oficio_principal.trim(),
        descripcion_profesional == null ? null : descripcion_profesional.trim(),
        zona_cobertura.trim(), anios_experiencia ?? null,
      ]
    );
    res.status(201).json(aPerfil(r.rows[0]));
  } catch (e) {
    if (e.code === '23505') return res.status(409).json({ error: 'Ya tienes un perfil de prestador' });
    console.error(e.message);
    res.status(500).json({ error: 'Error al crear el perfil' });
  }
});

module.exports = router;
