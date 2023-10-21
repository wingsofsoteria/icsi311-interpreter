/**
 * InterpreterDataType is the basic data type for the interpreter
 */
public class InterpreterDataType {

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

    @Override
    public String toString() {
        return "IDT: " + data;
    }
}
