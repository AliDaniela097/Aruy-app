const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { exigirToken, exigirRol } = require('../middleware/auth');
const { esUuid, textoValido } = require('../validar');

const router = express.Router();

// Calificaciones de un prestador, con el promedio (publico)
router.get('/', async (req, res) => {
  const { perfil_prestador_id } = req.query;
  if (!esUuid(perfil_prestador_id)) {
    return res.status(400).json({ error: 'perfil_prestador_id es obligatorio y debe ser un id valido' });
  }
  try {
    const r = await pool.query(
      `SELECT c.id, c.puntuacion, c.comentario, c.fecha_creacion, u.nombre AS contratante
       FROM calificaciones c JOIN usuarios u ON u.id = c.contratante_id
       WHERE c.perfil_prestador_id = $1
       ORDER BY c.fecha_creacion DESC`,
      [perfil_prestador_id]
    );
    const total = r.rows.length;
    const suma = r.rows.reduce((n, x) => n + Number(x.puntuacion), 0);
    res.json({
      perfil_prestador_id,
      promedio: total ? Math.round((suma / total) * 10) / 10 : null,
      total,
      calificaciones: r.rows.map((x) => ({ ...x, id: String(x.id) })),
    });
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al consultar las calificaciones' });
  }
});

// Calificar a un prestador: solo un CONTRATANTE, una vez por prestador, de 1 a 5
router.post('/', exigirToken, exigirRol('CONTRATANTE'), async (req, res) => {
  const { perfil_prestador_id, puntuacion, comentario } = req.body || {};
  if (!esUuid(perfil_prestador_id)) return res.status(400).json({ error: 'perfil_prestador_id invalido' });
  if (!Number.isInteger(puntuacion) || puntuacion < 1 || puntuacion > 5) {
    return res.status(400).json({ error: 'puntuacion debe ser un numero entero de 1 a 5' });
  }
  if (comentario != null && !textoValido(comentario, 1000)) {
    return res.status(400).json({ error: 'comentario invalido (maximo 1000 caracteres)' });
  }
  try {
    const p = await pool.query('SELECT 1 FROM perfiles_prestadores WHERE id = $1', [perfil_prestador_id]);
    if (p.rows.length === 0) return res.status(404).json({ error: 'Prestador no encontrado' });
    const r = await pool.query(
      `INSERT INTO calificaciones (id, contratante_id, perfil_prestador_id, puntuacion, comentario, fecha_creacion)
       VALUES ($1, $2, $3, $4, $5, NOW())
       RETURNING id, perfil_prestador_id, puntuacion, comentario, fecha_creacion`,
      [crypto.randomUUID(), req.usuario.id, perfil_prestador_id, puntuacion, comentario == null ? null : comentario.trim()]
    );
    res.status(201).json({ ...r.rows[0], id: String(r.rows[0].id), perfil_prestador_id: String(r.rows[0].perfil_prestador_id) });
  } catch (e) {
    if (e.code === '23505') return res.status(409).json({ error: 'Ya calificaste a este prestador' });
    console.error(e.message);
    res.status(500).json({ error: 'Error al guardar la calificacion' });
  }
});

module.exports = router;
