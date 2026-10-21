# TeqRuta — Plan de desarrollo (EP2 · DSY1105)

> App Android para que los técnicos de TEQMED SpA registren sus traslados en terreno,
> se calcule su costo con tarifas centralizadas y se detecten desviaciones entre la
> distancia oficial y la declarada.

- **Evaluación:** Parcial 2 — Encargo (8%). Plazo: 5 semanas (asignado semana 6, entrega semana 11).
- **Restricción del docente:** sin conexiones externas (HTTP). Todo local con **MVVM + Room**.
- **Modalidad:** desarrollo individual (la pauta dice parejas → confirmar con el docente cómo se evalúa
  la "participación distribuida" del IE 2.3.2).

---

## 1. Decisiones de diseño

| # | Tema | Decisión |
|---|---|---|
| 1 | Backend | **Ninguno en EP2.** Todo en Room. Se mantiene una capa `repository` para poder agregar una fuente remota en la EFT sin tocar ViewModels ni UI. |
| 2 | Perfiles | **Todos en la misma app**, con login local y rol guardado en Room (Técnico, Jefe, Gerencia, Admin). |
| 3 | Captura del traslado | **Enfoque híbrido:** GPS puntual al iniciar y al llegar (evidencia de presencia) + km del odómetro declarados por el técnico + distancia oficial desde la tabla de tramos. |
| 4 | Km que se pagan | **Se pagan los km oficiales.** Si los declarados superan a los oficiales en más del **umbral (15%)**, el tramo queda marcado con **desviación** para revisión del jefe. |
| 5 | Fotos del odómetro | **Obligatoria al iniciar y al cerrar la jornada**, opcional en los tramos intermedios. ML Kit como mejora opcional al final (ver §8). |
| 6 | Distancias oficiales | **Tabla de tramos en Room**, precargada con datos semilla y editable por el Admin. Cada tramo tiene fecha de vigencia (versionado). OpenRouteService queda postergado para la EFT. |
| 7 | Tramos fuera del catastro | Se registran igual con los km del odómetro y el GPS de inicio y fin, quedan marcados como **"no catalogado"** y los revisa el jefe. |
| 8 | Recursos nativos (≥2) | **GPS** (ubicación al iniciar y llegar) y **cámara** (foto del odómetro). |
| 9 | Precio del combustible | Lo ingresa el Admin con fecha de vigencia (la API de la CNE queda postergada). |
| 10 | Integración con el ERP | Fuera de alcance. |

### Fórmula de costo (por tramo)
```
costo = (kmOficial / rendimientoVehiculo[km/L]) × precioLitroVigente + peaje
desviacion = (kmDeclarado − kmOficial) / kmOficial      → si > 0.15 ⇒ DESVIACION
tramo no catalogado ⇒ se usa kmDeclarado y queda marcado NO_CATALOGADO
```
**Auditable:** cada traslado guarda una *copia* de los valores que usó (km oficial, peaje,
precio del litro y rendimiento). Si mañana cambia la tarifa, los viajes antiguos no se recalculan.

---

## 2. Alcance por perfil (base para los requisitos)

| Perfil | Pantallas / funciones | Formularios |
|---|---|---|
| Todos | Login, cerrar sesión | Login |
| **Técnico** | Iniciar jornada → salir hacia un destino → "llegué" → … → cerrar jornada; historial de jornadas | Iniciar jornada · Registrar llegada · Cerrar jornada |
| **Jefe** | Lista de jornadas por validar (resaltando desviaciones y tramos no catalogados), detalle, validar o rechazar | Validar/Rechazar (comentario obligatorio al rechazar) |
| **Gerencia** | Resumen de costo por cliente, técnico y vehículo, con filtro por mes (solo lectura) | — (solo filtros) |
| **Admin** | Mantenedores de destinos (sede y clínicas), vehículos, tramos, tarifa de combustible y usuarios | Destino · Vehículo · Tramo · Tarifa · Usuario |

**≈ 10 formularios.** La pauta exige que *todos* tengan validación visual por campo con íconos, así que **no conviene agregar más pantallas** sin revisar el alcance.

### Flujo del técnico (el corazón de la app)
1. **Iniciar jornada:** elige vehículo, ingresa km inicial, **foto obligatoria**, GPS automático. Origen = sede.
2. **Salir hacia…:** elige el destino (clínica). Si no existe un tramo para ese par → aviso de "no catalogado".
3. **Llegué:** ingresa los km actuales (el km de inicio del tramo se completa solo con el km anterior), GPS automático, foto opcional. Se muestra el costo calculado y si hay desviación.
4. Repite 2–3 por cada clínica.
5. **Cerrar jornada:** último tramo de vuelta a la sede, km final, **foto obligatoria**. La jornada queda `CERRADA` y pasa al jefe.

Estados de la jornada: `ABIERTA → CERRADA → VALIDADA | RECHAZADA`

---

## 3. Modelo de datos (Room)

| Entidad | Campos principales |
|---|---|
| `Usuario` | id, nombre, email, passwordHash, rol, jefeId? (solo técnicos), activo |
| `Vehiculo` | id, patente, descripcion, tipo (`EMPRESA`/`PARTICULAR`), rendimientoKmL, activo |
| `Destino` | id, nombre, direccion, ciudad, lat, lon, tipo (`SEDE`/`CLINICA`), activo |
| `Tramo` | id, origenId, destinoId, kmOficial, peaje, vigenteDesde, vigenteHasta? |
| `TarifaCombustible` | id, precioLitro, vigenteDesde |
| `Jornada` | id, tecnicoId, vehiculoId, fecha, kmInicial, fotoInicioUri, kmFinal?, fotoCierreUri?, estado, comentarioJefe? |
| `Traslado` | id, jornadaId, orden, origenId, destinoId, tramoId? (null = no catalogado), horaSalida, horaLlegada, kmInicio, kmFin, latSalida, lonSalida, latLlegada, lonLlegada, fotoUri?, **kmOficialUsado, peajeUsado, precioLitroUsado, rendimientoUsado**, costo, desviacionPct, marca (`OK`/`DESVIACION`/`NO_CATALOGADO`) |

- Las fotos se guardan como **archivos en el almacenamiento interno de la app**. En Room solo va la ruta (URI), nunca la imagen.
- `SeedData.kt` precarga: la sede de Concepción, unas 6–8 clínicas (Concepción, Chillán, Los Ángeles, Temuco), los tramos entre ellas, 3 vehículos, una tarifa y un usuario por rol.

---

## 4. Estructura de carpetas

Sigue la convención del ramo (`data/`, `ui/`, `ViewModelFactory`, `FormState`), separada por perfil
para que sea modular.

```
TeqRuta/                                  ← raíz del repositorio de GitHub
├── README.md                             ← descripción, autor, funcionalidades, cómo ejecutar
├── .gitignore                            ← incluye local.properties, /build, .idea
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
└── app/
    ├── build.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml       ← permisos CAMERA, ACCESS_FINE/COARSE_LOCATION + FileProvider
        │   ├── res/
        │   │   ├── values/ (strings.xml, colors.xml, themes.xml)
        │   │   └── xml/file_paths.xml    ← rutas que comparte el FileProvider (fotos)
        │   └── java/com/teqmed/teqruta/
        │       ├── MainActivity.kt
        │       ├── TeqRutaApp.kt         ← Application: crea la BD y los repositorios (DI manual)
        │       │
        │       ├── data/
        │       │   ├── local/
        │       │   │   ├── AppDatabase.kt
        │       │   │   ├── Converters.kt         ← enums y fechas ↔ tipos de Room
        │       │   │   ├── SeedData.kt           ← datos iniciales
        │       │   │   ├── entity/
        │       │   │   │   ├── Usuario.kt
        │       │   │   │   ├── Vehiculo.kt
        │       │   │   │   ├── Destino.kt
        │       │   │   │   ├── Tramo.kt
        │       │   │   │   ├── TarifaCombustible.kt
        │       │   │   │   ├── Jornada.kt
        │       │   │   │   └── Traslado.kt
        │       │   │   └── dao/
        │       │   │       ├── UsuarioDao.kt
        │       │   │       ├── VehiculoDao.kt
        │       │   │       ├── DestinoDao.kt
        │       │   │       ├── TramoDao.kt
        │       │   │       ├── TarifaDao.kt
        │       │   │       ├── JornadaDao.kt
        │       │   │       └── TrasladoDao.kt
        │       │   └── repository/
        │       │       ├── AuthRepository.kt
        │       │       ├── CatalogoRepository.kt     ← destinos, vehículos, tramos, tarifas
        │       │       ├── JornadaRepository.kt      ← jornadas + traslados (usa CalculadoraCosto)
        │       │       └── ReporteRepository.kt      ← consultas agregadas para Gerencia
        │       │
        │       ├── domain/                       ← lógica pura de Kotlin, sin Android → fácil de testear
        │       │   ├── model/
        │       │   │   ├── Rol.kt
        │       │   │   ├── EstadoJornada.kt
        │       │   │   └── MarcaTraslado.kt
        │       │   ├── calculo/
        │       │   │   ├── CalculadoraCosto.kt
        │       │   │   └── DetectorDesviacion.kt     ← UMBRAL_DESVIACION = 0.15
        │       │   └── validation/
        │       │       ├── ValidationResult.kt       ← Valid / Invalid(mensaje)
        │       │       ├── Validators.kt             ← reglas reutilizables (requerido, numérico, rango, patente…)
        │       │       └── JornadaValidator.kt       ← reglas de negocio (kmFin > kmInicio, etc.)
        │       │
        │       ├── device/                       ← recursos nativos, aislados de la UI
        │       │   ├── LocationProvider.kt           ← ubicación actual (FusedLocationProviderClient)
        │       │   └── PhotoStorage.kt               ← crea el archivo y la URI para la cámara
        │       │
        │       └── ui/
        │           ├── ViewModelFactory.kt
        │           ├── SessionViewModel.kt           ← usuario logueado y su rol
        │           ├── navigation/
        │           │   ├── Routes.kt
        │           │   └── NavGraph.kt               ← redirige por rol después del login
        │           ├── theme/ (Color.kt, Theme.kt, Type.kt)
        │           ├── components/
        │           │   ├── ValidatedTextField.kt     ← campo con ícono + mensaje de error animado
        │           │   ├── DropdownField.kt
        │           │   ├── PermissionGate.kt         ← pide permiso, explica si lo rechazan
        │           │   ├── EstadoChip.kt             ← OK / DESVIACIÓN / NO CATALOGADO
        │           │   └── ConfirmDialog.kt
        │           ├── auth/
        │           │   ├── LoginScreen.kt
        │           │   ├── LoginViewModel.kt
        │           │   └── LoginFormState.kt
        │           ├── tecnico/
        │           │   ├── TecnicoHomeScreen.kt      ← jornada activa o botón "Iniciar jornada"
        │           │   ├── IniciarJornadaScreen.kt
        │           │   ├── RegistrarLlegadaScreen.kt
        │           │   ├── CerrarJornadaScreen.kt
        │           │   ├── HistorialScreen.kt
        │           │   ├── JornadaViewModel.kt
        │           │   └── JornadaFormState.kt
        │           ├── jefe/
        │           │   ├── ValidacionListScreen.kt
        │           │   ├── ValidacionDetalleScreen.kt
        │           │   └── ValidacionViewModel.kt
        │           ├── gerencia/
        │           │   ├── ReporteScreen.kt
        │           │   └── ReporteViewModel.kt
        │           └── admin/
        │               ├── AdminHomeScreen.kt
        │               ├── destinos/   (DestinoListScreen, DestinoFormScreen, DestinoViewModel, DestinoFormState)
        │               ├── vehiculos/  (…mismo patrón)
        │               ├── tramos/     (…mismo patrón)
        │               ├── tarifas/    (…mismo patrón)
        │               └── usuarios/   (…mismo patrón)
        │
        └── test/java/com/teqmed/teqruta/       ← tests unitarios (plus, no los exige la pauta)
            ├── CalculadoraCostoTest.kt
            ├── DetectorDesviacionTest.kt
            └── ValidatorsTest.kt
```

**Regla para el IE 2.2.1:** los Composables **solo muestran** el estado (`FormState`) y avisan de eventos
(`onKmChange`, `onSubmit`). Toda la validación ocurre en el ViewModel usando `domain/validation`.
Ningún `if (km.isEmpty())` dentro de un Composable.

---

## 5. Fases de desarrollo

Ordenadas por dependencia: cada fase usa lo que construyó la anterior. Al final de cada fase la app
**compila y funciona**.

### Semana 1 — Base

**Fase 0 · Configuración**
- [ ] Crear el repositorio en GitHub (público) y el tablero de Trello (columnas: Backlog · Esta semana · En progreso · Hecho).
- [ ] Crear un issue en GitHub por cada tarea grande (la pauta valora "issues resueltos").
- [ ] Proyecto en Android Studio (Empty Activity, Compose), paquete `com.teqmed.teqruta`, minSdk 24.
- [ ] Dependencias: Room + KSP, Navigation Compose, lifecycle-viewmodel-compose, play-services-location.
- [ ] `.gitignore` y un README inicial.

**Fase 1 · Capa de datos**
- [ ] Enums de `domain/model` y `Converters`.
- [ ] Las 7 entidades y sus DAOs.
- [ ] `AppDatabase` + `SeedData` (precarga en `onCreate` de la BD).
- [ ] Repositorios y `TeqRutaApp` (DI manual) + `ViewModelFactory`.
- [ ] `CalculadoraCosto` y `DetectorDesviacion` con sus tests unitarios.

**Fase 2 · Esqueleto de UI + login**
- [ ] Tema (colores de TEQMED, tipografía).
- [ ] `ValidatedTextField` (ícono + mensaje de error con `AnimatedVisibility`) y `Validators`.
- [ ] Login con validación y `SessionViewModel`.
- [ ] `NavGraph` con transiciones animadas y redirección por rol (con pantallas vacías de relleno por ahora).

### Semana 2 — Flujo del técnico (sin recursos nativos todavía)

**Fase 3**
- [ ] `TecnicoHomeScreen` (jornada activa o botón "Iniciar").
- [ ] Iniciar jornada → salir hacia un destino → registrar llegada → cerrar jornada, con `JornadaValidator`.
- [ ] Cálculo de costo y marca de desviación / no catalogado al registrar cada llegada.
- [ ] Historial de jornadas del técnico.
- Al terminar: **demo mínima funcionando de punta a punta** con datos semilla.

### Semana 3 — Recursos nativos + jefe

**Fase 4 · GPS y cámara**
- [ ] Permisos en el Manifest + `PermissionGate` (pide el permiso, explica por qué y maneja el rechazo sin romper la app).
- [ ] `LocationProvider`: captura lat/lon al iniciar jornada y al registrar cada llegada. Si no hay permiso o no hay señal, se registra igual y queda marcado "sin ubicación".
- [ ] `PhotoStorage` + `FileProvider` + `ActivityResultContracts.TakePicture()`: foto obligatoria al iniciar y cerrar, opcional en las llegadas. Vista previa de la foto en el formulario.
- [ ] Probar en el emulador: ubicación simulada desde *Extended controls → Location* y la cámara virtual.

**Fase 5 · Jefe**
- [ ] Lista de jornadas `CERRADA` con chips de estado y orden por gravedad (desviaciones primero).
- [ ] Detalle: tramos, km oficiales vs declarados, fotos, coordenadas.
- [ ] Validar o rechazar (comentario obligatorio y validado al rechazar).

### Semana 4 — Admin + gerencia

**Fase 6 · Mantenedores del admin** (mismo patrón ×5, igual que el CRUD de productos del ramo)
- [ ] Destinos · Vehículos · Tramos (origen ≠ destino, km > 0, peaje ≥ 0; al editar se crea una nueva versión con vigencia) · Tarifa de combustible · Usuarios.

**Fase 7 · Reporte de gerencia**
- [ ] Consultas agregadas en `TrasladoDao` (`GROUP BY` cliente, técnico y vehículo).
- [ ] `ReporteScreen` con pestañas y filtro por mes.

### Semana 5 — Pulido y entrega

**Fase 8 · Animaciones y UX** (algunas ya entraron antes; aquí se completan)
- [ ] Mensajes de error con `AnimatedVisibility` (Fase 2) + "shake" del formulario al enviar con errores.
- [ ] Transiciones entre pantallas en el `NavHost`.
- [ ] `animateColorAsState` en los chips de estado; contador animado del costo al registrar una llegada.
- [ ] Estados de carga y vacío ("Aún no tienes jornadas") con `Crossfade`.

**Fase 9 · Entrega**
- [ ] README completo: descripción, autor, funcionalidades, usuarios de prueba por rol, cómo ejecutar y cómo simular el GPS en el emulador.
- [ ] Revisar que todos los formularios tengan validación + ícono + mensaje.
- [ ] Trello al día, issues cerrados.
- [ ] Subir el código a AVA y activar el enlace individual.
- [ ] **⚠️ Último commit ANTES de la fecha de entrega. Después, no tocar el repositorio** (cualquier cambio = nota 1.0).

**Opcional si sobra tiempo:** ML Kit (§8).

---

## 6. Convención de commits

El IE 2.3.2 vale **20%** y pide commits técnicos, bien escritos y con actividad real. Reglas:

1. **Un cambio lógico por commit.** Nada de "avance" con 30 archivos.
2. **Commitear cada vez que algo compila y funciona**, varias veces por semana. No subir todo junto al final.
3. Formato *Conventional Commits*, en español e imperativo:
   ```
   <tipo>(<módulo>): <qué hace>

   [cuerpo opcional: por qué / detalles]
   [Closes #N]
   ```
   - **Tipos:** `feat` (funcionalidad), `fix` (corrección), `refactor`, `style` (UI/tema), `test`, `docs`, `chore` (configuración, dependencias).
   - **Módulos:** `data`, `domain`, `auth`, `tecnico`, `jefe`, `gerencia`, `admin`, `device`, `nav`, `ui`, `build`.
4. **Una rama por fase** (`feat/capa-datos`, `feat/flujo-tecnico`…) y merge a `main` con un Pull Request que cierre los issues. Aunque trabajes solo, deja evidencia del proceso.

### Ejemplos por fase
```
chore(build): configurar Room con KSP y Navigation Compose
feat(data): agregar entidades Jornada y Traslado con sus DAOs
feat(data): precargar sede, clínicas y tramos de ejemplo
feat(domain): implementar CalculadoraCosto por tramo
test(domain): cubrir umbral de desviación y tramo no catalogado
feat(ui): crear ValidatedTextField con ícono y error animado
feat(auth): implementar login local con redirección por rol
feat(tecnico): registrar llegada con cálculo de costo
fix(tecnico): impedir km de llegada menores al km de salida
feat(device): capturar ubicación al iniciar jornada
feat(device): tomar foto del odómetro con FileProvider
feat(jefe): validar y rechazar jornadas con comentario
feat(admin): mantenedor de tramos con vigencia
feat(gerencia): reporte de costo por cliente y mes
style(ui): animar transiciones entre pantallas
docs: completar README con usuarios de prueba
```
❌ Evitar: `cambios`, `arreglos`, `ahora sí funciona`, `final final v2`.

---

## 7. Mapeo con la rúbrica

| Indicador | Peso | Dónde se cumple |
|---|---|---|
| IE 2.1.1 Interfaz y navegación | 15% | `ui/theme`, `NavGraph` por rol, Fases 2–8 |
| IE 2.1.2 Formularios validados con íconos | 15% | `ValidatedTextField` en los ~10 formularios |
| IE 2.2.1 Validación desacoplada | 10% | `domain/validation` + `FormState` en los ViewModels |
| IE 2.2.2 Animaciones | 10% | Fase 8 (+ errores animados desde la Fase 2) |
| IE 2.3.1 Modularidad + Room | 15% | Estructura del §4, Fase 1 |
| IE 2.3.2 GitHub + Trello | 20% | §6, issues y PRs, Trello actualizado cada semana |
| IE 2.4.1 Recursos nativos | 15% | GPS + cámara, Fase 4, `PermissionGate` |

---

## 8. Opcional: ML Kit (lectura automática del odómetro)

**Qué es:** una librería de Google con modelos de *machine learning* que corren **dentro del celular**,
sin internet. Uno de ellos, **Text Recognition**, recibe una imagen y devuelve el texto que encuentra en ella.

**Cómo se usaría aquí:** después de tomar la foto del odómetro, se le pasa la imagen a ML Kit, se
extraen los números que encuentre y se **prellena el campo de km**. El técnico solo confirma o corrige.
La foto pasa de ser un trámite a **ahorrar tiempo de escritura**.

**Implementación (aprox. 20 líneas):**
1. Dependencia: `com.google.mlkit:text-recognition` (versión con el modelo incluido en la app, así funciona sin conexión).
2. `InputImage.fromFilePath(context, fotoUri)` → `TextRecognition.getClient(...).process(imagen)`.
3. Del resultado, quedarse con la secuencia de dígitos más larga y ponerla en `FormState.km`.
4. Siempre editable: los odómetros digitales y los reflejos engañan al reconocimiento.

**Antes de implementarlo:** confirmar con el docente que cuenta como "local" (no hace llamadas HTTP,
pero es una librería de Google). Solo si las Fases 0–9 ya están listas.

---

## 9. Postergado para la EFT
- Servidor (Spring Boot) y sincronización real (WorkManager + `estadoSync` en cada traslado).
- Distancias oficiales con OpenRouteService (calculadas una vez en el servidor y versionadas).
- Precio del combustible desde la API pública de la CNE.
- Exportar el reporte de costos (CSV/PDF) para respaldar la facturación.
