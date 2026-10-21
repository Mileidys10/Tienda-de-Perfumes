# Luxury Perfumes Store &mdash; Backend REST API (Spring Boot 3 + PostgreSQL)

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/Spring_Security-6-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white" alt="Spring Security" />
  <img src="https://img.shields.io/badge/JWT-Authentication-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker Compose" />
  <img src="https://img.shields.io/badge/Licencia-MIT-F59E0B?style=for-the-badge" alt="MIT License" />
</p>

> **Servicio Backend Empresarial** para la gestión de catálogo de fragancias de lujo, control de inventario, procesamiento de pedidos y autenticación basada en roles (RBAC) con tokens JWT y persistencia relacional en PostgreSQL.

> **Desarrollado por**: [Mileidys Agamez Ospino](https://github.com/Mileidys10) • Ingeniería de Software & Arquitectura Full Stack.  
> **Frontend Asociado**: [Frontend-Perfume](https://github.com/Mileidys10/Frontend-Perfume) (Angular 20 + Ionic 8).

---

## 🏛️ Arquitectura de Software

El sistema sigue una arquitectura multicapa desacoplada y orientada al dominio:

```
[ Peticiones HTTP / Clientes REST ]
               ↓
[ Controllers (@RestController) ] ── (Validación de DTOs & Autenticación JWT)
               ↓
[ Services (@Service) ] ─────────── (Reglas de Negocio, Transacciones ACID)
               ↓
[ Repositories (@Repository) ] ──── (Spring Data JPA / Hibernate)
               ↓
[ Base de Datos PostgreSQL 16 ]
```

### Módulos Principales:
* **Autenticación & Seguridad**: Endpoints de login y registro con hash bcrypt y emisión de JWT.
* **Catálogo de Perfumes**: CRUD completo de fragancias, casas de diseño, notas olfativas y precios.
* **Gestión de Categorías**: Clasificación por género, familia olfativa y concentración (EDP, EDT, Parfum).
* **Manejo de Imágenes**: Carga y almacenamiento estático de fotografías de productos en `/uploads`.
* **Pedidos & Checkout**: Registro de órdenes de compra con control transaccional de existencias.

---

## 🐳 Despliegue Rápido con Docker Compose

El proyecto incluye orquestación multicontenedor con la base de datos PostgreSQL y la aplicación empaquetada:

```bash
# 1. Clonar el repositorio
git clone https://github.com/Mileidys10/Tienda-de-Perfumes.git
cd Tienda-de-Perfumes

# 2. Iniciar el entorno completo
docker compose up -d --build
```

* **API REST**: [http://localhost:8080/api](http://localhost:8080/api)
* **Base de Datos**: PostgreSQL expuesto en el puerto `5432`.

---

## 💻 Ejecución Local con Maven

### Requisitos Previos:
* **Java Development Kit (JDK)**: Versión 21 o superior.
* **PostgreSQL**: Instancia local corriendo con base de datos `perfumes_db`.

### Pasos:
```bash
# Configurar variables en application.properties o variables de entorno:
# DB_URL=jdbc:postgresql://localhost:5432/perfumes_db
# DB_USERNAME=postgres
# DB_PASSWORD=tu_password

# Compilar y ejecutar con Maven Wrapper:
./mvnw spring-boot:run
# En Windows:
.\mvnw.cmd spring-boot:run
```

---

## 📁 Estructura del Código (`src/main/java/com/backend/perfumes/`)

```text
perfumes/
├── config/             # Configuración de Seguridad, JWT Filter y CORS
├── controller/         # Controladores REST expuestos
├── dto/                # Data Transfer Objects para requests y responses
├── model/              # Entidades JPA (User, Role, Perfume, Category, Order)
├── repository/         # Interfaces de acceso a datos JPA
├── service/            # Lógica de negocio e interfaces de servicio
└── PerfumesApplication.java # Clase principal de arranque Spring Boot
```

---

## 📜 Licencia y Autoría

* **Autora**: Mileidys Agamez Ospino
* **GitHub**: [@Mileidys10](https://github.com/Mileidys10)
* **Licencia**: [MIT](LICENSE)
