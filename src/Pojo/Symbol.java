package Pojo;

public class Symbol {
    int kind; 		// const = 1, var = 2, proc = 3
    String name;	// id up to 10 chars
    int value; 		// number (ASCII value)
    int level; 		// L level
    int address; 	// M address

    public int getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public int getLevel() {
        return level;
    }

    public int getAddress() {
        return address;
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setAddress(int address) {
        this.address = address;
    }
}
