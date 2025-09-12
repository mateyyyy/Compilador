import java.util.ArrayList;

public class TablaSimbolos {
    ArrayList<String> Tabla = new ArrayList<>();
    ArrayList<String> TablaFunc = new ArrayList<>(); // Esta lista la voy a usar para agregar las variables declaradas
                                                     // en los parametros

    public TablaSimbolos() {
    }

    public void Agregar(String symbol) {
        Tabla.add(symbol);
    }

    public void AgregarFunc(String symbol) { // Agrega el simbolo a la tabla de funciones TablaFunc
        TablaFunc.add(symbol);
    }

    public void cleanTablaFunc() {
        TablaFunc.removeAll(TablaFunc);
    }

    public boolean buscar(String target) {
        for (String s : Tabla) {
            if (s.equals(target)) {
                return true;
            }
        }
        return false;
    }

    public void MostrarTabla() {
        for (String s : Tabla) {
            System.out.println(s);
        }
    }

    public boolean Existe(String target) {
        return Tabla.contains(target) || TablaFunc.contains(target);
    }
}