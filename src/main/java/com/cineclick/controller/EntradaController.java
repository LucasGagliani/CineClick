package com.cineclick.controller;

import com.cineclick.config.SoloAdmin;
import com.cineclick.dto.ValidacionEntradaResponseDTO;
import com.cineclick.dto.ValidarEntradaRequestDTO;
import com.cineclick.service.EntradaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/entradas")
@RequiredArgsConstructor
public class EntradaController {

    private final EntradaService entradaService;

    @SoloAdmin
    @PostMapping("/{codigo}/validar")
    public ValidacionEntradaResponseDTO validar(@PathVariable String codigo, @Valid @RequestBody ValidarEntradaRequestDTO dto) {
        return entradaService.validar(codigo, dto.cineId());
    }
}
