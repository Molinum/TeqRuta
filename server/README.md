# Servidor TeqRuta (Flask + SQLite)

API REST con la base de datos de TeqRuta: el catálogo (usuarios, vehículos, destinos, tramos,
tarifas) y las jornadas de todos los técnicos. **La app lee y escribe todos sus datos a través de
esta API**, así que el servidor tiene que estar corriendo para usarla.

## Levantarlo

```bash
cd server
./run.sh             # API en http://localhost:5000
./run.sh --ngrok     # además abre un túnel ngrok y muestra la URL pública
./run.sh --reset     # borra teqruta.db y la recrea con los datos de ejemplo
```

La primera vez crea un entorno virtual (`.venv`), instala Flask y crea `teqruta.db` con datos de
ejemplo (usuarios, vehículos, destinos, tramos, tarifa y 6 jornadas en distintos estados).
Para `--ngrok` necesitas ngrok instalado y tu token configurado una vez:
`ngrok config add-authtoken <token>`.

## Conectar la app

La dirección del servidor es **una** constante en
`app/src/main/java/com/teqmed/teqruta/data/remote/ApiConfig.kt`:

```kotlin
const val API_URL = "http://10.0.2.2:5000"   // valor por defecto
```

| Dónde corre la app | `API_URL` |
|---|---|
| Emulador con Flask local (por defecto) | `http://10.0.2.2:5000` |
| Emulador si `10.0.2.2` no responde | `http://localhost:5000` y ejecutar `adb reverse tcp:5000 tcp:5000` |
| Teléfono o emulador con ngrok | `https://xxxx.ngrok-free.app` (la URL que muestra `run.sh --ngrok`) |

Después de cambiarla hay que volver a compilar y ejecutar la app. La URL de ngrok gratis cambia
cada vez que lo reinicias.

`API_KEY` (misma constante, por defecto `teqruta-dev-key`) debe coincidir con la variable de entorno
`TEQRUTA_API_KEY` del servidor. La URL de ngrok es pública: la clave evita que cualquiera que la
conozca lea o escriba datos. Para cambiarla: `TEQRUTA_API_KEY=otra-clave ./run.sh`.

## Cómo la usa la app

- **Lecturas:** cada pantalla pide los datos al servidor al abrirse; el botón de actualizar de la
  barra superior los vuelve a pedir. Cada usuario recibe solo las jornadas que puede ver: el técnico
  las suyas, el jefe las de su equipo y gerencia solo las validadas (RN-15, RN-12).
- **Escrituras:** cada acción del técnico o del jefe envía la jornada completa con sus traslados
  (`PUT /jornadas/<uuid>`); los cambios del administrador van a su endpoint. El servidor valida,
  asigna los ids y responde con el resultado, que es lo que muestra la app.
- **Cálculo de costos:** el servidor recalcula cada traslado con su propio catálogo (tramo y tarifa
  vigentes a la fecha de salida), así el costo queda centralizado y auditable (RF-EFT-04). Una
  jornada validada automáticamente que el servidor marque con alertas vuelve al jefe.
- **Reglas del servidor:** un técnico tiene a lo más una jornada abierta y un vehículo está en una
  sola (RN-06), una jornada Validada o Rechazada no se puede modificar (RN-10), rechazar exige
  comentario y un traslado con alerta exige motivo.
- **Sin conexión** la app no puede leer ni guardar: muestra el error y una nube tachada en el botón
  de actualizar.

## Endpoints

Todos bajo `/api`, con el encabezado `X-API-Key` (salvo `health`).

| Método y ruta | Uso |
|---|---|
| `GET /health` | Comprobar que el servidor responde |
| `POST /login` | Validar correo y contraseña; devuelve el usuario (401 si no coinciden, 403 si está desactivado) |
| `GET /catalogos` | Usuarios, vehículos, destinos, tramos y tarifas |
| `POST /destinos`, `/vehiculos`, `/usuarios` | Crear (sin `id`) o editar (con `id`) |
| `POST /tramos` | Crear un tramo o una nueva versión (cierra la anterior) |
| `POST /tarifas` | Registrar una nueva tarifa de combustible |
| `GET /vehiculos/<id>/ultimo-km` | Km final de la última jornada del vehículo |
| `GET /jornadas?usuarioId=&desde=` | Jornadas visibles para ese usuario (opcionalmente, modificadas después de `desde`) |
| `PUT /jornadas/<uuid>` | Crear o actualizar una jornada con sus traslados (idempotente) |

Los nombres de tablas, columnas y campos JSON coinciden con las clases de
`app/src/main/java/com/teqmed/teqruta/data/local/entity`; el esquema está en `schema.sql`.

## Pruebas

```bash
.venv/bin/python -m unittest -v test_app
```

Cinco pruebas de la API sobre una base temporal: login correcto e incorrecto, clave de API
obligatoria, un vehículo en una sola jornada abierta y cálculo de costo con motivo obligatorio
ante una desviación.

## Limitaciones conocidas

- Las fotos del odómetro no se suben: el servidor guarda solo su ruta en el teléfono, así que otro
  dispositivo verá "Foto no disponible".
- No hay sesiones ni tokens por usuario: se confía en `usuarioId` y en la clave compartida. Las
  contraseñas se guardan como SHA-256 sin sal y nunca salen del servidor. Está pensado para una
  demostración, no para producción.
- Servidor de desarrollo de Flask (un solo proceso), suficiente para la demo.
