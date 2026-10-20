# Clínica Dental · Sistema de citas

Aplicación web de una clínica odontológica para que un paciente se registre,
inicie sesión, reserve una cita con un especialista y gestione su agenda. El
personal administrativo confirma, atiende o cancela citas desde un panel, y
consulta un registro de auditoría.

Proyecto del curso **8C003 Desarrollo de Aplicaciones Seguras (IDAT)**.

## Stack

- Java 17+
- Spring Boot 3.5.3
- Spring MVC + Thymeleaf
- Spring Security 6
- Spring Data JPA + Hibernate
- MySQL 8
- Monolito, sin frontend separado

## Requisitos

1. JDK 17 o superior.
2. MySQL 8 en `localhost:3306`.
3. IntelliJ IDEA (recomendado) o Maven 3.9+.

## 1. Preparar la base de datos

La aplicación usa la base `clinica_dental`. Hay dos caminos:

### Opción A: ejecutar el script (recomendado para la entrega)

Desde la raíz del proyecto:

```bash
mysql -u root -p < src/main/resources/db/scripts.sql
```

El script crea la base, las siete tablas, los roles, cuatro odontólogos, seis
tratamientos y los bloques de horario. Es idempotente: se puede ejecutar más de
una vez.

### Opción B: dejar que la aplicación la cree

La URL JDBC incluye `createDatabaseIfNotExist=true` y Hibernate tiene
`ddl-auto=update`. Al arrancar, la aplicación crea la base y las tablas si no
existen. Después, `DataInitializer` carga los datos mínimos.

En ambos casos, al primer arranque se crean las cuentas de demostración.

## 2. Configurar la conexión

La configuración está en `src/main/resources/application.properties`:

```properties
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASSWORD:}
```

El proyecto **no incluye la contraseña de MySQL**. Antes de arrancar, define
`DB_PASSWORD` con la clave de tu servidor local:

```bash
export DB_USER=root
export DB_PASSWORD=tu_password
mvn spring-boot:run
```

En IntelliJ, agrega `DB_PASSWORD=tu_password` en
`Run > Edit Configurations > Environment variables`. No escribas la contraseña
en `application.properties` ni en ningún archivo que vaya dentro del zip.

## 3. Ejecutar

En IntelliJ IDEA:

1. Abre la carpeta `app-clinica-dental` como proyecto Maven.
2. Espera a que se descarguen las dependencias.
3. Ejecuta `pe.edu.idat.appclinica.AppClinicaApplication`.
4. Abre `http://localhost:8080`.

Con Maven, desde la raíz:

```bash
mvn spring-boot:run
```

Si el puerto 8080 está ocupado por la aplicación anterior, usa otro puerto:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## Cuentas de demostración

Se crean automáticamente en el primer arranque. Son datos de demostración, no
credenciales de producción.

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | `admin@dentalcare.pe` | `Admin123!` |
| Paciente | `paciente@demo.pe` | `Paciente123!` |

El registro público solo crea cuentas con rol `PACIENTE`. Los usuarios con rol
`ADMIN` se crean desde el servidor, no desde el formulario.

## Flujo para probar

1. Entra a `/auth/frmRegistroUsuario`.
2. Completa el formulario y escribe el código de la imagen CAPTCHA.
3. Inicia sesión con la cuenta creada.
4. En `Agendar`, elige especialista, tratamiento y fecha, y pulsa `Ver horarios`.
5. Selecciona un horario y confirma la cita.
6. Revisa `Mis citas` y el `Calendario`.
7. Cancela una cita futura desde `Mis citas`.
8. Entra con la cuenta de administrador y abre `Administración`.
9. Confirma una cita pendiente y luego marca una cita confirmada como atendida.
10. Revisa `Auditoría`.

## Decisiones de seguridad

### CAPTCHA en el registro

El registro es público y permite crear cuentas de forma masiva. Ese es el punto
donde un bot puede generar cientos de cuentas. Por eso el CAPTCHA está en el
registro, no en el login.

El login no crea registros nuevos y ya tiene un mensaje de error genérico
(`Correo o contraseña incorrectos`) para no revelar si el correo existe. Si el
login recibiera mucho tráfico automatizado, también se podría agregar CAPTCHA
ahí, pero no es el riesgo principal de este sistema.

La implementación:

- Genera un PNG real en el servidor con Java AWT (`BufferedImage`).
- Guarda el código en la sesión HTTP, nunca en el HTML.
- Vence a los 5 minutos.
- Se consume en el primer intento, sea correcto o incorrecto.
- Compara con `MessageDigest.isEqual` para evitar comparaciones de tiempo variable.
- Se puede desactivar solo para pruebas con `--app.captcha.habilitado=false`.
  En la entrega debe quedar activo.

### Contraseñas con Argon2id

`PasswordConfig` usa `Argon2PasswordEncoder` con:

- sal de 16 bytes
- hash de 32 bytes
- paralelismo 1
- 16 MB de memoria
- 3 iteraciones

Se agregó `bcprov-jdk18on` de forma explícita porque el BOM de Spring Security
no administra la versión de BouncyCastle que necesita Argon2.

Las contraseñas nunca se guardan en texto plano ni se registran en el log. Los
campos de contraseña del formulario no usan `th:field`, para no reescribir la
clave en el HTML cuando hay un error de validación.

### Validación y escape

- Todos los formularios usan `@Valid` y anotaciones de Jakarta Validation en los
  DTO: `@NotBlank`, `@Email`, `@Size`, `@Pattern`, `@FutureOrPresent`,
  `@Min`, `@Max`.
- Thymeleaf usa `th:text` para mostrar datos. No se usa `th:utext`, por lo que
  un nombre o un motivo con etiquetas HTML se muestra como texto, no se ejecuta.
- Las consultas usan Spring Data JPA y JPQL con parámetros. No hay SQL
  concatenado con datos del usuario.
- Spring Security mantiene CSRF activo en los formularios POST.

### IDOR y reglas de negocio en el servidor

- El paciente de una cita se obtiene de la sesión, nunca de un campo oculto del
  formulario.
- Al cancelar, `CitaService` verifica que la cita pertenezca al paciente
  autenticado. Si no, responde 403.
- Al reservar, el servidor vuelve a calcular la disponibilidad. Si alguien altera
  el HTML y envía un horario ocupado, la operación falla con un mensaje claro.
- Las transiciones de estado son estrictas: `PENDIENTE` puede pasar a
  `CONFIRMADA` o `CANCELADA`; `CONFIRMADA` a `ATENDIDA` o `CANCELADA`;
  `ATENDIDA` y `CANCELADA` son finales.

### Agenda

- Los horarios se generan en bloques de 30 minutos.
- Se descartan los que no caben completos dentro del bloque de atención.
- Se descartan los que se cruzan con citas activas del odontólogo.
- Se descartan los que ya pasaron si la fecha es hoy.
- Solo se permite reservar hasta 90 días adelante.
- La zona horaria se fija en `America/Lima`.

### Auditoría

Cada evento se escribe en dos lugares:

1. Un renglón JSON en el log, con el logger `AUDITORIA`.
2. La tabla `auditoria_evento`, visible en el panel de administración.

Se registran accesos, cierres de sesión, intentos fallidos, errores de CAPTCHA,
altas de catálogo, cambios de estado y cancelaciones.

Lo que **no** se registra:

- contraseñas ni hashes
- tokens o identificadores de sesión
- correos completos
- teléfonos completos
- el texto libre del motivo de la cita

`AuditoriaService.enmascarar` convierte `paciente@demo.pe` en
`pa***@demo.pe` y las secuencias de seis o más dígitos en `************`.

### JPA, JPQL y SQL nativo

- **Consultas derivadas de Spring Data** para filtros simples y legibles:
  `findByPacienteIdOrderByFechaDescHoraDesc`,
  `findByOdontologoIdAndFechaAndEstadoNot`, etc.
- **JPQL** cuando la consulta necesita una proyección o un `distinct`, como
  `findAccionesDistintas` en auditoría.
- **Sin SQL nativo** en la aplicación. Las entidades y las consultas
  parametrizadas cubren todos los casos, evitando concatenar datos del usuario y
  manteniendo la portabilidad del motor.
- El archivo `src/main/resources/db/scripts.sql` es DDL de instalación, no una
  consulta de la aplicación.

## Estructura

```text
src/main/java/pe/edu/idat/appclinica/
  config/        PasswordConfig, SecurityConfig, DataInitializer
  controller/    Auth, Cita, Admin, Captcha, inicio y manejo de errores
  dto/           formularios validados y celda del calendario
  exception/     errores de negocio
  model/         Rol, Usuario, Odontologo, Servicio, Horario, Cita, AuditoriaEvento
  repository/    repositorios Spring Data
  security/      UserDetailsService, CAPTCHA, auditoría y handlers de login
  service/       Usuario, Agenda, Cita y Calendario

src/main/resources/
  templates/     vistas Thymeleaf
  static/        CSS, JavaScript y favicon
  db/scripts.sql esquema y datos iniciales
```

## Pruebas sugeridas para la exposición

- Registro con CAPTCHA correcto e incorrecto.
- Registro con correo duplicado.
- Login con credenciales correctas e incorrectas.
- Reserva de una cita y visualización en calendario.
- Intento de reservar el mismo horario con otra cuenta.
- Cancelación de una cita propia.
- Intento de cancelar la cita de otro paciente (debe responder 403).
- Acceso de un paciente a `/admin/citas` (debe responder 403).
- Confirmación y atención de una cita desde el panel admin.
- Revisión de la auditoría y comprobación de que el correo está enmascarado.
- Intento de enviar un nombre con etiquetas HTML: debe mostrarse como texto.

## Problemas frecuentes

**`Access denied for user 'root'@'localhost'`**

La contraseña de MySQL no coincide. Define `DB_PASSWORD` con la clave correcta
antes de arrancar.

**El puerto 8080 está ocupado**

Detén la aplicación anterior de la biblioteca o arranca esta en 8081:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

**No aparece el CAPTCHA**

Verifica que `app.captcha.habilitado=true` y que `/captcha` responde. La imagen
se genera en el servidor y requiere que el navegador envíe la cookie de sesión.

**Las tablas no existen**

Ejecuta `src/main/resources/db/scripts.sql` o deja que Hibernate cree el
esquema con `ddl-auto=update` en un primer arranque limpio.

**La hora de las citas no coincide**

La aplicación fija `America/Lima` al arrancar. Revisa también la zona horaria de
MySQL y del sistema operativo.

## Entrega

El zip del proyecto debe incluir solo:

- `pom.xml`
- `src/`
- `COMO-EJECUTAR.md`
- `.gitignore`

No incluyas `target/`, `.idea/`, archivos `.env` ni capturas de pantalla. La
contraseña de MySQL se configura con `DB_PASSWORD` fuera del proyecto. Las
cuentas `admin@dentalcare.pe` y `paciente@demo.pe` son cuentas de demostración
creadas por `DataInitializer`; no son secretos de infraestructura y deben
cambiarse en un despliegue real.

## Nota para producción

Este proyecto es académico. Antes de un despliegue real:

- cambiar las contraseñas de demostración
- usar un usuario de MySQL con privilegios mínimos, no `root`
- habilitar TLS en la conexión a la base (`useSSL=true`)
- servir la aplicación por HTTPS
- configurar un proxy inverso y límites de tasa
- externalizar todos los secretos en variables de entorno o un gestor de secretos
- revisar la política de retención de la auditoría y de los datos clínicos
