import parser.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
    protected final ProgramNode programNode;

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

    public ReturnType processStatement(HashMap<String, InterpreterDataType> localVariables, Node statement) throws Exception {

        if (statement instanceof BreakNode) {
            return new ReturnType(ReturnType.Types.Break);
        }
        if (statement instanceof ContinueNode) {
            return new ReturnType(ReturnType.Types.Continue);
        }
        if (statement instanceof DeleteNode node) {
            processDeleteStatement(node, localVariables);
            return new ReturnType(ReturnType.Types.None);
        }
        if (statement instanceof DoWhileNode node) {
            if (node.getWhileStatements().isEmpty()) return new ReturnType(ReturnType.Types.None);
            do {
                ReturnType returnType = interpretStatementList(node.getWhileStatements().get().getNodes(), localVariables);
                if (returnType.getReturnType() == ReturnType.Types.Break) {
                    break;
                } else if (returnType.getReturnType() == ReturnType.Types.Return) {
                    return returnType;
                }
            } while (getIDT(node.getConditionNode(), localVariables).toBool());
        }
        if (statement instanceof ForNode node) {
            return processForStatement(node, localVariables);
        }
        if (statement instanceof IfNode node) {
            IfNode next = node;
            while (next.getCondition().isPresent() && !getIDT(next.getCondition().get(), localVariables).toBool()) {
                if (next.getNextNode().isEmpty()) {
                    break;
                }
                next = next.getNextNode().get();
            }

            if (next.getStatementNodes().isEmpty()) return new ReturnType(ReturnType.Types.None);
            ReturnType returnType = interpretStatementList(next.getStatementNodes().get().getNodes(), localVariables);
            if (returnType.getReturnType() != ReturnType.Types.None) {
                return returnType;
            }
        }
        if (statement instanceof ReturnNode node) {
            String value = "";
            if (node.getReturnValue().isPresent()) {
                value = getIDT(node.getReturnValue().get(), localVariables).getData();
            }
            return new ReturnType(ReturnType.Types.Return, value);
        }
        if (statement instanceof WhileNode node) {
            if (node.getWhileStatements().isEmpty()) return new ReturnType(ReturnType.Types.None);
            while (getIDT(node.getConditionNode(), localVariables).toBool()) {
                ReturnType returnType = interpretStatementList(node.getWhileStatements().get().getNodes(), localVariables);
                if (returnType.getReturnType() == ReturnType.Types.Break) {
                    break;
                } else if (returnType.getReturnType() == ReturnType.Types.Return) {
                    return returnType;
                }
            }
        }
        InterpreterDataType value = getIDT(statement, localVariables);
        return new ReturnType(ReturnType.Types.None, value.getData());
    }

    private ReturnType processForStatement(ForNode node, HashMap<String, InterpreterDataType> localVariables) throws Exception {
        if (node.getCodeBlock().isEmpty()) return new ReturnType(ReturnType.Types.None);
        Optional<Node> expression;
        if ((expression = node.getInitializationExpression()).isPresent() && expression.get() instanceof StatementNode) {
            processStatement(localVariables, expression.get());
        }
        boolean condition = node.getConditionExpression().isEmpty() || getIDT(node.getConditionExpression().get(), localVariables).toBool();

        while (condition) {
            ReturnType returnType = interpretStatementList(node.getCodeBlock().get().getNodes(), localVariables);
            if (returnType.getReturnType() == ReturnType.Types.Break) {
                break;
            } else if (returnType.getReturnType() == ReturnType.Types.Return) {
                return returnType;
            }
            if (node.getModificationExpression().isPresent()) {
                processStatement(localVariables, node.getModificationExpression().get());
            }
            condition = node.getConditionExpression().isEmpty() || getIDT(node.getConditionExpression().get(), localVariables).toBool();
        }
        return new ReturnType(ReturnType.Types.None);
    }

    private ReturnType interpretStatementList(List<StatementNode> statements, HashMap<String, InterpreterDataType> localVariables) throws Exception {
        for (StatementNode statement : statements) {
            ReturnType returnType = processStatement(localVariables, statement);
            if (returnType.getReturnType() != ReturnType.Types.None) {
                return returnType;
            }
        }
        return new ReturnType(ReturnType.Types.None);
    }

    private void processDeleteStatement(DeleteNode node, HashMap<String, InterpreterDataType> localVariables) throws Exception {
        {
            if (!(node.getDeleteParameter() instanceof VariableReferenceNode array)) throw new IllegalArgumentException("Tried deleting element from " + node.getDeleteParameter().getClass().getName());

            InterpreterDataType value = localVariables.getOrDefault(array.getName(), globalVariableMap.get(array.getName()));

            if (value == null) throw new Exception("Variable " + array.getName() + " does not exist");

            if (array.getValue().isEmpty()) {
                InterpreterDataType removedValue = localVariables.remove(array.getName());
                if (removedValue == null) {
                    globalVariableMap.remove(array.getName());
                }
            } else {
                if (!(value instanceof InterpreterArrayDataType arrayData)) throw new IllegalArgumentException("Tried deleting element from non-array variable");
                Node index = array.getValue().get();
                InterpreterDataType indexData = getIDT(index, localVariables);
                arrayData.remove(indexData.getData());
            }
        }
    }


    /**
     * Takes a Node and returns an InterpreterDataType
     * @param baseNode the node to process
     * @param localVariables a list of local variables for reading and writing to
     * @return an InterpreterDataType
     * @throws Exception if baseNode is a patternNode
     */
    public InterpreterDataType getIDT(Node baseNode, HashMap<String, InterpreterDataType> localVariables) throws Exception {
        if (baseNode instanceof AssignmentNode node) return getAssignmentIDT(node, localVariables);
        if (baseNode instanceof ConstantNode node) return new InterpreterDataType(node.getValue());
        if (baseNode instanceof FunctionCallNode node) return new InterpreterDataType(callFunction(node, localVariables));
        if (baseNode instanceof PatternNode) throw new Exception("PatternNode cannot be processed into a valid data type");
        if (baseNode instanceof TernaryNode node) return getTernaryIDT(node, localVariables);
        if (baseNode instanceof VariableReferenceNode node) return getVariableIDT(node, localVariables);
        if (baseNode instanceof OperationNode node) return getOperationIDT(node, localVariables);
        throw new IllegalStateException("Tried getting IDT from invalid node " + baseNode.getClass().getName());
    }

    /**
     * helper function to grab globals
     * @param data an InterpreterDataType containing the variable name, in this case a number
     * @return an InterpreterDataType containing the value held in the variable
     * @throws Exception if the variable does not exist
     */
    private InterpreterDataType getDollarVariable(InterpreterDataType data) throws Exception {
        InterpreterDataType dollarVal = globalVariableMap.get("$" + data.getData());
        if (dollarVal == null) throw new Exception("Variable: $" + data.getData() + " is uninitialized");
        return dollarVal;
    }

    /**
     * checks an array to see if value is in it
     * @param value the value to check
     * @param operationNode the array to check
     * @param localVariables a list of local variables to locate the array
     * @return an InterpreterDataType containing 1 for true and 0 for false
     * @throws Exception if operationNode is not an array
     */
    private InterpreterDataType arrayMatcher(InterpreterDataType value, OperationNode operationNode, HashMap<String, InterpreterDataType> localVariables) throws Exception {
        if (!(operationNode.getRightHand().get() instanceof VariableReferenceNode array)) throw new Exception(operationNode.getRightHand() + " is not a valid array");
        InterpreterDataType arrayData = localVariables.getOrDefault(array.getName(), globalVariableMap.get(array.getName()));
        if (!(arrayData instanceof InterpreterArrayDataType actualArrayData)) throw new Exception(array + " is not a valid array");
        return new InterpreterDataType(actualArrayData.containsKey(value.getData()) ? "1" : "0");
    }

    /**
     * matches a regex expression in node to the value in left
     * @param left the string to match the pattern to
     * @param node the regex to match the pattern to
     * @return true if a match was found
     * @throws Exception if node is not a patternNode
     */
    private boolean patternMatcher(InterpreterDataType left, OperationNode node) throws Exception {
        if (!(node.getRightHand().get() instanceof PatternNode patternNode)) throw new Exception(node.getRightHand() + " must be a PatternNode");
        Pattern pattern = Pattern.compile(patternNode.getValue());
        Matcher matcher = pattern.matcher(left.getData());
        return matcher.find();
    }

    /**
     * takes an OperationNode and builds an InterpreterDataType
     * @param node the OperationNode to process
     * @param localVariables the local variables to write and read
     * @return an InterpreterDataType
     * @throws Exception if node is invalid
     */
    private InterpreterDataType getOperationIDT(OperationNode node, HashMap<String,InterpreterDataType> localVariables) throws Exception {
        InterpreterDataType returnData = new InterpreterDataType();
        if (node.getLeftHand().isEmpty()) {
            if (node.getRightHand().isEmpty()) throw new Exception(node + " is an invalid OperationNode");
            //$ ! - + ++ --
            InterpreterDataType data = getIDT(node.getRightHand().get(), localVariables);
            if (node.getOperation() == OperationNode.Operation.DOLLAR) return getDollarVariable(data);
            switch (node.getOperation()) {
                case NEG -> returnData.setData(data.negate());
                case SUB -> returnData.setData(data.floatNegate());
                case ADD -> returnData.setData(Double.toString(Double.parseDouble(data.getData())));
                case PREINC -> {data.increment(); returnData = data;}
                case PREDEC -> {data.decrement(); returnData = data;}
                default -> throw new IllegalStateException(node.getOperation() + " requires left hand");
            }
            return returnData;
        }
        InterpreterDataType left = getIDT(node.getLeftHand().get(), localVariables);
        if (node.getRightHand().isEmpty()) {
            if (node.getOperation() == OperationNode.Operation.POSTINC) left.increment();
            else if (node.getOperation() == OperationNode.Operation.POSTDEC) left.decrement();
            else throw new Exception(node.getOperation() + " requires right hand expression");
            return left;
        }
        if (node.getOperation() == OperationNode.Operation.IN) return arrayMatcher(left, node, localVariables);
        else if (node.getOperation() == OperationNode.Operation.MATCH) return new InterpreterDataType(patternMatcher(left, node) ? "1" : "0");
        else if (node.getOperation() == OperationNode.Operation.NOTMATCH) return new InterpreterDataType(patternMatcher(left, node) ? "0" : "1");
        InterpreterDataType right = getIDT(node.getRightHand().get(), localVariables);
        returnData.setData(switch (node.getOperation()) {
            case EXP -> left.pow(right);
            case MOD -> left.mod(right);
            case DIV -> left.div(right);
            case MUL -> left.mul(right);
            case SUB -> left.sub(right);
            case ADD -> left.add(right);
            case GT -> left.compareTo(right) > 0 ? "1" : "0";
            case LT -> left.compareTo(right) < 0 ? "1" : "0";
            case OR -> left.toBool() || right.toBool() ? "1" : "0";
            case AND -> left.toBool() && right.toBool() ? "1" : "0";
            case EQ -> left.compareTo(right) == 0 ? "1" : "0";
            case NE -> left.compareTo(right) != 0 ? "1" : "0";
            case GE -> left.compareTo(right) >= 0 ? "1" : "0";
            case LE -> left.compareTo(right) <= 0 ? "1" : "0";
            case CONCAT -> left.getData() + right.getData();
            default -> throw new IllegalStateException("Unexpected value: " + node.getOperation());
        });
        return returnData;
    }

    /**
     * takes an VariableReferenceNode and builds an InterpreterDataType
     * @param node the VariableReferenceNode to process
     * @param localVariables the local variables to write and read
     * @return an InterpreterDataType
     * @throws Exception if node is invalid
     */
    private InterpreterDataType getVariableIDT(VariableReferenceNode node, HashMap<String, InterpreterDataType> localVariables) throws Exception {
        InterpreterDataType data = localVariables.getOrDefault(node.getName(), globalVariableMap.get(node.getName()));
        if (data == null) throw new Exception(node + " is not initialized");
        if (node.getValue().isEmpty()) return data;
        if (!(data instanceof InterpreterArrayDataType array)) throw new Exception("Cannot index a non-array variable");
        InterpreterDataType index = getIDT(node.getValue().get(), localVariables);
        if (!array.containsKey(index.getData())) throw new IndexOutOfBoundsException(index + " is not a valid index for " + node.getName());
        return array.get(index.getData());
    }

    /**
     * takes a TernaryNode and builds an InterpreterDataType
     * @param node the TernaryNode to process
     * @param localVariables the local variables to write and read
     * @return an InterpreterDataType
     * @throws Exception if node is invalid
     */
    private InterpreterDataType getTernaryIDT(TernaryNode node, HashMap<String, InterpreterDataType> localVariables) throws Exception {
        InterpreterDataType boolCondition = getIDT(node.getCondition(), localVariables);
        return boolCondition.toBool() ? getIDT(node.getCaseTrue(), localVariables) : getIDT(node.getCaseFalse(), localVariables);
    }

    /**
     * takes a FunctionCallNode and executes it
     * @param node the FunctionCallNode to process
     * @param localVariables the local variables to pass to the function
     * @return a String
     */
    private String callFunction(FunctionCallNode node, HashMap<String,InterpreterDataType> localVariables) {
        return "";
    }

    /**
     * takes an AssignmentNode and builds an InterpreterDataType
     * @param node the AssignmentNode to process
     * @param localVariables the local variables to write and read
     * @return an InterpreterDataType
     * @throws Exception if node is invalid
     */
    private InterpreterDataType getAssignmentIDT(AssignmentNode node, HashMap<String, InterpreterDataType> localVariables) throws Exception {
        InterpreterDataType value = getIDT(node.getExpression(), localVariables);

        if (node.getTarget() instanceof VariableReferenceNode variable) {
            if (globalVariableMap.containsKey(variable.getName())) {
                globalVariableMap.put(variable.getName(), value);
                if (variable.getName().equals("FS")) lineManager.reset();
            } else localVariables.put(variable.getName(), value);
        } else if (node.getTarget() instanceof OperationNode variable) {
            if (variable.getOperation() != OperationNode.Operation.DOLLAR) throw new Exception("Expression cannot be assigned to");
            globalVariableMap.put("$" + variable.getLeftHand().get(), value);
        } else throw new IllegalStateException("Assignment target cannot be " + node.getClass().getName());

        return value;
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

        public void reset() {
            lineRecord--;
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
