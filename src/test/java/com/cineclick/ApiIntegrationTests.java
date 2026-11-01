package com.cineclick;

import com.cineclick.dto.ButacaResponseDTO;
import com.cineclick.dto.CineRequestDTO;
import com.cineclick.dto.CineResponseDTO;
import com.cineclick.dto.FuncionRequestDTO;
import com.cineclick.dto.LoginResponseDTO;
import com.cineclick.dto.MetricasResponseDTO;
import com.cineclick.dto.SalaRequestDTO;
import com.cineclick.dto.SalaResponseDTO;
import com.cineclick.dto.ClienteRequestDTO;
import com.cineclick.dto.ClienteResponseDTO;
import com.cineclick.dto.CompraRequestDTO;
import com.cineclick.dto.CompraResponseDTO;
import com.cineclick.dto.EntradaSolicitadaDTO;
import com.cineclick.dto.ErrorResponseDTO;
import com.cineclick.dto.FuncionResponseDTO;
import com.cineclick.dto.LoginRequestDTO;
import com.cineclick.dto.PagoRequestDTO;
import com.cineclick.dto.PeliculaRequestDTO;
import com.cineclick.dto.PeliculaResponseDTO;
import com.cineclick.model.Clasificacion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.Genero;
import com.cineclick.model.Idioma;
import com.cineclick.model.MetodoPago;
import com.cineclick.model.TipoEntrada;
import com.cineclick.model.Butaca;
import com.cineclick.model.Compra;
import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoEntrada;
import com.cineclick.model.Provincia;
import com.cineclick.model.TipoSala;
import com.cineclick.repository.ButacaRepository;
import com.cineclick.repository.ClienteRepository;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.service.MetricasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTests {

    @LocalServerPort
    private int port;

    // El cliente por defecto no soporta PATCH (lo usamos para cancelar compras)
    private final TestRestTemplate restTemplate = new TestRestTemplate(
        new RestTemplateBuilder().requestFactory(() -> new JdkClientHttpRequestFactory())
    );

    @Test
    void permiteConsultarRecursosConGet() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/peliculas"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Wildwood");
    }

    @Test
    void permiteCrearModificarYEliminarPelicula() {
        PeliculaRequestDTO crear = new PeliculaRequestDTO(
            "Matrix",
            "Un programador descubre que la realidad es una simulacion.",
            136,
            Clasificacion.MAS_13,
            Genero.CIENCIA_FICCION,
            Idioma.SUBTITULADA,
            Set.of(FormatoFuncion.DOS_D, FormatoFuncion.IMAX),
            "https://example.com/matrix.jpg",
            LocalDate.of(1999, 3, 31)
        );

        ResponseEntity<PeliculaResponseDTO> post = restTemplate.postForEntity(
            url("/api/peliculas"),
            crear,
            PeliculaResponseDTO.class
        );

        assertThat(post.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(post.getBody()).isNotNull();
        assertThat(post.getBody().titulo()).isEqualTo("Matrix");

        PeliculaRequestDTO actualizar = new PeliculaRequestDTO(
            "Matrix Recargada",
            crear.sinopsis(),
            crear.duracionMinutos(),
            crear.clasificacion(),
            crear.genero(),
            crear.idioma(),
            crear.formatosDisponibles(),
            crear.urlPoster(),
            crear.fechaEstreno()
        );

        ResponseEntity<PeliculaResponseDTO> put = restTemplate.exchange(
            url("/api/peliculas/" + post.getBody().id()),
            HttpMethod.PUT,
            new HttpEntity<>(actualizar),
            PeliculaResponseDTO.class
        );

        assertThat(put.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(put.getBody()).isNotNull();
        assertThat(put.getBody().titulo()).isEqualTo("Matrix Recargada");

        ResponseEntity<Void> delete = restTemplate.exchange(
            url("/api/peliculas/" + post.getBody().id()),
            HttpMethod.DELETE,
            HttpEntity.EMPTY,
            Void.class
        );

        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void rechazaDatosInvalidosConRespuestaComprensible() {
        ClienteRequestDTO invalido = new ClienteRequestDTO("", "", "email-invalido", "1234", "1122334455");

        ResponseEntity<ErrorResponseDTO> response = restTemplate.postForEntity(
            url("/api/clientes"),
            invalido,
            ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensaje()).isEqualTo("Hay datos invalidos");
        assertThat(response.getBody().detalles()).isNotEmpty();
    }

    @Test
    void informaCuandoUnRecursoNoExiste() {
        ResponseEntity<ErrorResponseDTO> response = restTemplate.getForEntity(
            url("/api/peliculas/999999"),
            ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensaje()).contains("Pelicula no encontrada");
    }

    @Test
    void permiteCrearClienteConPost() {
        ClienteRequestDTO request = new ClienteRequestDTO(
            "Ana",
            "Gomez",
            "ana.gomez@example.com",
            "1234",
            "1122334455"
        );

        ResponseEntity<ClienteResponseDTO> response = restTemplate.postForEntity(
            url("/api/clientes"),
            request,
            ClienteResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("ana.gomez@example.com");
        assertThat(response.getBody().activo()).isTrue();
    }

    @Test
    void permiteRecomprarUnaButacaDespuesDeCancelarLaCompra() {
        ClienteResponseDTO cliente = restTemplate.postForEntity(
            url("/api/clientes"),
            new ClienteRequestDTO("Juan", "Diaz", "juan.recompra@example.com", "1234", null),
            ClienteResponseDTO.class
        ).getBody();
        assertThat(cliente).isNotNull();

        FuncionResponseDTO funcion = restTemplate.getForObject(url("/api/funciones"), FuncionResponseDTO[].class)[0];
        ButacaResponseDTO butaca = restTemplate.getForObject(
            url("/api/funciones/" + funcion.id() + "/butacas-disponibles"),
            ButacaResponseDTO[].class
        )[0];
        CompraRequestDTO compra = new CompraRequestDTO(
            cliente.id(),
            funcion.id(),
            List.of(butaca.id()),
            List.of(new EntradaSolicitadaDTO(TipoEntrada.GENERAL, 1)),
            null
        );

        ResponseEntity<CompraResponseDTO> primera = restTemplate.postForEntity(url("/api/compras"), compra, CompraResponseDTO.class);
        assertThat(primera.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<ErrorResponseDTO> repetida = restTemplate.postForEntity(url("/api/compras"), compra, ErrorResponseDTO.class);
        assertThat(repetida.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        restTemplate.exchange(url("/api/compras/" + primera.getBody().id() + "/cancelar"), HttpMethod.PATCH, HttpEntity.EMPTY, CompraResponseDTO.class);

        ResponseEntity<CompraResponseDTO> recompra = restTemplate.postForEntity(url("/api/compras"), compra, CompraResponseDTO.class);
        assertThat(recompra.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void rechazaReservaConCantidadDeEntradasDistintaALaDeButacas() {
        ClienteResponseDTO cliente = crearCliente("sofia.cantidad@example.com");
        FuncionResponseDTO funcion = restTemplate.getForObject(url("/api/funciones"), FuncionResponseDTO[].class)[0];
        ButacaResponseDTO[] butacas = restTemplate.getForObject(
            url("/api/funciones/" + funcion.id() + "/butacas-disponibles"),
            ButacaResponseDTO[].class
        );
        CompraRequestDTO compra = new CompraRequestDTO(
            cliente.id(),
            funcion.id(),
            List.of(butacas[1].id(), butacas[2].id()),
            List.of(new EntradaSolicitadaDTO(TipoEntrada.GENERAL, 1), new EntradaSolicitadaDTO(TipoEntrada.MENOR, 2)),
            null
        );

        ResponseEntity<ErrorResponseDTO> response = restTemplate.postForEntity(url("/api/compras"), compra, ErrorResponseDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensaje()).contains("no coincide");
    }

    @Test
    void rechazaPagoSinAceptarTerminos() {
        ClienteResponseDTO cliente = crearCliente("pablo.terminos@example.com");
        FuncionResponseDTO funcion = restTemplate.getForObject(url("/api/funciones"), FuncionResponseDTO[].class)[0];
        ButacaResponseDTO[] butacas = restTemplate.getForObject(
            url("/api/funciones/" + funcion.id() + "/butacas-disponibles"),
            ButacaResponseDTO[].class
        );
        CompraRequestDTO reserva = new CompraRequestDTO(
            cliente.id(),
            funcion.id(),
            List.of(butacas[3].id()),
            List.of(new EntradaSolicitadaDTO(TipoEntrada.JUBILADO, 1)),
            null
        );
        CompraResponseDTO compra = restTemplate.postForEntity(url("/api/compras"), reserva, CompraResponseDTO.class).getBody();
        assertThat(compra).isNotNull();

        PagoRequestDTO pago = new PagoRequestDTO(MetodoPago.TARJETA_CREDITO, "Visa", "1111", false);
        ResponseEntity<ErrorResponseDTO> response = restTemplate.postForEntity(
            url("/api/compras/" + compra.id() + "/pagos"),
            pago,
            ErrorResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().detalles()).anyMatch(detalle -> detalle.startsWith("aceptaTerminos"));
        CompraResponseDTO sinPagar = restTemplate.getForObject(url("/api/compras/" + compra.id()), CompraResponseDTO.class);
        assertThat(sinPagar.estado()).isEqualTo(EstadoCompra.PENDIENTE_PAGO);
    }

    @Test
    void filtraFuncionesPorPeliculaYFecha() {
        FuncionResponseDTO funcion = restTemplate.getForObject(url("/api/funciones"), FuncionResponseDTO[].class)[0];
        String filtro = "/api/funciones?peliculaId=" + funcion.peliculaId() + "&fecha=" + funcion.fechaHoraInicio().toLocalDate();

        FuncionResponseDTO[] filtradas = restTemplate.getForObject(url(filtro), FuncionResponseDTO[].class);

        assertThat(filtradas).isNotEmpty();
        assertThat(filtradas).allMatch(f -> f.peliculaId().equals(funcion.peliculaId()));
    }

    @Test
    void respondeErroresDelClienteSinDevolver500() {
        HttpHeaders json = new HttpHeaders();
        json.setContentType(MediaType.APPLICATION_JSON);

        assertThat(restTemplate.getForEntity(url("/api/peliculas/abc"), String.class).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(restTemplate.postForEntity(url("/api/peliculas"), new HttpEntity<>("{\"titulo\": ", json), String.class).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        String generoInvalido = """
            {"titulo":"a","sinopsis":"b","duracionMinutos":10,"clasificacion":"ATP",
             "genero":"XX","idioma":"es","formatosDisponibles":["DOS_D"]}
            """;
        assertThat(restTemplate.postForEntity(url("/api/peliculas"), new HttpEntity<>(generoInvalido, json), String.class).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(restTemplate.getForEntity(url("/api/funciones?formato=XX"), String.class).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(restTemplate.getForEntity(url("/api/no-existe"), String.class).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(restTemplate.exchange(url("/api/peliculas/1"), HttpMethod.PATCH, HttpEntity.EMPTY, String.class).getStatusCode())
            .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void loginIncorrectoDevuelve401YEmailDuplicado409() {
        ResponseEntity<String> login = restTemplate.postForEntity(
            url("/api/auth/login"),
            new LoginRequestDTO("admin@cineclick.com", "incorrecta"),
            String.class
        );
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<String> duplicado = restTemplate.postForEntity(
            url("/api/clientes"),
            new ClienteRequestDTO("Otro", "Admin", "admin@cineclick.com", "1234", null),
            String.class
        );
        assertThat(duplicado.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private FuncionRepository funcionRepository;

    @Autowired
    private ButacaRepository butacaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    private record FixtureMetricas(Long cineId, Long funcionGrande, Long funcionChica, Long clienteId, LocalDate sabado) {
    }

    @Test
    void metricasNoSumanIngresosDeComprasCanceladasNiVencidas() {
        FixtureMetricas fx = fixtureMetricas();
        LocalDateTime ahora = LocalDateTime.now(MetricasService.ZONA);
        venderEntradas(fx, fx.funcionGrande(), 0, 2, EstadoCompra.CONFIRMADA, ahora.minusMinutes(30));
        venderEntradas(fx, fx.funcionGrande(), 2, 3, EstadoCompra.CANCELADA, ahora.minusMinutes(20));
        venderEntradas(fx, fx.funcionGrande(), 5, 1, EstadoCompra.PENDIENTE_PAGO, ahora.minusMinutes(15));

        LocalDate hoy = ahora.toLocalDate();
        MetricasResponseDTO metricas = pedirMetricas(hoy, hoy, fx.cineId());

        assertThat(metricas.resumen().ingresos().valor()).isEqualByComparingTo("2000");
        assertThat(metricas.resumen().entradasVendidas().valor()).isEqualByComparingTo("2");
        assertThat(metricas.resumen().ticketPromedio().valor()).isEqualByComparingTo("2000");
        assertThat(metricas.conversion().creadas()).isEqualTo(3);
        assertThat(metricas.conversion().pagadas()).isEqualTo(1);
        assertThat(metricas.conversion().vencidas()).isEqualTo(2);
        assertThat(metricas.conversion().pendientes()).isZero();
    }

    @Test
    void metricasCuentanIngresosPorFechaDeCompraYOcupacionPorFechaDeFuncion() {
        FixtureMetricas fx = fixtureMetricas();
        LocalDateTime ahora = LocalDateTime.now(MetricasService.ZONA);
        venderEntradas(fx, fx.funcionGrande(), 0, 10, EstadoCompra.CONFIRMADA, ahora.minusMinutes(5));

        LocalDate hoy = ahora.toLocalDate();
        MetricasResponseDTO deHoy = pedirMetricas(hoy, hoy, fx.cineId());
        assertThat(deHoy.resumen().ingresos().valor()).isEqualByComparingTo("10000");
        assertThat(deHoy.resumen().ocupacion().valor()).isEqualByComparingTo("0");

        MetricasResponseDTO delSabado = pedirMetricas(fx.sabado(), fx.sabado(), fx.cineId());
        assertThat(delSabado.resumen().ingresos().valor()).isEqualByComparingTo("0");
        assertThat(delSabado.resumen().ocupacion().valor()).isEqualByComparingTo("0.0667");
        assertThat(delSabado.franjas()).containsExactly(new MetricasResponseDTO.Franja(6, 18, 10));
    }

    @Test
    void metricasCalculanLaOcupacionPonderadaPorCapacidad() {
        FixtureMetricas fx = fixtureMetricas();
        LocalDateTime ayer = LocalDateTime.now(MetricasService.ZONA).minusDays(1);
        venderEntradas(fx, fx.funcionGrande(), 0, 50, EstadoCompra.CONFIRMADA, ayer);
        venderEntradas(fx, fx.funcionChica(), 0, 50, EstadoCompra.CONFIRMADA, ayer);

        MetricasResponseDTO metricas = pedirMetricas(fx.sabado(), fx.sabado(), fx.cineId());

        assertThat(metricas.resumen().ocupacion().valor()).isEqualByComparingTo("0.6667");
    }

    @Test
    void metricasComparanConUnPeriodoAnteriorDelMismoLargo() {
        FixtureMetricas fx = fixtureMetricas();
        LocalDate hoy = LocalDate.now(MetricasService.ZONA);
        venderEntradas(fx, fx.funcionGrande(), 0, 1, EstadoCompra.CONFIRMADA, hoy.minusDays(1).atTime(12, 0));
        venderEntradas(fx, fx.funcionGrande(), 1, 2, EstadoCompra.CONFIRMADA, hoy.minusDays(3).atTime(12, 0));
        venderEntradas(fx, fx.funcionGrande(), 3, 5, EstadoCompra.CONFIRMADA, hoy.minusDays(6).atTime(12, 0));

        MetricasResponseDTO metricas = pedirMetricas(hoy.minusDays(2), hoy, fx.cineId());

        assertThat(metricas.desdeAnterior()).isEqualTo(hoy.minusDays(5));
        assertThat(metricas.hastaAnterior()).isEqualTo(hoy.minusDays(3));
        assertThat(metricas.resumen().ingresos().valor()).isEqualByComparingTo("1000");
        assertThat(metricas.resumen().ingresos().valorAnterior()).isEqualByComparingTo("2000");
    }

    @Test
    void metricasSoloLasVeUnAdministrador() {
        LocalDate hoy = LocalDate.now(MetricasService.ZONA);
        String ruta = "/api/admin/metricas?desde=" + hoy + "&hasta=" + hoy;
        ClienteResponseDTO cliente = crearCliente("metricas-" + UUID.randomUUID() + "@mail.com");

        assertThat(restTemplate.exchange(url(ruta), HttpMethod.GET, conUsuario(cliente.usuarioId()), String.class).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.getForEntity(url(ruta), String.class).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.exchange(url(ruta), HttpMethod.GET, conUsuario(idAdmin()), String.class).getStatusCode())
            .isEqualTo(HttpStatus.OK);
    }

    // Cine nuevo con dos salas (100 y 50 butacas) y una funcion en cada una el proximo sabado a las 18
    private FixtureMetricas fixtureMetricas() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        CineResponseDTO cine = restTemplate.postForEntity(url("/api/cines"),
            new CineRequestDTO("Metricas " + sufijo, Provincia.CABA, "Palermo", "Calle", 100, null, null), CineResponseDTO.class).getBody();
        SalaResponseDTO grande = restTemplate.postForEntity(url("/api/cines/" + cine.id() + "/salas"),
            new SalaRequestDTO("Grande", 1, TipoSala.TRADICIONAL, 10, 10, null), SalaResponseDTO.class).getBody();
        SalaResponseDTO chica = restTemplate.postForEntity(url("/api/cines/" + cine.id() + "/salas"),
            new SalaRequestDTO("Chica", 2, TipoSala.TRADICIONAL, 5, 10, null), SalaResponseDTO.class).getBody();
        PeliculaResponseDTO pelicula = restTemplate.postForEntity(url("/api/peliculas"), new PeliculaRequestDTO(
            "Metricas " + sufijo, "Pelicula para las metricas", 100, Clasificacion.ATP, Genero.DRAMA, Idioma.CASTELLANO,
            Set.of(FormatoFuncion.DOS_D), null, null
        ), PeliculaResponseDTO.class).getBody();
        LocalDate sabado = LocalDate.now(MetricasService.ZONA).with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        Long funcionGrande = crearFuncion(pelicula.id(), grande.id(), sabado.atTime(18, 0));
        Long funcionChica = crearFuncion(pelicula.id(), chica.id(), sabado.atTime(18, 0));
        ClienteResponseDTO cliente = crearCliente("metricas-" + sufijo + "@mail.com");
        return new FixtureMetricas(cine.id(), funcionGrande, funcionChica, cliente.id(), sabado);
    }

    private Long crearFuncion(Long peliculaId, Long salaId, LocalDateTime inicio) {
        FuncionResponseDTO funcion = restTemplate.postForEntity(url("/api/funciones"),
            new FuncionRequestDTO(peliculaId, salaId, inicio, new BigDecimal("1000"), FormatoFuncion.DOS_D, Idioma.CASTELLANO),
            FuncionResponseDTO.class).getBody();
        assertThat(funcion).isNotNull();
        return funcion.id();
    }

    // Compra armada directo con los repositorios (asi se puede fijar la fecha y el estado), con
    // "cantidad" entradas de $ 1000 en las butacas que siguen a "desde"
    private void venderEntradas(FixtureMetricas fx, Long funcionId, int desde, int cantidad, EstadoCompra estado, LocalDateTime fecha) {
        var funcion = funcionRepository.findById(funcionId).orElseThrow();
        List<Butaca> butacas = butacaRepository.findBySalaIdAndActivaTrue(funcion.getSala().getId());
        Compra compra = new Compra();
        compra.setCliente(clienteRepository.findById(fx.clienteId()).orElseThrow());
        compra.setFuncion(funcion);
        compra.setFechaCompra(fecha);
        compra.setEstado(estado);
        for (Butaca butaca : butacas.subList(desde, desde + cantidad)) {
            Entrada entrada = new Entrada();
            entrada.setButaca(butaca);
            entrada.setCodigoQr(UUID.randomUUID().toString().substring(0, 18));
            entrada.setPrecioUnitario(new BigDecimal("1000"));
            entrada.setTipo(TipoEntrada.GENERAL);
            if (estado == EstadoCompra.CANCELADA) {
                entrada.setEstado(EstadoEntrada.CANCELADA);
                entrada.setVigente(null);
            }
            compra.agregarEntrada(entrada);
        }
        compra.setTotal(new BigDecimal(1000L * cantidad));
        compraRepository.save(compra);
    }

    private MetricasResponseDTO pedirMetricas(LocalDate desde, LocalDate hasta, Long cineId) {
        ResponseEntity<MetricasResponseDTO> respuesta = restTemplate.exchange(
            url("/api/admin/metricas?desde=" + desde + "&hasta=" + hasta + "&cineId=" + cineId),
            HttpMethod.GET, conUsuario(idAdmin()), MetricasResponseDTO.class
        );
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody();
    }

    private Long idAdmin() {
        return restTemplate.postForEntity(url("/api/auth/login"), new LoginRequestDTO("admin@cineclick.com", "admin123"), LoginResponseDTO.class)
            .getBody().usuarioId();
    }

    private HttpEntity<Void> conUsuario(Long usuarioId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Usuario-Id", String.valueOf(usuarioId));
        return new HttpEntity<>(headers);
    }

    private ClienteResponseDTO crearCliente(String email) {
        ClienteResponseDTO cliente = restTemplate.postForEntity(
            url("/api/clientes"),
            new ClienteRequestDTO("Cliente", "Prueba", email, "1234", null),
            ClienteResponseDTO.class
        ).getBody();
        assertThat(cliente).isNotNull();
        return cliente;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
