package com.cineclick.service;

import com.cineclick.model.Compra;
import com.cineclick.model.Pago;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class ComprobantePdfService {
    public byte[] generar(Pago pago) {
        Compra compra = pago.getCompra();
        try (PDDocument documento = new PDDocument(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            try (PDPageContentStream cs = new PDPageContentStream(documento, pagina)) {
                texto(cs, 55, 740, 20, "CineClick - Comprobante de pago");
                texto(cs, 55, 710, 11, "COMPROBANTE INTERNO - NO ES FACTURA FISCAL");
                texto(cs, 55, 670, 12, "Compra: " + compra.getId());
                texto(cs, 55, 645, 12, "Fecha: " + pago.getFechaPago().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                texto(cs, 55, 620, 12, "Pelicula: " + seguro(compra.getFuncion().getPelicula().getTitulo()));
                texto(cs, 55, 595, 12, "Cine: " + seguro(compra.getFuncion().getSala().getCine().getNombre()));
                texto(cs, 55, 570, 12, "Entradas: " + compra.getEntradas().size());
                texto(cs, 55, 545, 12, "Medio de pago: " + pago.getMetodoPago());
                texto(cs, 55, 520, 12, "Operacion: " + seguro(pago.getCodigoTransaccion()));
                texto(cs, 55, 480, 15, "TOTAL PAGADO: $" + compra.getTotal().toPlainString());
                texto(cs, 55, 430, 10, "Conserva este comprobante como constancia de tu compra.");
            }
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el comprobante PDF", e);
        }
    }
    private static void texto(PDPageContentStream cs, float x, float y, int size, String valor) throws IOException {
        cs.beginText(); cs.setFont(PDType1Font.HELVETICA, size); cs.newLineAtOffset(x, y);
        cs.showText(seguro(valor)); cs.endText();
    }
    private static String seguro(String valor) {
        if (valor == null) return "-";
        return valor.replaceAll("[^\\x20-\\x7E]", " ").replaceAll("[\\r\\n]", " ");
    }
}
