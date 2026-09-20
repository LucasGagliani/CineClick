package com.cineclick;

import com.cineclick.dto.ClienteRequestDTO;
import com.cineclick.dto.ClienteResponseDTO;
import com.cineclick.dto.ErrorResponseDTO;
import com.cineclick.dto.PeliculaRequestDTO;
import com.cineclick.dto.PeliculaResponseDTO;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Genero;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTests {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    void permiteConsultarRecursosConGet() {
        ResponseEntity<String> response = restTemplate.getForEntity(url("/api/peliculas"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Interestelar");
    }

    @Test
    void permiteCrearModificarYEliminarPelicula() {
        PeliculaRequestDTO crear = new PeliculaRequestDTO(
            "Matrix",
            "Un programador descubre que la realidad es una simulacion.",
            136,
            "+13",
            Genero.CIENCIA_FICCION,
            "Subtitulada",
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
        assertThat(response.getBody().mensaje()).isEqualTo("Datos de entrada invalidos");
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

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
