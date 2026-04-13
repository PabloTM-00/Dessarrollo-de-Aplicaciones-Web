package uma.daw.holamundo.modelo;

public class Mensaje {
    private String contenido;
    private String emisor;
    private String ipOrigen; // Campo sensible/técnico interno

    public Mensaje(String contenido, String emisor, String ip) {
        this.setContenido(contenido);
        this.setEmisor(emisor);
        this.setIpOrigen(ip);
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

    public String getIpOrigen() {
        return ipOrigen;
    }

    public void setIpOrigen(String ipOrigen) {
        this.ipOrigen = ipOrigen;
    }
}
