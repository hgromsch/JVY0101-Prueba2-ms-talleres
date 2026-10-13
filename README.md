# ms-talleres

Talleres grupales y control de cupos. Cubre **RF-06 y RF-07**. El cupo se descuenta de
forma atómica en Redis (`DECR`, mismo patrón que la reserva de horario de ms-agenda); al
agotarse, la inscripción pasa a **lista de espera** (`EN_ESPERA`) en vez de perderse.

**Puerto:** 8084 · **Base de datos:** `talleres_db` · **Swagger:** http://localhost:8084/swagger-ui.html

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/talleres` | Crea un taller (201) |
| GET | `/api/talleres?proximos=` | Lista talleres (filtro opcional de próximos) |
| GET | `/api/talleres/{id}` | Obtiene un taller (404 si no existe) |
| PUT | `/api/talleres/{id}` | Actualiza el taller |
| DELETE | `/api/talleres/{id}` | Elimina el taller (204) |
| POST | `/api/talleres/{id}/inscripciones` | Inscribe paciente (201; CONFIRMADA o EN_ESPERA) |
| GET | `/api/talleres/{id}/inscripciones` | Lista inscripciones del taller |
| DELETE | `/api/talleres/{id}/inscripciones/{inscId}` | Cancela la inscripción y devuelve el cupo |

## Cómo correr localmente

```bash
docker compose -f ../docker-compose.yml up -d   # Postgres + Redis (Redis es OBLIGATORIO aquí)
mvn spring-boot:run
```

## Arquitectura

Capas `api` → `service` → `repository` → `domain`. Reglas de negocio: no duplicar
inscripción activa del mismo paciente (409) y cupo total nunca menor que los ya ocupados (409).
Relación JPA destacada: `Taller` **OneToMany** `Inscripcion` (y **ManyToOne** inversa).

## Despliegue

Pensado para **Amazon ECS Fargate** con Aurora Serverless y ElastiCache (Redis).
Imagen: `docker build -t ms-talleres .`
