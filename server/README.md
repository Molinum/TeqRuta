# Servidor TeqRuta (Flask + SQLite)

API REST que guarda el catálogo (usuarios, vehículos, destinos, tramos, tarifas) y concentra las
jornadas de todos los técnicos. La app sigue funcionando sin conexión con su base local (Room) y
se sincroniza con este servidor cuando hay red.

## Levantarlo

```bash
cd server
./run.sh             # API en http://localhost:5000
./run.sh --ngrok     # además abre un túnel ngrok y muestra la URL pública
./run.sh --reset     # borra teqruta.db y la recrea con los datos de ejemplo
```

La primera vez crea un entorno virtual (`.venv`), instala Flask y crea `teqruta.db` con los mismos
datos de ejemplo que la app (usuarios, vehículos, destinos, tramos, tarifa y 6 jornadas).
Para `--ngrok` necesitas ngrok instalado y tu token configurado una vez:
`ngrok config add-authtoken <token>`.

## Conectar la app

Edita **una** constante en `app/src/main/java/com/teqmed/teqruta/data/remote/ApiConfig.kt`:

```kotlin
const val API_URL = "https://xxxx-xx-xx.ngrok-free.app"   // la URL que muestra run.sh --ngrok
```

| Dónde corre la app | `API_URL` |
|---|---|
| Dispositivo o emulador con ngrok | `https://xxxx.ngrok-free.app` |
| Emulador con Flask local | `http://10.0.2.2:5000` |
| Emulador si `10.0.2.2` no responde | `http://localhost:5000` y ejecutar `adb reverse tcp:5000 tcp:5000` |
| Sin servidor (solo base local) | `""` (valor por defecto) |

Con `API_URL` vacío la app no hace ninguna llamada de red ni muestra el botón de sincronizar.
Después de cambiarla hay que volver a compilar y ejecutar la app. La URL de ngrok gratis cambia
cada vez que lo reinicias.

`API_KEY` (misma constante, por defecto `teqruta-dev-key`) debe coincidir con la variable de entorno
`TEQRUTA_API_KEY` del servidor. La URL de ngrok es pública: la clave evita que cualquiera que la
conozca lea o escriba datos. Para cambiarla: `TEQRUTA_API_KEY=otra-clave ./run.sh`.

## Cómo se sincroniza

- **Catálogo:** el servidor manda. La app lo baja al iniciar, al iniciar sesión y al tocar el botón
  de sincronizar. Los cambios del administrador se envían primero al servidor (que valida y asigna
  los ids) y luego se bajan a Room; sin conexión no se pueden modificar.
- **Jornadas:** cada jornada y traslado tiene un `uuid` generado en el dispositivo. La app sube las
  jornadas con cambios pendientes y baja las que el usuario puede ver y cambiaron desde la última
  vez: el técnico las suyas, el jefe las de su equipo, gerencia solo las validadas (RN-15, RN-12).
- **Cálculo de costos:** el servidor recalcula cada traslado con su propio catálogo (tramo y tarifa
  vigentes a la fecha de salida) y devuelve el resultado: el costo queda centralizado y auditable
  (RF-EFT-04). Una jornada validada automáticamente que el servidor marque con alertas vuelve al jefe.
- **Reglas del servidor:** una jornada Validada o Rechazada no se puede modificar (RN-10) y un traslado
  con alerta exige motivo.
- Si no hay conexión, lo pendiente queda marcado en el dispositivo y se reenvía en la próxima sincronización.

## Endpoints

Todos bajo `/api`, con el encabezado `X-API-Key` (salvo `health`).

| Método y ruta | Uso |
|---|---|
| `GET /health` | Comprobar que el servidor responde |
| `GET /catalogos` | Usuarios, vehículos, destinos, tramos y tarifas |
| `POST /destinos`, `/vehiculos`, `/usuarios` | Crear (sin `id`) o editar (con `id`) |
| `POST /tramos` | Crear un tramo o una nueva versión (cierra la anterior) |
| `POST /tarifas` | Registrar una nueva tarifa de combustible |
| `GET /jornadas?usuarioId=&desde=` | Jornadas visibles para ese usuario modificadas después de `desde` |
| `PUT /jornadas/<uuid>` | Crear o actualizar una jornada con sus traslados (idempotente) |

Los nombres de tablas, columnas y campos JSON coinciden con las entidades de Room
(`app/src/main/java/com/teqmed/teqruta/data/local/entity`); el esquema está en `schema.sql`.

## Limitaciones conocidas

- Las fotos del odómetro no se suben: el servidor guarda solo su ruta local, así que otro
  dispositivo verá "Foto no disponible".
- No hay autenticación por usuario: se confía en `usuarioId` y en la clave compartida. Los hashes de
  contraseña (SHA-256 sin sal) viajan en el catálogo para permitir el login sin conexión. Está pensado
  para una demostración, no para producción.
- Servidor de desarrollo de Flask (un solo proceso), suficiente para la demo.
