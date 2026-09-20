# CineClick Backend

Backend Java/Spring Boot para el TPO de Aplicaciones Interactivas. El proyecto implementa una aplicacion web transaccional de cine tipo Hoyts/Cinemark, enfocada en la etapa 1: back-end, persistencia SQL, API HTTP, CRUD, validaciones, manejo de errores y arquitectura por capas.

## Alcance de Etapa 1

Esta entrega incluye solamente el back-end pedido por el TP:

- Servidor Java/Spring ejecutable en `localhost`.
- API HTTP con intercambio de datos en JSON.
- Persistencia en base de datos SQL.
- Entidades JPA y relaciones ORM.
- Operaciones CRUD sobre entidades del dominio.
- Logica de negocio en services.
- Validacion de datos de entrada.
- Manejo de errores con codigos HTTP.
- Documentacion basica de endpoints y requests de prueba.

Queda fuera de esta etapa:

- Front-end HTML/CSS/JavaScript.
- Integracion visual con una interfaz web.
- Autenticacion avanzada con JWT o Spring Security.
- Pasarela de pago real.
- Despliegue en un servidor externo.

## Tecnologias

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 Database en modo archivo
- Maven

## Arquitectura

El proyecto esta separado por responsabilidades:

- `controller`: recibe solicitudes HTTP y devuelve JSON.
- `service`: contiene reglas de negocio.
- `repository`: acceso a datos mediante Spring Data JPA. Funciona como DAO.
- `model`: entidades JPA persistidas en SQL.
- `dto`: objetos de entrada y salida de la API.
- `exception`: manejo centralizado de errores HTTP.
- `config`: datos iniciales para probar la aplicacion.

Flujo general:

```text
Cliente HTTP -> Controller -> Service -> Repository/DAO -> Base de datos SQL
```

## Como ejecutar

Desde la carpeta `cineclick-backend`:

```bash
mvn spring-boot:run
```

Si no se tiene Maven instalado, tambien se puede ejecutar el JAR generado:

```bash
java -jar cineclick-backend-0.0.1-SNAPSHOT.jar
```

La API queda disponible en:

```text
http://localhost:8080
```

Base de datos local H2:

```text
JDBC URL: jdbc:h2:file:./data/cineclick
User: sa
Password:
```

## Usuarios demo

El sistema crea automaticamente un usuario administrador:

```text
Email: admin@cineclick.com
Password: admin123
Rol: ADMIN
```

Los clientes se crean con:

```http
POST /api/clientes
```

El login demo se realiza con:

```http
POST /api/auth/login
```

> Nota: para esta etapa se implementa un login simple para demostrar usuarios tipo `ADMIN` y `CLIENTE`. No se agrego JWT ni Spring Security para mantener el foco del TPO en arquitectura, CRUD, persistencia, HTTP y reglas de negocio.

## Entidades principales

- `Usuario`
- `Cliente`
- `Pelicula`
- `Cine`
- `Sala`
- `Butaca`
- `Funcion`
- `Compra`
- `Entrada`
- `Pago`
- `Promocion`

## Documentacion complementaria

La carpeta `documentacion` incluye los diagramas UML y la guia de explicacion del proyecto:

- `uml-capas.puml`: arquitectura por capas.
- `uml-codigo-completo.puml`: clases, atributos, metodos y relaciones del codigo actual.
- `uml-flujo-compra.puml`: secuencia del caso de uso de compra de entradas.
- `guia-uml-profesor.md`: resumen para estudiar y defender el backend.

## Endpoints principales

### Auth

```http
POST /api/auth/login
```

Body:

```json
{
  "email": "admin@cineclick.com",
  "password": "admin123"
}
```

### Peliculas

```http
GET /api/peliculas
GET /api/peliculas/{id}
POST /api/peliculas
PUT /api/peliculas/{id}
DELETE /api/peliculas/{id}
```

Crear pelicula:

```json
{
  "titulo": "Matrix",
  "sinopsis": "Un programador descubre que la realidad es una simulacion.",
  "duracionMinutos": 136,
  "clasificacion": "+13",
  "genero": "CIENCIA_FICCION",
  "idioma": "Subtitulada",
  "formatosDisponibles": ["DOS_D", "IMAX"],
  "urlPoster": "https://example.com/matrix.jpg",
  "fechaEstreno": "1999-03-31"
}
```

### Cines y salas

```http
GET /api/cines
GET /api/cines/{id}
POST /api/cines
PUT /api/cines/{id}
DELETE /api/cines/{id}
GET /api/cines/{id}/salas
POST /api/cines/{id}/salas
```

Crear sala con butacas:

```json
{
  "nombre": "Sala Premium",
  "numero": 3,
  "tipo": "PREMIUM",
  "cantidadFilas": 5,
  "butacasPorFila": 8
}
```

### Funciones

```http
GET /api/funciones
GET /api/funciones/{id}
POST /api/funciones
PUT /api/funciones/{id}
DELETE /api/funciones/{id}
GET /api/funciones/{id}/butacas-disponibles
```

Filtros disponibles:

```http
GET /api/funciones?peliculaId=1&cineId=1&fecha=2026-09-20&formato=IMAX
```

Crear funcion:

```json
{
  "peliculaId": 1,
  "salaId": 1,
  "fechaHoraInicio": "2026-09-25T20:30:00",
  "precioBase": 6500,
  "formato": "IMAX",
  "idioma": "Subtitulada"
}
```

### Clientes

```http
GET /api/clientes
POST /api/clientes
GET /api/clientes/{id}
PUT /api/clientes/{id}
DELETE /api/clientes/{id}
GET /api/clientes/{id}/compras
```

Crear cliente:

```json
{
  "nombre": "Lucas",
  "apellido": "Perez",
  "email": "lucas@example.com",
  "password": "1234",
  "telefono": "1122334455"
}
```

### Compras

```http
POST /api/compras
GET /api/compras/{id}
PATCH /api/compras/{id}/cancelar
```

Crear compra:

```json
{
  "clienteId": 1,
  "funcionId": 1,
  "butacasIds": [1, 2],
  "codigoPromocion": "ESTRENO10"
}
```

### Pagos

```http
POST /api/compras/{compraId}/pagos
GET /api/pagos/{id}
```

Pago aprobado:

```json
{
  "metodoPago": "MERCADO_PAGO",
  "datosPagoToken": "token-demo"
}
```

Pago rechazado para probar error funcional:

```json
{
  "metodoPago": "MERCADO_PAGO",
  "datosPagoToken": "rechazar"
}
```

### Promociones

```http
GET /api/promociones
POST /api/promociones
```

## Pruebas solicitadas por la rubrica actualizada

El enunciado actualizado pide poder demostrar el funcionamiento del back-end mediante una herramienta capaz de realizar solicitudes HTTP, incluyendo:

- Consultas mediante `GET`.
- Creacion de recursos mediante `POST`.
- Modificacion mediante `PUT` o `PATCH`.
- Eliminacion mediante `DELETE`.
- Casos exitosos.
- Casos con datos invalidos.
- Casos con recursos inexistentes.

Para eso se agrego el archivo:

```text
requests/cineclick-demo.http
```

Ese archivo puede abrirse con IntelliJ IDEA, WebStorm, VS Code con la extension REST Client, o usarse como guia para Postman/Insomnia.

Secuencia sugerida para la defensa:

1. `GET /api/peliculas` para demostrar que el servidor responde y mostrar una consulta exitosa.
2. `POST /api/clientes` para crear un cliente.
3. `POST /api/clientes` con datos invalidos para mostrar validaciones y error `400`.
4. `GET /api/peliculas/999999` para mostrar recurso inexistente y error `404`.
5. `POST /api/peliculas` para crear una pelicula.
6. `PUT /api/peliculas/{id}` para modificarla.
7. `DELETE /api/peliculas/{id}` para eliminarla mediante baja logica.
8. `GET /api/funciones/{id}/butacas-disponibles` para mostrar logica de negocio.
9. `POST /api/compras` para mostrar compra de entradas y validacion de butacas.

Tambien se agregaron pruebas automaticas de integracion en:

```text
src/test/java/com/cineclick/ApiIntegrationTests.java
```

Estas pruebas levantan el contexto Spring, usan una base H2 en memoria separada y verifican:

- `GET` exitoso.
- `POST` exitoso.
- `PUT` exitoso.
- `DELETE` exitoso.
- Error por datos invalidos.
- Error por recurso inexistente.

Para ejecutarlas:

```bash
mvn test
```

## Reglas de negocio implementadas

- Una pelicula debe tener duracion positiva y al menos un formato disponible.
- Un cliente no puede registrarse con un email repetido.
- Una funcion debe programarse a futuro.
- Una funcion no puede solaparse con otra funcion de la misma sala.
- Una funcion solo puede usar formatos disponibles para su pelicula.
- Una compra debe tener al menos una butaca.
- Las butacas seleccionadas deben pertenecer a la sala de la funcion.
- Una butaca ocupada no puede volver a comprarse para la misma funcion.
- Las promociones deben estar vigentes.
- El pago aprobado confirma la compra.
- La compra cancelada cancela sus entradas.

## Como se evita vender dos veces la misma butaca

El sistema aplica dos barreras:

1. En `CompraService`, antes de crear la compra, se valida que la butaca no tenga una entrada activa para esa funcion.
2. En la base de datos, la tabla `entradas` tiene una restriccion unica:

```text
funcion_id + butaca_id
```

Esto permite defender la concurrencia:

```text
Si dos clientes intentan comprar la misma butaca al mismo tiempo, ambas solicitudes llegan al back-end.
La primera transaccion que inserta la entrada gana.
La segunda falla por restriccion unica en la base de datos y el sistema responde 409 Conflict.
```

## Manejo de errores

`GlobalExceptionHandler` centraliza los errores y devuelve respuestas JSON con:

- `timestamp`
- `status`
- `error`
- `mensaje`
- `path`
- `detalles`

Ejemplo:

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

## Que explicar en la defensa

- `Controller` no contiene reglas de negocio; solo recibe HTTP.
- `Service` decide reglas como disponibilidad, solapamiento de funciones y promociones.
- `Repository` abstrae el acceso a datos y cumple el rol de DAO.
- `Entity` representa tablas SQL mediante ORM/JPA.
- `DTO` evita exponer directamente las entidades en la API.
- `GlobalExceptionHandler` transforma excepciones en respuestas HTTP claras.
- La arquitectura permite que el front-end de etapa 2 consuma la API por HTTP.

## Relacion con la rubrica actualizada

| Criterio | Como lo cubre el proyecto |
| --- | --- |
| Arquitectura y organizacion | Paquetes separados en `controller`, `service`, `repository`, `model`, `dto`, `exception` y `config`. |
| Persistencia y SQL | Base H2 SQL, entidades JPA, claves primarias, claves foraneas y restricciones unicas. |
| Modelado del dominio y ORM | Entidades `Pelicula`, `Cine`, `Sala`, `Butaca`, `Funcion`, `Cliente`, `Compra`, `Entrada`, `Pago` y `Promocion`. |
| Logica de negocio | Services con validacion de horarios, butacas disponibles, promociones vigentes, pagos y cancelaciones. |
| API y HTTP | Controllers REST con `GET`, `POST`, `PUT`, `PATCH`, `DELETE` y codigos HTTP apropiados. |
| Validaciones y errores | Bean Validation y `GlobalExceptionHandler` con respuestas JSON comprensibles. |
| Funcionamiento | Servidor ejecutable en `localhost:8080` y archivo `.http` para pruebas. |
| Documentacion | Este README documenta arquitectura, endpoints, ejecucion, pruebas y decisiones. |
| Comprension individual | La seccion de defensa resume responsabilidades y justificaciones tecnicas. |
