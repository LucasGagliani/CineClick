package com.cineclick.config;

import com.cineclick.model.Butaca;
import com.cineclick.model.Cine;
import com.cineclick.model.EstadoFuncion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Funcion;
import com.cineclick.model.Genero;
import com.cineclick.model.Pelicula;
import com.cineclick.model.Promocion;
import com.cineclick.model.RolUsuario;
import com.cineclick.model.Sala;
import com.cineclick.model.TipoButaca;
import com.cineclick.model.TipoSala;
import com.cineclick.model.Usuario;
import com.cineclick.repository.CineRepository;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.repository.PeliculaRepository;
import com.cineclick.repository.PromocionRepository;
import com.cineclick.repository.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Component
public class DataSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PeliculaRepository peliculaRepository;
    private final CineRepository cineRepository;
    private final FuncionRepository funcionRepository;
    private final PromocionRepository promocionRepository;

    public DataSeeder(
        UsuarioRepository usuarioRepository,
        PeliculaRepository peliculaRepository,
        CineRepository cineRepository,
        FuncionRepository funcionRepository,
        PromocionRepository promocionRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.peliculaRepository = peliculaRepository;
        this.cineRepository = cineRepository;
        this.funcionRepository = funcionRepository;
        this.promocionRepository = promocionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        crearUsuariosDemo();
        if (peliculaRepository.count() == 0 && cineRepository.count() == 0) {
            crearDominioDemo();
        }
        if (promocionRepository.count() == 0) {
            crearPromocionesDemo();
        }
    }

    private void crearUsuariosDemo() {
        if (!usuarioRepository.existsByEmailIgnoreCase("admin@cineclick.com")) {
            Usuario admin = new Usuario();
            admin.setNombre("Admin");
            admin.setApellido("CineClick");
            admin.setEmail("admin@cineclick.com");
            admin.setPassword("admin123");
            admin.setRol(RolUsuario.ADMIN);
            usuarioRepository.save(admin);
        }
    }

    private void crearDominioDemo() {
        Pelicula interestelar = nuevaPelicula(
            "Interestelar",
            "Un grupo de exploradores viaja a traves de un agujero de gusano para buscar un nuevo hogar para la humanidad.",
            169,
            "+13",
            Genero.CIENCIA_FICCION,
            "Subtitulada",
            Set.of(FormatoFuncion.DOS_D, FormatoFuncion.IMAX),
            "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg"
        );
        Pelicula intensamente = nuevaPelicula(
            "Intensamente 2",
            "Riley crece y nuevas emociones llegan al centro de control.",
            96,
            "ATP",
            Genero.ANIMACION,
            "Castellano",
            Set.of(FormatoFuncion.DOS_D, FormatoFuncion.TRES_D),
            "https://image.tmdb.org/t/p/w500/vpnVM9B6NMmQpWeZvzLvDESb2QY.jpg"
        );
        peliculaRepository.save(interestelar);
        peliculaRepository.save(intensamente);

        Cine cine = new Cine();
        cine.setNombre("CineClick Abasto");
        cine.setDireccion("Av. Corrientes 3247");
        cine.setCiudad("Buenos Aires");
        cine.setTelefono("011-4000-1000");

        Sala sala1 = nuevaSala("Sala 1", 1, TipoSala.TRADICIONAL, 5, 8);
        Sala sala2 = nuevaSala("Sala IMAX", 2, TipoSala.IMAX, 4, 7);
        cine.agregarSala(sala1);
        cine.agregarSala(sala2);
        cineRepository.save(cine);

        crearFuncion(interestelar, sala2, LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(20, 30)), BigDecimal.valueOf(6500), FormatoFuncion.IMAX, "Subtitulada");
        crearFuncion(intensamente, sala1, LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(18, 0)), BigDecimal.valueOf(4200), FormatoFuncion.DOS_D, "Castellano");
        crearFuncion(intensamente, sala1, LocalDateTime.of(LocalDate.now().plusDays(2), LocalTime.of(16, 0)), BigDecimal.valueOf(4200), FormatoFuncion.TRES_D, "Castellano");
    }

    private Pelicula nuevaPelicula(
        String titulo,
        String sinopsis,
        Integer duracion,
        String clasificacion,
        Genero genero,
        String idioma,
        Set<FormatoFuncion> formatos,
        String urlPoster
    ) {
        Pelicula pelicula = new Pelicula();
        pelicula.setTitulo(titulo);
        pelicula.setSinopsis(sinopsis);
        pelicula.setDuracionMinutos(duracion);
        pelicula.setClasificacion(clasificacion);
        pelicula.setGenero(genero);
        pelicula.setIdioma(idioma);
        pelicula.setFormatosDisponibles(formatos);
        pelicula.setUrlPoster(urlPoster);
        pelicula.setFechaEstreno(LocalDate.now().minusMonths(1));
        return pelicula;
    }

    private Sala nuevaSala(String nombre, Integer numero, TipoSala tipo, int filas, int butacasPorFila) {
        Sala sala = new Sala();
        sala.setNombre(nombre);
        sala.setNumero(numero);
        sala.setTipo(tipo);
        for (int fila = 0; fila < filas; fila++) {
            for (int numeroButaca = 1; numeroButaca <= butacasPorFila; numeroButaca++) {
                Butaca butaca = new Butaca();
                butaca.setFila(String.valueOf((char) ('A' + fila)));
                butaca.setNumero(numeroButaca);
                butaca.setTipo(numeroButaca <= 2 ? TipoButaca.VIP : TipoButaca.NORMAL);
                sala.agregarButaca(butaca);
            }
        }
        return sala;
    }

    private void crearFuncion(Pelicula pelicula, Sala sala, LocalDateTime inicio, BigDecimal precio, FormatoFuncion formato, String idioma) {
        Funcion funcion = new Funcion();
        funcion.setPelicula(pelicula);
        funcion.setSala(sala);
        funcion.setFechaHoraInicio(inicio);
        funcion.setPrecioBase(precio);
        funcion.setFormato(formato);
        funcion.setIdioma(idioma);
        funcion.setEstado(EstadoFuncion.PROGRAMADA);
        funcion.calcularHorarioFin();
        funcionRepository.save(funcion);
    }

    private void crearPromocionesDemo() {
        Promocion promo = new Promocion();
        promo.setCodigo("ESTRENO10");
        promo.setDescripcion("10% de descuento en compras online");
        promo.setPorcentajeDescuento(BigDecimal.valueOf(10));
        promo.setFechaInicio(LocalDate.now().minusDays(1));
        promo.setFechaFin(LocalDate.now().plusMonths(2));
        promocionRepository.save(promo);
    }
}
