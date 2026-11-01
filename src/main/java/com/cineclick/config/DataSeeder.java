package com.cineclick.config;

import com.cineclick.config.CatalogoDemo.CineDemo;
import com.cineclick.config.CatalogoDemo.PeliculaDemo;
import com.cineclick.config.CatalogoDemo.SalaDemo;
import com.cineclick.model.Cine;
import com.cineclick.model.Cliente;
import com.cineclick.model.EstadoFuncion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Funcion;
import com.cineclick.model.Pelicula;
import com.cineclick.model.PlanoSala;
import com.cineclick.model.Promocion;
import com.cineclick.model.RolUsuario;
import com.cineclick.model.Sala;
import com.cineclick.model.TipoSala;
import com.cineclick.model.Usuario;
import com.cineclick.repository.ButacaRepository;
import com.cineclick.repository.CineRepository;
import com.cineclick.repository.ClienteRepository;
import com.cineclick.repository.EntradaRepository;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.repository.PeliculaRepository;
import com.cineclick.repository.PromocionRepository;
import com.cineclick.repository.SalaRepository;
import com.cineclick.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Order(2)
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final int DIAS_PROGRAMADOS = 7;
    private static final LocalTime PRIMERA_FUNCION = LocalTime.of(13, 30);
    private static final LocalTime ULTIMA_FUNCION = LocalTime.of(23, 0);
    private static final int MINUTOS_ENTRE_FUNCIONES = 20;
    private static final String TITULO_PROMO_DEMO = "Estrenos online";
    private static final String CONDICIONES_PROMO_DEMO = "Para cualquier función y formato. Un código por compra, hasta 10 entradas.";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PeliculaRepository peliculaRepository;
    private final CineRepository cineRepository;
    private final FuncionRepository funcionRepository;
    private final PromocionRepository promocionRepository;
    private final SalaRepository salaRepository;
    private final ButacaRepository butacaRepository;
    private final EntradaRepository entradaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${cineclick.funciones.minutos-limpieza:20}")
    private int minutosLimpieza;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        quitarRestriccionViejaDeButacas();
        crearUsuariosDemo();
        if (peliculaRepository.count() == 0 && cineRepository.count() == 0) {
            crearCatalogoDemo();
        }
        if (promocionRepository.count() == 0) {
            crearPromocionesDemo();
        }
        completarPromocionDemo();
        completarImagenesDeCines();
        redistribuirSalasSinPlano();
        if (!funcionRepository.existsByEstadoAndFechaHoraInicioAfter(EstadoFuncion.PROGRAMADA, LocalDateTime.now())) {
            programarSemana(LocalDate.now());
        }
    }

    private void completarImagenesDeCines() {
        for (CineDemo datos : CatalogoDemo.CINES) {
            if (datos.imagen() == null) {
                continue;
            }
            cineRepository.findAll().stream()
                .filter(cine -> cine.getNombre().equals(datos.nombre()) && cine.getUrlImagen() == null)
                .forEach(cine -> cine.setUrlImagen(datos.imagen()));
        }
    }

    private void redistribuirSalasSinPlano() {
        for (Sala sala : salaRepository.findAll()) {
            if (butacaRepository.existsBySalaIdAndColumnaIsNotNull(sala.getId())
                || entradaRepository.existsByButacaSalaId(sala.getId())) {
                continue;
            }
            butacaRepository.borrarPorSala(sala.getId());
            butacaRepository.saveAll(PlanoSala.sugeridoPara(sala).crearButacas(sala));
        }
    }

    private void quitarRestriccionViejaDeButacas() {
        jdbcTemplate.execute("ALTER TABLE entradas DROP CONSTRAINT IF EXISTS uk_entrada_funcion_butaca");
    }

    private void crearUsuariosDemo() {
        if (!usuarioRepository.existsByEmailIgnoreCase("admin@cineclick.com")) {
            usuarioRepository.save(nuevoUsuario("Admin", "CineClick", "admin@cineclick.com", "admin123", RolUsuario.ADMIN));
        }
        if (!usuarioRepository.existsByEmailIgnoreCase("cliente@cineclick.com")) {
            Cliente cliente = new Cliente();
            cliente.setUsuario(nuevoUsuario("Ana", "Gómez", "cliente@cineclick.com", "cliente123", RolUsuario.CLIENTE));
            clienteRepository.save(cliente);
        }
    }

    private Usuario nuevoUsuario(String nombre, String apellido, String email, String password, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(email);
        usuario.setPassword(password);
        usuario.setRol(rol);
        return usuario;
    }

    private void crearCatalogoDemo() {
        for (CineDemo datos : CatalogoDemo.CINES) {
            Cine cine = new Cine();
            cine.setNombre(datos.nombre());
            cine.setProvincia(datos.provincia());
            cine.setLocalidad(datos.localidad());
            cine.setCalle(datos.calle());
            cine.setAltura(datos.altura());
            cine.setTelefono(datos.telefono());
            cine.setUrlImagen(datos.imagen());
            for (SalaDemo sala : datos.salas()) {
                cine.agregarSala(nuevaSala(sala));
            }
            cineRepository.save(cine);
        }
        for (PeliculaDemo datos : CatalogoDemo.PELICULAS) {
            peliculaRepository.save(nuevaPelicula(datos));
        }
        programarSemana(LocalDate.now());
        programarPreventas();
    }

    private Sala nuevaSala(SalaDemo datos) {
        Sala sala = new Sala();
        sala.setNombre(datos.nombre());
        sala.setNumero(datos.numero());
        sala.setTipo(datos.tipo());
        datos.plano().crearButacas(sala).forEach(sala::agregarButaca);
        return sala;
    }

    private Pelicula nuevaPelicula(PeliculaDemo datos) {
        Pelicula pelicula = new Pelicula();
        pelicula.setTitulo(datos.titulo());
        pelicula.setSinopsis(datos.sinopsis());
        pelicula.setDuracionMinutos(datos.duracion());
        pelicula.setClasificacion(datos.clasificacion());
        pelicula.setGenero(datos.genero());
        pelicula.setIdioma(datos.idioma());
        pelicula.setFormatosDisponibles(new HashSet<>(datos.formatos()));
        pelicula.setUrlPoster(datos.poster());
        pelicula.setFechaEstreno(datos.estreno());
        pelicula.setDestacada(datos.destacada());
        return pelicula;
    }

    private void programarSemana(LocalDate desde) {
        List<Sala> salas = salaRepository.findAll().stream()
            .filter(Sala::isActiva)
            .sorted(Comparator.comparing(Sala::getId))
            .toList();
        List<Pelicula> peliculas = peliculaRepository.findByActivaTrue().stream()
            .sorted(Comparator.comparing(Pelicula::getId))
            .toList();
        LocalDateTime ahora = LocalDateTime.now();
        for (int dia = 0; dia < DIAS_PROGRAMADOS; dia++) {
            LocalDate fecha = desde.plusDays(dia);
            for (int indiceSala = 0; indiceSala < salas.size(); indiceSala++) {
                Sala sala = salas.get(indiceSala);
                List<Pelicula> candidatas = peliculas.stream()
                    .filter(pelicula -> enCartelera(pelicula, fecha) && formatoPara(sala, pelicula, 0) != null)
                    .toList();
                if (candidatas.isEmpty()) {
                    continue;
                }
                LocalDateTime inicio = fecha.atTime(PRIMERA_FUNCION).plusMinutes(15L * indiceSala);
                for (int turno = 0; inicio.toLocalDate().equals(fecha) && !inicio.toLocalTime().isAfter(ULTIMA_FUNCION); turno++) {
                    Pelicula pelicula = candidatas.get((indiceSala + dia + turno) % candidatas.size());
                    FormatoFuncion formato = formatoPara(sala, pelicula, turno);
                    if (inicio.isAfter(ahora)) {
                        crearFuncion(pelicula, sala, inicio, formato);
                    }
                    inicio = redondearAlCuartoDeHora(inicio.plusMinutes(pelicula.getDuracionMinutos() + MINUTOS_ENTRE_FUNCIONES));
                }
            }
        }
    }

    private void programarPreventas() {
        LocalDate finDeLaSemana = LocalDate.now().plusDays(DIAS_PROGRAMADOS - 1);
        List<Sala> salas = salaRepository.findAll();
        for (PeliculaDemo datos : CatalogoDemo.PELICULAS) {
            if (!datos.preventa() || !datos.estreno().isAfter(finDeLaSemana)) {
                continue;
            }
            Pelicula pelicula = peliculaRepository.findByActivaTrue().stream()
                .filter(p -> p.getTitulo().equals(datos.titulo()))
                .findFirst()
                .orElseThrow();
            for (int dia = 0; dia < 2; dia++) {
                LocalDate fecha = datos.estreno().plusDays(dia);
                for (Sala sala : salas) {
                    FormatoFuncion formato = formatoPara(sala, pelicula, 0);
                    if (formato != null && sala.getTipo() != TipoSala.TRADICIONAL) {
                        crearFuncion(pelicula, sala, fecha.atTime(LocalTime.of(19, 30)), formato);
                        crearFuncion(pelicula, sala, fecha.atTime(LocalTime.of(22, 45)), formato);
                    }
                }
            }
        }
    }

    private static boolean enCartelera(Pelicula pelicula, LocalDate fecha) {
        return pelicula.getFechaEstreno() == null || !pelicula.getFechaEstreno().isAfter(fecha);
    }

    private static FormatoFuncion formatoPara(Sala sala, Pelicula pelicula, int turno) {
        Set<FormatoFuncion> formatos = pelicula.getFormatosDisponibles();
        return switch (sala.getTipo()) {
            case IMAX -> formatos.contains(FormatoFuncion.IMAX) ? FormatoFuncion.IMAX : null;
            case CUATRO_D -> formatos.contains(FormatoFuncion.CUATRO_D) ? FormatoFuncion.CUATRO_D : null;
            case PREMIUM -> formatos.contains(FormatoFuncion.DOS_D) ? FormatoFuncion.DOS_D : null;
            case TRADICIONAL -> {
                if (formatos.contains(FormatoFuncion.TRES_D) && turno % 2 == 1) {
                    yield FormatoFuncion.TRES_D;
                }
                yield formatos.contains(FormatoFuncion.DOS_D) ? FormatoFuncion.DOS_D : null;
            }
        };
    }

    private static BigDecimal precioPara(Sala sala, FormatoFuncion formato) {
        return BigDecimal.valueOf(switch (formato) {
            case IMAX -> 6500;
            case CUATRO_D -> 9000;
            case TRES_D -> 5200;
            case DOS_D -> sala.getTipo() == TipoSala.PREMIUM ? 7800 : 4200;
        });
    }

    private static LocalDateTime redondearAlCuartoDeHora(LocalDateTime hora) {
        int resto = hora.getMinute() % 15;
        return resto == 0 ? hora.withSecond(0).withNano(0) : hora.plusMinutes(15 - resto).withSecond(0).withNano(0);
    }

    private void crearFuncion(Pelicula pelicula, Sala sala, LocalDateTime inicio, FormatoFuncion formato) {
        Funcion funcion = new Funcion();
        funcion.setPelicula(pelicula);
        funcion.setSala(sala);
        funcion.setFechaHoraInicio(inicio);
        funcion.setPrecioBase(precioPara(sala, formato));
        funcion.setFormato(formato);
        funcion.setIdioma(pelicula.getIdioma());
        funcion.setEstado(EstadoFuncion.PROGRAMADA);
        funcion.calcularHorarioFin(minutosLimpieza);
        funcionRepository.save(funcion);
    }

    private void completarPromocionDemo() {
        promocionRepository.findByCodigoIgnoreCase("ESTRENO10")
            .filter(promo -> promo.getTitulo() == null)
            .ifPresent(promo -> {
                promo.setTitulo(TITULO_PROMO_DEMO);
                promo.setDescripcion(CONDICIONES_PROMO_DEMO);
            });
    }

    private void crearPromocionesDemo() {
        Promocion promo = new Promocion();
        promo.setCodigo("ESTRENO10");
        promo.setTitulo(TITULO_PROMO_DEMO);
        promo.setDescripcion(CONDICIONES_PROMO_DEMO);
        promo.setPorcentajeDescuento(BigDecimal.valueOf(10));
        promo.setFechaInicio(LocalDate.now().minusDays(1));
        promo.setFechaFin(LocalDate.now().plusMonths(2));
        promocionRepository.save(promo);
    }
}
