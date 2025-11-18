# Informe materia Compiladores
# Alumnos : Gimenez Thomas Valentin y Gimenez Matias Nicolas

## Descripción
Este proyecto implementa un compilador para un subconjunto del lenguaje C. Utiliza **JFlex** para el análisis léxico y **Java CUP** para el análisis sintáctico (parsing). El compilador valida la sintaxis, realiza chequeos semánticos de tipos y genera código Assembler.

## 🛠️ Requisitos Previos

* **Java JDK** instalado y configurado en el PATH.
* Librerías incluidas en el directorio del proyecto:
    * `jflex-full-1.9.1.jar`
    * `java-cup-11b.jar`
    * `java-cup-11b-runtime.jar`

## 🚀 Instrucciones de Compilación y Ejecución

Para construir y ejecutar el compilador desde cero, sigue estos pasos en tu terminal:

### 1. Generar Analizadores
Genera el código fuente Java a partir de las especificaciones `.flex` y `.cup`.

```bash
# Generar el Lexer (Scanner)
java -jar jflex-full-1.9.1.jar lcalc.flex

# Generar el Parser
java -jar java-cup-11b.jar -parser parser -symbols sym ycalc.cup
```

### 2. Compilar el Código Java
Compila los archivos generados junto con el runtime de CUP.

```bash
javac -cp java-cup-11b-runtime.jar *.java
```

### 3. Ejecutar el Compilador
Ejecuta la clase principal pasando el archivo de entrada (ej. `test.txt`).

> **Nota:** En Windows se usa punto y coma (`;`) como separador en el classpath. En Linux/Mac usa dos puntos (`:`).

```bash
java -cp ".;java-cup-11b.jar" Main test.txt
```

---

## 💻 Ejemplos de Sintaxis Soportada

A continuación, se detallan ejemplos de programas funcionales para este compilador.

### 1. Declaraciones y Operaciones Básicas
Soporte para variables enteras y operaciones aritméticas simples.

```c
void main () {
    int x;
    int y;
    x = 5;
    y = 10;

    int z;
    z = x + y;
}
```

### 2. Control de Flujo (If / Else)
Estructuras condicionales y bloques de código.

```c
void main () {
    int a;
    a = 7;

    if (a > 5) {
        a = a + 1;
    } else {
        a = a - 1;
    }
}
```

### 3. Bucle While
Iteraciones condicionales.

```c
void main () {
    int i;
    i = 0;

    while (i < 5) {
        i = i + 1;
    }
}
```

### 4. Funciones y Retorno
Definición de funciones con parámetros y retorno de valores.

```c
int suma(int x, int y) {
    int z = x + y;
    return z;
}

void main() {
    int resultado;
    resultado = suma(3, 4);
}
```

### 5. Validación Semántica (Manejo de Errores)
El compilador verifica la compatibilidad de tipos. El siguiente código **lanzará una excepción** intencional:

```c
void main() {
    int n;
    float f;

    n = 10;
    f = 3.14;

    // Error semántico: Asignación incompatible (float -> int)
    n = f;
}
```

## 📝 Salida (Assembler)
Podes revisar la salida en la consola o en el archivo salida.txt generado en el directorio de ejecución.
