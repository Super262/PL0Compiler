import Core.CodeGenerator;
import Core.LexicalAnalyzer;
import Core.VirtualMachine;
import Pojo.Token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args){
        try (Scanner scanner = new Scanner(System.in)) {
            for (int i = 0; i < 12; ++i) {
                LexicalAnalyzer lexicalAnalyzer = new LexicalAnalyzer("testInput/test" + i + ".txt");
                ArrayList<Token> result = lexicalAnalyzer.analyze();
                CodeGenerator codeGenerator = new CodeGenerator(result,"testOutputCode/code" + i + ".txt");
                codeGenerator.parse();
            }
            for (int i = 0; i < 12; ++i) {
                VirtualMachine virtualMachine = new VirtualMachine("testOutputCode/code" + i + ".txt",
                        "testVmResult/result" + i + ".txt",
                        scanner);
                virtualMachine.startVM();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
