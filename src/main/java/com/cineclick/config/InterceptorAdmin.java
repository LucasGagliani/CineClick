package com.cineclick.config;

import com.cineclick.service.AccesoAdmin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class InterceptorAdmin implements HandlerInterceptor {

    public static final String CABECERA_USUARIO = "X-Usuario-Id";

    private final AccesoAdmin accesoAdmin;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod metodo && requiereAdmin(metodo)) {
            accesoAdmin.verificar(usuarioDe(request));
        }
        return true;
    }

    private static boolean requiereAdmin(HandlerMethod metodo) {
        return metodo.hasMethodAnnotation(SoloAdmin.class) || metodo.getBeanType().isAnnotationPresent(SoloAdmin.class);
    }

    private static Long usuarioDe(HttpServletRequest request) {
        String valor = request.getHeader(CABECERA_USUARIO);
        if (valor == null || !valor.trim().matches("\\d{1,18}")) {
            return null;
        }
        return Long.valueOf(valor.trim());
    }
}
