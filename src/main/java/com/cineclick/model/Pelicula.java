package com.cineclick.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "peliculas")
public class Pelicula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 1200)
    private String sinopsis;

    @Column(nullable = false)
    private Integer duracionMinutos;

    @Column(nullable = false, length = 20)
    private String clasificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Genero genero;

    @Column(nullable = false, length = 40)
    private String idioma;

    @ElementCollection(targetClass = FormatoFuncion.class)
    @CollectionTable(name = "pelicula_formatos", joinColumns = @JoinColumn(name = "pelicula_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "formato", nullable = false)
    private Set<FormatoFuncion> formatosDisponibles = new HashSet<>();

    @Column(length = 500)
    private String urlPoster;

    private LocalDate fechaEstreno;

    @Column(nullable = false)
    private boolean activa = true;

    public boolean estaDisponible() {
        return activa;
    }

    public void validarDuracion() {
        if (duracionMinutos == null || duracionMinutos <= 0) {
            throw new IllegalArgumentException("La duracion debe ser mayor a cero");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public Integer getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(Integer duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public String getClasificacion() {
        return clasificacion;
    }

    public void setClasificacion(String clasificacion) {
        this.clasificacion = clasificacion;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public Set<FormatoFuncion> getFormatosDisponibles() {
        return formatosDisponibles;
    }

    public void setFormatosDisponibles(Set<FormatoFuncion> formatosDisponibles) {
        this.formatosDisponibles = formatosDisponibles;
    }

    public String getUrlPoster() {
        return urlPoster;
    }

    public void setUrlPoster(String urlPoster) {
        this.urlPoster = urlPoster;
    }

    public LocalDate getFechaEstreno() {
        return fechaEstreno;
    }

    public void setFechaEstreno(LocalDate fechaEstreno) {
        this.fechaEstreno = fechaEstreno;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
