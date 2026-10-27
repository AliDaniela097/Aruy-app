# Pedido al backend: perfiles públicos de prestadores

**Para:** equipo de backend (PR #16 y #17) · **De:** frontend Android · **Estado:** pendiente

## Por qué hace falta

En la base real, `servicios.prestador_id` apunta al **perfil** del prestador (`perfiles_prestadores.id`),
no al usuario. Hoy la app no tiene forma de saber, a partir de un servicio:

- el **nombre** y la **foto** del prestador,
- su **teléfono** (para abrir WhatsApp),
- su **calificación promedio** (para ordenar "Mejor valorados"),
- si su cuenta está **verificada** y su **zona**.

Lo que existe solo sirve para el propio prestador (`GET /perfiles/yo`) o para un usuario ya conocido
(`GET /usuarios/:id`, que pide el id del usuario y no el del perfil).

## Endpoints pedidos (públicos, solo lectura)

Igual que `GET /servicios`: sin token. `GET /perfiles/yo` debe seguir definido **antes** de
`GET /perfiles/:id` para que `yo` no se confunda con un id.

### `GET /perfiles`

Lista de todos los prestadores activos (`usuarios.activo = true`). La app lo llama una sola vez junto con
`GET /servicios`, para no hacer una petición por prestador.

### `GET /perfiles/:id`

El mismo objeto, para un solo perfil. `404` si no existe o si el usuario está dado de baja.

### Objeto devuelto

```json
{
  "id": "uuid del perfil (el mismo que servicios.prestador_id)",
  "usuario_id": "uuid del usuario",
  "nombre": "Luis Quishpe",
  "telefono": "0998765432",
  "foto_perfil": "https://… o null",
  "estado_verificacion": "VERIFICADA",
  "oficio_principal": "Plomero",
  "descripcion_profesional": "texto o null",
  "zona_cobertura": "Sur",
  "anios_experiencia": 8,
  "disponible": true,
  "calificacion_promedio": 4.8,
  "total_calificaciones": 12
}
```

- `calificacion_promedio` es `null` y `total_calificaciones` es `0` si aún no hay opiniones
  (mismo cálculo que `GET /calificaciones`: promedio redondeado a un decimal).
- Nunca devolver `email` ni `password_hash`.
- El teléfono ya es público hoy en `GET /usuarios/:id`, así que no cambia la privacidad actual.

## Qué hace la app mientras no exista

- El catálogo carga igual (servicios, fotos y opiniones sí existen), pero los servicios salen como
  "Prestador" sin nombre ni calificación en las listas.
- El botón **Contactar por WhatsApp** queda desactivado con un aviso.
- Cuando el endpoint exista no hay que cambiar nada en la app: usa exactamente los nombres de campo de arriba.

## Otros pedidos pendientes (no bloquean)

- **Foto de perfil:** hoy `foto_perfil` solo acepta una URL https y no hay subida. Hace falta un endpoint tipo
  `POST /usuarios/yo/foto` que reciba la imagen en WebP (mismas reglas que las fotos del portafolio) y
  guarde la ruta en Supabase Storage.
- **Reportes:** el prototipo permite reportar servicios y perfiles; la app los guarda solo en el teléfono
  hasta que exista una tabla y un endpoint para recibirlos.
