package es.uma.informatica.daw.practicapruebas.controladores;

import es.uma.informatica.daw.practicapruebas.dtos.CitaDTO;
import es.uma.informatica.daw.practicapruebas.dtos.CitaNuevaDTO;
import es.uma.informatica.daw.practicapruebas.dtos.Mapper;
import es.uma.informatica.daw.practicapruebas.dtos.MensajeErrorDTO;
import es.uma.informatica.daw.practicapruebas.entidades.Cita;
import es.uma.informatica.daw.practicapruebas.excepciones.*;
import es.uma.informatica.daw.practicapruebas.servicios.ServicioCitas;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/citas")
public class ControladorCitas {

    private final ServicioCitas servicio;

    public ControladorCitas(ServicioCitas servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<CitaDTO> crear(@RequestBody CitaNuevaDTO cita, UriComponentsBuilder uriBuilder) {
        Cita entidad = servicio.crear(Mapper.toCita(cita));
        var uri = uriBuilder.path("/citas/{id}").buildAndExpand(entidad.getId()).toUri();
        return ResponseEntity.created(uri).body(Mapper.toCitaDTO(entidad));
    }

    @GetMapping("/{id}")
    public CitaDTO obtener(@PathVariable Long id) {
        return Mapper.toCitaDTO(servicio.obtener(id));
    }

    @PostMapping("/{id}/confirmar")
    public CitaDTO confirmar(@PathVariable Long id) {
        return Mapper.toCitaDTO(servicio.confirmar(id));
    }

    @PostMapping("/{id}/cancelar")
    public CitaDTO cancelar(@PathVariable Long id) {
        return Mapper.toCitaDTO(servicio.cancelar(id));
    }

    @GetMapping
    public List<CitaDTO> buscarPorFecha(@RequestParam String fecha) {
        return servicio.buscarPorFecha(LocalDate.parse(fecha))
            .stream()
            .map(Mapper::toCitaDTO)
            .toList();
    }

    @ExceptionHandler(CitaNoEncontradaException.class)
    public ResponseEntity<Void> noEncontrada() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(ConflictoCitasException.class)
    public ResponseEntity<MensajeErrorDTO> conflicto(ConflictoCitasException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new MensajeErrorDTO(ex.getMessage()));
    }

    @ExceptionHandler({DuracionNoValidaException.class,
        FueraHorarioLaboralException.class,
        EstadoInvalidoException.class,
        CitaCercanaException.class})
    public ResponseEntity<MensajeErrorDTO> argumentosIncorrectos(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new MensajeErrorDTO(ex.getMessage()));
    }
}
