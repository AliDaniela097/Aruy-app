const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { registrarError } = require('../registro');
const { exigirToken, exigirRol } = require('../middleware/auth');
const { duenoDeFoto } = require('../propiedad');
const { borrarReferencias } = require('../almacenamiento');
const { esUuid, limpiarEtiquetas } = require('../validar');

const router = express.Router();

router.get('/:id/etiquetas', async (req, res) => {
  if (!esUuid(req.params.id)) return res.status(400).json({ error: 'Id invalido' });
  try {
    const r = await pool.query('SELECT etiqueta FROM etiquetas_fotos WHERE foto_id = $1', [req.params.id]);
    res.json(r.rows.map((x) => x.etiqueta));
  } catch (e) {
    registrarError(e);
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
    const dueno = await duenoDeFoto(pool, req.params.id);
    if (dueno === null) return res.status(404).json({ error: 'Foto no encontrada' });
    if (dueno !== req.usuario.id) return res.status(403).json({ error: 'Esa foto no es de tu servicio' });
    for (const e of etiquetas) {
      await pool.query(
        'INSERT INTO etiquetas_fotos (id, foto_id, etiqueta) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING',
        [crypto.randomUUID(), req.params.id, e]
      );
    }
    res.status(201).json({ foto_id: req.params.id, etiquetas });
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al guardar etiquetas' });
  }
});

// Borrar una foto de mi servicio (sus etiquetas se borran con ella: ON DELETE CASCADE)
router.delete('/:id', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  if (!esUuid(req.params.id)) return res.status(400).json({ error: 'Id invalido' });
  try {
    const dueno = await duenoDeFoto(pool, req.params.id);
    if (dueno === null) return res.status(404).json({ error: 'Foto no encontrada' });
    if (dueno !== req.usuario.id) return res.status(403).json({ error: 'Esa foto no es de tu servicio' });
    const foto = await pool.query('SELECT referencia_imagen FROM fotos_portafolio WHERE id = $1', [req.params.id]);
    await pool.query('DELETE FROM fotos_portafolio WHERE id = $1', [req.params.id]);
    await borrarReferencias(foto.rows.map((f) => f.referencia_imagen));
    res.status(204).end();
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al borrar la foto' });
  }
});

module.exports = router;
