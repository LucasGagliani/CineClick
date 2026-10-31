package com.cineclick.repository;

import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    List<Entrada> findByFuncionId(Long funcionId);

    List<Entrada> findByCompraId(Long compraId);

    boolean existsByFuncionIdAndButacaIdAndEstadoNot(Long funcionId, Long butacaId, EstadoEntrada estado);

    @Query("select e.funcion.id, count(e) from Entrada e where e.vigente = true and e.funcion.id in :funcionIds group by e.funcion.id")
    List<Object[]> contarVigentesPorFuncion(@Param("funcionIds") Collection<Long> funcionIds);

}
