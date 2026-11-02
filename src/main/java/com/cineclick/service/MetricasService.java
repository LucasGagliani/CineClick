package com.cineclick.service;

import com.cineclick.dto.MetricasResponseDTO;
import com.cineclick.dto.MetricasResponseDTO.Asistencia;
import com.cineclick.dto.MetricasResponseDTO.CineMetricas;
import com.cineclick.dto.MetricasResponseDTO.Conversion;
import com.cineclick.dto.MetricasResponseDTO.Franja;
import com.cineclick.dto.MetricasResponseDTO.Indicador;
import com.cineclick.dto.MetricasResponseDTO.PeliculaRanking;
import com.cineclick.dto.MetricasResponseDTO.PromocionUso;
import com.cineclick.dto.MetricasResponseDTO.ProximaFuncion;
import com.cineclick.dto.MetricasResponseDTO.Resumen;
import com.cineclick.dto.MetricasResponseDTO.TipoEntradaVenta;
import com.cineclick.exception.CampoInvalidoException;
import com.cineclick.model.Compra;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.EstadoPago;
import com.cineclick.model.Funcion;
import com.cineclick.model.TipoEntrada;
import com.cineclick.repository.ButacaRepository;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.EntradaRepository;
import com.cineclick.repository.FuncionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Metricas del panel de administracion. Todo sale de consultas de agregacion (GROUP BY) sobre las
 * tablas existentes: una consulta por bloque, sin recorrer compras ni entradas una por una.
 *
 * Fechas: los cortes de dia y las franjas horarias usan America/Argentina/Buenos_Aires (la aplicacion
 * arranca con esa zona por defecto, asi las fechas guardadas con LocalDateTime.now() estan en hora
 * argentina). El rango incluye "desde" y "hasta" completos: [desde 00:00, hasta + 1 dia 00:00).
 */
@Service
@RequiredArgsConstructor
public class MetricasService {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final int TOP_PELICULAS = 10;

    private final CompraRepository compraRepository;
    private final EntradaRepository entradaRepository;
    private final FuncionRepository funcionRepository;
    private final ButacaRepository butacaRepository;

    private record Rango(LocalDateTime desde, LocalDateTime hasta) {
        static Rango deDias(LocalDate desde, LocalDate hasta) {
            return new Rango(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        }
    }

    private record VentaFuncion(long vendidas, long usadas) {
    }

    private record DatosFunciones(List<Funcion> funciones, Map<Long, VentaFuncion> ventas, Map<Long, Long> capacidades) {
        long vendidas(Funcion funcion) {
            VentaFuncion venta = ventas.get(funcion.getId());
            return venta == null ? 0 : venta.vendidas();
        }

        long usadas(Funcion funcion) {
            VentaFuncion venta = ventas.get(funcion.getId());
            return venta == null ? 0 : venta.usadas();
        }

        long capacidad(Funcion funcion) {
            return capacidades.getOrDefault(funcion.getSala().getId(), 0L);
        }
    }

    @Transactional(readOnly = true)
    public MetricasResponseDTO calcular(LocalDate desde, LocalDate hasta, Long cineId) {
        if (hasta.isBefore(desde)) {
            throw new CampoInvalidoException("hasta", "No puede ser anterior a la fecha de inicio");
        }
        // Periodo anterior: mismo largo, inmediatamente antes del rango pedido
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        LocalDate hastaAnterior = desde.minusDays(1);
        LocalDate desdeAnterior = desde.minusDays(dias);
        Rango actual = Rango.deDias(desde, hasta);
        Rango anterior = Rango.deDias(desdeAnterior, hastaAnterior);
        LocalDateTime ahora = LocalDateTime.now(ZONA);

        DatosFunciones funcionesActuales = datosDeFunciones(actual, cineId);
        DatosFunciones funcionesAnteriores = datosDeFunciones(anterior, cineId);

        return new MetricasResponseDTO(
            desde,
            hasta,
            desdeAnterior,
            hastaAnterior,
            resumen(actual, anterior, cineId, funcionesActuales, funcionesAnteriores),
            proximasFunciones(ahora, cineId),
            rankingPeliculas(actual, cineId),
            conversion(actual, cineId, ahora),
            cineId == null ? porCine(actual, funcionesActuales) : List.of(),
            franjas(funcionesActuales),
            promociones(actual, cineId),
            tiposEntrada(actual, cineId),
            asistencia(funcionesActuales, ahora)
        );
    }

    private Resumen resumen(Rango actual, Rango anterior, Long cineId, DatosFunciones funcionesActuales, DatosFunciones funcionesAnteriores) {
        Object[] ventas = primeraFila(compraRepository.resumenVentas(actual.desde(), actual.hasta(), cineId));
        Object[] ventasAnteriores = primeraFila(compraRepository.resumenVentas(anterior.desde(), anterior.hasta(), cineId));
        // Ingresos: suma de lo cobrado (total con descuento) de las ventas, por fecha de COMPRA.
        // Venta = compra CONFIRMADA (pagada); pendientes, vencidas y canceladas no cuentan.
        Indicador ingresos = new Indicador(decimal(ventas[1]), decimal(ventasAnteriores[1]));
        // Entradas vendidas: entradas de las ventas, por fecha de COMPRA
        Indicador entradas = new Indicador(
            BigDecimal.valueOf(entradaRepository.contarVendidas(actual.desde(), actual.hasta(), cineId)),
            BigDecimal.valueOf(entradaRepository.contarVendidas(anterior.desde(), anterior.hasta(), cineId))
        );
        // Ocupacion: por fecha de FUNCION; entradas vendidas / capacidad de esas funciones (ponderada)
        Indicador ocupacion = new Indicador(ocupacion(funcionesActuales, funcionesActuales.funciones()), ocupacion(funcionesAnteriores, funcionesAnteriores.funciones()));
        // Ticket promedio: ingresos / cantidad de compras (no de entradas)
        Indicador ticket = new Indicador(dividir(decimal(ventas[1]), entero(ventas[0])), dividir(decimal(ventasAnteriores[1]), entero(ventasAnteriores[0])));
        return new Resumen(ingresos, entradas, ocupacion, ticket);
    }

    // Proximas funciones: de ahora al final de manana (hora argentina); no dependen del rango, si del cine
    private List<ProximaFuncion> proximasFunciones(LocalDateTime ahora, Long cineId) {
        Rango proximas = new Rango(ahora, ahora.toLocalDate().plusDays(2).atStartOfDay());
        DatosFunciones datos = datosDeFunciones(proximas, cineId);
        return datos.funciones().stream()
            .map(funcion -> new ProximaFuncion(
                funcion.getId(),
                funcion.getPelicula().getTitulo(),
                funcion.getSala().getCine().getNombre(),
                funcion.getSala().getNombre(),
                funcion.getFechaHoraInicio(),
                datos.vendidas(funcion),
                datos.capacidad(funcion)
            ))
            .toList();
    }

    // Ranking: top 10 peliculas por entradas vendidas en el rango (por fecha de compra), con sus ingresos
    private List<PeliculaRanking> rankingPeliculas(Rango rango, Long cineId) {
        Map<Long, Long> entradas = aMapa(entradaRepository.vendidasPorPelicula(rango.desde(), rango.hasta(), cineId));
        return compraRepository.ingresosPorPelicula(rango.desde(), rango.hasta(), cineId).stream()
            .map(fila -> new PeliculaRanking((Long) fila[0], (String) fila[1], entradas.getOrDefault((Long) fila[0], 0L), decimal(fila[2])))
            .sorted(Comparator.comparingLong(PeliculaRanking::entradas).reversed().thenComparing(PeliculaRanking::ingresos, Comparator.reverseOrder()))
            .limit(TOP_PELICULAS)
            .toList();
    }

    // Conversion: reservas creadas en el rango (todas, por fecha de compra) y como terminaron.
    // - pagadas: CONFIRMADA
    // - canceladas: CANCELADA con el pago aprobado o ARREPENTIDA (se anulo despues de pagar)
    // - vencidas: CANCELADA sin pago aprobado (se vencio o se abandono) y las pendientes que ya pasaron
    //   los 8 minutos aunque la tarea de vencimiento todavia no las haya cancelado. El modelo no guarda
    //   el motivo de la cancelacion, asi que una reserva abandonada a mano tambien cuenta como vencida.
    // - pendientes: PENDIENTE_PAGO dentro de sus 8 minutos
    private Conversion conversion(Rango rango, Long cineId, LocalDateTime ahora) {
        long pagadas = 0;
        long canceladas = 0;
        long vencidas = 0;
        long pendientes = 0;
        for (Object[] fila : compraRepository.contarPorEstado(rango.desde(), rango.hasta(), cineId)) {
            EstadoCompra estado = (EstadoCompra) fila[0];
            EstadoPago pago = (EstadoPago) fila[1];
            long cantidad = entero(fila[2]);
            switch (estado) {
                case CONFIRMADA -> pagadas += cantidad;
                case CANCELADA -> {
                    if (pago == EstadoPago.APROBADO) {
                        canceladas += cantidad;
                    } else {
                        vencidas += cantidad;
                    }
                }
                case ARREPENTIDA -> canceladas += cantidad;
                case PENDIENTE_PAGO -> pendientes += cantidad;
            }
        }
        long pendientesVencidas = compraRepository.contarPendientesVencidas(
            rango.desde(), rango.hasta(), cineId, ahora.minusMinutes(Compra.MINUTOS_RESERVA)
        );
        pendientes -= pendientesVencidas;
        vencidas += pendientesVencidas;
        return new Conversion(pagadas + canceladas + vencidas + pendientes, pagadas, vencidas, canceladas, pendientes);
    }

    // Por cine (solo sin filtro de cine): ingresos y entradas por fecha de compra; ocupacion por fecha de funcion
    private List<CineMetricas> porCine(Rango rango, DatosFunciones funciones) {
        Map<Long, String> nombres = new LinkedHashMap<>();
        Map<Long, BigDecimal> ingresos = new HashMap<>();
        for (Object[] fila : compraRepository.ingresosPorCine(rango.desde(), rango.hasta())) {
            nombres.put((Long) fila[0], (String) fila[1]);
            ingresos.put((Long) fila[0], decimal(fila[2]));
        }
        Map<Long, Long> entradas = aMapa(entradaRepository.vendidasPorCine(rango.desde(), rango.hasta()));
        Map<Long, List<Funcion>> funcionesPorCine = funciones.funciones().stream()
            .collect(Collectors.groupingBy(funcion -> funcion.getSala().getCine().getId(), LinkedHashMap::new, Collectors.toList()));
        funcionesPorCine.forEach((cineId, lista) -> nombres.putIfAbsent(cineId, lista.get(0).getSala().getCine().getNombre()));
        return nombres.entrySet().stream()
            .map(cine -> new CineMetricas(
                cine.getKey(),
                cine.getValue(),
                ingresos.getOrDefault(cine.getKey(), BigDecimal.ZERO),
                entradas.getOrDefault(cine.getKey(), 0L),
                ocupacion(funciones, funcionesPorCine.getOrDefault(cine.getKey(), List.of()))
            ))
            .sorted(Comparator.comparing(CineMetricas::ingresos).reversed().thenComparing(CineMetricas::nombre))
            .toList();
    }

    // Franjas: entradas vendidas por dia de la semana (1 = lunes ... 7 = domingo) y hora de inicio de la
    // funcion, por fecha de FUNCION. Solo se devuelven las celdas con entradas.
    private List<Franja> franjas(DatosFunciones datos) {
        Map<Integer, Long> porCelda = new TreeMap<>();
        for (Funcion funcion : datos.funciones()) {
            long vendidas = datos.vendidas(funcion);
            if (vendidas > 0) {
                int celda = funcion.getFechaHoraInicio().getDayOfWeek().getValue() * 100 + funcion.getFechaHoraInicio().getHour();
                porCelda.merge(celda, vendidas, Long::sum);
            }
        }
        return porCelda.entrySet().stream()
            .map(celda -> new Franja(celda.getKey() / 100, celda.getKey() % 100, celda.getValue()))
            .toList();
    }

    // Promociones: compras vendidas con cada codigo (usos) y cuanto se desconto en total
    // (precio de lista de las entradas menos lo cobrado), por fecha de compra
    private List<PromocionUso> promociones(Rango rango, Long cineId) {
        Map<String, BigDecimal> precioDeLista = new HashMap<>();
        for (Object[] fila : entradaRepository.precioDeListaPorPromocion(rango.desde(), rango.hasta(), cineId)) {
            precioDeLista.put((String) fila[0], decimal(fila[1]));
        }
        return compraRepository.usosDePromociones(rango.desde(), rango.hasta(), cineId).stream()
            .map(fila -> new PromocionUso(
                (String) fila[0],
                entero(fila[1]),
                precioDeLista.getOrDefault((String) fila[0], BigDecimal.ZERO).subtract(decimal(fila[2])).max(BigDecimal.ZERO)
            ))
            .sorted(Comparator.comparingLong(PromocionUso::usos).reversed().thenComparing(PromocionUso::codigo))
            .toList();
    }

    // Tipos de entrada: cantidad vendida e ingresos por tipo, por fecha de compra. Los ingresos son el
    // precio de cada entrada (sin repartir el descuento de una promocion entre las entradas)
    private List<TipoEntradaVenta> tiposEntrada(Rango rango, Long cineId) {
        return entradaRepository.vendidasPorTipo(rango.desde(), rango.hasta(), cineId).stream()
            .map(fila -> {
                TipoEntrada tipo = fila[0] == null ? TipoEntrada.GENERAL : (TipoEntrada) fila[0];
                return new TipoEntradaVenta(tipo, tipo.getNombre(), entero(fila[1]), decimal(fila[2]));
            })
            .sorted(Comparator.comparingLong(TipoEntradaVenta::cantidad).reversed())
            .toList();
    }

    // Asistencia: en las funciones del rango que ya empezaron, entradas vendidas (emitidas) y cuantas
    // pasaron por el control de ingreso (usadas)
    private Asistencia asistencia(DatosFunciones datos, LocalDateTime ahora) {
        long emitidas = 0;
        long usadas = 0;
        for (Funcion funcion : datos.funciones()) {
            if (funcion.getFechaHoraInicio().isBefore(ahora)) {
                emitidas += datos.vendidas(funcion);
                usadas += datos.usadas(funcion);
            }
        }
        return new Asistencia(emitidas, usadas);
    }

    private DatosFunciones datosDeFunciones(Rango rango, Long cineId) {
        List<Funcion> funciones = funcionRepository.buscarParaMetricas(rango.desde(), rango.hasta(), cineId);
        Map<Long, VentaFuncion> ventas = new HashMap<>();
        for (Object[] fila : entradaRepository.vendidasYUsadasPorFuncion(rango.desde(), rango.hasta(), cineId)) {
            ventas.put((Long) fila[0], new VentaFuncion(entero(fila[1]), entero(fila[2])));
        }
        Set<Long> salas = funciones.stream().map(funcion -> funcion.getSala().getId()).collect(Collectors.toSet());
        Map<Long, Long> capacidades = salas.isEmpty() ? Map.of() : aMapa(butacaRepository.contarActivasPorSala(salas));
        return new DatosFunciones(funciones, ventas, capacidades);
    }

    // Promedio ponderado: suma de vendidas / suma de capacidades (no el promedio de los porcentajes)
    private BigDecimal ocupacion(DatosFunciones datos, List<Funcion> funciones) {
        long vendidas = funciones.stream().mapToLong(datos::vendidas).sum();
        long capacidad = funciones.stream().mapToLong(datos::capacidad).sum();
        return capacidad == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(vendidas).divide(BigDecimal.valueOf(capacidad), 4, RoundingMode.HALF_UP);
    }

    private static BigDecimal dividir(BigDecimal valor, long cantidad) {
        return cantidad == 0 ? BigDecimal.ZERO : valor.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }

    private static Object[] primeraFila(List<Object[]> filas) {
        return filas.isEmpty() ? new Object[] {0L, BigDecimal.ZERO} : filas.get(0);
    }

    private static Map<Long, Long> aMapa(List<Object[]> filas) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] fila : filas) {
            mapa.put((Long) fila[0], entero(fila[1]));
        }
        return mapa;
    }

    private static long entero(Object valor) {
        return valor == null ? 0 : ((Number) valor).longValue();
    }

    private static BigDecimal decimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor instanceof BigDecimal numero ? numero : new BigDecimal(Objects.toString(valor));
    }
}
