package es.uma.informatica.daw.practicapruebas;

import es.uma.informatica.daw.practicapruebas.dtos.CitaDTO;
import es.uma.informatica.daw.practicapruebas.dtos.CitaNuevaDTO;
import es.uma.informatica.daw.practicapruebas.dtos.Mapper;
import es.uma.informatica.daw.practicapruebas.dtos.MensajeErrorDTO;
import es.uma.informatica.daw.practicapruebas.entidades.Cita;
import es.uma.informatica.daw.practicapruebas.entidades.EstadoCita;
import es.uma.informatica.daw.practicapruebas.repositorios.RepositorioCitas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureTestRestTemplate
@DisplayName("Pruebas del servicio de citas")
class PracticaPruebasApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    int port;

    @Autowired
    private RepositorioCitas repositorioCitas;

    private String url(String ruta) {
        return "http://localhost:" + port + ruta;
    }

    private CitaNuevaDTO crearCitaDTO(String cliente, LocalDateTime inicio, Integer duracion) {
        CitaNuevaDTO dto = new CitaNuevaDTO();
        dto.setCliente(cliente);
        dto.setInicio(inicio);
        dto.setDuracion(duracion);
        return dto;
    }

    @Test
    @DisplayName("Crea cita , devuelve 201")
    void crearCitaOk() {
        CitaNuevaDTO dto = crearCitaDTO("Pablo", LocalDateTime.parse("2026-04-27T16:00:00"), 60);
        ResponseEntity<CitaDTO> res = restTemplate.postForEntity(url("/citas"), dto, CitaDTO.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getHeaders().getLocation()).isNotNull();
        assertThat(res.getBody().getCliente()).isEqualTo("Pablo");
        assertThat(res.getBody().getEstado()).isEqualTo(EstadoCita.CREADA);
    }

    @Test
    @DisplayName("Falla al crear cita con duración nula")
    void crearCitaDuracionNula() {
        CitaNuevaDTO dto = crearCitaDTO("Pablo", LocalDateTime.parse("2026-04-27T16:00:00"), null);
        ResponseEntity<MensajeErrorDTO> res = restTemplate.postForEntity(url("/citas"), dto, MensajeErrorDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Falla al crear cita con duración menor a 15 mins")
    void crearCitaDuracionCorta() {
        CitaNuevaDTO dto = crearCitaDTO("Pablo", LocalDateTime.parse("2026-04-27T16:00:00"), 13);
        ResponseEntity<MensajeErrorDTO> res = restTemplate.postForEntity(url("/citas"), dto, MensajeErrorDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Falla al crear cita con duración mayor a 120 min")
    void crearCitaDuracionLarga() {
        CitaNuevaDTO dto = crearCitaDTO("Pablo", LocalDateTime.parse("2026-04-27T16:00:00"), 180);
        ResponseEntity<MensajeErrorDTO> res = restTemplate.postForEntity(url("/citas"), dto, MensajeErrorDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Falla al crear cita con inicio nulo")
    void crearCitaInicioNulo() {
        CitaNuevaDTO dto = crearCitaDTO("Pablo", null, 60);
        ResponseEntity<MensajeErrorDTO> res = restTemplate.postForEntity(url("/citas"), dto, MensajeErrorDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Falla al crear cita antes del horario laboral (09:00)")
    void crearCitaAntesHorario() {
        CitaNuevaDTO dto = crearCitaDTO("Pablo", LocalDateTime.parse("2026-05-01T08:35:00"), 60);
        ResponseEntity<MensajeErrorDTO> res = restTemplate.postForEntity(url("/citas"), dto, MensajeErrorDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Falla al crear cita después del horario laboral (18:00)")
    void crearCitaDespuesHorario() {
        CitaNuevaDTO dto = crearCitaDTO("Pepe", LocalDateTime.parse("2026-05-01T18:39:00"), 60);
        ResponseEntity<MensajeErrorDTO> res = restTemplate.postForEntity(url("/citas"), dto, MensajeErrorDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Falla devolviendo 409 si hay conflicto con otra cita")
    void crearCitaConflicto() {
        // Insertamos directamente en base de datos para simular cita existente
        Cita cita = new Cita();
        cita.setCliente("Juan");
        cita.setInicio(LocalDateTime.parse("2026-05-01T10:00:00"));
        cita.setDuracion(60);
        cita.setEstado(EstadoCita.CREADA);
        repositorioCitas.save(cita);

        CitaNuevaDTO dto2 = crearCitaDTO("Pepe", LocalDateTime.parse("2026-05-01T10:30:00"), 60);
        ResponseEntity<MensajeErrorDTO> res = restTemplate.postForEntity(url("/citas"), dto2, MensajeErrorDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // --- PRUEBAS DE OBTENCIÓN Y BÚSQUEDA ---

    @Test
    @DisplayName("al buscar por fecha se encuentran las citas de ese día")
    void buscarPorFecha() {
        Cita cita = new Cita();
        cita.setCliente("Juan");
        cita.setInicio(LocalDateTime.parse("2026-04-30T10:00:00"));
        cita.setDuracion(60);
        cita.setEstado(EstadoCita.CREADA);
        repositorioCitas.save(cita);

        ResponseEntity<CitaDTO[]> res = restTemplate.getForEntity(url("/citas?fecha=2026-04-30"), CitaDTO[].class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Obtener una cita por id correctamente")
    void obtenerCitaOk() {
        Cita cita = new Cita();
        cita.setCliente("Juan");
        cita.setInicio(LocalDateTime.parse("2026-04-27T16:00:00"));
        cita.setDuracion(60);
        cita.setEstado(EstadoCita.CREADA);
        cita = repositorioCitas.save(cita);

        ResponseEntity<CitaDTO> res = restTemplate.getForEntity(url("/citas/" + cita.getId()), CitaDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getId()).isEqualTo(cita.getId());
    }

    @Test
    @DisplayName("devuelve 404 si la cita no existe")
    void obtenerCitaNoExiste() {
        ResponseEntity<Void> res = restTemplate.getForEntity(url("/citas/999"), Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Confirmacita correctamente")
    void confirmarCitaOk() {
        Cita cita = new Cita();
        cita.setCliente("Carlos");
        cita.setInicio(LocalDateTime.parse("2026-04-27T16:00:00"));
        cita.setDuracion(60);
        cita.setEstado(EstadoCita.CREADA);
        cita = repositorioCitas.save(cita);

        ResponseEntity<CitaDTO> res = restTemplate.postForEntity(url("/citas/" + cita.getId() + "/confirmar"), null, CitaDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getEstado()).isEqualTo(EstadoCita.CONFIRMADA);
    }

    @Test
    @DisplayName("Falla al confirmar cita si no está en estado CREADA")
    void confirmarCitaNoCreada() {
        Cita cita = new Cita();
        cita.setCliente("Luis");
        cita.setInicio(LocalDateTime.parse("2026-04-27T16:00:00"));
        cita.setDuracion(60);
        cita.setEstado(EstadoCita.CONFIRMADA);
        cita = repositorioCitas.save(cita);

        ResponseEntity<MensajeErrorDTO> res = restTemplate.postForEntity(url("/citas/" + cita.getId() + "/confirmar"), null, MensajeErrorDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }


    @Test
    @DisplayName("Cancelar cita correctamente si falta más de 24h")
    void cancelarCitaOk() {
        Cita cita = new Cita();
        cita.setCliente("Juan");
        cita.setInicio(LocalDateTime.now().plusDays(4).withHour(10));
        cita.setDuracion(60);
        cita.setEstado(EstadoCita.CREADA);
        cita = repositorioCitas.save(cita);

        ResponseEntity<CitaDTO> res = restTemplate.postForEntity(url("/citas/" + cita.getId() + "/cancelar"), null, CitaDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getEstado()).isEqualTo(EstadoCita.CANCELADA);
    }

    @Test
    @DisplayName("Falla al cancelar si ya cancelada")
    void cancelarCitaYaCancelada() {
        Cita cita = new Cita();
        cita.setCliente("Carlos");
        cita.setInicio(LocalDateTime.now().plusDays(2).withHour(10));
        cita.setDuracion(60);
        cita.setEstado(EstadoCita.CANCELADA);
        cita = repositorioCitas.save(cita);

        ResponseEntity<MensajeErrorDTO> res = restTemplate.postForEntity(url("/citas/" + cita.getId() + "/cancelar"), null, MensajeErrorDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Falla al cancelar cita con menos de 24 horas de antelación")
    void cancelarCitaMenos24h() {
        Cita cita = new Cita();
        cita.setCliente("Jorge");
        cita.setInicio(LocalDateTime.now().plusHours(7)); // faltan 7 horas
        cita.setDuracion(60);
        cita.setEstado(EstadoCita.CREADA);
        cita = repositorioCitas.save(cita);

        ResponseEntity<MensajeErrorDTO> res = restTemplate.postForEntity(url("/citas/" + cita.getId() + "/cancelar"), null, MensajeErrorDTO.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("maper controla opjetos nulos")
    void pruebaMapperNull() {
        assertThat(Mapper.toCita(null)).isNull();
        assertThat(Mapper.toCitaDTO(null)).isNull();
    }

    @Test
    @DisplayName("evalua ramas logicas")
    void coberturaExtraRamasConflicto() {

        Cita citaCancelada = new Cita();
        citaCancelada.setCliente("Lara");
        citaCancelada.setInicio(LocalDateTime.parse("2026-05-01T09:30:00"));
        citaCancelada.setDuracion(60);
        citaCancelada.setEstado(EstadoCita.CANCELADA);
        repositorioCitas.save(citaCancelada);

        Cita citaTemprana = new Cita();
        citaTemprana.setCliente("Luis");
        citaTemprana.setInicio(LocalDateTime.parse("2026-05-01T11:00:00"));
        citaTemprana.setDuracion(60);
        citaTemprana.setEstado(EstadoCita.CREADA);
        repositorioCitas.save(citaTemprana);

        CitaNuevaDTO dto = crearCitaDTO("Pepe", LocalDateTime.parse("2026-05-07T15:01:00"), 60);
        restTemplate.postForEntity(url("/citas"), dto, MensajeErrorDTO.class);
    }

    @Test
    @DisplayName("Ramas del bug logico mencionado en la entrega")
    void cazarUltimasRamasConflicto() {

        Cita citaFuturaCancelada = new Cita();
        citaFuturaCancelada.setCliente("Jorge");
        citaFuturaCancelada.setInicio(LocalDateTime.parse("2026-05-01T15:00:00"));
        citaFuturaCancelada.setDuracion(60);
        citaFuturaCancelada.setEstado(EstadoCita.CANCELADA);
        repositorioCitas.save(citaFuturaCancelada);

        Cita citaPasadaCreada = new Cita();
        citaPasadaCreada.setCliente("Luis");
        citaPasadaCreada.setInicio(LocalDateTime.parse("2026-05-01T09:00:00"));
        citaPasadaCreada.setDuracion(60);
        citaPasadaCreada.setEstado(EstadoCita.CREADA);
        repositorioCitas.save(citaPasadaCreada);


        CitaNuevaDTO dto = crearCitaDTO("Pepe", LocalDateTime.parse("2026-05-01T10:00:00"), 60);
        restTemplate.postForEntity(url("/citas"), dto, MensajeErrorDTO.class);
    }
}