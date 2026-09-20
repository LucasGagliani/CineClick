package com.cineclick.repository;

import com.cineclick.model.Entrada;
import com.cineclick.model.EstadoEntrada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EntradaRepository extends JpaRepository<Entrada, Long> {
    List<Entrada> findByFuncionId(Long funcionId);

    List<Entrada> findByCompraId(Long compraId);

    boolean existsByFuncionIdAndButacaIdAndEstadoNot(Long funcionId, Long butacaId, EstadoEntrada estado);
}
