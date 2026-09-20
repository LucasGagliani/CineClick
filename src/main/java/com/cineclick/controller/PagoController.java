package com.cineclick.controller;

import com.cineclick.dto.PagoRequestDTO;
import com.cineclick.dto.PagoResponseDTO;
import com.cineclick.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @PostMapping("/compras/{compraId}/pagos")
    @ResponseStatus(HttpStatus.CREATED)
    public PagoResponseDTO registrarPago(@PathVariable Long compraId, @Valid @RequestBody PagoRequestDTO dto) {
        return pagoService.registrarPago(compraId, dto);
    }

    @GetMapping("/pagos/{id}")
    public PagoResponseDTO obtener(@PathVariable Long id) {
        return pagoService.buscarPorId(id);
    }
}
