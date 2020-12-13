import Core.CodeGenerator;
import Core.LexicalAnalyzer;
import Core.VirtualMachine;
import Pojo.Token;

import java.io.IOException;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args){
        try {
            LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer("source.txt");
            ArrayList<Token> result = lexicalAnalyzer.analyze();
            for(Token token : result){
                System.out.print("sym: " + token.getSym());
                System.out.print("\t\tnum: " + token.getNum());
                System.out.println("\t\tid: " + token.getId());
            }
            CodeGenerator codeGenerator = new CodeGenerator(result, "code.txt");
            codeGenerator.parse();
            VirtualMachine virtualMachine = new VirtualMachine("code.txt", "result.txt");
            virtualMachine.startVM();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
