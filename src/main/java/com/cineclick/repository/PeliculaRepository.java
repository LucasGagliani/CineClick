package com.cineclick.repository;

import com.cineclick.model.Genero;
import com.cineclick.model.Pelicula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PeliculaRepository extends JpaRepository<Pelicula, Long> {
    List<Pelicula> findByActivaTrue();

    List<Pelicula> findByGeneroAndActivaTrue(Genero genero);
}
