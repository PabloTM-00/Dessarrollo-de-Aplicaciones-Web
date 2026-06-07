package es.uma.informatica.daw.pevau.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.uma.informatica.daw.pevau.entidades.EstudianteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositorioEstudiante extends JpaRepository<EstudianteEntity, Long> {

    Optional<EstudianteEntity> findByDniAndIdConvocatoria(String dni, Long idConvocatoria);

    List<EstudianteEntity> findByIdConvocatoria(Long idConvocatoria);

    List<EstudianteEntity> findByIdConvocatoriaAndIdSede(Long idConvocatoria, Long idSede);

    @Query("SELECT e FROM EstudianteEntity e WHERE " +
            "(:ap1 IS NULL OR UPPER(e.apellido1) = UPPER(:ap1)) AND " +
            "(:ap2 IS NULL OR UPPER(e.apellido2) = UPPER(:ap2)) AND " +
            "(:nom IS NULL OR UPPER(e.nombre) = UPPER(:nom))")
    List<EstudianteEntity> findByFullName(
            @Param("nom") String nombre,
            @Param("ap1") String apellido1,
            @Param("ap2") String apellido2
    );
}
