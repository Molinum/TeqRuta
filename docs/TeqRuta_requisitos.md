# TeqRuta — Requisitos funcionales

> Extraídos del caso *"TeqRuta – Traslados en Terreno"* (TEQMED SpA, DSY1105) y de las decisiones del
> plan de desarrollo (`TeqRuta_notas.md`). Por indicación del docente, en esta etapa **solo se levantan
> requisitos funcionales**

**Convenciones**
- **ID:** `RF-<módulo>-<n>`. Las reglas de negocio se identifican como `RN-<n>`.
- **Prioridad:** **Alta** = imprescindible para la EP2 · **Media** = deseable en la EP2 · **Baja** = si sobra tiempo.
- **Origen:** sección del caso que lo justifica (§2.4, §3.1…) o **[Diseño]** si sale de una decisión del plan.
- Los requisitos marcados **EFT** se derivan del caso, pero quedan fuera de la EP2 porque requieren conexión externa (§7).

---

## 1. Actores

| Actor | Descripción (caso §3.1) |
|---|---|
| **Técnico** | Registra sus traslados desde el celular al iniciar y finalizar cada visita. |
| **Jefe de servicio técnico** | Valida los traslados de su equipo y revisa las desviaciones detectadas. |
| **Gerencia / Finanzas** | Consulta el costo real de traslado por cliente para evaluar la rentabilidad de los contratos. |
| **Administrador** | Mantiene actualizados los vehículos, destinos, tramos y tarifas. |

---

## 2. Autenticación y sesión (`AUT`)

| ID | Requisito | Actor | Prioridad | Origen |
|---|---|---|---|---|
| RF-AUT-01 | El sistema debe permitir iniciar sesión con correo y contraseña. | Todos | Alta | §3.1 |
| RF-AUT-02 | El sistema debe redirigir al usuario a la pantalla de inicio correspondiente a su rol después de iniciar sesión. | Todos | Alta | §3.1 |
| RF-AUT-03 | El sistema debe impedir que un usuario acceda a funciones que no corresponden a su rol. | Todos | Alta | §3.1 |
| RF-AUT-04 | El sistema debe permitir cerrar la sesión. | Todos | Alta | [Diseño] |
| RF-AUT-05 | El sistema debe impedir el inicio de sesión de usuarios desactivados. | Todos | Media | [Diseño] |

---

## 3. Registro de jornada y traslados (`TEC`)

| ID | Requisito | Actor | Prioridad | Origen |
|---|---|---|---|---|
| RF-TEC-01 | El sistema debe permitir al técnico **iniciar una jornada** indicando el vehículo utilizado y el kilometraje inicial del odómetro. La fecha, la hora y la sede de origen se registran automáticamente. | Técnico | Alta | §2.2, §3.2 |
| RF-TEC-02 | Al iniciar la jornada, el sistema debe exigir una **fotografía del odómetro** tomada con la cámara del dispositivo. | Técnico | Alta | [Diseño] |
| RF-TEC-03 | Al iniciar la jornada, el sistema debe registrar automáticamente la **ubicación GPS** del dispositivo. | Técnico | Alta | [Diseño] |
| RF-TEC-04 | El sistema debe permitir registrar la **salida hacia un destino**, seleccionándolo del catastro de clínicas y hospitales. La hora y la ubicación de salida se registran automáticamente. | Técnico | Alta | §3.1, §3.2 |
| RF-TEC-05 | Si no existe un tramo oficial entre el origen y el destino elegidos, el sistema debe avisar al técnico y permitirle continuar; el traslado queda marcado como **no catalogado**. | Técnico | Alta | [Diseño] |
| RF-TEC-06 | El sistema debe permitir registrar la **llegada** a un destino indicando el kilometraje actual del odómetro. La hora y la ubicación de llegada se registran automáticamente. | Técnico | Alta | §3.1, §3.2 |
| RF-TEC-07 | El sistema debe completar automáticamente el kilometraje de inicio de cada traslado con el kilometraje registrado en la llegada anterior (o con el inicial de la jornada). | Técnico | Alta | §3.3 |
| RF-TEC-08 | El sistema debe permitir adjuntar opcionalmente una fotografía del odómetro al registrar una llegada. | Técnico | Media | [Diseño] |
| RF-TEC-09 | Al registrar una llegada, el sistema debe **calcular el costo del traslado** y su **marca** (OK, desviación o no catalogado) según RN-01 a RN-04, y mostrárselos al técnico. | Técnico | Alta | §2.4, §4.1 |
| RF-TEC-10 | El sistema debe mostrar un **resumen de la jornada en curso**: traslados registrados, km acumulados y costo acumulado. | Técnico | Media | §3.3 |
| RF-TEC-11 | El sistema debe permitir **cerrar la jornada** registrando el traslado de regreso a la sede, el kilometraje final y una **fotografía obligatoria** del odómetro. | Técnico | Alta | §2.2, [Diseño] |
| RF-TEC-12 | Al cerrar la jornada, el sistema debe cambiar su estado a **Cerrada** y dejarla disponible para la validación del jefe. | Técnico | Alta | §3.1 |
| RF-TEC-13 | El sistema debe permitir al técnico consultar el **historial de sus jornadas** con su estado y, si fue rechazada, el comentario del jefe. | Técnico | Media | [Diseño] |
| RF-TEC-14 | El sistema debe permitir registrar jornadas y traslados **sin conexión a internet**, sin pérdida de datos. | Técnico | Alta | §3.3, §5.1 |
| RF-TEC-15 | Si el permiso de cámara o de ubicación es denegado, el sistema debe explicar al técnico para qué se necesita y ofrecerle volver a solicitarlo (ver RN-08). | Técnico | Alta | [Diseño] |

---

## 4. Validación de jornadas (`JEF`)

| ID | Requisito | Actor | Prioridad | Origen |
|---|---|---|---|---|
| RF-JEF-01 | El sistema debe listar las jornadas **Cerradas** de los técnicos a cargo del jefe, pendientes de validación. | Jefe | Alta | §3.1 |
| RF-JEF-02 | El sistema debe **destacar visualmente** las jornadas que contienen traslados con desviación o no catalogados, y mostrarlas primero en la lista. | Jefe | Alta | §3.1, §4.1 |
| RF-JEF-03 | El sistema debe mostrar el **detalle de una jornada**: cada traslado con origen, destino, horarios, km oficiales vs declarados, % de desviación, costo, ubicaciones registradas y fotografías del odómetro. | Jefe | Alta | §2.3, §4.1 |
| RF-JEF-04 | El sistema debe permitir **validar** una jornada, cambiando su estado a **Validada**. | Jefe | Alta | §3.1 |
| RF-JEF-05 | El sistema debe permitir **rechazar** una jornada ingresando un comentario obligatorio, cambiando su estado a **Rechazada**. | Jefe | Alta | §2.3, §3.1 |
| RF-JEF-06 | El sistema debe permitir filtrar las jornadas por técnico, rango de fechas y estado. | Jefe | Baja | [Diseño] |

---

## 5. Reportes de costos (`GER`)

| ID | Requisito | Actor | Prioridad | Origen |
|---|---|---|---|---|
| RF-GER-01 | El sistema debe mostrar el **costo total de traslado por cliente** (clínica u hospital) en un período. | Gerencia | Alta | §2.3, §3.1, §4.1 |
| RF-GER-02 | El sistema debe mostrar el **costo total de traslado por técnico** en un período. | Gerencia | Alta | §2.3, §2.4 |
| RF-GER-03 | El sistema debe mostrar el **costo total de traslado por vehículo** en un período. | Gerencia | Alta | §2.3, §2.4 |
| RF-GER-04 | El sistema debe permitir seleccionar el período del reporte por mes. | Gerencia | Alta | §2.2 |
| RF-GER-05 | El sistema debe permitir ver el detalle de los traslados que componen el costo de un cliente. | Gerencia | Media | §4.1 |
| RF-GER-06 | El sistema debe mostrar, por técnico, la cantidad y el porcentaje de traslados con desviación en el período. | Gerencia | Media | §4.1 |

---

## 6. Administración (`ADM`)

| ID | Requisito | Actor | Prioridad | Origen |
|---|---|---|---|---|
| RF-ADM-01 | El sistema debe permitir **crear, editar, listar y desactivar destinos** (sede y clínicas/hospitales) con nombre, dirección, ciudad y coordenadas. | Admin | Alta | §3.1, §3.2 |
| RF-ADM-02 | El sistema debe permitir **crear, editar, listar y desactivar vehículos** con patente, descripción, tipo (empresa/particular) y rendimiento (km/L). | Admin | Alta | §3.1, §3.2 |
| RF-ADM-03 | El sistema debe permitir **crear y listar tramos oficiales** (origen, destino, km oficiales y peaje). | Admin | Alta | §3.1, §3.2 |
| RF-ADM-04 | Al editar un tramo, el sistema debe **crear una nueva versión con fecha de vigencia**, conservando la anterior. | Admin | Alta | §2.4, §5.1 |
| RF-ADM-05 | El sistema debe permitir **registrar el precio del combustible por litro** con fecha de vigencia y consultar el historial de precios. | Admin | Alta | §2.4, §3.4 |
| RF-ADM-06 | El sistema debe permitir **crear, editar, listar y desactivar usuarios**, asignándoles un rol y, en el caso de los técnicos, el jefe a cargo. | Admin | Alta | §3.1 |
| RF-ADM-07 | El sistema debe permitir consultar el historial de versiones de un tramo. | Admin | Baja | §5.1 |
| RF-ADM-08 | El sistema debe incluir **datos iniciales precargados** (sede, clínicas, tramos, vehículos, tarifa y un usuario por rol) para poder usar la aplicación desde la primera ejecución. | — | Alta | [Diseño] |

---

## 7. Requisitos derivados del caso, postergados para la EFT

Requieren conexión con sistemas externos, que el docente excluyó de esta etapa.

| ID | Requisito | Origen |
|---|---|---|
| RF-EFT-01 | El sistema debe **sincronizar automáticamente** con un servidor central los traslados registrados sin conexión al recuperar señal. | §3.3 |
| RF-EFT-02 | El sistema debe obtener el **precio del combustible** desde una fuente pública (API de la CNE). | §3.4 |
| RF-EFT-03 | El sistema debe calcular las **distancias oficiales** de los tramos nuevos mediante un servicio de rutas (OpenRouteService). | [Diseño] |
| RF-EFT-04 | El cálculo oficial de costos debe realizarse en el servidor central. | §5.1 |
| RF-EFT-05 | El sistema debe permitir **exportar** el reporte de costos por cliente (CSV/PDF) para respaldar la facturación. | §3.4, §4.1 |

**Fuera de alcance:** integración con el ERP contable (§3.4: *"no existe hoy integración"*).

---

## 8. Reglas de negocio

| ID | Regla |
|---|---|
| RN-01 | **Costo de un traslado** = `(km oficiales ÷ rendimiento del vehículo [km/L]) × precio del litro vigente + peaje del tramo`. |
| RN-02 | **Desviación:** si `(km declarados − km oficiales) ÷ km oficiales > 15%`, el traslado se marca con **desviación**. El costo se calcula igual con los km oficiales. |
| RN-03 | **Traslado no catalogado:** si no existe un tramo oficial, el costo se calcula con los km declarados y el traslado se marca como **no catalogado**. |
| RN-04 | **Auditoría:** el costo usa el tramo y la tarifa **vigentes a la fecha del traslado**, y el traslado guarda una copia de los valores usados (km oficiales, peaje, precio del litro y rendimiento). Un cambio posterior de tarifas no altera los traslados ya registrados. |
| RN-05 | El km de llegada debe ser **mayor** que el km de salida del traslado. El km inicial de una jornada no puede ser **menor** que el km final de la última jornada del mismo vehículo. |
| RN-06 | Un técnico solo puede tener **una jornada abierta** a la vez, y un vehículo no puede estar en dos jornadas abiertas simultáneamente. |
| RN-07 | La fotografía del odómetro es **obligatoria** al iniciar y al cerrar la jornada, y opcional en las llegadas intermedias. |
| RN-08 | Si se niega el permiso de **ubicación**, el traslado se registra igual y queda marcado "sin ubicación". Si se niega el permiso de **cámara**, no se puede iniciar ni cerrar la jornada (RN-07). |
| RN-09 | Toda jornada comienza en la sede, y cerrarla implica registrar el traslado de regreso a la sede. |
| RN-10 | Una jornada **Cerrada** no puede ser modificada por el técnico. Una jornada **Validada** o **Rechazada** no puede ser modificada por nadie. |
| RN-11 | Para rechazar una jornada es obligatorio ingresar un comentario. |
| RN-12 | Los reportes de Gerencia consideran **solo las jornadas Validadas**. |
| RN-13 | Los destinos, vehículos y usuarios que tienen registros asociados **no se eliminan, se desactivan**. Los desactivados no aparecen en los formularios de registro. |
| RN-14 | Un tramo debe tener origen distinto del destino, km oficiales > 0 y peaje ≥ 0. El precio del combustible y el rendimiento de los vehículos deben ser > 0. |
| RN-15 | Un técnico solo ve sus propias jornadas; un jefe solo ve las de los técnicos a su cargo. |

---

## 9. Formularios y validaciones por campo

Lista de control para el IE 2.1.2: **cada campo** debe mostrar un ícono y un mensaje si no cumple su validación.

| Formulario | Campo | Validaciones |
|---|---|---|
| **Login** | Correo | Obligatorio, formato de correo válido |
| | Contraseña | Obligatoria; mensaje genérico si las credenciales no coinciden |
| **Iniciar jornada** | Vehículo | Obligatorio, activo, sin otra jornada abierta (RN-06) |
| | Km inicial | Obligatorio, numérico entero, ≥ km final anterior del vehículo (RN-05) |
| | Foto odómetro | Obligatoria (RN-07) |
| **Salida a destino** | Destino | Obligatorio, distinto del origen; aviso si no hay tramo (RF-TEC-05) |
| **Registrar llegada** | Km actual | Obligatorio, numérico entero, > km de salida (RN-05) |
| | Foto | Opcional |
| **Cerrar jornada** | Km final | Obligatorio, numérico entero, > km de salida del último traslado (RN-05) |
| | Foto odómetro | Obligatoria (RN-07) |
| **Rechazar jornada** | Comentario | Obligatorio, mínimo 10 caracteres (RN-11) |
| **Destino** | Nombre | Obligatorio, sin duplicados |
| | Dirección / Ciudad | Obligatorios |
| | Latitud / Longitud | Obligatorias, numéricas, en rango (−90..90 / −180..180) |
| | Tipo | Obligatorio (Sede / Clínica) |
| **Vehículo** | Patente | Obligatoria, formato chileno (`AB1234` o `ABCD12`), sin duplicados |
| | Descripción | Obligatoria |
| | Tipo | Obligatorio (Empresa / Particular) |
| | Rendimiento | Obligatorio, numérico > 0 (RN-14) |
| **Tramo** | Origen / Destino | Obligatorios y distintos entre sí (RN-14) |
| | Km oficiales | Obligatorio, numérico > 0 |
| | Peaje | Obligatorio, numérico ≥ 0 |
| | Vigente desde | Obligatoria, no anterior a la vigencia de la versión previa |
| **Tarifa combustible** | Precio por litro | Obligatorio, numérico > 0 |
| | Vigente desde | Obligatoria, no anterior a la vigencia de la tarifa previa |
| **Usuario** | Nombre | Obligatorio |
| | Correo | Obligatorio, formato válido, sin duplicados |
| | Contraseña | Obligatoria al crear, mínimo 6 caracteres |
| | Rol | Obligatorio |
| | Jefe a cargo | Obligatorio si el rol es Técnico |

---

## 10. Resumen

| Módulo | Alta | Media | Baja | Total |
|---|---|---|---|---|
| AUT | 4 | 1 | 0 | 5 |
| TEC | 12 | 3 | 0 | 15 |
| JEF | 5 | 0 | 1 | 6 |
| GER | 4 | 2 | 0 | 6 |
| ADM | 7 | 0 | 1 | 8 |
| **EP2** | **32** | **6** | **2** | **40** |
| EFT (postergados) | | | | 5 |
