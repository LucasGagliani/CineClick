package com.cineclick.controller;

import com.cineclick.dto.ClienteRequestDTO;
import com.cineclick.dto.ClienteResponseDTO;
import com.cineclick.dto.CompraResponseDTO;
import com.cineclick.service.ClienteService;
import com.cineclick.service.CompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final CompraService compraService;

    @GetMapping
    public List<ClienteResponseDTO> listar() {
        return clienteService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponseDTO registrar(@Valid @RequestBody ClienteRequestDTO dto) {
        return clienteService.registrar(dto);
    }

    @GetMapping("/{id}")
    public ClienteResponseDTO obtener(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ClienteResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequestDTO dto) {
        return clienteService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
    }

    @GetMapping("/{id}/compras")
    public List<CompraResponseDTO> listarCompras(@PathVariable Long id) {
        return compraService.listarPorCliente(id);
    }
}
