package com.cineclick.repository;

import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query("update Entrada e set e.estado = com.cineclick.model.EstadoEntrada.USADA "
        + "where e.id = :id and e.funcion.id = :funcionId and e.estado = com.cineclick.model.EstadoEntrada.EMITIDA")
    int marcarUsadaSiEstaEmitida(@Param("id") Long id, @Param("funcionId") Long funcionId);
}
