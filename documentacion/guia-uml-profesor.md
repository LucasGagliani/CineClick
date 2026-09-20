# CineClick Backend - Guia UML y resumen del proyecto

Esta guia resume el backend real del proyecto `cineclick-backend`. Sirve para explicar el UML, defender decisiones de diseno y mostrar que la Etapa 1 cumple con backend, persistencia, API HTTP, CRUD, validaciones y manejo de errores.

## Archivos UML incluidos

- `cineclick-uml-codigo-completo.puml`: diagrama de clases completo del codigo actual.
- `cineclick-uml-capas.puml`: diagrama corto de arquitectura por capas.
- `cineclick-uml-flujo-compra.puml`: diagrama de secuencia del caso de uso principal, compra de entradas.

Convencion usada: el UML muestra atributos, relaciones y metodos importantes del sistema. Se omiten getters, setters y constructores triviales para que el diagrama sea legible, pero las clases, atributos, DTOs, services, repositories y controllers corresponden al codigo actual.

## Idea del sistema

CineClick es una aplicacion backend para administrar una pagina de cine similar a Hoyts. Permite gestionar peliculas, cines, salas, funciones, clientes, compras, entradas, pagos y promociones. En esta primera etapa no hay frontend: todo se expone mediante endpoints HTTP que reciben y devuelven JSON.

## Arquitectura

El proyecto usa una arquitectura por capas:

| Capa | Paquete | Responsabilidad |
| --- | --- | --- |
| Controller | `com.cineclick.controller` | Expone endpoints REST y recibe requests JSON. |
| DTO | `com.cineclick.dto` | Define objetos de entrada y salida para no exponer entidades directamente. |
| Service | `com.cineclick.service` | Contiene reglas de negocio y casos de uso. |
| Repository | `com.cineclick.repository` | Acceso a datos mediante Spring Data JPA. |
| Model | `com.cineclick.model` | Entidades JPA, atributos, relaciones y comportamiento de dominio. |
| Exception | `com.cineclick.exception` | Manejo centralizado de errores HTTP. |
| Config | `com.cineclick.config` | Carga datos iniciales de demostracion. |

Flujo general:

1. El cliente HTTP llama a un endpoint del controller.
2. El controller valida el DTO y delega al service.
3. El service aplica reglas de negocio.
4. El repository consulta o persiste entidades JPA.
5. El mapper transforma entidades a DTOs de respuesta.
6. Si hay error, `GlobalExceptionHandler` devuelve un JSON de error uniforme.

## Entidades principales

### Usuario y Cliente

`Usuario` representa los datos de acceso y rol. Puede ser `ADMIN` o `CLIENTE`.

`Cliente` representa al usuario comprador. Tiene relacion uno a uno con `Usuario`, fecha de registro y telefono. La baja del cliente es logica: no se elimina fisicamente, se desactiva su usuario.

Relacion:

- `Cliente 1 *-- 1 Usuario`

### Pelicula

`Pelicula` contiene titulo, sinopsis, duracion, clasificacion, genero, idioma, formatos disponibles, poster, fecha de estreno y estado activo.

Reglas:

- La duracion debe ser mayor a cero.
- Debe tener al menos un formato disponible.
- Se usa baja logica con `activa = false`.

### Cine, Sala y Butaca

`Cine` representa una sede fisica.

`Sala` pertenece a un cine y tiene un numero unico dentro de ese cine.

`Butaca` pertenece a una sala y tiene fila, numero, tipo y estado activo.

Relaciones:

- `Cine 1 *-- 0..* Sala`
- `Sala 1 *-- 0..* Butaca`

Restricciones relevantes:

- Una sala no puede repetirse por numero dentro del mismo cine: `cine_id + numero`.
- Una butaca no puede repetirse dentro de la misma sala: `sala_id + fila + numero`.

### Funcion

`Funcion` une una pelicula con una sala en un horario determinado. Tiene precio base, formato, idioma y estado.

Reglas:

- La funcion debe programarse a futuro.
- El formato de la funcion debe estar permitido por la pelicula.
- No puede solaparse con otra funcion activa en la misma sala.
- La hora de fin se calcula automaticamente con duracion de pelicula + 20 minutos.

Relaciones:

- `Funcion 0..* --> 1 Pelicula`
- `Funcion 0..* --> 1 Sala`

### Compra, Entrada, Pago y Promocion

`Compra` representa la compra de una o varias entradas por un cliente para una funcion.

`Entrada` representa cada ticket emitido. Tiene codigo QR, precio unitario, estado, funcion y butaca.

`Pago` representa el pago asociado a una compra.

`Promocion` aplica un porcentaje de descuento si esta vigente.

Relaciones:

- `Compra 0..* --> 1 Cliente`
- `Compra 0..* --> 1 Funcion`
- `Compra 1 *-- 1..* Entrada`
- `Compra 0..* --> 0..1 Promocion`
- `Compra 1 -- 0..1 Pago`
- `Entrada 0..* --> 1 Butaca`
- `Entrada 0..* --> 1 Funcion`
- `Pago 1 --> 1 Compra`

Estados:

- Compra: `PENDIENTE_PAGO`, `CONFIRMADA`, `CANCELADA`.
- Entrada: `EMITIDA`, `USADA`, `CANCELADA`.
- Pago: `PENDIENTE`, `APROBADO`, `RECHAZADO`.

## Regla importante: dos clientes comprando la misma butaca

El sistema lo resuelve en dos niveles:

1. Nivel de servicio: `CompraService.validarButacasDisponibles` consulta si ya existe una entrada para esa funcion y butaca que no este cancelada.
2. Nivel de base de datos: `Entrada` tiene una restriccion unica `uk_entrada_funcion_butaca` sobre `funcion_id + butaca_id`.

Defensa oral:

> Si dos clientes intentan comprar la misma butaca al mismo tiempo, puede pasar que ambos la vean libre antes de guardar. Por eso no confiamos solo en la validacion del service. La base de datos tiene una restriccion unica para que solo se pueda insertar una entrada por funcion y butaca. El primer request que guarda gana; el segundo dispara una violacion de integridad, el service la captura y devuelve un error `409 Conflict` indicando que la butaca ya no esta disponible.

## Servicios y reglas de negocio

| Servicio | Responsabilidad |
| --- | --- |
| `AuthService` | Login simple por email y password. |
| `ClienteService` | Registro, actualizacion, listado y baja logica de clientes. Valida email unico. |
| `PeliculaService` | CRUD de peliculas y validaciones de datos. |
| `CineService` | CRUD de cines, alta de salas y generacion automatica de butacas. |
| `FuncionService` | CRUD de funciones, filtros, validacion de horarios y butacas disponibles. |
| `CompraService` | Compra de entradas, validacion de butacas, precios, promociones y cancelacion. |
| `PagoService` | Registro de pago, confirmacion o rechazo y confirmacion de compra. |
| `PromocionService` | Alta, listado y validacion de promociones vigentes. |

## Repositories

Todos los repositories extienden `JpaRepository`, por eso heredan CRUD completo: `findAll`, `findById`, `save`, `delete`, etc.

Ademas tienen consultas derivadas especificas:

- `UsuarioRepository`: buscar y validar email.
- `PeliculaRepository`: listar activas y filtrar por genero.
- `CineRepository`: listar activos y filtrar por ciudad.
- `SalaRepository`: salas activas por cine.
- `ButacaRepository`: butacas activas por sala.
- `FuncionRepository`: funciones por pelicula, sala o rango horario.
- `EntradaRepository`: entradas por funcion/compra y validacion de butaca ocupada.
- `CompraRepository`: compras por cliente.
- `PagoRepository`: pago por compra.
- `PromocionRepository`: promocion por codigo y promociones activas.

## Controllers y endpoints

| Controller | Base URL | Operaciones |
| --- | --- | --- |
| `AuthController` | `/api/auth` | `POST /login` |
| `PeliculaController` | `/api/peliculas` | GET, GET por id, POST, PUT, DELETE |
| `CineController` | `/api/cines` | GET, GET por id, POST, PUT, DELETE, salas de cine |
| `FuncionController` | `/api/funciones` | GET con filtros, GET por id, POST, PUT, DELETE, butacas disponibles |
| `ClienteController` | `/api/clientes` | GET, GET por id, POST, PUT, DELETE, compras por cliente |
| `CompraController` | `/api/compras` | POST compra, GET compra, PATCH cancelar |
| `PagoController` | `/api` | POST pago de compra, GET pago |
| `PromocionController` | `/api/promociones` | GET activas, POST nueva promocion |

## CRUD pedido por el TP

El proyecto implementa CRUD sobre varias entidades:

- Peliculas: crear, listar, obtener, actualizar y baja logica.
- Cines: crear, listar, obtener, actualizar y baja logica.
- Clientes: registrar, listar, obtener, actualizar y baja logica.
- Funciones: crear, listar, obtener, actualizar y cancelar.
- Promociones: crear y listar activas.
- Compras: crear, consultar y cancelar.
- Pagos: registrar y consultar.

## Validaciones y errores

Validaciones de DTO:

- `@NotBlank`, `@NotNull`, `@Email`, `@Size`, `@Min`, `@Future`, `@DecimalMin`, `@DecimalMax`, `@NotEmpty`.

Errores del sistema:

- `RecursoNoEncontradoException` -> 404 Not Found.
- `ReglaNegocioException` -> 400 Bad Request.
- `ButacaNoDisponibleException` -> 409 Conflict.
- `MethodArgumentNotValidException` -> 400 con detalles por campo.
- `DataIntegrityViolationException` -> 409 por restriccion de base de datos.
- `Exception` generica -> 500 Internal Server Error.

## Persistencia

El backend usa JPA/Hibernate y base SQL. En desarrollo se usa H2 en modo archivo para que el proyecto corra facil en cualquier maquina durante la Etapa 1.

Tablas principales:

- `usuarios`
- `clientes`
- `peliculas`
- `pelicula_formatos`
- `cines`
- `salas`
- `butacas`
- `funciones`
- `compras`
- `entradas`
- `pagos`
- `promociones`

Restricciones importantes:

- Email de usuario unico.
- Codigo de promocion unico.
- Codigo QR de entrada unico.
- Una sala no se repite dentro del mismo cine.
- Una butaca no se repite dentro de la misma sala.
- Una butaca no puede venderse dos veces para la misma funcion.

## Como explicar el UML en la presentacion

Orden recomendado:

1. Mostrar `cineclick-uml-capas.puml`: explicar arquitectura por capas.
2. Mostrar el paquete `model` del diagrama completo: explicar entidades y relaciones.
3. Mostrar `service`: explicar donde viven las reglas de negocio.
4. Mostrar `controller` y DTOs: explicar API JSON y separacion entre entrada/salida y entidades.
5. Mostrar `cineclick-uml-flujo-compra.puml`: defender el caso mas importante del sistema.
6. Cerrar con validaciones, errores y pruebas HTTP.

Frase corta para defender arquitectura:

> Organizamos el backend por capas. Los controllers solo exponen endpoints y reciben DTOs; los services concentran reglas de negocio; los repositories encapsulan persistencia; y las entities modelan el dominio con relaciones JPA. Asi evitamos mezclar HTTP, logica de negocio y base de datos en una sola clase.

Frase corta para defender CRUD:

> La aplicacion implementa CRUD sobre recursos principales como peliculas, cines, clientes y funciones. Tambien agrega operaciones transaccionales propias del dominio, como compra de entradas, cancelacion de compra, pago y consulta de butacas disponibles.

Frase corta para defender validaciones:

> Validamos en dos lugares: en DTOs con Bean Validation para datos de entrada, y en services para reglas de negocio que dependen del estado del sistema, como horario de funciones, promocion vigente o butaca disponible.

## Pruebas para demostrar Etapa 1

El archivo `requests/cineclick-demo.http` contiene requests listos para probar:

- GET exitoso.
- POST exitoso.
- PUT exitoso.
- DELETE exitoso.
- Datos invalidos.
- Recurso inexistente.
- Compra de entradas.
- Cancelacion de compra.
- Pago de compra.

Tambien hay tests automatizados en `ApiIntegrationTests` que cubren los casos minimos pedidos por la rubrica actualizada.
