package com.cineclick.repository;

import com.cineclick.model.SolicitudArrepentimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolicitudArrepentimientoRepository extends JpaRepository<SolicitudArrepentimiento, Long> {

    Optional<SolicitudArrepentimiento> findByCompraId(Long compraId);

    boolean existsByCodigo(String codigo);

    List<SolicitudArrepentimiento> findAllByOrderByFechaDesc();
}
