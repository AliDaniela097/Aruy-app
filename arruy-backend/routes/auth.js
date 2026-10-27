const express = require('express');
const crypto = require('crypto');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../db');
const { textoValido, esEmail, passwordValida, ROLES } = require('../validar');
const { urlPublica } = require('../almacenamiento');

const router = express.Router();

const COLUMNAS_PUBLICAS = 'id, nombre, telefono, rol, estado_verificacion, foto_perfil, activo';

// Se compara contra este hash cuando el correo no existe, para que tarde lo mismo
const HASH_FALSO = bcrypt.hashSync('contrasena-falsa-para-igualar-tiempos', 10);

const aUsuario = (u) => ({ ...u, id: String(u.id), telefono: u.telefono ?? '', foto_perfil: urlPublica(u.foto_perfil) });

function crearToken(u) {
  return jwt.sign({ rol: u.rol }, process.env.JWT_SECRET, {
    subject: String(u.id),
    expiresIn: '8h',
  });
}

// Registro. Si el rol es PRESTADOR tambien se crea su perfil (perfiles_prestadores),
// porque en la base los servicios cuelgan del perfil y no del usuario.
router.post('/registro', async (req, res) => {
  const {
    nombre, telefono, email, password, rol,
    oficio_principal, zona_cobertura, descripcion_profesional, anios_experiencia,
  } = req.body || {};

  if (!textoValido(nombre, 100)) return res.status(400).json({ error: 'nombre es obligatorio' });
  if (!textoValido(telefono, 20)) return res.status(400).json({ error: 'telefono es obligatorio' });
  if (!esEmail(email)) return res.status(400).json({ error: 'email invalido' });
  if (!passwordValida(password)) {
    return res.status(400).json({ error: 'password debe tener al menos 8 caracteres' });
  }
  if (!ROLES.includes(rol)) {
    return res.status(400).json({ error: 'rol debe ser PRESTADOR o CONTRATANTE' });
  }
  if (rol === 'PRESTADOR') {
    if (!textoValido(oficio_principal, 100)) {
      return res.status(400).json({ error: 'oficio_principal es obligatorio para un PRESTADOR' });
    }
    if (!textoValido(zona_cobertura, 100)) {
      return res.status(400).json({ error: 'zona_cobertura es obligatoria para un PRESTADOR' });
    }
    if (descripcion_profesional != null && !textoValido(descripcion_profesional, 2000)) {
      return res.status(400).json({ error: 'descripcion_profesional invalida' });
    }
    if (anios_experiencia != null &&
        !(Number.isInteger(anios_experiencia) && anios_experiencia >= 0 && anios_experiencia <= 80)) {
      return res.status(400).json({ error: 'anios_experiencia debe ser un entero entre 0 y 80' });
    }
  }

  const client = await pool.connect();
  try {
    const correo = email.trim().toLowerCase();
    const existe = await client.query('SELECT 1 FROM usuarios WHERE lower(email) = $1', [correo]);
    if (existe.rows.length > 0) {
      return res.status(409).json({ error: 'Ese correo ya esta registrado' });
    }
    const hash = await bcrypt.hash(password, 10);

    await client.query('BEGIN');
    const r = await client.query(
      `INSERT INTO usuarios
         (id, nombre, telefono, rol, estado_verificacion, foto_perfil, activo, fecha_registro, email, password_hash)
       VALUES ($1, $2, $3, $4, 'NO_SOLICITADA', $5, true, NOW(), $6, $7)
       RETURNING ${COLUMNAS_PUBLICAS}`,
      [crypto.randomUUID(), nombre.trim(), telefono.trim(), rol, null, correo, hash]
    );
    const u = r.rows[0];

    let perfilId = null;
    if (rol === 'PRESTADOR') {
      perfilId = crypto.randomUUID();
      await client.query(
        `INSERT INTO perfiles_prestadores
           (id, usuario_id, oficio_principal, descripcion_profesional, zona_cobertura, anios_experiencia, disponible)
         VALUES ($1, $2, $3, $4, $5, $6, true)`,
        [
          perfilId, u.id, oficio_principal.trim(),
          descripcion_profesional == null ? null : descripcion_profesional.trim(),
          zona_cobertura.trim(), anios_experiencia ?? null,
        ]
      );
    }
    await client.query('COMMIT');
    res.status(201).json({ token: crearToken(u), usuario: aUsuario(u), perfil_prestador_id: perfilId });
  } catch (e) {
    await client.query('ROLLBACK').catch(() => {});
    if (e.code === '23505') {
      return res.status(409).json({ error: 'El telefono o el correo ya estan registrados' });
    }
    console.error(e.message);
    res.status(500).json({ error: 'Error al registrar el usuario' });
  } finally {
    client.release();
  }
});

router.post('/login', async (req, res) => {
  const { email, password } = req.body || {};
  if (!esEmail(email) || typeof password !== 'string' || password.length === 0) {
    return res.status(400).json({ error: 'email y password son obligatorios' });
  }
  try {
    const r = await pool.query(
      `SELECT ${COLUMNAS_PUBLICAS}, password_hash FROM usuarios WHERE lower(email) = $1`,
      [email.trim().toLowerCase()]
    );
    const u = r.rows[0];
    const coincide = await bcrypt
      .compare(password, u ? u.password_hash : HASH_FALSO)
      .catch(() => false);
    if (!u || !coincide || !u.activo) {
      return res.status(401).json({ error: 'Correo o contrasena incorrectos' });
    }
    delete u.password_hash;
    const p = await pool.query('SELECT id FROM perfiles_prestadores WHERE usuario_id = $1', [u.id]);
    res.json({
      token: crearToken(u),
      usuario: aUsuario(u),
      perfil_prestador_id: p.rows[0] ? String(p.rows[0].id) : null,
    });
  } catch (e) {
    console.error(e.message);
    res.status(500).json({ error: 'Error al iniciar sesion' });
  }
});

module.exports = router;
