public class ReturnType {

    private final Types returnType;
    private String value;

    public ReturnType(Types type) {
        this.returnType = type;
        this.value = "";
    }

    public String getValue() {
        return value;
    }

    public Types getReturnType() {
        return returnType;
    }

    public ReturnType(Types type, String value) {
        this(type);
        this.value = value;
    }

    public enum Types {
        None,
        Break,
        Continue,
        Return,

    }
}
