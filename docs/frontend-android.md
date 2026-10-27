# Frontend Android de Aruy

App móvil en Kotlin + Jetpack Compose (Material 3) que reemplaza al prototipo HTML (`aruy/`) y consume el backend REST de `arruy-backend/`.

## Cómo correrla

1. **Backend** (`arruy-backend/`): `npm install`, completar `.env` según `.env.example` (`DATABASE_URL`, `JWT_SECRET`, `SUPABASE_URL`, `SUPABASE_SECRET_KEY`, `SUPABASE_STORAGE_BUCKET`; el bucket debe ser público) y `npm start`. Swagger en `http://localhost:3000/docs`.
2. **App**: abrir el proyecto en Android Studio y elegir la variante:
   - `emuladorDebug`: usa `http://10.0.2.2:3000/` (el PC visto desde el emulador).
   - `celularDebug`: usa la IP de tu PC. Se define en `local.properties` (no se sube al repositorio):
     `aruy.baseUrl=http://192.168.0.10:3000/`
3. Desde la terminal: `./gradlew assembleEmuladorDebug`.

HTTP sin cifrar solo está permitido en `debug` (`app/src/debug/`). Para una versión de producción hace falta una URL `https` en `BASE_URL`.

## Arquitectura

MVVM con inyección manual de dependencias.

| Capa | Dónde | Notas |
|---|---|---|
| Pantallas | `ui/screen/` | Compose; una carpeta por área (`auth`, `catalogo`, `prestador`, `perfil`, `sistema`) |
| Componentes | `ui/components/` | Botones, tarjetas, chips, hojas, estados vacíos, etc. del sistema de diseño del prototipo |
| Tema | `ui/theme/` | Colores y tipografías del prototipo (`css/tokens.css`), claro y oscuro |
| Navegación | `ui/navigation/` | `Rutas.kt` (tabla de rutas y guardas por rol) y `AruyApp.kt` (NavHost y barra inferior) |
| ViewModels | `ui/viewmodel/` | Estado de cada pantalla |
| Dominio | `domain/` | Modelos y reglas sin Android: filtros, validaciones, planes Aruy Pro |
| Datos | `data/` | Retrofit (`remote/`), Room y DataStore (`local/`), sesión (`sesion/`), repositorios |
| Utilidades | `util/` | Conversión de fotos a WebP y etiquetas de ML Kit |

`AppContainer` crea las dependencias una sola vez; `AruyApplication` lo conserva mientras la app vive.

### Roles y navegación

Tres perfiles: **contratante**, **prestador** e **invitado** (sin cuenta). La barra inferior y las pantallas permitidas dependen del rol (`rutaPermitida`). El invitado ve el catálogo con datos de relleno desenfocados y se le ofrece crear una cuenta al tocar un servicio.

### Sesión

`SesionManager` es la única fuente de verdad. El token JWT (8 horas) se guarda cifrado con el Android Keystore en DataStore y se excluye de los respaldos. Si el servidor responde 401 a una petición con token, se cierra la sesión y el login avisa "Tu sesión venció".

## Fotos del portafolio (WebP)

1. La persona toma la foto (app de cámara del sistema, sin permiso `CAMERA`) o la elige con el selector del sistema.
2. `ConversorWebp` corrige la orientación EXIF, reduce a 1280 px de lado largo y comprime a WebP con calidad 80, bajando a 75/70/60/50 y luego a 1024/800 px solo si no cabe en 500 KB (`PlanWebp`, las mismas reglas que `arruy-backend/imagen.js`).
3. ML Kit (en el teléfono, sin internet) sugiere etiquetas y categoría.
4. Se sube como cuerpo binario `image/webp` a `POST /servicios/{id}/fotos?etiquetas=...`. El servidor vuelve a validar y guarda en Supabase Storage; en Neon solo queda la ruta.

La app no contiene ninguna clave de Supabase.

## Qué se guarda solo en el teléfono

El backend no ofrece estas funciones, así que viven en Room (por cuenta): favoritos, personas bloqueadas, historial de contactos, reportes y vistos recientemente. **Los reportes no se envían a ningún equipo**; la pantalla lo dice. También: preferencias de Ajustes y el borrador de un servicio nuevo.

## Dependencias con el backend

- `GET /perfiles` y `GET /perfiles/{id}` públicos (nombre, teléfono, calificación del prestador). Ver `docs/backend-pedido-perfiles-publicos.md`. Sin ellos el catálogo funciona, pero sin nombres ni WhatsApp.
- Endpoint de foto de perfil en WebP (hoy `foto_perfil` solo acepta una URL https).
- Tabla y endpoint de reportes.

## Pruebas

```bash
./gradlew testEmuladorDebugUnitTest              # unitarias (JVM, sin dispositivo)
./gradlew connectedEmuladorDebugAndroidTest      # instrumentadas (emulador o celular conectado)
```

- Unitarias: validaciones, filtros del catálogo, plan de WebP, sesión, repositorios y mapeo contra `MockWebServer`.
- Instrumentadas: conversión real de fotos a WebP (tamaño, orientación, formato) y ML Kit sin conexión.

## Ramas

Una rama por fase, encadenadas desde `feature/crud-completo`: `fe-01-base-tema` → `fe-02-auth` → `fe-03-catalogo` → `fe-04-prestador-fotos` → `fe-05-perfil-ajustes-pro` → `fe-06-pulido`.
