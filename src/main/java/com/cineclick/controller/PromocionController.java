package com.cineclick.controller;

import com.cineclick.dto.PromocionRequestDTO;
import com.cineclick.dto.PromocionResponseDTO;
import com.cineclick.service.PromocionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/promociones")
public class PromocionController {

    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    @GetMapping
    public List<PromocionResponseDTO> listarActivas() {
        return promocionService.listarActivas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PromocionResponseDTO crear(@Valid @RequestBody PromocionRequestDTO dto) {
        return promocionService.crear(dto);
    }
}
