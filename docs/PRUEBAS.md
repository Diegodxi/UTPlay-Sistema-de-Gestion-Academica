# Plan de pruebas de UTPlay

## Pruebas automatizadas

Ejecutar desde la raíz del proyecto:

```bash
mvn clean test
```

| Prueba | Clase | Comprobación |
|---|---|---|
| Registro válido | `UsuarioControllerIntegrationTest` | HTTP 201, correo guardado y password ausente en JSON |
| Login válido | `UsuarioControllerIntegrationTest` | HTTP 200, token Bearer y usuario correcto |
| Password incorrecto | `UsuarioControllerIntegrationTest` | HTTP 401 con mensaje controlado |
| Corrección | `QuizServiceIntegrationTest` | 2 de 3 respuestas generan 67 puntos y aprobación |
| Persistencia | `QuizServiceIntegrationTest` | Los intentos quedan registrados en H2 de prueba |
| Antifraude básico | `QuizServiceIntegrationTest` | Un segundo intento peor entrega 0 puntos nuevos |

Las pruebas utilizan una base H2 temporal en modo MySQL; no modifican `utplay_db`.

## Pruebas manuales de aceptación

### Registro

1. Abra `/registro.html`.
2. Complete nombre, correo, contraseña de ocho o más caracteres y ciclo.
3. Pulse **Crear mi cuenta**.
4. Resultado esperado: cuenta creada, sesión iniciada y redirección al dashboard.
5. Repita el registro con el mismo correo.
6. Resultado esperado: mensaje indicando que el correo ya existe.

### Login

1. Ingrese un correo y contraseña registrados.
2. Resultado esperado: acceso al dashboard.
3. Cierre sesión e ingrese una contraseña incorrecta.
4. Resultado esperado: HTTP 401 y mensaje “Correo o contraseña incorrectos”.

### Cursos y prerrequisitos

1. Registre un usuario de ciclo 3.
2. Abra **Cursos**.
3. Resultado esperado: cursos de ciclos superiores bloqueados.
4. Intente abrir directamente la URL de un quiz bloqueado.
5. Resultado esperado: el backend también rechaza el acceso; el bloqueo no depende solo del frontend.
6. Apruebe el quiz del curso prerrequisito y vuelva al listado.
7. Resultado esperado: el siguiente curso correspondiente al ciclo se desbloquea.

### Quiz y cálculo

1. Inicie el “Reto de fundamentos”.
2. Intente finalizar sin responder todo.
3. Resultado esperado: solicitud para completar todas las preguntas.
4. Responda las tres y finalice.
5. Resultado esperado: 0, 33, 67 o 100 según las respuestas; se muestra aprobación desde 60.
6. Si obtiene 100, resultado esperado: 120 puntos potenciales (100 + bono de 20).
7. Repita con un resultado menor.
8. Resultado esperado: el intento se guarda, pero aporta 0 puntos nuevos.

### Resultados y perfil

1. Abra **Mi progreso** después de resolver un quiz.
2. Resultado esperado: aparecen puntaje, fecha, curso, puntos ganados y avance.
3. Cambie nombre y ciclo.
4. Resultado esperado: datos actualizados en perfil y barra superior.

### Base de datos

Ejecute estas consultas en MySQL Workbench:

```sql
USE utplay_db;

SELECT id_usuario, nombre, correo, ciclo_actual, puntos_totales, nivel
FROM usuario;

SELECT r.id_resultado, u.correo, q.titulo, r.puntaje,
       r.respuestas_correctas, r.total_preguntas, r.puntos_ganados, r.fecha
FROM resultado r
JOIN usuario u ON u.id_usuario = r.id_usuario
JOIN quiz q ON q.id_quiz = r.id_quiz
ORDER BY r.fecha DESC;

SELECT u.correo, l.nombre, ul.fecha_obtenido
FROM usuario_logro ul
JOIN usuario u ON u.id_usuario = ul.id_usuario
JOIN logro l ON l.id_logro = ul.id_logro;
```

Resultado esperado: información consistente con las operaciones realizadas desde la web.

## Colección Postman

Importe `postman/UTPlay.postman_collection.json`. Ejecute en este orden:

1. Registrar usuario.
2. Login (guarda automáticamente `token` y `usuarioId`).
3. Listar cursos.
4. Obtener quiz.
5. Resolver quiz.
6. Consultar resultados.
