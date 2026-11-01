package com.cineclick.repository;

import com.cineclick.model.EstadoFuncion;
import com.cineclick.model.FormatoFuncion;
import com.cineclick.model.Funcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface FuncionRepository extends JpaRepository<Funcion, Long> {
    List<Funcion> findByPeliculaId(Long peliculaId);

    List<Funcion> findBySalaId(Long salaId);

    boolean existsBySalaId(Long salaId);

    boolean existsBySalaCineId(Long cineId);

    boolean existsByPeliculaId(Long peliculaId);

    boolean existsByEstadoAndFechaHoraInicioAfter(EstadoFuncion estado, LocalDateTime desde);

    boolean existsByPeliculaIdAndEstadoAndFechaHoraInicioAfter(Long peliculaId, EstadoFuncion estado, LocalDateTime desde);

    @Query("select distinct f.pelicula.id from Funcion f where f.estado = com.cineclick.model.EstadoFuncion.PROGRAMADA and f.fechaHoraInicio > :desde")
    Set<Long> peliculasConFuncionesAVenta(@Param("desde") LocalDateTime desde);

    List<Funcion> findByFechaHoraInicioBetween(LocalDateTime desde, LocalDateTime hasta);

    @Query("""
        select f from Funcion f
        join fetch f.pelicula p
        join fetch f.sala s
        join fetch s.cine c
        where f.estado <> com.cineclick.model.EstadoFuncion.CANCELADA
          and f.fechaHoraInicio >= :desde and f.fechaHoraInicio < :hasta
          and (:cineId is null or c.id = :cineId)
        order by f.fechaHoraInicio
        """)
    List<Funcion> buscarParaMetricas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId);

    // Con join fetch viene todo en una sola consulta (sin N+1).
    // Los filtros que llegan en null se ignoran.
    @Query("""
        select f from Funcion f
        join fetch f.pelicula p
        join fetch f.sala s
        join fetch s.cine c
        where (:peliculaId is null or p.id = :peliculaId)
          and (:cineId is null or c.id = :cineId)
          and (:soloCinesActivos = false or c.activo = true)
          and (:formato is null or f.formato = :formato)
          and (cast(:desde as localdatetime) is null or f.fechaHoraInicio >= :desde)
          and (cast(:hasta as localdatetime) is null or f.fechaHoraInicio < :hasta)
        order by f.fechaHoraInicio
        """)
    List<Funcion> buscarConFiltros(
        @Param("peliculaId") Long peliculaId,
        @Param("cineId") Long cineId,
        @Param("soloCinesActivos") boolean soloCinesActivos,
        @Param("formato") FormatoFuncion formato,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta
    );
}
