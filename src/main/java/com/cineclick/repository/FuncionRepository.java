package com.cineclick.repository;

import com.cineclick.model.Funcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FuncionRepository extends JpaRepository<Funcion, Long> {
    List<Funcion> findByPeliculaId(Long peliculaId);

    List<Funcion> findBySalaId(Long salaId);

    List<Funcion> findByFechaHoraInicioBetween(LocalDateTime desde, LocalDateTime hasta);
}
