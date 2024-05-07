package src;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeLoxError error) {
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

    private void execute_block(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;
        try {
            this.environment = environment;
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Void visit_expression_stmt(Stmt.Expression stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visit_print_stmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
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
    public Void visit_block_stmt(Stmt.Block stmt) {
        execute_block(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Object visit_literal_expr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visit_grouping_expr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visit_variable_expr(Expr.Variable expr) {
        return environment.get(expr.name);
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
                return ~((long)(double)right);
            }
        }
        return null;
    }

    private void check_number_operand(Token operator, Object operand) {
        if (operand instanceof Number) return;
        throw new RuntimeLoxError(operator, "Operand must be a number.");
    }

    private boolean is_truthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (boolean)value;
        return true;
    }

    @Override
    public Object visit_assign_expr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
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
                    throw new RuntimeLoxError(expr.operator, "Can't divide by zero.");
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
                throw new RuntimeLoxError(expr.operator, "Operands must be both numbers or either one of them a string.");
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
                Double a = (double)left, b = (double)right;
                Long result = a.longValue() & b.longValue();
                return result.doubleValue();
            }
            case BITWISE_OR: {
                check_number_operands(left, expr.operator, right);
                Double a = (double)left, b = (double)right;
                Long result = a.longValue() | b.longValue();
                return result.doubleValue();
            }
            case BITWISE_XOR: {
                check_number_operands(left, expr.operator, right);
                Double a = (double)left, b = (double)right;
                Long result = a.longValue() ^ b.longValue();
                return result.doubleValue();
            }
            case LEFT_SHIFT: {
                check_number_operands(left, expr.operator, right);
                Double a = (double)left, b = (double)right;
                Long result = a.longValue() << b.longValue();
                return result.doubleValue();
            }
            case RIGHT_SHIFT: {
                check_number_operands(left, expr.operator, right);
                Double a = (double)left, b = (double)right;
                Long result = a.longValue() >> b.longValue();
                return result.doubleValue();
            }
        }
        return null;
    }

    private void check_number_operands(Object a, Token operator, Object b) {
        if (a instanceof Number && b instanceof Number) return;
        throw new RuntimeLoxError(operator, "Operands must be numbers.");
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
