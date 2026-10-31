package com.cineclick.service;

import com.cineclick.dto.ListaEsperaResponseDTO;
import com.cineclick.exception.ReglaNegocioException;
import com.cineclick.model.CategoriaDestacada;
import com.cineclick.model.Cliente;
import com.cineclick.model.ListaEspera;
import com.cineclick.model.Pelicula;
import com.cineclick.repository.ListaEsperaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListaEsperaService {

    private final ListaEsperaRepository listaEsperaRepository;
    private final ClienteService clienteService;
    private final PeliculaService peliculaService;

    @Transactional
    public ListaEsperaResponseDTO anotar(Long peliculaId, Long clienteId) {
        Pelicula pelicula = peliculaService.buscarEntidad(peliculaId);
        Cliente cliente = clienteService.buscarEntidad(clienteId);
        if (peliculaService.categoriaDe(pelicula) != CategoriaDestacada.PROXIMAMENTE) {
            throw new ReglaNegocioException("Solo se puede pedir aviso para peliculas que todavia no estan a la venta");
        }
        ListaEspera aviso = listaEsperaRepository.findByClienteIdAndPeliculaId(clienteId, peliculaId)
            .orElseGet(() -> {
                ListaEspera nuevo = new ListaEspera();
                nuevo.setCliente(cliente);
                nuevo.setPelicula(pelicula);
                return listaEsperaRepository.save(nuevo);
            });
        return toResponse(aviso);
    }

    @Transactional
    public void quitar(Long peliculaId, Long clienteId) {
        listaEsperaRepository.findByClienteIdAndPeliculaId(clienteId, peliculaId)
            .ifPresent(listaEsperaRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ListaEsperaResponseDTO> listarPorCliente(Long clienteId) {
        clienteService.buscarEntidad(clienteId);
        return listaEsperaRepository.findByClienteIdOrderByFechaAltaDesc(clienteId).stream()
            .map(ListaEsperaService::toResponse)
            .toList();
    }

    private static ListaEsperaResponseDTO toResponse(ListaEspera aviso) {
        return new ListaEsperaResponseDTO(
            aviso.getPelicula().getId(),
            aviso.getPelicula().getTitulo(),
            aviso.getPelicula().getFechaEstreno(),
            aviso.getCliente().getId(),
            aviso.getFechaAlta()
        );
    }
}
