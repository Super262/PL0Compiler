import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class LexicalAnalyzer {
    private final String inputFile;
    private final ArrayList<Token> lexList;

    public LexicalAnalyzer(String inputFile) {
        this.inputFile = inputFile;
        this.lexList = new ArrayList<>();
    }

    public ArrayList<Token> analyze() throws IOException {

        FileReader fileReader=new FileReader(this.inputFile);

        //Variable Declarations
        int lexListIndex = 0;

        //for holding errors
        int errorHolder = 0;

        //For comments
        int comments;

        //Looks ahead at next character read in
        int lookAhead;

        //Variable to hold each character read in
        int c = fileReader.read();

        //Ignores spaces, tabs, and newlines as known as whitespace
        while(c != -1){
            if(c == (int) ' '|| c == (int) '\t'|| c == (int) '\r' || c == (int) '\n'){
                c = fileReader.read();
                continue;
            }

            //reads if the next character is part of the alphabet
            if(Character.isLowerCase((char) c) || Character.isUpperCase((char) c)){
                StringBuilder characterString = new StringBuilder();

                int index = 0;
                characterString.append((char) c);

                ++index;
                lookAhead = 1;

                //Error Checking: if the variable id is too long
                while((c = fileReader.read()) != -1 && (
                        Character.isLowerCase((char) c) ||
                                Character.isUpperCase((char) c) ||
                                Character.isDigit((char) c)
                )){
                    if(index > Config.MAX_ID_LENGTH - 1){
                        while ((c = fileReader.read()) != -1 &&(
                                Character.isLowerCase((char) c) ||
                                Character.isUpperCase((char) c) ||
                                Character.isDigit((char) c)))
                        {}
                        errorHolder = 1;
                        break;
                    }
                    characterString.append((char) c);
                    ++index;
                }

                //If there was an error, continue without accepting token
                if(errorHolder == 1) {
                    errorHolder = 0;
                    continue;
                }

                //Compares the variable id to see if it is one of the reserved words
                int reservedSwitch = -1;

                for(int i = 0; i < Config.reservedWords.length; ++i){
                    if(Config.reservedWords[i].equals(characterString.toString())){
                        reservedSwitch=i;
                        break;
                    }
                }

                // If it is a reserved word, print out the correct tokenType
                checkLexListBound(lexListIndex);
                switch(reservedSwitch){

                    //Case for const
                    case 0:
                        lexList.get(lexListIndex).setSym(TokenType.constsym);
                        break;

                    //Case for var
                    case 1:
                        lexList.get(lexListIndex).setSym(TokenType.varsym);
                        break;

                    //Case for procedure
                    case 2:
                        lexList.get(lexListIndex).setSym(TokenType.procsym);
                        break;

                    //Case for call
                    case 3:
                        lexList.get(lexListIndex).setSym(TokenType.callsym);
                        break;
                    //Case for begin
                    case 4:
                        lexList.get(lexListIndex).setSym(TokenType.beginsym);
                        break;

                    //Case for end
                    case 5:
                        lexList.get(lexListIndex).setSym(TokenType.endsym);
                        break;

                    //Case for if
                    case 6:
                        lexList.get(lexListIndex).setSym(TokenType.ifsym);
                        break;

                    //Case for then
                    case 7:
                        lexList.get(lexListIndex).setSym(TokenType.thensym);
                        break;

                    //Case for else
                    case 8:
                        lexList.get(lexListIndex).setSym(TokenType.elsesym);
                        break;

                    //Case for while
                    case 9:
                        lexList.get(lexListIndex).setSym(TokenType.whilesym);
                        break;

                    //Case for do
                    case 10:
                        lexList.get(lexListIndex).setSym(TokenType.dosym);
                        break;

                    //Case for read
                    case 11:
                        lexList.get(lexListIndex).setSym(TokenType.readsym);
                        break;

                    //Case for write
                    case 12:
                        lexList.get(lexListIndex).setSym(TokenType.writesym);
                        break;

                    //Case for odd
                    case 13:
                        lexList.get(lexListIndex).setSym(TokenType.oddsym);
                        break;

                    default:
                        lexList.get(lexListIndex).setSym(TokenType.identsym);
                        lexList.get(lexListIndex).setId(characterString.toString());
                        break;
                }
                lexListIndex++;
            }

            //reads if the next character is part of the 0-9 digits
            else if(Character.isDigit((char) c)){
                int number = c - '0';
                int place = 1;

                lookAhead = 1;


                while((c = fileReader.read()) != -1 && Character.isDigit((char) c)){
                    //Error checking: if the number is too long
                    if(place > Config.MAX_NUM_LENGTH - 1){
                        while ((c = fileReader.read()) != -1 && Character.isDigit((char) c)) {}
                        errorHolder = 1;
                        break;
                    }
                    number = 10 * number + (c - '0');
                    ++place;
                }

                // Error: if the variable starts with a digit, and not a number
                if(Character.isLowerCase((char) c) || Character.isUpperCase((char) c)){
                    while ((c = fileReader.read()) != -1 &&
                            (
                                    Character.isLowerCase((char) c) ||
                                    Character.isUpperCase((char) c) ||
                                    Character.isDigit((char) c)
                            )
                    ) {}
                    continue;
                }

                //If there was an error, continue without accepting token
                if(errorHolder == 1) {
                    errorHolder = 0;
                    continue;
                }

                checkLexListBound(lexListIndex);
                lexList.get(lexListIndex).setSym(TokenType.numbersym);
                lexList.get(lexListIndex).setNum(number);
                lexListIndex++;
            }

            //reads if the next character is part of the special symbols
            else {
                lookAhead = 0;
                int spec = -1;
                for(int i = 0; i < Config.specialSymbols.length; ++i){
                    if(((char) c) == Config.specialSymbols[i]){
                        spec = i;
                    }
                }

                //If it is a special symbol, print out the correct token type
                checkLexListBound(lexListIndex);
                switch(spec){

                    //Case for +
                    case 0: {
                        lexList.get(lexListIndex).setSym(TokenType.plussym);
                        lexListIndex++;
                        break;
                    }

                    //Case for -
                    case 1: {
                        lexList.get(lexListIndex).setSym(TokenType.minussym);
                        lexListIndex++;
                        break;
                    }

                    //Case for *
                    case 2: {
                        lexList.get(lexListIndex).setSym(TokenType.multsym);
                        lexListIndex++;
                        break;
                    }

                    //Case for comments
                    case 3: {
                        c = fileReader.read();
                        lookAhead = 1;
                        if(c == '*'){
                            comments = 1;
                            lookAhead = 0;
                            c = fileReader.read();
                            while(comments == 1){
                                if(c == '*'){
                                    c = fileReader.read();
                                    if(c =='/'){
                                        comments = 0;
                                    }
                                }
                                else{
                                    c = fileReader.read();
                                }
                            }
                        }
                        else{
                            lexList.get(lexListIndex).setSym(TokenType.slashsym);
                            lexListIndex++;
                        }
                        break;
                    }

                    //Case for (
                    case 4: {
                        lexList.get(lexListIndex).setSym(TokenType.lparentsym);
                        lexListIndex++;
                        break;
                    }

                    //Case for )
                    case 5: {
                        lexList.get(lexListIndex).setSym(TokenType.rparentsym);
                        lexListIndex++;
                        break;
                    }

                    //Case for =
                    case 6: {
                        lexList.get(lexListIndex).setSym(TokenType.eqsym);
                        lexListIndex++;
                        break;
                    }

                    //Case for ,
                    case 7: {
                        lexList.get(lexListIndex).setSym(TokenType.commasym);
                        lexListIndex++;
                        break;
                    }

                    //Case for .
                    case 8: {
                        lexList.get(lexListIndex).setSym(TokenType.periodsym);
                        lexListIndex++;
                        break;
                    }

                    //Case for <
                    case 9: {
                        c = fileReader.read();
                        lookAhead = 1;

                        //Case for <=
                        if(c=='='){
                            lexList.get(lexListIndex).setSym(TokenType.leqsym);
                            lookAhead = 0;
                        }

                        //Case for <
                        else{
                            lexList.get(lexListIndex).setSym(TokenType.lessym);
                        }

                        lexListIndex++;
                        break;
                    }

                    //Case for >=
                    case 10: {
                        c = fileReader.read();
                        lookAhead = 1;
                        if (c == '=') {
                            lexList.get(lexListIndex).setSym(TokenType.geqsym);
                            lookAhead = 0;
                        }

                        //Case for >
                        else {
                            lexList.get(lexListIndex).setSym(TokenType.gtrsym);
                        }

                        lexListIndex++;
                        break;
                    }

                    //Case for ;
                    case 11: {
                        lexList.get(lexListIndex).setSym(TokenType.semicolonsym);
                        lexListIndex++;
                        break;
                    }

                    //Case for :=
                    case 12: {
                        c = fileReader.read();
                        if(c == '='){
                            lexList.get(lexListIndex).setSym(TokenType.becomessym);
                            lexListIndex++;
                        }
                        break;
                    }

                    //Case for #
                    case 13: {
                        lexList.get(lexListIndex).setSym(TokenType.neqsym);
                        lexListIndex++;
                        break;
                    }

                    default:
                        break;
                }
            }
            //if we aren't looking at next character, read in next as a part of a new variable/number/symbol
            if(lookAhead == 0){
                c = fileReader.read();
            }

        }
        return lexList;
    }

    private void checkLexListBound(int lexListIndex){
        while(this.lexList.size() <= lexListIndex){
            this.lexList.add(new Token());
        }
    }
}
