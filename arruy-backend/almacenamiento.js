// Almacenamiento de imagenes en Supabase Storage (API REST, sin librerias extra).
// La clave secreta SOLO vive en el .env del servidor: la app nunca la ve.
// En la base (fotos_portafolio.referencia_imagen) se guarda solo la RUTA dentro del bucket: <id_servicio>/<id>.webp
const TIPOS_ENTRADA = ['image/jpeg', 'image/png', 'image/webp'];
const RUTA_VALIDA = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\.webp$/i;

function config() {
  // Acepta la URL del proyecto con o sin "/rest/v1/": solo se usa el dominio
  let url = '';
  try {
    url = new URL((process.env.SUPABASE_URL || '').trim()).origin;
  } catch {
    url = '';
  }
  const clave = (process.env.SUPABASE_SECRET_KEY || '').trim();
  const bucket = (process.env.SUPABASE_BUCKET || process.env.SUPABASE_STORAGE_BUCKET || 'aruy-portafolio').trim();
  return { url, clave, bucket, ok: url.startsWith('https://') && clave.length > 0 && /^[\w-]+$/.test(bucket) };
}

// Las claves nuevas (sb_secret_...) no son JWT: solo van en "apikey". Las antiguas van tambien en Authorization.
function cabeceras(clave, extra = {}) {
  const h = { apikey: clave, ...extra };
  if (!clave.startsWith('sb_')) h.Authorization = `Bearer ${clave}`;
  return h;
}

// No se confia en el Content-Type que manda el cliente: se mira el inicio real del archivo
function detectarTipo(buf) {
  if (!Buffer.isBuffer(buf)) return null;
  if (buf.length > 3 && buf[0] === 0xff && buf[1] === 0xd8 && buf[2] === 0xff) return { ext: 'jpg', mime: 'image/jpeg' };
  if (buf.length > 8 && buf.subarray(0, 8).equals(Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]))) {
    return { ext: 'png', mime: 'image/png' };
  }
  if (buf.length > 12 && buf.subarray(0, 4).toString('ascii') === 'RIFF' && buf.subarray(8, 12).toString('ascii') === 'WEBP') {
    return { ext: 'webp', mime: 'image/webp' };
  }
  return null;
}

class ErrorAlmacenamiento extends Error {}

// Sube un WebP a la ruta indicada
async function subirWebp(buffer, ruta) {
  const c = config();
  const r = await fetch(`${c.url}/storage/v1/object/${c.bucket}/${ruta}`, {
    method: 'POST',
    headers: cabeceras(c.clave, { 'Content-Type': 'image/webp', 'Cache-Control': 'max-age=31536000' }),
    body: buffer,
    signal: AbortSignal.timeout(20000),
  });
  if (!r.ok) {
    const detalle = (await r.text().catch(() => '')).slice(0, 200);
    throw new ErrorAlmacenamiento(`Supabase Storage respondio ${r.status}: ${detalle}`);
  }
}

const prefijoPublico = (c) => `${c.url}/storage/v1/object/public/${c.bucket}/`;

// De lo que haya guardado la base (ruta, o una URL de nuestro bucket) devuelve la ruta; si es otra cosa, null
function rutaDeReferencia(ref) {
  if (typeof ref !== 'string') return null;
  const c = config();
  if (c.ok && ref.startsWith(prefijoPublico(c))) {
    const ruta = decodeURIComponent(ref.slice(prefijoPublico(c).length).split('?')[0]);
    return RUTA_VALIDA.test(ruta) ? ruta : null;
  }
  return RUTA_VALIDA.test(ref) ? ref : null;
}

// Direccion publica para mostrar la foto. Las referencias antiguas que ya eran una URL https se devuelven tal cual.
function urlPublica(ref) {
  if (typeof ref === 'string' && ref.startsWith('https://')) return ref;
  const ruta = rutaDeReferencia(ref);
  const c = config();
  return ruta && c.ok ? `${prefijoPublico(c)}${ruta}` : null;
}

// Borra archivos del bucket. "Mejor esfuerzo": si falla, solo avisa en consola.
async function borrarReferencias(referencias) {
  try {
    const c = config();
    if (!c.ok) return;
    const rutas = (referencias || []).map(rutaDeReferencia).filter(Boolean);
    if (rutas.length === 0) return;
    const r = await fetch(`${c.url}/storage/v1/object/${c.bucket}`, {
      method: 'DELETE',
      headers: cabeceras(c.clave, { 'Content-Type': 'application/json' }),
      body: JSON.stringify({ prefixes: rutas }),
      signal: AbortSignal.timeout(20000),
    });
    if (!r.ok) console.warn(`No se pudieron borrar ${rutas.length} imagen(es) de Supabase: ${r.status}`);
  } catch (e) {
    console.warn('No se pudieron borrar imagenes de Supabase:', e.message);
  }
}

module.exports = { config, detectarTipo, subirWebp, urlPublica, rutaDeReferencia, borrarReferencias, ErrorAlmacenamiento, TIPOS_ENTRADA };
