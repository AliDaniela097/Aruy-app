const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const CATEGORIAS = ['PLOMERIA', 'ELECTRICIDAD', 'ALBANILERIA', 'PINTURA', 'CARPINTERIA', 'BELLEZA', 'OTROS'];
const ROLES = ['PRESTADOR', 'CONTRATANTE'];

const esUuid = (v) => typeof v === 'string' && UUID.test(v);
const textoValido = (v, max) =>
  typeof v === 'string' && v.trim().length > 0 && v.trim().length <= max;

function limpiarEtiquetas(lista) {
  if (lista === undefined) return [];
  if (!Array.isArray(lista) || lista.length > 20) return null;
  if (!lista.every((e) => textoValido(e, 50))) return null;
  return [...new Set(lista.map((e) => e.trim()))];
}

module.exports = { esUuid, textoValido, limpiarEtiquetas, CATEGORIAS, ROLES };