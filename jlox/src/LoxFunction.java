package src;

import java.util.List;

class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    private final Environment closure;

    LoxFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            String key = declaration.params.get(i).lexeme;
            environment.define(key, arguments.get(i));
        }
        try {
            interpreter.execute_block(declaration.body, environment);
        } catch (LoxReturn return_value) {
            return return_value.value;
        }
        return null;
    }

    @Override
    public String toString() {
        if (declaration.name.type == TokenType.FUN) return "<lambda>";
        return "<fn " + declaration.name.lexeme + ">";
    }
}
