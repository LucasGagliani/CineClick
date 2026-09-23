package com.cineclick.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "cines")
public class Cine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 160)
    private String direccion;

    @Column(nullable = false, length = 80)
    private String ciudad;

    @Column(length = 40)
    private String telefono;

    @Column(nullable = false)
    private boolean activo = true;

    @OneToMany(mappedBy = "cine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Sala> salas = new ArrayList<>();

    public void agregarSala(Sala sala) {
        sala.setCine(this);
        salas.add(sala);
    }

    public List<Sala> obtenerSalasActivas() {
        return salas.stream().filter(Sala::isActiva).toList();
    }
}
