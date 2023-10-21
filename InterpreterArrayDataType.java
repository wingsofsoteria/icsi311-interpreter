import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * InterpreterArrayDataType represents an array in awk
 * it implements Map because many operations involving InterpreterArrayDataType operate on this.arrayData and Map provides convenient functions to perform those operations
 */
public class InterpreterArrayDataType extends InterpreterDataType implements Map<String, InterpreterDataType> {

    /**
     * the data this data type is holding
     */
    private final HashMap<String, InterpreterDataType> arrayData;

    public InterpreterArrayDataType() {
        this.arrayData = new HashMap<>();
    }

    @Override
    public String toString() {
        return "IADT: " + arrayData;
    }

    @Override
    public int size() {
        return arrayData.size();
    }

    @Override
    public boolean isEmpty() {
        return arrayData.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return arrayData.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return arrayData.containsValue(value);
    }

    @Override
    public InterpreterDataType get(Object key) {
        return arrayData.get(key);
    }

    @Override
    public InterpreterDataType put(String key, InterpreterDataType value) {
        return arrayData.put(key, value);
    }

    @Override
    public InterpreterDataType remove(Object key) {
        return arrayData.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends InterpreterDataType> m) {
        arrayData.putAll(m);
    }

    @Override
    public void clear() {
        arrayData.clear();
    }

    @Override
    public Set<String> keySet() {
        return arrayData.keySet();
    }

    @Override
    public Collection<InterpreterDataType> values() {
        return arrayData.values();
    }

    @Override
    public Set<Entry<String, InterpreterDataType>> entrySet() {
        return arrayData.entrySet();
    }
}
