import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <out dir>");
            System.exit(64);
        }
        final String out_dir = args[0];

        define_ast(out_dir, "Expr", Arrays.asList(
            "Assign   : Token name, Expr value",
            "Binary   : Expr left, Token operator, Expr right",
            "Call     : Expr callee, Token paren, List<Expr> arguments",
            "Grouping : Expr expression",
            "Lambda   : Token token, List<Token> params, List<Stmt> body",
            "Literal  : Object value",
            "Logical  : Expr left, Token operator, Expr right",
            "Ternary  : Expr condition, Expr if_true, Expr otherwise",
            "Unary    : Token operator, Expr right",
            "Variable : Token name, boolean function"
        ));

        define_ast(out_dir, "Stmt", Arrays.asList(
            "Block       : List<Stmt> statements",
            "Break       : ",
            "Continue    : ",
            "Expression  : Expr expression",
            "Function    : Token name, List<Token> params, List<Stmt> body",
            "If          : Expr condition, Stmt then_branch," +
                         " List<Else_If> else_ifs," +
                         " Stmt else_branch",
            "Print       : Expr expression, boolean newline",
            "Return      : Token keyword, Expr value",
            "Var         : Token name, Expr initializer",
            "While       : Expr condition, Stmt body, boolean has_increment"
        ));
    }

    static void define_ast(String out_dir, String base_name, List<String> types) {
        try {
            define_ast_impl(out_dir, base_name, types);
        } catch (IOException error) {
            error.printStackTrace();
        }
    }

    static void define_ast_impl(String out_dir, String base_name, List<String> types) throws IOException {
        String path = out_dir + "/" + base_name + ".java";
        PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.println("package src;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + base_name + " {");

        if (base_name.equals("Expr")) {
            writer.println();
            define_to_string(writer);
        }

        writer.println();
        define_visitor(writer, base_name, types);

        // The base accept() method.
        writer.println("\n\tabstract <R> R accept(Visitor<R> visitor);");

        for (String type : types ) {
            writer.println();
            String class_name = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            define_type(writer, base_name, class_name, fields);
        }

        writer.println("}");
        writer.close();
    }

    static void define_type(PrintWriter writer, String base_name, String class_name, String fields) {
        writer.println("\tstatic class " + class_name + " extends " + base_name + " {");

        // Constructor.
        writer.println("\t\t" + class_name + "(" + fields + ") {");

        String[] individual_fields = fields.split(",");
        for (String field : individual_fields) {
            String[] parts = field.trim().split(" ");
            if (parts.length != 2) continue;
            String name = parts[1];
            writer.println("\t\t\tthis." + name + " = " + name + ";");
        }
        writer.println("\t\t}");

        // Visitor Pattern.
        writer.println();
        writer.println("\t\t@Override");
        writer.println("\t\t<R> R accept(Visitor<R> visitor) {");
        writer.println("\t\t\treturn visitor." + get_visitor_func_name(class_name, base_name) + "(this);");
        writer.println("\t\t}");

        // Constant fields.
        writer.println();
        for (String field : individual_fields) {
            if (field.trim().isEmpty()) continue;
            writer.println("\t\tfinal " + field.trim() + ";");
        }
        writer.println("\t}");
    }

    static String get_visitor_func_name(String type_name, String base_name) {
        return "visit_" + type_name.toLowerCase() + "_" + base_name.toLowerCase();
    }

    static void define_to_string(PrintWriter writer) {
        writer.println("\t@Override");
        writer.println("\tpublic String toString() {");
        writer.println("\t\tAstPrinter printer = new AstPrinter();");
        writer.println("\t\treturn printer.to_string(this);");
        writer.println("\t}");
    }

    static void define_visitor(PrintWriter writer, String base_name, List<String> types) {
        writer.println("\tinterface Visitor<R> {");
        for (String type : types) {
            String type_name = type.split(":")[0].trim();
            String func_name = get_visitor_func_name(type_name, base_name);
            writer.println("\t\tR " + func_name + "(" + type_name + " " + base_name.toLowerCase() + ");");
        }
        writer.println("\t}");
    }
}
