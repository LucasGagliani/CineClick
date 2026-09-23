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

La primera vez que arranca, se cargan datos de prueba: un admin, 2 películas, un cine con 2 salas, 3 funciones y la promo `ESTRENO10`. Las funciones quedan para el día siguiente, así que si pasan unos días conviene borrar la carpeta `data/` y volver a arrancar.

**Usuario admin:** `admin@cineclick.com` / `admin123`

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
| Películas | `GET`, `POST /api/peliculas` · `GET`, `PUT`, `DELETE /api/peliculas/{id}` |
| Cines | `GET`, `POST /api/cines` · `GET`, `PUT`, `DELETE /api/cines/{id}` · `GET`, `POST /api/cines/{id}/salas` |
| Funciones | `GET`, `POST /api/funciones` · `GET`, `PUT`, `DELETE /api/funciones/{id}` · `GET /api/funciones/{id}/butacas-disponibles` |
| Clientes | `GET`, `POST /api/clientes` · `GET`, `PUT`, `DELETE /api/clientes/{id}` · `GET /api/clientes/{id}/compras` |
| Compras | `POST /api/compras` · `GET /api/compras/{id}` · `PATCH /api/compras/{id}/cancelar` |
| Pagos | `POST /api/compras/{id}/pagos` · `GET /api/pagos/{id}` |
| Promociones | `GET`, `POST /api/promociones` |

`GET /api/funciones` acepta los filtros `peliculaId`, `cineId`, `ciudad`, `fecha` (`2026-09-25`) y `formato` (`IMAX`).

Los `DELETE` son bajas lógicas: el registro queda marcado como inactivo en vez de borrarse, porque hay compras que lo referencian.

En `requests/cineclick-demo.http` están todos los requests con sus bodies listos para probar desde IntelliJ o VS Code (con la extensión REST Client). Incluyen casos que salen bien y casos con error. Para simular un pago rechazado se manda `"datosPagoToken": "rechazar"`.

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
