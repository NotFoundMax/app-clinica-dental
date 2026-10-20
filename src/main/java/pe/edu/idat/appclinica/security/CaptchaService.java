package pe.edu.idat.appclinica.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * CAPTCHA del registro.
 *
 * Se genera una imagen PNG en el servidor con caracteres rotados y ruido.
 * El código se guarda en la sesión HTTP, nunca en el HTML. El formulario
 * solo envía lo que el usuario escribe y el servidor compara.
 *
 * ¿Por qué en el registro y no en el login?
 *   - El registro es público y permite crear cuentas masivamente con un bot.
 *   - El login ya tiene bloqueo por credenciales y no crea registros nuevos.
 *   - Si el login recibiera mucho tráfico automatizado, también se podría
 *     agregar ahí, pero no es el riesgo principal de este sistema.
 */
@Service
public class CaptchaService {

    public static final String SESSION_CODIGO = "CAPTCHA_CODIGO";
    public static final String SESSION_FECHA = "CAPTCHA_FECHA";

    private static final char[] ALFABETO =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LARGO_CODIGO = 5;
    private static final int ANCHO = 190;
    private static final int ALTO = 64;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.captcha.habilitado:true}")
    private boolean habilitado;

    @Value("${app.captcha.minutos-validez:5}")
    private int minutosValidez;

    public boolean isHabilitado() {
        return habilitado;
    }

    /**
     * Genera el código, lo guarda en la sesión y devuelve la imagen PNG.
     */
    public byte[] generar(HttpSession session) {
        String codigo = generarCodigo();
        session.setAttribute(SESSION_CODIGO, codigo);
        session.setAttribute(SESSION_FECHA, System.currentTimeMillis());
        return dibujar(codigo);
    }

    /**
     * Valida el código escrito por el usuario.
     *
     * El código se consume siempre, incluso si el intento es incorrecto,
     * para que no se pueda reutilizar la misma imagen.
     */
    public boolean validar(String ingresado, HttpSession session) {
        if (!habilitado) {
            return true;
        }
        if (session == null) {
            return false;
        }

        Object codigoGuardado = session.getAttribute(SESSION_CODIGO);
        Object fechaGuardada = session.getAttribute(SESSION_FECHA);

        limpiar(session);

        if (!(codigoGuardado instanceof String codigo)
                || !(fechaGuardada instanceof Long fecha)) {
            return false;
        }

        long transcurrido = System.currentTimeMillis() - fecha;
        if (transcurrido < 0 || transcurrido > minutosValidez * 60_000L) {
            return false;
        }

        if (ingresado == null || ingresado.isBlank()) {
            return false;
        }

        byte[] esperado = codigo.getBytes(StandardCharsets.UTF_8);
        byte[] recibido = ingresado.trim().toUpperCase().getBytes(StandardCharsets.UTF_8);

        // Comparación en tiempo constante.
        return MessageDigest.isEqual(esperado, recibido);
    }

    public void limpiar(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_CODIGO);
            session.removeAttribute(SESSION_FECHA);
        }
    }

    private String generarCodigo() {
        StringBuilder sb = new StringBuilder(LARGO_CODIGO);
        for (int i = 0; i < LARGO_CODIGO; i++) {
            sb.append(ALFABETO[random.nextInt(ALFABETO.length)]);
        }
        return sb.toString();
    }

    private byte[] dibujar(String codigo) {
        BufferedImage imagen = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(new Color(247, 248, 245));
            g.fillRect(0, 0, ANCHO, ALTO);

            // Líneas de ruido.
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 14; i++) {
                g.setColor(colorSuave());
                int x1 = random.nextInt(ANCHO);
                int y1 = random.nextInt(ALTO);
                int x2 = random.nextInt(ANCHO);
                int y2 = random.nextInt(ALTO);
                g.drawLine(x1, y1, x2, y2);
            }

            // Caracteres con rotación y tamaños distintos.
            String[] fuentes = {"SansSerif", "Serif", "Monospaced"};
            int x = 18;
            for (int i = 0; i < codigo.length(); i++) {
                String fuente = fuentes[random.nextInt(fuentes.length)];
                int tamano = 28 + random.nextInt(8);
                g.setFont(new Font(fuente, Font.BOLD, tamano));
                g.setColor(new Color(18 + random.nextInt(35),
                        48 + random.nextInt(35),
                        40 + random.nextInt(35)));

                double angulo = Math.toRadians(-20 + random.nextInt(41));
                AffineTransform original = g.getTransform();
                g.rotate(angulo, x, 42);

                // Pequeña sombra para que el texto no sea plano.
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                g.setColor(new Color(120, 130, 125));
                g.drawString(String.valueOf(codigo.charAt(i)), x + 1, 43);
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

                g.setColor(new Color(18 + random.nextInt(35),
                        48 + random.nextInt(35),
                        40 + random.nextInt(35)));
                g.drawString(String.valueOf(codigo.charAt(i)), x, 42);
                g.setTransform(original);

                x += 31 + random.nextInt(5);
            }

            // Puntos de ruido.
            for (int i = 0; i < 90; i++) {
                g.setColor(colorSuave());
                int px = random.nextInt(ANCHO);
                int py = random.nextInt(ALTO);
                g.fillOval(px, py, 2, 2);
            }

            g.setColor(new Color(205, 213, 208));
            g.drawRect(0, 0, ANCHO - 1, ALTO - 1);

        } finally {
            g.dispose();
        }

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el CAPTCHA", e);
        }
    }

    private Color colorSuave() {
        int base = 150 + random.nextInt(70);
        return new Color(base, base + 8, base + 2);
    }
}
