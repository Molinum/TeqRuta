# TeqRuta

Aplicación Android para registrar los traslados del personal técnico de TEQMED SpA, empresa de servicio técnico de equipamiento médico de la Región del Biobío.

Hoy los técnicos anotan sus traslados en una libreta y los declaran a fin de mes. TeqRuta permite registrar cada traslado en el momento en que ocurre (kilometraje, destino, ubicación y foto del odómetro), calcular su costo con tarifas centralizadas y detectar diferencias entre la distancia oficial de un trayecto y la declarada por el técnico.

Proyecto de la asignatura Desarrollo de Aplicaciones Móviles (DSY1105), Duoc UC.

## Autor

- Benjamin Molina

## Estado del proyecto

En desarrollo (Evaluación Parcial 2).

Implementado hasta ahora:

- Proyecto base en Android Studio con Jetpack Compose.
- Dependencias configuradas: Room (con KSP), Navigation Compose, ViewModel y servicios de ubicación.
- Plan de desarrollo y requisitos funcionales en `docs/`.

## Funcionalidades planificadas

- Inicio de sesión local con cuatro perfiles: técnico, jefe de servicio, gerencia y administrador.
- Técnico: iniciar jornada, registrar salida y llegada a cada clínica, cerrar jornada y ver su historial.
- Cálculo del costo de cada traslado y marca de desviación cuando los km declarados superan en más de 15% a los oficiales.
- Jefe: revisar, validar o rechazar las jornadas de su equipo.
- Gerencia: costo de traslados por cliente, técnico y vehículo.
- Administrador: mantenedores de destinos, vehículos, tramos, tarifa de combustible y usuarios.
- Recursos nativos: GPS (ubicación al iniciar y llegar) y cámara (foto del odómetro).

El detalle está en [`docs/TeqRuta_notas.md`](docs/TeqRuta_notas.md) (plan de desarrollo) y [`docs/TeqRuta_requisitos.md`](docs/TeqRuta_requisitos.md) (requisitos funcionales).

## Tecnologías

- Kotlin y Jetpack Compose (Material 3)
- Arquitectura MVVM
- Room para persistencia local
- Navigation Compose
- Google Play Services Location

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
