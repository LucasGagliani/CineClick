package com.cineclick.config;

import com.cineclick.model.EstadoFuncion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Funcion;
import com.cineclick.model.Genero;
import com.cineclick.model.Idioma;
import com.cineclick.model.Pelicula;
import com.cineclick.model.Sala;
import com.cineclick.model.TipoSala;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.repository.PeliculaRepository;
import com.cineclick.repository.SalaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class ProgramadorDeFunciones {

    private static final int MINUTOS_PUBLICIDAD = 15;
    private static final int MAXIMO_POR_DIA = 5;
    private static final LocalTime ULTIMA_ENTRE_SEMANA = LocalTime.of(23, 30);
    private static final LocalTime ULTIMA_FIN_DE_SEMANA = LocalTime.of(0, 30);

    private final FuncionRepository funcionRepository;
    private final SalaRepository salaRepository;
    private final PeliculaRepository peliculaRepository;

    @Value("${cineclick.funciones.minutos-limpieza:20}")
    private int minutosLimpieza;

    @Value("${cineclick.demo.semilla:20261101}")
    private long semilla;

    public List<Funcion> programar(LocalDate desde, LocalDate hasta) {
        List<Sala> salas = salaRepository.findAll().stream()
            .filter(sala -> sala.isActiva() && sala.getCine().isActivo())
            .sorted(Comparator.comparing(Sala::getId))
            .toList();
        List<Pelicula> peliculas = peliculaRepository.findByActivaTrue().stream()
            .sorted(Comparator.comparing(Pelicula::getId))
            .toList();
        Set<String> ocupados = funcionRepository.findAll().stream()
            .map(funcion -> funcion.getSala().getId() + "|" + funcion.getFechaHoraInicio().toLocalDate())
            .collect(Collectors.toSet());
        List<Funcion> nuevas = new ArrayList<>();
        for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
            for (Sala sala : salas) {
                if (!ocupados.contains(sala.getId() + "|" + fecha)) {
                    nuevas.addAll(programarDia(sala, fecha, peliculas));
                }
            }
        }
        return funcionRepository.saveAll(nuevas);
    }

    private List<Funcion> programarDia(Sala sala, LocalDate fecha, List<Pelicula> peliculas) {
        Random azar = new Random(mezclar(semilla ^ mezclar(sala.getId() * 31 + fecha.toEpochDay())));
        List<Pelicula> candidatas = peliculas.stream()
            .filter(pelicula -> estrenada(pelicula, fecha) && formatoPara(sala.getTipo(), pelicula) != null)
            .toList();
        List<Funcion> funciones = new ArrayList<>();
        if (candidatas.isEmpty()) {
            return funciones;
        }
        boolean finDeSemana = fecha.getDayOfWeek() == DayOfWeek.FRIDAY || fecha.getDayOfWeek() == DayOfWeek.SATURDAY;
        LocalDateTime limite = finDeSemana ? fecha.plusDays(1).atTime(ULTIMA_FIN_DE_SEMANA) : fecha.atTime(ULTIMA_ENTRE_SEMANA);
        LocalDateTime inicio = fecha.atTime(12, 30).plusMinutes(30L * azar.nextInt(5));
        Pelicula anterior = null;
        while (funciones.size() < MAXIMO_POR_DIA && !inicio.isAfter(limite)) {
            Pelicula pelicula = elegir(candidatas, fecha, anterior, azar);
            FormatoFuncion formato = formatoPara(sala.getTipo(), pelicula);
            Funcion funcion = new Funcion();
            funcion.setPelicula(pelicula);
            funcion.setSala(sala);
            funcion.setFechaHoraInicio(inicio);
            funcion.setFormato(formato);
            funcion.setIdioma(idiomaPara(pelicula, inicio, azar));
            funcion.setPrecioBase(precioPara(sala.getTipo(), formato));
            funcion.setEstado(EstadoFuncion.PROGRAMADA);
            funcion.calcularHorarioFin(minutosLimpieza);
            funciones.add(funcion);
            anterior = pelicula;
            LocalDateTime fin = inicio.plusMinutes(pelicula.getDuracionMinutos() + MINUTOS_PUBLICIDAD);
            inicio = redondearArribaADiez(fin.plusMinutes(15 + azar.nextInt(6)));
        }
        return funciones;
    }

    private static Pelicula elegir(List<Pelicula> candidatas, LocalDate fecha, Pelicula anterior, Random azar) {
        List<Pelicula> opciones = candidatas.size() > 1 && anterior != null
            ? candidatas.stream().filter(pelicula -> !pelicula.equals(anterior)).toList()
            : candidatas;
        double total = opciones.stream().mapToDouble(pelicula -> peso(pelicula, fecha)).sum();
        double tirada = azar.nextDouble() * total;
        for (Pelicula pelicula : opciones) {
            tirada -= peso(pelicula, fecha);
            if (tirada <= 0) {
                return pelicula;
            }
        }
        return opciones.get(opciones.size() - 1);
    }

    public static double peso(Pelicula pelicula, LocalDate fecha) {
        long dias = pelicula.getFechaEstreno() == null ? 365 : ChronoUnit.DAYS.between(pelicula.getFechaEstreno(), fecha);
        if (dias < 7) {
            return 6;
        }
        if (dias < 14) {
            return 4;
        }
        if (dias < 28) {
            return 2.5;
        }
        return dias < 45 ? 1.5 : 1;
    }

    private static boolean estrenada(Pelicula pelicula, LocalDate fecha) {
        return pelicula.getFechaEstreno() == null || !pelicula.getFechaEstreno().isAfter(fecha);
    }

    static FormatoFuncion formatoPara(TipoSala tipo, Pelicula pelicula) {
        Set<FormatoFuncion> formatos = pelicula.getFormatosDisponibles();
        FormatoFuncion formato = switch (tipo) {
            case IMAX -> FormatoFuncion.IMAX;
            case CUATRO_D -> FormatoFuncion.CUATRO_D;
            case TRES_D -> FormatoFuncion.TRES_D;
            case PREMIUM, TRADICIONAL -> FormatoFuncion.DOS_D;
        };
        return formatos.contains(formato) ? formato : null;
    }

    private static Idioma idiomaPara(Pelicula pelicula, LocalDateTime inicio, Random azar) {
        if (pelicula.getIdioma() == Idioma.CASTELLANO && pelicula.getGenero() != Genero.ANIMACION) {
            return Idioma.CASTELLANO;
        }
        boolean noche = !inicio.toLocalTime().isBefore(LocalTime.of(20, 0)) || inicio.toLocalTime().isBefore(LocalTime.of(4, 0));
        double castellano = pelicula.getGenero() == Genero.ANIMACION
            ? (noche ? 0.6 : 0.9)
            : (noche ? 0.1 : 0.3);
        return azar.nextDouble() < castellano ? Idioma.CASTELLANO : Idioma.SUBTITULADA;
    }

    static BigDecimal precioPara(TipoSala tipo, FormatoFuncion formato) {
        return BigDecimal.valueOf(switch (formato) {
            case IMAX -> 6500;
            case CUATRO_D -> 9000;
            case TRES_D -> 5200;
            case DOS_D -> tipo == TipoSala.PREMIUM ? 7800 : 4200;
        });
    }

    private static long mezclar(long valor) {
        long x = valor;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return x;
    }

    private static LocalDateTime redondearArribaADiez(LocalDateTime hora) {
        LocalDateTime sinSegundos = hora.withSecond(0).withNano(0);
        int resto = sinSegundos.getMinute() % 10;
        return resto == 0 ? sinSegundos : sinSegundos.plusMinutes(10 - resto);
    }
}
