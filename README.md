# TeqRuta

Aplicación Android para registrar los traslados del personal técnico de TEQMED SpA, empresa de servicio técnico de equipamiento médico de la Región del Biobío.

Hoy los técnicos anotan sus traslados en una libreta y los declaran a fin de mes. TeqRuta permite registrar cada traslado en el momento en que ocurre (kilometraje, destino, ubicación y foto del odómetro), calcular su costo con tarifas centralizadas y detectar diferencias entre la distancia oficial de un trayecto y la declarada por el técnico.

Proyecto de la asignatura Desarrollo de Aplicaciones Móviles (DSY1105), Duoc UC.

## Autor

- Benjamin Molina

## Estado del proyecto

En desarrollo (Evaluación Parcial 2).

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
- Base de datos local con Room, precargada con datos de ejemplo (sede, clínicas, tramos, vehículos, tarifa, usuarios y seis jornadas de los últimos 20 días en distintos estados).
- Recursos nativos: GPS y cámara.
- Animaciones: mensajes de error, transiciones entre pantallas, cambios de estado y chips de color.

### Pendiente

- Filtros del jefe por técnico y rango de fechas (RF-JEF-06).
- Explicar y volver a pedir los permisos de cámara y ubicación si se rechazan (RF-TEC-15).

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
- Room (sobre SQLite) para persistencia local
- Navigation Compose
- Google Play Services Location (GPS)
- Cámara del dispositivo mediante FileProvider
- Retrofit + Gson para la comunicación con la API
- Servidor: Python, Flask y SQLite

## Almacenamiento de datos

La app usa dos bases de datos:

| | Base local | Base central |
|---|---|---|
| Dónde | En el dispositivo | En el servidor ([`server/`](server/README.md)) |
| Tecnología | Room (SQLite) | Flask + SQLite (`server/teqruta.db`), expuesta como API REST |
| Para qué | La app lee y escribe siempre aquí, por eso funciona sin conexión | Reúne las jornadas de todos los técnicos, es dueña del catálogo y recalcula los costos |

Ambas se sincronizan: la app sube las jornadas con cambios pendientes y baja el catálogo y las jornadas que el usuario puede ver. Cada jornada y traslado tiene un `uuid` generado en el dispositivo, así no se duplican. Sin conexión, lo pendiente se reenvía en la siguiente sincronización.

La conexión se activa con una sola constante, `API_URL`: si está vacía (valor por defecto) la app no hace ninguna llamada de red y trabaja solo con Room.

## Estructura del proyecto

```
app/src/main/java/com/teqmed/teqruta/
├── data/
│   ├── local/        Room: entidades, DAOs, base de datos y datos de ejemplo
│   ├── remote/       Retrofit: API, configuración (ApiConfig) y mapeos
│   ├── repository/   Repositorios por área (auth, jornadas, catálogo, admin, reportes)
│   └── sync/         Sincronización con el servidor
├── device/           GPS y almacenamiento de fotos
├── domain/           Reglas de negocio: cálculo de costo, desviación, validaciones y reportes
└── ui/               Pantallas Compose y ViewModels por rol (tecnico, jefe, gerencia, admin)
server/               API REST en Flask + SQLite
docs/                 Requisitos y plan de desarrollo
```

## Servidor y sincronización (opcional)

```bash
cd server && ./run.sh --ngrok      # levanta la API y muestra la URL pública
```

Pega esa URL en `API_URL` de `app/src/main/java/com/teqmed/teqruta/data/remote/ApiConfig.kt` y vuelve a ejecutar la app: aparece un botón de sincronizar en la barra superior. Los detalles (modos de conexión, endpoints, reglas y limitaciones) están en [`server/README.md`](server/README.md).

## Cómo ejecutar

Requisitos:

- Android Studio (versión reciente, compatible con AGP 9)
- JDK 17 o superior (incluido con Android Studio)
- Emulador o dispositivo con Android 7.0 (API 24) o superior. Para el GPS se recomienda un emulador con imagen "Google Play" o "Google APIs".

Pasos:

1. Clonar el repositorio:
   ```
   git clone https://github.com/Molinum/TeqRuta.git
   ```
2. Abrir la carpeta `TeqRuta` en Android Studio y esperar a que termine la sincronización de Gradle.
3. Seleccionar un emulador o dispositivo y presionar **Run**.
4. Iniciar sesión con alguno de los usuarios de prueba.

Para probar el GPS en el emulador: abrir los controles extendidos del emulador (botón **⋯**) → **Location**, elegir un punto y presionar **Set location**. Si se niega el permiso de ubicación, los traslados se registran igual, marcados sin ubicación.

Si la app ya estaba instalada de una versión anterior (cambió el esquema de la base de datos), desinstalarla primero para que la base de datos se cree con los datos de ejemplo.
