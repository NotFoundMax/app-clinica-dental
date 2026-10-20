package pe.edu.idat.appclinica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración del hash de contraseñas.
 *
 * Argon2id es el algoritmo recomendado por OWASP para contraseñas nuevas.
 * Parámetros usados:
 *   - sal de 16 bytes
 *   - hash de 32 bytes
 *   - paralelismo 1
 *   - memoria 16 MB (16384 KB)
 *   - 3 iteraciones
 *
 * BouncyCastle es la implementación que usa Spring Security por dentro.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 16_384, 3);
    }
}
