package Core;

import Pojo.Instruction;
import Pojo.MutableInt;
import Pojo.Symbol;
import Pojo.Token;
import Property.Configuration;
import Property.TokenType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static Property.TokenType.*;

public class CodeGenerator {
    private final ArrayList<Token> lexList;
    private final File outputFile;

    //Global temporary variables defined for all functions
    private int codeIndex;
    private int numOfSymbol;
    private int kindOfSymbol;
    private int lexemeListIndex = 0;
    private int difference;
    private int previousDifference = 0;
    private TokenType tokenType;
    private String id;

    public CodeGenerator(ArrayList<Token> lexList, String outputPath) {
        this.lexList = lexList;
        this.outputFile = new File(outputPath);
    }

    public void parse() throws IOException {
        // array of instructions, called instructionArray, which holds the instructions formed in parser from the lexList,
        // which will be passed to the vm.
        final ArrayList<Instruction> instructionArray = new ArrayList<>();

        // Declaring symbol_table array of symbol structs
        //symbol table needs to be 2D array of size [max][4] because max lex level is 3, so 0,1,2,3 is size[4]
        final ArrayList<Symbol> symbolTable = new ArrayList<>();

        // start program
        program(symbolTable, instructionArray);

        // prints instructions to file
        StringBuilder stringBuilder = new StringBuilder();
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
        for (Instruction code : instructionArray) {
            stringBuilder.append(code.getOpCode()).append(" ");
            stringBuilder.append(code.getLevel()).append(" ");
            stringBuilder.append(code.getM()).append(" ");
            stringBuilder.append("\n");
        }
        bw.write(stringBuilder.toString());
        bw.close();
    }

    private void program(ArrayList<Symbol> table, ArrayList<Instruction> code) {
        tokenType = getNextToken();
        block(0,0, table, code);
        if (tokenType != periodsym) {
            error(9); //Period expected.
        }
    }

    private void block(int lev, int tx, ArrayList<Symbol> table, ArrayList<Instruction> code) {

        //errors for above max lexi level
        if(lev > Configuration.MAX_LEXI_LEVELS) {
            error(26);
        }

        int dx, tx0;
        // this changes the amount in M for INC calls, so if dx=3; we will only create space for sp,bp,pc, which throws off vm.c,
        // but if dx=4; meaning we will create space for sp,bp,pc,and retrun value, which makes vm.c work properly.
        dx = 4;
        tx0 = tx;
        updateSymbolTableBound(table, tx);
        emit(7,0,0, code); // 7 is JMP for op, 0 is for L and 0 for M

        do {
            if (tokenType == constsym) {
                tokenType = getNextToken();
                do {
                    constDeclaration(lev, new MutableInt(tx), new MutableInt(dx), table);
                    while(tokenType == commasym) {
                        tokenType = getNextToken();
                        constDeclaration(lev, new MutableInt(tx), new MutableInt(dx), table);
                    }
                    if(tokenType == semicolonsym) {
                        tokenType = getNextToken();
                    }
                    else {
                        error(5); //Semicolon or comma missing.
                    }
                } while (tokenType == identsym);
            }
            if (tokenType == varsym) {
                tokenType = getNextToken();
                do {
                    varDeclaration(lev, new MutableInt(tx), new MutableInt(dx), table);
                    while (tokenType == commasym) {
                        tokenType = getNextToken();
                        varDeclaration(lev, new MutableInt(tx), new MutableInt(dx), table);
                    }
                    if(tokenType == semicolonsym) {
                        tokenType = getNextToken();
                    }
                    else {
                        error(5); //Semicolon or comma missing.
                    }
                } while(tokenType == identsym);
            }
            while(tokenType == procsym) {
                tokenType = getNextToken();

                // get identsym, then store it in symbol table, need to store, kindOfSymbol, level, id, dont care about addr.

                // store symbol in the symbol table to fix it later, you need to go back and fix address.
                // you don't have enough info to generate code so you need to store it for later
                if(tokenType == identsym) {
                    enter(3, new MutableInt(tx), new MutableInt(dx), lev, table); //procedure
                    tokenType = getNextToken();
                }
                else {
                    error(4); //const, var, procedure must be followed by identifier.
                }
                if(tokenType == semicolonsym) {
                    tokenType = getNextToken();
                }
                else {
                    error(5); //Semicolon or comma missing.
                }
                //lev++; call lev in block after incrementing
                block(lev+1, tx, table, code); //Go to a block one level higher
                if(tokenType == semicolonsym) {
                    tokenType = getNextToken();
                }
                else {
                    error(5); //Semicolon or comma missing.
                }
            }
        }while(tokenType == constsym || tokenType == varsym);

        //The tentative jump address is fixed up
        updateInstructionArrayBound(code, table.get(tx0).getAddress());
        code.get(table.get(tx0).getAddress()).setM(codeIndex);

        //the space for address for the above jmp is now occupied by the new codeIndex
        table.get(tx0).setAddress(codeIndex);

        //inc 0, dx is generated. At run time, the space of dx is secured

        emit(6, 0, dx, code); // 6 is INC for op, 0 is for L, and dx is M
        statement(lev, new MutableInt(tx), code, table);
        emit(2, 0, 0, code); // 2 is OPR for op, 0 is RET for M inside OPR
    }

    private void constDeclaration(int lev, MutableInt ptx, MutableInt pdx, ArrayList<Symbol> table) {

        if (tokenType == identsym) {
            tokenType = getNextToken();
            if ((tokenType == eqsym) || (tokenType == becomessym)) {
                if (tokenType == becomessym) {
                    error(1); // Use = instead of :=
                }
                tokenType = getNextToken();
                if (tokenType == numbersym) {
                    enter(1,ptx,pdx,lev, table); // const
                    tokenType = getNextToken();
                }
            }
        }
    }

    private void varDeclaration(int lev, MutableInt ptx, MutableInt pdx, ArrayList<Symbol> table) {

        if (tokenType == identsym) {
            enter(2,ptx,pdx,lev, table); // var
            tokenType = getNextToken();
        }
        else error(4); //const, var, procedure must be followed by identifier.
    }

    private void statement(int lev, MutableInt ptx, ArrayList<Instruction> code, ArrayList<Symbol> table) {

        int i, cx1, cx2;

        if (tokenType == identsym){
            i = position(id,ptx.getValue(), table, lev);
            if(i == 0) {
                error(11); //Undeclared identifier.
            }
            else if (table.get(i).getKind() != 2) { //var
                error(12); //Assignment to constant or procedure is not allowed
                i = 0;
            }
            tokenType = getNextToken();
            if (tokenType == becomessym) {
                tokenType = getNextToken();
            }
            else {
                error(13); //Assignment operator expected.
            }
            expression(lev, ptx, code, table);
            if (i != 0) {
                // 4 is STO for op, lev-table[i].level is for L, table[i].adr for M
                emit(4, lev - table.get(i).getLevel(), table.get(i).getAddress(), code);
            }
        }
        else if (tokenType == callsym) {
            tokenType = getNextToken();
            if (tokenType != identsym) {
                error(14); //call must be followed by an identifier
            }
            else {
                i = position(id, ptx.getValue(), table, lev);
                if(i == 0) {
                    error(11); //Undeclared identifier.
                }
                else if (table.get(i).getKind() == 3) {//proc
                    // 5 is CAL for op, lev-table[i].level is for L, table[i].adr for M
                    emit(5,lev - table.get(i).getLevel(), table.get(i).getAddress(), code);
                    //statement::= ["call" ident | ...]
                }
                else {
                    error(15); //Call of a constant or variable is meaningless
                }
                tokenType = getNextToken();
            }
        }

        //if <condition> then <statement>
        else if (tokenType == ifsym) {
            tokenType = getNextToken();
            condition(lev, ptx, code, table);
            if(tokenType == thensym) {
                tokenType = getNextToken();
            }
            else {
                error(16);  // then expected
            }

            cx1 = codeIndex;
            emit(8, 0, 0, code); // 8 is JPC for op, 0 is for L and 0 for M
            statement(lev, ptx, code, table);

            if(tokenType == elsesym) {
                tokenType = getNextToken();
                updateInstructionArrayBound(code, cx1);
                code.get(cx1).setM(codeIndex + 1); //jumps past if
                cx1 = codeIndex;
                emit(7, 0, 0, code); // 7 is JMP for op, 0 is for L and cx1 for M

                //updates JPC M value
                statement(lev, ptx, code, table);
            }
            updateInstructionArrayBound(code, cx1);
            code.get(cx1).setM(codeIndex); //jumps past else (if theres an else statement) otherwise jumps past if
        }

        //begin <condition> end <statement>
        else if (tokenType == beginsym) {
            tokenType = getNextToken();
            statement(lev, ptx, code, table);

            while (tokenType == semicolonsym) {
                tokenType = getNextToken();
                statement(lev, ptx, code, table);
            }

            if (tokenType == endsym) {
                tokenType = getNextToken();
            }
            else {
                error(17);  //Semicolon or } expected.
            }
        }

        //while <condition> do <statement>
        else if (tokenType == whilesym) {
            cx1 =codeIndex;
            tokenType = getNextToken();
            condition(lev,ptx, code, table);
            cx2 = codeIndex;
            emit(8, 0, 0, code); // 8 is JPC for op, 0 is for L and 0 for M
            if(tokenType == dosym) {
                tokenType = getNextToken();
            }
            else {
                error(18);  // do expected
            }
            statement(lev, ptx, code, table);
            emit(7, 0, cx1, code); // 7 is JMP for op, 0 is for L and cx1 for M, jump to instruction cx1
            updateInstructionArrayBound(code, cx2);
            code.get(cx2).setM(codeIndex);
        }

        //write needs to write
        else if (tokenType == writesym) {
            tokenType = getNextToken();
            expression(lev, ptx, code, table);
            emit(9,0,1, code); // 9 is SIO1 for op, 0 is for L and 1 for M, write the top stack element to the screen
        }

        //read needs to read and STO
        else if (tokenType == readsym) {
            tokenType = getNextToken();
            emit(10,0,2, code); // 10 is SIO2 for op, 0 is for L and 1 for M, write the top stack element to the screen
            i = position(id,ptx.getValue(), table, lev);
            if(i == 0) {
                error(11); //Undeclared identifier.
            }
            else if (table.get(i).getKind() != 2) { //var
                error(12); //Assignment to constant or procedure is not allowed
                i = 0;
            }
            if (i != 0) {
                emit(4, lev - table.get(i).getLevel(), table.get(i).getAddress(), code); // 4 is STO for op, lev-table[i].level is for L, table[i].adr for M
            }
            tokenType = getNextToken();
        }

    }

    private void condition(int lev, MutableInt ptx, ArrayList<Instruction> code, ArrayList<Symbol> table) {

        TokenType relationSwitch;

        if (tokenType == oddsym) {
            tokenType = getNextToken();
            expression(lev,ptx, code, table);
            emit(2, 0, 6, code); // 2 is OPR for op, 6 is ODD for M inside OPR

        }
        else {
            expression(lev,ptx, code, table);
            if ((tokenType != eqsym) && (tokenType != neqsym) && (tokenType != lessym) &&
                    (tokenType != leqsym) && (tokenType != gtrsym) && (tokenType != geqsym)) {
                error(20); //Relational operator expected.
            }
            else { //for relational operators
                relationSwitch = tokenType;
                tokenType = getNextToken();
                expression(lev, ptx, code, table);
                switch(relationSwitch) {
                    case eqsym:
                        emit(2,0,8, code); // 2 is OPR for op, 8 is EQL for M inside OPR
                        break;
                    case neqsym:
                        emit(2,0,9, code); // 2 is OPR for op, 9 is NEQ for M inside OPR
                        break;
                    case lessym:
                        emit(2,0,10, code); // 2 is OPR for op, 10 is LSS for M inside OPR
                        break;
                    case leqsym:
                        emit(2,0,11, code); // 2 is OPR for op, 11 is LEQ for M inside OPR
                        break;
                    case gtrsym:
                        emit(2,0,12, code); // 2 is OPR for op, 12 is GTR for M inside OPR
                        break;
                    case geqsym:
                        emit(2,0,13, code); // 2 is OPR for op, 13 is GEQ for M inside OPR
                        break;
                }
            }
        }
    }

    private void expression(int lev, MutableInt ptx, ArrayList<Instruction> code, ArrayList<Symbol> table) {

        TokenType addop;
        if (tokenType == plussym || tokenType == minussym) {
            addop = tokenType;
            tokenType = getNextToken();
            term(lev, ptx, code, table);
            if(addop == minussym) {
                emit(2, 0, 1, code); // 2 is OPR for op, 1 is NEG for M inside OPR
            }
        }
        else {
            term (lev, ptx, code, table);
        }
        while (tokenType == plussym || tokenType == minussym) {
            addop = tokenType;
            tokenType = getNextToken();
            term(lev, ptx, code, table);
            if (addop == plussym) {
                emit(2, 0, 2, code); // 2 is OPR for op, 2 is ADD for M inside OPR
            }
            else {
                emit(2, 0, 3, code); // 2 is OPR for op, 3 is SUB for M inside OPR
            }
        }
    }

    private void term(int lev, MutableInt ptx, ArrayList<Instruction> code, ArrayList<Symbol> table) {
        TokenType mulop;
        factor(lev, ptx, table, code);
        while(tokenType == multsym || tokenType == slashsym) {
            mulop = tokenType;
            tokenType = getNextToken();
            factor(lev, ptx, table, code);
            if(mulop == multsym) {
                emit(2, 0, 4, code); // 2 is OPR for op, 4 is MUL for M inside OPR
            }
            else {
                emit(2, 0, 5, code); // 2 is OPR for op, 5 is DIV for M inside OPR
            }
        }
    }

    private void factor(int lev, MutableInt ptx, ArrayList<Symbol> table, ArrayList<Instruction> code) {

        int i, level, adr, val;

        while ((tokenType == identsym) || (tokenType == numbersym) || (tokenType == lparentsym)){
            if (tokenType == identsym) {
                i = position(id, ptx.getValue(), table, lev);
                if (i == 0) {
                    error(11); // undeclared identifier
                }
                else {
                    kindOfSymbol = table.get(i).getKind();
                    level = table.get(i).getLevel();
                    adr = table.get(i).getAddress();
                    val = table.get(i).getValue();

                    if (kindOfSymbol == 1) { //const
                        emit(1,0,val, code); // 1 is LIT for op, val is for M inside LIT
                    }
                    else if (kindOfSymbol == 2) { //var
                        // 3 is LOD for op, lev-level is L inside LOD, adr is for M inside LOD
                        emit(3,lev-level,adr, code);
                    }
                    else {
                        error(21); // Expression must not contain a procedure identifier
                    }
                }
                tokenType = getNextToken();
            }
            /*this might need to be changed*/
            else if(tokenType == numbersym) {
                if (numOfSymbol > Configuration.MAX_ADDRESS) { //maximum address
                    error(25);
                    numOfSymbol = 0;
                }
                emit(1, 0, numOfSymbol, code); // 1 is LIT for op, numOfSymbol is for M inside LIT
                tokenType = getNextToken();
            }
            else {
                tokenType = getNextToken();
                expression(lev,ptx, code, table);
                if (tokenType == rparentsym) {
                    tokenType = getNextToken();
                }
                else {
                    error(22); // Right parenthesis missing.
                }
            }
        }
    }

    private void emit(final int op, final int l, final int m, ArrayList<Instruction> code) {
        if (this.codeIndex > Configuration.MAX_CODE_SIZE){
            System.out.println("Program too long! codeIndex > " + Configuration.MAX_CODE_SIZE);
        }
        else {
            updateInstructionArrayBound(code, this.codeIndex);
            code.get(this.codeIndex).setOpCode(op); 	//opcode
            code.get(this.codeIndex).setLevel(l);	// lexicographical level
            code.get(this.codeIndex).setM(m);	// modifier
            this.codeIndex++;
        }
    }

    //This function enters a symbol into the table
    private void enter(final int k, MutableInt ptx, MutableInt pdx, final int level, ArrayList<Symbol> table) {

        ptx.setValue(ptx.getValue() + 1); //table index tx is increased by 1

        updateSymbolTableBound(table, ptx.getValue());
        table.get(ptx.getValue()).setName(this.id); //id is recorded in .id

        //updates kindOfSymbol
        table.get(ptx.getValue()).setKind(k);

        if (k == 1) { // for constants: updates value
            table.get(ptx.getValue()).setValue(this.numOfSymbol);
        }
        else if (k == 2) { // for variables: updates L and M
            table.get(ptx.getValue()).setLevel(level);
            table.get(ptx.getValue()).setAddress(pdx.getValue());
            pdx.setValue(pdx.getValue() + 1);
        }
        else { // for procedures: updates L because M will change
            table.get(ptx.getValue()).setLevel(level);
        }
    }

    //prints out errors
    private void error(final int errorCase) {
        switch (errorCase) {
            case 1:
                System.out.print("Error 1: ");
                System.out.print("Use = instead of :=.\n");
                break;
            case 2:
                System.out.print("Error 2: ");
                System.out.print("= must be followed by a number.\n");
                break;
            case 3:
                System.out.print("Error 3: ");
                System.out.print("Identifier must be followed by =.\n");
                break;
            case 4:
                System.out.print("Error 4: ");
                System.out.print("const, var, procedure must be followed by identifier.\n");
                break;
            case 5:
                System.out.print("Error 5: ");
                System.out.print("Semicolon or comma missing.\n");
                break;
            case 6:
                System.out.print("Error 6: ");
                System.out.print("Incorrect symbol after procedure declaration.\n");
                break;
            case 7:
                System.out.print("Error 7: ");
                System.out.print("Statement expected\n");
                break;
            case 8:
                System.out.print("Error 8: ");
                System.out.print("Incorrect symbol after statement part in block.\n");
                break;
            case 9:
                System.out.print("Error 9: ");
                System.out.print("Period expected.\n");
                break;
            case 10:
                System.out.print("Error 10: ");
                System.out.print("Semicolon between statements missing.\n");
                break;
            case 11:
                System.out.print("Error 11: ");
                System.out.print("Undeclared identifier.\n");
                break;
            case 12:
                System.out.print("Error 12: ");
                System.out.print("Assignment to constant or procedure is not allowed.\n");
                break;
            case 13:
                System.out.print("Error 13: ");
                System.out.print("Assignment operator expected.\n");
                break;
            case 14:
                System.out.print("Error 14: ");
                System.out.print("call must be followed by an identifier\n");
                break;
            case 15:
                System.out.print("Error 15: ");
                System.out.print("Call of a constant or variable is meaningless.\n");
                break;
            case 16:
                System.out.print("Error 16: ");
                System.out.print("then expected\n");
                break;
            case 17:
                System.out.print("Error 17: ");
                System.out.print("Semicolon or } expected\n");
                break;
            case 18:
                System.out.print("Error 18: ");
                System.out.print("do expected\n");
                break;
            case 19:
                System.out.print("Error 19: ");
                System.out.print("Incorrect symbol following statement.\n");
                break;
            case 20:
                System.out.print("Error 20: ");
                System.out.print("Relational operator expected.\n");
                break;
            case 21:
                System.out.print("Error 21: ");
                System.out.print("Expression must not contain a procedure identifier.\n");
                break;
            case 22:
                System.out.print("Error 22: ");
                System.out.print("Right parenthesis missing.\n");
                break;
            case 23:
                System.out.print("Error 23: ");
                System.out.print("The preceding factor cannot begin with this symbol.\n");
                break;
            case 24:
                System.out.print("Error 24: ");
                System.out.print("An expression cannot begin with this symbol.\n");
                break;
            case 25:
                System.out.print("Error 25: ");
                System.out.print("This number is too large.\n");
                break;
            case 26:
                System.out.print("Error: 26 ");
                System.out.print("Level is larger than the maximum allowed lexicographical levels!\n");
                break;
            default:
                break;
        }

        //stops program when error occurs
        System.exit(1);
    }

    private TokenType getNextToken() {

        this.tokenType = this.lexList.get(this.lexemeListIndex).getSym();

        if(this.tokenType == identsym){
            this.id = this.lexList.get(this.lexemeListIndex).getId();
        }
        else if(this.tokenType == numbersym){
            this.numOfSymbol = this.lexList.get(this.lexemeListIndex).getNum();
        }

        this.lexemeListIndex++;
        return this.tokenType;
    }

    private int position(String id, int tableIndex, final ArrayList<Symbol> table, final int level) {
        int currentS = tableIndex;
        int differenceCount = 0;
        while(tableIndex != 0) {
            if (table.size() > tableIndex && table.get(tableIndex).getName().equals(id)) {

                /*Added in right now, trying to do procedures*/
                // if table[s].level isn't equal to the current level, and the kindOfSymbol is not equal to constant,
                // then we don't have access to this variable in this stack frame
                // maybe table[s].level>lev ? because we can look at levels below, and current level, just not above.

                if(table.get(tableIndex).getLevel() <= level) {

                    if (differenceCount != 0) {
                        this.previousDifference = this.difference;
                    }

                    this.difference = level - table.get(tableIndex).getLevel();

                    if(differenceCount == 0) {
                        currentS = tableIndex;
                    }

                    if (this.difference < this.previousDifference) {
                        currentS = tableIndex;
                    }
                    differenceCount++;
                }
            }
            tableIndex--;
        }
        //minimum
        return currentS;
    }

    private void updateSymbolTableBound(ArrayList<Symbol> table, final int tableIndex){
        while(table.size() <= tableIndex){
            table.add(new Symbol());
        }
    }

    private void updateInstructionArrayBound(ArrayList<Instruction> instructionArray, final int codeIndex){
        while(instructionArray.size() <= codeIndex){
            instructionArray.add(new Instruction());
        }
    }
}
