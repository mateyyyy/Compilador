public class ListElement {
    private String id;
    private String type;
    private Object value;

    public ListElement(String id, String type, Object value) {
        this.id = id;
        this.type = type;
        this.value = value;
    }

    public ListElement(String id, String type) {
        this.id = id;
        this.type = type;
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

    public void setValue(Object value) {
        this.value = value;
    }
}