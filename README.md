# TeqRuta

Aplicación Android para registrar los traslados del personal técnico de TEQMED SpA, empresa de servicio técnico de equipamiento médico de la Región del Biobío.

Hoy los técnicos anotan sus traslados en una libreta y los declaran a fin de mes. TeqRuta permite registrar cada traslado en el momento en que ocurre (kilometraje, destino, ubicación y foto del odómetro), calcular su costo con tarifas centralizadas y detectar diferencias entre la distancia oficial de un trayecto y la declarada por el técnico.

Proyecto de la asignatura Desarrollo de Aplicaciones Móviles (DSY1105), Duoc UC.

## Autor

- Benjamin Molina

## Estado del proyecto

En desarrollo para la Evaluación Final Transversal (EFT): app Android que lee y escribe todos sus datos a través de una API REST propia.

### Funcionalidades implementadas

- Inicio de sesión local con cuatro perfiles (técnico, jefe de servicio, gerencia y administrador) y navegación según el rol.
- Formularios con validación por campo, ícono y mensaje de error.
- Técnico:
  - Iniciar jornada con vehículo, km inicial, foto del odómetro y ubicación GPS.
  - Registrar la salida hacia una clínica, con aviso si el trayecto no tiene tramo oficial.
  - Registrar la llegada con el km del odómetro; se confirma el registro y se marca como OK, desviación o no catalogado (el costo se calcula y guarda internamente, sin mostrarlo al técnico). Si hay desviación o el tramo no está catalogado, se pregunta el motivo en ese momento.
  - Volver a la sede y cerrar la jornada con foto obligatoria del odómetro.
  - Resumen de la jornada en curso: traslados y km recorridos.
  - Historial de jornadas con su estado y, si fue rechazada, el comentario del jefe.
- Jefe de servicio:
  - Las jornadas sin alertas se validan solas al cerrarse; al jefe le llegan las que tienen desviaciones o tramos sin catalogar, destacadas y primero, con el motivo que dio el técnico. Filtro por estado.
  - Detalle de la jornada: traslados, km oficiales vs declarados, desviación, costo, GPS y fotos del odómetro.
  - Validar la jornada o rechazarla con un comentario obligatorio.
- Gerencia: costo de traslados por cliente, técnico y vehículo en un mes (solo jornadas validadas), con el detalle de traslados de cada fila y el % de desviación por técnico.
- Administrador: mantenedores de destinos, vehículos, tramos oficiales (cada edición crea una versión con fecha de vigencia), tarifa de combustible con historial y usuarios (rol y jefe a cargo). Lo que ya tiene registros se desactiva, no se elimina.
- Cálculo de costo: km oficiales ÷ rendimiento del vehículo × precio del litro + peaje. Se marca desviación cuando los km declarados superan a los oficiales en más de 15% y en más de 2 km.
- API REST propia (Flask + SQLite) como única fuente de datos, precargada con datos de ejemplo (sede, clínicas, tramos, vehículos, tarifa, usuarios y seis jornadas en distintos estados). El servidor valida el login y las reglas de negocio y recalcula los costos.
- Recursos nativos: GPS y cámara.
- Animaciones: mensajes de error, transiciones entre pantallas, cambios de estado y chips de color.

### Pendiente

- Filtros del jefe por técnico y rango de fechas (RF-JEF-06).
- Explicar y volver a pedir los permisos de cámara y ubicación si se rechazan (RF-TEC-15).
- Subir las fotos del odómetro al servidor: hoy quedan solo en el dispositivo que las tomó, y en otro dispositivo se ve "Foto no disponible".

El detalle está en [`docs/TeqRuta_notas.md`](docs/TeqRuta_notas.md) (plan de desarrollo) y [`docs/TeqRuta_requisitos.md`](docs/TeqRuta_requisitos.md) (requisitos funcionales).

## Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Técnico | tecnico@teqmed.cl | tecnico123 |
| Técnico | tecnico2@teqmed.cl | tecnico123 |
| Jefe de servicio | jefe@teqmed.cl | jefe123 |
| Gerencia | gerencia@teqmed.cl | gerencia123 |
| Administrador | admin@teqmed.cl | admin123 |
| Usuario desactivado | inactivo@teqmed.cl | inactivo123 |

## Tecnologías

- Kotlin y Jetpack Compose (Material 3)
- Arquitectura MVVM
- Room: la base local sigue en el proyecto (`data/local`) pero la app no la usa por ahora
- Navigation Compose
- Google Play Services Location (GPS)
- Cámara del dispositivo mediante FileProvider
- Retrofit + Gson para la comunicación con la API
- Servidor: Python, Flask y SQLite

## Almacenamiento de datos

La app **no guarda datos en el teléfono**: todo se lee y se escribe a través de la API REST de la carpeta [`server/`](server/README.md), que usa SQLite (`server/teqruta.db`).

- Al abrir cada pantalla la app pide los datos al servidor, y cada acción (iniciar jornada, registrar un traslado, validar, editar el catálogo) se envía al servidor, que la valida y responde con el resultado.
- El servidor es el que valida el login y recalcula los costos con el catálogo vigente.
- La base local Room (`data/local`) queda en el proyecto, sin usarse, para poder volver a agregar el trabajo sin conexión más adelante.

Sin conexión con el servidor la app no puede iniciar sesión ni guardar cambios: lo indica con un mensaje y una nube tachada en el botón de actualizar de la barra superior.

## Estructura del proyecto

```
app/src/main/java/com/teqmed/teqruta/
├── data/
│   ├── local/        Clases de datos (entity); los DAOs y la base Room no se usan por ahora
│   ├── remote/       Retrofit: API, configuración (ApiConfig) y DatosRemotos
│   └── repository/   Repositorios por área (auth, jornadas, catálogo, admin, reportes)
├── device/           GPS y almacenamiento de fotos
├── domain/           Reglas de negocio: cálculo de costo, desviación, validaciones y reportes
└── ui/               Pantallas Compose y ViewModels por rol (tecnico, jefe, gerencia, admin)
server/               API REST en Flask + SQLite
docs/                 Requisitos y plan de desarrollo
```

## Cómo ejecutar

Requisitos:

- Android Studio (versión reciente, compatible con AGP 9)
- JDK 17 o superior (incluido con Android Studio)
- Emulador o dispositivo con Android 7.0 (API 24) o superior. Para el GPS se recomienda un emulador con imagen "Google Play" o "Google APIs".

- Python 3 para el servidor

Pasos:

1. Clonar el repositorio:
   ```
   git clone https://github.com/Molinum/TeqRuta.git
   ```
2. Levantar el servidor (la primera vez instala Flask y crea la base con los datos de ejemplo):
   ```
   cd TeqRuta/server && ./run.sh
   ```
3. Abrir la carpeta `TeqRuta` en Android Studio y esperar a que termine la sincronización de Gradle.
4. Seleccionar un emulador y presionar **Run**. La app se conecta a `http://10.0.2.2:5000`, que desde el emulador es el computador. Para usar un teléfono, levantar el servidor con `./run.sh --ngrok` y poner la URL que muestra en `API_URL` de `app/src/main/java/com/teqmed/teqruta/data/remote/ApiConfig.kt` (más opciones en [`server/README.md`](server/README.md)).
5. Iniciar sesión con alguno de los usuarios de prueba.

Para probar el GPS en el emulador: abrir los controles extendidos del emulador (botón **⋯**) → **Location**, elegir un punto y presionar **Set location**. Si se niega el permiso de ubicación, los traslados se registran igual, marcados sin ubicación.

Para volver a los datos de ejemplo: `./run.sh --reset` en la carpeta `server`.

## Pruebas

- App (pruebas unitarias de las reglas de negocio: cálculo de costo, desviación, validación automática, reportes y validaciones de formularios):
  ```
  ./gradlew testDebugUnitTest
  ```
- Servidor (pruebas de la API):
  ```
  cd server && .venv/bin/python -m unittest -v test_app
  ```
