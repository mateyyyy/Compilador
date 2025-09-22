public class Node {
    public String op;
    public Node left;
    public Node right;
    public String type;
    public String nodeType; // puede ser Operation, Leaf, Declaration, Funcion

    public Node(String op, Node left, Node right, String nodeType) {
        this.op = op;
        this.left = left;
        this.right = right;
        this.nodeType = nodeType;
    }

    public Node(String op, String nodeType, String type) { // Constructor para las hojas
        this.op = op;
        this.nodeType = nodeType;
        this.type = type;
    }

    public String getType() {
        switch (nodeType) {
            case "Operation":
                String leftType = left.getType();
                String rightType = right.getType();
                if (rightType.equals(leftType)) {
                    System.out.println("Tipos iguales en operacion : ");
                    System.out.println(this.op);
                    this.type = rightType;
                } else {
                    System.out.println("Tipos diferentes en operacion : ");
                    System.out.println(this.op);
                }
                return type;

            case "Declaration":
                this.type = right.getType();
                return right.getType();

            case "Leaf":
                return this.type;

            default:
                if (right != null)
                    right.getType();
                if (left != null)
                    left.getType();
                return this.type;
        }
    }

    public void recorrer(TablaSimbolos tabla) {
        recorrer("", tabla);
    }

    private void recorrer(String prefijo, TablaSimbolos tabla) {
        String label = (op != null) ? op : (nodeType != null ? nodeType : "null");
        System.out.println(prefijo + label);
        if ("Declaration".equals(nodeType)) {
            if (left != null) { // izquierda es el id
                String id = left.op;
                String declType = (right != null) ? right.getType() : type;
                String value = (right != null && right.nodeType.equals("Leaf")) ? right.op : null;

                tabla.Agregar(id, declType, value);
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

}