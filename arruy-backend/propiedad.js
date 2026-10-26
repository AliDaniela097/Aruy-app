// Consultas para saber de que usuario es cada cosa (servicio, foto).
// Devuelven el id del usuario dueño, o null si no existe.
async function duenoDeServicio(db, servicioId) {
  const r = await db.query(
    `SELECT pp.usuario_id
     FROM servicios s JOIN perfiles_prestadores pp ON pp.id = s.prestador_id
     WHERE s.id = $1`,
    [servicioId]
  );
  return r.rows.length ? String(r.rows[0].usuario_id) : null;
}

async function duenoDeFoto(db, fotoId) {
  const r = await db.query(
    `SELECT pp.usuario_id
     FROM fotos_portafolio fp
     JOIN servicios s ON s.id = fp.servicio_id
     JOIN perfiles_prestadores pp ON pp.id = s.prestador_id
     WHERE fp.id = $1`,
    [fotoId]
  );
  return r.rows.length ? String(r.rows[0].usuario_id) : null;
}

module.exports = { duenoDeServicio, duenoDeFoto };
