const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { exigirToken, exigirRol } = require('../middleware/auth');
const { esUuid, textoValido, limpiarEtiquetas, CATEGORIAS } = require('../validar');

const router = express.Router();

const COLUMNAS = 'id, prestador_id, titulo, descripcion, categoria, precio_referencial, disponible';

function aServicio(f) {
  return {
    ...f,
    id: String(f.id),
    prestador_id: String(f.prestador_id),
    precio_referencial: f.precio_referencial === null ? null : Number(f.precio_referencial),
  };
}

// ---- Lecturas: publicas (el catalogo lo puede ver cualquiera) ----

router.get('/', async (req, res) => {
  const { prestador_id } = req.query;
  if (prestador_id !== undefined && !esUuid(prestador_id)) {
    return res.status(400).json({ error: 'prestador_id invalido' });
  }
  try {
    const r = prestador_id
      ? await pool.query(`SELECT ${COLUMNAS} FROM servicios WHERE prestador_id = $1 ORDER BY titulo`, [prestador_id])
      : await pool.query(`SELECT ${COLUMNAS} FROM servicios ORDER BY titulo`);
    res.json(r.rows.map(aServicio));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al consultar servicios' });
  }
});

router.get('/:id/fotos', async (req, res) => {
  if (!esUuid(req.params.id)) return res.status(400).json({ error: 'Id invalido' });
  try {
    const f = await pool.query(
      `SELECT id, referencia_imagen, descripcion, orden
       FROM fotos_portafolio WHERE servicio_id = $1 ORDER BY orden`,
      [req.params.id]
    );
    const ids = f.rows.map((x) => x.id);
    const e = ids.length
      ? await pool.query('SELECT foto_id, etiqueta FROM etiquetas_fotos WHERE foto_id = ANY($1::uuid[])', [ids])
      : { rows: [] };
    res.json(
      f.rows.map((x) => ({
        ...x,
        id: String(x.id),
        etiquetas: e.rows.filter((t) => t.foto_id === x.id).map((t) => t.etiqueta),
      }))
    );
  } catch (err) {
    console.error(err.message);
    res.status(500).json({ error: 'Error al consultar las fotos' });
  }
});

router.get('/:id', async (req, res) => {
  if (!esUuid(req.params.id)) return res.status(400).json({ error: 'Id invalido' });
  try {
    const r = await pool.query(`SELECT ${COLUMNAS} FROM servicios WHERE id = $1`, [req.params.id]);
    if (r.rows.length === 0) return res.status(404).json({ error: 'Servicio no encontrado' });
    res.json(aServicio(r.rows[0]));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al consultar el servicio' });
  }
});

// ---- Escritura: solo un PRESTADOR con sesion iniciada ----

router.post('/', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const b = req.body || {};
  const fotos = b.fotos === undefined ? [] : b.fotos;
  // El prestador sale del token, nunca del cuerpo de la peticion

  if (!textoValido(b.titulo, 150)) return res.status(400).json({ error: 'titulo es obligatorio' });
  if (!textoValido(b.descripcion, 2000)) return res.status(400).json({ error: 'descripcion es obligatoria' });
  if (!CATEGORIAS.includes(b.categoria)) {
    return res.status(400).json({ error: `categoria debe ser una de: ${CATEGORIAS.join(', ')}` });
  }
  const p = b.precio_referencial;
  if (p != null && !(typeof p === 'number' && p >= 0)) {
    return res.status(400).json({ error: 'precio_referencial debe ser un numero mayor o igual a 0' });
  }
  if (!Array.isArray(fotos) || fotos.length > 10) {
    return res.status(400).json({ error: 'fotos debe ser una lista de maximo 10' });
  }
  const fotosLimpias = [];
  for (const f of fotos) {
    const etiquetas = limpiarEtiquetas((f || {}).etiquetas_ia);
    if (!f || !textoValido(f.referencia_imagen, 1000) || etiquetas === null) {
      return res.status(400).json({ error: 'cada foto necesita referencia_imagen y etiquetas_ia validas' });
    }
    fotosLimpias.push({ ref: f.referencia_imagen.trim(), descripcion: f.descripcion ?? null, etiquetas });
  }

  const client = await pool.connect();
  try {
    // En la base, servicios.prestador_id apunta al PERFIL del prestador (perfiles_prestadores)
    const perfil = await client.query('SELECT id FROM perfiles_prestadores WHERE usuario_id = $1', [req.usuario.id]);
    if (perfil.rows.length === 0) {
      return res.status(409).json({ error: 'Primero crea tu perfil de prestador (POST /perfiles)' });
    }
    const prestadorId = perfil.rows[0].id;

    await client.query('BEGIN');
    const servicioId = crypto.randomUUID();
    const s = await client.query(
      `INSERT INTO servicios (id, prestador_id, titulo, descripcion, categoria, precio_referencial, disponible, fecha_publicacion)
       VALUES ($1, $2, $3, $4, $5, $6, $7, NOW())
       RETURNING ${COLUMNAS}`,
      [servicioId, prestadorId, b.titulo.trim(), b.descripcion.trim(), b.categoria, p ?? null, b.disponible ?? true]
    );

    const guardadas = [];
    for (let i = 0; i < fotosLimpias.length; i++) {
      const f = fotosLimpias[i];
      const fotoId = crypto.randomUUID();
      await client.query(
        `INSERT INTO fotos_portafolio (id, servicio_id, referencia_imagen, descripcion, orden, fecha_creacion)
         VALUES ($1, $2, $3, $4, $5, NOW())`,
        [fotoId, servicioId, f.ref, f.descripcion, i]
      );
      for (const et of f.etiquetas) {
        await client.query('INSERT INTO etiquetas_fotos (id, foto_id, etiqueta) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING', [
          crypto.randomUUID(), fotoId, et,
        ]);
      }
      guardadas.push({ id: fotoId, referencia_imagen: f.ref, orden: i, etiquetas: f.etiquetas });
    }
    await client.query('COMMIT');
    res.status(201).json({ ...aServicio(s.rows[0]), fotos: guardadas });
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    console.error(e.message);
    res.status(500).json({ error: 'Error al crear el servicio' });
  } finally {
    client.release();
  }
});

module.exports = router;
