import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.lexer.Lexer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class InterpreterTest {

    //allows testing standard out
    private static final ByteArrayOutputStream OUT = new ByteArrayOutputStream();
    private static final PrintStream ORIG = System.out;

    @BeforeAll
    public static void setOutput() {

        System.setOut(new PrintStream(OUT));

    }

    @AfterAll
    public static void restoreOutput() {
        System.setOut(ORIG);
    }

    //I included the parser changes for this assignment in the last assignment by sheer coincidence (my ParserTest was failing due to some builtin function calls)
    @Test
    public void printfTest() throws Exception {
        OUT.reset();
        Interpreter interpreter = new Interpreter(new Parser(new Lexer("BEGIN {FS=\",\"}{ printf(\"%.3s %s \", $1, $2)}").Lex()).parse(), "input.txt");
        interpreter.interpretProgram();
        assertEquals("AMe 555-5553 ANT 555-3412 BEC 555-7685 bil 555-1675 BRO 555-0542 caM 555-2912 faB 555-1234 jUl 555-6699 mar 555-6480 Sam 555-3430 jEA 555-2127 ", OUT.toString());
    }

    @Test
    public void printTest() throws Exception {
        OUT.reset();
        Interpreter interpreter = new Interpreter(new Parser(new Lexer("BEGIN {FS=\",\"}{ print($1, $2)}").Lex()).parse(), "input.txt");
        interpreter.interpretProgram();
        String output = OUT.toString().replaceAll("\r", "");

        assertEquals("AMeLIA 555-5553\n" +
                "ANThoNY 555-3412\n" +
                "BECKY 555-7685\n" +
                "bilL 555-1675\n" +
                "BROdeRIck 555-0542\n" +
                "caMILlA 555-2912\n" +
                "faBiUS 555-1234\n" +
                "jUlIE 555-6699\n" +
                "martIn 555-6480\n" +
                "SamUEl 555-3430\n" +
                "jEAN-Paul 555-2127\n", output);
    }

    @Test
    public void getlineTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ getline() }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode getline = (BuiltinFunctionNode) interpreter.functionDefinitions.get("getline");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            assertEquals("1", getline.execute(args));
            assertEquals("ANThoNY,555-3412", interpreter.globalVariableMap.get("$0").getData());
            assertEquals("1", interpreter.globalVariableMap.get("NF").getData());
            assertEquals("1", interpreter.globalVariableMap.get("NR").getData());
            assertEquals("1", interpreter.globalVariableMap.get("FNR").getData());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }

    }

    @Test
    public void nextTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ next() }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode getline = (BuiltinFunctionNode) interpreter.functionDefinitions.get("getline");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            assertEquals("1", getline.execute(args));
            assertEquals("ANThoNY,555-3412", interpreter.globalVariableMap.get("$0").getData());
            assertEquals("1", interpreter.globalVariableMap.get("NF").getData());
            assertEquals("1", interpreter.globalVariableMap.get("NR").getData());
            assertEquals("1", interpreter.globalVariableMap.get("FNR").getData());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }

    }

    @Test
    public void gsubTest() throws Exception {
        OUT.reset();
        Interpreter interpreter = new Interpreter(new Parser(new Lexer("BEGIN {FS=\",\"}{ gsub(\"a\", \"x\"); print; }").Lex()).parse(), "input.txt");
        interpreter.interpretProgram();
        String output = OUT.toString().replaceAll("\r", "");
        assertEquals("AMeLIA,555-5553\n" +
                "ANThoNY,555-3412\n" +
                "BECKY,555-7685\n" +
                "bilL,555-1675\n" +
                "BROdeRIck,555-0542\n" +
                "cxMILlA,555-2912\n" +
                "fxBiUS,555-1234\n" +
                "jUlIE,555-6699\n" +
                "mxrtIn,555-6480\n" +
                "SxmUEl,555-3430\n" +
                "jEAN-Pxul,555-2127\n", output);
    }

    @Test
    public void subTest() throws Exception {
        OUT.reset();
        Interpreter interpreter = new Interpreter(new Parser(new Lexer("BEGIN {FS=\",\"}{ sub(\"[aA]\", \"x\"); print; }").Lex()).parse(), "input.txt");
        interpreter.interpretProgram();
        String output = OUT.toString().replaceAll("\r", "");
        assertEquals("xMeLIA,555-5553\n" +
                "xNThoNY,555-3412\n" +
                "BECKY,555-7685\n" +
                "bilL,555-1675\n" +
                "BROdeRIck,555-0542\n" +
                "cxMILlA,555-2912\n" +
                "fxBiUS,555-1234\n" +
                "jUlIE,555-6699\n" +
                "mxrtIn,555-6480\n" +
                "SxmUEl,555-3430\n" +
                "jExN-Paul,555-2127\n", output);
    }

    @Test
    public void matchTest() throws Exception {
        OUT.reset();
        Interpreter interpreter = new Interpreter(new Parser(new Lexer("BEGIN {FS=\",\"}{ match($0, \"[0-9]+\"); print RSTART RLENGTH;  }").Lex()).parse(), "input.txt");
        interpreter.interpretProgram();
        String output = OUT.toString().replaceAll("\r", "");
        assertEquals("83\n93\n73\n63\n113\n93\n83\n73\n83\n83\n113\n", output);

    }

    @Test
    public void indexTest() throws Exception {
        OUT.reset();
        Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ print index($0, \",55\") }").Lex()).parse(), "input.txt");
        interpreter.interpretProgram();
        String output = OUT.toString().replaceAll("\r", "");
        assertEquals("6\n7\n5\n4\n9\n7\n6\n5\n6\n6\n9\n", output);
    }

    @Test
    public void lengthTest() throws Exception {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("BEGIN {FS=\",\"}{ print length($1) }").Lex()).parse(), "input.txt");

    }

    @Test
    public void splitTest() throws Exception {
        OUT.reset();
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ split($0, array, \",\"); for (i in array) { printf \"%s \", array[i] } print \"\"; }").Lex()).parse(), "input.txt");
            interpreter.interpretProgram();
            String output = OUT.toString().replaceAll("\r", "");
            assertEquals("AMeLIA 555-5553 \n" +
                    "ANThoNY 555-3412 \n" +
                    "BECKY 555-7685 \n" +
                    "bilL 555-1675 \n" +
                    "BROdeRIck 555-0542 \n" +
                    "caMILlA 555-2912 \n" +
                    "faBiUS 555-1234 \n" +
                    "jUlIE 555-6699 \n" +
                    "martIn 555-6480 \n" +
                    "SamUEl 555-3430 \n" +
                    "jEAN-Paul 555-2127 \n", output);

    }

    @Test
    public void substrTest() throws Exception {
        OUT.reset();

        Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ print substr($0, 3, 7) }").Lex()).parse(), "input.txt");
        interpreter.interpretProgram();
        String output = OUT.toString().replaceAll("\r", "");
        assertEquals("eLIA,55\n" +
                "ThoNY,5\n" +
                "CKY,555\n" +
                "lL,555-\n" +
                "OdeRIck\n" +
                "MILlA,5\n" +
                "BiUS,55\n" +
                "lIE,555\n" +
                "rtIn,55\n" +
                "mUEl,55\n" +
                "AN-Paul\n", output);
    }

    @Test
    public void tolowerTest() throws Exception {
        Interpreter interpreter = new Interpreter(new Parser(new Lexer("BEGIN {FS=\",\"}{ print tolower($1) }").Lex()).parse(), "input.txt");

    }

    @Test
    public void toupperTest() throws Exception {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("BEGIN {FS=\",\"}{ print toupper($1) }").Lex()).parse(), "input.txt");

    }

    @Test
    public void interpreterTest() throws Exception {
        OUT.reset();
        String program = Files.readString(Path.of("test.awk"));
        Interpreter interpreter = new Interpreter(new Parser(new Lexer(program).Lex()).parse(), "input.txt");
        interpreter.interpretProgram();
        String output = OUT.toString().replaceAll("\r", "");
        assertEquals("xxx: \n Amelia,555-5553\nxxx: \n Anthony,555-3412\nxxx: \n Becky,555-7685\nxxx: \n Bill,555-1675\nxxx: \n Broderick,555-0542\nxxx: \n Camilla,555-2912\nxxx: \n Fabius,555-1234\nxxx: \n Julie,555-6699\nxxx: \n Martin,555-6480\nxxx: \n Samuel,555-3430\nxxx: \n Jean-paul,555-2127\n", output);


    }

    @Test
    public void sumTest() throws Exception {
        OUT.reset();
        String program = Files.readString(Path.of("sum.awk"));
        Interpreter interpreter = new Interpreter(new Parser(new Lexer(program).Lex()).parse(), "sum.txt");
        interpreter.interpretProgram();
        String output = OUT.toString().replaceAll("\r", "");
        assertEquals("Line 1: 15.0\\nLine 2: 100.0\\nGrand Total: 115.0", output);

    }

    @Test
    public void wordsTest() throws Exception {
        OUT.reset();
        String program = Files.readString(Path.of("words.awk"));
        Interpreter interpreter = new Interpreter(new Parser(new Lexer(program).Lex()).parse(), "words.txt");
        interpreter.interpretProgram();
        String output = OUT.toString().replaceAll("\r", "");
        assertEquals("4\n0\n2\n", output);

    }

}
