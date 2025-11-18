import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

public class AssemblerGenerator {
    private StringBuilder code = new StringBuilder();
    private static int tempCount = 0;
    private int stackOffset = 0;
    private Map<String, Integer> varOffsets = new HashMap<>();
    private int finIfLabel = 0;

    // Para manejar funciones
    private Map<String, FunctionInfo> functions = new HashMap<>();
    private String currentFunction = null;
    private boolean inMainFunction = true;

    // Clase auxiliar para guardar información de funciones
    private static class FunctionInfo {
        String name;
        String returnType;
        List<String> params;
        int localVarCount;

        public FunctionInfo(String name, String returnType) {
            this.name = name;
            this.returnType = returnType;
            this.params = new ArrayList<>();
            this.localVarCount = 0;
        }
    }

    public AssemblerGenerator() {
    }

    public String generateAssembler(Node nodo, TablaSimbolos tabla) {
        // Primera pasada: registrar todas las funciones
        registerFunctions(nodo, tabla);

        // Segunda pasada: generar código
        if (nodo.nodeType.equals("program")) {
            // Si hay funciones definidas, generarlas primero
            if (nodo.left != null) {
                generateFunctions(nodo.left, tabla);
            }

            // Generar main
            code.append(".globl main\n");
            code.append("main:\n");
            code.append("    pushq   %rbp\n");
            code.append("    movq    %rsp, %rbp\n");

            stackOffset = 0;
            varOffsets.clear();
            inMainFunction = true;
            currentFunction = "main";

            // Generar código del main
            if (nodo.right != null) {
                generateNode(nodo.right, tabla);
            }

            code.append("    movq    $0, %rax\n");
            code.append("    leave\n");
            code.append("    ret\n");
        } else if (nodo.nodeType.equals("root")) {
            // Solo hay main, sin funciones
            code.append(".globl main\n");
            code.append("main:\n");
            code.append("    pushq   %rbp\n");
            code.append("    movq    %rsp, %rbp\n");

            stackOffset = 0;
            varOffsets.clear();
            inMainFunction = true;
            currentFunction = "main";

            if (nodo.right != null) {
                generateNode(nodo.right, tabla);
            }

            code.append("    movq    $0, %rax\n");
            code.append("    leave\n");
            code.append("    ret\n");
        }

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

    /**
     * Primera pasada: registrar todas las funciones del programa
     */
    private void registerFunctions(Node nodo, TablaSimbolos tabla) {
        if (nodo == null)
            return;

        if (nodo.nodeType.equals("func_decl")) {
            String funcName = nodo.op;
            String returnType = nodo.left.op;
            FunctionInfo info = new FunctionInfo(funcName, returnType);
            functions.put(funcName, info);
        } else if (nodo.nodeType.equals("functions")) {
            registerFunctions(nodo.left, tabla);
            registerFunctions(nodo.right, tabla);
        }
    }

    /**
     * Generar código para todas las funciones definidas
     */
    private void generateFunctions(Node nodo, TablaSimbolos tabla) {
        if (nodo == null)
            return;

        if (nodo.nodeType.equals("func_decl")) {
            generateFunctionDecl(nodo, tabla);
        } else if (nodo.nodeType.equals("functions")) {
            generateFunctions(nodo.left, tabla);
            generateFunctions(nodo.right, tabla);
        }
    }

    /**
     * Generar código para una declaración de función
     */
    private void generateFunctionDecl(Node nodo, TablaSimbolos tabla) {
        String funcName = nodo.op;
        currentFunction = funcName;
        inMainFunction = false;

        // Resetear offsets para esta función
        stackOffset = 0;
        varOffsets.clear();

        // Prólogo de la función
        code.append("\n").append(funcName).append(":\n");
        code.append("    pushq   %rbp\n");
        code.append("    movq    %rsp, %rbp\n");

        // --- INICIO DEL CAMBIO: Procesar Parámetros ---

        // Los parámetros ahora están accesibles en nodo.left.right
        // (donde nodo.left es el nodo de tipo retorno y su hijo derecho es la lista de
        // params)
        if (nodo.left != null && nodo.left.right != null) {
            Node paramsNode = nodo.left.right;

            String[] argRegisters = { "%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9" };
            int paramIndex = 0;

            // Iterar sobre la lista enlazada de parámetros del CUP
            Node currentParam = paramsNode;
            while (currentParam != null && paramIndex < argRegisters.length) {
                // La estructura exacta depende de tu CUP.
                // Si "params" devuelve una lista enlazada de nodos "params" o "Leaf":

                String paramName = null;

                // Caso 1: Nodo intermedio de lista (tiene hijo izquierdo)
                if (currentParam.left != null) {
                    paramName = currentParam.left.op;
                }
                // Caso 2: Nodo hoja o final de lista (el nombre está en op)
                else if (currentParam.op != null) {
                    paramName = currentParam.op;
                }

                if (paramName != null && !paramName.equals("empty_params")) {
                    // 1. Asignar espacio en el stack
                    stackOffset -= 8;

                    // 2. Guardar en el mapa para que generateNode sepa dónde está
                    varOffsets.put(paramName, stackOffset);

                    // 3. Generar instrucción para mover del registro al stack
                    emit("movq " + argRegisters[paramIndex] + ", " + stackOffset + "(%rbp)");

                    paramIndex++;
                }

                // Avanzar en la lista
                currentParam = currentParam.right;
            }
        }
        // --- FIN DEL CAMBIO ---

        // Generar código del cuerpo de la función
        if (nodo.right != null) {
            generateNode(nodo.right, tabla);
        }

        // Epílogo de la función
        String returnType = nodo.left.op;
        // Si es main, asegurar retorno 0
        if (funcName.equals("main")) {
            emit("movq $0, %rax");
        } else if (!returnType.equals("void")) {
            // Si no es void, asumimos que el código del cuerpo ya puso algo en %rax
            // o generamos un default 0 por seguridad si falta el return
            // emit("movq $0, %rax"); // Opcional
        }

        code.append("    leave\n");
        code.append("    ret\n");
    }

    private String generateNode(Node nodo, TablaSimbolos tabla) {
        if (nodo == null)
            return "";

        switch (nodo.nodeType) {
            case "Declaration":
                // 1. Reservamos espacio para la nueva variable
                stackOffset -= 8;
                varOffsets.put(nodo.left.op, stackOffset);

                // IMPORTANTE: Guardamos este offset en una variable local
                // porque 'stackOffset' puede cambiar al generar el nodo derecho.
                int offsetDestino = stackOffset;

                if (nodo.right != null) {
                    String rval = generateNode(nodo.right, tabla);
                    emit("movq " + rval + ", %rax");
                    // Usamos la variable local offsetDestino, NO stackOffset global
                    emit("movq %rax, " + offsetDestino + "(%rbp)");
                } else {
                    emit("movq $0, %rax");
                    emit("movq %rax, " + offsetDestino + "(%rbp)");
                }
                return offsetDestino + "(%rbp)";

            case "Asignacion":
                // Nota: En asignación generas primero el valor (derecha)
                String rval = generateNode(nodo.right, tabla);

                // Luego buscas dónde guardarlo. Esto está bien porque usas el mapa.
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
                if (nodo.type != null && nodo.type.equals("int"))
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

            case "func_call":
                return generateFunctionCall(nodo, tabla);

            case "return":
                generateReturn(nodo, tabla);
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

    /**
     * Generar código para una llamada a función
     */
    private String generateFunctionCall(Node nodo, TablaSimbolos tabla) {
        String funcName = nodo.op;

        // Recolectar argumentos en orden
        List<String> args = new ArrayList<>();
        if (nodo.left != null) {
            collectArguments(nodo.left, args, tabla);
        }

        // En x86-64, los primeros 6 argumentos van en registros:
        // %rdi, %rsi, %rdx, %rcx, %r8, %r9
        String[] argRegisters = { "%rdi", "%rsi", "%rdx", "%rcx", "%r8", "%r9" };

        // Guardar registros que podrían ser modificados
        // (en una implementación más completa)

        // Cargar argumentos en registros o push al stack
        for (int i = 0; i < args.size(); i++) {
            if (i < 6) {
                emit("movq " + args.get(i) + ", " + argRegisters[i]);
            } else {
                // Más de 6 argumentos: push al stack (orden inverso)
                emit("pushq " + args.get(i));
            }
        }

        // Alinear stack a 16 bytes antes de call (convención x86-64)
        // Para simplificar, asumimos que está alineado
        emit("call " + funcName);

        // Limpiar stack si había más de 6 argumentos
        if (args.size() > 6) {
            int bytesToClean = (args.size() - 6) * 8;
            emit("addq $" + bytesToClean + ", %rsp");
        }

        // El valor de retorno está en %rax
        // Guardarlo en una temporal para usarlo después
        String temp = newTemp();
        stackOffset -= 8;
        varOffsets.put(temp, stackOffset);
        emit("movq %rax, " + stackOffset + "(%rbp)");

        return stackOffset + "(%rbp)";
    }

    /**
     * Recolectar argumentos de una llamada a función en orden
     */
    private void collectArguments(Node nodo, List<String> args, TablaSimbolos tabla) {
        if (nodo == null)
            return;

        if (nodo.nodeType.equals("args")) {
            // Procesar lista de argumentos recursivamente (izquierda primero)
            collectArguments(nodo.left, args, tabla);
            // Luego agregar el argumento derecho
            String argValue = generateNode(nodo.right, tabla);
            args.add(argValue);
        } else {
            // Es un solo argumento
            String argValue = generateNode(nodo, tabla);
            args.add(argValue);
        }
    }

    /**
     * Generar código para un return statement
     */
    private void generateReturn(Node nodo, TablaSimbolos tabla) {
        if (nodo.left != null) {
            // Return con valor
            String returnValue = generateNode(nodo.left, tabla);
            emit("movq " + returnValue + ", %rax");
        }
        // Si es void o return sin valor, %rax ya tiene un valor por defecto

        emit("leave");
        emit("ret");
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

    /**
     * Genera código para una condición lógica completa
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
     */
    private void generateCondicionInvertida(Node nodo, String trueLabel) {
        if (nodo.nodeType.equals("condition")) {
            String leftValue = getValueString(nodo.left);
            String rightValue = getValueString(nodo.right);

            emit("movq " + leftValue + ", %rax");
            emit("cmp " + rightValue + ", %rax");

            // Saltos INVERTIDOS (si condición es TRUE, saltar)
            switch (nodo.op) {
                case ">":
                    emit("jg " + trueLabel);
                    break;
                case "<":
                    emit("jl " + trueLabel);
                    break;
                case ">=":
                    emit("jge " + trueLabel);
                    break;
                case "<=":
                    emit("jle " + trueLabel);
                    break;
                case "==":
                    emit("je " + trueLabel);
                    break;
                case "!=":
                    emit("jne " + trueLabel);
                    break;
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
            case ">":
                emit("jle " + falseLabel);
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
            case "==":
                emit("jne " + falseLabel);
                break;
            case "!=":
                emit("je " + falseLabel);
                break;
        }
    }

    /**
     * Helper para obtener la representación en string de un valor
     */
    private String getValueString(Node node) {
        if (node.nodeType.equals("VarOP")) {
            Integer offset = varOffsets.get(node.op);
            if (offset != null) {
                return offset + "(%rbp)";
            }
            return "$0"; // Fallback
        } else if (node.nodeType.equals("Leaf")) {
            return "$" + node.op;
        } else {
            return "$" + node.op;
        }
    }

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