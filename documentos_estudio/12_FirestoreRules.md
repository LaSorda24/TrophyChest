# 12 - firestore.rules

## Resumen Para Tontos

Este archivo es la seguridad de Firestore. Aunque la app tenga botones y pantallas, las reglas son las que deciden realmente quien puede leer o escribir datos.

## Estado

- No hay estado Compose.
- Las reglas trabajan con:
  - `request.auth`: usuario autenticado.
  - `request.resource.data`: datos nuevos que quiere escribir.
  - `resource.data`: datos actuales guardados.

## Flujo

1. Si un usuario intenta leer o escribir, Firestore revisa estas reglas.
2. `signedIn()` comprueba que hay sesion.
3. `isSelf(uid)` comprueba que el usuario toca su propio perfil.
4. `users/{uid}` permite actualizar solo campos concretos.
5. `usernames` y `friendCodes` solo se crean si no existen.
6. `connections` controla solicitudes y amistades.
7. `messages` solo permite mensajes entre miembros de una conexion aceptada.

## Firebase/API

Protege estas colecciones:

- `users`
- `usernames`
- `friendCodes`
- `connections`
- `connections/{connectionId}/messages`

No es codigo Kotlin, pero afecta directamente a lo que `UserRepository` y `ChatRepository` pueden hacer.

## Kotlin Basico Que Aparece Aqui

No es Kotlin, es lenguaje de reglas de Firestore.

Conceptos parecidos:

- `function`: funcion auxiliar.
- `allow read/create/update`: permisos.
- `&&`: todas las condiciones deben cumplirse.
- `||`: vale una condicion u otra.
- `diff(...).affectedKeys().hasOnly(...)`: limita campos modificados.

## Pregunta Para Defender

**Si me preguntan: Que pasa si alguien modifica la app y manda una escritura falsa?**

Responderia: "Firestore no se fia de la app cliente. Aunque alguien cambie el APK, la escritura pasa por estas reglas. Por ejemplo, un usuario solo puede modificar su propio perfil y solo ciertos campos."

**Si me preguntan: Por que no permites delete?**

Responderia: "Para evitar borrados accidentales o maliciosos desde cliente. Si quisiera borrar datos de verdad, lo haria con una funcion controlada o desde administracion."

## Frase Para Decir En La Defensa

"Las reglas de Firestore son la seguridad real de la parte social y de perfiles."

