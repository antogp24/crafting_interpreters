package src;

enum TokenType {
    LEFT_PAREN, RIGHT_PAREN, // ( )
    LEFT_BRACE, RIGHT_BRACE, // { }
    COMMA, DOT, MINUS, PLUS, // , . - +
    SEMICOLON, SLASH, STAR,  // ; / *

    BANG, BANG_EQUAL,        //  !  !=
    EQUAL, EQUAL_EQUAL,      //  =  ==
    GREATER, GREATER_EQUAL,  //  >  >=
    LESS, LESS_EQUAL,        //  <  <=

    BITWISE_AND, BITWISE_OR,  // &   |
    BITWISE_NOT, BITWISE_XOR, // ~   ^
    LEFT_SHIFT, RIGHT_SHIFT,  // <<  >>

    COLON, QUESTION_MARK, // ? :

    IDENTIFIER, STRING, NUMBER,

    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    EOF,
};