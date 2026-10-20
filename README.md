# Clínica Dental · Sistema de citas

Aplicación web para una clínica odontológica: los pacientes se registran,
inician sesión, reservan una cita con un especialista, consultan su calendario y
cancelan sus propias citas. El personal administrativo ve toda la agenda,
confirma, atiende o cancela citas y revisa la auditoría del sistema.

Proyecto del curso **8C003 Desarrollo de Aplicaciones Seguras (IDAT)**.

## Funcionalidades

### Paciente

- Registro con validación de datos y CAPTCHA.
- Inicio y cierre de sesión.
- Reserva de citas por especialista, tratamiento, fecha y hora.
- Validación de disponibilidad en el servidor (no se puede duplicar un horario).
- Vista de próximas citas e historial.
- Calendario mensual.
- Cancelación de citas propias, con verificación de pertenencia (anti-IDOR).

### Administración

- Panel con todas las citas y filtros por fecha y estado.
- Cambio de estado: `PENDIENTE` → `CONFIRMADA` → `ATENDIDA`, con cancelación en
  los estados permitidos.
- Calendario general de la clínica.
- Catálogo de odontólogos, tratamientos y horarios.
- Auditoría de accesos, cambios de estado y acciones administrativas.

## Stack

- Java 17+
- Spring Boot 3.5.3
- Spring MVC + Thymeleaf
- Spring Security 6
- Spring Data JPA + Hibernate
- MySQL 8
- Maven
- Monolito, sin frontend separado

## ¿Dónde vive la base de datos?

**La base de datos no se sube a GitHub.** Lo que se versiona es el código y el
script que crea la estructura; los datos reales (cuentas, citas, auditoría)
viven en el MySQL de tu computadora.

```
Tu computadora                              GitHub
┌──────────────────────────┐               ┌──────────────────────────┐
│ MySQL local              │               │ Código fuente            │
│ Base: clinica_dental     │   no viaja    │ scripts.sql              │
│ Datos: cuentas y citas   │  ──────────►  │ application.properties   │
└──────────────────────────┘               │ README, tests, CI        │
                                           └──────────────────────────┘
```

En el repositorio sí están:

- `src/main/resources/db/scripts.sql`: creación de la base, tablas, roles,
  odontólogos, tratamientos y horarios.
- `DataInitializer`: crea las cuentas de demostración y verifica los datos
  mínimos al arrancar.
- `application.properties`: placeholders de conexión, sin contraseñas reales.

En el repositorio **no** están:

- La contraseña de MySQL.
- El archivo `.env` con secretos.
- La carpeta `target/`, `.idea/` ni archivos `.zip`.
- Los datos de tu base local.

### Cómo lo usa otra persona

1. Instala MySQL 8 (o levanta el `docker-compose.yml` incluido).
2. Ejecuta `src/main/resources/db/scripts.sql` para crear la estructura.
3. Define `DB_USER` y `DB_PASSWORD` en su entorno.
4. Arranca la aplicación. `DataInitializer` crea las cuentas demo si no existen.

Así cada persona tiene su propia base local y ninguno comparte credenciales en el
repositorio. En producción se usa un MySQL gestionado y los secretos se
configuran en el proveedor, nunca en el código.

## Requisitos

- JDK 17 o superior.
- MySQL 8 en `localhost:3306` (o Docker).
- Maven 3.9+ o IntelliJ IDEA.

## Puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/NotFoundMax/app-clinica-dental.git
cd app-clinica-dental
```

### 2. Crear la base de datos

**Opción A: MySQL local**

```bash
mysql -u root -p < src/main/resources/db/scripts.sql
```

**Opción B: Docker Compose (opcional)**

```bash
cp .env.example .env
# Edita .env y cambia las claves de ejemplo
docker compose up -d
```

El contenedor ejecuta el script de esquema la primera vez que se crea el
volumen. Si ya tienes un MySQL local ocupando el puerto 3306, detén ese servicio
o cambia el mapeo de puertos en `docker-compose.yml`.

### 3. Configurar la conexión

La aplicación lee estas variables de entorno:

```properties
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASSWORD:}
```

En Linux/macOS:

```bash
export DB_USER=root
export DB_PASSWORD=tu_password
mvn spring-boot:run
```

En IntelliJ: `Run > Edit Configurations > Environment variables` y agrega
`DB_USER=root;DB_PASSWORD=tu_password`.

Si usas el `docker-compose.yml`, el usuario y la clave son los que definiste en
`.env` (`MYSQL_USER` y `MYSQL_PASSWORD`).

> El archivo `.env` no lo lee Spring Boot automáticamente. Se usa para Docker
> Compose. Para la aplicación, exporta las variables o configúralas en el IDE.

### 4. Abrir la aplicación

```text
http://localhost:8080
```

## Cuentas de demostración

Se crean en el primer arranque. Son cuentas de demostración, no credenciales de
producción.

| Rol | Correo | Contraseña |
| --- | --- | --- |
| Administrador | `admin@dentalcare.pe` | `Admin123!` |
| Paciente | `paciente@demo.pe` | `Paciente123!` |

El registro público solo crea usuarios con rol `PACIENTE`. Los administradores
se crean desde el servidor.

## Pruebas

```bash
mvn test
```

Las pruebas unitarias cubren:

- Transiciones válidas e inválidas del estado de una cita.
- Hash Argon2id y verificación de contraseña.
- Rechazo de cancelación de una cita ajena (anti-IDOR).
- Rechazo de transiciones de estado no permitidas.

La compilación y las pruebas también se ejecutan en GitHub Actions en cada push
y pull request.

## Seguridad implementada

- **Argon2id** para contraseñas, con parámetros explícitos.
- **CSRF** en todos los formularios.
- **Roles** `ADMIN`, `PACIENTE` y `ODONTOLOGO`; rutas `/admin/**` solo para
  administradores.
- **IDOR**: antes de cancelar se verifica que la cita pertenezca al paciente.
- **Validación** con Jakarta Bean Validation en los formularios.
- **Escape** con `th:text` en Thymeleaf para evitar XSS.
- **CAPTCHA** en el registro público.
- **Auditoría** con correos y teléfonos enmascarados; nunca se guardan
  contraseñas, hashes, tokens ni motivos médicos.
- **Consultas parametrizadas** mediante Spring Data JPA; sin SQL concatenado.
- **Errores** 403, 404, 405 y 500 con páginas propias.

## Estructura

```text
src/main/java/pe/edu/idat/appclinica/
├── config/        Seguridad, contraseñas y datos iniciales
├── controller/    Vistas MVC y endpoints
├── dto/           Formularios con validación
├── exception/     Excepciones de negocio
├── model/         Entidades JPA y estados
├── repository/    Repositorios Spring Data
├── security/      Autenticación, CAPTCHA y auditoría
└── service/       Reglas de agenda y citas

src/main/resources/
├── db/scripts.sql Esquema y catálogos iniciales
├── static/        CSS, JS y favicon
├── templates/     Vistas Thymeleaf
└── application.properties
```

## Documentación adicional

- [`COMO-EJECUTAR.md`](COMO-EJECUTAR.md): instalación paso a paso, flujo de
  prueba, decisiones de seguridad y problemas frecuentes.
- [`src/main/resources/db/scripts.sql`](src/main/resources/db/scripts.sql):
  esquema completo y datos iniciales.

## Nota para producción

Este proyecto es académico. Antes de un despliegue real:

- Cambiar las contraseñas de demostración.
- Usar un usuario de MySQL con privilegios mínimos, no `root`.
- Habilitar TLS en la conexión a la base.
- Servir la aplicación por HTTPS.
- Externalizar secretos en variables de entorno o un gestor de secretos.
- Sustituir `ddl-auto=update` por migraciones versionadas (Flyway o Liquibase)
  y `ddl-auto=validate`.
- Revisar la retención de auditoría y de datos clínicos.
