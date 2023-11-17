import parser.ConstantNode;
import parser.Node;
import parser.OperationNode;
import parser.PatternNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * InterpreterDataType is the basic data type for the interpreter
 */
public class InterpreterDataType implements Comparable<InterpreterDataType> {

    /**
     * the value of this data type
     */
    private String data;

    public InterpreterDataType() {
        this.data = "";
    }

    public InterpreterDataType(String data) {
        this.data = data;
    }

    //basic accessor
    public String getData() {
        return data;
    }

    //basic mutator
    public void setData(String data) {
        this.data = data;
    }

    public String floatNegate() {
        return String.valueOf(Double.parseDouble(data) * -1);
    }

    public void increment() {
        this.data = String.valueOf(Integer.parseInt(data) + 1);
    }

    public void decrement() {
        this.data = String.valueOf(Integer.parseInt(data) - 1);
    }

    public String negate() {
        return toBool() ? "0" : "1";
    }

    public String pow(InterpreterDataType rightSide) {
        double leftFloat = Double.parseDouble(data);
        double rightFloat = Double.parseDouble(rightSide.data);
        return String.valueOf(Math.pow(leftFloat, rightFloat));
    }

    public String mod(InterpreterDataType rightSide) {
        double leftFloat = Double.parseDouble(data);
        double rightFloat = Double.parseDouble(rightSide.data);
        return String.valueOf(leftFloat % rightFloat);
    }

    public String div(InterpreterDataType rightSide) {
        double leftFloat = Double.parseDouble(data);
        double rightFloat = Double.parseDouble(rightSide.data);
        return String.valueOf(leftFloat % rightFloat);
    }

    public String mul(InterpreterDataType rightSide) {
        double leftFloat = Double.parseDouble(data);
        double rightFloat = Double.parseDouble(rightSide.data);
        return String.valueOf(leftFloat * rightFloat);
    }

    public String add(InterpreterDataType rightSide) {
        if (data.isEmpty()) return rightSide.data;
        if (rightSide.data.isEmpty()) return data;

        double leftFloat = Double.parseDouble(data);
        double rightFloat = Double.parseDouble(rightSide.data);
        return String.valueOf(leftFloat + rightFloat);
    }

    public String sub(InterpreterDataType rightSide) {
        double leftFloat = Double.parseDouble(data);
        double rightFloat = Double.parseDouble(rightSide.data);
        return String.valueOf(leftFloat - rightFloat);
    }

    public boolean toBool() {
        try {
            double value = Double.parseDouble(data);
            return value != 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "IDT: \"" + data + "\"";
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof InterpreterDataType other)) return false;
        return other.data.equals(data);
    }

    @Override
    public int compareTo(InterpreterDataType o) {
        try {
            Double leftFloat = Double.parseDouble(data);
            Double rightFloat = Double.parseDouble(o.data);
           return leftFloat.compareTo(rightFloat);
        } catch (Exception ignored) {
            return data.compareTo(o.data);
        }
    }
}
