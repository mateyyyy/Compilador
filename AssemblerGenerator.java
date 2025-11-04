import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AssemblerGenerator {
    private StringBuilder code = new StringBuilder();
    private static int tempCount = 0;
    private int stackOffset = 0;
    private Map<String, Integer> varOffsets = new HashMap<>();

    private int finIfLabel = 0;

    public AssemblerGenerator() {
    }

    public String generateAssembler(Node nodo, TablaSimbolos tabla) {
        code.append(".globl main\n");
        code.append("main:\n");
        code.append("    pushq   %rbp\n");
        code.append("    movq    %rsp, %rbp\n");

        stackOffset = 0;
        generateNode(nodo, tabla);

        code.append("    movq    $0, %rax\n");
        code.append("    leave\n");
        code.append("    ret\n");

        return code.toString();
    }

    public void saveToFile(String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(code.toString());
            System.out.println("Código guardado en: " + fileName);
        } catch (IOException e) {
            System.err.println("Error al guardar el archivo: " + e.getMessage());
        }
    }

    private String generateNode(Node nodo, TablaSimbolos tabla) {
        if (nodo == null)
            return "";

        switch (nodo.nodeType) {
            case "Declaration":
                stackOffset -= 8;
                varOffsets.put(nodo.left.op, stackOffset);
                if (nodo.right != null) {
                    String rval = generateNode(nodo.right, tabla);
                    emit("movq " + rval + ", %rax");
                    emit("movq %rax, " + stackOffset + "(%rbp)");
                } else {
                    emit("movq $0, %rax");
                    emit("movq %rax, " + stackOffset + "(%rbp)");
                }
                return stackOffset + "(%rbp)";

            case "Asignacion":
                String rval = generateNode(nodo.right, tabla);
                Integer offset = varOffsets.get(nodo.left.op);
                if (offset == null) {
                    stackOffset -= 8;
                    offset = stackOffset;
                    varOffsets.put(nodo.left.op, offset);
                }
                emit("movq " + rval + ", %rax");
                emit("movq %rax, " + offset + "(%rbp)");
                return offset + "(%rbp)";

            case "VarOP":
                return varOffsets.containsKey(nodo.op) ? varOffsets.get(nodo.op) + "(%rbp)" : nodo.op;

            case "Leaf":
                if (nodo.type.equals("int"))
                    return "$" + nodo.op;
                else
                    return nodo.op;

            case "Operation":
                switch (nodo.op) {
                    case "+":
                        return generateSum(nodo, tabla);
                    case "-":
                        return generateSub(nodo, tabla);
                    case "*":
                        return generateMul(nodo, tabla);
                    case "/":
                        return generateDiv(nodo, tabla);
                }
                break;

            case "if":
                generateIf(nodo, tabla, null);
                break;

            case "ifelse":
                String endElseLabel = newLabel();
                generateIf(nodo.left, tabla, endElseLabel);
                generateNode(nodo.right.right, tabla);
                emit(endElseLabel + ":");
                break;

            case "while":
                generateWhile(nodo, tabla);
                break;

            default:
                if (nodo.left != null)
                    generateNode(nodo.left, tabla);
                if (nodo.right != null)
                    generateNode(nodo.right, tabla);
                break;
        }

        return "";
    }

    private String generateSum(Node nodo, TablaSimbolos tabla) {
        String left = generateNode(nodo.left, tabla);
        String right = generateNode(nodo.right, tabla);
        String temp = newTemp();
        stackOffset -= 8;
        varOffsets.put(temp, stackOffset);

        emit("movq " + left + ", %rax");
        emit("addq " + right + ", %rax");
        emit("movq %rax, " + stackOffset + "(%rbp)");
        return stackOffset + "(%rbp)";
    }

    private String generateSub(Node nodo, TablaSimbolos tabla) {
        String left = generateNode(nodo.left, tabla);
        String right = generateNode(nodo.right, tabla);
        String temp = newTemp();
        stackOffset -= 8;
        varOffsets.put(temp, stackOffset);

        emit("movq " + left + ", %rax");
        emit("subq " + right + ", %rax");
        emit("movq %rax, " + stackOffset + "(%rbp)");
        return stackOffset + "(%rbp)";
    }

    private String generateMul(Node nodo, TablaSimbolos tabla) {
        String left = generateNode(nodo.left, tabla);
        String right = generateNode(nodo.right, tabla);
        String temp = newTemp();
        stackOffset -= 8;
        varOffsets.put(temp, stackOffset);

        emit("movq " + left + ", %rax");
        emit("imulq " + right + ", %rax");
        emit("movq %rax, " + stackOffset + "(%rbp)");
        return stackOffset + "(%rbp)";
    }

    private String generateDiv(Node nodo, TablaSimbolos tabla) {
        String left = generateNode(nodo.left, tabla);
        String right = generateNode(nodo.right, tabla);
        String temp = newTemp();
        stackOffset -= 8;
        varOffsets.put(temp, stackOffset);

        emit("movq " + left + ", %rax");
        emit("cqto");
        emit("idivq " + right);
        emit("movq %rax, " + stackOffset + "(%rbp)");
        return stackOffset + "(%rbp)";
    }

    private void emit(String line) {
        code.append("    ").append(line).append("\n");
    }

    private String newTemp() {
        return "T" + (tempCount++);
    }

    public String newLabel() {
        String label = ".L" + this.finIfLabel;
        this.finIfLabel++;
        return label;
    }

    // ============= FUNCIONES PARA MANEJO DE CONDICIONES =============

    /**
     * Genera código para una condición lógica completa (puede incluir AND, OR, o condiciones simples)
     * Salta a falseLabel si la condición completa es FALSE
     */
    private void generateCondicionLogica(Node nodo, String falseLabel) {
        if (nodo.nodeType.equals("AND")) {
            // Para AND: si cualquiera falla, ir a falseLabel
            generateCondicionLogica(nodo.left, falseLabel);
            generateCondicionLogica(nodo.right, falseLabel);

        } else if (nodo.nodeType.equals("OR")) {
            // Para OR: si la primera es TRUE, saltar el resto (cortocircuito)
            String trueLabel = newLabel();
            String endLabel = newLabel();

            // Evaluar primera condición: si es TRUE, ir a trueLabel (éxito)
            generateCondicionInvertida(nodo.left, trueLabel);

            // Si llegamos aquí, primera fue FALSE, evaluar segunda normalmente
            generateCondicionLogica(nodo.right, falseLabel);
            emit("jmp " + endLabel);

            emit(trueLabel + ":");
            emit(endLabel + ":");

        } else if (nodo.nodeType.equals("condition")) {
            // Condición simple
            generateCondicionSimple(nodo, falseLabel);
        }
    }

    /**
     * Genera código para una condición invertida
     * Salta a trueLabel si la condición es TRUE
     * Se usa para OR (evaluación con cortocircuito)
     */
    private void generateCondicionInvertida(Node nodo, String trueLabel) {
        if (nodo.nodeType.equals("condition")) {
            String leftValue = getValueString(nodo.left);
            String rightValue = getValueString(nodo.right);

            emit("movq " + leftValue + ", %rax");
            emit("cmp " + rightValue + ", %rax");

            // Saltos INVERTIDOS (si condición es TRUE, saltar)
            switch (nodo.op) {
                case ">":  emit("jg " + trueLabel); break;
                case "<":  emit("jl " + trueLabel); break;
                case ">=": emit("jge " + trueLabel); break;
                case "<=": emit("jle " + trueLabel); break;
                case "==": emit("je " + trueLabel); break;
                case "!=": emit("jne " + trueLabel); break;
            }
        } else if (nodo.nodeType.equals("AND")) {
            // AND invertido: necesitamos que AMBAS sean TRUE para saltar
            String checkSecond = newLabel();

            // Si primera es FALSE, no saltamos (ir a checkSecond)
            generateCondicionSimple(nodo.left, checkSecond);
            // Si llegamos aquí, primera es TRUE, verificar segunda
            generateCondicionInvertida(nodo.right, trueLabel);

            emit(checkSecond + ":");

        } else if (nodo.nodeType.equals("OR")) {
            // OR invertido: si CUALQUIERA es TRUE, saltamos
            generateCondicionInvertida(nodo.left, trueLabel);
            generateCondicionInvertida(nodo.right, trueLabel);
        }
    }

    /**
     * Genera código para una condición simple (>, <, ==, etc.)
     * Salta a falseLabel si la condición es FALSE
     */
    private void generateCondicionSimple(Node nodo, String falseLabel) {
        String leftValue = getValueString(nodo.left);
        String rightValue = getValueString(nodo.right);

        emit("movq " + leftValue + ", %rax");
        emit("cmp " + rightValue + ", %rax");

        // Saltos normales (si condición es FALSE, saltar)
        switch (nodo.op) {
            case ">":  emit("jle " + falseLabel); break;
            case "<":  emit("jge " + falseLabel); break;
            case ">=": emit("jl " + falseLabel); break;
            case "<=": emit("jg " + falseLabel); break;
            case "==": emit("jne " + falseLabel); break;
            case "!=": emit("je " + falseLabel); break;
        }
    }

    /**
     * Helper para obtener la representación en string de un valor
     * (variable o constante)
     */
    private String getValueString(Node node) {
        if (node.nodeType.equals("VarOP")) {
            return varOffsets.get(node.op) + "(%rbp)";
        } else {
            return "$" + node.op;
        }
    }

    // ============= GENERACIÓN DE IF Y WHILE =============

    /**
     * Genera código para un IF statement
     */
    private void generateIf(Node nodo, TablaSimbolos tabla, String endElseLabel) {
        String falseLabel = newLabel();

        // Generar condición (salta a falseLabel si es FALSE)
        generateCondicionLogica(nodo.left, falseLabel);

        // Código del if (se ejecuta si condición es TRUE)
        generateNode(nodo.right, tabla);

        // Si hay else, saltar al final después del bloque if
        if (endElseLabel != null) {
            emit("jmp " + endElseLabel);
        }

        emit(falseLabel + ":");
    }

    /**
     * Genera código para un WHILE statement
     */
    private void generateWhile(Node nodo, TablaSimbolos tabla) {
        String labelStart = newLabel();
        String falseLabel = newLabel();

        emit(labelStart + ":");

        // Evaluar condición (salta a falseLabel si es FALSE)
        generateCondicionLogica(nodo.left, falseLabel);

        // Cuerpo del while (se ejecuta si condición es TRUE)
        generateNode(nodo.right, tabla);

        // Volver al inicio del while
        emit("jmp " + labelStart);
        emit(falseLabel + ":");
    }
}