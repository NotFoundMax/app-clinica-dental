document.addEventListener('DOMContentLoaded', () => {
    // Renueva la imagen del CAPTCHA sin recargar la página.
    document.querySelectorAll('[data-captcha-refresh]').forEach((boton) => {
        boton.addEventListener('click', () => {
            const imagen = document.getElementById('captcha-img');
            if (!imagen) {
                return;
            }
            const base = imagen.dataset.captchaUrl || '/captcha';
            const separador = base.includes('?') ? '&' : '?';
            imagen.src = base + separador + 't=' + Date.now();

            const campo = document.getElementById('captcha');
            if (campo) {
                campo.value = '';
                campo.focus();
            }
        });
    });

    // El código del CAPTCHA se muestra en mayúsculas.
    const campoCaptcha = document.getElementById('captcha');
    if (campoCaptcha) {
        campoCaptcha.addEventListener('input', () => {
            campoCaptcha.value = campoCaptcha.value.toUpperCase();
        });
    }

    // Confirmación antes de cancelar una cita o cambiar un estado.
    document.querySelectorAll('form[data-confirm]').forEach((formulario) => {
        formulario.addEventListener('submit', (evento) => {
            const mensaje = formulario.getAttribute('data-confirm');
            if (mensaje && !window.confirm(mensaje)) {
                evento.preventDefault();
            }
        });
    });
});
