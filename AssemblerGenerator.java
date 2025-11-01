import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    /** NUEVO MÉTODO **/
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

    private void generateIf(Node nodo, TablaSimbolos tabla, String endElseLabel) {
        if (nodo.left.nodeType == "condition") {
            Node nodeCond = nodo.left;
            String ifLabel = newLabel();
            generateCondicion(nodeCond, ifLabel);
            generateNode(nodo.right, tabla);
            if (endElseLabel != null) {
                emit("jmp " + endElseLabel);
            }
            emit(ifLabel + ":");
        }
    }

    private void generateWhile(Node nodo, TablaSimbolos tabla) {
        String labelStart = newLabel();
        emit(labelStart + ": ");
        Node comp = nodo.left;
        String falseLabel = newLabel();

        Set<String> operadores = new HashSet<>();
        operadores.add(">");
        operadores.add("<");
        operadores.add(">=");
        operadores.add("<=");
        operadores.add("==");
        operadores.add("!=");
        if (operadores.contains(comp.op)) {
            generateCondicion(comp, falseLabel);
        } else if (comp.nodeType.equals("AND")) {
            generateAnd(comp, falseLabel, null, null, false);
        } else if (comp.nodeType.equals("OR")) {
            generateOr(comp, falseLabel);
        }
        generateNode(nodo.right, tabla);
        emit("jmp " + labelStart);
        emit(falseLabel + ":");
    }

    public void generateCondicion(Node comp, String falseLabel) {
        String leftValue = "";
        String rightValue = "";
        if (comp.left.nodeType.equals("VarOP")) {
            leftValue = varOffsets.get(comp.left.op).toString();
            leftValue += "(%rbp)";
        } else {
            leftValue = "$" + comp.left.op;
        }
        if (comp.right.nodeType.equals("VarOP")) {
            rightValue = varOffsets.get(comp.right.op).toString();
            rightValue += "(%rbp)";
        } else {
            rightValue = "$" + comp.right.op;
        }
        if (comp.left.nodeType.equals("VarOP")) {
            emit("cmp " + rightValue + ", " + leftValue);
        } else {
            emit("cmp " + leftValue + ", " + rightValue);
        }
        switch (comp.op) {
            case ">":
                emit("jle " + falseLabel);
                break;

            case "==":
                emit("jne " + falseLabel);
                break;

            case "<":
                emit("jge " + falseLabel);
                break;

            case ">=":
                emit("jl " + falseLabel);
                break;

            case "<=":
                emit("jg " + falseLabel);
                break;

            case "!=":
                emit("je " + falseLabel);
                break;

            default:
                break;
        }
    }

    public void generateCondicionInverse(Node comp, String falseLabel) {
        String leftValue = "";
        String rightValue = "";
        if (comp.left.nodeType.equals("VarOP")) {
            leftValue = varOffsets.get(comp.left.op).toString();
            leftValue += "(%rbp)";
        } else {
            leftValue = "$" + comp.left.op;
        }
        if (comp.right.nodeType.equals("VarOP")) {
            rightValue = varOffsets.get(comp.right.op).toString();
            rightValue += "(%rbp)";
        } else {
            rightValue = "$" + comp.right.op;
        }
        if (comp.left.nodeType.equals("VarOP")) {
            emit("cmp " + rightValue + ", " + leftValue);
        } else {
            emit("cmp " + leftValue + ", " + rightValue);
        }
        switch (comp.op) {
            case "<=":
                emit("jle " + falseLabel);
                break;

            case "!=":
                emit("jne " + falseLabel);
                break;

            case ">=":
                emit("jge " + falseLabel);
                break;

            case "<":
                emit("jl " + falseLabel);
                break;

            case ">":
                emit("jg " + falseLabel);
                break;

            case "==":
                emit("je " + falseLabel);
                break;

            default:
                break;
        }
    }

    public void generateAnd(Node nodo, String falseLabel, String trueLabel, String secondConditionLabel, boolean or) {
        boolean isLeftCondition = true;
        boolean isRightCondition = true;
        if (nodo.left.nodeType.equals("AND")) {
            generateAnd(nodo.left, falseLabel, trueLabel, null, false);
            isLeftCondition = false;
        }
        if (nodo.right.nodeType.equals("AND")) {
            generateAnd(nodo.right, falseLabel, trueLabel, null, false);
            isRightCondition = false;
        }

        if (nodo.left.nodeType.equals("OR")) {
            generateOr(nodo.left, falseLabel);
            isLeftCondition = false;
        }
        if (nodo.right.nodeType.equals("OR")) {
            generateOr(nodo.right, falseLabel);
            isRightCondition = false;
        }

        if (isLeftCondition && !or) {
            generateCondicion(nodo.left, falseLabel);
        } else {
            generateCondicion(nodo.left, secondConditionLabel); // en caso de ser falso evalua la segunda condicion
        }
        if (isRightCondition && !or) {
            generateCondicion(nodo.right, falseLabel);
        } else {
            generateCondicionInverse(nodo.right, trueLabel);
        }

    }

    public void generateOr(Node nodo, String falseLabel) {
        boolean isLeftCondition = true;
        boolean isRightCondition = true;
        String trueLabel = newLabel();
        String secondConditionLabel = newLabel();

        if (nodo.left.nodeType.equals("AND")) {
            generateAnd(nodo.left, falseLabel, trueLabel, secondConditionLabel, true);
            isLeftCondition = false;
        }
        if (nodo.right.nodeType.equals("AND")) {
            generateAnd(nodo.right, falseLabel, trueLabel, secondConditionLabel, true);
            isRightCondition = false;
        }

        if (nodo.left.nodeType.equals("OR")) {
            generateOr(nodo.left, falseLabel);
            isLeftCondition = false;
        }
        if (nodo.right.nodeType.equals("OR")) {
            generateOr(nodo.right, falseLabel);
            isRightCondition = false;
        }
        emit(secondConditionLabel + " : ");
        if (isLeftCondition) {
            generateCondicionInverse(nodo.left, trueLabel);
        }

        if (isRightCondition) {
            generateCondicion(nodo.right, falseLabel);
        }
        emit(trueLabel + " :");
    }
}