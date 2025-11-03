/*
  File Name: lcalc.flex
  To Create: > jflex lcalc.flex

  and then after the parser is created
  > javac Lexer.java
*/
   
/* --------------------------Usercode Section------------------------ */
   
import java_cup.runtime.*;

      
%%
   
/* -----------------Options and Declarations Section----------------- */
   
/* 
   The name of the class JFlex will create will be Lexer.
   Will write the code to the file Lexer.java. 
*/
%class Lexer

/*
  The current line number can be accessed with the variable yyline
  and the current column number with the variable yycolumn.
*/
%line
%column
    
/* 
   Will switch to a CUP compatibility mode to interface with a CUP
   generated parser.
*/
%cup
   
/*
  Declarations
   
  Code between %{ and %}, both of which must be at the beginning of a
  line, will be copied letter to letter into the lexer class source.
  Here you declare member variables and functions that are used inside
  scanner actions.  
*/
%{   
    /* To create a new java_cup.runtime.Symbol with information about
       the current token, the token will have no value in this
       case. */
    private Symbol symbol(int type) {
        return new Symbol(type, yyline, yycolumn);
    }
    
    /* Also creates a new java_cup.runtime.Symbol with information
       about the current token, but this object has a value. */
    private Symbol symbol(int type, Object value) {
        return new Symbol(type, yyline, yycolumn, value);
    }
%}
   

/*
  Macro Declarations
  
  These declarations are regular expressions that will be used latter
  in the Lexical Rules Section.  
*/
   
/* A line terminator is a \r (carriage return), \n (line feed), or
   \r\n. */
LineTerminator = \r|\n|\r\n
   
/* White space is a line terminator, space, tab, or line feed. */
WhiteSpace     = {LineTerminator} | [ \t\f]
   
/* A literal integer is is a number beginning with a number between
   one and nine followed by zero or more numbers between zero and nine
   or just a zero.  */
dec_int_lit = 0 | [1-9][0-9]*
dec_float_lit = [0-9]+\.[0-9]+
boolType = "true" | "false"
   
/* A identifier integer is a word beginning a letter between A and
   Z, a and z, or an underscore followed by zero or more letters
   between A and Z, a and z, zero and nine, or an underscore. */
dec_int_id = [A-Za-z_][A-Za-z_0-9]*
   
%%
/* ------------------------Lexical Rules Section---------------------- */
   
<YYINITIAL> {
   
    /* Return the token SEMI declared in the class sym that was found. */
    ";"                { System.out.println(" ; "); return symbol(sym.SEMI); }
   
    /* Print the token found that was declared in the class sym and then
       return it. */
    "+"                { System.out.print(" + "); return symbol(sym.PLUS); }
    "-"                { System.out.print(" - "); return symbol(sym.MINUS); }
    "*"                { System.out.print(" * "); return symbol(sym.TIMES); }
    "/"                { System.out.print(" / "); return symbol(sym.DIVIDE); }
    "("                { System.out.print(" ( "); return symbol(sym.LPAREN); }
    ")"                { System.out.print(" ) "); return symbol(sym.RPAREN); }
    "void"             { System.out.print(" void "); return symbol(sym.VOID); }
    "main"             { System.out.print(" main "); return symbol(sym.MAIN); }
/*  "function"         { System.out.print(" function "); return symbol(sym.FUNCTION); }*/
    "{"                { System.out.print(" { "); return symbol(sym.LKEY); }
    "}"                { System.out.print(" } "); return symbol(sym.RKEY); }
    "int"              { System.out.print(" int "); return symbol(sym.INT); }
    ","                { System.out.print(" , "); return symbol(sym.COMA); }
/*  "return"           { System.out.print(" return "); return symbol(sym.RETURN); } */
    "="                { System.out.print(" = "); return symbol(sym.EQUAL); }
    "!="                { System.out.print(" != "); return symbol(sym.NOTEQUAL); }
    "=="                { System.out.print(" == "); return symbol(sym.DOUBLEEQUAL); }
    "float"            { System.out.print(" float "); return symbol(sym.FLOAT); }
    "String"           { System.out.print(" String "); return symbol(sym.STRING); }
    "boolean"          { System.out.print(" boolean "); return symbol(sym.BOOLEAN); }
    "if"               { System.out.print(" if "); return symbol(sym.IF); }
    "else"             { System.out.print(" else "); return symbol(sym.ELSE); }  
    "while"            { System.out.print(" while "); return symbol(sym.WHILE); }
    "&&"                { System.out.print(" && "); return symbol(sym.AND); }
    "||"                { System.out.print(" || "); return symbol(sym.OR); }
/*  "for"              { System.out.print(" for "); return symbol(sym.FOR); } */
    ">"                { System.out.print(" > "); return symbol(sym.GREATER); }
    "<"                { System.out.print(" < "); return symbol(sym.LESS); }
    "<="               { System.out.print(" <= "); return symbol(sym.LESSEQUAL); }
    ">="               { System.out.print(" >= "); return symbol(sym.GREATEREQUAL); }
    {boolType}           { System.out.print(yytext()); return symbol(sym.BOOLEAN_LIT, Boolean.valueOf(yytext())); }
    {dec_int_lit}      { System.out.print(yytext());
                         return symbol(sym.NUMBER, Integer.valueOf(yytext())); }
    {dec_int_id}       { System.out.print(yytext());
                         return symbol(sym.ID, yytext()); }
    {dec_float_lit}    { System.out.print(yytext());
                         return symbol(sym.FLOATNUM, Float.valueOf(yytext())); }
    {WhiteSpace}       { /* just skip what was found, do nothing */ }
}


/* No token was found for the input so through an error.  Print out an
   Illegal character message with the illegal character that was found. */
[^]                    { throw new Error("Illegal character <"+yytext()+">"); }
