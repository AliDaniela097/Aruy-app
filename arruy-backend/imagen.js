// Normaliza cualquier foto (JPG, PNG o WEBP) a WebP: maximo 1280 px por el lado largo, maximo 500 KB.
// Usa "sharp". Si la libreria no esta instalada, el resto del servidor funciona igual
// y la subida de fotos responde 503 con el aviso de instalarla.
const LADO_MAX = 1280;
const MAX_BYTES = 500 * 1024;
const MAX_PIXELES = 40_000_000; // protege la memoria: rechaza imagenes gigantes (ej. mas de 6000 x 6000)
const CALIDADES = [80, 75, 70, 60, 50]; // se empieza en 80 y solo se baja si no cabe en 500 KB
const LADOS = [LADO_MAX, 1024, 800];

class ErrorImagen extends Error {
  constructor(status, mensaje) {
    super(mensaje);
    this.status = status;
  }
}

let sharpCargada;
function cargarSharp() {
  if (sharpCargada === undefined) {
    try {
      sharpCargada = require('sharp');
    } catch {
      sharpCargada = null;
    }
  }
  return sharpCargada;
}

const sharpDisponible = () => cargarSharp() !== null;

async function normalizarAWebp(buffer) {
  const sharp = cargarSharp();
  if (!sharp) {
    throw new ErrorImagen(503, 'Falta la libreria sharp en el servidor (npm install sharp)');
  }
  const abrir = () => sharp(buffer, { limitInputPixels: MAX_PIXELES, failOn: 'error' });

  // Formato y dimensiones REALES, leidos de la imagen (no de lo que diga el cliente)
  let meta;
  try {
    meta = await abrir().metadata();
  } catch {
    throw new ErrorImagen(400, 'El archivo no es una imagen valida');
  }
  if (!['jpeg', 'png', 'webp'].includes(meta.format) || !meta.width || !meta.height) {
    throw new ErrorImagen(400, 'La imagen debe ser JPG, PNG o WEBP');
  }

  for (const lado of LADOS) {
    for (const calidad of CALIDADES) {
      let salida;
      try {
        salida = await abrir()
          .rotate() // respeta la orientacion de la foto; el WebP resultante no conserva EXIF (ni la ubicacion GPS)
          .resize({ width: lado, height: lado, fit: 'inside', withoutEnlargement: true })
          .webp({ quality: calidad, effort: 4 })
          .toBuffer({ resolveWithObject: true });
      } catch {
        throw new ErrorImagen(400, 'No se pudo procesar la imagen');
      }
      if (salida.data.length <= MAX_BYTES) {
        return {
          buffer: salida.data,
          ancho: salida.info.width,
          alto: salida.info.height,
          bytes: salida.data.length,
          calidad,
        };
      }
    }
  }
  throw new ErrorImagen(422, 'No se pudo reducir la imagen a 500 KB; prueba con una foto mas pequena');
}

module.exports = { normalizarAWebp, sharpDisponible, ErrorImagen, LADO_MAX, MAX_BYTES };
