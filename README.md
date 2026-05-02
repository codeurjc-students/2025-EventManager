# EventManager

[![CI](https://github.com/codeurjc-students/2025-EventManager/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/codeurjc-students/2025-EventManager/actions/workflows/ci.yml)
[![Cobertura JaCoCo](https://img.shields.io/badge/coverage-JaCoCo%20artifact-blue)](https://github.com/codeurjc-students/2025-EventManager/actions/workflows/ci.yml)

> **Descripción**
EventManager es una plataforma web para crear y administrar eventos de forma completa: permite a los usuarios registrarse, acceder, gestionar su perfil y participar en eventos, así como controlar sus entradas y regalos en estos.

---

## Características

- **Autenticación**: registro, login, logout y refresh token.
- **Perfil de usuario**: consulta y actualización de datos.
- **Gestión de eventos**: creación, listado, detalle y actualización.
- **Entradas**: inscripción a eventos y gestión de tickets.
- **Regalos**: visualización y administración.
- **SPA con routing**: vistas públicas y privadas en el frontend.

---

## Tecnologías

| Capa | Tecnología | Versión / Detalles |
|------|------------|--------------------|
| Frontend | Vue | 3.x |
| Frontend | Vite | 5.x |
| Frontend | TypeScript | 4.x |
| UI | Element Plus | 2.x |
| HTTP Client | Axios | 0.21.x |
| Backend | Java | 21 |
| Backend | Spring Boot | 3.4.4 |
| Backend | Spring Security | (starter) |
| Backend | Spring Data JPA | (starter) |
| API Docs | springdoc-openapi-ui | 1.6.9 |
| Auth | JJWT | 0.12.6 |
| DB | PostgreSQL | (driver `org.postgresql`) |
| Build | Maven Wrapper | `mvnw` / `mvnw.cmd` |
| Cloud AWS | AWS SDK (S3) | Dependencias incluidas |

---

## Estructura del repositorio

Un único repositorio con:
- **Backend** en la raíz (Spring Boot + Maven)
- **Frontend** en `frontend/` (Vue + Vite)
- **Tests** en `src/test/`
- **Workflow** CI en `.github/workflows/ci.yml`

Árbol simplificado:

```text
eventManager/
├─ pom.xml
├─ mvnw
├─ mvnw.cmd
├─ docker-compose-base.local.yaml
├─ docker-compose-detailed.local.yaml
├─ docker-compose.aws.yaml
├─ .env.example
├─ .gitignore
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ eventManager/
│  │  └─ resources/
│  │     ├─ api/
│  │     ├─ application.yml
│  │     └─ static/
│  └─ test/
│     ├─ java/
│     │  └─ eventManager/
│     │     ├─ security/
│     │     ├─ service/
│     │     ├─ web/
│     │     └─ selenium/
│     └─ resources/
├─ frontend/
│  ├─ package.json
│  ├─ vite.config.mjs
│  ├─ index.html
│  ├─ public/
│  └─ src/
│     ├─ api/
│     ├─ components/
│     ├─ router/
│     ├─ stores/
│     ├─ views/
│     └─ main.ts
├─ minio_data/
├─ .github/
│  └─ workflows/
      ├─ cd.yml
│     └─ ci.yml
```

---

## Puertos por defecto

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8090`

## Endpoints y documentación API

- Base URL local: `http://localhost:8090`
- Swagger UI: `http://localhost:8090/swagger-ui/index.html`

---

## Variables de Entorno (.env)

Este proyecto utiliza variables de entorno principalmente para el **backend** (Spring Boot).
Se proporciona un fichero un fichero .env.example de referencia.

> **Nota (pendiente)**: Si el frontend se sirve desde un dominio/puerto distinto al del backend (S3, Cloudfront, etc), será necesario ajustar la configuración CORS del backend para permitir ese origen.

---

## Almacenamiento de Imágenes

El proyecto utiliza **MinIO** (compatible con S3) para desarrollo local.

### Iniciar MinIO

```bash
docker compose -f docker-compose-base.local.yaml up -d
```

### Acceso:
- **API**: http://localhost:9000
- **Consola Web**: http://localhost:9001

### Credenciales:
- Usuario: `${MINIO_ACCESS_KEY}` (ver `.env.example`)
- Contraseña: `${MINIO_SECRET_KEY}` (ver `.env.example`)

El bucket `event-manager-images` se crea automáticamente al iniciar la aplicación con perfil `dev`.

### Stack local completo

Si quieres levantar la aplicación completa en local junto con PostgreSQL y MinIO:

```bash
docker compose -f docker-compose-detailed.local.yaml up -d --build
```

Este stack arranca:
- **App**: `http://localhost:8090`
- **PostgreSQL**: `localhost:5432`
- **MinIO API**: `http://localhost:9000`
- **MinIO Console**: `http://localhost:9001`

La imagen de la app se construye con el `Dockerfile` de la raíz, que ya integra el frontend compilado dentro del backend Spring Boot.

### Arquitectura

```
┌─────────────────┐
│   S3Service     │  ← Interfaz común
└─────────────────┘
         ↑
         │ implements
    ┌────┴──────┐
┌───┴────┐  ┌───┴─────────┐
│MinioSvc│  │S3ServiceImpl│
│@dev    │  │@aws         │
└────────┘  └─────────────┘
    │             │
┌───┴────┐    ┌───┴────┐
│MinIO   │    │AWS S3  │
│Docker  │    │Cloud   │
└────────┘    └────────┘
```

---

## Testing

El proyecto incluye:
- Tests unitarios (servicios, seguridad, excepciones)
- Tests de controlador (WebMvc)
- Tests de integración UI con Selenium (vistas de autenticación, home, eventos, usuarios, regalos y tickets)

### Perfiles Maven
- `unit`: perfil por defecto; excluye Selenium
- `selenium`: ejecuta suites Selenium
- `selenium-ci`: desactiva tareas pesadas para acelerar CI Selenium

### Ejecución habitual
- Unit tests: `./mvnw -Punit test`
- Selenium: `./mvnw -Pselenium,selenium-ci test`
- Cobertura JaCoCo: `./mvnw -Punit verify`

### JaCoCo

- Plugin: `jacoco-maven-plugin`.
- `prepare-agent`: instrumenta durante tests.
- `report` en fase `verify`: genera reportes.
- Ficheros generados típicos:
  - `target/jacoco.exec`
  - `target/site/jacoco/index.html`
  - `target/site/jacoco/jacoco.xml`

Estos ficheros son artefactos de ejecución, no se versionan porque `target/` está ignorado en `.gitignore`.

---

## CI/CD

### Integración Continua (CI)

**¿Qué tareas se ejecutan automáticamente?**
- Ejecución de tests unitarios, Selenium y generación de cobertura con JaCoCo.
- Publicación de reportes de tests y cobertura como artifacts.

**¿Cuándo se ejecuta?**
- En *push* o *pull requests* hacia `main`, `develop`, `release/**` y `hotfix/**`.

**¿Dónde se almacenan los artefactos?**
- En **GitHub Actions → Artifacts** del run correspondiente (`surefire-*`, `jacoco-*`, `backend-log-*`).

### Entrega Continua (CD)

**¿Qué tareas se ejecutan automáticamente?**
- Construcción de la imagen Docker usando `Dockerfile`.
- Publicación de la imagen en **DockerHub**.
- Publicación del `docker-compose.aws.yaml` como **OCI artifact** (solo en releases).

**¿Cuándo se ejecuta?**
- En commits a `main` (por ejemplo tras merge de un pull request).
- En publicaciones de *release*.

**¿Dónde se almacenan los artefactos?**
- En **DockerHub**: `docker.io/<usuario>/eventmanagerapp`.
- El compose OCI: `docker.io/<usuario>/eventmanagerapp-compose:<tag-release>`.
- Etiquetas típicas: `latest` (main), `sha` corto y etiqueta de release.

---

## Despliegue en AWS y endpoint actual de la aplicación
Actualmente el endpoint público usa la siguiente IP elástica: https://52.51.236.28/