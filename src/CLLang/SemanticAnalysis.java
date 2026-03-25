package CLLang;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class SemanticAnalysis {
    
    private static Pattern intRe; 
    private static Pattern floatRe;
    private static Pattern stringRe;
    private static Pattern exprRe;

    public SemanticAnalysis(){
        this.intRe = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
        this.floatRe =  Pattern.compile("\\d+.\\d+", Pattern.CASE_INSENSITIVE);
        this.stringRe =  Pattern.compile("\"[\\w | \\s]+\"", Pattern.CASE_INSENSITIVE);
        this.exprRe = Pattern.compile("\\(?[\\w+ | \\d+ | \\s]+\\)?", Pattern.CASE_INSENSITIVE);

    }

    public String infereType(String dataValue) {
        String valType;
        if(intRe.matcher(dataValue).matches()){
            valType = "int";
        }
        else if(floatRe.matcher(dataValue).matches()){
            valType = "float";
        }
        else if(stringRe.matcher(dataValue).matches()){
            valType = "string";
        }
        else {
            valType = "invalid";
        }

        return valType;
    }

}
