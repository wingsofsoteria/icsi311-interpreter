import parser.BlockNode;
import parser.FunctionNode;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class BuiltinFunctionNode extends FunctionNode {

    private Function<HashMap<String, InterpreterDataType>, String> execute;
    private final boolean variadic;
    /**
     * constructs a FunctionNode
     *
     * @param name       the function name
     * @param parameters the function arguments
     */
    public BuiltinFunctionNode(String name, List<String> parameters, boolean variadic, Function<HashMap<String, InterpreterDataType>, String> functionDefinition) {
        super(name, parameters, Optional.empty(), variadic);
        this.variadic = variadic;
        this.execute = functionDefinition;
    }

    public String execute(HashMap<String, InterpreterDataType> args) {
        return execute.apply(args);
    }

}
