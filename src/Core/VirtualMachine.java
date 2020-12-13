package Core;

import Pojo.Instruction;

import java.io.*;
import java.util.Scanner;

public class VirtualMachine {

    private final File inputFile, outputFile;
    private int SP = 0, BP = 1, PC = 0, IR = 0;
    private final int[] stack;
    private final Instruction[] instructionArray;
    private final String[] opCodes = {"ILLEGAL", "lit", "opr", "lod", "sto", "cal", "int", "jmp", "jpc", "sio", "sio"};

    public VirtualMachine(String inputPath, String outputPath){
        this.inputFile = new File(inputPath);
        this.outputFile = new File(outputPath);
        stack = new int[Property.Configuration.MAX_STACK_HEIGHT];

        // codeArray is a filled array of instructions, containing the op, l, and m.
        instructionArray = new Instruction[Property.Configuration.MAX_CODE_SIZE];
    }

    public void startVM() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        FileReader fileReader = new FileReader(inputFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        // Scans in the instructions line by line until end of file.
        // The Line, OP (by name not number), L, and M.
        stringBuilder.append("Line\tOP\t\tL\tM\n");
        String lineValue;
        int lineIndex = 0;
        while((lineValue = bufferedReader.readLine()) != null){
            String[] instructEles = lineValue.split(" ");
            if(instructEles.length == 3){
                Instruction instruction = new Instruction(
                        Integer.parseInt(instructEles[0]),
                        Integer.parseInt(instructEles[1]),
                        Integer.parseInt(instructEles[2]));
                instructionArray[lineIndex] = instruction;
                stringBuilder.append(lineIndex).append("\t\t").append(opCodes[instructionArray[lineIndex].getOpCode()]).append("\t\t");
                stringBuilder.append(instructionArray[lineIndex].getLevel()).append("\t");
                stringBuilder.append(instructionArray[lineIndex].getM()).append("\t");
                stringBuilder.append("\n");
                ++lineIndex;
            }
        }

        stringBuilder.append("\n\n");
        stringBuilder.append("\t\t\t\t\tpc \tbp \tsp \tstack\n");
        stringBuilder.append("Initial values\t\t").append(PC).append("  \t").append(BP).append(" \t").append(SP).append(" \n");

        while(BP != 0){
            Instruction instruction = instructionArray[PC];
            stringBuilder.append(PC).append("\t");
            stringBuilder.append(opCodes[instruction.getOpCode()]).append(" \t");
            stringBuilder.append(instruction.getLevel()).append(" \t");
            stringBuilder.append(instruction.getM());
            ++PC;
            executeCycle(instruction);
            stringBuilder.append("\t").append(PC).append("  \t").append(BP).append(" \t").append(SP).append(" \t");
            printStackFrame(SP, BP, stringBuilder);
            stringBuilder.append("\n");
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
        bw.write(stringBuilder.toString());
        bw.close();
    }

    //Recursive function that prints each stack frame using | in between
    private void printStackFrame(final int SP, final int BP, StringBuilder stringBuilder) {
        if (BP == 0) {
            // Base Case #1: if BP is 0, the program has finished. No stack frames are left to print out
            return;
        }
        else if (BP == 1) {
            // Base Case #2: if BP is 1, then it is in the main stack frame, and we print out the stack from BP to SP, with BP pointing to the bottom of the main stack frame, and SP pointing to the top of the stack
            for(int i = 1;i <= SP; ++i){
                stringBuilder.append(stack[i]).append(" ");
            }
        }
        else {
            // Recursive Case: Prints out each new stack frame, separating them with |
            printStackFrame(BP - 1, stack[BP + 2], stringBuilder);

            // Covers one case, where CAL instruction is just called, meaning a new Stack Frame is created, but SP is still less than BP
            if (SP < BP) {
                stringBuilder.append("| ");
                for (int i = 0; i < 4; ++i) {
                    stringBuilder.append(stack[BP + i]).append(" ");
                }
            }

            // For SP being greater than BP, as known as most cases
            else {
                stringBuilder.append("| ");
                for (int i = BP; i <= SP; ++i) {
                    stringBuilder.append(stack[i]).append(" ");
                }
            }
        }
    }

    // Execute Cycle Function: mimics execute, and contains code
    // for each instruction in ISA, implemented in a switch.
    // OPR is another function because it was better looking.
    private void executeCycle(Instruction instruction){
        switch(instruction.getOpCode()){
            case 1: { // LIT
                SP = SP + 1;
                stack[SP] = instruction.getM();
                break;
            }

            case 2: { // OPR function
                OPR(instruction);
                break;
            }

            case 3: { // LOD
                SP = SP + 1;
                stack[SP] = stack[findBase(instruction.getLevel()) + instruction.getM()];
                break;
            }

            case 4: { // STO
                stack[findBase(instruction.getLevel()) + instruction.getM()] = stack[SP];
                SP = SP - 1;
                break;
            }

            case 5: { // CAL
                stack[SP + 1] = 0; // space to return value
                stack[SP + 2] = findBase(instruction.getLevel()); //static link (SL)
                stack[SP + 3] = BP; // dynamic link (DL)
                stack[SP + 4] = PC; // return address (RA)
                BP = SP + 1;
                PC = instruction.getM();
                break;
            }

            case 6: { // INT
                SP = SP + instruction.getM();
                break;
            }

            case 7: { // JMP
                PC = instruction.getM();
                break;
            }

            case 8: { // JPC
                if (stack[SP] == 0) {
                    PC = instruction.getM();
                }
                SP = SP - 1;
                break;
            }

            case 9: { // SIO1
                System.out.println(stack[SP]);
                SP = SP - 1;
                break;
            }

            case 10: { // SIO2
                SP = SP + 1;
                Scanner scanner = new Scanner(System.in);
                stack[SP] = scanner.nextInt();
                scanner.close();
                break;
            }

            default:{
                System.out.println("Illegal OPR! ");
            }
        }
    }

    //OPR: also a switch, determined by the OP's M, passed through Pojo.Instruction.
    private void OPR (Instruction instruction){
        switch(instruction.getM()){

            case 0: { // RETURN
                SP = BP - 1;
                PC = stack[SP + 4];
                BP = stack[SP + 3];
                break;
            }

            case 1: { // NEG
                stack[SP] = -stack[SP];
                break;
            }

            case 2: { // ADD
                SP = SP - 1;
                stack[SP] = stack[SP] + stack[SP + 1];
                break;
            }

            case 3: { // SUB
                SP = SP - 1;
                stack[SP] = stack[SP] - stack[SP + 1];
                break;
            }

            case 4: { // MUL
                SP = SP - 1;
                stack[SP] = stack[SP] * stack[SP + 1];
                break;
            }

            case 5: { // DIV
                SP = SP - 1;
                stack[SP] = stack[SP] / stack[SP + 1];
                break;
            }

            case 6: { // ODD
                stack[SP] = stack[SP] % 2;
                break;
            }

            case 7: { // MOD
                SP = SP - 1;
                stack[SP] = stack[SP] % stack[SP + 1];
                break;
            }

            case 8: { //EQL
                SP = SP - 1;
                stack[SP] = (stack[SP] == stack[SP + 1] ? 1 : 0);
                break;
            }

            case 9: { //NEQ
                SP = SP - 1;
                stack[SP] = (stack[SP] != stack[SP + 1] ? 1 : 0);
                break;
            }

            case 10: { //LSS
                SP = SP - 1;
                stack[SP] = (stack[SP] < stack[SP + 1] ? 1 : 0);
                break;
            }

            case 11: { //LEQ
                SP = SP - 1;
                stack[SP] = (stack[SP] <= stack[SP + 1] ? 1 : 0);
                break;
            }

            case 12: { //GTR
                SP = SP - 1;
                stack[SP] = (stack[SP] > stack[SP + 1] ? 1 : 0);
                break;
            }

            case 13: { //GEQ
                SP = SP - 1;
                stack[SP] = (stack[SP] >= stack[SP + 1] ? 1 : 0);
                break;
            }

            default: {
                System.out.println("Illegal M! ");
                break;
            }
        }
    }

    //findBase: function used to find base L levels down
    private int findBase(int level){
        int b1 = BP;
        while(level > 0){
            b1 = stack[b1 + 1];
            --level;
        }
        return b1;
    }

}
