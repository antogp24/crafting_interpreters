package src;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    boolean find(Token name) {
        if (values.containsKey(name.lexeme)) return true;
        if (enclosing != null) return enclosing.find(name);
        return false;
    }

    Environment ancestor(int distance) {
        Environment environment = this;
        for (int i = 0; i < distance; i++) {
            assert environment != null;
            environment = environment.enclosing;
        }
        return environment;
    }

    Object get_at(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            Object value = values.get(name.lexeme);
            if (value instanceof UninitializedValue) {
                throw new LoxRuntimeError(name, "Can't access uninitialized variable '" + name.lexeme  + "'.");
            }
            return value;
        }

        if (enclosing != null) return enclosing.get(name);

        throw new LoxRuntimeError(name, "Undefined variable '" + name.lexeme  + "'.");
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    void assign_at(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new LoxRuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
