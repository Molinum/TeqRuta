"""Datos de ejemplo: réplica de SeedData.kt de la app (mismos ids, nombres y uuids de las
jornadas), para que servidor y dispositivos partan del mismo catálogo y las mismas jornadas.

Usuarios de prueba (correo / contraseña):
  admin@teqmed.cl / admin123          jefe@teqmed.cl / jefe123
  gerencia@teqmed.cl / gerencia123    tecnico@teqmed.cl / tecnico123
  tecnico2@teqmed.cl / tecnico123     inactivo@teqmed.cl / inactivo123 (desactivado)
"""
import hashlib
from datetime import datetime, timedelta

import costos

INICIO_2026 = 1_767_225_600_000  # 01-01-2026 00:00 UTC
MINUTO = 60_000
PRECIO_LITRO = 1290.0


def sha256(texto):
    return hashlib.sha256(texto.encode()).hexdigest()


def poblar(conn):
    usuarios(conn)
    vehiculos(conn)
    destinos_y_tramos(conn)
    conn.execute("INSERT INTO tarifas_combustible (precioLitro, vigenteDesde) VALUES (?, ?)",
                 (PRECIO_LITRO, INICIO_2026))
    historial(conn)


def usuarios(conn):
    def nuevo(nombre, email, password, rol, jefe_id=None, activo=1):
        return conn.execute(
            "INSERT INTO usuarios (nombre, email, passwordHash, rol, jefeId, activo) VALUES (?,?,?,?,?,?)",
            (nombre, email, sha256(password), rol, jefe_id, activo)).lastrowid

    nuevo("Ana Pérez", "admin@teqmed.cl", "admin123", "ADMIN")
    jefe = nuevo("Jorge Soto", "jefe@teqmed.cl", "jefe123", "JEFE")
    nuevo("Carla Muñoz", "gerencia@teqmed.cl", "gerencia123", "GERENCIA")
    nuevo("Diego Rojas", "tecnico@teqmed.cl", "tecnico123", "TECNICO", jefe)
    nuevo("Felipe Vera", "tecnico2@teqmed.cl", "tecnico123", "TECNICO", jefe)
    nuevo("Pablo Díaz", "inactivo@teqmed.cl", "inactivo123", "TECNICO", jefe, activo=0)


def vehiculos(conn):
    for patente, desc, tipo, rend in [
        ("RTKJ45", "Toyota Hilux 2.4", "EMPRESA", 10.5),
        ("PXLV12", "Nissan NP300", "EMPRESA", 11.2),
        ("LKHP83", "Suzuki Swift", "PARTICULAR", 16.0),
    ]:
        conn.execute("INSERT INTO vehiculos (patente, descripcion, tipo, rendimientoKmL) VALUES (?,?,?,?)",
                     (patente, desc, tipo, rend))


def destinos_y_tramos(conn):
    def destino(nombre, direccion, ciudad, lat, lon, tipo):
        return conn.execute(
            "INSERT INTO destinos (nombre, direccion, ciudad, lat, lon, tipo) VALUES (?,?,?,?,?,?)",
            (nombre, direccion, ciudad, lat, lon, tipo)).lastrowid

    sede = destino("Sede TEQMED", "Av. Arturo Prat 390", "Concepción", -36.8285, -73.0605, "SEDE")
    sanatorio = destino("Clínica Sanatorio Alemán", "Av. Pedro de Valdivia 801", "Concepción", -36.8360, -73.0430, "CLINICA")
    regional = destino("Hospital Regional Guillermo Grant Benavente", "San Martín 1436", "Concepción", -36.8190, -73.0390, "CLINICA")
    biobio = destino("Clínica Biobío", "Av. Jorge Alessandri 3515", "Talcahuano", -36.7880, -73.0790, "CLINICA")
    herminda = destino("Hospital Clínico Herminda Martín", "Francisco Ramírez 10", "Chillán", -36.6120, -72.0950, "CLINICA")
    chillan = destino("Clínica Chillán", "Pedro Aguirre Cerda 35", "Chillán", -36.6060, -72.1000, "CLINICA")
    los_angeles = destino("Complejo Asistencial Dr. Víctor Ríos Ruiz", "Ricardo Vicuña 147", "Los Ángeles", -37.4700, -72.3560, "CLINICA")
    temuco = destino("Hospital Dr. Hernán Henríquez Aravena", "Manuel Montt 115", "Temuco", -38.7360, -72.6000, "CLINICA")
    alemana = destino("Clínica Alemana Temuco", "Senador Estébanez 645", "Temuco", -38.7350, -72.6050, "CLINICA")

    # Tramos de ida y vuelta. A propósito no hay tramos entre ciudades distintas (ej. Chillán →
    # Los Ángeles) para poder probar los traslados no catalogados.
    for (a, b), km, peaje in [
        ((sede, sanatorio), 2.1, 0), ((sede, regional), 2.4, 0), ((sede, biobio), 9.5, 0),
        ((sede, herminda), 108.0, 3200), ((sede, chillan), 107.0, 3200),
        ((sede, los_angeles), 136.0, 3600), ((sede, temuco), 292.0, 7400),
        ((sede, alemana), 293.0, 7400), ((sanatorio, regional), 2.0, 0),
        ((regional, biobio), 9.0, 0), ((herminda, chillan), 1.2, 0), ((temuco, alemana), 0.8, 0),
    ]:
        for origen, dest in ((a, b), (b, a)):
            conn.execute(
                "INSERT INTO tramos (origenId, destinoId, kmOficial, peaje, vigenteDesde) VALUES (?,?,?,?,?)",
                (origen, dest, km, peaje, INICIO_2026))


def historial(conn):
    """Seis jornadas de los últimos 20 días en distintos estados (igual que la app)."""
    ids = {r["email"]: r["id"] for r in conn.execute("SELECT id, email FROM usuarios")}
    veh = {r["patente"]: dict(r) for r in conn.execute("SELECT * FROM vehiculos")}
    dest = {r["nombre"]: dict(r) for r in conn.execute("SELECT * FROM destinos")}
    sede = next(d for d in dest.values() if d["tipo"] == "SEDE")
    km_vehiculo = {veh["RTKJ45"]["id"]: 45_210, veh["PXLV12"]["id"]: 78_540}
    ahora = int(datetime.now().timestamp() * 1000)
    contador = [0]

    def jornada(tecnico, vehiculo, dias_atras, estado, viajes, comentario=None, validada_por_jefe=False):
        contador[0] += 1
        n = contador[0]
        inicio = int((datetime.now() - timedelta(days=dias_atras)).replace(
            hour=8, minute=0, second=0, microsecond=0).timestamp() * 1000)
        km_inicial = km_vehiculo[vehiculo["id"]]
        jornada_id = conn.execute(
            "INSERT INTO jornadas (uuid, tecnicoId, vehiculoId, inicio, kmInicial, actualizadoEn) VALUES (?,?,?,?,?,?)",
            (f"seed-j{n}", tecnico, vehiculo["id"], inicio, km_inicial, ahora)).lastrowid
        km, hora, origen = km_inicial, inicio + 15 * MINUTO, sede
        for i, (nombre, recorridos, motivo, detalle) in enumerate(viajes):
            destino = sede if nombre == sede["nombre"] else dest[nombre]
            tramo = conn.execute(
                "SELECT * FROM tramos WHERE origenId=? AND destinoId=? AND vigenteDesde<=? "
                "AND (vigenteHasta IS NULL OR vigenteHasta>?) ORDER BY vigenteDesde DESC LIMIT 1",
                (origen["id"], destino["id"], hora, hora)).fetchone()
            km_oficial = tramo["kmOficial"] if tramo else None
            peaje = tramo["peaje"] if tramo else None
            costo, pct, marca = costos.calcular(
                recorridos, km_oficial, peaje or 0, vehiculo["rendimientoKmL"], PRECIO_LITRO)
            llegada = hora + (20 + recorridos // 2) * MINUTO
            conn.execute(
                "INSERT INTO traslados (uuid, jornadaId, orden, origenId, destinoId, tramoId, horaSalida, "
                "horaLlegada, kmInicio, kmFin, kmOficialUsado, peajeUsado, precioLitroUsado, rendimientoUsado, "
                "costo, desviacionPct, marca, motivoTipo, motivoDetalle) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                (f"seed-j{n}-t{i + 1}", jornada_id, i + 1, origen["id"], destino["id"],
                 tramo["id"] if tramo else None, hora, llegada, km, km + recorridos, km_oficial, peaje,
                 PRECIO_LITRO, vehiculo["rendimientoKmL"], costo, pct, marca, motivo, detalle))
            km += recorridos
            hora = llegada + 45 * MINUTO
            origen = destino
        conn.execute(
            "UPDATE jornadas SET fin=?, kmFinal=?, estado=?, comentarioJefe=?, validacionAutomatica=? WHERE id=?",
            (hora, km, estado, comentario, int(estado == "VALIDADA" and not validada_por_jefe), jornada_id))
        km_vehiculo[vehiculo["id"]] = km

    diego, felipe = ids["tecnico@teqmed.cl"], ids["tecnico2@teqmed.cl"]
    hilux, nissan = veh["RTKJ45"], veh["PXLV12"]
    sanatorio, regional = "Clínica Sanatorio Alemán", "Hospital Regional Guillermo Grant Benavente"
    biobio, herminda = "Clínica Biobío", "Hospital Clínico Herminda Martín"
    chillan, los_angeles = "Clínica Chillán", "Complejo Asistencial Dr. Víctor Ríos Ruiz"
    s = sede["nombre"]

    jornada(diego, hilux, 20, "VALIDADA", [(sanatorio, 2, None, None), (regional, 2, None, None), (s, 3, None, None)],
            validada_por_jefe=True)
    jornada(felipe, nissan, 12, "RECHAZADA",
            [(biobio, 16, "RUTA_ALTERNATIVA", "Cierre de calle por obras"), (s, 10, None, None)],
            comentario="El trayecto a Clínica Biobío figura con 16 km y el oficial es 9,5. Revisa el odómetro.")
    jornada(diego, hilux, 9, "VALIDADA", [(herminda, 110, None, None), (chillan, 2, None, None), (s, 109, None, None)])
    jornada(felipe, nissan, 6, "VALIDADA", [(los_angeles, 139, None, None), (s, 138, None, None)])
    jornada(felipe, nissan, 3, "CERRADA",
            [(biobio, 15, "PARADA_ADICIONAL", "Retiro de repuestos en bodega"), (s, 10, None, None)])
    jornada(diego, hilux, 2, "CERRADA",
            [(herminda, 109, None, None),
             (los_angeles, 105, "TRAYECTO_NUEVO", "Cliente nuevo sin ruta registrada"),
             (s, 137, None, None)])
