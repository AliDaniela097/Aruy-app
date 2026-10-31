// Registro de errores en la consola del servidor. Nunca deja el mensaje vacio:
// los fallos de red a veces llegan sin texto (por ejemplo, un AggregateError al no poder conectar a la base).
function registrarError(e) {
  if (!e) return console.error('Error desconocido');
  const partes = [e.code, e.name && e.name !== 'Error' ? e.name : null, e.message].filter(Boolean);
  const internos = Array.isArray(e.errors) ? e.errors.map((x) => x.code || x.message).filter(Boolean) : [];
  console.error(partes.join(' | ') || String(e), internos.length ? `(${internos.join(', ')})` : '');
}

module.exports = { registrarError };
