package Pojo;

public class Instruction {

    public Instruction(int opCode, int level, int m){
        this.opCode = opCode;
        this.level = level;
        this.m = m;
    }

    public Instruction(){
        opCode = -1;
        level = -1;
        m = -1;
    }

    public void setOpCode(int opCode) {
        this.opCode = opCode;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setM(int m) {
        this.m = m;
    }

    private int opCode, level, m;

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
