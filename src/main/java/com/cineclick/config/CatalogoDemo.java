package com.cineclick.config;

import com.cineclick.model.Clasificacion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Genero;
import com.cineclick.model.Idioma;
import com.cineclick.model.PlanoSala;
import com.cineclick.model.Provincia;
import com.cineclick.model.TipoSala;

import java.util.List;
import java.util.Set;

import static com.cineclick.model.Clasificacion.ATP;
import static com.cineclick.model.Clasificacion.MAS_13;
import static com.cineclick.model.Clasificacion.MAS_16;
import static com.cineclick.model.Clasificacion.MAS_18;
import static com.cineclick.model.FormatoFuncion.CUATRO_D;
import static com.cineclick.model.FormatoFuncion.DOS_D;
import static com.cineclick.model.FormatoFuncion.IMAX;
import static com.cineclick.model.FormatoFuncion.TRES_D;
import static com.cineclick.model.Idioma.CASTELLANO;
import static com.cineclick.model.Idioma.SUBTITULADA;

public final class CatalogoDemo {

    public record SalaDemo(String nombre, int numero, TipoSala tipo, PlanoSala plano) {
    }

    public record CineDemo(
        String nombre,
        Provincia provincia,
        String localidad,
        String calle,
        int altura,
        String telefono,
        String imagen,
        List<SalaDemo> salas
    ) {
    }

    public record PeliculaDemo(
        String titulo,
        int duracion,
        Clasificacion clasificacion,
        Genero genero,
        Idioma idioma,
        Set<FormatoFuncion> formatos,
        int estrenoEnDias,
        String poster,
        boolean destacada,
        String sinopsis
    ) {
    }

    public record UsuarioDemo(String nombre, String apellido, String email, String password) {
    }

    private static final String POSTERS = "/img/posters/";

    public static final UsuarioDemo ADMIN = new UsuarioDemo("Admin", "CineClick", "admin@cineclick.com", "admin123");
    public static final UsuarioDemo FRECUENTE = new UsuarioDemo("Ana", "Gómez", "ana@cineclick.com", "ana123");
    public static final UsuarioDemo HOY_A_LA_NOCHE = new UsuarioDemo("Martín", "Pereyra", "martin@cineclick.com", "martin123");
    public static final UsuarioDemo SIN_COMPRAS = new UsuarioDemo("Sofía", "Ramírez", "sofia@cineclick.com", "sofia123");
    public static final UsuarioDemo OCASIONAL = new UsuarioDemo("Diego", "Fernández", "diego@cineclick.com", "diego123");

    public static final List<String> NOMBRES = List.of(
        "Lucía", "Mateo", "Valentina", "Santiago", "Camila", "Benjamín", "Martina", "Joaquín", "Julieta", "Tomás",
        "Catalina", "Facundo", "Agustina", "Nicolás", "Florencia", "Lautaro", "Micaela", "Franco", "Rocío", "Ignacio"
    );

    public static final List<String> APELLIDOS = List.of(
        "González", "Rodríguez", "López", "Martínez", "García", "Pérez", "Sánchez", "Romero", "Sosa", "Torres",
        "Álvarez", "Ruiz", "Díaz", "Acosta", "Benítez", "Medina", "Suárez", "Herrera", "Castro", "Rojas"
    );

    public static final List<CineDemo> CINES = List.of(
        new CineDemo("CineClick Abasto", Provincia.CABA, "Balvanera", "Av. Corrientes", 3247, "1140001000", "/img/cines/abasto.jpg", List.of(
            new SalaDemo("Sala 1", 1, TipoSala.TRADICIONAL, PlanoSala.GRANDE),
            new SalaDemo("Sala 2", 2, TipoSala.TRADICIONAL, PlanoSala.CLASICA),
            new SalaDemo("Sala 3", 3, TipoSala.TRADICIONAL, PlanoSala.CHICA),
            new SalaDemo("Sala 3D", 4, TipoSala.TRES_D, PlanoSala.ESTADIO),
            new SalaDemo("Sala Premium", 5, TipoSala.PREMIUM, PlanoSala.PREMIUM),
            new SalaDemo("Sala IMAX", 6, TipoSala.IMAX, PlanoSala.IMAX)
        )),
        new CineDemo("CineClick Recoleta", Provincia.CABA, "Recoleta", "Av. Pueyrredón", 1852, "1140002000", "/img/cines/recoleta.jpg", List.of(
            new SalaDemo("Sala 1", 1, TipoSala.TRADICIONAL, PlanoSala.ESTADIO),
            new SalaDemo("Sala 2", 2, TipoSala.TRADICIONAL, PlanoSala.CHICA),
            new SalaDemo("Sala 3D", 3, TipoSala.TRES_D, PlanoSala.CLASICA),
            new SalaDemo("Sala Premium", 4, TipoSala.PREMIUM, PlanoSala.PREMIUM)
        )),
        new CineDemo("CineClick Palermo", Provincia.CABA, "Palermo", "Av. Santa Fe", 3253, "1140003000", "/img/cines/palermo.jpg", List.of(
            new SalaDemo("Sala 1", 1, TipoSala.TRADICIONAL, PlanoSala.CLASICA),
            new SalaDemo("Sala 2", 2, TipoSala.TRADICIONAL, PlanoSala.ESTADIO),
            new SalaDemo("Sala 3", 3, TipoSala.TRADICIONAL, PlanoSala.CHICA),
            new SalaDemo("Sala 3D", 4, TipoSala.TRES_D, PlanoSala.CLASICA),
            new SalaDemo("Sala Premium", 5, TipoSala.PREMIUM, PlanoSala.PREMIUM),
            new SalaDemo("Sala 4D", 6, TipoSala.CUATRO_D, PlanoSala.CUATRO_D)
        )),
        new CineDemo("CineClick Núñez", Provincia.CABA, "Núñez", "Av. Cabildo", 3018, "1140004000", "/img/cines/nunez.jpg", List.of(
            new SalaDemo("Sala 1", 1, TipoSala.TRADICIONAL, PlanoSala.GRANDE),
            new SalaDemo("Sala 2", 2, TipoSala.TRADICIONAL, PlanoSala.CHICA),
            new SalaDemo("Sala 3D", 3, TipoSala.TRES_D, PlanoSala.ESTADIO),
            new SalaDemo("Sala Premium", 4, TipoSala.PREMIUM, PlanoSala.PREMIUM),
            new SalaDemo("Sala IMAX", 5, TipoSala.IMAX, PlanoSala.IMAX)
        ))
    );

    public static final List<PeliculaDemo> PELICULAS = List.of(
        new PeliculaDemo(
            "Wildwood", 132, ATP, Genero.ANIMACION, CASTELLANO, Set.of(DOS_D, TRES_D), -10, POSTERS + "wildwood.jpg", true,
            "Prue se adentra en el Bosque Salvaje, un territorio secreto y mágico en las afueras de su ciudad, para rescatar a su hermanito, que fue raptado por una bandada de cuervos."
        ),
        new PeliculaDemo(
            "Clayface", 108, MAS_16, Genero.TERROR, SUBTITULADA, Set.of(DOS_D, IMAX, CUATRO_D), -10, POSTERS + "clayface.jpg", true,
            "Matt Hagen, un actor al que le desfiguran el rostro, se somete a un tratamiento experimental que le permite moldear su cuerpo como arcilla. El precio es convertirse en un monstruo."
        ),
        new PeliculaDemo(
            "¿Querés ser mi hijo?", 95, MAS_13, Genero.COMEDIA, CASTELLANO, Set.of(DOS_D), -11, null, true,
            "Una mujer que tiene que rehacer su vida profesional se enamora de un hombre mucho más joven que ella. Con Wanda Nara y Agustín Bernasconi."
        ),
        new PeliculaDemo(
            "Sensatez y Sentimiento", 132, ATP, Genero.DRAMA, SUBTITULADA, Set.of(DOS_D), -17, POSTERS + "sensatez-y-sentimiento.jpg", false,
            "Basada en la novela de Jane Austen. Tras la muerte de su padre, las hermanas Elinor y Marianne Dashwood tienen que empezar de nuevo y descubren el amor de maneras muy distintas."
        ),
        new PeliculaDemo(
            "Other Mommy", 93, MAS_16, Genero.TERROR, SUBTITULADA, Set.of(DOS_D), -24, POSTERS + "other-mommy.jpg", false,
            "Una madre tiene que proteger a su hija de siete años de una entidad sobrenatural que toma su apariencia."
        ),
        new PeliculaDemo(
            "Interestelar", 169, MAS_13, Genero.CIENCIA_FICCION, SUBTITULADA, Set.of(DOS_D, IMAX), -30, POSTERS + "interestelar.jpg", false,
            "Reestreno. Un grupo de exploradores viaja a través de un agujero de gusano para buscar un nuevo hogar para la humanidad."
        ),
        new PeliculaDemo(
            "Verity: La sombra de un engaño", 117, MAS_18, Genero.DRAMA, SUBTITULADA, Set.of(DOS_D), -31, POSTERS + "verity-la-sombra-de-un-engano.jpg", false,
            "Lowen, una escritora contratada para terminar la saga de la exitosa Verity Crawford, encuentra en su casa unas memorias que revelan una verdad escalofriante."
        ),
        new PeliculaDemo(
            "Resident Evil", 94, MAS_16, Genero.TERROR, SUBTITULADA, Set.of(DOS_D, IMAX, CUATRO_D), -45, POSTERS + "resident-evil.png", false,
            "Dirigida por Zach Cregger. Un repartidor de insumos médicos queda atrapado en Raccoon City en medio de un brote de criaturas infectadas y tiene que abrirse paso para sobrevivir."
        ),
        new PeliculaDemo(
            "La Caja del Demonio", 105, MAS_16, Genero.TERROR, SUBTITULADA, Set.of(DOS_D), -50, POSTERS + "la-caja-del-demonio.jpg", false,
            "Dirigida por Daniel Stamm. Ellen se muda a un pueblo tranquilo para empezar de nuevo, pero pronto descubre que una presencia sobrenatural la acecha."
        ),
        new PeliculaDemo(
            "Intensamente 2", 96, ATP, Genero.ANIMACION, CASTELLANO, Set.of(DOS_D, TRES_D), -60, POSTERS + "intensamente-2.jpg", false,
            "Reestreno. Riley crece y nuevas emociones llegan al centro de control."
        ),
        new PeliculaDemo(
            "Godzilla Minus Zero", 135, MAS_13, Genero.CIENCIA_FICCION, SUBTITULADA, Set.of(DOS_D, IMAX, CUATRO_D), 3, POSTERS + "godzilla-minus-zero.jpg", true,
            "Secuela de Godzilla Minus One, dirigida por Takashi Yamazaki. En 1949, con Japón todavía en reconstrucción, una nueva amenaza surge del océano."
        ),
        new PeliculaDemo(
            "El gato con sombrero", 107, ATP, Genero.ANIMACION, CASTELLANO, Set.of(DOS_D, TRES_D), 3, POSTERS + "el-gato-con-sombrero.png", false,
            "El Gato con Sombrero, el personaje de Dr. Seuss, llega para ayudar a dos hermanos a adaptarse a una mudanza. A su manera: con mucho caos."
        ),
        new PeliculaDemo(
            "Ebenezer: Los fantasmas de Navidad", 116, MAS_13, Genero.DRAMA, SUBTITULADA, Set.of(DOS_D), 10, POSTERS + "ebenezer-los-fantasmas-de-navidad.jpg", false,
            "Dirigida por Ti West. Johnny Depp es Ebenezer Scrooge en una versión gótica del clásico de Charles Dickens, con los fantasmas de la Navidad visitando al avaro en una noche inolvidable."
        ),
        new PeliculaDemo(
            "Los juegos del hambre: Amanecer en la cosecha", 158, MAS_13, Genero.ACCION, SUBTITULADA, Set.of(DOS_D, IMAX), 17, POSTERS + "los-juegos-del-hambre-amanecer-en-la-cosecha.jpg", false,
            "Precuela de la saga, dirigida por Francis Lawrence. Un joven Haymitch Abernathy es elegido para los 50.º Juegos del Hambre, una edición especial en la que el Capitolio duplica la cantidad de tributos."
        ),
        new PeliculaDemo(
            "Yo soy Rocky", 128, MAS_13, Genero.DRAMA, SUBTITULADA, Set.of(DOS_D), 17, POSTERS + "yo-soy-rocky.png", false,
            "Dirigida por Peter Farrelly. Cómo un joven Sylvester Stallone escribió Rocky y se empeñó en protagonizarla cuando nadie en Hollywood creía en él."
        ),
        new PeliculaDemo(
            "Hechizo: Bienvenidos a Hexe", 103, ATP, Genero.ANIMACION, CASTELLANO, Set.of(DOS_D, TRES_D), 24, POSTERS + "hechizo-bienvenidos-a-hexe.jpg", false,
            "Animación de Disney. Una adolescente descubre que tiene poderes mágicos y viaja con su madre al reino de Hexe, donde nada es lo que parece."
        ),
        new PeliculaDemo(
            "La nueva Focker", 105, MAS_13, Genero.COMEDIA, SUBTITULADA, Set.of(DOS_D), 24, POSTERS + "la-nueva-focker.jpg", false,
            "Cuarta película de la saga. Ahora es Greg Focker el que desconfía: Olivia, la prometida de su hijo, no le termina de cerrar."
        )
    );

    private CatalogoDemo() {
    }
}
