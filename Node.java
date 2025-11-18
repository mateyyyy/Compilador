import java.util.ArrayList;
import java.util.List;

public class Node {
    public String op;
    public Node left;
    public Node right;
    public String type;
    public String nodeType; // puede ser Operation: 1, Leaf: 2, Declaration: 3, Funcion: 4, VarOP(Variable
                            // en expresion): 5, VarDcl (Variable declarada): 6
    public ListElement listElement; // Esto es solo para las hojas

    public Node(String op, Node left, Node right, String nodeType) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.nodeType = nodeType;
        listElement = new ListElement(op);
    }

    public Node(String op, String nodeType, String type) {
        this.op = op;
        this.nodeType = nodeType;
        this.type = type;

        Object valor = null;
        if (op != null) {
            try {
                switch (type) {
                    case "int":
                        // sólo parsear si op es un número entero literal
                        if (op.matches("-?\\d+"))
                            valor = Integer.parseInt(op);
                        break;
                    case "float":
                        // acepta formatos con punto y/o exponencial
                        if (op.matches("-?\\d+(\\.\\d+)?([eE]-?\\d+)?"))
                            valor = Float.parseFloat(op);
                        break;
                    case "boolean":
                        if (op.equals("true") || op.equals("false"))
                            valor = Boolean.parseBoolean(op);
                        break;
                    default:
                        // strings y otros: dejar como op (literal)
                        valor = op;
                }
            } catch (Exception e) {
                // si no se puede parsear, dejar valor null (por ejemplo, identificador usado
                // por error aquí)
                valor = null;
            }
        }

        listElement = new ListElement(op, type, valor);
    }

    public Node(String op, String nodeType) { // Constructor para las variables que esten en expresiones
        this.op = op;
        this.nodeType = nodeType;
    }

    public String getType(TablaSimbolos tabla) {
        switch (nodeType) {
            case "Operation":
                String leftType = left.getType(tabla);
                String rightType = right.getType(tabla);
                if (rightType.equals(leftType)) {
                    System.out.println("Tipos iguales en operacion : ");
                    System.out.println(this.op);
                    listElement.setType(rightType);
                } else {
                    throw new RuntimeException("Los tipos de : " + left.op + ": " + leftType + ", " + right.op + ": "
                            + rightType + " no coinciden");
                }
                return listElement.getType();

            case "Declaration":
                if (right != null) {
                    leftType = left.listElement.getType();
                    if (leftType == right.getType(tabla)) {
                        this.type = leftType;
                        System.out.println("Los tipos coinciden");
                    } else {
                        throw new RuntimeException("Los tipos de : " + left.op + " y " + right.op + " no coinciden");
                    }
                } else {
                    this.type = left.getType(tabla);
                }
                return this.type;

            case "Leaf":
                return listElement.getType();

            case "Asignacion":
                leftType = left.getType(tabla);
                if (leftType.equals(right.getType(tabla))) {
                    this.type = leftType;
                    return leftType;
                } else {
                    throw new RuntimeException("No coinciden los tipos");
                }

            case "VarOP":
                if (this.type == null) {
                    listElement = tabla.buscar(op);
                    if (listElement != null) {
                        this.type = listElement.getType();
                        return this.type;
                    } else {
                        throw new RuntimeException("Variable " + op + " no declarada");
                    }
                }
                return this.type;

            case "func_call":
                // Buscar la función en la tabla de símbolos
                listElement = tabla.buscar(op);
                if (listElement == null) {
                    throw new RuntimeException("Función " + op + " no declarada");
                }

                // El tipo guardado al declarar la función es su tipo de retorno
                this.type = listElement.getType();
                return this.type;

            default:
                if (left != null)
                    left.getType(tabla);
                if (right != null)
                    right.getType(tabla);

                return this.type;
        }
    }

    public void recorrer(TablaSimbolos tabla) {
        recorrer("", tabla);
    }

    private void recorrer(String prefijo, TablaSimbolos tabla) {
        String label = (op != null) ? op : (nodeType != null ? nodeType : "null");
        System.out.println(prefijo + label);
        if (nodeType.equals("Declaration")) {
            if (left != null) { // izquierda es el id
                String id = left.op;
                String declType = left.getType(tabla);
                String value = (right != null && right.nodeType.equals("Leaf")) ? right.op : null;
                if (right != null) {
                    tabla.Agregar(id, declType, value);
                } else {
                    tabla.Agregar(id, declType);
                }
                System.out.println(prefijo + ">> Declarada variable: " + id + " de tipo " + declType);
            }
        }
        if (type != null) {
            System.out.println(prefijo + "Tipo : " + type);
        }
        if (this.left != null) {
            System.out.println(prefijo + "├── Hijo izquierdo:");
            left.recorrer(prefijo + "│   ", tabla);
        }
        if (this.right != null) {
            System.out.println(prefijo + "└── Hijo derecho:");
            right.recorrer(prefijo + "    ", tabla);
        }
    }

    public Object recorridoInterprete(TablaSimbolos tabla) { // Devuelve valor
        switch (nodeType) {
            case "Operation":
                switch (op) {
                    case "+":
                        Object rightValue = right.recorridoInterprete(tabla);
                        Object leftValue = left.recorridoInterprete(tabla);
                        // después podés hacer casting según lo que necesites
                        return ((Integer) rightValue + (Integer) leftValue);

                    case "-":
                        rightValue = right.recorridoInterprete(tabla);
                        leftValue = left.recorridoInterprete(tabla);
                        // después podés hacer casting según lo que necesites
                        return ((Integer) leftValue - (Integer) rightValue);

                    case "*":
                        rightValue = right.recorridoInterprete(tabla);
                        leftValue = left.recorridoInterprete(tabla);
                        // después podés hacer casting según lo que necesites
                        return ((Integer) rightValue * (Integer) leftValue);

                    case "/*":
                        rightValue = right.recorridoInterprete(tabla);
                        leftValue = left.recorridoInterprete(tabla);
                        // después podés hacer casting según lo que necesites
                        return ((Integer) leftValue / (Integer) rightValue);

                    default:
                        return null;
                }

            case "Leaf":
                return listElement.getValue();

            case "Asignacion":
                Object valor = right.recorridoInterprete(tabla);
                System.out.println("Asignando a " + left.op + " el valor " + valor);

                ListElement variable = tabla.buscar(left.op);
                if (variable != null) {
                    variable.setValue(valor); // ← guarda el tipo real (Integer, Float, Boolean, etc.)
                } else {
                    throw new RuntimeException("Variable " + left.op + " no declarada");
                }

                return valor;

            case "Declaration":
                if (right != null) {
                    valor = right.recorridoInterprete(tabla);
                    System.out.println("Asignando a " + left.op + " el valor " + valor);

                    variable = tabla.buscar(left.op);
                    if (variable != null) {
                        variable.setValue(valor); // ← guarda el tipo real (Integer, Float, Boolean, etc.)
                    } else {
                        throw new RuntimeException("Variable " + left.op + " no declarada");
                    }

                    return valor;
                }
                return null;

            case "VarOP":
                ListElement var = tabla.buscar(op);
                if (var == null)
                    throw new RuntimeException("Variable " + op + " no declarada");
                Object val = var.getValue();
                if (val == null)
                    throw new RuntimeException("Variable " + op + " no inicializada");

                return val;

            default:
                if (left != null)
                    left.recorridoInterprete(tabla);
                if (right != null)
                    right.recorridoInterprete(tabla);

                return null;
        }
    }

}