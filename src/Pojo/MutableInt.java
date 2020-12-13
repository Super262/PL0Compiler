package Pojo;

public class MutableInt {
    private int value;
    public MutableInt(){
        value = 0;
    }

    public MutableInt(int value){
        this.value = value;
    }

    public int getValue(){
        return value;
    }

    public void setValue(int value){
        this.value = value;
    }
}
