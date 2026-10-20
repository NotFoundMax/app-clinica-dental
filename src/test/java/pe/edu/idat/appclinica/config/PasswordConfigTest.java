package pe.edu.idat.appclinica.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordConfigTest {

    private final PasswordEncoder encoder = new PasswordConfig().passwordEncoder();

    @Test
    void generaHashArgon2idYValidaLaContrasena() {
        String hash = encoder.encode("ClaveSegura123!");

        assertNotEquals("ClaveSegura123!", hash);
        assertTrue(hash.startsWith("$argon2id$"));
        assertTrue(encoder.matches("ClaveSegura123!", hash));
        assertFalse(encoder.matches("OtraClave123!", hash));
    }
}
