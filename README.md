# 💎 Aeterna Perfumes — Backend REST API (Spring Boot 3 + PostgreSQL)

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Spring_Security-6-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security" />
  <img src="https://img.shields.io/badge/JWT-Stateless_Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker Compose" />
  <img src="https://img.shields.io/badge/Licencia-MIT-F59E0B?style=for-the-badge" alt="MIT License" />
</p>

> **Backend Empresarial de Alta Disponibilidad** para la gestión del catálogo de fragancias de lujo, control de inventario transaccional, procesamiento de pedidos, notificaciones transaccionales por correo y autenticación segura basada en roles (RBAC) con tokens JWT y persistencia relacional en PostgreSQL.

* **Desarrolladora**: [Mileidys Agámez Ospino](https://github.com/Mileidys10) — Ingeniera de Software & Arquitecta Full Stack.
* **Frontend Web & Móvil Asociado**: [Frontend-Perfume](https://github.com/Mileidys10/Frontend-Perfume) (Angular 20 + Ionic 8 + Quiet Luxury Design).

---

## 🏛️ Arquitectura del Sistema

El sistema implementa una arquitectura multicapa desacoplada y orientada al dominio (Domain-Driven Design / Clean Architecture):

```
[ Peticiones HTTP / Clientes REST (Angular 20 / Ionic 8) ]
                           │
[ Controllers (@RestController) ] ── (Validación de DTOs & Autenticación JWT)
                           │
[ Services (@Service) ] ────────── (Reglas de Negocio, Transacciones ACID, Emails)
                           │
[ Repositories (@Repository) ] ─── (Spring Data JPA / Hibernate)
                           │
[ Base de Datos PostgreSQL 16 ]
```

### Módulos Principales:
* **Autenticación & Seguridad**: Endpoints de login, registro y refresh tokens con hashing BCrypt y emisión de JWT stateless.
* **Catálogo de Perfumes**: CRUD transaccional de fragancias, casas de diseño, pirámide olfativa (salida, corazón, fondo) y stock.
* **Gestión de Categorías & Familias**: Clasificación por género, notas olfativas y concentraciones (Extrait, EDP, EDT, Cologne).
* **Manejo de Imágenes & Multimedia**: Almacenamiento desacoplado con soporte para filesystem local y Supabase Storage.
* **Procesamiento de Pedidos & Checkout**: Registro de órdenes con control estricto de existencias y templates HTML de confirmación por email (Thymeleaf).
* **Dashboard del Vendedor & Moderación**: Métricas operativas, control de inventario y moderación automática de productos.

---

## 🐳 Despliegue en 1 Clic con Docker Compose

El proyecto cuenta con orquestación multicontenedor lista para producción:

```bash
# 1. Clonar el repositorio
git clone https://github.com/Mileidys10/Tienda-de-Perfumes.git
cd Tienda-de-Perfumes

# 2. Iniciar el entorno completo (PostgreSQL + Spring Boot API)
docker compose up -d --build
```

* **API REST**: `http://localhost:8080/api`
* **Base de Datos**: PostgreSQL expuesto en el puerto `5432`.
* **Healthcheck**: Verificación activa de conectividad de base de datos antes del arranque del servicio web.

---

## 💻 Ejecución Local con Maven

### Requisitos Previos:
* **Java Development Kit (JDK)**: Versión 17 o 21 LTS.
* **PostgreSQL** o H2 Database (fallback local embebido incluido para desarrollo ágil sin dependencias).

### Pasos:
```bash
# Compilar y ejecutar con Maven Wrapper:
./mvnw spring-boot:run

# En Windows (PowerShell):
.\mvnw.cmd spring-boot:run
```

---

## 📁 Estructura del Código (`src/main/java/com/backend/perfumes/`)

```text
perfumes/
├── config/             # Configuración de Seguridad, JWT Filter, CORS y OpenAPI
├── controller/         # Controladores REST (Auth, Perfumes, Orders, Categories, Seller)
├── dto/                # Data Transfer Objects para requests y responses
├── model/              # Entidades JPA (User, Role, Perfume, Category, Order, Notification)
├── repositories/       # Interfaces de persistencia JPA con Spring Data
├── services/           # Lógica de negocio transaccional y servicios auxiliares
└── PerfumesApplication.java # Entrypoint principal Spring Boot
```

---

## 🧪 Pruebas Automatizadas

El backend incluye una suite de pruebas unitarias y de integración sobre servicios críticos:
* `AccountTokenServiceTest`: Validación de tokens de activación y recuperación de cuenta.
* `OrderEmailTemplateServiceTest`: Renderizado determinista de plantillas de email HTML.
* `PaymentGatewayServiceTest`: Simulación de pasarelas de pago y estados de transacción.
* `OrderStatusTest`: Máquina de estados finitos para el ciclo de vida de los pedidos.

```bash
mvn test
```

---

## 📄 Licencia y Autoría

* **Autora**: Mileidys Agámez Ospino
* **GitHub**: [@Mileidys10](https://github.com/Mileidys10)
* **Licencia**: MIT
