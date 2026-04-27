package es.uma.informatica.daw.practicapruebas.repositorios;

import es.uma.informatica.daw.practicapruebas.entidades.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioCitas extends JpaRepository<Cita, Long> {
}
