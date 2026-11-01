package com.cineclick.repository;

import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    List<Entrada> findByFuncionId(Long funcionId);

    List<Entrada> findByCompraId(Long compraId);

    boolean existsByFuncionIdAndButacaIdAndEstadoNot(Long funcionId, Long butacaId, EstadoEntrada estado);

    boolean existsByCodigoQr(String codigoQr);

    boolean existsByButacaSalaId(Long salaId);

    Optional<Entrada> findByCodigoQrIgnoreCase(String codigoQr);

    @Query("select e.funcion.id, count(e) from Entrada e where e.vigente = true and e.funcion.id in :funcionIds group by e.funcion.id")
    List<Object[]> contarVigentesPorFuncion(@Param("funcionIds") Collection<Long> funcionIds);

    @Query("select count(e) from Entrada e where e.compra.estado = com.cineclick.model.EstadoCompra.CONFIRMADA and e.compra.fechaCompra >= :desde and e.compra.fechaCompra < :hasta and (:cineId is null or e.funcion.sala.cine.id = :cineId)")
    long contarVendidas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId);

    @Query("select e.funcion.pelicula.id, count(e) from Entrada e "
        + "where e.compra.estado = com.cineclick.model.EstadoCompra.CONFIRMADA and e.compra.fechaCompra >= :desde and e.compra.fechaCompra < :hasta and (:cineId is null or e.funcion.sala.cine.id = :cineId) group by e.funcion.pelicula.id")
    List<Object[]> vendidasPorPelicula(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId);

    @Query("select e.funcion.sala.cine.id, count(e) from Entrada e "
        + "where e.compra.estado = com.cineclick.model.EstadoCompra.CONFIRMADA and e.compra.fechaCompra >= :desde and e.compra.fechaCompra < :hasta group by e.funcion.sala.cine.id")
    List<Object[]> vendidasPorCine(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("select e.tipo, count(e), sum(e.precioUnitario) from Entrada e "
        + "where e.compra.estado = com.cineclick.model.EstadoCompra.CONFIRMADA and e.compra.fechaCompra >= :desde and e.compra.fechaCompra < :hasta and (:cineId is null or e.funcion.sala.cine.id = :cineId) group by e.tipo")
    List<Object[]> vendidasPorTipo(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId);

    @Query("select e.compra.promocion.codigo, sum(e.precioUnitario) from Entrada e "
        + "where e.compra.estado = com.cineclick.model.EstadoCompra.CONFIRMADA and e.compra.fechaCompra >= :desde and e.compra.fechaCompra < :hasta and (:cineId is null or e.funcion.sala.cine.id = :cineId) and e.compra.promocion is not null "
        + "group by e.compra.promocion.codigo")
    List<Object[]> precioDeListaPorPromocion(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId);

    @Query("select e.funcion.id, count(e), sum(case when e.estado = com.cineclick.model.EstadoEntrada.USADA then 1 else 0 end) "
        + "from Entrada e where e.compra.estado = com.cineclick.model.EstadoCompra.CONFIRMADA "
        + "and e.funcion.fechaHoraInicio >= :desde and e.funcion.fechaHoraInicio < :hasta and (:cineId is null or e.funcion.sala.cine.id = :cineId) "
        + "group by e.funcion.id")
    List<Object[]> vendidasYUsadasPorFuncion(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("cineId") Long cineId);

    @Modifying
    @Query("update Entrada e set e.estado = com.cineclick.model.EstadoEntrada.USADA "
        + "where e.id = :id and e.funcion.id = :funcionId and e.estado = com.cineclick.model.EstadoEntrada.EMITIDA")
    int marcarUsadaSiEstaEmitida(@Param("id") Long id, @Param("funcionId") Long funcionId);
}
