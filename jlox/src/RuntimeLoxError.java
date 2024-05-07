package src;

class RuntimeLoxError extends RuntimeException {
    final Token token;

    RuntimeLoxError(Token token, String message) {
        super(message);
        this.token = token;
    }
}
