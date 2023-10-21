import org.junit.jupiter.api.Test;
import parser.Parser;
import parser.lexer.Lexer;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class InterpreterTest {

    //I included the parser changes for this assignment in the last assignment by sheer coincidence (my ParserTest was failing due to some builtin function calls)
    @Test
    public void printfTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ printf(\"%-10s %s\", $1, $2)}").Lex()).parse(), "input.txt");
            BuiltinFunctionNode printf = (BuiltinFunctionNode) interpreter.functionDefinitions.get("printf");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            InterpreterArrayDataType argData = new InterpreterArrayDataType();
            argData.put("0", new InterpreterDataType("$1"));
            argData.put("1", new InterpreterDataType("$2"));
            args.put("fmt", new InterpreterDataType("%-10s %s\n"));
            args.put("args", argData);
            printf.execute(args);
            args.clear();
            argData.clear();
            argData.put("0", new InterpreterDataType("test"));
            args.put("fmt", new InterpreterDataType("Hello World!\n"));
            printf.execute(args);
            args.put("fmt", new InterpreterDataType("%s hehe\n"));
            args.put("args", argData);
            printf.execute(args);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }
    }

    @Test
    public void printTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ print($1, $2)}").Lex()).parse(), "input.txt");
            BuiltinFunctionNode print = (BuiltinFunctionNode) interpreter.functionDefinitions.get("print");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            InterpreterArrayDataType argData = new InterpreterArrayDataType();
            argData.put("0", new InterpreterDataType("$1"));
            argData.put("1", new InterpreterDataType("$2"));
            args.put("args", argData);
            print.execute(args);
            argData.put("2", argData.get("1"));
            argData.put("1", new InterpreterDataType(" - spacer - "));
            args.put("args", argData);
            print.execute(args);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }
    }

    @Test
    public void getlineTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ getline() }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode getline = (BuiltinFunctionNode) interpreter.functionDefinitions.get("getline");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            assertEquals("1", getline.execute(args));
            assertEquals("Anthony 555-3412", interpreter.globalVariableMap.get("$0").getData());
            assertEquals("2", interpreter.globalVariableMap.get("NF").getData());
            assertEquals("2", interpreter.globalVariableMap.get("NR").getData());
            assertEquals("2", interpreter.globalVariableMap.get("FNR").getData());
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
            assertEquals("Anthony 555-3412", interpreter.globalVariableMap.get("$0").getData());
            assertEquals("2", interpreter.globalVariableMap.get("NF").getData());
            assertEquals("2", interpreter.globalVariableMap.get("NR").getData());
            assertEquals("2", interpreter.globalVariableMap.get("FNR").getData());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }

    }

    @Test
    public void gsubTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ gsub(`a`, \"x\", $1) }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode gsub = (BuiltinFunctionNode) interpreter.functionDefinitions.get("gsub");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            args.put("regexp", new InterpreterDataType("[aA]"));
            args.put("replacement", new InterpreterDataType("x"));
            args.put("target", new InterpreterDataType("$1"));
            gsub.execute(args);
            assertEquals("xmelix", interpreter.globalVariableMap.get("$1").getData());
            args.remove("target");
            gsub.execute(args);

            assertEquals("xmelix 555-5553", interpreter.globalVariableMap.get("$0").getData());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }


    }

    @Test
    public void subTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ sub(`a`, \"x\", $1) }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode sub = (BuiltinFunctionNode) interpreter.functionDefinitions.get("sub");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            args.put("regexp", new InterpreterDataType("[aA]"));
            args.put("replacement", new InterpreterDataType("x"));
            args.put("target", new InterpreterDataType("$1"));
            sub.execute(args);
            assertEquals("xmelia", interpreter.globalVariableMap.get("$1").getData());
            args.remove("target");
            sub.execute(args);

            assertEquals("xmelia 555-5553", interpreter.globalVariableMap.get("$0").getData());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }


    }

    @Test
    public void matchTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ match() }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode match = (BuiltinFunctionNode) interpreter.functionDefinitions.get("match");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            args.put("regexp", new InterpreterDataType("e.*\\s\\d+"));
            args.put("string", new InterpreterDataType("$0"));
            assertEquals("2", match.execute(args));
            assertEquals("3", interpreter.globalVariableMap.get("RSTART").getData());
            assertEquals("8", interpreter.globalVariableMap.get("RLENGTH").getData());

            args.put("regexp", new InterpreterDataType("x"));
            assertEquals("0", match.execute(args));
            assertEquals("0", interpreter.globalVariableMap.get("RSTART").getData());
            assertEquals("-1", interpreter.globalVariableMap.get("RLENGTH").getData());
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }
    }

    @Test
    public void indexTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ index() }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode index = (BuiltinFunctionNode) interpreter.functionDefinitions.get("index");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            args.put("in", new InterpreterDataType("$0"));
            args.put("find", new InterpreterDataType(" 55"));
            assertEquals("6", index.execute(args));

        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }
    }

    @Test
    public void lengthTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ length() }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode length = (BuiltinFunctionNode) interpreter.functionDefinitions.get("length");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            args.put("string", new InterpreterDataType("$1"));
            assertEquals("6", length.execute(args));
            args.put("string", new InterpreterDataType("test"));
            assertEquals("4", length.execute(args));
            args.put("string", null);
            assertEquals("15", length.execute(args));
            args.put("string", new InterpreterDataType(""));
            assertEquals("0", length.execute(args));


        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }
    }

    @Test
    public void splitTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ split() }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode split = (BuiltinFunctionNode) interpreter.functionDefinitions.get("split");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            args.put("string", new InterpreterDataType("$0"));
            args.put("array", new InterpreterArrayDataType());
            assertEquals("2", split.execute(args));
            assertEquals("Amelia", ((InterpreterArrayDataType) args.get("array")).get("0").getData());

            assertEquals("555-5553", ((InterpreterArrayDataType) args.get("array")).get("1").getData());

        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }

    }

    @Test
    public void substrTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ substr() }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode substr = (BuiltinFunctionNode) interpreter.functionDefinitions.get("substr");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            args.put("string", new InterpreterDataType("$0"));
            args.put("start", new InterpreterDataType("3"));
            args.put("length", new InterpreterDataType("7"));
            assertEquals("lia 555", substr.execute(args));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }
    }

    @Test
    public void tolowerTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ length() }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode tolower = (BuiltinFunctionNode) interpreter.functionDefinitions.get("tolower");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            args.put("string", new InterpreterDataType("$1"));
            assertEquals("amelia", tolower.execute(args));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }

    }

    @Test
    public void toupperTest() {
        try {
            Interpreter interpreter = new Interpreter(new Parser(new Lexer("{ toupper() }").Lex()).parse(), "input.txt");
            BuiltinFunctionNode toupper = (BuiltinFunctionNode) interpreter.functionDefinitions.get("toupper");
            HashMap<String, InterpreterDataType> args = new HashMap<>();
            args.put("string", new InterpreterDataType("$1"));
            assertEquals("AMELIA", toupper.execute(args));
        } catch (Exception e) {
            e.printStackTrace(System.err);
            fail();
        }

    }

}
