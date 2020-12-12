import java.io.IOException;

public class Main {
    public static void main(String[] args){
        VirtualMachine vm = new VirtualMachine("parserout.txt", "result.txt");
        try {
            vm.runVirtualMachine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
