package src;

class AstPrinter implements Expr.Visitor<String> {

    public static void demo() {
        Expr expr = new Expr.Binary(
            new Expr.Unary(
                new Token(TokenType.MINUS, "-", null, 1),
                new Expr.Literal(123)),
            new Token(TokenType.STAR, "*", null, 1),
            new Expr.Grouping(new Expr.Literal(45.67)));

        AstPrinter printer = new AstPrinter();
        String representation = printer.to_string(expr);
        System.out.println(representation);
    }

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
    public String visit_binary_expr(Expr.Binary expr) {
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
        return expr.value.toString();
    }

    @Override
    public String visit_unary_expr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }
}