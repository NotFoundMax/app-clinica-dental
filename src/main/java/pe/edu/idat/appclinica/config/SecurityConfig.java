package pe.edu.idat.appclinica.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import pe.edu.idat.appclinica.security.AuditoriaLoginFailureHandler;
import pe.edu.idat.appclinica.security.AuditoriaLoginSuccessHandler;
import pe.edu.idat.appclinica.security.AuditoriaLogoutSuccessHandler;

/**
 * Reglas de acceso de la aplicación.
 *
 * Público:
 *   - login y registro
 *   - imagen del CAPTCHA
 *   - CSS, JS y favicon
 *   - página de acceso denegado
 *
 * Privado:
 *   - /citas/** para pacientes y administradores
 *   - /admin/** solo para ADMIN
 *   - cualquier otra ruta requiere sesión
 *
 * CSRF queda activo (valor por defecto de Spring Security).
 */
@Configuration
public class SecurityConfig {

    private final AuditoriaLoginSuccessHandler loginSuccessHandler;
    private final AuditoriaLoginFailureHandler loginFailureHandler;
    private final AuditoriaLogoutSuccessHandler logoutSuccessHandler;

    public SecurityConfig(AuditoriaLoginSuccessHandler loginSuccessHandler,
                          AuditoriaLoginFailureHandler loginFailureHandler,
                          AuditoriaLogoutSuccessHandler logoutSuccessHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/auth/login",
                                "/auth/frmRegistroUsuario",
                                "/auth/guardarUsuario",
                                "/captcha",
                                "/acceso-denegado",
                                "/css/**",
                                "/js/**",
                                "/favicon.svg",
                                "/error",
                                "/error/**"
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/citas/**").hasAnyRole("PACIENTE", "ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .usernameParameter("correo")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/acceso-denegado")
                )
                .csrf(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(Customizer.withDefaults())
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                );

        return http.build();
    }
}
