import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {

    static final String base_name = "Expr";
    static final List<String> types = Arrays.asList(
        "Binary   : Expr left, Token operator, Expr right",
        "Ternary  : Expr condition, Expr if_true, Expr otherwise",
        "Grouping : Expr expression",
        "Literal  : Object value",
        "Unary    : Token operator, Expr right");

    public static void main(String args[]) {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <out dir>");
            System.exit(64);
        }
        try {
            define_ast(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void define_ast(String out_dir) throws IOException {
        String path = out_dir + "/" + base_name + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package src;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + base_name + " {");

        writer.println();
        define_to_string(writer);

        writer.println();
        define_visitor(writer);

        // The base accept() method.
        writer.println("\n\tabstract <R> R accept(Visitor<R> visitor);");

        for (String type : types ) {
            writer.println();
            String class_name = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            define_type(writer, class_name, fields);
        }

        writer.println("}");
        writer.close();
    }

    static void define_type(PrintWriter writer, String class_name, String fields) {
        writer.println("\tstatic class " + class_name + " extends " + base_name + " {");

        // Constructor.
        writer.println("\t\t" + class_name + "(" + fields + ") {");

        String[] individual_fields = fields.split(",");
        for (String field : individual_fields) {
            String name = field.trim().split(" ")[1];
            writer.println("\t\t\tthis." + name + " = " + name + ";");
        }
        writer.println("\t\t}");

        // Visitor Pattern.
        writer.println();
        writer.println("\t\t@Override");
        writer.println("\t\t<R> R accept(Visitor<R> visitor) {");
        writer.println("\t\t\treturn visitor." + get_visitor_func_name(class_name) + "(this);");
        writer.println("\t\t}");
        writer.println();

        // Constant fields.
        for (String field : individual_fields) {
            writer.println("\t\tfinal " + field.trim() + ";");
        }
        writer.println("\t}");
    }

    static String get_visitor_func_name(String type_name) {
        return "visit_" + type_name.toLowerCase() + "_" + base_name.toLowerCase();
    }

    static void define_to_string(PrintWriter writer) {
        writer.println("\t@Override");
        writer.println("\tpublic String toString() {");
        writer.println("\t\tAstPrinter printer = new AstPrinter();");
        writer.println("\t\treturn printer.to_string(this);");
        writer.println("\t}");
    }

    static void define_visitor(PrintWriter writer) {
        writer.println("\tinterface Visitor<R> {");
        for (String type : types) {
            String type_name = type.split(":")[0].trim();
            String func_name = get_visitor_func_name(type_name);
            writer.println("\t\tR " + func_name + "(" + type_name + " " + base_name.toLowerCase() + ");");
        }
        writer.println("\t}");
    }
}
