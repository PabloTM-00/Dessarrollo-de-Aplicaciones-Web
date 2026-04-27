package es.uma.informatica.daw.practicapruebas.servicios;

import es.uma.informatica.daw.practicapruebas.entidades.Cita;
import es.uma.informatica.daw.practicapruebas.entidades.EstadoCita;
import es.uma.informatica.daw.practicapruebas.excepciones.*;
import es.uma.informatica.daw.practicapruebas.repositorios.RepositorioCitas;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ServicioCitas {

    @Value("${citas.horarioLaboral.horaInicio:9}")
    private int horarioLaboralHoraInicio;
    @Value("${citas.horarioLaboral.horaFin:18}")
    private int horarioLaboralHoraFin;
    @Value("${citas.duracion.minima:15}")
    private int duracionMinima;
    @Value("${citas.duracion.maxima:15}")
    private int duractionMaxima;
    @Value("${citas.horaMinimaCancelacion}")
    private int horaMinimaCancelacion;

    private RepositorioCitas repositorio;

    public ServicioCitas(RepositorioCitas repositorio) {
        this.repositorio = repositorio;
    }

    public Cita crear(Cita cita) {
        if (cita.getDuracion() == null || cita.getDuracion() < duracionMinima || cita.getDuracion() > duractionMaxima) {
            throw new DuracionNoValidaException(
                String.format("La duración de la cita no es válida, tiene que estar entre %d y %d minutos",
                    duracionMinima, duractionMaxima));
        }

        if (cita.getInicio()==null || !esHorarioLaboral(cita.getInicio())) {
            throw new FueraHorarioLaboralException();
        }

        if (hayConflicto(cita.getInicio(), cita.getFin())) {
            throw new ConflictoCitasException();
        }

        cita.setId(null);
        cita.setEstado(EstadoCita.CREADA);
        return repositorio.save(cita);
    }

    public Cita obtener(Long id) {
        return repositorio.findById(id)
            .orElseThrow(CitaNoEncontradaException::new);
    }

    public Cita confirmar(Long id) {
        Cita cita = obtener(id);

        if (cita.getEstado() != EstadoCita.CREADA) {
            throw new EstadoInvalidoException("Solo se pueden confirmar citas en estado CREADA");
        }

        cita.setEstado(EstadoCita.CONFIRMADA);
        return cita;
    }

    public Cita cancelar(Long id) {
        Cita cita = obtener(id);

        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new EstadoInvalidoException("La cita ya está cancelada");
        }

        if (Duration.between(LocalDateTime.now(), cita.getInicio()).toHours() < horaMinimaCancelacion) {
            throw new CitaCercanaException("Demasiado tarde para cancelar la cita");
        }

        cita.setEstado(EstadoCita.CANCELADA);
        return cita;
    }

    public List<Cita> buscarPorFecha(LocalDate fecha) {
        return repositorio.findAll().stream()
            .filter(c -> c.getInicio().toLocalDate().equals(fecha))
            .toList();
    }

    private boolean esHorarioLaboral(LocalDateTime inicio) {
        int hora = inicio.getHour();
        return hora >= horarioLaboralHoraInicio && hora < horarioLaboralHoraFin;
    }

    private boolean hayConflicto(LocalDateTime inicio, LocalDateTime fin) {
        return repositorio.findAll().stream().anyMatch(c ->
            c.getEstado() != EstadoCita.CANCELADA &&
                inicio.isBefore(c.getFin()) ||
                fin.isAfter(c.getInicio())
        );
    }
}