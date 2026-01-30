# Practica_Chat_Cliente-Servidor

Este proyecto es un chat multihilo desarrollado en Java utilizando Sockets. Permite que varios usuarios se conecten simultáneamente para chatear en salas públicas o enviarse mensajes privados. Incluye un sistema de administración para gestionar salas, controlar el aforo y expulsar usuarios.

## 📋 Instrucciones de ejecución

1.  Arrancar el servidor:
    Ejecuta la clase `Servidor.java`. Se creará el archivo de log y quedará a la espera de conexiones en el puerto configurado.

2.  Conectar clientes:
    Ejecuta la clase `ClienteChat.java` (tantas veces como usuarios quieras simular).
    * Al entrar, te pedirá un **Nick**.
    * Si eres Admin, te pedirá la **Contraseña**.

3.  Comandos:
    Escribe `/ayuda` dentro del chat para ver la lista completa de acciones disponibles.

## ⚙️ Configuración (chat.properties)

El comportamiento del servidor se puede ajustar sin tocar el código editando el archivo `chat.properties`:

* `servidor.puerto`: Puerto de escucha.
* `sala.aforo.maximo`: Límite de usuarios por sala.
* `sala.aforo.creacion`: Numero de aforo por defecto al crear la sala
* `admins`: Lista de usuarios con permisos de administrador (separados por comas).
* `admin.password`: Contraseña para acceder como administrador.
