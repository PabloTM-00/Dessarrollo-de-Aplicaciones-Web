package com.uma.daw.demomom.receptores;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class ColaReceptor {
    // Se activa automáticamente cuando llega algo a "mi_cola"
    @JmsListener(destination = "mi_cola")
    public void recibir(String mensaje) {
        System.out.println(" >>> [CONSUMIDOR] Procesando mensaje: " + mensaje);

        // Simula un proceso pesado
        try { Thread.sleep(2000); } catch (InterruptedException e) {}

        System.out.println(" >>> [CONSUMIDOR] ¡Proceso terminado!");
    }
}
