package src;

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
    public String visit_logical_expr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visit_ternary_expr(Expr.Ternary expr) {
        final Expr colon = new Expr.Literal(":");
        return parenthesize("?", expr.condition, expr.if_true, colon, expr.otherwise);
    }

    @Override
    public String visit_grouping_expr(Expr.Grouping expr) {
        return parenthesize("grouping", expr.expression);
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
        return "(var " + expr.name.lexeme + ")";
    }
}