package src;

import java.util.List;

abstract class Stmt {

	interface Visitor<R> {
		R visit_block_stmt(Block stmt);
		R visit_break_stmt(Break stmt);
		R visit_continue_stmt(Continue stmt);
		R visit_expression_stmt(Expression stmt);
		R visit_function_stmt(Function stmt);
		R visit_if_stmt(If stmt);
		R visit_print_stmt(Print stmt);
		R visit_return_stmt(Return stmt);
		R visit_var_stmt(Var stmt);
		R visit_while_stmt(While stmt);
	}

	abstract <R> R accept(Visitor<R> visitor);

	static class Block extends Stmt {
		Block(List<Stmt> statements) {
			this.statements = statements;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visit_block_stmt(this);
		}

		final List<Stmt> statements;
	}

	static class Break extends Stmt {
		Break() {
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visit_break_stmt(this);
		}

	}

	static class Continue extends Stmt {
		Continue() {
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visit_continue_stmt(this);
		}

	}

	static class Expression extends Stmt {
		Expression(Expr expression) {
			this.expression = expression;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visit_expression_stmt(this);
		}

		final Expr expression;
	}

	static class Function extends Stmt {
		Function(Token name, List<Token> params, List<Stmt> body) {
			this.name = name;
			this.params = params;
			this.body = body;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visit_function_stmt(this);
		}

		final Token name;
		final List<Token> params;
		final List<Stmt> body;
	}

	static class If extends Stmt {
		If(Expr condition, Stmt then_branch, List<Else_If> else_ifs, Stmt else_branch) {
			this.condition = condition;
			this.then_branch = then_branch;
			this.else_ifs = else_ifs;
			this.else_branch = else_branch;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visit_if_stmt(this);
		}

		final Expr condition;
		final Stmt then_branch;
		final List<Else_If> else_ifs;
		final Stmt else_branch;
	}

	static class Print extends Stmt {
		Print(Expr expression, boolean newline) {
			this.expression = expression;
			this.newline = newline;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visit_print_stmt(this);
		}

		final Expr expression;
		final boolean newline;
	}

	static class Return extends Stmt {
		Return(Token keyword, Expr value) {
			this.keyword = keyword;
			this.value = value;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visit_return_stmt(this);
		}

		final Token keyword;
		final Expr value;
	}

	static class Var extends Stmt {
		Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visit_var_stmt(this);
		}

		final Token name;
		final Expr initializer;
	}

	static class While extends Stmt {
		While(Expr condition, Stmt body, boolean has_increment) {
			this.condition = condition;
			this.body = body;
			this.has_increment = has_increment;
		}

		@Override
		<R> R accept(Visitor<R> visitor) {
			return visitor.visit_while_stmt(this);
		}

		final Expr condition;
		final Stmt body;
		final boolean has_increment;
	}
}
