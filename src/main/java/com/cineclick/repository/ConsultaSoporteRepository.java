package com.cineclick.repository;

import com.cineclick.model.ConsultaSoporte;
import com.cineclick.model.EstadoConsulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsultaSoporteRepository extends JpaRepository<ConsultaSoporte, Long> {
    @Query("select c from ConsultaSoporte c left join c.compra co where lower(c.email) = lower(:email) or co.cliente.id = :clienteId order by c.fecha desc")
    List<ConsultaSoporte> buscarDelUsuario(@Param("email") String email, @Param("clienteId") Long clienteId);


    boolean existsByNumero(String numero);

    Optional<ConsultaSoporte> findByClaveIdempotencia(String claveIdempotencia);

    List<ConsultaSoporte> findAllByOrderByFechaDesc();

    List<ConsultaSoporte> findByEstadoOrderByFechaDesc(EstadoConsulta estado);

    List<ConsultaSoporte> findByCompraIdOrderByFechaDesc(Long compraId);
}
