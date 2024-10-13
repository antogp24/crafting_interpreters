package src;

import java.util.Map;
import java.util.List;

class LoxClass implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;
    public final Map<String, Object> fields;

    LoxClass(String name, Map<String, LoxFunction> methods, Map<String, Object> fields) {
        this.name = name;
        this.methods = methods;
        this.fields = fields;
    }

    LoxFunction find_method(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }
        return null;
    }

    @Override
    public String toString() {
        return "<class " + this.name + ">";
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        return new LoxInstance(this);
    }
}
