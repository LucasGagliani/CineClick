# Configurar el envío de emails de CineClick

El backend usa Gmail por SMTP. No guardes la contraseña ni la contraseña de aplicación en este repositorio.

## 1. Preparar Gmail
1. Activá la verificación en dos pasos en la cuenta Gmail que enviará los correos.
2. Generá una contraseña de aplicación desde la configuración de seguridad de Google.
3. Usá esa contraseña de aplicación (no la contraseña normal de Gmail) como `MAIL_PASSWORD`.

## 2. Configurar variables en Windows PowerShell

Abrí PowerShell y ejecutá estos comandos, reemplazando los valores por los tuyos:

```powershell
$env:MAIL_HOST = "smtp.gmail.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "tu-cuenta@gmail.com"
$env:MAIL_PASSWORD = "TU_CONTRASENA_DE_APLICACION"
$env:CINECLICK_FRONTEND_URL = "http://localhost:5173"
```

Estas variables quedan configuradas en esa ventana de PowerShell. Iniciá el backend desde esa misma ventana:

```powershell
cd C:\Proyectos\CineClick
.\mvnw.cmd spring-boot:run
```

No pegues tu contraseña de aplicación en el chat ni la agregues a `application.properties`, `.env` o Git.

## 3. Probar
- En el login, elegí “¿Olvidaste tu contraseña?” e ingresá un email registrado.
- El enlace vence a los 30 minutos y se puede usar una sola vez.
- Después de un pago aprobado, el backend envía un PDF al email registrado.
- El PDF está identificado como comprobante interno y no como factura fiscal.

Si no configurás Gmail, el resto del backend puede arrancar, pero los correos no se van a poder enviar.
