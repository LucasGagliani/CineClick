package com.cineclick.model;

import com.cineclick.service.Normalizador;

import java.util.List;
import java.util.Optional;

public final class BarriosCaba {

    public static final List<String> TODOS = List.of(
        "Agronomía", "Almagro", "Balvanera", "Barracas", "Belgrano", "Boedo", "Caballito", "Chacarita",
        "Coghlan", "Colegiales", "Constitución", "Flores", "Floresta", "La Boca", "La Paternal", "Liniers",
        "Mataderos", "Monte Castro", "Monserrat", "Nueva Pompeya", "Núñez", "Palermo", "Parque Avellaneda",
        "Parque Chacabuco", "Parque Chas", "Parque Patricios", "Puerto Madero", "Recoleta", "Retiro", "Saavedra",
        "San Cristóbal", "San Nicolás", "San Telmo", "Vélez Sarsfield", "Versalles", "Villa Crespo",
        "Villa del Parque", "Villa Devoto", "Villa General Mitre", "Villa Lugano", "Villa Luro", "Villa Ortúzar",
        "Villa Pueyrredón", "Villa Real", "Villa Riachuelo", "Villa Santa Rita", "Villa Soldati", "Villa Urquiza"
    );

    private BarriosCaba() {
    }

    public static Optional<String> buscar(String texto) {
        String clave = Normalizador.clave(texto);
        return TODOS.stream().filter(barrio -> Normalizador.clave(barrio).equals(clave)).findFirst();
    }
}
