package es.uma.informatica.daw.pevau;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PevauApplicationTests {

    @Test
    @DisplayName("Debe arrancar la aplicación sin excepciones")
    void main() {

        // Arrange
        String[] args = {};

        // Act + Assert
        assertDoesNotThrow(() ->
                PevauApplication.main(args)
        );
    }
}
