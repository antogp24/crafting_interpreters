package src;

import java.util.List;
import java.util.Stack;
import java.util.Map;
import java.util.HashMap;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType current_fn = FunctionType.NONE;
    private ClassType current_class = ClassType.NONE;

    private enum FunctionType {
        NONE,
        FUNCTION,
        LAMBDA,
        INITIALIZER,
        METHOD,
    }

    private enum ClassType {
        NONE,
        CLASS,
    }

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visit_block_stmt(Stmt.Block stmt) {
        begin_scope();
        resolve_statements(stmt.statements);
        end_scope();
        return null;
    }

    void begin_scope() {
        scopes.push(new HashMap<String, Boolean>());
    }

    void end_scope() {
        scopes.pop();
    }

    void resolve_statements(List<Stmt> statements) {
        for (Stmt statement : statements) {
            resolve_stmt(statement);
        }
    }

    void resolve_stmt(Stmt statement) {
        statement.accept(this);
    }

    void resolve_expr(Expr expr) {
        expr.accept(this);
    }

    @Override
    public Void visit_var_stmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve_expr(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;
        var scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Redeclaration of variable '" + name.lexeme + "'.");
        }
        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    @Override
    public Void visit_variable_expr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }
        resolve_local(expr, expr.name);
        return null;
    }

    private void resolve_local(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                int depth = scopes.size() - 1 - i;
                interpreter.resolve(expr, depth);
                return;
            }
        }
    }

    @Override
    public Void visit_assign_expr(Expr.Assign expr) {
        resolve_expr(expr.value);
        resolve_local(expr, expr.name);
        return null;
    }

    @Override
    public Void visit_function_stmt(Stmt.Function function) {
        declare(function.name);
        define(function.name);

        resolve_function(function.params, function.body, FunctionType.FUNCTION);
        return null;
    }

    private void resolve_function(List<Token> params, List<Stmt> body, FunctionType type) {
        FunctionType enclosing_fn = current_fn;
        current_fn = type;

        begin_scope();
        for (Token param : params) {
            declare(param);
            define(param);
        }
        resolve_statements(body);
        end_scope();

        current_fn = enclosing_fn;
    }

    @Override
    public Void visit_if_stmt(Stmt.If stmt) {
        resolve_expr(stmt.condition);
        resolve_stmt(stmt.then_branch);
        for (Else_If else_if : stmt.else_ifs) {
            resolve_expr(else_if.condition);
            resolve_stmt(else_if.then_branch);
        }
        if (stmt.else_branch != null) resolve_stmt(stmt.else_branch);
        return null;
    }

    @Override
    public Void visit_print_stmt(Stmt.Print stmt) {
        resolve_expr(stmt.expression);
        return null;
    }
    @Override
    public Void visit_return_stmt(Stmt.Return stmt) {
        if (current_fn == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return outside of function.");
        }
        if (stmt.value != null) resolve_expr(stmt.value);
        return null;
    }

    @Override
    public Void visit_while_stmt(Stmt.While stmt) {
        resolve_expr(stmt.condition);
        resolve_stmt(stmt.body);
        return null;
    }

    @Override
    public Void visit_break_stmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visit_continue_stmt(Stmt.Continue stmt) {
        return null;
    }

    @Override
    public Void visit_class_stmt(Stmt.Class stmt) {
        ClassType enclosing_class = current_class;
        current_class = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        begin_scope();
        scopes.peek().put("this", true);
        for (Stmt.Function method : stmt.methods) {
            resolve_function(method.params, method.body, FunctionType.METHOD);
        }
        end_scope();
        current_class = enclosing_class;

        return null;
    }

    @Override
    public Void visit_expression_stmt(Stmt.Expression stmt) {
        resolve_expr(stmt.expression);
        return null;
    }

    @Override
    public Void visit_binary_expr(Expr.Binary expr) {
        resolve_expr(expr.left);
        resolve_expr(expr.right);
        return null;
    }

    @Override
    public Void visit_call_expr(Expr.Call expr) {
        resolve_expr(expr.callee);
        for (Expr arg : expr.arguments) {
            resolve_expr(arg);
        }
        return null;
    }

    @Override
    public Void visit_get_expr(Expr.Get expr) {
        resolve_expr(expr.object);
        return null;
    }

    @Override
    public Void visit_grouping_expr(Expr.Grouping expr) {
        resolve_expr(expr.expression);
        return null;
    }

    @Override
    public Void visit_lambda_expr(Expr.Lambda lambda) {
        resolve_function(lambda.params, lambda.body, FunctionType.LAMBDA);
        return null;
    }

    @Override
    public Void visit_literal_expr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visit_logical_expr(Expr.Logical expr) {
        resolve_expr(expr.left);
        resolve_expr(expr.right);
        return null;
    }

    @Override
    public Void visit_set_expr(Expr.Set expr) {
        resolve_expr(expr.object);
        resolve_expr(expr.value);
        return null;
    }

    @Override
    public Void visit_this_expr(Expr.This expr) {
        if (current_class == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of class.");
            return null;
        }
        resolve_local(expr, expr.keyword);
        return null;
    }

    @Override
    public Void visit_ternary_expr(Expr.Ternary expr) {
        resolve_expr(expr.condition);
        resolve_expr(expr.if_true);
        resolve_expr(expr.otherwise);
        return null;
    }

    @Override
    public Void visit_unary_expr(Expr.Unary expr) {
        resolve_expr(expr.right);
        return null;
    }
}
