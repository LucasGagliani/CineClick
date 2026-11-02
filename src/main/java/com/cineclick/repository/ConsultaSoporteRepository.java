package com.cineclick.repository;

import com.cineclick.model.ConsultaSoporte;
import com.cineclick.model.EstadoConsulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultaSoporteRepository extends JpaRepository<ConsultaSoporte, Long> {

    boolean existsByNumero(String numero);

    Optional<ConsultaSoporte> findByClaveIdempotencia(String claveIdempotencia);

    List<ConsultaSoporte> findAllByOrderByFechaDesc();

    List<ConsultaSoporte> findByEstadoOrderByFechaDesc(EstadoConsulta estado);

    List<ConsultaSoporte> findByCompraIdOrderByFechaDesc(Long compraId);
}
