package bit.minisys.minicc.scanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;

enum DFA_STATE{
    INIT,
    ERROR,
    IDF_1, IDF_2,
    INT_1, INT_2, INT_3, INT_4, INT_5, INT_6,
    INT_SUF_1, INT_SUF_2, INT_SUF_3, INT_SUF_4, INT_SUF_5, INT_SUF_6, INT_SUF_7, INT_SUF_8,
    FLOAT_1, FLOAT_2, FLOAT_3, FLOAT_4, FLOAT_5, FLOAT_6, FLOAT_7, FLOAT_8,
    FLOAT_SUF_1, FLOAT_SUF_2,
    CHAR_1, CHAR_2, CHAR_3, CHAR_4, CHAR_5, CHAR_6, CHAR_7, CHAR_8, CHAR_9, CHAR_10, CHAR_11, CHAR_12,
    STR_1, STR_2, STR_3, STR_4,
    OP_1, OP_2, OP_3, OP_4, OP_5, OP_6, OP_7, OP_8, OP_10, OP_11, OP_12, OP_13, OP_14, OP_16, OP_17, OP_18, OP_19
}

public class MyScanner implements IMiniCCScanner {

    private int lIndex = 0;
    private int cIndex = 0;
    private int totalIndex = -1;
    private int startLIndex = 0;
    private int startCIndex = 0;
    private int startTotalIndex = -1;
    private String strTokens = "";
    private String lexeme;
    private int iTknNum = 0;
    private boolean keep;	//keep current char
    private boolean end;

    private String file_content;

    private ArrayList<String> srcLines;

    private HashSet<String> keywordSet;
    private String[] keywords = {"auto", "break", "case", "char", "const", "continue", "default", "do",
                                "double", "else", "enum", "extern", "float", "for", "goto", "if", "inline",
                                "int", "long", "register", "restrict", "return", "short", "signed",
                                "sizeof", "static", "struct", "switch", "typedef", "union",
                                "unsigned", "void", "volatile", "while"};

    public MyScanner(){
        this.keywordSet = new HashSet<>();
        this.initKeywordSet();
    }

    private void initKeywordSet() {
        Collections.addAll(this.keywordSet, this.keywords);
    }

    private char getNextChar() {
        char c = Character.MAX_VALUE;
        while(true) {
            if(lIndex < this.srcLines.size()) {
                String line = this.srcLines.get(lIndex);
                if(cIndex < line.length()) {
                    c = line.charAt(cIndex);
                    break;
                }else {
                    lIndex++;
                    totalIndex += 1;
                    cIndex = 0;
                }
            }else {
                break;
            }
        }
        if(c == '\u001a') {
            c = Character.MAX_VALUE;
        }
        cIndex ++;
        totalIndex ++;
        return c;
    }

    private boolean isAlpha(char c) { return Character.isAlphabetic(c); }

    private boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    private boolean isAlphaOrDigit(char c) {
        return Character.isLetterOrDigit(c);
    }

    private boolean isOctDigit(char c) {
        return c >= '0' && c <= '7';
    }

    private boolean isHexDigit(char c) { return isDigit(c) || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F'; }

    private boolean isEscape(char c) {
        String escape = "'\"\\?abfnrtv";
        return escape.contains(String.valueOf(c));
    }

    private String genToken(String lexeme, String type) {
        String strToken = "";
        strToken += "[@" + iTknNum + "," + startTotalIndex + ":" + (this.startTotalIndex+lexeme.length()-1);
        strToken += "='" + lexeme + "',<" + type + ">," + (startLIndex+1) + ":" + (startCIndex-1) + "]\n";
        iTknNum ++;
        return strToken;
    }

    private String genToken(String e) {
        String strToken = "";
        strToken += "[@" + iTknNum + "," + (startTotalIndex-1) + ":" + (startTotalIndex-2);
        strToken += "='<" + e + ">',<" + e + ">," + (startLIndex) + ":" + (startCIndex) + "]\n";
        iTknNum ++;
        return strToken;
    }

    private DFA_STATE init(char c) {
        if (c == Character.MAX_VALUE) {
            end = true;
            cIndex = 5;
            strTokens += genToken("EOF");
            return DFA_STATE.INIT;
        } else if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            return DFA_STATE.INIT;
        } else if ((isAlpha(c) || c == '_') && c != 'u' && c != 'U' && c != 'L') {
            lexeme += c;
            return DFA_STATE.IDF_1;
        } else if (isDigit(c) && c != '0') {
            lexeme += c;
            return DFA_STATE.INT_1;
        } else if (c == '0') {
            lexeme += c;
            return DFA_STATE.INT_2;
        } else if (c == '+') {
            lexeme += c;
            return DFA_STATE.OP_1;
        } else if (c == '-') {
            lexeme += c;
            return DFA_STATE.OP_2;
        } else if (c == '=' || c == '^' || c == '*' || c == '/' || c == '!') {
            lexeme += c;
            return DFA_STATE.OP_3;
        } else if (c == '<') {
            lexeme += c;
            return DFA_STATE.OP_4;
        } else if (c == '>') {
            lexeme += c;
            return DFA_STATE.OP_5;
        } else if (c == '%') {
            lexeme += c;
            return DFA_STATE.OP_6;
        } else if (c == ':') {
            lexeme += c;
            return DFA_STATE.OP_10;
        } else if (c == '&') {
            lexeme += c;
            return DFA_STATE.OP_12;
        } else if (c == '.') {
            lexeme += c;
            return DFA_STATE.OP_13;
        } else if (c == '|') {
            lexeme += c;
            return DFA_STATE.OP_16;
        } else if (c == '#') {
            lexeme += c;
            return DFA_STATE.OP_17;
        } else if ("[](){}?;~,".contains(String.valueOf(c))) {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else if (c == '\'') {
            lexeme += c;
            return DFA_STATE.CHAR_3;
        } else if (c == 'u') {
            lexeme += c;
            return DFA_STATE.CHAR_1;
        } else if (c == 'U' || c == 'L') {
            lexeme += c;
            return DFA_STATE.CHAR_2;
        } else if (c == '"') {
            lexeme += c;
            return DFA_STATE.STR_2;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE idf_1(char c) {
        if (isAlphaOrDigit(c) || c == '_') {
            lexeme += c;
            return DFA_STATE.IDF_1;
        } else {
            keep = true;
            return DFA_STATE.IDF_2;
        }
    }
    private DFA_STATE idf_2() {
        if (this.keywordSet.contains(lexeme)) {
            strTokens += genToken(lexeme, "'" + lexeme + "'");
        } else {
            strTokens += genToken(lexeme, "Identifier");
        }
        keep = true;
        return DFA_STATE.INIT;
    }
    private DFA_STATE int_1(char c) {
        if (isDigit(c)) {
            lexeme += c;
            return DFA_STATE.INT_1;
        } else if (c == '.') {
            lexeme += c;
            return DFA_STATE.FLOAT_1;
        } else if (c == 'e' || c == 'E') {
            lexeme += c;
            return DFA_STATE.FLOAT_2;
        } else {
            keep = true;
            return DFA_STATE.INT_6;
        }
    }
    private DFA_STATE int_2(char c) {
        if (isOctDigit(c)) {
            lexeme += c;
            return DFA_STATE.INT_4;
        } else if (c == 'x' || c == 'X') {
            lexeme += c;
            return DFA_STATE.INT_3;
        } else if (c == '.') {
            lexeme += c;
            return DFA_STATE.FLOAT_1;
        } else if (c == 'e' || c == 'E') {
            lexeme += c;
            return DFA_STATE.FLOAT_2;
        } else {
            keep = true;
            return DFA_STATE.INT_6;
        }
    }
    private DFA_STATE int_3(char c) {
        if (isHexDigit(c)) {
            lexeme += c;
            return DFA_STATE.INT_5;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE int_4(char c) {
        if (isOctDigit(c)) {
            lexeme += c;
            return DFA_STATE.INT_4;
        } else {
            keep = true;
            return DFA_STATE.INT_6;
        }
    }
    private DFA_STATE int_5(char c) {
        if (isHexDigit(c)) {
            lexeme += c;
            return DFA_STATE.INT_5;
        } else if (c == '.') {
            lexeme += c;
            return DFA_STATE.FLOAT_7;
        } else if (c == 'p' || c == 'P') {
            lexeme += c;
            return DFA_STATE.FLOAT_2;
        } else {
            keep = true;
            return DFA_STATE.INT_6;
        }
    }
    private DFA_STATE int_6(char c) {
        if (c == 'u' || c == 'U') {
            lexeme += c;
            return DFA_STATE.INT_SUF_1;
        } else if (c == 'l') {
            lexeme += c;
            return DFA_STATE.INT_SUF_2;
        } else if (c == 'L') {
            lexeme += c;
            return DFA_STATE.INT_SUF_3;
        } else {
            keep = true;
            return DFA_STATE.INT_SUF_8;
        }
    }
    private DFA_STATE int_suf_1(char c) {
        if (c == 'l') {
            lexeme += c;
            return DFA_STATE.INT_SUF_4;
        } else {
            keep = true;
            return DFA_STATE.INT_SUF_8;
        }
    }
    private DFA_STATE int_suf_2(char c) {
        if (c == 'u' || c == 'U') {
            lexeme += c;
            return DFA_STATE.INT_SUF_7;
        } else if (c == 'l') {
            lexeme += c;
            return DFA_STATE.INT_SUF_5;
        } else {
            keep = true;
            return DFA_STATE.INT_SUF_8;
        }
    }
    private DFA_STATE int_suf_3(char c) {
        if (c == 'u' || c == 'U') {
            lexeme += c;
            return DFA_STATE.INT_SUF_7;
        } else if (c == 'L') {
            lexeme += c;
            return DFA_STATE.INT_SUF_5;
        } else {
            keep = true;
            return DFA_STATE.INT_SUF_8;
        }
    }
    private DFA_STATE int_suf_4(char c) {
        if (c == 'l') {
            lexeme += c;
            return DFA_STATE.INT_SUF_7;
        } else {
            keep = true;
            return DFA_STATE.INT_SUF_8;
        }
    }
    private DFA_STATE int_suf_5(char c) {
        if (c == 'u' || c == 'U') {
            lexeme += c;
            return DFA_STATE.INT_SUF_7;
        } else {
            keep = true;
            return DFA_STATE.INT_SUF_8;
        }
    }
    private DFA_STATE int_suf_6(char c) {
        if (c == 'u' || c == 'U') {
            lexeme += c;
            return DFA_STATE.INT_SUF_7;
        } else {
            keep = true;
            return DFA_STATE.INT_SUF_8;
        }
    }
    private DFA_STATE int_suf_7(char c) {
        keep = true;
        return DFA_STATE.INT_SUF_8;
    }
    private DFA_STATE int_suf_8() {
        strTokens += genToken(lexeme, "IntegerConstant");
        keep = true;
        return DFA_STATE.INIT;
    }
    private DFA_STATE float_1(char c) {
        if (isDigit(c)) {
            lexeme += c;
            return DFA_STATE.FLOAT_3;
        } else if (c == 'e' || c == 'E') {
            lexeme += c;
            return DFA_STATE.FLOAT_2;
        } else {
            keep = true;
            return DFA_STATE.FLOAT_6;
        }
    }
    private DFA_STATE float_2(char c) {
        if (isDigit(c)) {
            lexeme += c;
            return DFA_STATE.FLOAT_5;
        } else if (c == '+' || c == '-') {
            lexeme += c;
            return DFA_STATE.FLOAT_4;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE float_3(char c) {
        if (isDigit(c)) {
            lexeme += c;
            return DFA_STATE.FLOAT_3;
        } else if (c == 'e' || c == 'E') {
            lexeme += c;
            return DFA_STATE.FLOAT_2;
        } else {
            keep = true;
            return DFA_STATE.FLOAT_6;
        }
    }
    private DFA_STATE float_4(char c) {
        if (isDigit(c)) {
            lexeme += c;
            return DFA_STATE.FLOAT_5;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE float_5(char c) {
        keep = true;
        return DFA_STATE.FLOAT_6;
    }
    private DFA_STATE float_6(char c) {
        if (c == 'f' || c == 'F' || c == 'l' || c == 'L') {
            lexeme += c;
            return DFA_STATE.FLOAT_SUF_1;
        } else {
            keep = true;
            return DFA_STATE.FLOAT_SUF_2;
        }
    }
    private DFA_STATE float_7(char c) {
        if (isHexDigit(c)) {
            lexeme += c;
            return DFA_STATE.FLOAT_8;
        } else if (c == 'p' || c == 'P') {
            lexeme += c;
            return DFA_STATE.FLOAT_2;
        } else {
            keep = true;
            return DFA_STATE.FLOAT_6;
        }
    }
    private DFA_STATE float_8(char c) {
        if (isHexDigit(c)) {
            lexeme += c;
            return DFA_STATE.FLOAT_8;
        } else if (c == 'p' || c == 'P') {
            lexeme += c;
            return DFA_STATE.FLOAT_2;
        } else {
            keep = true;
            return DFA_STATE.FLOAT_6;
        }
    }
    private DFA_STATE float_suf_1(char c) {
        keep = true;
        return DFA_STATE.FLOAT_SUF_2;
    }
    private DFA_STATE float_suf_2() {
        strTokens += genToken(lexeme, "FloatingConstant");
        keep = true;
        return DFA_STATE.INIT;
    }
    private DFA_STATE char_1(char c) {
        if (c == '8') {
            lexeme += c;
            return DFA_STATE.STR_1;
        } else if (isAlphaOrDigit(c) || c == '_') {
            lexeme += c;
            return DFA_STATE.IDF_1;
        } else if (c == '\'') {
            lexeme += c;
            return DFA_STATE.CHAR_3;
        } else if (c == '"') {
            lexeme += c;
            return DFA_STATE.STR_2;
        } else {
            keep = true;
            return DFA_STATE.IDF_2;
        }
    }
    private DFA_STATE char_2(char c) {
        if (isAlphaOrDigit(c) || c == '_') {
            lexeme += c;
            return DFA_STATE.IDF_1;
        } else if (c == '\'') {
            lexeme += c;
            return DFA_STATE.CHAR_3;
        } else if (c == '"') {
            lexeme += c;
            return DFA_STATE.STR_2;
        } else {
            keep = true;
            return DFA_STATE.IDF_2;
        }
    }
    private DFA_STATE char_3(char c) {
        if (c == '\\') {
            lexeme += c;
            return DFA_STATE.CHAR_7;
        } else {
            lexeme += c;
            return DFA_STATE.CHAR_4;
        }
    }
    private DFA_STATE char_4(char c) {
        if (c == '\'') {
            lexeme += c;
            return DFA_STATE.CHAR_5;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE char_5(char c) {
        keep = true;
        return DFA_STATE.CHAR_6;
    }
    private DFA_STATE char_6() {
        strTokens += genToken(lexeme, "CharacterConstant");
        keep = true;
        return DFA_STATE.INIT;
    }
    private DFA_STATE char_7(char c) {
        if (c == 'x') {
            lexeme += c;
            return DFA_STATE.CHAR_11;
        } else if (isOctDigit(c)) {
            lexeme += c;
            return DFA_STATE.CHAR_8;
        } else if (isEscape(c)) {
            lexeme += c;
            return DFA_STATE.CHAR_4;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE char_8(char c) {
        if (c == '\'') {
            lexeme += c;
            return DFA_STATE.CHAR_5;
        } else if (isOctDigit(c)) {
            lexeme += c;
            return DFA_STATE.CHAR_9;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE char_9(char c) {
        if (c == '\'') {
            lexeme += c;
            return DFA_STATE.CHAR_5;
        } else if (isOctDigit(c)) {
            lexeme += c;
            return DFA_STATE.CHAR_10;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE char_10(char c) {
        if (c == '\'') {
            lexeme += c;
            return DFA_STATE.CHAR_5;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE char_11(char c) {
        if (isHexDigit(c)) {
            lexeme += c;
            return DFA_STATE.CHAR_12;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE char_12(char c) {
        if (isHexDigit(c)) {
            lexeme += c;
            return DFA_STATE.CHAR_12;
        } else if (c == '\'') {
            lexeme += c;
            return DFA_STATE.CHAR_5;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE str_1(char c) {
        if (isAlphaOrDigit(c) || c == '_') {
            lexeme += c;
            return DFA_STATE.IDF_1;
        } else if (c == '"') {
            lexeme += c;
            return DFA_STATE.STR_2;
        } else {
            keep = true;
            return DFA_STATE.IDF_2;
        }
    }
    private DFA_STATE str_2(char c) {
        if (c == '"') {
            lexeme += c;
            return DFA_STATE.STR_4;
        } else if (c == '\\') {
            lexeme += c;
            return DFA_STATE.STR_3;
        } else {
            lexeme += c;
            return DFA_STATE.STR_2;
        }
    }
    private DFA_STATE str_3(char c) {
        if (isEscape(c)) {
            lexeme += c;
            return DFA_STATE.STR_2;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE str_4() {
        strTokens += genToken(lexeme, "StringLiteral");
        keep = true;
        return DFA_STATE.INIT;
    }
    private DFA_STATE op_1(char c) {
        if (c == '+' || c == '=') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_2(char c) {
        if (c == '-' || c == '=' || c == '>') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_3(char c) {
        if (c == '=') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_4(char c) {
        if (c == ':' || c == '=' || c == '%') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else if (c == '<') {
            lexeme += c;
            return DFA_STATE.OP_11;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_5(char c) {
        if (c == '=') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else if (c == '>') {
            lexeme += c;
            return DFA_STATE.OP_11;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_6(char c) {
        if (c == '>' || c == '=') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else if (c == ':') {
            lexeme += c;
            return DFA_STATE.OP_7;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_7(char c) {
        if (c == '%') {
            lexeme += c;
            return DFA_STATE.OP_8;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_8(char c) {
        if (c == ':') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE op_10(char c) {
        if (c == '>') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_11(char c) {
        if (c == '=') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_12(char c) {
        if (c == '&' || c == '=') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_13(char c) {
        if (c == '.') {
            lexeme += c;
            return DFA_STATE.OP_14;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_14(char c) {
        if (c == '.') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else {
            return DFA_STATE.ERROR;
        }
    }
    private DFA_STATE op_16(char c) {
        if (c == '|' || c == '=') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_17(char c) {
        if (c == '#') {
            lexeme += c;
            return DFA_STATE.OP_18;
        } else {
            keep = true;
            return DFA_STATE.OP_19;
        }
    }
    private DFA_STATE op_18(char c) {
        keep = true;
        return DFA_STATE.OP_19;
    }
    private DFA_STATE op_19() {
        strTokens += genToken(lexeme, "'" + lexeme + "'");
        keep = true;
        return DFA_STATE.INIT;
    }
    private void err(char c) {
        System.out.println("[ERROR]Scanner:line=" + lIndex + ", column=" + cIndex + ", char=" + c + ", index=" + (int)c + ", unreachable state!");
    }

    @Override
    public String run(String iFile) throws Exception {
        System.out.println("Scanning...");

        this.srcLines = MiniCCUtil.readFile(iFile);

        DFA_STATE state = DFA_STATE.INIT;

        lexeme = "";		//token lexeme
        char c = ' ';		//next char
        keep = false;	    //keep current char
        end = false;

        while(!end) {				//scanning loop
            if(!keep) {
                c = getNextChar();
            }

            keep = false;

            switch(state) {
                case INIT:
                    startCIndex = cIndex;
                    startLIndex = lIndex;
                    startTotalIndex = totalIndex;
                    lexeme = "";
                    state = init(c);
                    break;
                case ERROR:
                    err(c);
                    end = true;
                    break;
                case IDF_1:
                    state = idf_1(c);
                    break;
                case IDF_2:
                    state = idf_2();
                    break;
                case INT_1:
                    state = int_1(c);
                    break;
                case INT_2:
                    state = int_2(c);
                    break;
                case INT_3:
                    state = int_3(c);
                    break;
                case INT_4:
                    state = int_4(c);
                    break;
                case INT_5:
                    state = int_5(c);
                    break;
                case INT_6:
                    state = int_6(c);
                    break;
                case INT_SUF_1:
                    state = int_suf_1(c);
                    break;
                case INT_SUF_2:
                    state = int_suf_2(c);
                    break;
                case INT_SUF_3:
                    state = int_suf_3(c);
                    break;
                case INT_SUF_4:
                    state = int_suf_4(c);
                    break;
                case INT_SUF_5:
                    state = int_suf_5(c);
                    break;
                case INT_SUF_6:
                    state = int_suf_6(c);
                    break;
                case INT_SUF_7:
                    state = int_suf_7(c);
                    break;
                case INT_SUF_8:
                    state = int_suf_8();
                    break;
                case FLOAT_1:
                    state = float_1(c);
                    break;
                case FLOAT_2:
                    state = float_2(c);
                    break;
                case FLOAT_3:
                    state = float_3(c);
                    break;
                case FLOAT_4:
                    state = float_4(c);
                    break;
                case FLOAT_5:
                    state = float_5(c);
                    break;
                case FLOAT_6:
                    state = float_6(c);
                    break;
                case FLOAT_7:
                    state = float_7(c);
                    break;
                case FLOAT_8:
                    state = float_8(c);
                    break;
                case FLOAT_SUF_1:
                    state = float_suf_1(c);
                    break;
                case FLOAT_SUF_2:
                    state = float_suf_2();
                    break;
                case CHAR_1:
                    state = char_1(c);
                    break;
                case CHAR_2:
                    state = char_2(c);
                    break;
                case CHAR_3:
                    state = char_3(c);
                    break;
                case CHAR_4:
                    state = char_4(c);
                    break;
                case CHAR_5:
                    state = char_5(c);
                    break;
                case CHAR_6:
                    state = char_6();
                    break;
                case CHAR_7:
                    state = char_7(c);
                    break;
                case CHAR_8:
                    state = char_8(c);
                    break;
                case CHAR_9:
                    state = char_9(c);
                    break;
                case CHAR_10:
                    state = char_10(c);
                    break;
                case CHAR_11:
                    state = char_11(c);
                    break;
                case CHAR_12:
                    state = char_12(c);
                    break;
                case STR_1:
                    state = str_1(c);
                    break;
                case STR_2:
                    state = str_2(c);
                    break;
                case STR_3:
                    state = str_3(c);
                    break;
                case STR_4:
                    state = str_4();
                    break;
                case OP_1:
                    state = op_1(c);
                    break;
                case OP_2:
                    state = op_2(c);
                    break;
                case OP_3:
                    state = op_3(c);
                    break;
                case OP_4:
                    state = op_4(c);
                    break;
                case OP_5:
                    state = op_5(c);
                    break;
                case OP_6:
                    state = op_6(c);
                    break;
                case OP_7:
                    state = op_7(c);
                    break;
                case OP_8:
                    state = op_8(c);
                    break;
                case OP_10:
                    state = op_10(c);
                    break;
                case OP_11:
                    state = op_11(c);
                    break;
                case OP_12:
                    state = op_12(c);
                    break;
                case OP_13:
                    state = op_13(c);
                    break;
                case OP_14:
                    state = op_14(c);
                    break;
                case OP_16:
                    state = op_16(c);
                    break;
                case OP_17:
                    state = op_17(c);
                    break;
                case OP_18:
                    state = op_18(c);
                    break;
                case OP_19:
                    state = op_19();
                    break;
                default:
                    keep = true;
                    state = DFA_STATE.ERROR;
                    break;
            }
        }

        String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_SCANNER_OUTPUT_EXT;
        MiniCCUtil.createAndWriteFile(oFile, strTokens);

        return oFile;
    }
}
