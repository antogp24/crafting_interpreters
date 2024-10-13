package src;

import java.util.Map;
import java.util.HashMap;

class LoxInstance {
    private final LoxClass lox_class;
    private final Map<String, Object> fields;

    LoxInstance(LoxClass lox_class) {
        this.lox_class = lox_class;
        this.fields = new HashMap<>(lox_class.fields);
    }

    Object get(Token name) {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LoxFunction method = lox_class.find_method(name.lexeme);
        if (method != null) return method.bind(this);

        String message = "The class '" + lox_class.name + "' doesn't contain field '" + name.lexeme + "'.";
        throw new LoxRuntimeError(name, message);
    }

    void set(Token name, Object value) {
        if (!fields.containsKey(name.lexeme)) {
            String message = "The class '" + lox_class.name + "' doesn't contain field '" + name.lexeme + "'.";
            throw new LoxRuntimeError(name, message);
        }
        if (lox_class.find_method(name.lexeme) != null) {
            String message = "The name '" + name.lexeme + "' is already used as a method.";
            throw new LoxRuntimeError(name, message);
        }
        fields.put(name.lexeme, value);
    }

    @Override
    public String toString() {
        return "<instanceof " + lox_class.name + ">";
    }
}
