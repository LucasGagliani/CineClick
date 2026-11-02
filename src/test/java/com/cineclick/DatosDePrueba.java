package com.cineclick;

import com.cineclick.model.Cine;
import com.cineclick.model.EstadoFuncion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Funcion;
import com.cineclick.model.Genero;
import com.cineclick.model.Idioma;
import com.cineclick.model.Clasificacion;
import com.cineclick.model.Pelicula;
import com.cineclick.model.PlanoSala;
import com.cineclick.model.Provincia;
import com.cineclick.model.RolUsuario;
import com.cineclick.model.Sala;
import com.cineclick.model.TipoSala;
import com.cineclick.model.Usuario;
import com.cineclick.repository.CineRepository;
import com.cineclick.repository.FuncionRepository;
import com.cineclick.repository.PeliculaRepository;
import com.cineclick.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Component
@Order(2)
@Profile("test")
@RequiredArgsConstructor
class DatosDePrueba implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final CineRepository cineRepository;
    private final PeliculaRepository peliculaRepository;
    private final FuncionRepository funcionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Usuario admin = new Usuario();
        admin.setNombre("Admin");
        admin.setApellido("CineClick");
        admin.setEmail("admin@cineclick.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRol(RolUsuario.ADMIN);
        usuarioRepository.save(admin);

        Cine cine = new Cine();
        cine.setNombre("CineClick Prueba");
        cine.setProvincia(Provincia.CABA);
        cine.setLocalidad("Palermo");
        cine.setCalle("Av. Santa Fe");
        cine.setAltura(1000);
        Sala sala = new Sala();
        sala.setNombre("Sala 1");
        sala.setNumero(1);
        sala.setTipo(TipoSala.TRADICIONAL);
        PlanoSala.CLASICA.crearButacas(sala).forEach(sala::agregarButaca);
        cine.agregarSala(sala);
        cineRepository.save(cine);

        Pelicula pelicula = new Pelicula();
        pelicula.setTitulo("Wildwood");
        pelicula.setSinopsis("Prue se adentra en el Bosque Salvaje para rescatar a su hermanito.");
        pelicula.setDuracionMinutos(132);
        pelicula.setClasificacion(Clasificacion.ATP);
        pelicula.setGenero(Genero.ANIMACION);
        pelicula.setIdioma(Idioma.CASTELLANO);
        pelicula.setFormatosDisponibles(Set.of(FormatoFuncion.DOS_D));
        pelicula.setFechaEstreno(LocalDate.now().minusDays(10));
        peliculaRepository.save(pelicula);

        Funcion funcion = new Funcion();
        funcion.setPelicula(pelicula);
        funcion.setSala(sala);
        funcion.setFechaHoraInicio(LocalDate.now().plusDays(1).atTime(14, 0));
        funcion.setFormato(FormatoFuncion.DOS_D);
        funcion.setIdioma(Idioma.CASTELLANO);
        funcion.setPrecioBase(new BigDecimal("4200"));
        funcion.setEstado(EstadoFuncion.PROGRAMADA);
        funcion.calcularHorarioFin(20);
        funcionRepository.save(funcion);
    }
}
