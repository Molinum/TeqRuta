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
  - Registrar la llegada con el km del odómetro; se calcula el costo del traslado y se marca como OK, desviación o no catalogado.
  - Volver a la sede y cerrar la jornada con foto obligatoria del odómetro.
  - Resumen de la jornada en curso: traslados, km recorridos y costo acumulado.
- Cálculo de costo: km oficiales ÷ rendimiento del vehículo × precio del litro + peaje. Se marca desviación cuando los km declarados superan a los oficiales en más de 15% y en más de 2 km.
- Base de datos local con Room, precargada con datos de ejemplo (sede, clínicas, tramos, vehículos, tarifa y usuarios).
- Recursos nativos: GPS y cámara.
- Animaciones: mensajes de error, transiciones entre pantallas, cambios de estado y contador del costo.

### Pendiente

- Historial de jornadas del técnico.
- Jefe: revisar, validar o rechazar jornadas.
- Gerencia: costo de traslados por cliente, técnico y vehículo.
- Administrador: mantenedores de destinos, vehículos, tramos, tarifa de combustible y usuarios.

Las pantallas de jefe, gerencia y administrador existen pero aún muestran "Próximamente".

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

Toda la información se guarda localmente en el dispositivo; la aplicación no se conecta a servicios externos.

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

Si la app ya estaba instalada de una versión anterior, desinstalarla primero para que la base de datos se cree con los datos de ejemplo.
