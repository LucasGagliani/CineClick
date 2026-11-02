package com.cineclick.config;

import com.cineclick.config.CatalogoDemo.CineDemo;
import com.cineclick.config.CatalogoDemo.PeliculaDemo;
import com.cineclick.config.CatalogoDemo.SalaDemo;
import com.cineclick.config.CatalogoDemo.UsuarioDemo;
import com.cineclick.model.Cine;
import com.cineclick.model.Cliente;
import com.cineclick.model.Funcion;
import com.cineclick.model.Pelicula;
import com.cineclick.model.Promocion;
import com.cineclick.model.RolUsuario;
import com.cineclick.model.Sala;
import com.cineclick.model.Usuario;
import com.cineclick.repository.CineRepository;
import com.cineclick.repository.ClienteRepository;
import com.cineclick.repository.PeliculaRepository;
import com.cineclick.repository.PromocionRepository;
import com.cineclick.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Component
@Order(2)
@Profile("demo")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final int DIAS_HACIA_ATRAS = 21;
    private static final int DIAS_HACIA_ADELANTE = 7;
    private static final int CLIENTES_GENERICOS = 60;

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PeliculaRepository peliculaRepository;
    private final CineRepository cineRepository;
    private final PromocionRepository promocionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProgramadorDeFunciones programador;
    private final GeneradorDeCompras generadorDeCompras;

    @Value("${cineclick.demo.semilla:20261101}")
    private long semilla;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        LocalDateTime ahora = LocalDateTime.now().withSecond(0).withNano(0);
        LocalDate hoy = ahora.toLocalDate();
        boolean baseVacia = cineRepository.count() == 0 && peliculaRepository.count() == 0;

        Usuario admin = usuarioPara(CatalogoDemo.ADMIN, RolUsuario.ADMIN);
        Cliente frecuente = clientePara(CatalogoDemo.FRECUENTE, ahora.minusDays(200));
        Cliente hoyALaNoche = clientePara(CatalogoDemo.HOY_A_LA_NOCHE, ahora.minusDays(40));
        clientePara(CatalogoDemo.SIN_COMPRAS, ahora.minusDays(1));
        Cliente ocasional = clientePara(CatalogoDemo.OCASIONAL, ahora.minusDays(90));
        Map<String, Promocion> promociones = crearPromocionesQueFalten(ahora);

        if (baseVacia) {
            crearCatalogo(hoy);
            List<Funcion> funciones = programador.programar(hoy.minusDays(DIAS_HACIA_ATRAS), hoy.plusDays(DIAS_HACIA_ADELANTE));
            List<Cliente> genericos = crearClientesGenericos(ahora);
            generadorDeCompras.generar(funciones, new GeneradorDeCompras.Clientes(frecuente, hoyALaNoche, ocasional, genericos), promociones, ahora);
            log.info("Demo: base sembrada con {} funciones y {} clientes (admin: {})", funciones.size(), genericos.size() + 4, admin.getEmail());
        } else {
            int nuevas = programador.programar(hoy, hoy.plusDays(DIAS_HACIA_ADELANTE)).size();
            if (nuevas > 0) {
                log.info("Demo: se programaron {} funciones para cubrir los proximos {} dias", nuevas, DIAS_HACIA_ADELANTE);
            }
        }
    }

    private Usuario usuarioPara(UsuarioDemo datos, RolUsuario rol) {
        return usuarioRepository.findByEmailIgnoreCase(datos.email())
            .orElseGet(() -> usuarioRepository.save(nuevoUsuario(datos.nombre(), datos.apellido(), datos.email(), passwordEncoder.encode(datos.password()), rol)));
    }

    private Cliente clientePara(UsuarioDemo datos, LocalDateTime registro) {
        return clienteRepository.findByUsuarioEmailIgnoreCase(datos.email()).orElseGet(() -> {
            Cliente cliente = new Cliente();
            cliente.setUsuario(nuevoUsuario(datos.nombre(), datos.apellido(), datos.email(), passwordEncoder.encode(datos.password()), RolUsuario.CLIENTE));
            cliente.setFechaRegistro(registro);
            return clienteRepository.save(cliente);
        });
    }

    private List<Cliente> crearClientesGenericos(LocalDateTime ahora) {
        Random azar = new Random(semilla + 1);
        String hash = passwordEncoder.encode(Long.toHexString(azar.nextLong()) + Long.toHexString(azar.nextLong()));
        List<Cliente> clientes = new ArrayList<>();
        for (int i = 0; i < CLIENTES_GENERICOS; i++) {
            String nombre = CatalogoDemo.NOMBRES.get(i % CatalogoDemo.NOMBRES.size());
            String apellido = CatalogoDemo.APELLIDOS.get((i * 7 + i / CatalogoDemo.NOMBRES.size()) % CatalogoDemo.APELLIDOS.size());
            String email = sinAcentos(nombre + "." + apellido) + (i + 1) + "@correo.example";
            Cliente cliente = new Cliente();
            cliente.setUsuario(nuevoUsuario(nombre, apellido, email, hash, RolUsuario.CLIENTE));
            cliente.setTelefono(azar.nextBoolean() ? "11" + (40000000 + azar.nextInt(59999999)) : null);
            cliente.setFechaRegistro(ahora.minusDays(1 + azar.nextInt(180)).minusMinutes(azar.nextInt(1440)));
            clientes.add(cliente);
        }
        return clienteRepository.saveAll(clientes);
    }

    private static Usuario nuevoUsuario(String nombre, String apellido, String email, String hash, RolUsuario rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setApellido(apellido);
        usuario.setEmail(email);
        usuario.setPassword(hash);
        usuario.setRol(rol);
        return usuario;
    }

    private void crearCatalogo(LocalDate hoy) {
        for (CineDemo datos : CatalogoDemo.CINES) {
            Cine cine = new Cine();
            cine.setNombre(datos.nombre());
            cine.setProvincia(datos.provincia());
            cine.setLocalidad(datos.localidad());
            cine.setCalle(datos.calle());
            cine.setAltura(datos.altura());
            cine.setTelefono(datos.telefono());
            cine.setUrlImagen(datos.imagen());
            for (SalaDemo salaDemo : datos.salas()) {
                Sala sala = new Sala();
                sala.setNombre(salaDemo.nombre());
                sala.setNumero(salaDemo.numero());
                sala.setTipo(salaDemo.tipo());
                salaDemo.plano().crearButacas(sala).forEach(sala::agregarButaca);
                cine.agregarSala(sala);
            }
            cineRepository.save(cine);
        }
        for (PeliculaDemo datos : CatalogoDemo.PELICULAS) {
            Pelicula pelicula = new Pelicula();
            pelicula.setTitulo(datos.titulo());
            pelicula.setSinopsis(datos.sinopsis());
            pelicula.setDuracionMinutos(datos.duracion());
            pelicula.setClasificacion(datos.clasificacion());
            pelicula.setGenero(datos.genero());
            pelicula.setIdioma(datos.idioma());
            pelicula.setFormatosDisponibles(new HashSet<>(datos.formatos()));
            pelicula.setUrlPoster(datos.poster());
            pelicula.setFechaEstreno(hoy.plusDays(datos.estrenoEnDias()));
            pelicula.setDestacada(datos.destacada());
            peliculaRepository.save(pelicula);
        }
    }

    private Map<String, Promocion> crearPromocionesQueFalten(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        List<Promocion> definidas = List.of(
            promocion("BIENVENIDA", "Tu primera entrada, gratis", "Para tu primera compra en CineClick.", 100,
                hoy.minusDays(60).atStartOfDay(), hoy.plusDays(120).atTime(23, 59, 59), Set.of(), null, 1, true, 1, null),
            promocion("LUNMIEVIE20", "Lunes, miércoles y viernes", null, 20,
                hoy.minusDays(30).atStartOfDay(), hoy.plusDays(60).atTime(23, 59, 59),
                Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), null, null, false, null, null),
            promocion("MIERCOLES50", "Miércoles a mitad de precio", null, 50,
                hoy.minusDays(30).atStartOfDay(), hoy.plusDays(60).atTime(23, 59, 59), Set.of(DayOfWeek.WEDNESDAY), null, null, false, 2, null),
            promocion("FUGAZ30", "Promo relámpago", "Por tiempo limitado o hasta agotar los 50 usos.", 30,
                ahora.minusHours(46), ahora.plusHours(20), Set.of(), 50, null, false, null, null),
            promocion("ESTRENO10", "Estrenos online", "Para cualquier función y formato.", 10,
                hoy.minusDays(30).atStartOfDay(), hoy.plusDays(60).atTime(23, 59, 59), Set.of(), null, null, false, null, null),
            promocion("PRIMAVERA15", "Primavera en el cine", null, 15,
                hoy.minusDays(21).atStartOfDay(), hoy.minusDays(8).atTime(23, 59, 59), Set.of(), null, null, false, null, null),
            promocion("CYBERCINE25", "Semana del cine online", null, 25,
                hoy.plusDays(7).atStartOfDay(), hoy.plusDays(13).atTime(23, 59, 59), Set.of(), null, null, false, null, new BigDecimal("6000")),
            promocion("PREESTRENO40", "Preestreno exclusivo", "Cupos limitados.", 40,
                hoy.minusDays(10).atStartOfDay(), hoy.plusDays(20).atTime(23, 59, 59), Set.of(), 15, null, false, null, null)
        );
        Map<String, Promocion> porCodigo = new LinkedHashMap<>();
        for (Promocion promo : definidas) {
            porCodigo.put(promo.getCodigo(), promocionRepository.findByCodigoIgnoreCase(promo.getCodigo()).orElseGet(() -> promocionRepository.save(promo)));
        }
        return porCodigo;
    }

    private static Promocion promocion(String codigo, String titulo, String descripcion, int porcentaje, LocalDateTime desde, LocalDateTime hasta,
                                       Set<DayOfWeek> dias, Integer usosTotales, Integer usosPorUsuario, boolean primeraCompra,
                                       Integer maxEntradas, BigDecimal montoMaximo) {
        Promocion promo = new Promocion();
        promo.setCodigo(codigo);
        promo.setTitulo(titulo);
        promo.setDescripcion(descripcion);
        promo.setPorcentajeDescuento(BigDecimal.valueOf(porcentaje));
        promo.setVigenciaDesde(desde);
        promo.setVigenciaHasta(hasta);
        promo.setDiasSemana(new HashSet<>(dias));
        promo.setUsosMaximosTotales(usosTotales);
        promo.setUsosMaximosPorUsuario(usosPorUsuario);
        promo.setSoloPrimeraCompra(primeraCompra);
        promo.setMaxEntradasConDescuento(maxEntradas);
        promo.setMontoMaximoDescuento(montoMaximo);
        return promo;
    }

    private static String sinAcentos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
