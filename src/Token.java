public class Token {
    private TokenType sym;
    private int num;
    private String id;   // Max Length = 10

    public Token(){
        this.sym = TokenType.none;
        id = "";
        num = 0;
    }

    public TokenType getSym() {
        return sym;
    }

    public void setSym(TokenType sym) {
        this.sym = sym;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
