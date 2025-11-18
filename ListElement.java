import java.util.ArrayList;
import java.util.List;

public class ListElement {
    private String id;
    private String type;
    private Object value;
    private boolean esFuncion; // ← indica si es función
    private List<ListElement> parametros; // ← lista de parámetros si es función

    public ListElement(String id, String type, Object value) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.parametros = new ArrayList<>();
    }

    public ListElement(String id, String type) {
        this(id, type, null);
    }

    public ListElement(String id) {
        this(id, null, null);
    }

    // Getters y setters
    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public boolean esFuncion() {
        return esFuncion;
    }

    public List<ListElement> getParametros() {
        return parametros;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setEsFuncion(boolean esFuncion) {
        this.esFuncion = esFuncion;
    }

    public void setParametros(List<ListElement> parametros) {
        this.parametros = parametros;
    }
}
