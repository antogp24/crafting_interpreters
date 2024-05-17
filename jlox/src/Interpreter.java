package src;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;
    private final Map<Expr, Integer> locals = new HashMap<>();

    private boolean broke = false;
    private boolean continued = false;

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn clock>";
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (LoxRuntimeError error) {
            Lox.runtime_error(error);
        }
    }

    private String stringify(Object value) {
        if (value == null) return "nil";

        if (value instanceof Number) {
            String text = value.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return value.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }

    public void execute_block(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt stmt : statements) {
                if (this.broke || this.continued) break;
                execute(stmt);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visit_expression_stmt(Stmt.Expression stmt) {
        Object value = evaluate(stmt.expression);
        if (Lox.REPL) {
            String first_part = "expression statement '" + stmt.expression + "' has a value of '";
            System.out.println(first_part + stringify(value) + "'.");
        }
        return null;
    }

    @Override
    public Void visit_function_stmt(Stmt.Function stmt) {
        if (environment.find(stmt.name)) {
            String message = "Function name '" + stmt.name.lexeme + "' is already in use.";
            throw new LoxRuntimeError(stmt.name, message);
        }

        LoxFunction function = new LoxFunction(stmt, environment);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visit_print_stmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        if (stmt.newline)
            System.out.println(stringify(value));
        else
            System.out.print(stringify(value));
        return null;
    }

    @Override
    public Void visit_return_stmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);
        throw new LoxReturn(value);
    }

    @Override
    public Void visit_var_stmt(Stmt.Var stmt) {
        Object value;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        } else {
            value = new UninitializedValue();
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visit_if_stmt(Stmt.If stmt) {
        Object condition = evaluate(stmt.condition);
        if (is_truthy(condition)) {
            execute(stmt.then_branch);
        } else {
            boolean matched = false;
            for (Else_If else_if : stmt.else_ifs) {
                Object else_if_condition = evaluate(else_if.condition);
                if (is_truthy(else_if_condition)) {
                    execute(else_if.then_branch);
                    matched = true;
                    break;
                }
            }
            if (stmt.else_branch != null && !matched) execute(stmt.else_branch);
        }
        return null;
    }

    @Override
    public Void visit_break_stmt(Stmt.Break stmt) {
        this.broke = true;
        return null;
    }

    @Override
    public Void visit_continue_stmt(Stmt.Continue stmt) {
        this.continued = true;
        return null;
    }

    @Override
    public Void visit_while_stmt(Stmt.While stmt) {
        while (is_truthy(evaluate(stmt.condition))) {
            execute(stmt.body);
            if (this.broke) {
                this.broke = false;
                break;
            }
            if (this.continued) {
                this.continued = false;
                if (stmt.body instanceof Stmt.Block block && stmt.has_increment)
                    execute(block.statements.getLast());
            }
        }
        return null;
    }

    @Override
    public Void visit_block_stmt(Stmt.Block stmt) {
        execute_block(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Object visit_lambda_expr(Expr.Lambda expr) {
        Stmt.Function fn = new Stmt.Function(expr.token, expr.params, expr.body);
        return new LoxFunction(fn, environment);
    }

    @Override
    public Object visit_literal_expr(Expr.Literal expr) {
        if (expr.value instanceof String str) {
            return str.translateEscapes();
        }
        return expr.value;
    }

    @Override
    public Object visit_grouping_expr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visit_variable_expr(Expr.Variable expr) {
        return lookup_variable(expr.name, expr);
    }

    private Object lookup_variable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if (distance != null) {
            return environment.get_at(distance, name.lexeme);
        }
        return globals.get(name);
    }

    @Override
    public Object visit_unary_expr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS: {
                check_number_operand(expr.operator, right);
                return -(double)right;
            }
            case PLUS: {
                check_number_operand(expr.operator, right);
                return (double)right;
            }
            case BANG: {
                return !is_truthy(right);
            }
            case BITWISE_NOT: {
                check_number_operand(expr.operator, right);
                return (double)~((long)(double)right);
            }
        }
        return null;
    }

    private void check_number_operand(Token operator, Object operand) {
        if (operand instanceof Number) return;
        throw new LoxRuntimeError(operator, "Operand must be a number.");
    }

    private boolean is_truthy(Object value) {
        if (value == null) return false;
        if (value instanceof Number) return (double)value != 0;
        if (value instanceof Boolean) return (boolean)value;
        return true;
    }

    @Override
    public Object visit_assign_expr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer distance = locals.get(expr);
        if (distance != null) {
            environment.assign_at(distance, expr.name, value);
        } else {
            globals.assign(expr.name, value);
        }

        return value;
    }

    @Override
    public Object visit_call_expr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            Object value = evaluate(argument);
            arguments.add(value);
        }

        if (!(callee instanceof LoxCallable function)) {
            throw new LoxRuntimeError(expr.paren,
                    "Can only call functions and classes.");
        }

        if (arguments.size() != function.arity()) {
            String message = "Expected " + function.arity() + " arguments but found " + arguments.size() + ".";
            throw new LoxRuntimeError(expr.paren, message);
        }

        return function.call(this, arguments);
    }

    @Override
    public Object visit_binary_expr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case STAR: {
                check_number_operands(left, expr.operator, right);
                return (double)left * (double)right;
            }
            case SLASH: {
                check_number_operands(left, expr.operator, right);
                if ((double)right == 0)
                    throw new LoxRuntimeError(expr.operator, "Can't divide by zero.");
                return (double)left / (double)right;
            }
            case MINUS: {
                check_number_operands(left, expr.operator, right);
                return (double)left - (double)right;
            }
            case PLUS: {
                if (left instanceof Number && right instanceof Number)
                    return (double)left + (double)right;
                if (left instanceof String && right instanceof String)
                    return (String)left + (String)right;
                if (left instanceof String && right instanceof Number)
                    return (String)left + stringify(right);
                if (left instanceof Number && right instanceof String)
                    return stringify(left) + (String)right;
                throw new LoxRuntimeError(expr.operator, "Operands must be both numbers or either one of them a string.");
            }

            case GREATER: {
                check_number_operands(left, expr.operator, right);
                return (double)left > (double)right;
            }
            case LESS: {
                check_number_operands(left, expr.operator, right);
                return (double)left < (double)right;
            }
            case GREATER_EQUAL: {
                check_number_operands(left, expr.operator, right);
                return (double)left >= (double)right;
            }
            case LESS_EQUAL: {
                check_number_operands(left, expr.operator, right);
                return (double)left <= (double)right;
            }

            case EQUAL_EQUAL: return is_equal(left, right);
            case BANG_EQUAL: return !is_equal(left, right);

            case BITWISE_AND: {
                check_number_operands(left, expr.operator, right);
                return (double)((long)(double)left & (long)(double)right);
            }
            case BITWISE_OR: {
                check_number_operands(left, expr.operator, right);
                return (double)((long)(double)left | (long)(double)right);
            }
            case BITWISE_XOR: {
                check_number_operands(left, expr.operator, right);
                return (double)((long)(double)left ^ (long)(double)right);
            }
            case LEFT_SHIFT: {
                check_number_operands(left, expr.operator, right);
                return (double)((long)(double)left << (long)(double)right);
            }
            case RIGHT_SHIFT: {
                check_number_operands(left, expr.operator, right);
                return (double)((long)(double)left >> (long)(double)right);
            }
        }
        return null;
    }

    @Override
    public Object visit_logical_expr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        boolean a = is_truthy(left), b = is_truthy(right);

        switch (expr.operator.type) {
            case OR: {
                if (a) return true;
            }
            case AND: {
                if (!a) return false;
            }
            default: return b;
        }
    }

    private void check_number_operands(Object a, Token operator, Object b) {
        if (a instanceof Number && b instanceof Number) return;
        throw new LoxRuntimeError(operator, "Operands must be numbers.");
    }

    private boolean is_equal(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    @Override
    public Object visit_ternary_expr(Expr.Ternary expr) {
        Object condition = evaluate(expr.condition);
        Object if_true = evaluate(expr.if_true);
        Object otherwise = evaluate(expr.otherwise);

        if (is_truthy(condition)) {
            return if_true;
        }
        return otherwise;
    }
}
