# Paula Andrea Piedrahita
# Santiago Carlosama Ortiz
# Jean Carlo Ocampo

# Chat-multiple-client--text-audio-activity-
# ChatMe

## Características

Mensajería privada: Envío de mensajes de texto entre usuarios

Notas de voz: Grabación y envío de mensajes de audio

Llamadas de voz: Comunicación de audio en tiempo real

Grupos de chat: Creación y gestión de grupos de conversación

Historial: Registro automático de todas las conversaciones

## Instrucciones

Compile cada subproyecto (Usuario/Servidor )usando gradle

### Servidor

Inicie el servidor en una terminal.
El servidor se ejecutará en el puerto 8080 por defecto.

### Usuarios

En terminales separadas, ejecute los clientes.
Siga las instrucciones para configurar cada cliente.
El nombre que elija será el nombre con el que otros usuarios (terminales) podrán encontrarlo y referirse a su sesión para enviarle mensajes y audios.
Escriba la ip de la términal (maquina/equipo) que está corriendo el servidor. Escriba el puerto en que esta escuchando el servidor.

#### Funciones 

Menú Principal
El menú principal ofrece las siguientes opciones:

Enviar mensaje privado - Mensajes directos a otro usuario

Ver contactos - Mostrar lista de contactos guardados

Agregar contacto - Añadir nuevo contacto con nombre e IP

Grupos - Acceder al menú de gestión de grupos

Enviar nota de voz - Grabar y enviar mensaje de audio

Llamadas de voz - Acceder al menú de llamadas

Salir - Cerrar la aplicación

Mensajería Privada
Seleccione opción 1 del menú principal

Ingrese el nombre de usuario del destino

Escriba el mensaje y presione Enter

El mensaje se entregará inmediatamente al usuario destino

Notas de Voz
Seleccione opción 5 del menú principal

Elija entre enviar a usuario o grupo

Especifique la duración en segundos (máximo 30)

Ingrese el destino (usuario o grupo)

La aplicación grabará audio y lo enviará automáticamente

Llamadas de Voz
Iniciar Llamada:
Seleccione opción 6 del menú principal

Elija "Llamar a usuario"

Ingrese el nombre del usuario a llamar

Espere a que el usuario destino acepte la llamada

Recibir Llamada:
Cuando reciba una solicitud de llamada, aparecerá un mensaje

Responda "s" para aceptar o "n" para rechazar

Si acepta, la conexión de audio se establecerá automáticamente

Durante la Llamada:
El audio se transmite en tiempo real

Para colgar, regrese al menú de llamadas y seleccione "Colgar llamada actual"

Gestión de Grupos
Crear Grupo:
Seleccione opción 4 del menú principal

Elija "Crear grupo"

Ingrese un nombre para el grupo

El grupo se creará y usted se unirá automáticamente

Unirse a Grupo:
Seleccione "Unirse a grupo" en el menú de grupos

Ingrese el nombre del grupo existente

Será añadido a la lista de miembros

Enviar Mensaje a Grupo:
Seleccione "Enviar mensaje a grupo"

Ingrese el nombre del grupo

Escriba el mensaje

Todos los miembros del grupo recibirán el mensaje

Comandos Especiales
En el Cliente:
exit - Cerrar conexión y salir de la aplicación

## Commit suggested structure

### Format

`<type>(<optional scope>): <short description>`


- The `<type>` describes the purpose of the change.
- The `<scope>` (optional) specifies the area of the codebase affected.
- The `<short description>` summarizes the change in a concise way.

### Common Types

| Type     | Description                                      |
|----------|--------------------------------------------------|
| `feat`   | Introduces a new feature                         |
| `fix`    | Fixes a bug or issue                             |
| `docs`   | Updates or improves documentation                |
| `style`  | Code formatting changes (no logic impact)        |
| `refactor` | Code restructuring without changing behavior   |
| `test`   | Adds or updates tests                            |
| `chore`  | Maintenance tasks (e.g., configs, dependencies)  |

### Examples

```bash
feat(auth): add JWT-based login
fix(api): resolve user serialization error
docs(readme): update setup instructions
style(frontend): apply consistent spacing
refactor(models): simplify client-reservation relationship



