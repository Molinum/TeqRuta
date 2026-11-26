"""API REST de TeqRuta (Flask + SQLite).

Es la única fuente de datos de la app: guarda el catálogo (usuarios, vehículos, destinos,
tramos, tarifas) y las jornadas de todos los técnicos. La app lee y escribe solo a través de
esta API; el servidor valida las reglas de negocio y recalcula los costos.

Uso:  python app.py            (crea teqruta.db con datos de ejemplo si no existe)
      python app.py --reset    (borra y recrea la base de datos)
Variables de entorno opcionales: TEQRUTA_DB, TEQRUTA_API_KEY, PORT.
"""
import os
import re
import sqlite3
import sys
import time

from flask import Flask, g, jsonify, request

import costos
import seed
from seed import sha256

BASE = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.environ.get("TEQRUTA_DB", os.path.join(BASE, "teqruta.db"))
# La URL de ngrok es pública: sin esta clave cualquiera que la conozca podría leer y escribir.
# Debe coincidir con API_KEY en ApiConfig.kt de la app.
API_KEY = os.environ.get("TEQRUTA_API_KEY", "teqruta-dev-key")

REGEX_CORREO = re.compile(r"^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$")
REGEX_PATENTE = re.compile(r"^([A-Z]{2}\d{4}|[A-Z]{4}\d{2})$")
ROLES = ("TECNICO", "JEFE", "GERENCIA", "ADMIN")
ESTADOS_FINALES = ("VALIDADA", "RECHAZADA")

app = Flask(__name__)
app.json.sort_keys = False
app.json.ensure_ascii = False


# ---------------------------------------------------------------- base de datos

def init_db(reset=False):
    if reset and os.path.exists(DB_PATH):
        os.remove(DB_PATH)
    if os.path.exists(DB_PATH):
        return False
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    try:
        with open(os.path.join(BASE, "schema.sql"), encoding="utf-8") as f:
            conn.executescript(f.read())
        seed.poblar(conn)
        conn.commit()
    except Exception:
        conn.close()
        os.remove(DB_PATH)
        raise
    conn.close()
    return True


def db():
    if "db" not in g:
        g.db = sqlite3.connect(DB_PATH)
        g.db.row_factory = sqlite3.Row
        g.db.execute("PRAGMA foreign_keys = ON")
    return g.db


@app.teardown_appcontext
def cerrar_db(_):
    conexion = g.pop("db", None)
    if conexion is not None:
        conexion.close()


def ahora_ms():
    return int(time.time() * 1000)


# ---------------------------------------------------------------- errores y acceso

class ApiError(Exception):
    def __init__(self, mensaje, status=400):
        super().__init__(mensaje)
        self.mensaje, self.status = mensaje, status


@app.errorhandler(ApiError)
def manejar_api_error(e):
    return jsonify(error=e.mensaje), e.status


@app.errorhandler(404)
def no_encontrado(_):
    return jsonify(error="No encontrado"), 404


@app.errorhandler(Exception)
def error_inesperado(e):
    app.logger.exception(e)
    return jsonify(error="Error interno del servidor"), 500


@app.before_request
def exigir_clave():
    if request.path.startswith("/api/") and request.path != "/api/health":
        if request.headers.get("X-API-Key") != API_KEY:
            raise ApiError("Clave de API inválida", 401)


def cuerpo():
    datos = request.get_json(silent=True)
    if not isinstance(datos, dict):
        raise ApiError("Se esperaba un objeto JSON")
    return datos


# ---------------------------------------------------------------- validación

def texto(d, campo, etiqueta):
    valor = d.get(campo)
    if not isinstance(valor, str) or not valor.strip():
        raise ApiError(f"{etiqueta} es obligatorio")
    return valor.strip()


def numero(d, campo, etiqueta, minimo=None, maximo=None, estricto=False):
    valor = d.get(campo)
    if isinstance(valor, bool) or not isinstance(valor, (int, float)):
        raise ApiError(f"{etiqueta} debe ser un número")
    if minimo is not None and (valor <= minimo if estricto else valor < minimo):
        raise ApiError(f"{etiqueta} debe ser mayor{'' if estricto else ' o igual'} a {minimo}")
    if maximo is not None and valor > maximo:
        raise ApiError(f"{etiqueta} debe ser menor o igual a {maximo}")
    return valor


def entero(d, campo, etiqueta, minimo=None):
    valor = numero(d, campo, etiqueta, minimo)
    if int(valor) != valor:
        raise ApiError(f"{etiqueta} debe ser un número entero")
    return int(valor)


def opcion(d, campo, etiqueta, validos):
    valor = d.get(campo)
    if valor not in validos:
        raise ApiError(f"{etiqueta} inválido (use: {', '.join(validos)})")
    return valor


def fila(consulta, *args):
    return db().execute(consulta, args).fetchone()


def a_dict(r, booleanos=()):
    if r is None:
        return None
    d = dict(r)
    for campo in booleanos:
        d[campo] = bool(d[campo])
    return d


def usuario_dict(r):
    """Usuario sin el hash de su contraseña: el login lo valida el servidor y el hash no sale de aquí."""
    d = a_dict(r, ("activo",))
    if d is not None:
        d["passwordHash"] = ""
    return d


def guardar(tabla, columnas, valores, id_existente):
    """INSERT o UPDATE según haya id. Devuelve el id."""
    conexion = db()
    if id_existente:
        sets = ", ".join(f"{c} = ?" for c in columnas)
        conexion.execute(f"UPDATE {tabla} SET {sets} WHERE id = ?", (*valores, id_existente))
        return id_existente
    marcas = ", ".join("?" for _ in columnas)
    return conexion.execute(
        f"INSERT INTO {tabla} ({', '.join(columnas)}) VALUES ({marcas})", valores).lastrowid


def id_de(d):
    valor = d.get("id") or 0
    if not isinstance(valor, int) or valor < 0:
        raise ApiError("id inválido")
    return valor


# ---------------------------------------------------------------- salud y catálogo

@app.get("/api/health")
def health():
    return jsonify(ok=True, servicio="teqruta", ahora=ahora_ms())


@app.post("/api/login")
def login():
    """Valida correo y contraseña. Mismo mensaje si el correo no existe o la clave no coincide (RF-AUT-01)."""
    d = cuerpo()
    usuario = fila("SELECT * FROM usuarios WHERE email = ?", texto(d, "email", "El correo").lower())
    if not usuario or usuario["passwordHash"] != sha256(texto(d, "password", "La contraseña")):
        raise ApiError("Correo o contraseña incorrectos", 401)
    if not usuario["activo"]:
        raise ApiError("Tu usuario está desactivado. Contacta al administrador.", 403)
    return jsonify(usuario_dict(usuario))


@app.get("/api/catalogos")
def catalogos():
    conexion = db()
    return jsonify(
        usuarios=[usuario_dict(r) for r in conexion.execute("SELECT * FROM usuarios ORDER BY id")],
        vehiculos=[a_dict(r, ("activo",)) for r in conexion.execute("SELECT * FROM vehiculos ORDER BY id")],
        destinos=[a_dict(r, ("activo",)) for r in conexion.execute("SELECT * FROM destinos ORDER BY id")],
        tramos=[a_dict(r) for r in conexion.execute("SELECT * FROM tramos ORDER BY id")],
        tarifas=[a_dict(r) for r in conexion.execute("SELECT * FROM tarifas_combustible ORDER BY id")],
    )


# ---------------------------------------------------------------- mantenedores (admin)

@app.post("/api/destinos")
def guardar_destino():
    d = cuerpo()
    id_ = id_de(d)
    nombre = texto(d, "nombre", "El nombre")
    direccion = texto(d, "direccion", "La dirección")
    ciudad = texto(d, "ciudad", "La ciudad")
    lat = numero(d, "lat", "La latitud", -90, 90)
    lon = numero(d, "lon", "La longitud", -180, 180)
    tipo = opcion(d, "tipo", "El tipo", ("SEDE", "CLINICA"))
    activo = 1 if d.get("activo", True) else 0
    actual = fila("SELECT * FROM destinos WHERE id = ?", id_) if id_ else None
    if id_ and not actual:
        raise ApiError("Destino no encontrado", 404)
    if fila("SELECT 1 FROM destinos WHERE lower(trim(nombre)) = lower(?) AND id != ?", nombre, id_):
        raise ApiError("Ya existe un destino con ese nombre")
    # La app necesita exactamente una sede activa para registrar jornadas (RN-09)
    otra_sede = fila("SELECT 1 FROM destinos WHERE tipo='SEDE' AND activo=1 AND id != ?", id_)
    if tipo == "SEDE" and activo and otra_sede:
        raise ApiError("Ya existe una sede activa")
    if actual and actual["tipo"] == "SEDE" and actual["activo"] and not (tipo == "SEDE" and activo) and not otra_sede:
        raise ApiError("Debe existir una sede activa")
    nuevo = guardar("destinos", ("nombre", "direccion", "ciudad", "lat", "lon", "tipo", "activo"),
                    (nombre, direccion, ciudad, lat, lon, tipo, activo), id_)
    db().commit()
    return jsonify(a_dict(fila("SELECT * FROM destinos WHERE id = ?", nuevo), ("activo",)))


@app.post("/api/vehiculos")
def guardar_vehiculo():
    d = cuerpo()
    id_ = id_de(d)
    patente = re.sub(r"[^A-Z0-9]", "", texto(d, "patente", "La patente").upper())
    if not REGEX_PATENTE.match(patente):
        raise ApiError("Patente inválida (ej. AB1234 o ABCD12)")
    descripcion = texto(d, "descripcion", "La descripción")
    tipo = opcion(d, "tipo", "El tipo", ("EMPRESA", "PARTICULAR"))
    rendimiento = numero(d, "rendimientoKmL", "El rendimiento", 0, estricto=True)
    activo = 1 if d.get("activo", True) else 0
    if id_ and not fila("SELECT 1 FROM vehiculos WHERE id = ?", id_):
        raise ApiError("Vehículo no encontrado", 404)
    if fila("SELECT 1 FROM vehiculos WHERE patente = ? AND id != ?", patente, id_):
        raise ApiError("Ya existe un vehículo con esa patente")
    nuevo = guardar("vehiculos", ("patente", "descripcion", "tipo", "rendimientoKmL", "activo"),
                    (patente, descripcion, tipo, rendimiento, activo), id_)
    db().commit()
    return jsonify(a_dict(fila("SELECT * FROM vehiculos WHERE id = ?", nuevo), ("activo",)))


@app.post("/api/usuarios")
def guardar_usuario():
    d = cuerpo()
    id_ = id_de(d)
    nombre = texto(d, "nombre", "El nombre")
    email = texto(d, "email", "El correo").lower()
    if not REGEX_CORREO.match(email):
        raise ApiError("Correo inválido")
    rol = opcion(d, "rol", "El rol", ROLES)
    jefe_id = d.get("jefeId") if rol == "TECNICO" else None
    if rol == "TECNICO" and not fila("SELECT 1 FROM usuarios WHERE id = ? AND rol = 'JEFE' AND activo = 1", jefe_id):
        raise ApiError("El técnico necesita un jefe activo a cargo")
    activo = 1 if d.get("activo", True) else 0
    actual = fila("SELECT * FROM usuarios WHERE id = ?", id_) if id_ else None
    if id_ and not actual:
        raise ApiError("Usuario no encontrado", 404)
    # Al editar sin escribir una contraseña nueva se conserva la actual
    password_hash = (d.get("passwordHash") or "").strip() or (actual["passwordHash"] if actual else "")
    if not password_hash:
        raise ApiError("La contraseña es obligatoria")
    if fila("SELECT 1 FROM usuarios WHERE email = ? AND id != ?", email, id_):
        raise ApiError("Ya existe un usuario con ese correo")
    nuevo = guardar("usuarios", ("nombre", "email", "passwordHash", "rol", "jefeId", "activo"),
                    (nombre, email, password_hash, rol, jefe_id, activo), id_)
    db().commit()
    return jsonify(usuario_dict(fila("SELECT * FROM usuarios WHERE id = ?", nuevo)))


@app.post("/api/tramos")
def guardar_tramo():
    """Crea el tramo o, si ya existe para ese par, una nueva versión (RF-ADM-04)."""
    d = cuerpo()
    origen = entero(d, "origenId", "El origen")
    destino = entero(d, "destinoId", "El destino")
    if origen == destino:
        raise ApiError("El destino debe ser distinto del origen")
    for id_ in (origen, destino):
        if not fila("SELECT 1 FROM destinos WHERE id = ?", id_):
            raise ApiError("Destino no encontrado", 404)
    km = numero(d, "kmOficial", "Los km oficiales", 0, estricto=True)
    peaje = entero(d, "peaje", "El peaje", 0)
    desde = entero(d, "vigenteDesde", "La vigencia")
    anterior = fila("SELECT * FROM tramos WHERE origenId=? AND destinoId=? ORDER BY vigenteDesde DESC, id DESC LIMIT 1",
                    origen, destino)
    if anterior and desde < anterior["vigenteDesde"]:
        raise ApiError("La vigencia no puede ser anterior a la de la versión vigente")
    conexion = db()
    if anterior:
        conexion.execute("UPDATE tramos SET vigenteHasta = ? WHERE id = ?", (desde, anterior["id"]))
    nuevo = conexion.execute(
        "INSERT INTO tramos (origenId, destinoId, kmOficial, peaje, vigenteDesde) VALUES (?,?,?,?,?)",
        (origen, destino, km, peaje, desde)).lastrowid
    conexion.commit()
    return jsonify(a_dict(fila("SELECT * FROM tramos WHERE id = ?", nuevo)))


@app.post("/api/tarifas")
def registrar_tarifa():
    d = cuerpo()
    precio = numero(d, "precioLitro", "El precio", 0, estricto=True)
    desde = entero(d, "vigenteDesde", "La vigencia")
    ultima = fila("SELECT MAX(vigenteDesde) AS m FROM tarifas_combustible")["m"]
    if ultima is not None and desde < ultima:
        raise ApiError("La vigencia no puede ser anterior a la de la tarifa anterior")
    nuevo = db().execute("INSERT INTO tarifas_combustible (precioLitro, vigenteDesde) VALUES (?, ?)",
                         (precio, desde)).lastrowid
    db().commit()
    return jsonify(a_dict(fila("SELECT * FROM tarifas_combustible WHERE id = ?", nuevo)))


# ---------------------------------------------------------------- jornadas

CAMPOS_JORNADA = ("tecnicoId", "vehiculoId", "inicio", "fin", "kmInicial", "kmFinal", "fotoInicioUri",
                  "fotoCierreUri", "latInicio", "lonInicio", "estado", "comentarioJefe", "validacionAutomatica")
CAMPOS_TRASLADO = ("uuid", "orden", "origenId", "destinoId", "tramoId", "horaSalida", "horaLlegada", "kmInicio",
                   "kmFin", "latSalida", "lonSalida", "latLlegada", "lonLlegada", "fotoUri", "kmOficialUsado",
                   "peajeUsado", "precioLitroUsado", "rendimientoUsado", "costo", "desviacionPct", "marca",
                   "motivoTipo", "motivoDetalle")


def jornada_dict(r):
    """Jornada con sus traslados. Los ids los asigna el servidor; el uuid lo genera la app al crearla."""
    d = a_dict(r, ("validacionAutomatica",))
    d["traslados"] = []
    for t in db().execute("SELECT * FROM traslados WHERE jornadaId = ? ORDER BY orden", (r["id"],)):
        t = dict(t)
        del t["jornadaId"]
        d["traslados"].append(t)
    return d


@app.get("/api/vehiculos/<int:vehiculo_id>/ultimo-km")
def ultimo_km(vehiculo_id):
    """Km final de la última jornada cerrada del vehículo, de cualquier técnico (último odómetro conocido)."""
    r = fila("SELECT kmFinal FROM jornadas WHERE vehiculoId = ? AND kmFinal IS NOT NULL ORDER BY fin DESC LIMIT 1",
             vehiculo_id)
    return jsonify(kmFinal=r["kmFinal"] if r else None)


@app.get("/api/jornadas")
def listar_jornadas():
    """Jornadas que el usuario puede ver (RN-15) modificadas después de `desde`."""
    usuario = fila("SELECT * FROM usuarios WHERE id = ? AND activo = 1", request.args.get("usuarioId", type=int))
    if not usuario:
        raise ApiError("Usuario no encontrado", 404)
    desde = request.args.get("desde", 0, type=int)
    # Se fija antes de consultar: lo que se escriba mientras tanto llegará en la próxima descarga
    ahora = ahora_ms()
    filtro, args = {
        "TECNICO": ("j.tecnicoId = ?", (usuario["id"],)),
        "JEFE": ("j.tecnicoId IN (SELECT id FROM usuarios WHERE jefeId = ?)", (usuario["id"],)),
        "GERENCIA": ("j.estado = 'VALIDADA'", ()),  # los reportes solo consideran jornadas validadas (RN-12)
    }.get(usuario["rol"], ("0", ()))
    filas = db().execute(
        f"SELECT j.* FROM jornadas j WHERE {filtro} AND j.actualizadoEn > ? AND j.actualizadoEn <= ? "
        "ORDER BY j.actualizadoEn", (*args, desde, ahora)).fetchall()
    return jsonify(ahora=ahora, jornadas=[jornada_dict(r) for r in filas])


@app.put("/api/jornadas/<uuid>")
def subir_jornada(uuid):
    """Crea o actualiza una jornada con sus traslados. Idempotente: se puede reenviar sin riesgo."""
    d = cuerpo()
    existente = fila("SELECT * FROM jornadas WHERE uuid = ?", uuid)
    # Una jornada Validada o Rechazada no la modifica nadie (RN-10); un envío atrasado de un
    # dispositivo no puede reabrir una Cerrada.
    if existente and (existente["estado"] in ESTADOS_FINALES or
                      (existente["estado"] == "CERRADA" and d.get("estado") == "ABIERTA")):
        return jsonify(jornada_dict(existente) | {"ignorada": True})

    tecnico = entero(d, "tecnicoId", "El técnico")
    vehiculo = fila("SELECT * FROM vehiculos WHERE id = ?", entero(d, "vehiculoId", "El vehículo"))
    if not fila("SELECT 1 FROM usuarios WHERE id = ? AND rol = 'TECNICO'", tecnico):
        raise ApiError("Técnico no encontrado", 404)
    if not vehiculo:
        raise ApiError("Vehículo no encontrado", 404)
    valores = {
        "tecnicoId": tecnico, "vehiculoId": vehiculo["id"],
        "inicio": entero(d, "inicio", "El inicio"), "fin": d.get("fin"),
        "kmInicial": entero(d, "kmInicial", "El km inicial", 0), "kmFinal": d.get("kmFinal"),
        "fotoInicioUri": d.get("fotoInicioUri"), "fotoCierreUri": d.get("fotoCierreUri"),
        "latInicio": d.get("latInicio"), "lonInicio": d.get("lonInicio"),
        "estado": opcion(d, "estado", "El estado", ("ABIERTA", "CERRADA", "VALIDADA", "RECHAZADA")),
        "comentarioJefe": d.get("comentarioJefe"),
        "validacionAutomatica": 1 if d.get("validacionAutomatica") else 0,
    }
    if valores["estado"] == "ABIERTA":
        # Un técnico tiene a lo más una jornada abierta y un vehículo está en una sola (RN-06)
        if fila("SELECT 1 FROM jornadas WHERE tecnicoId = ? AND estado = 'ABIERTA' AND uuid != ?", tecnico, uuid):
            raise ApiError("Ya tienes una jornada abierta", 409)
        if fila("SELECT 1 FROM jornadas WHERE vehiculoId = ? AND estado = 'ABIERTA' AND uuid != ?",
                vehiculo["id"], uuid):
            raise ApiError("El vehículo está en uso en otra jornada abierta", 409)
    if valores["estado"] == "RECHAZADA" and not (valores["comentarioJefe"] or "").strip():
        raise ApiError("Para rechazar una jornada el comentario es obligatorio")

    traslados = d.get("traslados") or []
    if not isinstance(traslados, list):
        raise ApiError("traslados debe ser una lista")
    filas_traslado = [preparar_traslado(t, vehiculo) for t in sorted(traslados, key=lambda t: t.get("orden", 0))]
    # Si el catálogo del servidor cambia el resultado, una jornada validada sola vuelve al jefe
    if (valores["estado"] == "VALIDADA" and valores["validacionAutomatica"]
            and any(t["marca"] != "OK" for t in filas_traslado)):
        valores["estado"], valores["validacionAutomatica"] = "CERRADA", 0

    conexion = db()
    columnas = (*CAMPOS_JORNADA, "actualizadoEn")
    datos = (*(valores[c] for c in CAMPOS_JORNADA), ahora_ms())
    if existente:
        guardar("jornadas", columnas, datos, existente["id"])
        jornada_id = existente["id"]
    else:
        jornada_id = guardar("jornadas", ("uuid", *columnas), (uuid, *datos), None)
    # Los traslados se actualizan por uuid para que conserven su id entre escrituras
    uuids = [t["uuid"] for t in filas_traslado]
    conexion.execute(f"DELETE FROM traslados WHERE jornadaId = ? AND uuid NOT IN ({', '.join('?' * len(uuids))})",
                     (jornada_id, *uuids))
    for t in filas_traslado:
        actual = fila("SELECT id FROM traslados WHERE uuid = ?", t["uuid"])
        guardar("traslados", ("jornadaId", *CAMPOS_TRASLADO), (jornada_id, *(t[c] for c in CAMPOS_TRASLADO)),
                actual["id"] if actual else None)
    conexion.commit()
    return jsonify(jornada_dict(fila("SELECT * FROM jornadas WHERE id = ?", jornada_id)))


def preparar_traslado(t, vehiculo):
    """Valida un traslado y recalcula su costo con el catálogo del servidor (RF-EFT-04, RN-04)."""
    if not isinstance(t, dict):
        raise ApiError("Traslado inválido")
    fila_t = {c: t.get(c) for c in CAMPOS_TRASLADO}
    fila_t["uuid"] = texto(t, "uuid", "El uuid del traslado")
    fila_t["orden"] = entero(t, "orden", "El orden", 1)
    fila_t["origenId"] = entero(t, "origenId", "El origen")
    fila_t["destinoId"] = entero(t, "destinoId", "El destino")
    for id_ in (fila_t["origenId"], fila_t["destinoId"]):
        if not fila("SELECT 1 FROM destinos WHERE id = ?", id_):
            raise ApiError("Destino no encontrado", 404)
    salida = fila_t["horaSalida"] = entero(t, "horaSalida", "La hora de salida")
    fila_t["kmInicio"] = entero(t, "kmInicio", "El km de inicio", 0)
    if t.get("horaLlegada") is None or t.get("kmFin") is None:
        return fila_t  # traslado en curso: aún no hay nada que calcular
    if t["kmFin"] <= fila_t["kmInicio"]:
        raise ApiError("El km de llegada debe ser mayor al de salida")
    tramo = fila(
        "SELECT * FROM tramos WHERE origenId=? AND destinoId=? AND vigenteDesde<=? "
        "AND (vigenteHasta IS NULL OR vigenteHasta>?) ORDER BY vigenteDesde DESC LIMIT 1",
        fila_t["origenId"], fila_t["destinoId"], salida, salida)
    tarifa = fila("SELECT * FROM tarifas_combustible WHERE vigenteDesde<=? ORDER BY vigenteDesde DESC LIMIT 1", salida)
    if tarifa is None:
        raise ApiError("No hay una tarifa de combustible vigente para la fecha del traslado")
    costo, pct, marca = costos.calcular(
        t["kmFin"] - fila_t["kmInicio"], tramo["kmOficial"] if tramo else None,
        tramo["peaje"] if tramo else 0, vehiculo["rendimientoKmL"], tarifa["precioLitro"])
    fila_t.update(
        tramoId=tramo["id"] if tramo else None,
        kmOficialUsado=tramo["kmOficial"] if tramo else None,
        peajeUsado=tramo["peaje"] if tramo else None,
        precioLitroUsado=tarifa["precioLitro"], rendimientoUsado=vehiculo["rendimientoKmL"],
        costo=costo, desviacionPct=pct, marca=marca)
    if marca == "OK":
        fila_t["motivoTipo"] = fila_t["motivoDetalle"] = None
    elif not fila_t.get("motivoTipo"):
        raise ApiError("Un traslado con desviación o sin catalogar necesita un motivo")
    return fila_t


# ---------------------------------------------------------------- arranque

if __name__ == "__main__":
    creada = init_db(reset="--reset" in sys.argv)
    puerto = int(os.environ.get("PORT", 5000))
    print(f"Base de datos: {DB_PATH} ({'creada con datos de ejemplo' if creada else 'existente'})")
    print(f"Clave de API: {API_KEY}")
    app.run(host="0.0.0.0", port=puerto)
