# Cómo está armado CineClick

Acá explicamos cómo organizamos el backend, qué hace cada parte y por qué tomamos algunas decisiones. Los diagramas están en esta misma carpeta:

- `uml-capas.puml`: las capas y cómo se comunican.
- `uml-codigo-completo.puml`: todas las clases con sus atributos, métodos y relaciones. No incluye getters, setters ni constructores para que se pueda leer.
- `uml-flujo-compra.puml`: el paso a paso de una compra de entradas, que es el caso más complejo.

## La idea

CineClick es una app para comprar entradas de cine. En esta etapa solo existe el backend: todo se maneja por HTTP y JSON, y el front de la etapa 2 va a consumir esta misma API. Se pueden administrar películas, cines, salas y funciones. Los clientes se registran, eligen butacas, compran, pagan y pueden cancelar.

## Capas

Separamos el código por responsabilidad, en un paquete por capa:

| Paquete | Qué hace |
| --- | --- |
| `controller` | Define los endpoints. Recibe el request, valida el body con `@Valid` y le pasa el trabajo al service. No tiene lógica de negocio. |
| `service` | Las reglas del negocio: si una función se superpone con otra, si la butaca está libre, si la promo está vigente, etc. También maneja las transacciones. |
| `repository` | Acceso a la base. Son interfaces que extienden `JpaRepository`; Spring genera la implementación. Cumplen el rol de DAO. |
| `model` | Las entidades JPA, que se mapean a tablas. Algunas tienen métodos propios, como `Compra.calcularTotal()` o `Funcion.estaDisponibleParaVenta()`. |
| `dto` | Lo que entra y sale de la API. `DtoMapper` convierte las entidades en DTOs de respuesta. |
| `exception` | Excepciones propias y `GlobalExceptionHandler`, que las convierte en respuestas HTTP. |
| `config` | `DataSeeder`, que carga datos de prueba al arrancar. |

Un request pasa siempre por el mismo camino: `Controller → Service → Repository → Base de datos`. La respuesta vuelve convertida en DTO. Si algo falla en cualquier punto, se lanza una excepción y el `GlobalExceptionHandler` arma la respuesta de error.

Separarlo así nos permite cambiar una parte sin romper las otras. Si mañana pasamos de H2 a MySQL, solo cambian el driver en el `pom.xml` y la conexión en `application.properties`; los services y los controllers quedan iguales.

## Entidades y relaciones

- **Usuario / Cliente:** `Usuario` tiene los datos de acceso y el rol (`ADMIN` o `CLIENTE`). `Cliente` se relaciona uno a uno con su `Usuario` y agrega teléfono y fecha de registro.
- **Película:** título, sinopsis, duración, género, formatos en los que se puede ver (2D, 3D, IMAX), etc. Los formatos se guardan en una tabla aparte, `pelicula_formatos`, con `@ElementCollection`.
- **Cine → Sala → Butaca:** un cine tiene varias salas y cada sala tiene sus butacas (`@OneToMany` con cascade). Cuando se crea una sala se generan las butacas automáticamente según filas y butacas por fila. No se pueden repetir dos salas con el mismo número en un cine, ni dos butacas iguales en una sala.
- **Función:** une una película con una sala en un horario, con precio, formato e idioma. La hora de fin se calcula sola: duración de la película más 20 minutos entre función y función.
- **Compra → Entradas:** una compra es de un cliente, para una función, y tiene una entrada por butaca. Puede tener una promoción y un pago.
- **Pago:** uno por compra. Si se aprueba, la compra pasa a `CONFIRMADA`.

Estados que usamos:

- Compra: `PENDIENTE_PAGO`, `CONFIRMADA`, `CANCELADA`
- Entrada: `EMITIDA`, `USADA`, `CANCELADA`
- Pago: `PENDIENTE`, `APROBADO`, `RECHAZADO`

## Anotaciones que más usamos

- `@Entity` y `@Table`: la clase es una tabla.
- `@Id` y `@GeneratedValue(strategy = IDENTITY)`: clave primaria autoincremental.
- `@Column(nullable = false, unique = true, ...)`: restricciones de la columna.
- `@ManyToOne` + `@JoinColumn`: el lado que tiene la clave foránea.
- `@OneToMany(mappedBy = ...)`: el otro lado de la relación. La FK no está en esta tabla sino en la de los "muchos".
- `@Enumerated(EnumType.STRING)`: guarda el nombre del enum en vez del número, así la base se entiende y no se rompe si cambia el orden.
- `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, etc.: definen los endpoints.
- `@Valid` en el controller y `@NotBlank`, `@Email`, `@Min`, `@Future` en los DTOs: validación de los datos que llegan.
- `@Transactional` en los services: todo lo que pasa en el método se guarda junto o no se guarda nada.
- `@Getter`, `@Setter` y `@RequiredArgsConstructor` (Lombok): generan getters, setters y el constructor para la inyección de dependencias.

## Decisiones que tomamos

**DTOs en vez de devolver entidades.** Así no exponemos cosas como el password, evitamos loops al serializar relaciones bidireccionales y la API no depende de cómo están armadas las tablas. Los DTOs son `record` de Java, que ya traen constructor, accesores, `equals` y `toString`.

**Lombok, pero sin `@Data` en las entidades.** `@Data` arma `equals`, `hashCode` y `toString` con todos los campos. Con relaciones de ida y vuelta, como `Cine` y `Sala`, esos métodos se llaman entre sí hasta tirar `StackOverflowError`. Por eso usamos solo `@Getter` y `@Setter`.

**Baja lógica.** Los `DELETE` de películas, cines y clientes no borran la fila: la marcan como inactiva. Hay compras y funciones que apuntan a esos registros, y además queremos conservar el historial. Por eso, después de un `DELETE`, un `GET` por id sigue encontrando el registro.

**No vender dos veces la misma butaca.** Es lo que más nos preocupaba. Hay dos controles:

1. Antes de guardar, `CompraService` revisa que la butaca no tenga una entrada vigente para esa función.
2. En la base, la tabla `entradas` tiene una restricción única sobre `funcion_id + butaca_id + vigente`.

El primer control solo no alcanza: si dos personas compran la misma butaca al mismo tiempo, las dos pueden pasar la validación antes de que alguna guarde. La restricción de la base es la que asegura que solo entre una. La segunda falla con `DataIntegrityViolationException` y le devolvemos un 409. Para poder capturar ese error ahí mismo usamos `saveAndFlush`, que obliga a escribir en ese momento y no recién al final de la transacción.

¿Por qué `vigente` en la restricción? Al principio era solo `funcion_id + butaca_id`, y nos pasó que si alguien cancelaba una compra, esa butaca ya no se podía volver a vender, porque la entrada cancelada seguía ocupando el lugar. Ahora `vigente` vale `TRUE` mientras la entrada está activa y pasa a `NULL` cuando se cancela. En SQL, una restricción única no compara los `NULL`, así que la entrada cancelada queda guardada como historial pero ya no bloquea la butaca.

**Filtros de funciones en la base.** `GET /api/funciones` filtra con una consulta JPQL en `FuncionRepository.buscarConFiltros`. Usa `join fetch` para traer la película, la sala y el cine en el mismo SELECT. Sin eso, Hibernate hace una consulta extra por cada relación de cada función (el problema N+1).

**Sin JDBC a mano.** No escribimos `PreparedStatement` ni `executeQuery()`. Hibernate los usa por debajo. Con `spring.jpa.show-sql=true` se ve en la consola el SQL que genera, con los `?` de los parámetros.

**H2 en archivo.** Es una base SQL real que no requiere instalar nada, y guarda los datos en `data/`, así que no se pierden al reiniciar. Para ver las tablas está la consola en `/h2-console`.

**Login simple.** Por ahora el login compara email y contraseña y devuelve el rol. No agregamos JWT ni Spring Security porque no era el foco de esta etapa.

## Errores

Todos los errores salen con el mismo formato JSON (`status`, `mensaje`, `path`, `detalles`, etc.). El `GlobalExceptionHandler` decide el código:

| Excepción | Código |
| --- | --- |
| `MethodArgumentNotValidException` (falla `@Valid`) | 400, con el detalle de cada campo |
| `HttpMessageNotReadableException` (JSON roto, enum o fecha inválidos) | 400 |
| `MethodArgumentTypeMismatchException` (por ejemplo `/api/peliculas/abc`) | 400 |
| `ReglaNegocioException` | 400 |
| `CredencialesInvalidasException` | 401 |
| `RecursoNoEncontradoException` o endpoint inexistente | 404 |
| Método HTTP no soportado | 405 |
| `ButacaNoDisponibleException`, `RecursoDuplicadoException`, violación de restricción en la base | 409 |
| Content-Type que no es JSON | 415 |
| Cualquier otra cosa | 500 |

`ButacaNoDisponibleException`, `RecursoDuplicadoException` y `CredencialesInvalidasException` heredan de `ReglaNegocioException`. Spring usa el handler más específico, por eso una butaca ocupada da 409 y no 400.

## Cómo probarlo

En `requests/cineclick-demo.http` está el recorrido completo: consultas, altas, modificaciones, bajas, una compra, la cancelación y el pago, además de casos con error (datos inválidos, id que no existe, JSON roto, butaca repetida).

Los tests de `ApiIntegrationTests` levantan la app con una base en memoria y prueban lo mismo de forma automática (`./mvnw test`).
