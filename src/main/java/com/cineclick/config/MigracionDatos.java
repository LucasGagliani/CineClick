package com.cineclick.config;

import com.cineclick.model.BarriosCaba;
import com.cineclick.model.Clasificacion;
import com.cineclick.model.Idioma;
import com.cineclick.model.Provincia;
import com.cineclick.service.Normalizador;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(1)
@RequiredArgsConstructor
public class MigracionDatos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionDatos.class);
    private static final Pattern CALLE_Y_ALTURA = Pattern.compile("^(.+?)\\s+(\\d{1,5})$");
    private static final Map<String, String> ZONAS_SIN_BARRIO_PROPIO = Map.of("abasto", "Balvanera");

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrarCines();
        migrarColumna("peliculas", "clasificacion", Clasificacion.class, Clasificacion::desdeTexto, Clasificacion.ATP);
        migrarColumna("peliculas", "idioma", Idioma.class, Idioma::desdeTexto, Idioma.CASTELLANO);
        migrarColumna("funciones", "idioma", Idioma.class, Idioma::desdeTexto, Idioma.CASTELLANO);
    }

    private void migrarCines() {
        if (!existeColumna("cines", "ciudad")) {
            return;
        }
        List<Map<String, Object>> cines = jdbcTemplate.queryForList("select id, nombre, direccion, ciudad, telefono from cines");
        for (Map<String, Object> cine : cines) {
            String nombre = (String) cine.get("nombre");
            String direccion = Normalizador.texto((String) cine.get("direccion"));
            String ciudad = Normalizador.texto((String) cine.get("ciudad"));
            Optional<String> barrio = barrioDe(nombre, ciudad);
            Provincia provincia = provinciaDe(ciudad, barrio.isPresent());
            String localidad = provincia == Provincia.CABA ? barrio.orElse(null) : Normalizador.formatoTitulo(ciudad);
            String calle = direccion;
            Integer altura = null;
            Matcher partes = direccion == null ? null : CALLE_Y_ALTURA.matcher(direccion);
            if (partes != null && partes.matches()) {
                calle = partes.group(1);
                altura = Integer.valueOf(partes.group(2));
            }
            String telefono = telefonoDe((String) cine.get("telefono"));
            jdbcTemplate.update(
                "update cines set provincia = ?, localidad = ?, calle = ?, altura = ?, telefono = ? where id = ?",
                provincia == null ? null : provincia.name(), localidad, calle, altura, telefono, cine.get("id")
            );
            log.info("Migracion de cine {} \"{}\": \"{}\", \"{}\" -> provincia={}, localidad={}, calle={}, altura={}, telefono={}",
                cine.get("id"), nombre, direccion, ciudad, provincia, localidad, calle, altura, telefono);
        }
        jdbcTemplate.execute("alter table cines drop column ciudad");
        jdbcTemplate.execute("alter table cines drop column if exists direccion");
    }

    private Optional<String> barrioDe(String nombre, String ciudad) {
        String texto = Normalizador.clave((nombre == null ? "" : nombre) + " " + (ciudad == null ? "" : ciudad));
        Optional<String> barrio = BarriosCaba.TODOS.stream()
            .filter(candidato -> texto.matches(".*\\b" + Pattern.quote(Normalizador.clave(candidato)) + "\\b.*"))
            .findFirst();
        if (barrio.isPresent()) {
            return barrio;
        }
        return ZONAS_SIN_BARRIO_PROPIO.entrySet().stream()
            .filter(zona -> texto.contains(zona.getKey()))
            .map(Map.Entry::getValue)
            .findFirst();
    }

    private Provincia provinciaDe(String ciudad, boolean tieneBarrioPorteno) {
        String clave = Normalizador.clave(ciudad);
        if (clave.equals("caba") || clave.contains("capital federal") || clave.contains("ciudad autonoma")) {
            return Provincia.CABA;
        }
        if (clave.equals("buenos aires")) {
            return tieneBarrioPorteno ? Provincia.CABA : Provincia.BUENOS_AIRES;
        }
        for (Provincia provincia : Provincia.values()) {
            if (Normalizador.clave(provincia.getNombreCorto()).equals(clave)) {
                return provincia;
            }
        }
        return null;
    }

    private String telefonoDe(String telefono) {
        try {
            return Normalizador.telefono(telefono, "telefono");
        } catch (RuntimeException e) {
            log.warn("Migracion: el telefono \"{}\" no tiene 10 digitos, queda vacio", telefono);
            return null;
        }
    }

    private <E extends Enum<E>> void migrarColumna(String tabla, String columna, Class<E> tipo, Function<String, Optional<E>> convertir, E porDefecto) {
        List<String> valores = jdbcTemplate.queryForList("select distinct " + columna + " from " + tabla, String.class);
        for (String valor : valores) {
            if (valor == null || esConstante(tipo, valor)) {
                continue;
            }
            E nuevo = convertir.apply(valor).orElse(porDefecto);
            if (convertir.apply(valor).isEmpty()) {
                log.warn("Migracion de {}.{}: \"{}\" no se reconoce, pasa a {}", tabla, columna, valor, nuevo);
            }
            int filas = jdbcTemplate.update("update " + tabla + " set " + columna + " = ? where " + columna + " = ?", nuevo.name(), valor);
            log.info("Migracion de {}.{}: \"{}\" -> {} ({} filas)", tabla, columna, valor, nuevo, filas);
        }
    }

    private static <E extends Enum<E>> boolean esConstante(Class<E> tipo, String valor) {
        for (E constante : tipo.getEnumConstants()) {
            if (constante.name().equals(valor)) {
                return true;
            }
        }
        return false;
    }

    private boolean existeColumna(String tabla, String columna) {
        Integer cantidad = jdbcTemplate.queryForObject(
            "select count(*) from information_schema.columns where lower(table_name) = ? and lower(column_name) = ?",
            Integer.class, tabla, columna
        );
        return cantidad != null && cantidad > 0;
    }
}
