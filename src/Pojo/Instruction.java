package Pojo;

public class Instruction {
    private final int opCode, level, m;

    public Instruction(){
        this.opCode = -1;
        this.level = -1;
        this.m = -1;
    }

    public Instruction(int opCode, int level, int m){
        this.opCode = opCode;
        this.level = level;
        this.m = m;
    }

    public int getOpCode() {
        return opCode;
    }

    public int getLevel() {
        return level;
    }

    public int getM() {
        return m;
    }
}
