# CineClick – Backend

Backend de CineClick, una app de venta de entradas de cine (estilo Hoyts o Cinemark), para el TPO de Aplicaciones Interactivas. Esta es la etapa 1: API REST con Spring Boot y base de datos SQL. El front llega en la etapa 2.

**Stack:** Java 21, Spring Boot 3 (Web, Data JPA, Validation), H2, Lombok y Maven.

## Cómo correrlo

No hace falta instalar Maven, el wrapper lo baja solo:

```bash
./mvnw spring-boot:run
```

En Windows con CMD es `mvnw.cmd spring-boot:run`. También se puede correr `CineClickApplication` desde IntelliJ.

La API queda en `http://localhost:8080`.

La primera vez que arranca con la base vacía se carga el catálogo de prueba (`config/CatalogoDemo`): 3 cines con 6 salas de distinto plano (clásica, estadio, IMAX, premium y 4D), películas reales en cartelera, próximos estrenos (algunos en preventa), la promo `ESTRENO10` y la programación de los próximos 7 días a partir de la fecha del día.

Si en la base no queda ninguna función a futuro (por ejemplo, porque pasaron varios días), al arrancar se vuelve a programar una semana con las películas y salas que haya. Para empezar de cero con el catálogo completo, borrá la carpeta `data/` y volvé a arrancar.

| Usuario | Email | Contraseña |
| --- | --- | --- |
| Admin | `admin@cineclick.com` | `admin123` |
| Cliente | `cliente@cineclick.com` | `cliente123` |

### Base de datos

Usamos H2 guardada en archivo (`data/cineclick.mv.db`), así que los datos no se pierden al reiniciar. Para ver las tablas desde el navegador:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/cineclick`
- Usuario `sa`, sin contraseña

## Estructura

```text
controller  → recibe los requests HTTP y devuelve JSON
service     → reglas de negocio
repository  → acceso a datos (Spring Data JPA, cumple el rol de DAO)
model       → entidades JPA
dto         → lo que entra y sale de la API
exception   → manejo de errores centralizado
config      → datos de prueba
```

El flujo es `Cliente → Controller → Service → Repository → Base de datos`.

En `documentacion/` están los diagramas UML y `explicacion-del-proyecto.md`, donde contamos cómo está armado todo y por qué.

## Endpoints

| Recurso | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/login` |
| Películas | `GET`, `POST /api/peliculas` · `GET`, `PUT`, `DELETE /api/peliculas/{id}` · `PATCH /api/peliculas/{id}/estado` · `GET /api/peliculas/destacadas` · `GET /api/peliculas/proximos-estrenos` |
| Lista de espera | `POST /api/peliculas/{id}/lista-espera` · `DELETE /api/peliculas/{id}/lista-espera/{clienteId}` · `GET /api/clientes/{id}/lista-espera` |
| Cines | `GET`, `POST /api/cines` · `GET`, `PUT`, `DELETE /api/cines/{id}` · `PATCH /api/cines/{id}/estado` · `GET`, `POST /api/cines/{id}/salas` |
| Salas | `GET /api/salas` · `PUT`, `DELETE /api/salas/{id}` · `PATCH /api/salas/{id}/estado` |
| Funciones | `GET`, `POST /api/funciones` · `GET`, `PUT`, `DELETE /api/funciones/{id}` · `PATCH /api/funciones/{id}/cancelar` · `GET /api/funciones/{id}/butacas-disponibles` · `GET /api/funciones/{id}/precios` |
| Clientes | `GET`, `POST /api/clientes` · `GET`, `PUT`, `DELETE /api/clientes/{id}` · `GET /api/clientes/{id}/compras` |
| Compras | `POST /api/compras` · `GET /api/compras/{id}` · `PATCH /api/compras/{id}/cancelar` · `POST /api/compras/{id}/promocion` |
| Entradas | `POST /api/entradas/{codigo}/validar` |
| Métricas | `GET /api/admin/metricas?desde=&hasta=&cineId=` (solo administradores: encabezado `X-Usuario-Id`) |
| Pagos | `POST /api/compras/{id}/pagos` · `GET /api/pagos/{id}` |
| Promociones | `GET`, `POST /api/promociones` · `GET /api/promociones/vigentes` · `PUT`, `DELETE /api/promociones/{id}` · `PATCH /api/promociones/{id}/estado` |

`GET /api/funciones` acepta los filtros `peliculaId`, `cineId`, `fecha` (`2026-09-25`) y `formato` (`IMAX`). Por defecto no devuelve funciones de cines inactivos; el panel pide todo con `incluirInactivos=true` (lo mismo `GET /api/cines?incluirInactivos=true` y `GET /api/peliculas?incluirInactivas=true`).

Los `DELETE` de cines, salas, películas, funciones y promociones borran el registro solo si no tiene datos asociados. Si los tiene, responden `409` con lo que tiene asociado en `detalles` (por ejemplo `["salas", "funciones", "compras"]`) y lo que corresponde es desactivarlo con `PATCH .../estado` (`{"activo": false}`) o, en una función, cancelarla. Un cine inactivo no aparece en el sitio, pero sus compras siguen visibles.

Los conjuntos cerrados son enums: provincia (las 24 jurisdicciones, con `CABA` aparte de `BUENOS_AIRES`), calificación (`ATP`, `MAS_13`, `MAS_16`, `MAS_18`), idioma (`CASTELLANO`, `SUBTITULADA`), género, formato y tipo de sala. Los textos se guardan sin espacios de más; los nombres de cine y las localidades en formato título; el teléfono con sus 10 dígitos. En CABA la localidad tiene que ser uno de los 48 barrios. El margen de limpieza entre funciones de una sala se configura con `cineclick.funciones.minutos-limpieza` (20 por defecto).

`GET /api/admin/metricas` junta en un solo JSON todos los bloques del panel (resumen contra el período anterior, próximas funciones, ranking, conversión, por cine, franjas, promociones, tipos de entrada y asistencia). Sale de consultas de agregación, sin recorrer compras una por una. Las definiciones de cada métrica están comentadas en `MetricasService`: una venta es una compra confirmada; ingresos y entradas van por fecha de compra; la ocupación, por fecha de función y ponderada por capacidad. Sin `X-Usuario-Id` de un administrador responde 403. La aplicación arranca con la zona horaria `America/Argentina/Buenos_Aires`.

Al arrancar, `MigracionDatos` pasa los datos viejos al formato nuevo (dirección y ciudad del cine a provincia, localidad, calle y altura; calificación e idioma de texto libre a los enums) y deja en el log cómo quedó cada registro.

En `requests/cineclick-demo.http` están todos los requests con sus bodies listos para probar desde IntelliJ o VS Code (con la extensión REST Client). Incluyen casos que salen bien y casos con error. El pago es simulado: se mandan solo la marca y los últimos 4 dígitos de la tarjeta (`"ultimos4": "0002"` simula un rechazo) y `"aceptaTerminos": true`. Una compra se reserva por 8 minutos; si no se paga en ese tiempo se cancela sola.

## Errores

Todos los errores devuelven el mismo formato JSON:

```json
{
  "timestamp": "2026-09-19T10:15:00",
  "status": 409,
  "error": "Conflict",
  "mensaje": "La butaca A1 ya no esta disponible",
  "path": "/api/compras",
  "detalles": []
}
```

| Código | Cuándo |
| --- | --- |
| 400 | Datos inválidos, JSON mal formado o una regla de negocio que no se cumple |
| 401 | Login incorrecto |
| 404 | No existe el recurso o el endpoint |
| 405 | Método HTTP no soportado |
| 409 | Butaca ya vendida o email ya registrado |
| 500 | Error inesperado del servidor |

## Reglas de negocio principales

- Una función no puede superponerse con otra en la misma sala, tiene que ser a futuro y usar un formato que la película tenga.
- Las butacas de una compra tienen que ser de la sala de la función y estar libres.
- La promo tiene que estar vigente.
- Si el pago se aprueba, la compra queda confirmada. Si se cancela la compra, se liberan las butacas.

**Venta doble de una butaca:** además del chequeo en `CompraService`, la tabla `entradas` tiene una restricción única sobre `funcion_id + butaca_id + vigente`. Si dos personas compran la misma butaca al mismo tiempo, la base solo acepta la primera y la segunda recibe un 409. Cuando se cancela una entrada, `vigente` pasa a `NULL`. Como la restricción única no compara valores `NULL`, la butaca se puede volver a vender.

## Tests

```bash
./mvnw test
```

Son tests de integración (`ApiIntegrationTests`) que levantan la app con una base en memoria y prueban el CRUD, las validaciones, los códigos de error y el flujo de compra, cancelación y recompra.
