const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { exigirToken, exigirRol } = require('../middleware/auth');
const { esUuid, limpiarEtiquetas } = require('../validar');

const router = express.Router();

router.get('/:id/etiquetas', async (req, res) => {
  if (!esUuid(req.params.id)) return res.status(400).json({ error: 'Id invalido' });
  try {
    const r = await pool.query('SELECT etiqueta FROM etiquetas_fotos WHERE foto_id = $1', [req.params.id]);
    res.json(r.rows.map((x) => x.etiqueta));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al consultar etiquetas' });
  }
});

// Solo el PRESTADOR dueno del servicio de la foto puede agregar etiquetas
router.post('/:id/etiquetas', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const etiquetas = limpiarEtiquetas((req.body || {}).etiquetas);
  if (!esUuid(req.params.id)) return res.status(400).json({ error: 'Id invalido' });
  if (!etiquetas || etiquetas.length === 0) {
    return res.status(400).json({ error: 'Envia etiquetas: lista de textos (maximo 20)' });
  }
  try {
    const f = await pool.query(
      `SELECT pp.usuario_id
       FROM fotos_portafolio fp
       JOIN servicios s ON s.id = fp.servicio_id
       JOIN perfiles_prestadores pp ON pp.id = s.prestador_id
       WHERE fp.id = $1`,
      [req.params.id]
    );
    if (f.rows.length === 0) return res.status(404).json({ error: 'Foto no encontrada' });
    if (String(f.rows[0].usuario_id) !== req.usuario.id) {
      return res.status(403).json({ error: 'Esa foto no es de tu servicio' });
    }
    for (const e of etiquetas) {
      await pool.query(
        'INSERT INTO etiquetas_fotos (id, foto_id, etiqueta) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING',
        [crypto.randomUUID(), req.params.id, e]
      );
    }
    res.status(201).json({ foto_id: req.params.id, etiquetas });
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al guardar etiquetas' });
  }
});

module.exports = router;
