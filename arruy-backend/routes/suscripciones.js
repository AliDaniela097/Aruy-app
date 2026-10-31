const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { registrarError } = require('../registro');
const { exigirToken, exigirRol } = require('../middleware/auth');
const { esUuid } = require('../validar');

const router = express.Router();

// SIMULADO: no hay pago real. Duracion en dias de cada plan.
const PLANES = { ARUY_PRO_SEMANAL: 7, ARUY_PRO_MENSUAL: 30 };

const COLUMNAS = 'id, prestador_id, tipo_plan, estado, fecha_inicio, fecha_fin, renovacion_automatica';

const aSuscripcion = (s) => ({ ...s, id: String(s.id), prestador_id: String(s.prestador_id) });

// Las suscripciones ACTIVAS cuya fecha_fin ya paso se marcan VENCIDA
async function marcarVencidas(db, perfilId) {
  await db.query(
    `UPDATE suscripciones SET estado = 'VENCIDA'
     WHERE prestador_id = $1 AND estado = 'ACTIVA' AND fecha_fin IS NOT NULL AND fecha_fin < NOW()`,
    [perfilId]
  );
}

async function perfilDe(db, usuarioId) {
  const r = await db.query('SELECT id FROM perfiles_prestadores WHERE usuario_id = $1', [usuarioId]);
  return r.rows.length ? r.rows[0].id : null;
}

// Mi suscripcion mas reciente
router.get('/mia', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  try {
    const perfilId = await perfilDe(pool, req.usuario.id);
    if (!perfilId) return res.status(404).json({ error: 'Todavia no tienes perfil de prestador' });
    await marcarVencidas(pool, perfilId);
    const r = await pool.query(
      `SELECT ${COLUMNAS} FROM suscripciones WHERE prestador_id = $1 ORDER BY fecha_inicio DESC LIMIT 1`,
      [perfilId]
    );
    if (r.rows.length === 0) return res.status(404).json({ error: 'Todavia no tienes suscripciones' });
    res.json(aSuscripcion(r.rows[0]));
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al consultar tu suscripcion' });
  }
});

// Suscribirme (pago SIMULADO: queda ACTIVA de inmediato)
router.post('/', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const { tipo_plan, renovacion_automatica } = req.body || {};
  if (!Object.prototype.hasOwnProperty.call(PLANES, tipo_plan)) {
    return res.status(400).json({ error: `tipo_plan debe ser uno de: ${Object.keys(PLANES).join(', ')}` });
  }
  if (renovacion_automatica !== undefined && typeof renovacion_automatica !== 'boolean') {
    return res.status(400).json({ error: 'renovacion_automatica debe ser true o false' });
  }
  const client = await pool.connect();
  try {
    const perfilId = await perfilDe(client, req.usuario.id);
    if (!perfilId) return res.status(409).json({ error: 'Primero crea tu perfil de prestador (POST /perfiles)' });

    await client.query('BEGIN');
    await marcarVencidas(client, perfilId);
    // La base solo admite UNA suscripcion PENDIENTE o ACTIVA por prestador (indice unico parcial)
    const vigente = await client.query(
      `SELECT 1 FROM suscripciones WHERE prestador_id = $1 AND estado IN ('PENDIENTE', 'ACTIVA')`,
      [perfilId]
    );
    if (vigente.rows.length > 0) {
      await client.query('ROLLBACK');
      return res.status(409).json({ error: 'Ya tienes una suscripcion pendiente o activa' });
    }
    const r = await client.query(
      `INSERT INTO suscripciones (id, prestador_id, tipo_plan, estado, fecha_inicio, fecha_fin, renovacion_automatica)
       VALUES ($1, $2, $3, 'ACTIVA', NOW(), NOW() + ($4::int * INTERVAL '1 day'), $5)
       RETURNING ${COLUMNAS}`,
      [crypto.randomUUID(), perfilId, tipo_plan, PLANES[tipo_plan], renovacion_automatica ?? false]
    );
    await client.query('COMMIT');
    res.status(201).json({ ...aSuscripcion(r.rows[0]), simulada: true });
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    // Dos peticiones al mismo tiempo: el indice unico de la base frena a la segunda
    if (e.code === '23505') return res.status(409).json({ error: 'Ya tienes una suscripcion pendiente o activa' });
    registrarError(e);
    res.status(500).json({ error: 'Error al crear la suscripcion' });
  } finally {
    client.release();
  }
});

// Cancelar mi suscripcion
router.patch('/:id/cancelar', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  if (!esUuid(req.params.id)) return res.status(400).json({ error: 'Id invalido' });
  try {
    const s = await pool.query(
      `SELECT s.estado, pp.usuario_id
       FROM suscripciones s JOIN perfiles_prestadores pp ON pp.id = s.prestador_id
       WHERE s.id = $1`,
      [req.params.id]
    );
    if (s.rows.length === 0) return res.status(404).json({ error: 'Suscripcion no encontrada' });
    if (String(s.rows[0].usuario_id) !== req.usuario.id) {
      return res.status(403).json({ error: 'Esa suscripcion no es tuya' });
    }
    if (!['ACTIVA', 'PENDIENTE'].includes(s.rows[0].estado)) {
      return res.status(409).json({ error: `No se puede cancelar una suscripcion ${s.rows[0].estado}` });
    }
    const r = await pool.query(
      `UPDATE suscripciones SET estado = 'CANCELADA', renovacion_automatica = false
       WHERE id = $1 RETURNING ${COLUMNAS}`,
      [req.params.id]
    );
    res.json(aSuscripcion(r.rows[0]));
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al cancelar la suscripcion' });
  }
});

module.exports = router;
