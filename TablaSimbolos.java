import java.util.ArrayList;

public class TablaSimbolos {
    private ArrayList<ListElement> Tabla = new ArrayList<>();

    public TablaSimbolos() {
    }

    public void Agregar(String symbol, String type) {
        if (buscar(symbol) == null) {
            Tabla.add(new ListElement(symbol, type));
        } else {
            System.out.println("Variable ya declarada");
        }
    }

    public void Agregar(String symbol, String type, String value) {
        if (buscar(symbol) == null) {
            Tabla.add(new ListElement(symbol, type, value));
        } else {
            System.out.println("Variable ya declarada");
        }
    }

    public ListElement buscar(String target) {
        for (ListElement sym : Tabla) {
            if (sym.getId().equals(target)) {
                return sym;
            }
        }
        return null;
    }

    public void MostrarTabla() {
        for (ListElement s : Tabla) {
            System.out.println(s.getId() + " : " + s.getValue());
        }
    }
}