package com.cineclick.controller;

import com.cineclick.dto.CompraRequestDTO;
import com.cineclick.dto.CompraResponseDTO;
import com.cineclick.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompraResponseDTO crear(@Valid @RequestBody CompraRequestDTO dto) {
        return compraService.crearCompra(dto);
    }

    @GetMapping("/{id}")
    public CompraResponseDTO obtener(@PathVariable Long id) {
        return compraService.buscarPorId(id);
    }

    @PatchMapping("/{id}/cancelar")
    public CompraResponseDTO cancelar(@PathVariable Long id) {
        return compraService.cancelar(id);
    }
}
