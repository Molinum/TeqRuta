"""Cálculo de costo y detección de desviación. Réplica exacta de CalculadoraCosto y
DetectorDesviacion de la app (RN-01 a RN-03): el servidor recalcula cada traslado que recibe
para que el costo quede centralizado y auditable (RF-EFT-04)."""

UMBRAL_DESVIACION = 0.15
TOLERANCIA_MINIMA_KM = 2.0


def calcular(km_declarados, km_oficial, peaje, rendimiento_km_l, precio_litro):
    """Devuelve (costo, desviacion_pct, marca)."""
    km_cobrados = km_oficial if km_oficial is not None else float(km_declarados)
    peaje_aplicado = 0 if km_oficial is None else peaje
    costo = km_cobrados / rendimiento_km_l * precio_litro + peaje_aplicado
    if km_oficial is None:
        return costo, None, "NO_CATALOGADO"
    pct = (km_declarados - km_oficial) / km_oficial
    desviado = pct > UMBRAL_DESVIACION and (km_declarados - km_oficial) > TOLERANCIA_MINIMA_KM
    return costo, pct, "DESVIACION" if desviado else "OK"
