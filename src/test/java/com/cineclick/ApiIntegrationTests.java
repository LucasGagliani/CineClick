package com.cineclick;

import com.cineclick.dto.ArrepentimientoRequestDTO;
import com.cineclick.dto.ArrepentimientoResponseDTO;
import com.cineclick.dto.ButacaResponseDTO;
import com.cineclick.dto.CombinablesResponseDTO;
import com.cineclick.dto.CompraAdminDTO;
import com.cineclick.dto.DetalleCompraAdminDTO;
import com.cineclick.dto.PaginaDTO;
import com.cineclick.dto.PromocionRequestDTO;
import com.cineclick.dto.PromocionResponseDTO;
import com.cineclick.dto.ResumenCompraDTO;
import com.cineclick.dto.CineRequestDTO;
import com.cineclick.dto.CineResponseDTO;
import com.cineclick.dto.FuncionRequestDTO;
import com.cineclick.dto.LoginResponseDTO;
import com.cineclick.dto.MetricasResponseDTO;
import com.cineclick.dto.SalaRequestDTO;
import com.cineclick.dto.ValidacionEntradaResponseDTO;
import com.cineclick.dto.ValidarEntradaRequestDTO;
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
import com.cineclick.model.EstadoArrepentimiento;
import com.cineclick.model.MotivoArrepentimiento;
import com.cineclick.model.MotivoFueraDeHorario;
import com.cineclick.model.ResultadoValidacion;
import com.cineclick.model.Provincia;
import com.cineclick.model.TipoSala;
import com.cineclick.repository.ButacaRepository;
import com.cineclick.repository.ClienteRepository;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.repository.UsuarioRepository;
import com.cineclick.service.GeneradorCodigos;
import com.cineclick.service.MetricasService;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
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
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTests {

    @LocalServerPort
    private int port;

    @TestConfiguration
    static class RelojDeLosTests {
        @Bean
        @Primary
        RelojDePrueba relojDePrueba() {
            return new RelojDePrueba(MetricasService.ZONA);
        }
    }

    @Autowired
    private RelojDePrueba reloj;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Long adminId;

    // El cliente por defecto no soporta PATCH (lo usamos para cancelar compras)
    private final TestRestTemplate restTemplate = new TestRestTemplate(
        new RestTemplateBuilder()
            .requestFactory(() -> new JdkClientHttpRequestFactory())
            .additionalInterceptors((pedido, cuerpo, ejecucion) -> {
                if (!pedido.getHeaders().containsKey("X-Usuario-Id")) {
                    pedido.getHeaders().set("X-Usuario-Id", String.valueOf(idAdmin()));
                }
                return ejecucion.execute(pedido, cuerpo);
            })
    );

    private final TestRestTemplate sinSesion = new TestRestTemplate(
        new RestTemplateBuilder().requestFactory(() -> new JdkClientHttpRequestFactory())
    );

    @AfterEach
    void volverALaHoraReal() {
        reloj.usarHoraReal();
    }

    @Test
    void validarDentroDeLaVentanaMarcaLaEntradaYLaSegundaVezDaYaUsada() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = codigoDeEntrada(venderEntradas(fx, fx.funcionGrande(), 0, 1, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA)));
        LocalDateTime inicio = fx.sabado().atTime(18, 0);

        reloj.fijar(inicio.minusMinutes(15));
        ValidacionEntradaResponseDTO primera = validar(codigo.toLowerCase(), fx.cineId());
        assertThat(primera.resultado()).isEqualTo(ResultadoValidacion.VALIDA);
        assertThat(primera.usadaEn()).isEqualTo(inicio.minusMinutes(15));
        assertThat(primera.salaNombre()).isNotBlank();
        assertThat(primera.tipo()).isEqualTo(TipoEntrada.GENERAL);

        reloj.fijar(inicio.minusMinutes(5));
        ValidacionEntradaResponseDTO segunda = validar(codigo, fx.cineId());
        assertThat(segunda.resultado()).isEqualTo(ResultadoValidacion.YA_USADA);
        assertThat(segunda.usadaEn()).isEqualTo(inicio.minusMinutes(15));
    }

    @Test
    void dosValidacionesSimultaneasDanUnaSolaValida() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = codigoDeEntrada(venderEntradas(fx, fx.funcionGrande(), 0, 1, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA)));
        reloj.fijar(fx.sabado().atTime(17, 45));

        List<CompletableFuture<ValidacionEntradaResponseDTO>> pedidos = List.of(
            CompletableFuture.supplyAsync(() -> validar(codigo, fx.cineId())),
            CompletableFuture.supplyAsync(() -> validar(codigo, fx.cineId())),
            CompletableFuture.supplyAsync(() -> validar(codigo, fx.cineId()))
        );
        List<ResultadoValidacion> resultados = pedidos.stream().map(CompletableFuture::join).map(ValidacionEntradaResponseDTO::resultado).toList();

        assertThat(resultados).containsOnlyOnce(ResultadoValidacion.VALIDA);
        assertThat(resultados).filteredOn(r -> r == ResultadoValidacion.YA_USADA).hasSize(2);
    }

    @Test
    void unaEntradaDeOtroCineNoSeMarca() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = codigoDeEntrada(venderEntradas(fx, fx.funcionGrande(), 0, 1, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA)));
        reloj.fijar(fx.sabado().atTime(17, 30));

        ValidacionEntradaResponseDTO otroCine = validar(codigo, fx.cineId() + 1000);
        assertThat(otroCine.resultado()).isEqualTo(ResultadoValidacion.OTRO_CINE);
        assertThat(otroCine.cineNombre()).isNotBlank();
        assertThat(validar(codigo, fx.cineId()).resultado()).isEqualTo(ResultadoValidacion.VALIDA);
    }

    @Test
    void fueraDelHorarioDeIngresoDaElMotivoYNoMarca() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = codigoDeEntrada(venderEntradas(fx, fx.funcionGrande(), 0, 1, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA)));
        LocalDateTime inicio = fx.sabado().atTime(18, 0);

        reloj.fijar(inicio.minusMinutes(61));
        ValidacionEntradaResponseDTO temprano = validar(codigo, fx.cineId());
        assertThat(temprano.resultado()).isEqualTo(ResultadoValidacion.FUERA_DE_HORARIO);
        assertThat(temprano.motivo()).isEqualTo(MotivoFueraDeHorario.DEMASIADO_TEMPRANO);
        assertThat(temprano.ingresoDesde()).isEqualTo(inicio.minusMinutes(60));

        reloj.fijar(inicio.plusMinutes(31));
        assertThat(validar(codigo, fx.cineId()).motivo()).isEqualTo(MotivoFueraDeHorario.FUNCION_TERMINADA);

        reloj.fijar(fx.sabado().minusDays(1).atTime(LocalTime.of(18, 0)));
        assertThat(validar(codigo, fx.cineId()).motivo()).isEqualTo(MotivoFueraDeHorario.OTRO_DIA);

        reloj.fijar(inicio.plusMinutes(30));
        assertThat(validar(codigo, fx.cineId()).resultado()).isEqualTo(ResultadoValidacion.VALIDA);
    }

    @Test
    void unaEntradaDeUnaCompraCanceladaEstaAnuladaYUnCodigoInexistenteDa404() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = codigoDeEntrada(venderEntradas(fx, fx.funcionGrande(), 0, 1, EstadoCompra.CANCELADA, LocalDateTime.now(MetricasService.ZONA)));
        reloj.fijar(fx.sabado().atTime(17, 30));

        assertThat(validar(codigo, fx.cineId()).resultado()).isEqualTo(ResultadoValidacion.ANULADA);
        assertThat(restTemplate.postForEntity(url("/api/entradas/ZZZZ9999/validar"), new ValidarEntradaRequestDTO(fx.cineId()), String.class).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unHorarioQueNoTerminaEnCeroOCincoSeRechaza() {
        FixtureMetricas fx = fixtureMetricas();
        ResponseEntity<ErrorResponseDTO> respuesta = restTemplate.postForEntity(url("/api/funciones"),
            new FuncionRequestDTO(fx.peliculaId(), fx.salaGrandeId(), fx.sabado().atTime(21, 32), new BigDecimal("1000"), FormatoFuncion.DOS_D, Idioma.CASTELLANO),
            ErrorResponseDTO.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().detalles()).anyMatch(d -> d.contains("El horario tiene que terminar en 0 o 5 (por ejemplo, 19:30 o 22:15)"));
    }

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

    private record FixtureMetricas(Long cineId, Long funcionGrande, Long funcionChica, Long clienteId, LocalDate sabado,
                                   Long peliculaId, Long salaGrandeId, String email) {
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
        assertThat(sinSesion.getForEntity(url(ruta), String.class).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.exchange(url(ruta), HttpMethod.GET, conUsuario(idAdmin()), String.class).getStatusCode())
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    void lasContrasenasSeGuardanConHashYElLoginLasCompara() {
        String email = "hash-" + UUID.randomUUID() + "@mail.com";
        crearCliente(email);

        String guardada = usuarioRepository.findByEmailIgnoreCase(email).orElseThrow().getPassword();
        assertThat(guardada).startsWith("$2").doesNotContain("1234");
        assertThat(restTemplate.postForEntity(url("/api/auth/login"), new LoginRequestDTO(email, "1234"), LoginResponseDTO.class).getStatusCode())
            .isEqualTo(HttpStatus.OK);
        assertThat(restTemplate.postForEntity(url("/api/auth/login"), new LoginRequestDTO(email, "12345"), String.class).getStatusCode())
            .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void losEndpointsDeAdministracionRechazanAQuienNoEsAdmin() {
        ClienteResponseDTO cliente = crearCliente("no-admin-" + UUID.randomUUID() + "@mail.com");
        HttpHeaders comoCliente = new HttpHeaders();
        comoCliente.setContentType(MediaType.APPLICATION_JSON);
        comoCliente.set("X-Usuario-Id", String.valueOf(cliente.usuarioId()));
        HttpHeaders anonimo = new HttpHeaders();
        anonimo.setContentType(MediaType.APPLICATION_JSON);
        LocalDate hoy = LocalDate.now(MetricasService.ZONA);
        List<Map.Entry<HttpMethod, String>> endpoints = List.of(
            Map.entry(HttpMethod.POST, "/api/cines"),
            Map.entry(HttpMethod.PATCH, "/api/salas/999999/estado"),
            Map.entry(HttpMethod.POST, "/api/peliculas"),
            Map.entry(HttpMethod.POST, "/api/funciones"),
            Map.entry(HttpMethod.POST, "/api/promociones"),
            Map.entry(HttpMethod.GET, "/api/admin/compras"),
            Map.entry(HttpMethod.POST, "/api/entradas/ABCDEFGH/validar"),
            Map.entry(HttpMethod.GET, "/api/admin/metricas?desde=" + hoy + "&hasta=" + hoy),
            Map.entry(HttpMethod.GET, "/api/admin/arrepentimientos"),
            Map.entry(HttpMethod.GET, "/api/admin/soporte")
        );
        for (Map.Entry<HttpMethod, String> endpoint : endpoints) {
            for (HttpHeaders headers : List.of(comoCliente, anonimo)) {
                ResponseEntity<String> respuesta = sinSesion.exchange(
                    url(endpoint.getValue()), endpoint.getKey(), new HttpEntity<>("{}", headers), String.class
                );
                assertThat(respuesta.getStatusCode()).as(endpoint.getKey() + " " + endpoint.getValue()).isEqualTo(HttpStatus.FORBIDDEN);
            }
        }
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
        String email = "metricas-" + sufijo + "@mail.com";
        ClienteResponseDTO cliente = crearCliente(email);
        return new FixtureMetricas(cine.id(), funcionGrande, funcionChica, cliente.id(), sabado, pelicula.id(), grande.id(), email);
    }

    // ---------- Arrepentimiento ----------

    @Test
    void arrepentimientoEnPlazoAnulaLasEntradasYLiberaLasButacas() {
        FixtureMetricas fx = fixtureMetricas();
        Long compraId = venderEntradas(fx, fx.funcionGrande(), 0, 2, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA).minusHours(1));
        CompraResponseDTO antes = restTemplate.getForObject(url("/api/compras/" + compraId), CompraResponseDTO.class);
        String codigoEntrada = antes.entradas().get(0).codigoQr();

        ResponseEntity<ArrepentimientoResponseDTO> respuesta = restTemplate.postForEntity(url("/api/arrepentimientos"),
            new ArrepentimientoRequestDTO(fx.email().toUpperCase(), codigoEntrada.substring(0, 4) + "-" + codigoEntrada.substring(4).toLowerCase(),
                "Ana Prueba", MotivoArrepentimiento.NO_PUEDO_ASISTIR, true),
            ArrepentimientoResponseDTO.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody().estado()).isEqualTo(EstadoArrepentimiento.APROBADA);
        assertThat(respuesta.getBody().codigo()).matches("ARR-[0-9A-NP-Z]{8}");
        CompraResponseDTO despues = restTemplate.getForObject(url("/api/compras/" + compraId), CompraResponseDTO.class);
        assertThat(despues.estado()).isEqualTo(EstadoCompra.ARREPENTIDA);
        assertThat(despues.entradas()).allMatch(e -> e.estado() == EstadoEntrada.CANCELADA);
        List<String> butacas = antes.entradas().stream().map(e -> e.codigoButaca()).toList();
        ButacaResponseDTO[] mapa = restTemplate.getForObject(url("/api/funciones/" + fx.funcionGrande() + "/butacas-disponibles"), ButacaResponseDTO[].class);
        assertThat(mapa).filteredOn(b -> butacas.contains(b.codigo())).hasSize(2).allMatch(ButacaResponseDTO::disponible);
    }

    @Test
    void arrepentimientoFueraDePlazoQuedaParaRevisionSinAnular() {
        FixtureMetricas fx = fixtureMetricas();
        Long compraId = venderEntradas(fx, fx.funcionGrande(), 0, 1, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA).minusDays(11));

        ArrepentimientoResponseDTO solicitud = restTemplate.postForObject(url("/api/arrepentimientos"),
            new ArrepentimientoRequestDTO(fx.email(), String.valueOf(compraId), "Ana Prueba", null, true), ArrepentimientoResponseDTO.class);

        assertThat(solicitud.estado()).isEqualTo(EstadoArrepentimiento.FUERA_DE_PLAZO);
        assertThat(solicitud.motivoFueraDePlazo()).contains("10 días");
        CompraResponseDTO compra = restTemplate.getForObject(url("/api/compras/" + compraId), CompraResponseDTO.class);
        assertThat(compra.estado()).isEqualTo(EstadoCompra.CONFIRMADA);
        assertThat(compra.entradas()).allMatch(e -> e.estado() == EstadoEntrada.EMITIDA);
    }

    @Test
    void dosSolicitudesDeArrepentimientoDevuelvenElMismoCodigo() {
        FixtureMetricas fx = fixtureMetricas();
        Long compraId = venderEntradas(fx, fx.funcionGrande(), 0, 1, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA).minusHours(2));
        ArrepentimientoRequestDTO pedido = new ArrepentimientoRequestDTO(fx.email(), String.valueOf(compraId), "Ana Prueba", null, true);

        ArrepentimientoResponseDTO primera = restTemplate.postForObject(url("/api/arrepentimientos"), pedido, ArrepentimientoResponseDTO.class);
        ArrepentimientoResponseDTO segunda = restTemplate.postForObject(url("/api/arrepentimientos"), pedido, ArrepentimientoResponseDTO.class);

        assertThat(segunda.codigo()).isEqualTo(primera.codigo());
        assertThat(segunda.id()).isEqualTo(primera.id());
    }

    @Test
    void laBusquedaDeCompraNoRevelaSiElEmailExiste() {
        FixtureMetricas fx = fixtureMetricas();
        Long compraId = venderEntradas(fx, fx.funcionGrande(), 0, 1, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA).minusHours(2));

        ResponseEntity<ErrorResponseDTO> emailInexistente = restTemplate.getForEntity(
            url("/api/compras/buscar?email=nadie@mail.com&codigo=" + compraId), ErrorResponseDTO.class);
        ResponseEntity<ErrorResponseDTO> codigoInexistente = restTemplate.getForEntity(
            url("/api/compras/buscar?email=" + fx.email() + "&codigo=ZZZZ9999"), ErrorResponseDTO.class);
        ResponseEntity<ResumenCompraDTO> correcta = restTemplate.getForEntity(
            url("/api/compras/buscar?email=" + fx.email() + "&codigo=" + compraId), ResumenCompraDTO.class);

        assertThat(emailInexistente.getStatusCode()).isEqualTo(codigoInexistente.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(emailInexistente.getBody().mensaje()).isEqualTo(codigoInexistente.getBody().mensaje());
        assertThat(correcta.getBody().compraId()).isEqualTo(compraId);
        assertThat(correcta.getBody().elegibleArrepentimiento()).isTrue();
    }

    // ---------- Doble funcion ----------

    @Test
    void combinablesRespetanElMargenYDescartanMismaPeliculaOtroCineYAgotadas() {
        FixtureMetricas fx = fixtureMetricas();
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        Long salaB = crearSala(fx.cineId(), "B", 3, 3, 3);
        Long salaC = crearSala(fx.cineId(), "C", 4, 3, 3);
        Long salaUnica = crearSala(fx.cineId(), "Unica", 5, 1, 1);
        Long otra = crearPelicula("Otra " + sufijo);
        Long tercera = crearPelicula("Tercera " + sufijo);
        LocalDate sabado = fx.sabado();
        Long elegida = crearFuncion(fx.peliculaId(), fx.salaGrandeId(), sabado.atTime(20, 30));
        Long terminaCincoAntes = crearFuncion(otra, salaB, sabado.atTime(18, 30));
        Long terminaVeinteAntes = crearFuncion(tercera, salaC, sabado.atTime(18, 15));
        Long mismaPelicula = crearFuncion(fx.peliculaId(), salaC, sabado.atTime(23, 0));
        Long agotada = crearFuncion(otra, salaUnica, sabado.atTime(22, 45));
        venderEntradas(fx, agotada, 0, 1, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA));
        Long despues = crearFuncion(tercera, salaB, sabado.atTime(23, 10));
        FixtureMetricas otroCine = fixtureMetricas();
        Long deOtroCine = crearFuncion(otra, otroCine.salaGrandeId(), sabado.atTime(23, 0));

        CombinablesResponseDTO combinables = restTemplate.getForObject(url("/api/funciones/" + elegida + "/combinables"), CombinablesResponseDTO.class);

        assertThat(combinables.elegida().finEstimado()).isEqualTo(sabado.atTime(22, 25));
        assertThat(combinables.antes()).extracting(CombinablesResponseDTO.Combinable::funcionId).containsExactly(terminaVeinteAntes);
        assertThat(combinables.antes().get(0).minutosEntre()).isEqualTo(20);
        assertThat(combinables.despues()).extracting(CombinablesResponseDTO.Combinable::funcionId).containsExactly(despues);
        assertThat(combinables.antes()).extracting(CombinablesResponseDTO.Combinable::funcionId).doesNotContain(terminaCincoAntes);
        assertThat(combinables.despues()).extracting(CombinablesResponseDTO.Combinable::funcionId).doesNotContain(mismaPelicula, agotada, deOtroCine);
    }

    // ---------- Reglas de promociones ----------

    @Test
    void promocionDeLunesNoValeParaUnaFuncionDelSabado() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = crearPromocion(new BigDecimal("10"), Set.of(DayOfWeek.MONDAY), null, null, false, null, null);
        CompraResponseDTO compra = reservar(fx.clienteId(), fx.funcionGrande(), List.of(TipoEntrada.GENERAL));

        ResponseEntity<ErrorResponseDTO> respuesta = aplicarPromocion(compra.id(), codigo);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().mensaje()).isEqualTo("Esta promoción vale los lunes, y tu función es el sábado");
    }

    @Test
    void promocionDeLunesValeParaUnaFuncionDelLunesAunqueSeCompreOtroDia() {
        FixtureMetricas fx = fixtureMetricas();
        LocalDate lunes = fx.sabado().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Long funcionLunes = crearFuncion(fx.peliculaId(), fx.salaGrandeId(), lunes.atTime(18, 0));
        String codigo = crearPromocion(new BigDecimal("10"), Set.of(DayOfWeek.MONDAY), null, null, false, null, null);
        CompraResponseDTO compra = reservar(fx.clienteId(), funcionLunes, List.of(TipoEntrada.GENERAL));

        ResponseEntity<CompraResponseDTO> respuesta = restTemplate.postForEntity(url("/api/compras/" + compra.id() + "/promocion"),
            new com.cineclick.dto.AplicarPromocionRequestDTO(codigo), CompraResponseDTO.class);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().total()).isEqualByComparingTo("900");
    }

    @Test
    void promocionDeUnUsoPorUsuarioRechazaLaSegundaCompra() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = crearPromocion(new BigDecimal("10"), Set.of(), null, 1, false, null, null);
        CompraResponseDTO primera = reservarConPromocion(fx.clienteId(), fx.funcionGrande(), List.of(TipoEntrada.GENERAL), codigo);
        assertThat(pagar(primera.id()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CompraResponseDTO segunda = reservar(fx.clienteId(), fx.funcionChica(), List.of(TipoEntrada.GENERAL));

        ResponseEntity<ErrorResponseDTO> respuesta = aplicarPromocion(segunda.id(), codigo);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().mensaje()).isEqualTo("Ya usaste esta promoción");
    }

    @Test
    void promocionDePrimeraCompraSoloCuentaComprasConfirmadas() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = crearPromocion(new BigDecimal("10"), Set.of(), null, null, true, null, null);
        venderEntradas(fx, fx.funcionGrande(), 0, 1, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA).minusDays(1));
        CompraResponseDTO conCompraPrevia = reservar(fx.clienteId(), fx.funcionChica(), List.of(TipoEntrada.GENERAL));
        ClienteResponseDTO nuevo = crearCliente("primera-" + UUID.randomUUID() + "@mail.com");
        venderEntradas(nuevo.id(), fx.funcionGrande(), 1, 1, EstadoCompra.CANCELADA, LocalDateTime.now(MetricasService.ZONA).minusDays(1));
        CompraResponseDTO soloCanceladas = reservar(nuevo.id(), fx.funcionChica(), List.of(TipoEntrada.GENERAL));

        ResponseEntity<ErrorResponseDTO> rechazada = aplicarPromocion(conCompraPrevia.id(), codigo);
        ResponseEntity<String> aceptada = restTemplate.postForEntity(url("/api/compras/" + soloCanceladas.id() + "/promocion"),
            new com.cineclick.dto.AplicarPromocionRequestDTO(codigo), String.class);

        assertThat(rechazada.getBody().mensaje()).isEqualTo("Esta promoción es solo para tu primera compra");
        assertThat(aceptada.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void conTopeDeUnUsoDosConfirmacionesConcurrentesSoloUnaLlevaElDescuento() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = crearPromocion(new BigDecimal("10"), Set.of(), 1, null, false, null, null);
        ClienteResponseDTO otro = crearCliente("concurrente-" + UUID.randomUUID() + "@mail.com");
        CompraResponseDTO a = reservarConPromocion(fx.clienteId(), fx.funcionGrande(), List.of(TipoEntrada.GENERAL), codigo);
        CompraResponseDTO b = reservarConPromocion(otro.id(), fx.funcionChica(), List.of(TipoEntrada.GENERAL), codigo);

        CompletableFuture<ResponseEntity<String>> pagoA = CompletableFuture.supplyAsync(() -> pagar(a.id()));
        CompletableFuture<ResponseEntity<String>> pagoB = CompletableFuture.supplyAsync(() -> pagar(b.id()));
        List<HttpStatus> estados = List.of((HttpStatus) pagoA.join().getStatusCode(), (HttpStatus) pagoB.join().getStatusCode());

        assertThat(estados).containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.BAD_REQUEST);
        List<CompraResponseDTO> compras = List.of(
            restTemplate.getForObject(url("/api/compras/" + a.id()), CompraResponseDTO.class),
            restTemplate.getForObject(url("/api/compras/" + b.id()), CompraResponseDTO.class)
        );
        assertThat(compras).filteredOn(c -> codigo.equals(c.codigoPromocion())).hasSize(1).allMatch(c -> c.estado() == EstadoCompra.CONFIRMADA);
        assertThat(compras).filteredOn(c -> c.codigoPromocion() == null).hasSize(1)
            .allMatch(c -> c.estado() == EstadoCompra.PENDIENTE_PAGO && c.total().compareTo(new BigDecimal("1000")) == 0);
        assertThat(promocion(codigo).usosActuales()).isEqualTo(1);
    }

    @Test
    void elDescuentoSeAplicaSoloALaEntradaDeMayorPrecio() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = crearPromocion(new BigDecimal("10"), Set.of(), null, null, false, 1, null);

        CompraResponseDTO compra = reservarConPromocion(fx.clienteId(), fx.funcionGrande(),
            List.of(TipoEntrada.MENOR, TipoEntrada.GENERAL, TipoEntrada.JUBILADO), codigo);

        assertThat(compra.subtotal()).isEqualByComparingTo("2300");
        assertThat(compra.total()).isEqualByComparingTo("2200");
        assertThat(compra.entradas()).filteredOn(e -> e.descuento().signum() > 0)
            .singleElement().matches(e -> e.tipo() == TipoEntrada.GENERAL && e.descuento().compareTo(new BigDecimal("100")) == 0);
    }

    @Test
    void conDescuentoTotalLaCompraSeConfirmaSinDatosDePago() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = crearPromocion(new BigDecimal("100"), Set.of(), null, null, false, 1, null);
        CompraResponseDTO dos = reservarConPromocion(fx.clienteId(), fx.funcionChica(), List.of(TipoEntrada.GENERAL, TipoEntrada.JUBILADO), codigo);
        assertThat(dos.total()).isEqualByComparingTo("600");
        CompraResponseDTO una = reservarConPromocion(fx.clienteId(), fx.funcionGrande(), List.of(TipoEntrada.GENERAL), codigo);
        assertThat(una.total()).isEqualByComparingTo("0");

        ResponseEntity<PagoResponseDTOConMetodo> pago = restTemplate.postForEntity(url("/api/compras/" + una.id() + "/pagos"),
            new PagoRequestDTO(null, null, null, true), PagoResponseDTOConMetodo.class);
        ResponseEntity<String> sinTarjeta = restTemplate.postForEntity(url("/api/compras/" + dos.id() + "/pagos"),
            new PagoRequestDTO(null, null, null, true), String.class);

        assertThat(pago.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(pago.getBody().estado()).isEqualTo("APROBADO");
        assertThat(pago.getBody().metodoPago()).isEqualTo("SIN_CARGO");
        assertThat(sinTarjeta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unArrepentimientoAprobadoDevuelveElUsoDeLaPromocion() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = crearPromocion(new BigDecimal("10"), Set.of(), 5, null, false, null, null);
        CompraResponseDTO compra = reservarConPromocion(fx.clienteId(), fx.funcionGrande(), List.of(TipoEntrada.GENERAL), codigo);
        pagar(compra.id());
        assertThat(promocion(codigo).usosActuales()).isEqualTo(1);

        restTemplate.postForObject(url("/api/arrepentimientos"),
            new ArrepentimientoRequestDTO(fx.email(), String.valueOf(compra.id()), "Ana Prueba", null, true), ArrepentimientoResponseDTO.class);

        assertThat(promocion(codigo).usosActuales()).isZero();
    }

    @Test
    void elAdminBuscaComprasConFiltrosYVeElDetalle() {
        FixtureMetricas fx = fixtureMetricas();
        String codigo = crearPromocion(new BigDecimal("10"), Set.of(), null, null, false, null, null);
        CompraResponseDTO conPromo = reservarConPromocion(fx.clienteId(), fx.funcionGrande(), List.of(TipoEntrada.GENERAL), codigo);
        pagar(conPromo.id());
        restTemplate.postForObject(url("/api/arrepentimientos"),
            new ArrepentimientoRequestDTO(fx.email(), String.valueOf(conPromo.id()), "Ana Prueba", null, true), ArrepentimientoResponseDTO.class);
        Long otra = venderEntradas(fx, fx.funcionChica(), 0, 2, EstadoCompra.CONFIRMADA, LocalDateTime.now(MetricasService.ZONA).minusHours(1));

        PaginaDTO<CompraAdminDTO> porEmail = buscarComprasAdmin("email=" + fx.email().toUpperCase());
        assertThat(porEmail.contenido()).extracting(CompraAdminDTO::id).contains(conPromo.id(), otra);

        String codigoEntrada = conPromo.entradas().get(0).codigoQr();
        PaginaDTO<CompraAdminDTO> porEntrada = buscarComprasAdmin("codigo=" + codigoEntrada.substring(0, 4) + "-" + codigoEntrada.substring(4));
        assertThat(porEntrada.contenido()).extracting(CompraAdminDTO::id).containsExactly(conPromo.id());

        PaginaDTO<CompraAdminDTO> arrepentidas = buscarComprasAdmin("cineId=" + fx.cineId() + "&estado=ARREPENTIDA");
        assertThat(arrepentidas.contenido()).extracting(CompraAdminDTO::id).containsExactly(conPromo.id());

        PaginaDTO<CompraAdminDTO> paginada = buscarComprasAdmin("cineId=" + fx.cineId() + "&tamano=1&pagina=1");
        assertThat(paginada.contenido()).hasSize(1);
        assertThat(paginada.totalElementos()).isEqualTo(2);

        DetalleCompraAdminDTO detalle = restTemplate.getForObject(url("/api/admin/compras/" + conPromo.id()), DetalleCompraAdminDTO.class);
        assertThat(detalle.promocion().codigo()).isEqualTo(codigo);
        assertThat(detalle.arrepentimiento()).isNotNull();
        assertThat(detalle.compra().pago()).isNotNull();
        assertThat(detalle.clienteEmail()).isEqualTo(fx.email());
    }

    private PaginaDTO<CompraAdminDTO> buscarComprasAdmin(String filtros) {
        ResponseEntity<PaginaDTO<CompraAdminDTO>> respuesta = restTemplate.exchange(
            url("/api/admin/compras?" + filtros), HttpMethod.GET, null, new ParameterizedTypeReference<>() { });
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody();
    }

    private record PagoResponseDTOConMetodo(String estado, String metodoPago) {
    }

    private String crearPromocion(BigDecimal porcentaje, Set<DayOfWeek> dias, Integer usosTotales, Integer usosPorUsuario,
                                  boolean primeraCompra, Integer maxEntradas, BigDecimal montoMaximo) {
        String codigo = "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
        LocalDateTime ahora = LocalDateTime.now(MetricasService.ZONA);
        ResponseEntity<PromocionResponseDTO> respuesta = restTemplate.postForEntity(url("/api/promociones"), new PromocionRequestDTO(
            codigo, null, null, porcentaje, ahora.minusDays(1), ahora.plusDays(30), dias, usosTotales, usosPorUsuario,
            primeraCompra, maxEntradas, montoMaximo, true
        ), PromocionResponseDTO.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return codigo;
    }

    private PromocionResponseDTO promocion(String codigo) {
        PromocionResponseDTO[] todas = restTemplate.getForObject(url("/api/promociones"), PromocionResponseDTO[].class);
        return java.util.Arrays.stream(todas).filter(p -> p.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    private CompraResponseDTO reservar(Long clienteId, Long funcionId, List<TipoEntrada> tipos) {
        return reservarConPromocion(clienteId, funcionId, tipos, null);
    }

    private CompraResponseDTO reservarConPromocion(Long clienteId, Long funcionId, List<TipoEntrada> tipos, String codigo) {
        ButacaResponseDTO[] butacas = restTemplate.getForObject(url("/api/funciones/" + funcionId + "/butacas-disponibles"), ButacaResponseDTO[].class);
        List<Long> libres = java.util.Arrays.stream(butacas).filter(ButacaResponseDTO::disponible).map(ButacaResponseDTO::id).limit(tipos.size()).toList();
        List<EntradaSolicitadaDTO> entradas = tipos.stream().map(tipo -> new EntradaSolicitadaDTO(tipo, 1)).toList();
        ResponseEntity<CompraResponseDTO> respuesta = restTemplate.postForEntity(url("/api/compras"),
            new CompraRequestDTO(clienteId, funcionId, libres, entradas, codigo), CompraResponseDTO.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return respuesta.getBody();
    }

    private ResponseEntity<ErrorResponseDTO> aplicarPromocion(Long compraId, String codigo) {
        return restTemplate.postForEntity(url("/api/compras/" + compraId + "/promocion"),
            new com.cineclick.dto.AplicarPromocionRequestDTO(codigo), ErrorResponseDTO.class);
    }

    private ResponseEntity<String> pagar(Long compraId) {
        return restTemplate.postForEntity(url("/api/compras/" + compraId + "/pagos"),
            new PagoRequestDTO(MetodoPago.TARJETA_CREDITO, "Visa", "4242", true), String.class);
    }

    private Long crearSala(Long cineId, String nombre, int numero, int filas, int butacasPorFila) {
        return restTemplate.postForEntity(url("/api/cines/" + cineId + "/salas"),
            new SalaRequestDTO(nombre, numero, TipoSala.TRADICIONAL, filas, butacasPorFila, null), SalaResponseDTO.class).getBody().id();
    }

    private Long crearPelicula(String titulo) {
        return restTemplate.postForEntity(url("/api/peliculas"), new PeliculaRequestDTO(
            titulo, "Pelicula de prueba", 100, Clasificacion.ATP, Genero.DRAMA, Idioma.CASTELLANO, Set.of(FormatoFuncion.DOS_D), null, null
        ), PeliculaResponseDTO.class).getBody().id();
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
    private Long venderEntradas(FixtureMetricas fx, Long funcionId, int desde, int cantidad, EstadoCompra estado, LocalDateTime fecha) {
        return venderEntradas(fx.clienteId(), funcionId, desde, cantidad, estado, fecha);
    }

    private Long venderEntradas(Long clienteId, Long funcionId, int desde, int cantidad, EstadoCompra estado, LocalDateTime fecha) {
        var funcion = funcionRepository.findById(funcionId).orElseThrow();
        List<Butaca> butacas = butacaRepository.findBySalaIdAndActivaTrue(funcion.getSala().getId());
        Compra compra = new Compra();
        compra.setCliente(clienteRepository.findById(clienteId).orElseThrow());
        compra.setFuncion(funcion);
        compra.setFechaCompra(fecha);
        compra.setEstado(estado);
        for (Butaca butaca : butacas.subList(desde, desde + cantidad)) {
            Entrada entrada = new Entrada();
            entrada.setButaca(butaca);
            entrada.setCodigoQr(GeneradorCodigos.nuevo("", codigo -> false));
            entrada.setPrecioUnitario(new BigDecimal("1000"));
            entrada.setTipo(TipoEntrada.GENERAL);
            if (estado == EstadoCompra.CANCELADA) {
                entrada.setEstado(EstadoEntrada.CANCELADA);
                entrada.setVigente(null);
            }
            compra.agregarEntrada(entrada);
        }
        compra.setTotal(new BigDecimal(1000L * cantidad));
        return compraRepository.save(compra).getId();
    }

    private String codigoDeEntrada(Long compraId) {
        return restTemplate.getForObject(url("/api/compras/" + compraId), CompraResponseDTO.class).entradas().get(0).codigoQr();
    }

    private ValidacionEntradaResponseDTO validar(String codigo, Long cineId) {
        ResponseEntity<ValidacionEntradaResponseDTO> respuesta = restTemplate.postForEntity(
            url("/api/entradas/" + codigo + "/validar"), new ValidarEntradaRequestDTO(cineId), ValidacionEntradaResponseDTO.class);
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return respuesta.getBody();
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
        if (adminId == null) {
            adminId = usuarioRepository.findByEmailIgnoreCase("admin@cineclick.com").orElseThrow().getId();
        }
        return adminId;
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
