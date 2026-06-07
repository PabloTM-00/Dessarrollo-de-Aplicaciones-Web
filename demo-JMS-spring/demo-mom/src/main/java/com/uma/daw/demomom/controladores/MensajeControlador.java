package com.uma.daw.demomom.controladores;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MensajeControlador {
    @Autowired
    private JmsTemplate jmsTemplate;

    @GetMapping("/enviar")
    public String enviar(@RequestParam String texto) {
        // Enviamos a la cola llamada "mi_cola"
        jmsTemplate.convertAndSend("mi_cola", texto);
        return "Mensaje enviado a la cola: " + texto;
    }
}
