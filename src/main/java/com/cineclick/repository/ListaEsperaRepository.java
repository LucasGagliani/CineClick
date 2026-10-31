package com.cineclick.repository;

import com.cineclick.model.ListaEspera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListaEsperaRepository extends JpaRepository<ListaEspera, Long> {
    Optional<ListaEspera> findByClienteIdAndPeliculaId(Long clienteId, Long peliculaId);

    List<ListaEspera> findByClienteIdOrderByFechaAltaDesc(Long clienteId);
}
