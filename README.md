# EventManager

> **Descripción**
Plataforma de gestión de eventos con autenticación del usuario y gestión de: usuarios, eventos, entradas y regalos.

---

## Características

- **Autenticación**: registro, login, logout y refresh token.
- **Perfil de usuario**: consulta y actualización de datos.
- **Gestión de eventos**: crear, listar, ver detalles y actualizar.
- **Entradas**: inscripción a eventos y gestión de tickets.
- **Regalos**: visualización y gestión.
- **SPA con routing**: vistas públicas/privadas en el frontend.

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

Árbol:

```text
eventManager/
├─ pom.xml
├─ mvnw
├─ mvnw.cmd
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ eventManager/
│  │  └─ resources/
│  │     ├─ api/
│  │     │  └─ api.yaml
│  │     ├─ application.yml
│  │     ├─ static/
│  │     └─ templates/
│  └─ test/
├─ frontend/
│  ├─ package.json
│  ├─ vite.config.js
│  ├─ index.html
│  ├─ public/
│  └─ src/
│     ├─ api/
│     ├─ components/
│     ├─ router/
│     ├─ stores/
│     ├─ views/
│     └─ main.ts
```

---

## Puertos por defecto:
- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8090`

---

## Variables de Entorno (.env)

Este proyecto utiliza variables de entorno principalmente para el **backend** (Spring Boot).

---

## Endpoints y documentación API

- Base URL (local): `http://localhost:8090`
- Swagger UI: `http://localhost:8090/swagger-ui/index.html`
