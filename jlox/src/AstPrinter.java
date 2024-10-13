package src;

import java.util.ArrayList;

class AstPrinter implements Expr.Visitor<String> {

    static void print(Expr expr) {
        AstPrinter printer = new AstPrinter();
        System.out.println(printer.to_string(expr));
    }

    String to_string(Expr expr) {
        return expr.accept(this);
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for (Expr expr : exprs) {
            builder.append(" ");
            String expr_str = to_string(expr);
            builder.append(expr_str);
        }
        builder.append(")");
        return builder.toString();
    }

    @Override
    public String visit_assign_expr(Expr.Assign expr) {
        return parenthesize("'" + expr.name.lexeme + "' = ", expr.value);
    }

    @Override
    public String visit_binary_expr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visit_call_expr(Expr.Call expr) {
        Expr[] arguments = expr.arguments.toArray(new Expr[0]);
        return parenthesize("call:" + expr.callee.toString(), arguments);
    }

    @Override
    public String visit_logical_expr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visit_set_expr(Expr.Set expr) {
        return "(" + to_string(expr.object) + "." + expr.name.lexeme + " = " + to_string(expr.value) + ")";
    }

    @Override
    public String visit_this_expr(Expr.This expr) {
        return "this";
    }

    @Override
    public String visit_ternary_expr(Expr.Ternary expr) {
        final Expr colon = new Expr.Literal(":");
        return parenthesize("?", expr.condition, expr.if_true, colon, expr.otherwise);
    }

    @Override
    public String visit_get_expr(Expr.Get expr) {
        return "(" + to_string(expr.object) + "." + expr.name.lexeme + ")";
    }

    @Override
    public String visit_grouping_expr(Expr.Grouping expr) {
        return parenthesize("grouping", expr.expression);
    }

    @Override
    public String visit_lambda_expr(Expr.Lambda expr) {
        if (expr.params.isEmpty()) return "(lambda)";

        StringBuilder params = new StringBuilder();
        for (Token param : expr.params) {
            params.append(param.lexeme).append(" ");
        }
        return "(lambda " + params.toString() + ")";
    }

    @Override
    public String visit_literal_expr(Expr.Literal expr) {
        if (expr.value == null) {
            return "nil";
        }
        if (expr.value instanceof String) {
            return "\"" + expr.value + "\"";
        }
        return expr.value.toString();
    }

    @Override
    public String visit_unary_expr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visit_variable_expr(Expr.Variable expr) {
        String type = expr.function ? "(fn " : "(var ";
        return type + expr.name.lexeme + ")";
    }
}