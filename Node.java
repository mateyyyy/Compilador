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


    public String getType(TablaSimbolos tabla) {
        switch (nodeType) {
            case "Operation":
                String leftType = left.getType(tabla);
                String rightType = right.getType(tabla);
                if (rightType.equals(leftType)) {
                    System.out.println("Tipos iguales en operacion : ");
                    System.out.println(this.op);
                    this.type = rightType;
                } else {
                    throw new RuntimeException("Los tipos de : " + left.op + ": " + leftType + ", " + right.op + ": " + rightType +" no coinciden");
                }
                return type;

            case "Declaration":
                if(right!=null){
                    leftType = left.getType(tabla);
                    if(leftType == right.getType(tabla)){
                        this.type = leftType;
                        System.out.println("Los tipos coinciden");
                    }
                    else{
                        throw new RuntimeException("Los tipos de : " + left.op + " y " + right.op + " no coinciden");
                    }
                }
                else{
                    this.type = left.getType(tabla);
                }
                return this.type;

            case "Leaf":
                if(this.type == null) {
                    ListElement var = tabla.buscar(op);
                    System.out.println("variable encontrada en tabla : " + var);
                    if(var!=null){
                        this.type = var.getType();
                        return type;
                    }else{
                        throw new RuntimeException("Variable " + op + " no declarada");
                    }
                }
                return this.type;

            default:
                if (right != null)
                    right.getType(tabla);
                if (left != null)
                    left.getType(tabla);
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
                if(right!=null){
                    tabla.Agregar(id, declType, value);
                }
                else{
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

}