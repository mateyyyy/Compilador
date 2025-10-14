import java.util.HashMap;
import java.util.Map;

public class AssemblerGenerator {
    private StringBuilder code = new StringBuilder();
    private static int tempCount = 0;
    private int stackOffset = 0; // va decrementando para variables locales
    private Map<String, Integer> varOffsets = new HashMap<>();

    public AssemblerGenerator() {}

    public String generateAssembler(Node nodo) {
        code.append(".globl main\n");
        code.append("main:\n");
        code.append("    pushq   %rbp\n");
        code.append("    movq    %rsp, %rbp\n");

        // Reservar espacio para variables (de momento 8 bytes por variable)
        stackOffset = 0;
        generateNode(nodo);

        code.append("    movq    $0, %rax\n");
        code.append("    leave\n");
        code.append("    ret\n");

        return code.toString();
    }

    private String generateNode(Node nodo) {
        if (nodo == null) return "";

        switch (nodo.nodeType) {
            case "Declaration":
                stackOffset -= 8;
                varOffsets.put(nodo.left.op, stackOffset);
                if (nodo.right != null) {
                    String rval = generateNode(nodo.right);
                    emit("movq " + rval + ", %rax");
                    emit("movq %rax, " + stackOffset + "(%rbp)");
                } else {
                    emit("movq $0, %rax");
                    emit("movq %rax, " + stackOffset + "(%rbp)");
                }
                return stackOffset + "(%rbp)";

            case "Asignacion":
                String rval = generateNode(nodo.right);
                Integer offset = varOffsets.get(nodo.left.op);
                if (offset == null) {
                    // variable no declarada, la declaramos en stack
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
                if (nodo.type.equals("int")) return "$" + nodo.op;
                else return nodo.op; // otros tipos simplificados

            case "Operation":
                switch (nodo.op) {
                    case "+":
                        return generateSum(nodo);
                    case "-":
                        return generateSub(nodo);
                    case "*":
                        return generateMul(nodo);
                    case "/":
                        return generateDiv(nodo);
                }
                break;

            default:
                if (nodo.left != null) generateNode(nodo.left);
                if (nodo.right != null) generateNode(nodo.right);
                break;
        }

        return "";
    }

    private String generateSum(Node nodo) {
        String left = generateNode(nodo.left);
        String right = generateNode(nodo.right);
        String temp = newTemp();
        stackOffset -= 8;
        varOffsets.put(temp, stackOffset);

        emit("movq " + left + ", %rax");
        emit("addq " + right + ", %rax");
        emit("movq %rax, " + stackOffset + "(%rbp)");

        return stackOffset + "(%rbp)";
    }

    private String generateSub(Node nodo) {
        String left = generateNode(nodo.left);
        String right = generateNode(nodo.right);
        String temp = newTemp();
        stackOffset -= 8;
        varOffsets.put(temp, stackOffset);

        emit("movq " + left + ", %rax");
        emit("subq " + right + ", %rax");
        emit("movq %rax, " + stackOffset + "(%rbp)");

        return stackOffset + "(%rbp)";
    }

    private String generateMul(Node nodo) {
        String left = generateNode(nodo.left);
        String right = generateNode(nodo.right);
        String temp = newTemp();
        stackOffset -= 8;
        varOffsets.put(temp, stackOffset);

        emit("movq " + left + ", %rax");
        emit("imulq " + right + ", %rax");
        emit("movq %rax, " + stackOffset + "(%rbp)");

        return stackOffset + "(%rbp)";
    }

    private String generateDiv(Node nodo) {
        String left = generateNode(nodo.left);
        String right = generateNode(nodo.right);
        String temp = newTemp();
        stackOffset -= 8;
        varOffsets.put(temp, stackOffset);

        emit("movq " + left + ", %rax");
        emit("cqto"); // extiende rax a rdx:rax
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
}
