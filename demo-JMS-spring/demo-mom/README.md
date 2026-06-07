# DEMO de Mensajería con JMS y ActiveMQ Artemis

Esta guía describe cómo levantar un entorno de mensajería asíncrona, enviar mensajes desde el navegador y supervisarlos visualmente en el broker.

---

## 1. Instalación del Broker

Para no instalar software localmente, utilizamos **Docker**. El siguiente comando levanta el servidor **ActiveMQ Artemis** con la configuración necesaria:

```bash
docker run -it --rm \
  -p 8161:8161 \
  -p 61616:61616 \
  -e ARTEMIS_USER=admin \
  -e ARTEMIS_PASSWORD=admin \
  apache/activemq-artemis:latest-alpine
```

* **Puerto 8161**: Consola web de administración.
* **Puerto 61616**: Puerto de comunicación para la aplicación Java.

---

## 2. Configuración en Spring Boot

En el archivo `src/main/resources/application.properties`, se especifica la conexión al broker:

```properties
spring.artemis.host=localhost
spring.artemis.port=61616
spring.artemis.user=admin
spring.artemis.password=admin

# Modo Punto a Punto (Queues)
spring.jms.pub-sub-domain=false
```

---

## 3. Pruebas de Funcionamiento

### Paso A: Enviar mensajes desde el navegador
Una vez arrancada la aplicación Spring Boot, el navegador actúa como cliente enviando peticiones al **Producer** a través de un endpoint REST:

1.  Abre el navegador y escribe: `http://localhost:8080/api/enviar?texto=Mensaje_Prueba_1`
2.  El navegador responderá "Mensaje enviado ..." de forma instantánea.
3.  En la terminal de la aplicación, verás que el **Consumer** recibe el texto y lo procesa tras un retardo intencionado.

---

## 4. Cómo Ver los Mensajes "Vivos" en Artemis

Si el consumidor es muy rápido, no verás los mensajes en el broker porque se borran al procesarse. Para verlos realmente en la cola, haz lo siguiente:

### Preparación para la Demo
1.  **Detén la aplicación Java** (pero deja Docker encendido).
2. Comenta en ```ColaReceptor``` la anotación ``@JmsListener`` (esto desactiva el consumidor)
3. **Inicia la aplicación Java**.
4. Envía 5 mensajes desde el navegador: `http://localhost:8080/api/enviar?texto=Test_Congelado` (refresca 5 veces).
5. Envía otro mensaje desde el navegador: `http://localhost:8080/api/enviar?texto=Ultimo_mensaje`

### Inspección Visual en el Broker
1.  Entra en la Consola Web: [http://localhost:8161/console](http://localhost:8161/console) (admin/admin).
2.  En el árbol de la izquierda, navega por:
    `addresses` -> `mi_cola` -> `queues` -> `anycast` -> `mi_cola`.
3.  Verás que la columna **Message Count** marca **6**.
4.  Haz clic en la pestaña superior **Operations**.
5.  Busca el botón **browse()** y púlsalo.
6.  Haz clic en **Execute**. Aparecerá la lista de mensajes. Si pulsas en uno, verás el texto enviado en el apartado **Body**.

### Comprobación de entrega de mensajes al consumidor
1.  **Detén la aplicación Java** (pero deja Docker encendido).
2. Descomenta en ```ColaReceptor``` la anotación ``@JmsListener`` (esto desactiva el consumidor)
3. **Inicia la aplicación Java**.
4. Observa en el terminal de la aplicación cómo está recibiendo los mensajes anteriores encolados.