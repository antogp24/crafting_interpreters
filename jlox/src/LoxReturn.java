package src;

class LoxReturn extends RuntimeException {
    final Object value;

    LoxReturn(Object value) {
        super(null, null, false, false);
        this.value = value;
    }
}
