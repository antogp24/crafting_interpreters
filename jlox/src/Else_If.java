package src;

class Else_If {
    final Expr condition;
    final Stmt then_branch;

    Else_If(Expr condition, Stmt then_branch) {
        this.condition = condition;
        this.then_branch = then_branch;
    }
}
