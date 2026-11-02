package com.cineclick.config;

import com.cineclick.model.Butaca;
import com.cineclick.model.Clasificacion;
import com.cineclick.model.Cliente;
import com.cineclick.model.Compra;
import com.cineclick.model.ConsultaSoporte;
import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoArrepentimiento;
import com.cineclick.model.EstadoCompra;
import com.cineclick.model.EstadoConsulta;
import com.cineclick.model.EstadoPago;
import com.cineclick.model.Funcion;
import com.cineclick.model.Genero;
import com.cineclick.model.MetodoPago;
import com.cineclick.model.MotivoArrepentimiento;
import com.cineclick.model.MotivoSoporte;
import com.cineclick.model.Pago;
import com.cineclick.model.Promocion;
import com.cineclick.model.SolicitudArrepentimiento;
import com.cineclick.model.TipoButaca;
import com.cineclick.model.TipoEntrada;
import com.cineclick.repository.ButacaRepository;
import com.cineclick.repository.CompraRepository;
import com.cineclick.repository.ConsultaSoporteRepository;
import com.cineclick.repository.PromocionRepository;
import com.cineclick.repository.SolicitudArrepentimientoRepository;
import com.cineclick.service.GeneradorCodigos;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class GeneradorDeCompras {

    private static final Logger log = LoggerFactory.getLogger(GeneradorDeCompras.class);
    private static final int COMPRAS_OBJETIVO = 500;
    private static final double PROBABILIDAD_CANCELADA = 0.2;
    private static final double PROBABILIDAD_ARREPENTIDA = 0.015;
    private static final double PROBABILIDAD_PROMO = 0.15;
    private static final int[] PESOS_CANTIDAD = {15, 40, 15, 18, 6, 6};
    private static final List<String> MARCAS = List.of("Visa", "Mastercard", "American Express");

    private final CompraRepository compraRepository;
    private final ButacaRepository butacaRepository;
    private final PromocionRepository promocionRepository;
    private final SolicitudArrepentimientoRepository arrepentimientoRepository;
    private final ConsultaSoporteRepository consultaSoporteRepository;

    @Value("${cineclick.demo.semilla:20261101}")
    private long semilla;

    private Random azar;
    private LocalDateTime ahora;
    private LocalDateTime inicioPeriodo;
    private final Map<Long, List<Butaca>> butacasPorSala = new HashMap<>();
    private final Map<Long, Set<Long>> ocupadas = new HashMap<>();
    private final Set<String> codigos = new HashSet<>();
    private final Map<Long, Integer> usos = new HashMap<>();
    private final List<Compra> compras = new ArrayList<>();
    private final List<SolicitudArrepentimiento> solicitudes = new ArrayList<>();

    public record Clientes(Cliente frecuente, Cliente hoyALaNoche, Cliente ocasional, List<Cliente> genericos) {
    }

    public void generar(List<Funcion> funciones, Clientes clientes, Map<String, Promocion> promociones, LocalDateTime momento) {
        azar = new Random(semilla);
        ahora = momento;
        inicioPeriodo = ahora.toLocalDate().minusDays(21).atStartOfDay();
        List<Funcion> todas = funciones.stream().sorted(Comparator.comparing(Funcion::getFechaHoraInicio).thenComparing(Funcion::getId)).toList();
        List<Cliente> bienvenida = clientes.genericos().subList(clientes.genericos().size() - 6, clientes.genericos().size());
        List<Cliente> pool = clientes.genericos().subList(0, clientes.genericos().size() - 6);

        comprasConPromocion(todas, pool, bienvenida, promociones);
        ocupacionDeHoyYManiana(todas, pool);
        comprasDeLosClientesDemo(todas, clientes);
        comprasGenerales(todas, pool, promociones);
        entradasDeHoyEnCadaCine(todas, pool);

        compraRepository.saveAll(compras);
        marcarUsadas();
        actualizarUsos(promociones);
        arrepentimientoRepository.saveAll(solicitudes);
        consultasDeSoporte(clientes);
        log.info("Demo: {} compras ({} confirmadas, {} canceladas, {} arrepentidas)", compras.size(),
            contar(EstadoCompra.CONFIRMADA), contar(EstadoCompra.CANCELADA), contar(EstadoCompra.ARREPENTIDA));
    }

    private void comprasConPromocion(List<Funcion> funciones, List<Cliente> pool, List<Cliente> bienvenida, Map<String, Promocion> promociones) {
        repetirConPromocion(promociones.get("FUGAZ30"), 42, funciones, pool);
        repetirConPromocion(promociones.get("PREESTRENO40"), 15, funciones, pool);
        repetirConPromocion(promociones.get("PRIMAVERA15"), 10, funciones, pool);
        Promocion primera = promociones.get("BIENVENIDA");
        for (Cliente cliente : bienvenida) {
            for (int intento = 0; intento < 20; intento++) {
                Funcion funcion = elegirPonderado(funciones, f -> f.getFechaHoraInicio().isAfter(inicioPeriodo.plusDays(1)) ? pesoFuncion(f) : 0);
                LocalDateTime fecha = momentoDeCompra(funcion);
                if (fecha != null && primera.estaVigente(fecha)
                    && comprar(cliente, funcion, 1 + azar.nextInt(2), fecha, primera, EstadoCompra.CONFIRMADA) != null) {
                    break;
                }
            }
        }
    }

    private void repetirConPromocion(Promocion promo, int veces, List<Funcion> funciones, List<Cliente> pool) {
        int hechas = 0;
        for (int intento = 0; hechas < veces && intento < veces * 30; intento++) {
            Funcion funcion = elegirPonderado(funciones, f -> f.getFechaHoraInicio().isAfter(promo.getVigenciaDesde().plusMinutes(30)) ? pesoFuncion(f) : 0);
            LocalDateTime fecha = momentoDeCompra(funcion);
            if (fecha == null || !promo.estaVigente(fecha) || !promo.valeElDia(funcion.getFechaHoraInicio().getDayOfWeek())) {
                continue;
            }
            if (comprar(azarDe(pool), funcion, cantidad(), fecha, promo, EstadoCompra.CONFIRMADA) != null) {
                hechas++;
            }
        }
    }

    private void ocupacionDeHoyYManiana(List<Funcion> funciones, List<Cliente> pool) {
        LocalDate hoy = ahora.toLocalDate();
        List<Funcion> visibles = funciones.stream()
            .filter(f -> f.getFechaHoraInicio().isAfter(ahora.plusMinutes(30)) && f.getFechaHoraInicio().toLocalDate().isBefore(hoy.plusDays(2)))
            .toList();
        Set<Funcion> elegidas = new HashSet<>();

        Funcion agotada = visibles.stream()
            .filter(f -> f.getFechaHoraInicio().toLocalDate().equals(hoy) && f.getFechaHoraInicio().isAfter(ahora.plusMinutes(90)))
            .min(Comparator.comparingInt((Funcion f) -> capacidad(f)).thenComparing(f -> Math.abs(f.getFechaHoraInicio().getHour() - 21)))
            .orElse(visibles.stream().min(Comparator.comparingInt(this::capacidad)).orElse(null));
        if (agotada != null) {
            llenar(agotada, 1.0, pool);
            elegidas.add(agotada);
        }
        List<Funcion> noche = new ArrayList<>(visibles.stream()
            .filter(f -> !elegidas.contains(f) && capacidad(f) <= 100 && f.getFechaHoraInicio().getHour() >= 19)
            .toList());
        Collections.shuffle(noche, azar);
        for (Funcion funcion : noche.subList(0, Math.min(3, noche.size()))) {
            llenar(funcion, 0.82 + azar.nextDouble() * 0.1, pool);
            elegidas.add(funcion);
        }
        List<Funcion> medias = new ArrayList<>(visibles.stream().filter(f -> !elegidas.contains(f) && capacidad(f) <= 142).toList());
        Collections.shuffle(medias, azar);
        for (Funcion funcion : medias.subList(0, Math.min(8, medias.size()))) {
            llenar(funcion, 0.2 + azar.nextDouble() * 0.5, pool);
        }
    }

    private void llenar(Funcion funcion, double ocupacion, List<Cliente> pool) {
        int objetivo = (int) Math.round(capacidad(funcion) * ocupacion);
        for (int intento = 0; vendidas(funcion) < objetivo && intento < 400; intento++) {
            LocalDateTime fecha = momentoDeCompra(funcion);
            if (fecha == null) {
                return;
            }
            int cantidad = Math.min(cantidad(), objetivo - vendidas(funcion));
            comprar(azarDe(pool), funcion, cantidad, fecha, null, estadoAlAzar(funcion, fecha));
        }
    }

    private void comprasDeLosClientesDemo(List<Funcion> funciones, Clientes clientes) {
        LocalDate hoy = ahora.toLocalDate();
        Cliente ana = clientes.frecuente();
        for (int i = 0; i < 9; i++) {
            LocalDate dia = hoy.minusDays(19 - i * 2L);
            comprarEnElDia(ana, funciones, dia, LocalTime.of(18, 0), 2, EstadoCompra.CONFIRMADA);
        }
        for (int dias : new int[] {1, 3, 6}) {
            comprarEnElDia(ana, funciones, hoy.plusDays(dias), LocalTime.of(20, 0), 2, EstadoCompra.CONFIRMADA);
        }
        Compra arrepentida = comprarEnElDia(ana, funciones, hoy.plusDays(2), LocalTime.of(21, 0), 2, EstadoCompra.CONFIRMADA);
        if (arrepentida != null) {
            arrepentir(arrepentida, arrepentida.getFechaCompra().plusHours(5), MotivoArrepentimiento.NO_PUEDO_ASISTIR);
        }
        Funcion lejana = buscarFuncion(funciones, hoy.plusDays(2), LocalTime.of(17, 0), f -> true);
        if (lejana != null) {
            Compra vieja = comprar(ana, lejana, 3, ahora.minusDays(12).withHour(11), null, EstadoCompra.CONFIRMADA);
            if (vieja != null) {
                SolicitudArrepentimiento solicitud = nuevaSolicitud(vieja, ahora.minusHours(3), MotivoArrepentimiento.EQUIVOQUE_FUNCION);
                solicitud.setEstado(EstadoArrepentimiento.FUERA_DE_PLAZO);
                solicitud.setMotivoFueraDePlazo("Pasaron más de 10 días corridos desde la compra");
                solicitudes.add(solicitud);
            }
        }

        Cliente martin = clientes.hoyALaNoche();
        LocalTime desde = ahora.toLocalTime().isAfter(LocalTime.of(18, 0)) ? ahora.toLocalTime().plusMinutes(60) : LocalTime.of(19, 0);
        Funcion esta = buscarFuncion(funciones, hoy, desde, f -> f.getFechaHoraInicio().isAfter(ahora.plusMinutes(60)));
        if (esta == null) {
            esta = buscarFuncion(funciones, hoy.plusDays(1), LocalTime.of(20, 0), f -> true);
        }
        if (esta != null) {
            comprar(martin, esta, 2, ahora.minusHours(4).isAfter(ahora.toLocalDate().atStartOfDay()) ? ahora.minusHours(4) : ahora.minusMinutes(30), null, EstadoCompra.CONFIRMADA);
        }
        comprarEnElDia(martin, funciones, hoy.minusDays(8), LocalTime.of(21, 0), 2, EstadoCompra.CONFIRMADA);

        Cliente diego = clientes.ocasional();
        comprarEnElDia(diego, funciones, hoy.minusDays(9), LocalTime.of(20, 0), 1, EstadoCompra.CONFIRMADA);
        comprarEnElDia(diego, funciones, hoy.plusDays(3), LocalTime.of(22, 0), 4, EstadoCompra.CONFIRMADA);
        comprarEnElDia(diego, funciones, hoy.minusDays(4), LocalTime.of(19, 0), 2, EstadoCompra.CANCELADA);
    }

    private Compra comprarEnElDia(Cliente cliente, List<Funcion> funciones, LocalDate dia, LocalTime hora, int cantidad, EstadoCompra estado) {
        Funcion funcion = buscarFuncion(funciones, dia, hora, f -> true);
        if (funcion == null) {
            return null;
        }
        LocalDateTime fecha = momentoDeCompra(funcion);
        return fecha == null ? null : comprar(cliente, funcion, cantidad, fecha, null, estado);
    }

    private Funcion buscarFuncion(List<Funcion> funciones, LocalDate dia, LocalTime hora, Predicate<Funcion> condicion) {
        return funciones.stream()
            .filter(f -> f.getFechaHoraInicio().toLocalDate().equals(dia) && condicion.test(f))
            .filter(f -> capacidad(f) - vendidas(f) >= 4)
            .min(Comparator.comparingLong(f -> Math.abs(Duration.between(dia.atTime(hora), f.getFechaHoraInicio()).toMinutes())))
            .orElse(null);
    }

    private void comprasGenerales(List<Funcion> funciones, List<Cliente> pool, Map<String, Promocion> promociones) {
        List<Promocion> abiertas = List.of(promociones.get("ESTRENO10"), promociones.get("LUNMIEVIE20"), promociones.get("MIERCOLES50"));
        int faltan = Math.max(120, COMPRAS_OBJETIVO - compras.size());
        for (int intento = 0; faltan > 0 && intento < faltan * 20; intento++) {
            Funcion funcion = elegirPonderado(funciones, this::pesoFuncion);
            LocalDateTime fecha = momentoDeCompra(funcion);
            if (fecha == null) {
                continue;
            }
            EstadoCompra estado = estadoAlAzar(funcion, fecha);
            Promocion promo = null;
            if (estado == EstadoCompra.CONFIRMADA && azar.nextDouble() < PROBABILIDAD_PROMO) {
                Promocion candidata = abiertas.get(azar.nextInt(abiertas.size()));
                if (candidata.estaVigente(fecha) && candidata.valeElDia(funcion.getFechaHoraInicio().getDayOfWeek())) {
                    promo = candidata;
                }
            }
            if (comprar(azarDe(pool), funcion, cantidad(), fecha, promo, estado) != null) {
                faltan--;
            }
        }
    }

    private void entradasDeHoyEnCadaCine(List<Funcion> funciones, List<Cliente> pool) {
        LocalDate hoy = ahora.toLocalDate();
        Map<Long, List<Funcion>> porCine = new LinkedHashMap<>();
        for (Funcion funcion : funciones) {
            if (funcion.getFechaHoraInicio().toLocalDate().equals(hoy) && funcion.getFechaHoraInicio().isAfter(ahora.minusMinutes(30))) {
                porCine.computeIfAbsent(funcion.getSala().getCine().getId(), id -> new ArrayList<>()).add(funcion);
            }
        }
        for (List<Funcion> deHoy : porCine.values()) {
            for (Funcion funcion : deHoy.subList(0, Math.min(3, deHoy.size()))) {
                boolean tieneVenta = compras.stream()
                    .anyMatch(c -> c.getFuncion().equals(funcion) && c.getEstado() == EstadoCompra.CONFIRMADA);
                if (!tieneVenta) {
                    LocalDateTime fecha = ahora.minusMinutes(20 + azar.nextInt(120));
                    comprar(azarDe(pool), funcion, 2, fecha.isBefore(inicioPeriodo) ? ahora.minusMinutes(5) : fecha, null, EstadoCompra.CONFIRMADA);
                }
            }
        }
    }

    private EstadoCompra estadoAlAzar(Funcion funcion, LocalDateTime fecha) {
        double tirada = azar.nextDouble();
        if (tirada < PROBABILIDAD_CANCELADA) {
            return EstadoCompra.CANCELADA;
        }
        boolean puedeArrepentirse = funcion.getFechaHoraInicio().minusMinutes(61).isAfter(fecha.plusMinutes(10))
            && ahora.isAfter(fecha.plusMinutes(10));
        if (tirada < PROBABILIDAD_CANCELADA + PROBABILIDAD_ARREPENTIDA && puedeArrepentirse) {
            return EstadoCompra.ARREPENTIDA;
        }
        return EstadoCompra.CONFIRMADA;
    }

    private Compra comprar(Cliente cliente, Funcion funcion, int cantidad, LocalDateTime fecha, Promocion promo, EstadoCompra estado) {
        if (promo != null && (promo.getUsosMaximosTotales() != null && usos.getOrDefault(promo.getId(), 0) >= promo.getUsosMaximosTotales())) {
            promo = null;
        }
        List<Butaca> butacas = elegirButacas(funcion, cantidad);
        if (butacas == null) {
            return null;
        }
        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setFuncion(funcion);
        compra.setFechaCompra(fecha.withNano(0));
        for (Butaca butaca : butacas) {
            Entrada entrada = new Entrada();
            entrada.setButaca(butaca);
            entrada.setCodigoQr(codigo(""));
            entrada.setTipo(tipoPara(funcion));
            entrada.setPrecioUnitario(entrada.getTipo().precioPara(funcion.getPrecioBase()));
            compra.agregarEntrada(entrada);
        }
        compra.setPromocion(promo);
        compra.calcularTotal();
        compras.add(compra);

        if (estado == EstadoCompra.CANCELADA) {
            if (azar.nextDouble() < 0.35) {
                Pago rechazado = nuevoPago(compra, "0002");
                rechazado.setEstado(EstadoPago.RECHAZADO);
            }
            compra.cancelar();
            return compra;
        }
        ocupadas.computeIfAbsent(funcion.getId(), id -> new HashSet<>()).addAll(butacas.stream().map(Butaca::getId).toList());
        Pago pago = nuevoPago(compra, String.format("%04d", 10 + azar.nextInt(9980)));
        pago.setEstado(EstadoPago.APROBADO);
        pago.setCodigoTransaccion("TX-" + new UUID(azar.nextLong(), azar.nextLong()));
        compra.confirmar();
        if (promo != null) {
            usos.merge(promo.getId(), 1, Integer::sum);
        }
        if (estado == EstadoCompra.ARREPENTIDA) {
            LocalDateTime limite = funcion.getFechaHoraInicio().minusMinutes(61);
            limite = limite.isBefore(ahora) ? limite : ahora;
            limite = limite.isBefore(fecha.plusDays(10)) ? limite : fecha.plusDays(10);
            long minutos = Math.max(10, Duration.between(fecha, limite).toMinutes());
            arrepentir(compra, fecha.plusMinutes(10 + (long) (azar.nextDouble() * (minutos - 10))), MotivoArrepentimiento.values()[azar.nextInt(3)]);
        }
        return compra;
    }

    private Pago nuevoPago(Compra compra, String ultimos4) {
        Pago pago = new Pago();
        pago.setCompra(compra);
        pago.setMonto(compra.getTotal());
        pago.setFechaPago(compra.getFechaCompra().plusMinutes(2 + azar.nextInt(5)));
        if (compra.getTotal().signum() == 0) {
            pago.setMetodoPago(MetodoPago.SIN_CARGO);
        } else {
            pago.setMetodoPago(azar.nextDouble() < 0.7 ? MetodoPago.TARJETA_CREDITO : MetodoPago.TARJETA_DEBITO);
            pago.setMarcaTarjeta(MARCAS.get(azar.nextInt(MARCAS.size())));
            pago.setUltimos4(ultimos4);
        }
        compra.setPago(pago);
        return pago;
    }

    private void arrepentir(Compra compra, LocalDateTime cuando, MotivoArrepentimiento motivo) {
        if (compra.getPromocion() != null) {
            usos.merge(compra.getPromocion().getId(), -1, Integer::sum);
        }
        ocupadas.getOrDefault(compra.getFuncion().getId(), Set.of()).removeAll(compra.getEntradas().stream().map(e -> e.getButaca().getId()).toList());
        compra.arrepentir();
        compra.getPago().setFechaReembolso(cuando);
        SolicitudArrepentimiento solicitud = nuevaSolicitud(compra, cuando, motivo);
        solicitud.setEstado(EstadoArrepentimiento.APROBADA);
        solicitud.setMontoReembolso(compra.getTotal());
        solicitud.setFechaReembolso(cuando);
        solicitud.setFechaResolucion(cuando);
        solicitudes.add(solicitud);
    }

    private SolicitudArrepentimiento nuevaSolicitud(Compra compra, LocalDateTime cuando, MotivoArrepentimiento motivo) {
        SolicitudArrepentimiento solicitud = new SolicitudArrepentimiento();
        solicitud.setCodigo(codigo("ARR-"));
        solicitud.setCompra(compra);
        solicitud.setFecha(cuando);
        solicitud.setMotivo(motivo);
        solicitud.setNombre(compra.getCliente().nombreCompleto());
        solicitud.setEmail(compra.getCliente().getUsuario().getEmail());
        return solicitud;
    }

    private void marcarUsadas() {
        for (Compra compra : compras) {
            LocalDateTime inicio = compra.getFuncion().getFechaHoraInicio();
            if (compra.getEstado() != EstadoCompra.CONFIRMADA || !inicio.plusMinutes(30).isBefore(ahora)) {
                continue;
            }
            for (Entrada entrada : compra.getEntradas()) {
                if (azar.nextDouble() < 0.85) {
                    LocalDateTime cuando = inicio.minusMinutes(5 + azar.nextInt(50));
                    entrada.marcarUsada(cuando.isAfter(compra.getFechaCompra()) ? cuando : compra.getFechaCompra().plusMinutes(1));
                }
            }
        }
    }

    private void actualizarUsos(Map<String, Promocion> promociones) {
        for (Promocion promo : promociones.values()) {
            int cantidad = usos.getOrDefault(promo.getId(), 0);
            promo.setUsosActuales(cantidad);
            if ("PREESTRENO40".equals(promo.getCodigo())) {
                promo.setUsosMaximosTotales(cantidad);
            }
        }
        promocionRepository.saveAll(promociones.values());
    }

    private void consultasDeSoporte(Clientes clientes) {
        Compra deAna = compras.stream()
            .filter(c -> c.getCliente().equals(clientes.frecuente()) && c.getEstado() == EstadoCompra.CONFIRMADA)
            .reduce((primera, segunda) -> segunda)
            .orElse(null);
        Compra deDiego = compras.stream().filter(c -> c.getCliente().equals(clientes.ocasional())).findFirst().orElse(null);
        List<ConsultaSoporte> consultas = List.of(
            consulta(clientes.frecuente(), MotivoSoporte.PROBLEMA_COMPRA, deAna, ahora.minusHours(5), false,
                "Compré dos entradas y en el mail no me aparece el QR. ¿Me lo pueden reenviar o lo puedo ver en algún lado?"),
            consulta(clientes.ocasional(), MotivoSoporte.PAGO, deDiego, ahora.minusDays(1).minusHours(2), false,
                "Me rechazaron el pago con tarjeta de débito pero el banco me dice que no hubo ningún problema. ¿Qué puede ser?"),
            consulta(clientes.genericos().get(3), MotivoSoporte.SUGERENCIA, null, ahora.minusDays(2), false,
                "Estaría bueno poder elegir la butaca desde el celular con un zoom más grande, en pantallas chicas cuesta."),
            consulta(clientes.genericos().get(8), MotivoSoporte.CUENTA, null, ahora.minusDays(6), true,
                "No me acuerdo con qué email me registré. Mi nombre es el mismo que figura en la compra de la semana pasada."),
            consulta(clientes.genericos().get(15), MotivoSoporte.OTRO, null, ahora.minusDays(9), true,
                "¿Las funciones subtituladas de la noche también tienen precio de jubilado? Gracias.")
        );
        consultaSoporteRepository.saveAll(consultas);
    }

    private ConsultaSoporte consulta(Cliente cliente, MotivoSoporte motivo, Compra compra, LocalDateTime fecha, boolean resuelta, String mensaje) {
        ConsultaSoporte consulta = new ConsultaSoporte();
        consulta.setNumero(codigo("SOP-"));
        consulta.setNombre(cliente.nombreCompleto());
        consulta.setEmail(cliente.getUsuario().getEmail());
        consulta.setMotivo(motivo);
        consulta.setCompra(compra);
        consulta.setMensaje(mensaje);
        consulta.setFecha(fecha);
        consulta.setClaveIdempotencia(new UUID(azar.nextLong(), azar.nextLong()).toString());
        consulta.setEstado(resuelta ? EstadoConsulta.RESUELTA : EstadoConsulta.ABIERTA);
        consulta.setFechaResolucion(resuelta ? fecha.plusHours(3) : null);
        return consulta;
    }

    private List<Butaca> elegirButacas(Funcion funcion, int cantidad) {
        List<Butaca> todas = butacasPorSala.computeIfAbsent(funcion.getSala().getId(), id -> butacaRepository.findBySalaIdAndActivaTrue(id).stream()
            .sorted(Comparator.comparing(Butaca::getFila).thenComparing(Butaca::getNumero))
            .toList());
        Set<Long> tomadas = ocupadas.getOrDefault(funcion.getId(), Set.of());
        Map<String, List<Butaca>> porFila = new LinkedHashMap<>();
        for (Butaca butaca : todas) {
            if (!tomadas.contains(butaca.getId()) && butaca.getTipo() != TipoButaca.DISCAPACIDAD) {
                porFila.computeIfAbsent(butaca.getFila(), fila -> new ArrayList<>()).add(butaca);
            }
        }
        List<String> filas = new ArrayList<>(porFila.keySet());
        Collections.shuffle(filas, azar);
        for (String fila : filas) {
            List<Butaca> libres = porFila.get(fila);
            for (int desde = 0; desde + cantidad <= libres.size(); desde++) {
                List<Butaca> tramo = libres.subList(desde, desde + cantidad);
                if (tramo.get(cantidad - 1).getNumero() - tramo.get(0).getNumero() == cantidad - 1) {
                    return new ArrayList<>(tramo);
                }
            }
        }
        List<Butaca> libres = todas.stream().filter(b -> !tomadas.contains(b.getId())).toList();
        return libres.size() >= cantidad ? new ArrayList<>(libres.subList(0, cantidad)) : null;
    }

    private LocalDateTime momentoDeCompra(Funcion funcion) {
        LocalDateTime inicio = funcion.getFechaHoraInicio();
        LocalDateTime hasta = inicio.minusMinutes(20).isBefore(ahora.minusMinutes(2)) ? inicio.minusMinutes(20) : ahora.minusMinutes(2);
        LocalDateTime desde = inicio.minusDays(7).isAfter(inicioPeriodo) ? inicio.minusDays(7) : inicioPeriodo;
        if (!hasta.isAfter(desde)) {
            return null;
        }
        double tirada = azar.nextDouble();
        long minutosAntes = tirada < 0.55 ? 20 + azar.nextInt(8 * 60)
            : tirada < 0.85 ? 24 * 60 + azar.nextInt(2 * 24 * 60)
            : 3 * 24 * 60 + azar.nextInt(4 * 24 * 60);
        LocalDateTime momento = inicio.minusMinutes(minutosAntes);
        if (momento.getHour() < 9) {
            momento = momento.withHour(9 + azar.nextInt(14));
        }
        if (momento.isAfter(hasta) || momento.isBefore(desde)) {
            long rango = Duration.between(desde, hasta).toMinutes();
            momento = desde.plusMinutes((long) (azar.nextDouble() * rango));
        }
        return momento.withSecond(azar.nextInt(60));
    }

    private double pesoFuncion(Funcion funcion) {
        if (!funcion.getFechaHoraInicio().isAfter(inicioPeriodo.plusHours(12))) {
            return 0;
        }
        int hora = funcion.getFechaHoraInicio().getHour();
        double peso = hora >= 19 || hora < 2 ? 3 : hora >= 16 ? 1.5 : 1;
        DayOfWeek dia = funcion.getFechaHoraInicio().getDayOfWeek();
        if (dia == DayOfWeek.FRIDAY || dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY) {
            peso *= 1.6;
        }
        return peso * ProgramadorDeFunciones.peso(funcion.getPelicula(), funcion.getFechaHoraInicio().toLocalDate());
    }

    private Funcion elegirPonderado(List<Funcion> funciones, ToDoubleFunction<Funcion> peso) {
        double total = funciones.stream().mapToDouble(peso).sum();
        double tirada = azar.nextDouble() * total;
        for (Funcion funcion : funciones) {
            tirada -= peso.applyAsDouble(funcion);
            if (tirada <= 0 && peso.applyAsDouble(funcion) > 0) {
                return funcion;
            }
        }
        return funciones.get(funciones.size() - 1);
    }

    private TipoEntrada tipoPara(Funcion funcion) {
        double tirada = azar.nextDouble();
        boolean familiar = funcion.getPelicula().getGenero() == Genero.ANIMACION;
        boolean admiteMenores = funcion.getPelicula().getClasificacion() == Clasificacion.ATP;
        double menor = !admiteMenores ? 0 : familiar ? 0.4 : 0.12;
        if (tirada < menor) {
            return TipoEntrada.MENOR;
        }
        return tirada < menor + (familiar ? 0.1 : 0.17) ? TipoEntrada.JUBILADO : TipoEntrada.GENERAL;
    }

    private int cantidad() {
        int tirada = azar.nextInt(100);
        for (int i = 0; i < PESOS_CANTIDAD.length; i++) {
            tirada -= PESOS_CANTIDAD[i];
            if (tirada < 0) {
                return i + 1;
            }
        }
        return 2;
    }

    private String codigo(String prefijo) {
        String codigo;
        do {
            StringBuilder texto = new StringBuilder(prefijo);
            for (int i = 0; i < GeneradorCodigos.LARGO; i++) {
                texto.append(GeneradorCodigos.ALFABETO.charAt(azar.nextInt(GeneradorCodigos.ALFABETO.length())));
            }
            codigo = texto.toString();
        } while (!codigos.add(codigo));
        return codigo;
    }

    private int capacidad(Funcion funcion) {
        return butacasPorSala.computeIfAbsent(funcion.getSala().getId(), id -> butacaRepository.findBySalaIdAndActivaTrue(id).stream()
            .sorted(Comparator.comparing(Butaca::getFila).thenComparing(Butaca::getNumero))
            .toList()).size();
    }

    private int vendidas(Funcion funcion) {
        return ocupadas.getOrDefault(funcion.getId(), Set.of()).size();
    }

    private Cliente azarDe(List<Cliente> clientes) {
        return clientes.get(azar.nextInt(clientes.size()));
    }

    private long contar(EstadoCompra estado) {
        return compras.stream().filter(c -> c.getEstado() == estado).count();
    }
}
