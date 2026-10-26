const jwt = require('jsonwebtoken');

// Guardia de la puerta: revisa que la persona traiga un token valido
function exigirToken(req, res, next) {
  const [tipo, token] = (req.headers.authorization || '').split(' ');
  if (tipo !== 'Bearer' || !token) {
    return res.status(401).json({ error: 'Falta el token (Authorization: Bearer ...)' });
  }
  try {
    const datos = jwt.verify(token, process.env.JWT_SECRET);
    req.usuario = { id: datos.sub, rol: datos.rol };
    next();
  } catch {
    res.status(401).json({ error: 'Token invalido o vencido' });
  }
}

// Revisa que el rol del usuario (PRESTADOR o CONTRATANTE) tenga permiso
function exigirRol(...roles) {
  return (req, res, next) =>
    roles.includes(req.usuario && req.usuario.rol)
      ? next()
      : res.status(403).json({ error: 'No tienes permiso para esta accion' });
}

module.exports = { exigirToken, exigirRol };
