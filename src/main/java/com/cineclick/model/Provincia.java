package com.cineclick.model;

public enum Provincia {
    CABA("Ciudad Autónoma de Buenos Aires", "CABA"),
    BUENOS_AIRES("Buenos Aires", "Buenos Aires"),
    CATAMARCA("Catamarca", "Catamarca"),
    CHACO("Chaco", "Chaco"),
    CHUBUT("Chubut", "Chubut"),
    CORDOBA("Córdoba", "Córdoba"),
    CORRIENTES("Corrientes", "Corrientes"),
    ENTRE_RIOS("Entre Ríos", "Entre Ríos"),
    FORMOSA("Formosa", "Formosa"),
    JUJUY("Jujuy", "Jujuy"),
    LA_PAMPA("La Pampa", "La Pampa"),
    LA_RIOJA("La Rioja", "La Rioja"),
    MENDOZA("Mendoza", "Mendoza"),
    MISIONES("Misiones", "Misiones"),
    NEUQUEN("Neuquén", "Neuquén"),
    RIO_NEGRO("Río Negro", "Río Negro"),
    SALTA("Salta", "Salta"),
    SAN_JUAN("San Juan", "San Juan"),
    SAN_LUIS("San Luis", "San Luis"),
    SANTA_CRUZ("Santa Cruz", "Santa Cruz"),
    SANTA_FE("Santa Fe", "Santa Fe"),
    SANTIAGO_DEL_ESTERO("Santiago del Estero", "Santiago del Estero"),
    TIERRA_DEL_FUEGO("Tierra del Fuego, Antártida e Islas del Atlántico Sur", "Tierra del Fuego"),
    TUCUMAN("Tucumán", "Tucumán");

    private final String nombre;
    private final String nombreCorto;

    Provincia(String nombre, String nombreCorto) {
        this.nombre = nombre;
        this.nombreCorto = nombreCorto;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNombreCorto() {
        return nombreCorto;
    }
}
