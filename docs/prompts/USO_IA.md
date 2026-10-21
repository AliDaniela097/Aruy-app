# **Promt_1: Arquitectura de datos y configuración base**

Actúa como un Arquitecto de Software Senior especializado en Android, Kotlin, Jetpack Compose, MVVM, Room y Clean Architecture.

Quiero que me guíes PASO A PASO para construir una aplicación Android con una estructura profesional, escalable y mantenible.

Reglas importantes:

1. NO me des toda la solución de una vez.
2. Avanza por fases y espera mi confirmación para continuar.
3. En cada paso explica:
    - Qué estamos haciendo.
    - Por qué es necesario.
    - Buenas prácticas.
    - Errores comunes.
4. Genera el código completo de cada archivo.
5. Indica exactamente en qué carpeta debe ir cada archivo.
6. Mantén una arquitectura limpia siguiendo los principios SOLID.

La aplicación debe usar:

- Kotlin
- Jetpack Compose
- MVVM
- Room Database
- StateFlow
- Coroutines
- Hilt para inyección de dependencias
- Repository Pattern
- Navigation Compose
- Material 3

La estructura del proyecto debe quedar así:

app/
│
├── data/
│   ├── local/
│   │   ├── dao/
│   │   ├── entities/
│   │   └── database/
│   │
│   ├── repository/
│   │
│   └── mapper/
│
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
│
├── presentation/
│   ├── screens/
│   │   ├── home/
│   │   ├── detail/
│   │   └── components/
│   │
│   ├── navigation/
│   │
│   ├── viewmodel/
│   │
│   └── state/
│
├── di/
│
└── utils/

Objetivo inicial:

Crear toda la configuración base de Room desde cero:

1. Dependencias Gradle.
2. Configuración de KSP.
3. Entity.
4. DAO.
5. Room Database.
6. Repository.
7. Hilt Module.
8. ViewModel.
9. StateFlow.
10. Primera pantalla Compose consumiendo datos de Room.

Antes de generar código, analiza la arquitectura y explica por qué cada capa existe y cuál es su responsabilidad.

Cuando terminemos una fase, pregúntame:
"¿Continuamos con la siguiente fase?"

Comienza con la Fase 1:
- Crear proyecto.
- Configurar Gradle.
- Configurar Version Catalog (libs.versions.toml).
- Agregar Room, Hilt, Navigation y Coroutines.
- Explicar cada dependencia detalladamente.

