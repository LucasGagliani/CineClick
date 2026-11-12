package com.cineclick.service;

import com.cineclick.model.Pago;
import com.cineclick.repository.PagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComprobanteEmailListener {
    private final PagoRepository pagoRepository;
    private final ComprobantePdfService pdfService;
    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enviar(PagoAprobadoEvent evento) {
        Pago pago = pagoRepository.findById(evento.pagoId()).orElse(null);
        if (pago == null || pago.getEstado() != com.cineclick.model.EstadoPago.APROBADO || pago.getComprobanteEnviadoEn() != null) return;
        String email = pago.getCompra().getCliente().getUsuario().getEmail();
        try {
            byte[] pdf = pdfService.generar(pago);
            emailService.enviarAdjunto(email, "Tu comprobante de CineClick - Compra " + pago.getCompra().getId(),
                "¡Gracias por tu compra! Adjuntamos tu comprobante interno en PDF. Este documento no es una factura fiscal.",
                pdf, "comprobante-cineclick-" + pago.getCompra().getId() + ".pdf");
            pago.setComprobanteEnviadoEn(LocalDateTime.now());
            pagoRepository.save(pago);
        } catch (Exception e) {
            log.error("No se pudo enviar el comprobante del pago {}. Revisar la configuracion SMTP y los logs del backend.", pago.getId(), e);
        }
    }
}
