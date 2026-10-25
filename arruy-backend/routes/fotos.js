const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
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

router.post('/:id/etiquetas', async (req, res) => {
  const etiquetas = limpiarEtiquetas((req.body || {}).etiquetas);
  if (!esUuid(req.params.id)) return res.status(400).json({ error: 'Id invalido' });
  if (!etiquetas || etiquetas.length === 0) {
    return res.status(400).json({ error: 'Envia etiquetas: lista de textos (maximo 20)' });
  }
  try {
    const f = await pool.query('SELECT 1 FROM fotos_portafolio WHERE id = $1', [req.params.id]);
    if (f.rows.length === 0) return res.status(404).json({ error: 'Foto no encontrada' });
    for (const e of etiquetas) {
      await pool.query(
        'INSERT INTO etiquetas_fotos (id, foto_id, etiqueta) VALUES ($1, $2, $3)',
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