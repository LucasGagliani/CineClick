package com.cineclick.repository;

import com.cineclick.model.Compra;
import com.cineclick.model.EstadoCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByClienteId(Long clienteId);

    boolean existsByFuncionId(Long funcionId);

    boolean existsByFuncionSalaId(Long salaId);

    boolean existsByFuncionSalaCineId(Long cineId);

    boolean existsByFuncionPeliculaId(Long peliculaId);

    boolean existsByPromocionId(Long promocionId);

    @Query("select count(c), coalesce(sum(c.total), 0) from Compra c "
        + "where c.estado = com.cineclick.model.EstadoCompra.CONFIRMADA and c.fechaCompra >= :desde and c.fechaCompra < :hasta and (:cineId is null or c.funcion.sala.cine.id = :cineId)")
    List<Object[]> resumenVentas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId);

    @Query("select c.funcion.pelicula.id, c.funcion.pelicula.titulo, sum(c.total) from Compra c "
        + "where c.estado = com.cineclick.model.EstadoCompra.CONFIRMADA and c.fechaCompra >= :desde and c.fechaCompra < :hasta and (:cineId is null or c.funcion.sala.cine.id = :cineId) "
        + "group by c.funcion.pelicula.id, c.funcion.pelicula.titulo")
    List<Object[]> ingresosPorPelicula(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId);

    @Query("select c.funcion.sala.cine.id, c.funcion.sala.cine.nombre, sum(c.total) from Compra c "
        + "where c.estado = com.cineclick.model.EstadoCompra.CONFIRMADA and c.fechaCompra >= :desde and c.fechaCompra < :hasta "
        + "group by c.funcion.sala.cine.id, c.funcion.sala.cine.nombre")
    List<Object[]> ingresosPorCine(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select c.estado, pg.estado, count(c) from Compra c left join c.pago pg "
        + "where c.fechaCompra >= :desde and c.fechaCompra < :hasta and (:cineId is null or c.funcion.sala.cine.id = :cineId) group by c.estado, pg.estado")
    List<Object[]> contarPorEstado(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId);

    @Query("select count(c) from Compra c where c.estado = com.cineclick.model.EstadoCompra.PENDIENTE_PAGO "
        + "and c.fechaCompra >= :desde and c.fechaCompra < :hasta and (:cineId is null or c.funcion.sala.cine.id = :cineId) and c.fechaCompra < :limiteReserva")
    long contarPendientesVencidas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId, @Param("limiteReserva") LocalDateTime limiteReserva);

    @Query("select pr.codigo, count(c), sum(c.total) from Compra c join c.promocion pr "
        + "where c.estado = com.cineclick.model.EstadoCompra.CONFIRMADA and c.fechaCompra >= :desde and c.fechaCompra < :hasta and (:cineId is null or c.funcion.sala.cine.id = :cineId) group by pr.codigo")
    List<Object[]> usosDePromociones(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId);

    List<Compra> findByEstadoAndFechaCompraBefore(EstadoCompra estado, LocalDateTime limite);

    List<Compra> findByFuncionIdAndEstadoAndFechaCompraBefore(Long funcionId, EstadoCompra estado, LocalDateTime limite);
}
