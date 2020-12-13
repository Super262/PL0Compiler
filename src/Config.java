public class Config {
    public static final int MAX_STACK_HEIGHT = 2000;
    public static final int MAX_CODE_SIZE = 1000;
    public static final int MAX_SYMBOL_TABLE_SIZE = 1000;
    public static final int MAX_LEXI_LEVELS = 3;
    public static final int MAX_ID_LENGTH = 10;
    public static final int MAX_NUM_LENGTH = 5;
    public static final String[] reservedWords ={
            "const", "var", "procedure", "call", "begin", "end", "if",
            "then", "else", "while", "do", "read", "write", "odd"};
    public static final char[] specialSymbols={'+', '-', '*', '/', '(', ')', '=', ',' , '.', '<', '>',  ';' , ':','#'};
}
