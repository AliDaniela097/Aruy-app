// Documentación de la API (OpenAPI 3). Se ve y se prueba en http://localhost:3000/docs
const errorRespuesta = (descripcion) => ({
  description: descripcion,
  content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } },
});

const idPath = (descripcion) => ({
  name: 'id',
  in: 'path',
  required: true,
  description: descripcion,
  schema: { type: 'string', format: 'uuid' },
});

const conToken = [{ bearerAuth: [] }];

const CATEGORIAS = ['PLOMERIA', 'ELECTRICIDAD', 'ALBANILERIA', 'PINTURA', 'CARPINTERIA', 'BELLEZA', 'OTROS'];

const info = {
  title: 'Aruy API',
  version: '1.0.0',
  description:
    'API REST de Aruy: conecta a trabajadores independientes de oficios en Quito con personas que buscan servicios.\n\n' +
    '**Cómo probar las rutas protegidas:**\n' +
    '1. Ejecuta `POST /auth/registro` (o `POST /auth/login` si ya tienes cuenta).\n' +
    '2. Copia el valor de `token` de la respuesta.\n' +
    '3. Pulsa el botón **Authorize** (arriba a la derecha), pega **solo el token** (sin la palabra Bearer) y pulsa Authorize.\n' +
    '4. Las rutas con candado ya se pueden probar.\n\n' +
    'Las consultas del catálogo (GET) son públicas y no necesitan token.',
};

const tags = [
  { name: 'Salud', description: 'Comprobar que el servidor funciona' },
  { name: 'Autenticación', description: 'Registro e inicio de sesión' },
  { name: 'Usuarios', description: 'Consulta de usuarios' },
  { name: 'Perfiles', description: 'Perfil del prestador (oficio y zona)' },
  { name: 'Servicios', description: 'Catálogo de servicios y su publicación' },
  { name: 'Fotos', description: 'Etiquetas de las fotos del portafolio (IA)' },
];

const ref = (nombre) => ({ $ref: `#/components/schemas/${nombre}` });
const json = (schema) => ({ 'application/json': { schema } });

const paths = {
  '/health': {
    get: {
      tags: ['Salud'],
      summary: 'Estado del servidor',
      responses: {
        200: {
          description: 'El servidor está funcionando',
          content: json({ type: 'object', properties: { estado: { type: 'string', example: 'ok' } } }),
        },
      },
    },
  },

  '/auth/registro': {
    post: {
      tags: ['Autenticación'],
      summary: 'Crear una cuenta',
      description:
        'Si el rol es `PRESTADOR` también se crea su perfil, por eso son obligatorios `oficio_principal` y `zona_cobertura`. ' +
        'Para `CONTRATANTE` se ignoran. Límite: 20 intentos cada 15 minutos.',
      requestBody: {
        required: true,
        content: {
          'application/json': {
            schema: ref('Registro'),
            examples: {
              prestador: {
                summary: 'Prestador',
                value: {
                  nombre: 'PRUEBA Prestador',
                  telefono: '0991234567',
                  email: 'prestador@example.com',
                  password: 'ClaveSegura123',
                  rol: 'PRESTADOR',
                  oficio_principal: 'Plomero',
                  zona_cobertura: 'Norte de Quito',
                  anios_experiencia: 3,
                },
              },
              contratante: {
                summary: 'Contratante',
                value: {
                  nombre: 'PRUEBA Cliente',
                  telefono: '0997654321',
                  email: 'cliente@example.com',
                  password: 'ClaveSegura123',
                  rol: 'CONTRATANTE',
                },
              },
            },
          },
        },
      },
      responses: {
        201: { description: 'Cuenta creada', content: json(ref('RespuestaAuth')) },
        400: errorRespuesta('Datos inválidos o incompletos'),
        409: errorRespuesta('El correo o el teléfono ya están registrados'),
        429: errorRespuesta('Demasiados intentos, intenta más tarde'),
      },
    },
  },

  '/auth/login': {
    post: {
      tags: ['Autenticación'],
      summary: 'Iniciar sesión',
      description: 'Devuelve un token válido por 8 horas. Límite: 20 intentos cada 15 minutos.',
      requestBody: {
        required: true,
        content: {
          'application/json': {
            schema: ref('Login'),
            example: { email: 'prestador@example.com', password: 'ClaveSegura123' },
          },
        },
      },
      responses: {
        200: { description: 'Sesión iniciada', content: json(ref('RespuestaAuth')) },
        400: errorRespuesta('Faltan el correo o la contraseña'),
        401: errorRespuesta('Correo o contraseña incorrectos'),
        429: errorRespuesta('Demasiados intentos, intenta más tarde'),
      },
    },
  },

  '/usuarios/{id}': {
    get: {
      tags: ['Usuarios'],
      summary: 'Consultar un usuario',
      description: 'Público. Nunca devuelve el correo ni la contraseña.',
      parameters: [idPath('Id del usuario')],
      responses: {
        200: { description: 'El usuario', content: json(ref('Usuario')) },
        400: errorRespuesta('Id con formato inválido'),
        404: errorRespuesta('Usuario no encontrado'),
      },
    },
  },

  '/perfiles/yo': {
    get: {
      tags: ['Perfiles'],
      summary: 'Mi perfil de prestador',
      security: conToken,
      responses: {
        200: { description: 'Mi perfil', content: json(ref('Perfil')) },
        401: errorRespuesta('Falta el token o no es válido'),
        403: errorRespuesta('Solo un PRESTADOR puede hacerlo'),
        404: errorRespuesta('Todavía no tienes perfil de prestador'),
      },
    },
  },

  '/perfiles': {
    post: {
      tags: ['Perfiles'],
      summary: 'Crear mi perfil de prestador',
      description:
        'Para prestadores que se registraron antes de que existiera el perfil. Solo se puede crear uno por usuario.',
      security: conToken,
      requestBody: {
        required: true,
        content: {
          'application/json': {
            schema: ref('PerfilNuevo'),
            example: { oficio_principal: 'Plomero', zona_cobertura: 'Norte de Quito', anios_experiencia: 3 },
          },
        },
      },
      responses: {
        201: { description: 'Perfil creado', content: json(ref('Perfil')) },
        400: errorRespuesta('Datos inválidos'),
        401: errorRespuesta('Falta el token o no es válido'),
        403: errorRespuesta('Solo un PRESTADOR puede hacerlo'),
        409: errorRespuesta('Ya tienes un perfil de prestador'),
      },
    },
  },
};

Object.assign(paths, {
  '/servicios': {
    get: {
      tags: ['Servicios'],
      summary: 'Listar servicios',
      description: 'Público. Se puede filtrar por prestador.',
      parameters: [
        {
          name: 'prestador_id',
          in: 'query',
          required: false,
          description: 'Id del PERFIL del prestador (no el id del usuario)',
          schema: { type: 'string', format: 'uuid' },
        },
      ],
      responses: {
        200: { description: 'Lista de servicios', content: json({ type: 'array', items: ref('Servicio') }) },
        400: errorRespuesta('prestador_id con formato inválido'),
      },
    },
    post: {
      tags: ['Servicios'],
      summary: 'Publicar un servicio (con fotos y etiquetas de IA)',
      description:
        'Solo un `PRESTADOR` con perfil. El prestador se toma del token, no del cuerpo. ' +
        'El servicio, sus fotos y sus etiquetas se guardan juntos: si algo falla, no se guarda nada.',
      security: conToken,
      requestBody: {
        required: true,
        content: {
          'application/json': {
            schema: ref('ServicioNuevo'),
            example: {
              titulo: 'PRUEBA plomería',
              descripcion: 'Servicio de prueba',
              categoria: 'PLOMERIA',
              precio_referencial: 25.5,
              fotos: [{ referencia_imagen: 'https://ejemplo.com/foto1.jpg', etiquetas_ia: ['Tuberia', 'Bano'] }],
            },
          },
        },
      },
      responses: {
        201: { description: 'Servicio publicado', content: json(ref('ServicioCreado')) },
        400: errorRespuesta('Datos inválidos'),
        401: errorRespuesta('Falta el token o no es válido'),
        403: errorRespuesta('Solo un PRESTADOR puede publicar'),
        409: errorRespuesta('Primero crea tu perfil de prestador (POST /perfiles)'),
      },
    },
  },

  '/servicios/{id}': {
    get: {
      tags: ['Servicios'],
      summary: 'Consultar un servicio',
      parameters: [idPath('Id del servicio')],
      responses: {
        200: { description: 'El servicio', content: json(ref('Servicio')) },
        400: errorRespuesta('Id con formato inválido'),
        404: errorRespuesta('Servicio no encontrado'),
      },
    },
  },

  '/servicios/{id}/fotos': {
    get: {
      tags: ['Servicios'],
      summary: 'Fotos de un servicio, con sus etiquetas',
      parameters: [idPath('Id del servicio')],
      responses: {
        200: { description: 'Fotos del portafolio', content: json({ type: 'array', items: ref('Foto') }) },
        400: errorRespuesta('Id con formato inválido'),
      },
    },
  },

  '/fotos/{id}/etiquetas': {
    get: {
      tags: ['Fotos'],
      summary: 'Etiquetas de una foto',
      parameters: [idPath('Id de la foto')],
      responses: {
        200: {
          description: 'Lista de etiquetas',
          content: json({ type: 'array', items: { type: 'string' }, example: ['Tuberia', 'Bano'] }),
        },
        400: errorRespuesta('Id con formato inválido'),
      },
    },
    post: {
      tags: ['Fotos'],
      summary: 'Agregar etiquetas a una foto (resultado de ML Kit)',
      description: 'Solo el prestador dueño del servicio de esa foto. Las etiquetas repetidas se ignoran.',
      security: conToken,
      parameters: [idPath('Id de la foto')],
      requestBody: {
        required: true,
        content: {
          'application/json': {
            schema: {
              type: 'object',
              required: ['etiquetas'],
              properties: { etiquetas: { type: 'array', maxItems: 20, items: { type: 'string', maxLength: 50 } } },
            },
            example: { etiquetas: ['Llave', 'Lavamanos'] },
          },
        },
      },
      responses: {
        201: { description: 'Etiquetas guardadas' },
        400: errorRespuesta('Datos inválidos'),
        401: errorRespuesta('Falta el token o no es válido'),
        403: errorRespuesta('Esa foto no es de tu servicio'),
        404: errorRespuesta('Foto no encontrada'),
      },
    },
  },
});

const components = {
  securitySchemes: {
    bearerAuth: {
      type: 'http',
      scheme: 'bearer',
      bearerFormat: 'JWT',
      description: 'Pega el token que devuelve /auth/login o /auth/registro (sin escribir "Bearer").',
    },
  },
  schemas: {
    Error: { type: 'object', properties: { error: { type: 'string', example: 'Id invalido' } } },
    Registro: {
      type: 'object',
      required: ['nombre', 'telefono', 'email', 'password', 'rol'],
      properties: {
        nombre: { type: 'string', maxLength: 100 },
        telefono: { type: 'string', maxLength: 20, description: 'Debe ser único' },
        email: { type: 'string', format: 'email' },
        password: { type: 'string', minLength: 8, description: 'Mínimo 8 caracteres' },
        rol: { type: 'string', enum: ['PRESTADOR', 'CONTRATANTE'] },
        foto_perfil: { type: 'string', nullable: true },
        oficio_principal: { type: 'string', description: 'Obligatorio si el rol es PRESTADOR' },
        zona_cobertura: { type: 'string', description: 'Obligatoria si el rol es PRESTADOR' },
        descripcion_profesional: { type: 'string', nullable: true },
        anios_experiencia: { type: 'integer', minimum: 0, maximum: 80, nullable: true },
      },
    },
    Login: {
      type: 'object',
      required: ['email', 'password'],
      properties: { email: { type: 'string', format: 'email' }, password: { type: 'string' } },
    },
    RespuestaAuth: {
      type: 'object',
      properties: {
        token: { type: 'string', description: 'Úsalo en el botón Authorize' },
        usuario: ref('Usuario'),
        perfil_prestador_id: {
          type: 'string',
          format: 'uuid',
          nullable: true,
          description: 'Solo para prestadores con perfil',
        },
      },
    },
    Usuario: {
      type: 'object',
      properties: {
        id: { type: 'string', format: 'uuid' },
        nombre: { type: 'string' },
        telefono: { type: 'string' },
        rol: { type: 'string', enum: ['PRESTADOR', 'CONTRATANTE'] },
        estado_verificacion: { type: 'string', enum: ['NO_SOLICITADA', 'PENDIENTE', 'VERIFICADA', 'RECHAZADA'] },
        foto_perfil: { type: 'string', nullable: true },
        activo: { type: 'boolean' },
      },
    },
    PerfilNuevo: {
      type: 'object',
      required: ['oficio_principal', 'zona_cobertura'],
      properties: {
        oficio_principal: { type: 'string', maxLength: 100 },
        zona_cobertura: { type: 'string', maxLength: 100 },
        descripcion_profesional: { type: 'string', nullable: true },
        anios_experiencia: { type: 'integer', minimum: 0, maximum: 80, nullable: true },
      },
    },
    Perfil: {
      type: 'object',
      properties: {
        id: { type: 'string', format: 'uuid', description: 'Este es el id que usan los servicios como prestador_id' },
        usuario_id: { type: 'string', format: 'uuid' },
        oficio_principal: { type: 'string' },
        descripcion_profesional: { type: 'string', nullable: true },
        zona_cobertura: { type: 'string' },
        anios_experiencia: { type: 'integer', nullable: true },
        disponible: { type: 'boolean' },
      },
    },
    Servicio: {
      type: 'object',
      properties: {
        id: { type: 'string', format: 'uuid' },
        prestador_id: { type: 'string', format: 'uuid', description: 'Id del PERFIL del prestador' },
        titulo: { type: 'string' },
        descripcion: { type: 'string' },
        categoria: { type: 'string', enum: CATEGORIAS },
        precio_referencial: { type: 'number', nullable: true },
        disponible: { type: 'boolean' },
      },
    },
    FotoNueva: {
      type: 'object',
      required: ['referencia_imagen'],
      properties: {
        referencia_imagen: { type: 'string', maxLength: 1000 },
        descripcion: { type: 'string', nullable: true },
        etiquetas_ia: { type: 'array', maxItems: 20, items: { type: 'string', maxLength: 50 } },
      },
    },
    ServicioNuevo: {
      type: 'object',
      required: ['titulo', 'descripcion', 'categoria'],
      properties: {
        titulo: { type: 'string', maxLength: 150 },
        descripcion: { type: 'string', maxLength: 2000 },
        categoria: { type: 'string', enum: CATEGORIAS },
        precio_referencial: { type: 'number', minimum: 0, nullable: true },
        disponible: { type: 'boolean', default: true },
        fotos: { type: 'array', maxItems: 10, items: ref('FotoNueva') },
      },
    },
    Foto: {
      type: 'object',
      properties: {
        id: { type: 'string', format: 'uuid' },
        referencia_imagen: { type: 'string' },
        descripcion: { type: 'string', nullable: true },
        orden: { type: 'integer' },
        etiquetas: { type: 'array', items: { type: 'string' } },
      },
    },
    ServicioCreado: {
      allOf: [
        ref('Servicio'),
        {
          type: 'object',
          properties: {
            fotos: {
              type: 'array',
              items: {
                type: 'object',
                properties: {
                  id: { type: 'string', format: 'uuid' },
                  referencia_imagen: { type: 'string' },
                  orden: { type: 'integer' },
                  etiquetas: { type: 'array', items: { type: 'string' } },
                },
              },
            },
          },
        },
      ],
    },
  },
};

// ---- CRUD completo: editar, borrar, calificaciones y suscripciones ----
const sinContenido = (descripcion) => ({ description: descripcion });
const e401 = errorRespuesta('Falta el token o no es válido');
const SOLO_PRESTADOR = errorRespuesta('Solo un PRESTADOR puede hacerlo');

paths['/servicios/{id}'].patch = {
  tags: ['Servicios'],
  summary: 'Editar mi servicio',
  description: 'Solo el prestador dueño. Se cambian únicamente los campos que se envían; si no envías ninguno, responde 400.',
  security: conToken,
  parameters: [idPath('Id del servicio')],
  requestBody: {
    required: true,
    content: {
      'application/json': {
        schema: ref('ServicioEditar'),
        example: { titulo: 'PRUEBA plomería 24 horas', precio_referencial: 30, disponible: true },
      },
    },
  },
  responses: {
    200: { description: 'Servicio actualizado', content: json(ref('Servicio')) },
    400: errorRespuesta('Datos inválidos'),
    401: e401,
    403: errorRespuesta('Ese servicio no es tuyo, o no eres PRESTADOR'),
    404: errorRespuesta('Servicio no encontrado'),
  },
};
paths['/servicios/{id}'].delete = {
  tags: ['Servicios'],
  summary: 'Borrar mi servicio',
  description: 'Solo el dueño. También se borran sus fotos y etiquetas (ON DELETE CASCADE en la base).',
  security: conToken,
  parameters: [idPath('Id del servicio')],
  responses: {
    204: sinContenido('Servicio borrado'),
    401: e401,
    403: errorRespuesta('Ese servicio no es tuyo, o no eres PRESTADOR'),
    404: errorRespuesta('Servicio no encontrado'),
  },
};
paths['/servicios/{id}/fotos'].post = {
  tags: ['Servicios'],
  summary: 'Agregar una foto a un servicio ya publicado',
  description:
    'La imagen se sube a Firebase Storage desde la app; aquí solo se guarda su dirección (`referencia_imagen`, debe ser https). Máximo 10 fotos por servicio.',
  security: conToken,
  parameters: [idPath('Id del servicio')],
  requestBody: {
    required: true,
    content: {
      'application/json': {
        schema: ref('FotoNueva'),
        example: {
          referencia_imagen: 'https://firebasestorage.googleapis.com/v0/b/aruy/o/foto2.jpg?alt=media',
          etiquetas_ia: ['Llave', 'Lavamanos'],
        },
      },
    },
  },
  responses: {
    201: { description: 'Foto agregada', content: json(ref('FotoGuardada')) },
    400: errorRespuesta('Datos inválidos'),
    401: e401,
    403: errorRespuesta('Ese servicio no es tuyo, o no eres PRESTADOR'),
    404: errorRespuesta('Servicio no encontrado'),
    409: errorRespuesta('El servicio ya tiene 10 fotos'),
  },
};
paths['/fotos/{id}'] = {
  delete: {
    tags: ['Fotos'],
    summary: 'Borrar una foto de mi servicio',
    description: 'Solo el dueño. Sus etiquetas se borran con ella.',
    security: conToken,
    parameters: [idPath('Id de la foto')],
    responses: {
      204: sinContenido('Foto borrada'),
      401: e401,
      403: errorRespuesta('Esa foto no es de tu servicio'),
      404: errorRespuesta('Foto no encontrada'),
    },
  },
};
paths['/perfiles/yo'].patch = {
  tags: ['Perfiles'],
  summary: 'Editar mi perfil de prestador',
  description: 'El perfil no se borra (la base lo impide si tiene servicios o calificaciones); para ocultarte usa `disponible: false`.',
  security: conToken,
  requestBody: {
    required: true,
    content: {
      'application/json': {
        schema: ref('PerfilEditar'),
        example: { zona_cobertura: 'Quito norte y valles', anios_experiencia: 5, disponible: true },
      },
    },
  },
  responses: {
    200: { description: 'Perfil actualizado', content: json(ref('Perfil')) },
    400: errorRespuesta('Datos inválidos'),
    401: e401,
    403: SOLO_PRESTADOR,
    404: errorRespuesta('Todavía no tienes perfil de prestador'),
  },
};

paths['/usuarios/yo'] = {
  get: {
    tags: ['Usuarios'],
    summary: 'Mis datos (incluye mi correo)',
    security: conToken,
    responses: {
      200: { description: 'Mis datos', content: json(ref('MisDatos')) },
      401: e401,
      404: errorRespuesta('Usuario no encontrado'),
    },
  },
  patch: {
    tags: ['Usuarios'],
    summary: 'Editar mis datos',
    security: conToken,
    requestBody: {
      required: true,
      content: {
        'application/json': {
          schema: ref('UsuarioEditar'),
          example: { nombre: 'Alisson Basantes', telefono: '0991234567' },
        },
      },
    },
    responses: {
      200: { description: 'Datos actualizados', content: json(ref('Usuario')) },
      400: errorRespuesta('Datos inválidos'),
      401: e401,
      409: errorRespuesta('Ese teléfono ya está registrado'),
    },
  },
  delete: {
    tags: ['Usuarios'],
    summary: 'Darme de baja',
    description:
      'No se borra el usuario (la base lo impide si tiene perfil o calificaciones y así se conserva el historial): se marca `activo = false`, ' +
      'su perfil queda no disponible y ya no puede iniciar sesión. Su token actual sigue siendo válido hasta que venza (8 horas).',
    security: conToken,
    responses: { 204: sinContenido('Baja realizada'), 401: e401 },
  },
};

paths['/calificaciones'] = {
  get: {
    tags: ['Calificaciones'],
    summary: 'Calificaciones de un prestador, con promedio',
    description: 'Público.',
    parameters: [
      {
        name: 'perfil_prestador_id',
        in: 'query',
        required: true,
        description: 'Id del PERFIL del prestador',
        schema: { type: 'string', format: 'uuid' },
      },
    ],
    responses: {
      200: { description: 'Promedio y lista', content: json(ref('CalificacionesDePrestador')) },
      400: errorRespuesta('Falta perfil_prestador_id o no es válido'),
    },
  },
  post: {
    tags: ['Calificaciones'],
    summary: 'Calificar a un prestador',
    description: 'Solo un `CONTRATANTE`, una vez por prestador, con puntuación entera de 1 a 5.',
    security: conToken,
    requestBody: {
      required: true,
      content: {
        'application/json': {
          schema: ref('CalificacionNueva'),
          example: { perfil_prestador_id: '00000000-0000-4000-8000-000000000000', puntuacion: 5, comentario: 'Muy puntual' },
        },
      },
    },
    responses: {
      201: { description: 'Calificación guardada', content: json(ref('Calificacion')) },
      400: errorRespuesta('Datos inválidos'),
      401: e401,
      403: errorRespuesta('Solo un CONTRATANTE puede calificar'),
      404: errorRespuesta('Prestador no encontrado'),
      409: errorRespuesta('Ya calificaste a este prestador'),
    },
  },
};

paths['/suscripciones'] = {
  post: {
    tags: ['Suscripciones'],
    summary: 'Suscribirme (pago SIMULADO)',
    description:
      'No hay pago real: queda `ACTIVA` de inmediato. `ARUY_PRO_SEMANAL` dura 7 días y `ARUY_PRO_MENSUAL` 30. Solo una activa a la vez.',
    security: conToken,
    requestBody: {
      required: true,
      content: {
        'application/json': {
          schema: ref('SuscripcionNueva'),
          example: { tipo_plan: 'ARUY_PRO_MENSUAL', renovacion_automatica: false },
        },
      },
    },
    responses: {
      201: { description: 'Suscripción activa', content: json(ref('Suscripcion')) },
      400: errorRespuesta('Plan inválido'),
      401: e401,
      403: SOLO_PRESTADOR,
      409: errorRespuesta('Ya tienes una suscripción activa, o falta tu perfil de prestador'),
    },
  },
};
paths['/suscripciones/mia'] = {
  get: {
    tags: ['Suscripciones'],
    summary: 'Mi suscripción más reciente',
    description: 'Si la fecha de fin ya pasó, se muestra como `VENCIDA`.',
    security: conToken,
    responses: {
      200: { description: 'Mi suscripción', content: json(ref('Suscripcion')) },
      401: e401,
      403: SOLO_PRESTADOR,
      404: errorRespuesta('Todavía no tienes suscripciones'),
    },
  },
};
paths['/suscripciones/{id}/cancelar'] = {
  patch: {
    tags: ['Suscripciones'],
    summary: 'Cancelar mi suscripción',
    security: conToken,
    parameters: [idPath('Id de la suscripción')],
    responses: {
      200: { description: 'Suscripción cancelada', content: json(ref('Suscripcion')) },
      401: e401,
      403: errorRespuesta('Esa suscripción no es tuya, o no eres PRESTADOR'),
      404: errorRespuesta('Suscripción no encontrada'),
      409: errorRespuesta('Ya estaba cancelada o vencida'),
    },
  },
};

tags.push(
  { name: 'Calificaciones', description: 'Opiniones de los contratantes sobre los prestadores' },
  { name: 'Suscripciones', description: 'Plan Aruy Pro (pago simulado)' }
);

Object.assign(components.schemas, {
  ServicioEditar: {
    type: 'object',
    description: 'Envía solo los campos que quieras cambiar',
    properties: {
      titulo: { type: 'string', maxLength: 150 },
      descripcion: { type: 'string', maxLength: 2000 },
      categoria: { type: 'string', enum: CATEGORIAS },
      precio_referencial: { type: 'number', minimum: 0, nullable: true },
      disponible: { type: 'boolean' },
    },
  },
  FotoGuardada: {
    type: 'object',
    properties: {
      id: { type: 'string', format: 'uuid' },
      referencia_imagen: { type: 'string' },
      descripcion: { type: 'string', nullable: true },
      orden: { type: 'integer' },
      etiquetas: { type: 'array', items: { type: 'string' } },
    },
  },
  PerfilEditar: {
    type: 'object',
    description: 'Envía solo los campos que quieras cambiar',
    properties: {
      oficio_principal: { type: 'string', maxLength: 100 },
      zona_cobertura: { type: 'string', maxLength: 100 },
      descripcion_profesional: { type: 'string', nullable: true },
      anios_experiencia: { type: 'integer', minimum: 0, maximum: 80, nullable: true },
      disponible: { type: 'boolean' },
    },
  },
  UsuarioEditar: {
    type: 'object',
    description: 'Envía solo los campos que quieras cambiar',
    properties: {
      nombre: { type: 'string', maxLength: 100 },
      telefono: { type: 'string', maxLength: 20 },
      foto_perfil: { type: 'string', nullable: true, description: 'Dirección https de la imagen' },
    },
  },
  MisDatos: {
    allOf: [ref('Usuario'), { type: 'object', properties: { email: { type: 'string', format: 'email' } } }],
  },
  CalificacionNueva: {
    type: 'object',
    required: ['perfil_prestador_id', 'puntuacion'],
    properties: {
      perfil_prestador_id: { type: 'string', format: 'uuid' },
      puntuacion: { type: 'integer', minimum: 1, maximum: 5 },
      comentario: { type: 'string', maxLength: 1000, nullable: true },
    },
  },
  Calificacion: {
    type: 'object',
    properties: {
      id: { type: 'string', format: 'uuid' },
      perfil_prestador_id: { type: 'string', format: 'uuid' },
      puntuacion: { type: 'integer' },
      comentario: { type: 'string', nullable: true },
      fecha_creacion: { type: 'string', format: 'date-time' },
    },
  },
  CalificacionesDePrestador: {
    type: 'object',
    properties: {
      perfil_prestador_id: { type: 'string', format: 'uuid' },
      promedio: { type: 'number', nullable: true, example: 4.5 },
      total: { type: 'integer' },
      calificaciones: {
        type: 'array',
        items: {
          type: 'object',
          properties: {
            id: { type: 'string', format: 'uuid' },
            puntuacion: { type: 'integer' },
            comentario: { type: 'string', nullable: true },
            fecha_creacion: { type: 'string', format: 'date-time' },
            contratante: { type: 'string', description: 'Nombre de quien calificó' },
          },
        },
      },
    },
  },
  SuscripcionNueva: {
    type: 'object',
    required: ['tipo_plan'],
    properties: {
      tipo_plan: { type: 'string', enum: ['ARUY_PRO_SEMANAL', 'ARUY_PRO_MENSUAL'] },
      renovacion_automatica: { type: 'boolean', default: false },
    },
  },
  Suscripcion: {
    type: 'object',
    properties: {
      id: { type: 'string', format: 'uuid' },
      prestador_id: { type: 'string', format: 'uuid', description: 'Id del PERFIL del prestador' },
      tipo_plan: { type: 'string', enum: ['ARUY_PRO_SEMANAL', 'ARUY_PRO_MENSUAL'] },
      estado: { type: 'string', enum: ['PENDIENTE', 'ACTIVA', 'CANCELADA', 'VENCIDA'] },
      fecha_inicio: { type: 'string', format: 'date-time' },
      fecha_fin: { type: 'string', format: 'date-time', nullable: true },
      renovacion_automatica: { type: 'boolean' },
      simulada: { type: 'boolean', description: 'Solo al crearla: el pago es simulado' },
    },
  },
});

// ---- Imágenes (Supabase Storage) ----
const imagenBinaria = { type: 'string', format: 'binary' };
paths['/imagenes'] = {
  post: {
    tags: ['Imágenes'],
    summary: 'Subir una imagen (foto de trabajo o de perfil)',
    description:
      'El cuerpo es **la imagen tal cual** (JPG, PNG o WEBP, máximo 5 MB), con su `Content-Type`. ' +
      'El servidor la guarda en Supabase Storage y devuelve su dirección pública (`url`). ' +
      'Esa `url` es la que luego se manda en `referencia_imagen` (al publicar un servicio o agregar una foto) o en `foto_perfil`. ' +
      'Se revisa el contenido real del archivo, no solo lo que diga el `Content-Type`.',
    security: conToken,
    requestBody: {
      required: true,
      content: { 'image/jpeg': { schema: imagenBinaria }, 'image/png': { schema: imagenBinaria }, 'image/webp': { schema: imagenBinaria } },
    },
    responses: {
      201: { description: 'Imagen guardada', content: json(ref('ImagenSubida')) },
      400: errorRespuesta('El archivo no es una imagen JPG, PNG o WEBP válida'),
      401: e401,
      413: errorRespuesta('La imagen pesa más de 5 MB'),
      415: errorRespuesta('Falta la imagen o su Content-Type no es image/jpeg, image/png o image/webp'),
      429: errorRespuesta('Demasiadas imágenes en poco tiempo'),
      502: errorRespuesta('No se pudo guardar la imagen en el almacenamiento'),
      503: errorRespuesta('El almacenamiento no está configurado en el servidor'),
    },
  },
};
tags.push({ name: 'Imágenes', description: 'Subida de fotos (se guardan en Supabase Storage; en la base solo va la dirección)' });
components.schemas.ImagenSubida = {
  type: 'object',
  properties: {
    url: { type: 'string', description: 'Dirección pública https de la imagen', example: 'https://xxxx.supabase.co/storage/v1/object/public/aruy-portafolio/ID/foto.jpg' },
    ruta: { type: 'string' },
    tipo: { type: 'string', example: 'image/jpeg' },
    bytes: { type: 'integer' },
  },
};

// ---- Fotos: se suben directo a un servicio mío (WebP en Supabase Storage) ----
// Esta sección reemplaza lo anterior de /imagenes y de las fotos dentro de POST /servicios.
delete paths['/imagenes'];
tags.splice(tags.findIndex((t) => t.name === 'Imágenes'), 1);
delete components.schemas.ImagenSubida;
delete components.schemas.FotoNueva;
delete components.schemas.FotoGuardada;
delete components.schemas.ServicioCreado;
delete components.schemas.ServicioNuevo.properties.fotos;

paths['/servicios'].post.summary = 'Publicar un servicio';
paths['/servicios'].post.description =
  'Solo un `PRESTADOR` con perfil. El prestador se toma del token, no del cuerpo. ' +
  'Las fotos se suben después, una por una, con `POST /servicios/{id}/fotos`.';
paths['/servicios'].post.requestBody.content['application/json'].example = {
  titulo: 'PRUEBA plomería',
  descripcion: 'Servicio de prueba',
  categoria: 'PLOMERIA',
  precio_referencial: 25.5,
};
paths['/servicios'].post.responses[201] = { description: 'Servicio publicado', content: json(ref('Servicio')) };

const imagenBin = { type: 'string', format: 'binary' };
paths['/servicios/{id}/fotos'].post = {
  tags: ['Servicios'],
  summary: 'Subir una foto al portafolio de mi servicio',
  description:
    'El cuerpo es **la imagen tal cual** (JPG, PNG o WEBP, hasta 5 MB), con su `Content-Type`. ' +
    'Primero se comprueba que el servicio sea tuyo y que tenga menos de 10 fotos; recién entonces se lee la imagen. ' +
    'El servidor revisa el formato y las dimensiones reales, la **convierte a WebP** (máximo 1280 px por el lado largo, calidad 80 y, si hace falta para no pasar de 500 KB, menos), ' +
    'la guarda en Supabase Storage y registra en la base **solo su ruta**. No se conserva la ubicación GPS ni el resto de datos EXIF.\n\n' +
    'Opcional en la dirección: `?etiquetas=Tuberia,Bano` (resultado de ML Kit, máximo 20) y `?descripcion=...`.',
  security: conToken,
  parameters: [
    idPath('Id del servicio'),
    { name: 'etiquetas', in: 'query', required: false, description: 'Etiquetas separadas por coma', schema: { type: 'string', example: 'Tuberia,Bano' } },
    { name: 'descripcion', in: 'query', required: false, description: 'Descripción de la foto (máximo 500 caracteres)', schema: { type: 'string' } },
  ],
  requestBody: {
    required: true,
    content: { 'image/webp': { schema: imagenBin }, 'image/jpeg': { schema: imagenBin }, 'image/png': { schema: imagenBin } },
  },
  responses: {
    201: { description: 'Foto guardada', content: json(ref('FotoSubida')) },
    400: errorRespuesta('Datos inválidos, o el archivo no es una imagen válida'),
    401: e401,
    403: errorRespuesta('Ese servicio no es tuyo, o no eres PRESTADOR'),
    404: errorRespuesta('Servicio no encontrado'),
    409: errorRespuesta('El servicio ya tiene 10 fotos'),
    413: errorRespuesta('La imagen pesa más de 5 MB'),
    415: errorRespuesta('Falta la imagen o su Content-Type no es image/jpeg, image/png o image/webp'),
    422: errorRespuesta('No se pudo reducir la imagen a 500 KB'),
    429: errorRespuesta('Demasiadas imágenes en poco tiempo'),
    502: errorRespuesta('No se pudo guardar la imagen en el almacenamiento'),
    503: errorRespuesta('El almacenamiento o el procesamiento de imágenes no están configurados en el servidor'),
  },
};

components.schemas.FotoSubida = {
  type: 'object',
  properties: {
    id: { type: 'string', format: 'uuid' },
    referencia_imagen: { type: 'string', description: 'Lo que se guarda en la base: la ruta dentro del bucket', example: 'ID_SERVICIO/ID_FOTO.webp' },
    url: { type: 'string', description: 'Dirección pública para mostrar la foto', example: 'https://xxxx.supabase.co/storage/v1/object/public/aruy-portafolio/ID_SERVICIO/ID_FOTO.webp' },
    descripcion: { type: 'string', nullable: true },
    orden: { type: 'integer' },
    etiquetas: { type: 'array', items: { type: 'string' } },
    formato: { type: 'string', example: 'webp' },
    ancho: { type: 'integer' },
    alto: { type: 'integer' },
    bytes: { type: 'integer', description: 'Tamaño final (máximo 512000)' },
    calidad: { type: 'integer', description: 'Calidad WebP usada (80 si cupo; baja solo si hacía falta)' },
  },
};
components.schemas.Foto.properties.url = { type: 'string', nullable: true, description: 'Dirección pública para mostrar la foto' };
components.schemas.Foto.properties.referencia_imagen.description = 'Lo guardado en la base: la ruta dentro del bucket';

// ---- Perfil público del prestador y foto de perfil ----
paths['/perfiles/{id}'] = {
  get: {
    tags: ['Perfiles'],
    summary: 'Perfil público de un prestador',
    description:
      'Público (no pide token). El `id` es el `prestador_id` de los servicios, es decir, el id del **perfil**. ' +
      'Devuelve nombre, teléfono, número listo para WhatsApp (`https://wa.me/{whatsapp}`), foto y calificación promedio. ' +
      'Si el prestador se dio de baja, responde 404.',
    parameters: [idPath('Id del perfil del prestador (prestador_id de un servicio)')],
    responses: {
      200: { description: 'Perfil del prestador', content: json(ref('PerfilPublico')) },
      400: errorRespuesta('Id con formato inválido'),
      404: errorRespuesta('Prestador no encontrado'),
    },
  },
};
paths['/usuarios/yo/foto'] = {
  post: {
    tags: ['Usuarios'],
    summary: 'Subir mi foto de perfil',
    description:
      'El cuerpo es **la imagen tal cual** (JPG, PNG o WEBP, hasta 5 MB), con su `Content-Type`. ' +
      'El servidor la convierte a **WebP** (máximo 512 px por el lado largo y 200 KB), la guarda en Supabase Storage y registra solo su ruta. ' +
      'Reemplaza la foto anterior (y borra su archivo). Sin ubicación GPS ni EXIF. Para quitarla: `PATCH /usuarios/yo` con `{"foto_perfil": null}`.',
    security: conToken,
    requestBody: {
      required: true,
      content: { 'image/webp': { schema: imagenBin }, 'image/jpeg': { schema: imagenBin }, 'image/png': { schema: imagenBin } },
    },
    responses: {
      200: { description: 'Foto guardada. `foto_perfil` ya es la dirección para mostrarla', content: json(ref('UsuarioConFoto')) },
      400: errorRespuesta('El archivo no es una imagen válida'),
      401: e401,
      413: errorRespuesta('La imagen pesa más de 5 MB'),
      415: errorRespuesta('Falta la imagen o su Content-Type no es image/jpeg, image/png o image/webp'),
      422: errorRespuesta('No se pudo reducir la imagen a 200 KB'),
      429: errorRespuesta('Demasiadas imágenes en poco tiempo'),
      502: errorRespuesta('No se pudo guardar la imagen en el almacenamiento'),
      503: errorRespuesta('El almacenamiento o el procesamiento de imágenes no están configurados en el servidor'),
    },
  },
};
paths['/usuarios/yo'].patch.description = 'Solo cambia los campos que envíes. La foto de perfil se cambia con `POST /usuarios/yo/foto`; aquí solo se puede quitar enviando `foto_perfil: null`.';
components.schemas.UsuarioEditar.properties.foto_perfil = { type: 'string', nullable: true, enum: [null], description: 'Solo se acepta null (quita la foto). Para ponerla usa POST /usuarios/yo/foto' };
delete components.schemas.Registro.properties.foto_perfil;
components.schemas.Usuario.properties.foto_perfil = { type: 'string', nullable: true, description: 'Dirección pública para mostrar la foto de perfil (WebP), o null' };
components.schemas.UsuarioConFoto = {
  allOf: [
    ref('Usuario'),
    { type: 'object', properties: { formato: { type: 'string', example: 'webp' }, ancho: { type: 'integer' }, alto: { type: 'integer' }, bytes: { type: 'integer' }, calidad: { type: 'integer' } } },
  ],
};
components.schemas.PerfilPublico = {
  type: 'object',
  properties: {
    id: { type: 'string', format: 'uuid', description: 'Id del perfil (= prestador_id de los servicios)' },
    usuario_id: { type: 'string', format: 'uuid' },
    nombre: { type: 'string' },
    telefono: { type: 'string', example: '0991234567' },
    whatsapp: { type: 'string', nullable: true, description: 'Solo dígitos con código de Ecuador, para https://wa.me/{whatsapp}. null si el teléfono no es un celular ecuatoriano', example: '593991234567' },
    foto_perfil: { type: 'string', nullable: true, description: 'Dirección pública de la foto (WebP), o null' },
    estado_verificacion: { type: 'string', enum: ['NO_SOLICITADA', 'PENDIENTE', 'VERIFICADA', 'RECHAZADA'] },
    oficio_principal: { type: 'string' },
    descripcion_profesional: { type: 'string', nullable: true },
    zona_cobertura: { type: 'string' },
    anios_experiencia: { type: 'integer', nullable: true },
    disponible: { type: 'boolean' },
    calificacion_promedio: { type: 'number', nullable: true, description: 'De 1 a 5 con un decimal; null si todavía no tiene calificaciones', example: 4.5 },
    total_calificaciones: { type: 'integer' },
  },
};

module.exports = {
  openapi: '3.0.3',
  info,
  servers: [{ url: '/', description: 'Este mismo servidor' }],
  tags,
  paths,
  components,
};
