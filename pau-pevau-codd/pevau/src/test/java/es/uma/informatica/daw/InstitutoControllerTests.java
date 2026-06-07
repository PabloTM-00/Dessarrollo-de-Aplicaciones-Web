package es.uma.informatica.daw.pevau;

import es.uma.informatica.daw.pevau.entidades.EstudianteEntity;
import es.uma.informatica.daw.pevau.entidades.InstitutoEntity;
import es.uma.informatica.daw.pevau.repositorios.RepositorioEstudiante;
import es.uma.informatica.daw.pevau.repositorios.RepositorioInstituto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InstitutoControllerTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RepositorioInstituto repositorioInstituto;

    @Autowired
    private RepositorioEstudiante repositorioEstudiante;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();

        repositorioEstudiante.deleteAll();
        repositorioInstituto.deleteAll();
    }

    @Test
    @DisplayName("[GET] Listar institutos")
    @WithMockUser(authorities = "VICERRECTORADO")
    void consultarInstitutos() throws Exception {
        InstitutoEntity i1 = new InstitutoEntity();
        i1.setNombre("IES Prueba");
        repositorioInstituto.save(i1);

        mockMvc.perform(get("/institutos")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[POST] Crear instituto")
    @WithMockUser(authorities = "VICERRECTORADO")
    void crearInstituto() throws Exception {
        String json = "{\"nombre\":\"IES Nuevo\",\"localidad\":\"Málaga\",\"codigoPostal\":29000}";

        mockMvc.perform(post("/institutos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("[DELETE] Conflict con estudiantes")
    @WithMockUser(authorities = "VICERRECTORADO")
    void eliminarInstitutoConConflicto() throws Exception {
        InstitutoEntity i = new InstitutoEntity();
        i.setNombre("IES Conflicto");
        i.setEstudiantes(new HashSet<>());
        i = repositorioInstituto.save(i);

        EstudianteEntity e = new EstudianteEntity();
        e.setNombre("Juan");
        e.setApellido1("Apellido1");
        e.setApellido2("Apellido2");
        e.setDni("12345678Z");
        e.setTelefono("600000000");
        e.setEmail("juan@test.com");
        e.setInstituto(i);
        e.setIdConvocatoria(1L);
        e.setIdSede(1L);
        e.setNoEliminar(false);
        repositorioEstudiante.save(e);

        mockMvc.perform(delete("/institutos/" + i.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("[GET] Consultar instituto por id")
    @WithMockUser(authorities = "VICERRECTORADO")
    void consultarInstituto() throws Exception {
        InstitutoEntity i = new InstitutoEntity();
        i.setNombre("IES Test");
        i = repositorioInstituto.save(i);

        mockMvc.perform(get("/institutos/" + i.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[GET] Consultar instituto inexistente devuelve 404")
    @WithMockUser(authorities = "VICERRECTORADO")
    void consultarInstitutoNoExiste() throws Exception {
        mockMvc.perform(get("/institutos/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("[PUT] Actualizar instituto existente")
    @WithMockUser(authorities = "VICERRECTORADO")
    void actualizarInstituto() throws Exception {
        InstitutoEntity i = new InstitutoEntity();
        i.setNombre("IES Viejo");
        i = repositorioInstituto.save(i);

        String json = "{\"nombre\":\"IES Nuevo\",\"localidad\":\"Sevilla\",\"codigoPostal\":41001}";

        mockMvc.perform(put("/institutos/" + i.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[DELETE] Eliminar instituto sin estudiantes")
    @WithMockUser(authorities = "VICERRECTORADO")
    void eliminarInstituto() throws Exception {
        InstitutoEntity i = new InstitutoEntity();
        i.setNombre("IES Borrable");
        i = repositorioInstituto.save(i);

        mockMvc.perform(delete("/institutos/" + i.getId()))
                .andExpect(status().isOk());
    }
}