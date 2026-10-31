package com.cineclick.repository;

import com.cineclick.model.Compra;
import com.cineclick.model.EstadoCompra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findByClienteId(Long clienteId);

    List<Compra> findByEstadoAndFechaCompraBefore(EstadoCompra estado, LocalDateTime limite);

    List<Compra> findByFuncionIdAndEstadoAndFechaCompraBefore(Long funcionId, EstadoCompra estado, LocalDateTime limite);
}
