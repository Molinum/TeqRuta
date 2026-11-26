-- Mismas tablas y columnas que las entidades Room de la app (data/local/entity), con los mismos
-- nombres, para que el JSON de la API coincida campo a campo con las clases de Kotlin.
-- Los enums se guardan por nombre ('TECNICO', 'CERRADA', ...) y los booleanos como 0/1.
PRAGMA foreign_keys = ON;

CREATE TABLE usuarios (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    passwordHash TEXT NOT NULL,
    rol TEXT NOT NULL CHECK (rol IN ('TECNICO', 'JEFE', 'GERENCIA', 'ADMIN')),
    jefeId INTEGER REFERENCES usuarios(id),
    activo INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE vehiculos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    patente TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL,
    tipo TEXT NOT NULL CHECK (tipo IN ('EMPRESA', 'PARTICULAR')),
    rendimientoKmL REAL NOT NULL CHECK (rendimientoKmL > 0),
    activo INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE destinos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    direccion TEXT NOT NULL,
    ciudad TEXT NOT NULL,
    lat REAL NOT NULL,
    lon REAL NOT NULL,
    tipo TEXT NOT NULL CHECK (tipo IN ('SEDE', 'CLINICA')),
    activo INTEGER NOT NULL DEFAULT 1
);

-- Cada edición de un tramo crea una versión nueva; la anterior se cierra con vigenteHasta
CREATE TABLE tramos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    origenId INTEGER NOT NULL REFERENCES destinos(id),
    destinoId INTEGER NOT NULL REFERENCES destinos(id),
    kmOficial REAL NOT NULL CHECK (kmOficial > 0),
    peaje INTEGER NOT NULL CHECK (peaje >= 0),
    vigenteDesde INTEGER NOT NULL,
    vigenteHasta INTEGER
);
CREATE INDEX index_tramos_origenId_destinoId ON tramos(origenId, destinoId);
CREATE INDEX index_tramos_destinoId ON tramos(destinoId);

CREATE TABLE tarifas_combustible (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    precioLitro REAL NOT NULL CHECK (precioLitro > 0),
    vigenteDesde INTEGER NOT NULL
);

-- uuid: identificador global de la jornada, generado en el dispositivo (los ids locales de
-- cada teléfono se repetirían). actualizadoEn: lo fija el servidor en cada escritura y es lo
-- que usan los dispositivos para pedir solo lo que cambió.
CREATE TABLE jornadas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid TEXT NOT NULL UNIQUE,
    tecnicoId INTEGER NOT NULL REFERENCES usuarios(id),
    vehiculoId INTEGER NOT NULL REFERENCES vehiculos(id),
    inicio INTEGER NOT NULL,
    fin INTEGER,
    kmInicial INTEGER NOT NULL,
    kmFinal INTEGER,
    fotoInicioUri TEXT,
    fotoCierreUri TEXT,
    latInicio REAL,
    lonInicio REAL,
    estado TEXT NOT NULL DEFAULT 'ABIERTA' CHECK (estado IN ('ABIERTA', 'CERRADA', 'VALIDADA', 'RECHAZADA')),
    comentarioJefe TEXT,
    validacionAutomatica INTEGER NOT NULL DEFAULT 0,
    actualizadoEn INTEGER NOT NULL
);
CREATE INDEX index_jornadas_tecnicoId ON jornadas(tecnicoId);
CREATE INDEX index_jornadas_vehiculoId ON jornadas(vehiculoId);
CREATE INDEX index_jornadas_actualizadoEn ON jornadas(actualizadoEn);

CREATE TABLE traslados (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    uuid TEXT NOT NULL UNIQUE,
    jornadaId INTEGER NOT NULL REFERENCES jornadas(id) ON DELETE CASCADE,
    orden INTEGER NOT NULL,
    origenId INTEGER NOT NULL REFERENCES destinos(id),
    destinoId INTEGER NOT NULL REFERENCES destinos(id),
    tramoId INTEGER,
    horaSalida INTEGER NOT NULL,
    horaLlegada INTEGER,
    kmInicio INTEGER NOT NULL,
    kmFin INTEGER,
    latSalida REAL,
    lonSalida REAL,
    latLlegada REAL,
    lonLlegada REAL,
    fotoUri TEXT,
    kmOficialUsado REAL,
    peajeUsado INTEGER,
    precioLitroUsado REAL,
    rendimientoUsado REAL,
    costo REAL,
    desviacionPct REAL,
    marca TEXT CHECK (marca IN ('OK', 'DESVIACION', 'NO_CATALOGADO')),
    motivoTipo TEXT CHECK (motivoTipo IN
        ('RUTA_ALTERNATIVA', 'PARADA_ADICIONAL', 'ERROR_ODOMETRO', 'TRAYECTO_NUEVO', 'OTRO')),
    motivoDetalle TEXT
);
CREATE INDEX index_traslados_jornadaId ON traslados(jornadaId);
CREATE INDEX index_traslados_origenId ON traslados(origenId);
CREATE INDEX index_traslados_destinoId ON traslados(destinoId);
CREATE INDEX index_traslados_tramoId ON traslados(tramoId);
