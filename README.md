# CineClick – Backend

Backend de CineClick, una app de venta de entradas de cine, para el TPO de Aplicaciones Interactivas: API REST con Spring Boot y base de datos SQL. El frontend (React) está en [Roman-lc/FrontCineClick](https://github.com/Roman-lc/FrontCineClick).

**Stack:** Java 21, Spring Boot 3.3 (Web, Data JPA, Validation), H2, Lombok, Maven y `spring-security-crypto` (solo para BCrypt).

## Requisitos

- **JDK 21.** Nada más: Maven lo baja el wrapper (`mvnw`) y la base de datos es H2 embebida, que se guarda en un archivo dentro del proyecto. No hace falta Docker ni instalar una base.

## Levantar desde cero

```bash
git clone https://github.com/LucasGagliani/CineClick.git
cd CineClick
./mvnw spring-boot:run
```

En Windows con CMD es `mvnw.cmd spring-boot:run`. También se puede correr `CineClickApplication` desde IntelliJ (con la carpeta del proyecto como directorio de trabajo, para que la base quede en `data/`).

La API queda en `http://localhost:8080`. La primera vez que arranca con la base vacía siembra los datos de demo (tarda unos segundos); los arranques siguientes no los duplican.

## Configuración

Todo está en `src/main/resources/application.properties`. Cualquier valor se puede cambiar sin tocar el archivo con una variable de entorno (Spring la toma con mayúsculas y guiones bajos: `SERVER_PORT=8081`) o con un argumento (`./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`). No hay secretos ni archivo `.env`.

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `server.port` | `8080` | Puerto de la API |
| `spring.profiles.default` | `demo` | Perfil con el seed de datos de demo. Los tests usan el perfil `test`, sin seed |
| `spring.datasource.url` | `jdbc:h2:file:./data/cineclick` | Base H2 en archivo, en `data/` |
| `spring.jpa.show-sql` | `false` | `true` muestra cada consulta SQL en la consola |
| `cineclick.demo.semilla` | `20261101` | Semilla del generador aleatorio del seed: misma semilla, mismos datos |
| `cineclick.funciones.minutos-limpieza` | `20` | Margen entre funciones de una sala |
| `cineclick.funciones.minutos-publicidad` | `15` | Publicidad antes de la película (para estimar el fin en la doble función) |
| `cineclick.combinables.margen-minutos` / `ventana-horas` | `10` / `3` | Doble función: margen mínimo entre una y otra y ventana de búsqueda |
| `cineclick.limite.pedidos-por-minuto` | `20` | Límite por IP de los formularios públicos (búsqueda de compra, arrepentimiento, soporte) |
| `cineclick.ingreso.minutos-antes` / `minutos-despues` | `60` / `30` | Ventana de ingreso para validar entradas |

## Datos de demo (seed)

Corre solo en el perfil `demo` (el de por defecto) y está en `config/`: `CatalogoDemo` (cines, salas, películas y cuentas), `ProgramadorDeFunciones` (horarios) y `GeneradorDeCompras` (compras y lo que depende de ellas).

- **Fechas relativas al día de arranque:** funciones desde hace 21 días hasta 7 días hacia adelante, compras de los últimos 21 días y estrenos ubicados respecto de hoy. La cartelera nunca queda vacía, se clone cuando se clone.
- **Determinístico:** usa un generador aleatorio con semilla fija (`cineclick.demo.semilla`), así todos obtienen los mismos datos.
- **Idempotente:** si la base ya tiene datos, no los duplica. En cada arranque completa las funciones que falten para cubrir los próximos 7 días.

Qué crea:

- **4 cines** (Abasto, Recoleta, Palermo y Núñez) con 4 a 6 salas: 2D tradicionales, una 3D, una Premium y, en algunos, IMAX o 4D; de 60 a 242 butacas, con pasillos.
- **17 películas** (en cartelera, en preventa y próximos estrenos) y **unas 50 funciones**: 8 hoy (dos por cine, una a la tarde y otra a la noche), 6 mañana, 2 por día hasta dentro de una semana y 1 por día en las tres semanas anteriores (para el historial). Los horarios siguen la grilla de una sala real: la primera entre las 12:30 y las 14:30 y la última a más tardar a las 23:30 (00:30 viernes y sábados); cada una empieza después del fin de la anterior (duración + 15 min de publicidad + 15 a 20 de limpieza), redondeado a múltiplos de 10. El formato coincide con la sala; más castellano en animación, más subtitulado a la noche; más funciones para los estrenos.
- **Unas 500 compras** con butacas únicas y totales calculados como en el backend: más a la noche y los fines de semana, de 1 a 6 entradas (más peso en 2), ~70 % General, ~15 % Menor (solo en películas ATP), ~15 % Jubilado; ~15 % con promoción; ~20 % vencidas o canceladas y ~2 % arrepentidas; en funciones pasadas, ~85 % de las entradas usadas. Hoy hay una función agotada y algunas "Casi llena", y entradas de hoy en cada cine para probar la validación.
- **8 promociones**, una por cada regla y estado (ver más abajo), con los contadores de uso de acuerdo con las compras.
- **5 consultas de soporte** (abiertas y resueltas) y una solicitud de arrepentimiento fuera de plazo pendiente de revisión.
- **1 administrador, 4 clientes de prueba y 60 clientes genéricos** (sin contraseña publicada) que generan el volumen para las métricas.

**Resetear la demo:** frenar el backend, borrar la carpeta `data/` y volver a arrancar. Se siembra todo de nuevo con las fechas del día.

La demo completa (cuentas de prueba, compras y promociones) solo se siembra sobre una base vacía. Si `data/` ya tenía datos de una versión anterior, el backend deja un aviso en el log y no crea las cuentas de prueba (que aparecerían sin compras): hay que resetear.

### Cuentas de prueba

| Cuenta | Email | Contraseña | Qué muestra |
|---|---|---|---|
| Administrador | `admin@cineclick.com` | `admin123` | Panel completo con métricas, compras, validación y promociones |
| Cliente frecuente | `ana@cineclick.com` | `ana123` | Compras próximas y anteriores, entradas usadas, una arrepentida y otra pendiente de revisión |
| Cliente con función hoy | `martin@cineclick.com` | `martin123` | Una función hoy a la noche |
| Cliente nuevo | `sofia@cineclick.com` | `sofia123` | Sin compras: puede usar `BIENVENIDA` |
| Cliente ocasional | `diego@cineclick.com` | `diego123` | Tres compras: una vista, una próxima y una vencida |

### Base de datos

H2 guardada en archivo (`data/cineclick.mv.db`): los datos no se pierden al reiniciar. Para ver las tablas desde el navegador:

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/cineclick`
- Usuario `sa`, sin contraseña

Al arrancar, `MigracionDatos` actualiza bases de versiones anteriores (enums, vigencia de promociones, contraseñas en texto plano a BCrypt, salas sin plano) y deja en el log lo que cambió.

## Estructura

```text
controller  → recibe los requests HTTP y devuelve JSON
service     → reglas de negocio
repository  → acceso a datos (Spring Data JPA, cumple el rol de DAO)
model       → entidades JPA
dto         → lo que entra y sale de la API
exception   → manejo de errores centralizado
config      → seed de demo, migraciones, control de admin (@SoloAdmin), BCrypt y reloj
```

El flujo es `Cliente → Controller → Service → Repository → Base de datos`. En `documentacion/` están los diagramas UML y `explicacion-del-proyecto.md`.

## Endpoints

🔒 = solo administradores (encabezado `X-Usuario-Id` con el id de un usuario ADMIN; si no, `403`).

| Recurso | Endpoints |
|---|---|
| Auth | `POST /api/auth/login` |
| Películas | `GET /api/peliculas` · `GET /api/peliculas/{id}` · `GET /api/peliculas/destacadas` · `GET /api/peliculas/proximos-estrenos` · 🔒 `POST /api/peliculas` · 🔒 `PUT`, `DELETE /api/peliculas/{id}` · 🔒 `PATCH /api/peliculas/{id}/estado` |
| Lista de espera | `POST /api/peliculas/{id}/lista-espera` · `DELETE /api/peliculas/{id}/lista-espera/{clienteId}` · `GET /api/clientes/{id}/lista-espera` |
| Cines | `GET /api/cines` · `GET /api/cines/{id}` · `GET /api/cines/{id}/salas` · 🔒 `POST /api/cines` · 🔒 `PUT`, `DELETE /api/cines/{id}` · 🔒 `PATCH /api/cines/{id}/estado` · 🔒 `POST /api/cines/{id}/salas` |
| Salas | `GET /api/salas` · 🔒 `PUT`, `DELETE /api/salas/{id}` · 🔒 `PATCH /api/salas/{id}/estado` |
| Funciones | `GET /api/funciones` · `GET /api/funciones/{id}` · `GET /api/funciones/{id}/butacas-disponibles` · `GET /api/funciones/{id}/precios` · `GET /api/funciones/{id}/combinables` · 🔒 `POST /api/funciones` · 🔒 `PUT`, `DELETE /api/funciones/{id}` · 🔒 `PATCH /api/funciones/{id}/cancelar` |
| Clientes | `POST /api/clientes` (registro) · `GET /api/clientes/{id}/compras` · 🔒 `GET /api/clientes` · 🔒 `GET`, `PUT`, `DELETE /api/clientes/{id}` |
| Compras | `POST /api/compras` · `GET /api/compras/{id}` · `PATCH /api/compras/{id}/cancelar` · `POST /api/compras/{id}/promocion` · `GET /api/compras/buscar?email=&codigo=` |
| Pagos | `POST /api/compras/{id}/pagos` · `GET /api/pagos/{id}` |
| Promociones | `GET /api/promociones/vigentes` · 🔒 `GET`, `POST /api/promociones` · 🔒 `PUT`, `DELETE /api/promociones/{id}` · 🔒 `PATCH /api/promociones/{id}/estado` |
| Arrepentimiento | `POST /api/arrepentimientos` · 🔒 `GET /api/admin/arrepentimientos` · 🔒 `PATCH /api/admin/arrepentimientos/{id}/aprobar` · 🔒 `PATCH /api/admin/arrepentimientos/{id}/rechazar` |
| Soporte | `POST /api/soporte` · 🔒 `GET /api/admin/soporte?estado=` · 🔒 `PATCH /api/admin/soporte/{id}/resolver` |
| Compras (admin) | 🔒 `GET /api/admin/compras` · 🔒 `GET /api/admin/compras/{id}` |
| Métricas | 🔒 `GET /api/admin/metricas?desde=&hasta=&cineId=` |
| Entradas | 🔒 `POST /api/entradas/{codigo}/validar` |

Detalles de los endpoints nuevos:

- **`GET /api/funciones`** acepta los filtros `peliculaId`, `cineId`, `fecha` (`2026-09-25`) y `formato` (`IMAX`). Por defecto no devuelve funciones de cines inactivos; el panel pide todo con `incluirInactivos=true` (lo mismo `/api/cines?incluirInactivos=true` y `/api/peliculas?incluirInactivas=true`).
- **`GET /api/funciones/{id}/combinables`** (doble función): funciones del mismo cine que terminan antes o empiezan después de la elegida, con al menos `margen-minutos` entre una y otra (el fin se estima con la duración más la publicidad), dentro de `ventana-horas`, de otra película y con butacas libres. Hasta 4 por lista, con `minutosEntre`.
- **`GET /api/compras/buscar?email=&codigo=`**: búsqueda pública de una compra (por número de compra o código de cualquier entrada) para el arrepentimiento y el soporte. Si el email o el código no coinciden, responde el mismo `404` (no revela si el email existe). Dice si la compra es elegible para arrepentirse y, si no, por qué.
- **`POST /api/arrepentimientos`** `{ email, codigo, nombre, motivo, confirmaAnulacion }`: dentro de los 10 días corridos de la compra y hasta 60 minutos antes de la función, anula las entradas, libera las butacas, registra el reembolso y devuelve el uso de la promoción. Fuera de plazo igual se registra como `FUERA_DE_PLAZO` para que la revise un administrador. Devuelve un código `ARR-XXXXXXXX`; pedirlo de nuevo para la misma compra devuelve la misma solicitud.
- **`POST /api/soporte`** `{ nombre, email, motivo, codigo?, mensaje, claveIdempotencia }`: crea una consulta `SOP-XXXXXXXX`. La `claveIdempotencia` (un UUID por formulario) evita duplicados.
- **`GET /api/admin/compras`**: filtros `codigo` (N° de compra o código de entrada, con o sin guiones), `email` (parcial), `cineId`, `estado`, `desde` y `hasta` (por fecha de compra), `pagina` (desde 0) y `tamano` (hasta 100). Responde `{ contenido, pagina, tamano, totalElementos, totalPaginas }`. `GET /api/admin/compras/{id}` devuelve la compra completa con el email del cliente, la promoción, el arrepentimiento y las consultas de soporte.
- **`GET /api/admin/metricas`** junta en un JSON todos los bloques del panel (resumen contra el período anterior, próximas funciones, ranking, conversión, por cine, franjas, promociones, tipos de entrada y asistencia). Las definiciones están en `MetricasService`: una venta es una compra confirmada; ingresos y entradas van por fecha de compra; la ocupación, por fecha de función y ponderada por capacidad.
- **`POST /api/entradas/{codigo}/validar`** `{ "cineId": 1 }`: ver "Validación de entradas".

Los `DELETE` de cines, salas, películas, funciones y promociones borran el registro solo si no tiene datos asociados. Si los tiene, responden `409` con lo asociado en `detalles` y lo que corresponde es desactivarlo (`PATCH .../estado` con `{"activo": false}`) o, en una función, cancelarla.

En `requests/cineclick-demo.http` hay ejemplos de todos los endpoints con las cuentas y los códigos del seed, para correr desde VS Code (extensión REST Client).

## Control de acceso y contraseñas

- Los endpoints de administración llevan la anotación `@SoloAdmin` (en el método o en el controller). `InterceptorAdmin` la verifica en un solo lugar: lee `X-Usuario-Id` y responde `403` si no es un administrador activo.
- Las contraseñas se guardan con **BCrypt** (`spring-security-crypto` 6.3.10, sin Spring Security completo) y el login las compara con `matches()`. Las contraseñas en texto plano de bases anteriores se migran solas al arrancar.
- Los formularios públicos tienen un límite de pedidos por IP (`429` si se supera).

## Reglas de promociones

Una promoción tiene porcentaje y, opcionalmente: vigencia con fecha y hora, días de la semana (se comparan con el **día de la función**), tope de usos totales, tope de usos por usuario, "solo primera compra", máximo de entradas con descuento (se aplica a las de mayor precio) y monto máximo de descuento. Las reglas se validan al aplicar el código y otra vez al pagar; el uso se cuenta al pagar con un `UPDATE` atómico, así dos pagos simultáneos no superan el tope. Si deja de valer entre la reserva y el pago, se quita la promoción y la reserva sigue. Si el descuento cubre todo, el pago se confirma sin datos de tarjeta (`SIN_CARGO`). Un arrepentimiento aprobado devuelve el uso.

| Código del seed | Regla | Estado |
|---|---|---|
| `BIENVENIDA` | 100 % en 1 entrada, solo primera compra, 1 uso por usuario | Activa |
| `LUNMIEVIE20` | 20 % lunes, miércoles y viernes | Activa |
| `MIERCOLES50` | 50 % los miércoles, máximo 2 entradas con descuento | Activa |
| `FUGAZ30` | 30 %, tope de 50 usos (42 usados), vence en menos de 24 h | Activa |
| `ESTRENO10` | 10 %, sin restricciones | Activa |
| `PRIMAVERA15` | 15 % | Vencida |
| `CYBERCINE25` | 25 % con tope de $ 6.000 | Programada (semana que viene) |
| `PREESTRENO40` | 40 %, tope de usos | Agotada |

## Validación de entradas

`POST /api/entradas/{codigo}/validar` recibe solo el cine del puesto: el código alcanza para saber película, sala, butaca y horario. Responde `200` para todo código que existe (con película, cine, sala, butaca, inicio, tipo de entrada y `usadaEn`) y `404` si no existe:

| Resultado | Cuándo | ¿Marca la entrada? |
|---|---|---|
| `VALIDA` | Es de este cine y está dentro de la ventana de ingreso (60 min antes a 30 min después del inicio) | Sí: queda `USADA` con `usadaEn` |
| `YA_USADA` | Ya se usó; trae `usadaEn` (sirve para detectar capturas reenviadas) | No |
| `OTRO_CINE` | Es de otro cine | No |
| `FUERA_DE_HORARIO` | Es de este cine pero no está en la ventana; `motivo`: `DEMASIADO_TEMPRANO`, `FUNCION_TERMINADA` u `OTRO_DIA` | No |
| `ANULADA` | La compra se canceló, venció o se arrepintió | No |

Primero se calculan las condiciones y después se marca con un `UPDATE ... WHERE estado = 'EMITIDA'`: si dos puestos validan la misma entrada a la vez, solo uno recibe `VALIDA`.

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
|---|---|
| 400 | Datos inválidos (el detalle de cada campo va en `detalles`), JSON mal formado o una regla de negocio que no se cumple |
| 401 | Login incorrecto |
| 403 | Endpoint de administración sin `X-Usuario-Id` de un administrador |
| 404 | No existe el recurso o el endpoint |
| 405 | Método HTTP no soportado |
| 409 | Butaca ya vendida, email ya registrado o registro con datos asociados |
| 429 | Demasiados pedidos desde la misma IP en un formulario público |
| 500 | Error inesperado del servidor |

## Reglas de negocio principales

- Una función no puede superponerse con otra en la misma sala, tiene que ser a futuro, usar un formato que la película tenga y empezar en un horario que termine en 0 o 5 (`19:30`, `22:15`).
- Las butacas de una compra tienen que ser de la sala de la función y estar libres. Una reserva dura 8 minutos; si no se paga, se cancela sola.
- El pago es simulado: se mandan solo la marca y los últimos 4 dígitos (`"0002"` simula un rechazo) y `"aceptaTerminos": true`.
- **Venta doble de una butaca:** además del chequeo en `CompraService`, la tabla `entradas` tiene una restricción única sobre `funcion_id + butaca_id + vigente`. Si dos personas compran la misma butaca a la vez, la base acepta solo la primera y la segunda recibe un `409`. Al cancelar una entrada, `vigente` pasa a `NULL` y la butaca se puede volver a vender.

## Tests

```bash
./mvnw test
```

Son 39 tests de integración que levantan la app con una base en memoria y sus propios datos (`DatosDePrueba`, perfil `test`, sin el seed de demo). Cubren el CRUD, las validaciones y los códigos de error, el flujo de compra, las reglas de promociones (incluido el tope con pagos simultáneos), el arrepentimiento, la doble función, las métricas, el acceso de administrador en cada grupo de endpoints, BCrypt, las compras del admin, los horarios redondos y la validación de entradas (cada resultado y cada motivo, con un reloj de prueba, y dos validaciones simultáneas).

## Limitaciones conocidas

- **Autenticación sin tokens.** El backend identifica al usuario por el encabezado `X-Usuario-Id`, que cualquiera puede falsificar. El control de administrador es básico, no seguridad real; un sistema en producción usaría sesiones o tokens firmados.
- **Sin envío de mails.** Los códigos de arrepentimiento y soporte y las entradas se muestran en pantalla.
- **Pagos simulados.** No hay pasarela de pagos.