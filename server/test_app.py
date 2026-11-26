"""Pruebas de la API. Usan una base de datos temporal con los datos de ejemplo.

Uso:  .venv/bin/python -m unittest -v test_app
"""
import os
import tempfile
import unittest
import uuid

os.environ["TEQRUTA_DB"] = os.path.join(tempfile.mkdtemp(), "test.db")

import app as api  # noqa: E402  (la ruta de la base se fija antes de importar)

CLAVE = {"X-API-Key": api.API_KEY}


class ApiTest(unittest.TestCase):

    def setUp(self):
        api.init_db(reset=True)
        self.cliente = api.app.test_client()
        catalogo = self.cliente.get("/api/catalogos", headers=CLAVE).get_json()
        self.tecnico = next(u for u in catalogo["usuarios"] if u["email"] == "tecnico@teqmed.cl")
        self.vehiculo = catalogo["vehiculos"][0]
        sede = next(d for d in catalogo["destinos"] if d["tipo"] == "SEDE")
        self.tramo = next(t for t in catalogo["tramos"] if t["origenId"] == sede["id"])

    def jornada(self, traslados=(), vehiculo=None, tecnico=None):
        return {
            "uuid": str(uuid.uuid4()), "tecnicoId": (tecnico or self.tecnico)["id"],
            "vehiculoId": (vehiculo or self.vehiculo)["id"], "inicio": 1_000, "kmInicial": 1_000,
            "estado": "ABIERTA", "traslados": list(traslados),
        }

    def traslado(self, km_recorridos, motivo=None):
        return {
            "uuid": str(uuid.uuid4()), "orden": 1, "origenId": self.tramo["origenId"],
            "destinoId": self.tramo["destinoId"], "horaSalida": api.ahora_ms(), "horaLlegada": api.ahora_ms(),
            "kmInicio": 1_000, "kmFin": 1_000 + km_recorridos, "motivoTipo": motivo,
        }

    def subir(self, jornada):
        return self.cliente.put(f"/api/jornadas/{jornada['uuid']}", json=jornada, headers=CLAVE)

    def test_login_correcto_devuelve_el_usuario(self):
        r = self.cliente.post("/api/login", json={"email": "tecnico@teqmed.cl", "password": "tecnico123"},
                              headers=CLAVE)
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.get_json()["rol"], "TECNICO")

    def test_login_con_clave_incorrecta_es_rechazado(self):
        r = self.cliente.post("/api/login", json={"email": "tecnico@teqmed.cl", "password": "otra"},
                              headers=CLAVE)
        self.assertEqual(r.status_code, 401)

    def test_sin_clave_de_api_no_se_puede_leer(self):
        self.assertEqual(self.cliente.get("/api/catalogos").status_code, 401)

    def test_un_vehiculo_no_puede_estar_en_dos_jornadas_abiertas(self):
        self.assertEqual(self.subir(self.jornada()).status_code, 200)
        catalogo = self.cliente.get("/api/catalogos", headers=CLAVE).get_json()
        otro_tecnico = next(u for u in catalogo["usuarios"] if u["email"] == "tecnico2@teqmed.cl")
        self.assertEqual(self.subir(self.jornada(tecnico=otro_tecnico)).status_code, 409)

    def test_el_servidor_calcula_el_costo_y_exige_motivo_si_hay_desviacion(self):
        km_desviados = int(self.tramo["kmOficial"] * 1.5) + 3
        sin_motivo = self.subir(self.jornada([self.traslado(km_desviados)]))
        self.assertEqual(sin_motivo.status_code, 400)

        con_motivo = self.subir(self.jornada([self.traslado(km_desviados, motivo="RUTA_ALTERNATIVA")]))
        traslado = con_motivo.get_json()["traslados"][0]
        self.assertEqual(traslado["marca"], "DESVIACION")
        self.assertGreater(traslado["costo"], 0)


if __name__ == "__main__":
    unittest.main()
