# 👤 Microservicio de Usuario - `ms_user`

Este microservicio forma parte del ecosistema de servicios distribuidos y se encarga de gestionar usuarios en el sistema, permitiendo a los administradores crear, actualizar, consultar y eliminar usuarios con distintos roles (`ADMIN` y `USER`). 

También permite a los usuarios autenticados actualizar su perfil personal, cambiar su contraseña o correo electrónico de forma segura.

---

## 🚀 Tecnologías Utilizadas

- **Java 17**
- **Spring Boot 3.5.0**
- **Spring WebFlux (reactivo)**
- **Spring Security + JWT (OAuth2 Firebase)**
- **PostgreSQL + R2DBC**
- **Firebase Admin SDK (autenticación)**
- **Almacenamiento de imágenes (ImgBB / Supabase / externo)**
- **OpenFeign (para comunicación entre microservicios)**
- **Docker Ready**

---

## 📁 Estructura de Proyecto

- `controller/` – Controladores REST para administración y gestión de perfil del usuario.
- `dto/` – Objetos de transferencia de datos entre capas.
- `service/` – Lógica de negocio y conexión a repositorios.
- `config/` – Configuración de seguridad, JWT y autenticación personalizada.
- `repository/` – Interfaces R2DBC para acceso a datos en PostgreSQL.

---

## 🧩 Funcionalidades Principales

### 🔐 ADMIN

- Listar todos los usuarios del sistema.
- Buscar usuario por ID o email.
- Verificar si un email ya está registrado.
- Crear nuevos usuarios con o sin imagen de perfil.
- Actualizar datos de cualquier usuario (incluyendo su imagen).
- Eliminar usuarios existentes.

### 👤 USUARIO AUTENTICADO

- Ver su propio perfil (`/me`) usando el UID de Firebase.
- Actualizar sus datos personales (nombre, documento, celular, imagen...).
- Cambiar su contraseña.
- Cambiar su email.

---

## 🔧 Instalación y ejecución

### 1. Clonar el proyecto

```bash
git clone https://github.com/tu-usuario/ms_user.git
cd ms_user
