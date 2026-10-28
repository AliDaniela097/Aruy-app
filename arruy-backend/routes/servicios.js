const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { registrarError } = require('../registro');
const { exigirToken, exigirRol } = require('../middleware/auth');
const { duenoDeServicio } = require('../propiedad');
const { normalizarAWebp, sharpDisponible, ErrorImagen } = require('../imagen');
const { config, detectarTipo, subirWebp, urlPublica, borrarReferencias, TIPOS_ENTRADA } = require('../almacenamiento');
const { esUuid, textoValido, limpiarEtiquetas, CATEGORIAS } = require('../validar');

const router = express.Router();

const COLUMNAS = 'id, prestador_id, titulo, descripcion, categoria, precio_referencial, disponible';
const MAX_FOTOS = 10;
const MAX_ENTRADA = 5 * 1024 * 1024; // lo que se acepta recibir; lo que se guarda pesa como maximo 500 KB (WebP)

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
    registrarError(e);
    res.status(500).json({ error: 'Error al consultar servicios' });
  }
});

// Fotos de un servicio: "referencia_imagen" es lo guardado en la base (la ruta) y "url" la direccion para mostrarla
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
        url: urlPublica(x.referencia_imagen),
        etiquetas: e.rows.filter((t) => t.foto_id === x.id).map((t) => t.etiqueta),
      }))
    );
  } catch (err) {
    registrarError(err);
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
    registrarError(e);
    res.status(500).json({ error: 'Error al consultar el servicio' });
  }
});

// ---- Escritura: solo un PRESTADOR con sesion iniciada, y solo sobre lo suyo ----

// Publicar un servicio. Las fotos se suben DESPUES, una por una, con POST /servicios/:id/fotos
router.post('/', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const b = req.body || {};
  if (!textoValido(b.titulo, 150)) return res.status(400).json({ error: 'titulo es obligatorio' });
  if (!textoValido(b.descripcion, 2000)) return res.status(400).json({ error: 'descripcion es obligatoria' });
  if (!CATEGORIAS.includes(b.categoria)) {
    return res.status(400).json({ error: `categoria debe ser una de: ${CATEGORIAS.join(', ')}` });
  }
  const p = b.precio_referencial;
  if (p != null && !(typeof p === 'number' && p >= 0)) {
    return res.status(400).json({ error: 'precio_referencial debe ser un numero mayor o igual a 0' });
  }
  if (b.fotos !== undefined && !(Array.isArray(b.fotos) && b.fotos.length === 0)) {
    return res.status(400).json({ error: 'Las fotos se suben despues, una por una, con POST /servicios/{id}/fotos' });
  }
  try {
    // En la base, servicios.prestador_id apunta al PERFIL del prestador (perfiles_prestadores)
    const perfil = await pool.query('SELECT id FROM perfiles_prestadores WHERE usuario_id = $1', [req.usuario.id]);
    if (perfil.rows.length === 0) {
      return res.status(409).json({ error: 'Primero crea tu perfil de prestador (POST /perfiles)' });
    }
    const s = await pool.query(
      `INSERT INTO servicios (id, prestador_id, titulo, descripcion, categoria, precio_referencial, disponible, fecha_publicacion)
       VALUES ($1, $2, $3, $4, $5, $6, $7, NOW())
       RETURNING ${COLUMNAS}`,
      [crypto.randomUUID(), perfil.rows[0].id, b.titulo.trim(), b.descripcion.trim(), b.categoria, p ?? null, b.disponible ?? true]
    );
    res.status(201).json(aServicio(s.rows[0]));
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al crear el servicio' });
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
    registrarError(e);
    res.status(500).json({ error: 'Error al editar el servicio' });
  }
});

// Borrar mi servicio (sus fotos y etiquetas se borran en la base por ON DELETE CASCADE, y sus archivos en Supabase)
router.delete('/:id', exigirToken, exigirRol('PRESTADOR'), async (req, res) => {
  const { id } = req.params;
  if (!esUuid(id)) return res.status(400).json({ error: 'Id invalido' });
  try {
    const dueno = await duenoDeServicio(pool, id);
    if (dueno === null) return res.status(404).json({ error: 'Servicio no encontrado' });
    if (dueno !== req.usuario.id) return res.status(403).json({ error: 'Ese servicio no es tuyo' });
    const fotos = await pool.query('SELECT referencia_imagen FROM fotos_portafolio WHERE servicio_id = $1', [id]);
    await pool.query('DELETE FROM servicios WHERE id = $1', [id]);
    await borrarReferencias(fotos.rows.map((f) => f.referencia_imagen));
    res.status(204).end();
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al borrar el servicio' });
  }
});

// ---- Subir una foto al portafolio de mi servicio ----
// El cuerpo es la imagen tal cual (JPG, PNG o WEBP). El servidor la convierte a WebP (max. 1280 px y 500 KB),
// la guarda en Supabase Storage y registra SOLO su ruta en fotos_portafolio.referencia_imagen.
// Opcional en la direccion: ?etiquetas=Tuberia,Bano (resultado de ML Kit) y ?descripcion=...

class ServicioLleno extends Error {}

// Se ejecuta ANTES de leer la imagen: el servicio debe ser mio y debe haber lugar
async function verificarDuenoYCupo(req, res, next) {
  const { id } = req.params;
  if (!esUuid(id)) return res.status(400).json({ error: 'Id invalido' });
  if (!config().ok) {
    return res.status(503).json({ error: 'El almacenamiento de imagenes no esta configurado en el servidor' });
  }
  if (!sharpDisponible()) {
    return res.status(503).json({ error: 'Falta la libreria sharp en el servidor (npm install sharp)' });
  }
  try {
    const dueno = await duenoDeServicio(pool, id);
    if (dueno === null) return res.status(404).json({ error: 'Servicio no encontrado' });
    if (dueno !== req.usuario.id) return res.status(403).json({ error: 'Ese servicio no es tuyo' });
    const c = await pool.query('SELECT COUNT(*)::int AS n FROM fotos_portafolio WHERE servicio_id = $1', [id]);
    if (c.rows[0].n >= MAX_FOTOS) {
      return res.status(409).json({ error: `Un servicio admite como maximo ${MAX_FOTOS} fotos` });
    }
    next();
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al verificar el servicio' });
  }
}

router.post(
  '/:id/fotos',
  exigirToken,
  exigirRol('PRESTADOR'),
  verificarDuenoYCupo,
  express.raw({ type: TIPOS_ENTRADA, limit: MAX_ENTRADA }),
  async (req, res) => {
    const servicioId = req.params.id;
    if (!Buffer.isBuffer(req.body) || req.body.length === 0) {
      return res.status(415).json({
        error: 'Envia la imagen como cuerpo binario con Content-Type image/jpeg, image/png o image/webp',
      });
    }
    const tipo = detectarTipo(req.body);
    if (!tipo || tipo.mime !== String(req.headers['content-type']).split(';')[0].trim().toLowerCase()) {
      return res.status(400).json({ error: 'El archivo no es una imagen JPG, PNG o WEBP valida' });
    }

    // Etiquetas y descripcion opcionales, validadas antes de procesar o subir nada
    const crudas = [].concat(req.query.etiquetas ?? []).flatMap((x) => String(x).split(',')).map((x) => x.trim()).filter(Boolean);
    const etiquetas = limpiarEtiquetas(crudas.length ? crudas : undefined);
    const descripcion = req.query.descripcion === undefined ? null : req.query.descripcion;
    if (etiquetas === null) {
      return res.status(400).json({ error: 'etiquetas: maximo 20, de hasta 50 caracteres cada una' });
    }
    if (descripcion !== null && !textoValido(descripcion, 500)) {
      return res.status(400).json({ error: 'descripcion invalida (maximo 500 caracteres)' });
    }

    let img;
    try {
      img = await normalizarAWebp(req.body);
    } catch (e) {
      if (e instanceof ErrorImagen) return res.status(e.status).json({ error: e.message });
      registrarError(e);
      return res.status(500).json({ error: 'Error al procesar la imagen' });
    }

    const ruta = `${servicioId}/${crypto.randomUUID()}.webp`;
    try {
      await subirWebp(img.buffer, ruta);
    } catch (e) {
      registrarError(e);
      return res.status(502).json({ error: 'No se pudo guardar la imagen en el almacenamiento' });
    }

    const client = await pool.connect();
    try {
      await client.query('BEGIN');
      const c = await client.query(
        'SELECT COUNT(*)::int AS n, COALESCE(MAX(orden), -1) AS maximo FROM fotos_portafolio WHERE servicio_id = $1',
        [servicioId]
      );
      if (c.rows[0].n >= MAX_FOTOS) throw new ServicioLleno();
      const fotoId = crypto.randomUUID();
      const orden = Number(c.rows[0].maximo) + 1;
      await client.query(
        `INSERT INTO fotos_portafolio (id, servicio_id, referencia_imagen, descripcion, orden, fecha_creacion)
         VALUES ($1, $2, $3, $4, $5, NOW())`,
        [fotoId, servicioId, ruta, descripcion === null ? null : descripcion.trim(), orden]
      );
      for (const et of etiquetas) {
        await client.query(
          'INSERT INTO etiquetas_fotos (id, foto_id, etiqueta) VALUES ($1, $2, $3) ON CONFLICT DO NOTHING',
          [crypto.randomUUID(), fotoId, et]
        );
      }
      await client.query('COMMIT');
      res.status(201).json({
        id: fotoId,
        referencia_imagen: ruta,
        url: urlPublica(ruta),
        descripcion: descripcion === null ? null : descripcion.trim(),
        orden,
        etiquetas,
        formato: 'webp',
        ancho: img.ancho,
        alto: img.alto,
        bytes: img.bytes,
        calidad: img.calidad,
      });
    } catch (e) {
      await client.query('ROLLBACK').catch(() => {});
      await borrarReferencias([ruta]); // no dejar el archivo huerfano si la base fallo
      if (e instanceof ServicioLleno) {
        return res.status(409).json({ error: `Un servicio admite como maximo ${MAX_FOTOS} fotos` });
      }
      registrarError(e);
      res.status(500).json({ error: 'Error al registrar la foto' });
    } finally {
      client.release();
    }
  }
);

module.exports = router;
