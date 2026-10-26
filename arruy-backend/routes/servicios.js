const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { exigirToken, exigirRol } = require('../middleware/auth');
const { duenoDeServicio } = require('../propiedad');
const { esUuid, textoValido, esUrlHttps, limpiarEtiquetas, CATEGORIAS } = require('../validar');

const router = express.Router();

const COLUMNAS = 'id, prestador_id, titulo, descripcion, categoria, precio_referencial, disponible';
const MAX_FOTOS = 10;

function aServicio(f) {
  return {
    ...f,
    id: String(f.id),
    prestador_id: String(f.prestador_id),
    precio_referencial: f.precio_referencial === null ? null : Number(f.precio_referencial),
  };
}

// Valida y limpia una foto { referencia_imagen, descripcion?, etiquetas_ia? }. Devuelve null si es invalida.
function limpiarFoto(f) {
  if (!f || typeof f !== 'object') return null;
  const etiquetas = limpiarEtiquetas(f.etiquetas_ia);
  const descripcionOk = f.descripcion == null || textoValido(f.descripcion, 500);
  if (!esUrlHttps(f.referencia_imagen) || etiquetas === null || !descripcionOk) return null;
  return {
    ref: f.referencia_imagen.trim(),
    descripcion: f.descripcion == null ? null : f.descripcion.trim(),
    etiquetas,
  };
}

async function guardarFoto(client, servicioId, foto, orden) {
  const fotoId = crypto.randomUUID();
  await client.query(
    `INSERT INTO fotos_portafolio (id, servicio_id, referencia_imagen, descripcion, orden, fecha_creacion)
     VALUES ($1, $2, $3, $4, $5, NOW())`,
    [fotoId, servicioId, foto.ref, foto.descripcion, orden]
  );
  for (const et of foto.etiquetas) {
    await client.query(
      'INSERT INTO etiquetas_fotos (id, foto_id, etiqueta) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING',
      [crypto.randomUUID(), fotoId, et]
    );
  }
  return { id: fotoId, referencia_imagen: foto.ref, descripcion: foto.descripcion, orden, etiquetas: foto.etiquetas };
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

// ---- Escritura: solo un PRESTADOR con sesion iniciada, y solo sobre lo suyo ----

router.post('/', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const b = req.body || {};
  const fotos = b.fotos === undefined ? [] : b.fotos;

  if (!textoValido(b.titulo, 150)) return res.status(400).json({ error: 'titulo es obligatorio' });
  if (!textoValido(b.descripcion, 2000)) return res.status(400).json({ error: 'descripcion es obligatoria' });
  if (!CATEGORIAS.includes(b.categoria)) {
    return res.status(400).json({ error: `categoria debe ser una de: ${CATEGORIAS.join(', ')}` });
  }
  const p = b.precio_referencial;
  if (p != null && !(typeof p === 'number' && p >= 0)) {
    return res.status(400).json({ error: 'precio_referencial debe ser un numero mayor o igual a 0' });
  }
  if (!Array.isArray(fotos) || fotos.length > MAX_FOTOS) {
    return res.status(400).json({ error: `fotos debe ser una lista de maximo ${MAX_FOTOS}` });
  }
  const fotosLimpias = fotos.map(limpiarFoto);
  if (fotosLimpias.includes(null)) {
    return res.status(400).json({
      error: 'cada foto necesita referencia_imagen (direccion https) y etiquetas_ia validas',
    });
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
      guardadas.push(await guardarFoto(client, servicioId, fotosLimpias[i], i));
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

// Editar mi servicio: solo se cambian los campos que se envian
router.patch('/:id', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const { id } = req.params;
  if (!esUuid(id)) return res.status(400).json({ error: 'Id invalido' });
  const b = req.body || {};
  const sets = [];
  const vals = [];
  const poner = (columna, valor) => { vals.push(valor); sets.push(`${columna} = $${vals.length}`); };

  if (b.titulo !== undefined) {
    if (!textoValido(b.titulo, 150)) return res.status(400).json({ error: 'titulo invalido' });
    poner('titulo', b.titulo.trim());
  }
  if (b.descripcion !== undefined) {
    if (!textoValido(b.descripcion, 2000)) return res.status(400).json({ error: 'descripcion invalida' });
    poner('descripcion', b.descripcion.trim());
  }
  if (b.categoria !== undefined) {
    if (!CATEGORIAS.includes(b.categoria)) {
      return res.status(400).json({ error: `categoria debe ser una de: ${CATEGORIAS.join(', ')}` });
    }
    poner('categoria', b.categoria);
  }
  if (b.precio_referencial !== undefined) {
    const p = b.precio_referencial;
    if (p !== null && !(typeof p === 'number' && p >= 0)) {
      return res.status(400).json({ error: 'precio_referencial debe ser un numero mayor o igual a 0, o null' });
    }
    poner('precio_referencial', p);
  }
  if (b.disponible !== undefined) {
    if (typeof b.disponible !== 'boolean') return res.status(400).json({ error: 'disponible debe ser true o false' });
    poner('disponible', b.disponible);
  }
  if (sets.length === 0) {
    return res.status(400).json({ error: 'No enviaste ningun campo para cambiar' });
  }

  try {
    const dueno = await duenoDeServicio(pool, id);
    if (dueno === null) return res.status(404).json({ error: 'Servicio no encontrado' });
    if (dueno !== req.usuario.id) return res.status(403).json({ error: 'Ese servicio no es tuyo' });
    vals.push(id);
    const r = await pool.query(
      `UPDATE servicios SET ${sets.join(', ')} WHERE id = $${vals.length} RETURNING ${COLUMNAS}`,
      vals
    );
    res.json(aServicio(r.rows[0]));
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al editar el servicio' });
  }
});

// Borrar mi servicio (tambien se borran sus fotos y etiquetas: ON DELETE CASCADE en la base)
router.delete('/:id', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const { id } = req.params;
  if (!esUuid(id)) return res.status(400).json({ error: 'Id invalido' });
  try {
    const dueno = await duenoDeServicio(pool, id);
    if (dueno === null) return res.status(404).json({ error: 'Servicio no encontrado' });
    if (dueno !== req.usuario.id) return res.status(403).json({ error: 'Ese servicio no es tuyo' });
    await pool.query('DELETE FROM servicios WHERE id = $1', [id]);
    res.status(204).end();
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al borrar el servicio' });
  }
});

// Agregar una foto (con sus etiquetas de IA) a un servicio ya publicado
router.post('/:id/fotos', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const { id } = req.params;
  if (!esUuid(id)) return res.status(400).json({ error: 'Id invalido' });
  const foto = limpiarFoto(req.body);
  if (!foto) {
    return res.status(400).json({ error: 'Envia referencia_imagen (direccion https) y etiquetas_ia validas' });
  }
  const client = await pool.connect();
  try {
    const dueno = await duenoDeServicio(client, id);
    if (dueno === null) return res.status(404).json({ error: 'Servicio no encontrado' });
    if (dueno !== req.usuario.id) return res.status(403).json({ error: 'Ese servicio no es tuyo' });

    const c = await client.query(
      'SELECT COUNT(*)::int AS n, COALESCE(MAX(orden), -1) AS maximo FROM fotos_portafolio WHERE servicio_id = $1',
      [id]
    );
    if (c.rows[0].n >= MAX_FOTOS) {
      return res.status(409).json({ error: `Un servicio admite como maximo ${MAX_FOTOS} fotos` });
    }
    await client.query('BEGIN');
    const guardada = await guardarFoto(client, id, foto, Number(c.rows[0].maximo) + 1);
    await client.query('COMMIT');
    res.status(201).json(guardada);
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    console.error(e.message);
    res.status(500).json({ error: 'Error al agregar la foto' });
  } finally {
    client.release();
  }
});

module.exports = router;
