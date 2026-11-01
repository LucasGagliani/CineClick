package com.cineclick.controller;

import com.cineclick.dto.MetricasResponseDTO;
import com.cineclick.service.MetricasService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class MetricasController {

    private final MetricasService metricasService;

    @GetMapping("/metricas")
    public MetricasResponseDTO metricas(
        @RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(required = false) Long cineId
    ) {
        metricasService.verificarAdmin(usuarioId);
        return metricasService.calcular(desde, hasta, cineId);
    }
}
