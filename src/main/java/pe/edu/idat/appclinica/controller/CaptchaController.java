package pe.edu.idat.appclinica.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import pe.edu.idat.appclinica.security.CaptchaService;

@Controller
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * Devuelve una imagen PNG nueva en cada solicitud.
     * El navegador no debe cachearla.
     */
    @GetMapping("/captcha")
    public ResponseEntity<byte[]> captcha(HttpSession session) {
        byte[] imagen = captchaService.generar(session);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_PNG)
                .body(imagen);
    }
}
