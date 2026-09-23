package com.cineclick.repository;

import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Funcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FuncionRepository extends JpaRepository<Funcion, Long> {
    List<Funcion> findByPeliculaId(Long peliculaId);

    List<Funcion> findBySalaId(Long salaId);

    List<Funcion> findByFechaHoraInicioBetween(LocalDateTime desde, LocalDateTime hasta);

    // Con join fetch viene todo en una sola consulta (sin N+1).
    // Los filtros que llegan en null se ignoran.
    @Query("""
        select f from Funcion f
        join fetch f.pelicula p
        join fetch f.sala s
        join fetch s.cine c
        where (:peliculaId is null or p.id = :peliculaId)
          and (:cineId is null or c.id = :cineId)
          and (:ciudad is null or lower(c.ciudad) = lower(:ciudad))
          and (:formato is null or f.formato = :formato)
          and (cast(:desde as localdatetime) is null or f.fechaHoraInicio >= :desde)
          and (cast(:hasta as localdatetime) is null or f.fechaHoraInicio < :hasta)
        order by f.fechaHoraInicio
        """)
    List<Funcion> buscarConFiltros(
        @Param("peliculaId") Long peliculaId,
        @Param("cineId") Long cineId,
        @Param("ciudad") String ciudad,
        @Param("formato") FormatoFuncion formato,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta
    );
}
