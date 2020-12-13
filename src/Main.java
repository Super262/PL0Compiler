import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args){
        LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer("test.txt");
        ArrayList<Token> result = null;
        try {
            result = lexicalAnalyzer.analyze();
            for(Token token : result){
                System.out.print("sym: " + token.getSym());
                System.out.print("\t\tnum: " + token.getNum());
                System.out.println("\t\tid: " + token.getId());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
