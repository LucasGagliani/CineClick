package com.cineclick.config;

import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Genero;
import com.cineclick.model.PlanoSala;
import com.cineclick.model.TipoSala;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static com.cineclick.model.FormatoFuncion.CUATRO_D;
import static com.cineclick.model.FormatoFuncion.DOS_D;
import static com.cineclick.model.FormatoFuncion.IMAX;
import static com.cineclick.model.FormatoFuncion.TRES_D;

public final class CatalogoDemo {

    public record SalaDemo(String nombre, int numero, TipoSala tipo, PlanoSala plano) {
    }

    public record CineDemo(String nombre, String direccion, String ciudad, String telefono, List<SalaDemo> salas) {
    }

    public record PeliculaDemo(
        String titulo,
        int duracion,
        String clasificacion,
        Genero genero,
        String idioma,
        Set<FormatoFuncion> formatos,
        LocalDate estreno,
        String poster,
        boolean destacada,
        boolean preventa,
        String sinopsis
    ) {
    }

    private static final String WIKI = "https://upload.wikimedia.org/wikipedia/en/";

    public static final List<CineDemo> CINES = List.of(
        new CineDemo("CineClick Abasto", "Av. Corrientes 3247", "Buenos Aires", "011-4000-1000", List.of(
            new SalaDemo("Sala 1", 1, TipoSala.TRADICIONAL, PlanoSala.CLASICA),
            new SalaDemo("Sala IMAX", 2, TipoSala.IMAX, PlanoSala.IMAX)
        )),
        new CineDemo("CineClick Recoleta", "Av. Pueyrredón 1852", "CABA", "011-4000-2000", List.of(
            new SalaDemo("Sala 1", 1, TipoSala.TRADICIONAL, PlanoSala.ESTADIO),
            new SalaDemo("Sala Premium", 2, TipoSala.PREMIUM, PlanoSala.PREMIUM)
        )),
        new CineDemo("CineClick Palermo", "Av. Santa Fe 3253", "CABA", "011-4000-3000", List.of(
            new SalaDemo("Sala 1", 1, TipoSala.TRADICIONAL, PlanoSala.CLASICA),
            new SalaDemo("Sala 4D", 2, TipoSala.CUATRO_D, PlanoSala.CUATRO_D)
        ))
    );

    public static final List<PeliculaDemo> PELICULAS = List.of(
        new PeliculaDemo(
            "Wildwood", 132, "ATP", Genero.ANIMACION, "Castellano", Set.of(DOS_D, TRES_D),
            LocalDate.of(2026, 10, 23), WIKI + "a/aa/Wildwood_2026_poster.jpg", true, false,
            "Prue se adentra en el Bosque Salvaje, un territorio secreto y mágico en las afueras de su ciudad, para rescatar a su hermanito, que fue raptado por una bandada de cuervos."
        ),
        new PeliculaDemo(
            "Clayface", 108, "+16", Genero.TERROR, "Subtitulada", Set.of(DOS_D, IMAX),
            LocalDate.of(2026, 10, 23), WIKI + "4/4f/Clayface_%28film%29_poster.jpg", true, false,
            "Matt Hagen, un actor al que le desfiguran el rostro, se somete a un tratamiento experimental que le permite moldear su cuerpo como arcilla. El precio es convertirse en un monstruo."
        ),
        new PeliculaDemo(
            "La Caja del Demonio", 105, "+16", Genero.TERROR, "Subtitulada", Set.of(DOS_D),
            LocalDate.of(2026, 7, 3), WIKI + "0/06/Lockbox_%28film%29_poster.jpg", false, false,
            "Dirigida por Daniel Stamm. Ellen se muda a un pueblo tranquilo para empezar de nuevo, pero pronto descubre que una presencia sobrenatural la acecha."
        ),
        new PeliculaDemo(
            "Other Mommy", 93, "+16", Genero.TERROR, "Subtitulada", Set.of(DOS_D),
            LocalDate.of(2026, 10, 9), WIKI + "d/d2/Other_Mommy_poster.jpg", false, false,
            "Una madre tiene que proteger a su hija de siete años de una entidad sobrenatural que toma su apariencia."
        ),
        new PeliculaDemo(
            "Sensatez y Sentimiento", 132, "ATP", Genero.DRAMA, "Subtitulada", Set.of(DOS_D),
            LocalDate.of(2026, 10, 16), WIKI + "e/e0/Sense_and_Sensibility%2C_2026.jpg", false, false,
            "Basada en la novela de Jane Austen. Tras la muerte de su padre, las hermanas Elinor y Marianne Dashwood tienen que empezar de nuevo y descubren el amor de maneras muy distintas."
        ),
        new PeliculaDemo(
            "¿Querés ser mi hijo?", 95, "+13", Genero.COMEDIA, "Castellano", Set.of(DOS_D),
            LocalDate.of(2026, 10, 22), null, true, false,
            "Una mujer que tiene que rehacer su vida profesional se enamora de un hombre mucho más joven que ella. Con Wanda Nara y Agustín Bernasconi."
        ),
        new PeliculaDemo(
            "Resident Evil", 94, "+16", Genero.TERROR, "Subtitulada", Set.of(DOS_D, IMAX, CUATRO_D),
            LocalDate.of(2026, 9, 18), WIKI + "3/3c/Resident_Evil_2026_film_poster.png", false, false,
            "Dirigida por Zach Cregger. Un repartidor de insumos médicos queda atrapado en Raccoon City en medio de un brote de criaturas infectadas y tiene que abrirse paso para sobrevivir."
        ),
        new PeliculaDemo(
            "Verity: La sombra de un engaño", 117, "+18", Genero.DRAMA, "Subtitulada", Set.of(DOS_D),
            LocalDate.of(2026, 10, 2), WIKI + "6/60/Verity_poster.jpg", false, false,
            "Lowen, una escritora contratada para terminar la saga de la exitosa Verity Crawford, encuentra en su casa unas memorias que revelan una verdad escalofriante."
        ),
        new PeliculaDemo(
            "Godzilla Minus Zero", 135, "+13", Genero.CIENCIA_FICCION, "Subtitulada", Set.of(DOS_D, IMAX, CUATRO_D),
            LocalDate.of(2026, 11, 5), WIKI + "0/09/Godzilla_Minus_Zero_poster.jpg", false, true,
            "Secuela de Godzilla Minus One, dirigida por Takashi Yamazaki. En 1949, con Japón todavía en reconstrucción, una nueva amenaza surge del océano."
        ),
        new PeliculaDemo(
            "El gato con sombrero", 107, "ATP", Genero.ANIMACION, "Castellano", Set.of(DOS_D, TRES_D),
            LocalDate.of(2026, 11, 5), WIKI + "e/e4/The_Cat_in_the_Hat_%282026_film_poster%29.png", false, true,
            "El Gato con Sombrero, el personaje de Dr. Seuss, llega para ayudar a dos hermanos a adaptarse a una mudanza. A su manera: con mucho caos."
        ),
        new PeliculaDemo(
            "Ebenezer: Los fantasmas de Navidad", 116, "+13", Genero.DRAMA, "Subtitulada", Set.of(DOS_D),
            LocalDate.of(2026, 11, 12), WIKI + "c/ca/Ebenezer_poster.jpg", false, false,
            "Dirigida por Ti West. Johnny Depp es Ebenezer Scrooge en una versión gótica del clásico de Charles Dickens, con los fantasmas de la Navidad visitando al avaro en una noche inolvidable."
        ),
        new PeliculaDemo(
            "Los juegos del hambre: Amanecer en la cosecha", 158, "+13", Genero.ACCION, "Subtitulada", Set.of(DOS_D, IMAX),
            LocalDate.of(2026, 11, 19), WIKI + "d/da/The_Hunger_Games_-_Sunrise_on_the_Reaping_poster.jpg", false, true,
            "Precuela de la saga, dirigida por Francis Lawrence. Un joven Haymitch Abernathy es elegido para los 50.º Juegos del Hambre, una edición especial en la que el Capitolio duplica la cantidad de tributos."
        ),
        new PeliculaDemo(
            "Yo soy Rocky", 128, "+13", Genero.DRAMA, "Subtitulada", Set.of(DOS_D),
            LocalDate.of(2026, 11, 19), WIKI + "5/5e/I_Play_Rocky_poster.png", false, false,
            "Dirigida por Peter Farrelly. Cómo un joven Sylvester Stallone escribió Rocky y se empeñó en protagonizarla cuando nadie en Hollywood creía en él."
        ),
        new PeliculaDemo(
            "Hechizo: Bienvenidos a Hexe", 103, "ATP", Genero.ANIMACION, "Castellano", Set.of(DOS_D, TRES_D),
            LocalDate.of(2026, 11, 26), WIKI + "7/72/Hexed_poster.jpg", false, false,
            "Animación de Disney. Una adolescente descubre que tiene poderes mágicos y viaja con su madre al reino de Hexe, donde nada es lo que parece."
        ),
        new PeliculaDemo(
            "La nueva Focker", 105, "+13", Genero.COMEDIA, "Subtitulada", Set.of(DOS_D),
            LocalDate.of(2026, 11, 26), WIKI + "3/33/Focker_In-Law_poster.jpg", false, false,
            "Cuarta película de la saga. Ahora es Greg Focker el que desconfía: Olivia, la prometida de su hijo, no le termina de cerrar."
        )
    );

    private CatalogoDemo() {
    }
}
