package es.uma.informatica.daw.pevau;

import es.uma.informatica.daw.pevau.seguridad.JwtAuthFilter;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthFilterTests {

    private JwtAuthFilter filter;
    private FilterChain filterChain;
    private HttpServletRequest request;
    private HttpServletResponse response;

    private final String SECRET = "desarrollodeaplicacionesweb20252026desarrollodeaplicacionesweb20252026";
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter();
        filterChain = mock(FilterChain.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        SecurityContextHolder.clearContext();
    }

    private String generarToken(String userId, List<String> roles) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", roles)
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("Sin header Authorization pasa sin autenticar")
    void sinHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Con token válido autentica al usuario")
    void tokenValido() throws Exception {
        String token = generarToken("123", List.of("VICERRECTORADO"));
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("123");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Con token inválido limpia el contexto y sigue la cadena")
    void tokenInvalido() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer tokenbasura");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Header sin Bearer pasa sin autenticar")
    void headerSinBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic algo");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}