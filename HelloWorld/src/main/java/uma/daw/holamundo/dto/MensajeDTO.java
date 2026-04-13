package uma.daw.holamundo.dto;

public class MensajeDTO {
    private String contenido;
    private String emisor;

    // Spring usará este constructor o los setters para generar el JSON
    public MensajeDTO(String contenido, String emisor) {
        this.setContenido(contenido);
        this.setEmisor(emisor);
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getEmisor() {
        return emisor;
    }

    public void setEmisor(String emisor) {
        this.emisor = emisor;
    }
}
