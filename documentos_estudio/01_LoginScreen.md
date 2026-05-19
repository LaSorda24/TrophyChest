# 01 - LoginScreen.kt

## Resumen Para Tontos

Este archivo pinta la pantalla donde el usuario entra a la app. Sirve para iniciar sesion, crear cuenta o continuar con Google. No guarda datos en Firebase directamente: cuando el usuario pulsa un boton llama a `FirebaseManager`, que es quien habla con Firebase.

Idea clave: esta pantalla mezcla interfaz y estado local. No hay ViewModel.

## Estado

- `email`: texto que escribe el usuario en el campo de correo.
- `password`: texto de la contrasena.
- `username`: nombre publico, solo se usa al crear cuenta.
- `isRegisterMode`: decide si la pantalla esta en modo login o registro.
- `isLoading`: bloquea botones y muestra carga mientras Firebase responde.
- `errorMessage`: mensaje que se pinta si algo falla.
- `googleSignInClient`: cliente que abre la pantalla de Google.
- `googleLauncher`: recibe la respuesta de Google cuando el usuario vuelve a la app.

## Flujo

1. Al entrar, `LaunchedEffect(Unit)` comprueba si ya hay usuario en `FirebaseManager.currentUser`.
2. Si ya hay usuario, navega a `principal`.
3. Si pulsa email/contrasena, se validan campos con `validateCredentials`.
4. Si todo es correcto, se lanza una corrutina con `coroutineScope.launch`.
5. En login llama a `FirebaseManager.login`.
6. En registro llama a `FirebaseManager.register`.
7. Si usa Google, `googleLauncher` abre Google Sign-In y luego llama a `FirebaseManager.loginWithGoogle`.
8. Si todo sale bien, `navigateToPrincipal` limpia la pila y entra a la app.

## Firebase/API

- Envia a Firebase Auth: correo, contrasena o token de Google.
- Si es registro, tambien envia el `username` para crear el perfil en Firestore.
- La pantalla no llama a Firestore directamente.
- La pantalla recibe un `Result`: si es `Success`, navega; si es `Failure`, muestra error.

## Kotlin Basico Que Aparece Aqui

- `rememberSaveable`: guarda estado de Compose y aguanta cambios como rotacion.
- `mutableStateOf`: si cambia el valor, Compose redibuja la UI.
- `by`: permite escribir `email = "x"` en vez de `email.value = "x"`.
- `LaunchedEffect`: ejecuta codigo al entrar en la pantalla.
- `coroutineScope.launch`: lanza trabajo asincrono sin bloquear la interfaz.
- `Result`: encapsula exito o error.
- `return@Button`: sale solo del click del boton, no de toda la funcion.

## Pregunta Para Defender

**Si me preguntan: Como se actualiza la interfaz cuando cambian los datos?**

Responderia: "En esta pantalla la UI depende de variables de estado como `isLoading`, `errorMessage` o `isRegisterMode`. Cuando cambia una de esas variables, Compose vuelve a dibujar automaticamente la parte afectada. Por ejemplo, si `isLoading` pasa a true, el boton se desactiva y aparece el indicador de carga."

**Si me preguntan: Por que usas corrutinas?**

Responderia: "Porque las llamadas a Firebase tardan y no puedo bloquear el hilo principal. Con `launch`, la app sigue respondiendo mientras espera el resultado."

## Frase Para Decir En La Defensa

"LoginScreen solo controla la interfaz y el estado visible; la autenticacion real la delego en FirebaseManager."

