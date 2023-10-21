import parser.FunctionNode;
import parser.ProgramNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Interpreter builds a ProgramNode and runs it on a supplied file (if present)
 */
public class Interpreter {
    //TODO local variables?

    /**
     * the awk program AST
     */
    private final ProgramNode programNode;

    /**
     * a list of all global variables used by the program
     */
    protected HashMap<String, InterpreterDataType> globalVariableMap;

    /**
     * a list of function definitions in the program
     */
    protected HashMap<String, FunctionNode> functionDefinitions;

    /**
     * a handler for the input file
     */
    private LineManager lineManager;

    public Interpreter(ProgramNode programNode) {

        this.globalVariableMap = new HashMap<>();
        this.functionDefinitions = new HashMap<>();
        this.programNode = programNode;
        this.lineManager = new LineManager(List.of());
        initGlobalVariables();
        initFunctionDefinitions();
    }

    public Interpreter(ProgramNode programNode, String filePath) throws IOException {
        this(programNode);
        this.lineManager = new LineManager(Files.readAllLines(Path.of(filePath)));
    }

    /**
     * initializes the global variables used by the program
     */
    private void initGlobalVariables() {
        globalVariableMap.put("FS", new InterpreterDataType(" "));
        globalVariableMap.put("OFMT", new InterpreterDataType("%.6g"));
        globalVariableMap.put("OFS", new InterpreterDataType(" "));
        globalVariableMap.put("ORS", new InterpreterDataType("\n"));
    }

    /**
     * initializes the builtin function definitions
     */
    private void initFunctionDefinitions() {
        functionDefinitions.put("printf", printfDefinition());
        functionDefinitions.put("print", printDefinition());
        functionDefinitions.put("getline", getlineDefinition());
        functionDefinitions.put("next", nextDefinition());
        functionDefinitions.put("gsub", gsubDefinition());
        functionDefinitions.put("sub", subDefinition());
        functionDefinitions.put("match", matchDefinition());
        functionDefinitions.put("index", indexDefinition());
        functionDefinitions.put("length", lengthDefinition());
        functionDefinitions.put("split", splitDefinition());
        functionDefinitions.put("substr", substrDefinition());
        functionDefinitions.put("tolower", tolowerDefinition());
        functionDefinitions.put("toupper", toupperDefinition());
    }

    /**
     * the builtin printf definition
     * @return a BuiltinFunctionNode representing the printf function
     */
    private BuiltinFunctionNode printfDefinition() {
        return new BuiltinFunctionNode("printf", List.of("fmt", "args"), true,
                (parameters) -> {
                    String format = parameters.get("fmt").getData();
                    if (!parameters.containsKey("args")) {
                        System.out.printf(format);
                        return "";
                    }
                    InterpreterArrayDataType argData = (InterpreterArrayDataType) parameters.get("args");
                    System.out.printf(format, argData.values().stream().map(value -> globalVariableMap.getOrDefault(value.getData(), value).getData()).toList().toArray());
                    return "";
                });
    }

    /**
     * the builtin print definition
     * @return a BuiltinFunctionNode representing the print function
     */
    private BuiltinFunctionNode printDefinition() {
        return new BuiltinFunctionNode("print", List.of("args"), true,
                (parameters) -> {
                    InterpreterArrayDataType argData = (InterpreterArrayDataType) parameters.get("args");
                    System.out.println(argData.values().stream().map(value -> globalVariableMap.getOrDefault(value.getData(), value).getData()).collect(Collectors.joining(" ")));
                    return "";
                });
    }

    /**
     * the builtin getline definition
     * @return a BuiltinFunctionNode representing the getline function
     */
    private BuiltinFunctionNode getlineDefinition() {
        return new BuiltinFunctionNode("getline", List.of(), false,
                (parameters) -> lineManager.splitAndAssign() ? "1" : "0");
    }

    /**
     * the builtin next definition
     * @return a BuiltinFunctionNode representing the next function
     */
    private BuiltinFunctionNode nextDefinition() {
        return new BuiltinFunctionNode("next", List.of(), false,
                (parameters) -> {
                    lineManager.splitAndAssign();
                    return "";
                });
    }

    /**
     * the builtin gsub definition
     * @return a BuiltinFunctionNode representing the gsub function
     */
    private BuiltinFunctionNode gsubDefinition() {
        return new BuiltinFunctionNode("gsub", List.of("regexp", "replacement", "target"), false,
                (parameters) -> {
                    String regularExpression = parameters.get("regexp").getData();
                    String replacement = parameters.get("replacement").getData();
                    AtomicBoolean shouldContinue = new AtomicBoolean(true);
                    //gsub has optional target string
                    parameters.computeIfPresent("target", (name, data) -> {

                        String target = globalVariableMap.get(data.getData()).getData();
                        String newData = target.replaceAll(regularExpression, replacement);
                        globalVariableMap.put(data.getData(), new InterpreterDataType(newData));

                        shouldContinue.set(false);
                        return data;
                    });

                    if (!shouldContinue.get()) return "";
                    String newData = globalVariableMap.get("$0").getData().replaceAll(regularExpression, replacement);

                    globalVariableMap.put("$0", new InterpreterDataType(newData));

                    return "";
                });
    }

    /**
     * the builtin sub definition
     * @return a BuiltinFunctionNode representing the sub function
     */
    private BuiltinFunctionNode subDefinition() {
        return new BuiltinFunctionNode("sub", List.of("regexp", "replacement", "target"), false,
                (parameters) -> {
                    String regularExpression = parameters.get("regexp").getData();
                    String replacement = parameters.get("replacement").getData();
                    AtomicBoolean shouldContinue = new AtomicBoolean(true);
                    //gsub has optional target string
                    parameters.computeIfPresent("target", (name, data) -> {

                        String target = globalVariableMap.get(data.getData()).getData();
                        String newData = target.replaceFirst(regularExpression, replacement);
                        globalVariableMap.put(data.getData(), new InterpreterDataType(newData));

                        shouldContinue.set(false);
                        return data;
                    });

                    if (!shouldContinue.get()) return "";
                    String newData = globalVariableMap.get("$0").getData().replaceFirst(regularExpression, replacement);

                    globalVariableMap.put("$0", new InterpreterDataType(newData));

                    return "";
                });
    }

    /**
     * the builtin match definition
     * @return a BuiltinFunctionNode representing the match function
     */
    private BuiltinFunctionNode matchDefinition() {
        return new BuiltinFunctionNode("match", List.of("string", "regexp"), false,
                (parameters) -> {
                    String regularExpression = parameters.get("regexp").getData();
                    String match = globalVariableMap.getOrDefault(parameters.get("string").getData(), parameters.get("string")).getData();
                    Pattern pattern = Pattern.compile(regularExpression);
                    Matcher matches = pattern.matcher(match);
                    if (!matches.find()) {
                        globalVariableMap.put("RSTART", new InterpreterDataType("0"));
                        globalVariableMap.put("RLENGTH", new InterpreterDataType("-1"));
                        return "0";
                    }
                    globalVariableMap.put("RSTART", new InterpreterDataType(String.valueOf(matches.start() + 1)));
                    globalVariableMap.put("RLENGTH", new InterpreterDataType(String.valueOf(matches.end() - matches.start())));
                    return String.valueOf(matches.start());
                });
    }

    /**
     * the builtin index definition
     * @return a BuiltinFunctionNode representing the index function
     */
    private BuiltinFunctionNode indexDefinition() {
        return new BuiltinFunctionNode("index", List.of("in", "find"), false,
                (parameters) -> {
                    String searchString = globalVariableMap.getOrDefault(parameters.get("in").getData(), parameters.get("in")).getData();
                    String findString = parameters.get("find").getData();
                    return String.valueOf(searchString.indexOf(findString));
                });
    }

    /**
     * the builtin length definition
     * @return a BuiltinFunctionNode representing the length function
     */
    private BuiltinFunctionNode lengthDefinition() {
        return new BuiltinFunctionNode("length", List.of("string"), false,
                (parameters) -> {
                    String lengthString = parameters.get("string") != null ? globalVariableMap.getOrDefault(parameters.get("string").getData(), parameters.get("string")).getData() : globalVariableMap.get("$0").getData();

                    return String.valueOf(lengthString.length());
                });
    }

    /**
     * the builtin split definition
     * @return a BuiltinFunctionNode representing the split function
     */
    private BuiltinFunctionNode splitDefinition() {
        return new BuiltinFunctionNode("split", List.of("string", "array", "separator"), false,
                (parameters) -> {
                    String string = globalVariableMap.getOrDefault(parameters.get("string").getData(), parameters.get("string")).getData();
                    InterpreterArrayDataType splitArray = (InterpreterArrayDataType) globalVariableMap.getOrDefault(parameters.get("array").getData(), parameters.get("array"));
                    String separator = parameters.containsKey("separator") ? parameters.get("separator").getData() : globalVariableMap.get("FS").getData();

                    String[] splits = string.split(separator);
                    splitArray.clear();
                    for (int i = 0; i < splits.length; i++) {
                        splitArray.put(String.valueOf(i), new InterpreterDataType(splits[i]));
                    }
                    parameters.put("array", splitArray);
                    return String.valueOf(splits.length);
                });
    }

    /**
     * the builtin substr definition
     * @return a BuiltinFunctionNode representing the substr function
     */
    private BuiltinFunctionNode substrDefinition() {
        return new BuiltinFunctionNode("substr", List.of("string", "start", "length"), false,
                (parameters) -> {
                    String string = globalVariableMap.getOrDefault(parameters.get("string").getData(), parameters.get("string")).getData();
                    int startIndex = Integer.parseInt(parameters.get("start").getData());
                    int length = Integer.parseInt(parameters.getOrDefault("length", new InterpreterDataType("0")).getData());
                    return length > 0 ? string.substring(startIndex, startIndex + length) : string.substring(startIndex);
                });
    }

    /**
     * the builtin tolower definition
     * @return a BuiltinFunctionNode representing the tolower function
     */
    private BuiltinFunctionNode tolowerDefinition() {
        return new BuiltinFunctionNode("tolower", List.of("string"), false,
                (parameters) -> {
                    String string = globalVariableMap.getOrDefault(parameters.get("string").getData(), parameters.get("string")).getData();
                    return string.toLowerCase();
                });
    }

    /**
     * the builtin toupper definition
     * @return a BuiltinFunctionNode representing the toupper function
     */
    private BuiltinFunctionNode toupperDefinition() {
        return new BuiltinFunctionNode("toupper", List.of("string"), false,
                (parameters) -> {
                    String string = globalVariableMap.getOrDefault(parameters.get("string").getData(), parameters.get("string")).getData();
                    return string.toUpperCase();
                });
    }

    private class LineManager {
        private final List<String> lines;
        private int lineRecord;

        public LineManager(List<String> lines) {
            this.lines = lines;
            this.lineRecord = 0;
            splitAndAssign();
        }

        public boolean splitAndAssign() {
            if (lineRecord >= lines.size()) return false;
            String line = lines.get(lineRecord++);
            if (line.isEmpty()) return false;
            String[] splitLine = line.split(globalVariableMap.get("FS").getData());
            globalVariableMap.put("$0", new InterpreterDataType(line));
            int i = 0;
            for (; i < splitLine.length; i++) {
                globalVariableMap.put("$" + (i + 1), new InterpreterDataType(splitLine[i]));
            }
            globalVariableMap.put("NF", new InterpreterDataType(Integer.toString(i)));
            //NR can go past lineRecord
            InterpreterDataType oldNR = globalVariableMap.getOrDefault("NR", new InterpreterDataType(String.valueOf(lineRecord - 1)));
            oldNR.setData(String.valueOf(Integer.parseInt(oldNR.getData()) + 1));
            globalVariableMap.put("NR", oldNR);
            globalVariableMap.put("FNR", new InterpreterDataType(String.valueOf(lineRecord)));
            return true;
        }
    }

}
