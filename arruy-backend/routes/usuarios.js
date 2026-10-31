const express = require('express');
const crypto = require('crypto');
const pool = require('../db');
const { registrarError } = require('../registro');
const { exigirToken } = require('../middleware/auth');
const { normalizarAWebp, sharpDisponible, ErrorImagen, PERFIL } = require('../imagen');
const { config, detectarTipo, subirWebp, urlPublica, borrarReferencias, TIPOS_ENTRADA } = require('../almacenamiento');
const { esUuid, textoValido } = require('../validar');

const router = express.Router();

// Nunca se devuelve password_hash. El correo solo se devuelve en "mis datos" (/yo).
const COLUMNAS = 'id, nombre, telefono, rol, estado_verificacion, foto_perfil, activo';
const MAX_ENTRADA = 5 * 1024 * 1024;

// En la base, foto_perfil guarda la RUTA en el almacenamiento; la app recibe la direccion lista para mostrar
const aUsuario = (u) => ({ ...u, id: String(u.id), telefono: u.telefono ?? '', foto_perfil: urlPublica(u.foto_perfil) });

// El registro y el login estan en /auth (routes/auth.js)

// Mis datos (incluye mi correo)
router.get('/yo', exigirToken, async (req, res) => {
  try {
    const r = await pool.query(`SELECT ${COLUMNAS}, email FROM usuarios WHERE id = $1`, [req.usuario.id]);
    if (r.rows.length === 0) return res.status(404).json({ error: 'Usuario no encontrado' });
    res.json(aUsuario(r.rows[0]));
  } catch (e) {
    registrarError(e);
    res.status(500).json({ error: 'Error al consultar tus datos' });
  }
});

// Editar mis datos: solo se cambian los campos que se envian.
// La foto de perfil se cambia con POST /usuarios/yo/foto; aqui solo se puede QUITAR (foto_perfil: null).
router.patch('/yo', exigirToken, async (req, res) => {
  const b = req.body || {};
  const sets = [];
  const vals = [];
  const poner = (columna, valor) => { vals.push(valor); sets.push(`${columna} = $${vals.length}`); };
  let quitarFoto = false;

  if (b.nombre !== undefined) {
    if (!textoValido(b.nombre, 100)) return res.status(400).json({ error: 'nombre invalido' });
    poner('nombre', b.nombre.trim());
  }
  if (b.telefono !== undefined) {
    if (!textoValido(b.telefono, 20)) return res.status(400).json({ error: 'telefono invalido' });
    poner('telefono', b.telefono.trim());
  }
  if (b.foto_perfil !== undefined) {
    if (b.foto_perfil !== null) {
      return res.status(400).json({ error: 'Para cambiar la foto usa POST /usuarios/yo/foto; aqui solo puedes enviar null para quitarla' });
    }
    quitarFoto = true;
    poner('foto_perfil', null);
  }
  if (sets.length === 0) return res.status(400).json({ error: 'No enviaste ningun campo para cambiar' });

  try {
    const previo = quitarFoto ? await pool.query('SELECT foto_perfil FROM usuarios WHERE id = $1', [req.usuario.id]) : { rows: [] };
    vals.push(req.usuario.id);
    const r = await pool.query(
      `UPDATE usuarios SET ${sets.join(', ')} WHERE id = $${vals.length} RETURNING ${COLUMNAS}`,
      vals
    );
    if (r.rows.length === 0) return res.status(404).json({ error: 'Usuario no encontrado' });
    await borrarReferencias(previo.rows.map((x) => x.foto_perfil));
    res.json(aUsuario(r.rows[0]));
  } catch (e) {
    if (e.code === '23505') return res.status(409).json({ error: 'Ese telefono ya esta registrado' });
    registrarError(e);
    res.status(500).json({ error: 'Error al editar tus datos' });
  }
});

// Subir mi foto de perfil: el cuerpo es la imagen tal cual (JPG, PNG o WEBP, hasta 5 MB).
// Se convierte a WebP (max. 512 px y 200 KB) y se guarda en Supabase; en la base queda solo la ruta.
function prepararFoto(req, res, next) {
  if (!config().ok) return res.status(503).json({ error: 'El almacenamiento de imagenes no esta configurado en el servidor' });
  if (!sharpDisponible()) return res.status(503).json({ error: 'Falta la libreria sharp en el servidor (npm install sharp)' });
  next();
}

router.post('/yo/foto', exigirToken, prepararFoto, express.raw({ type: TIPOS_ENTRADA, limit: MAX_ENTRADA }), async (req, res) => {
  if (!Buffer.isBuffer(req.body) || req.body.length === 0) {
    return res.status(415).json({ error: 'Envia la imagen como cuerpo binario con Content-Type image/jpeg, image/png o image/webp' });
  }
  const tipo = detectarTipo(req.body);
  if (!tipo || tipo.mime !== String(req.headers['content-type']).split(';')[0].trim().toLowerCase()) {
    return res.status(400).json({ error: 'El archivo no es una imagen JPG, PNG o WEBP valida' });
  }
  let img;
  try {
    img = await normalizarAWebp(req.body, PERFIL);
  } catch (e) {
    if (e instanceof ErrorImagen) return res.status(e.status).json({ error: e.message });
    registrarError(e);
    return res.status(500).json({ error: 'Error al procesar la imagen' });
  }
  const ruta = `perfiles/${req.usuario.id}/${crypto.randomUUID()}.webp`;
  try {
    await subirWebp(img.buffer, ruta);
  } catch (e) {
    registrarError(e);
    return res.status(502).json({ error: 'No se pudo guardar la imagen en el almacenamiento' });
  }
  try {
    const previo = await pool.query('SELECT foto_perfil FROM usuarios WHERE id = $1', [req.usuario.id]);
    const r = await pool.query(`UPDATE usuarios SET foto_perfil = $1 WHERE id = $2 RETURNING ${COLUMNAS}`, [ruta, req.usuario.id]);
    if (r.rows.length === 0) {
      await borrarReferencias([ruta]);
      return res.status(404).json({ error: 'Usuario no encontrado' });
    }
    await borrarReferencias(previo.rows.map((x) => x.foto_perfil)); // la foto anterior ya no se usa
    res.json({ ...aUsuario(r.rows[0]), formato: 'webp', ancho: img.ancho, alto: img.alto, bytes: img.bytes, calidad: img.calidad });
  } catch (e) {
    await borrarReferencias([ruta]); // no dejar el archivo huerfano si la base fallo
    registrarError(e);
    res.status(500).json({ error: 'Error al guardar la foto de perfil' });
  }
});

// Eliminar mi cuenta: se borran de verdad mis datos (Google Play no acepta solo desactivarla).
// Orden por las llaves foraneas: calificaciones y suscripciones, servicios (sus fotos y etiquetas
// caen por ON DELETE CASCADE), perfil y usuario. Los archivos de Supabase se borran despues del COMMIT.
router.delete('/yo', exigirToken, async (req, res) => {
  const yo = req.usuario.id;
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const perfiles = (await client.query('SELECT id FROM perfiles_prestadores WHERE usuario_id = $1', [yo])).rows.map((p) => p.id);
    const fotos = await client.query(
      `SELECT f.referencia_imagen FROM fotos_portafolio f JOIN servicios s ON s.id = f.servicio_id
       WHERE s.prestador_id = ANY($1::uuid[])`,
      [perfiles]
    );
    const usuario = await client.query('SELECT foto_perfil FROM usuarios WHERE id = $1', [yo]);
    await client.query(
      'DELETE FROM calificaciones WHERE contratante_id = $1 OR perfil_prestador_id = ANY($2::uuid[])',
      [yo, perfiles]
    );
    await client.query('DELETE FROM suscripciones WHERE prestador_id = ANY($1::uuid[])', [perfiles]);
    await client.query('DELETE FROM servicios WHERE prestador_id = ANY($1::uuid[])', [perfiles]);
    await client.query('DELETE FROM perfiles_prestadores WHERE usuario_id = $1', [yo]);
    await client.query('DELETE FROM usuarios WHERE id = $1', [yo]);
    await client.query('COMMIT');
    await borrarReferencias([...fotos.rows.map((f) => f.referencia_imagen), ...usuario.rows.map((u) => u.foto_perfil)]);
    res.status(204).end();
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    registrarError(e);
    res.status(500).json({ error: 'Error al eliminar tu cuenta' });
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
    registrarError(e);
    res.status(500).json({ error: 'Error al consultar el usuario' });
  }
});

module.exports = router;
