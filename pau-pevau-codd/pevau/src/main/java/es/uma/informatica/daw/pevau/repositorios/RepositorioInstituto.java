package es.uma.informatica.daw.pevau.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.uma.informatica.daw.pevau.entidades.InstitutoEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositorioInstituto extends JpaRepository<InstitutoEntity, Long> {

    //Buscar instituto por nombre exacto
    Optional<InstitutoEntity> findByNombre(String nombre);
    List<InstitutoEntity> findByNombreContainingIgnoreCase(String nombre);

    //Busqueda para filtros (no implementados en el controlador, pero pense que serian utiles)
    List<InstitutoEntity> findByLocalidad(String localidad);
    List<InstitutoEntity> findByLocalidadContainingIgnoreCase(String localidad);

    //Busca por nombre y localidad
    Optional<InstitutoEntity> findByNombreAndLocalidad(String nombre, String localidad);
}