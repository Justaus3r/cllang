package CLLang;

import java.util.regex.Pattern;

public class SemanticAnalysis {
    
    private static Pattern intRe; 
    private static Pattern floatRe;
    private static Pattern stringRe;
    private static Pattern exprRe;

    public SemanticAnalysis(){
        this.intRe = Pattern.compile("\\d+", Pattern.CASE_INSENSITIVE);
        this.floatRe =  Pattern.compile("\\d+\\.\\d+", Pattern.CASE_INSENSITIVE);
        this.stringRe =  Pattern.compile("\"[\\w | \\s]+\"", Pattern.CASE_INSENSITIVE);
        // God help, what horrific , demon summoning i have written below
        // and yes. the following regex matches expressions with varying whitespaces, depth, parenthesis, etc
        this.exprRe = Pattern.compile("\\(?\\s*[\\w+ | \\d+ | \\s+]\\s*[\\+ | \\- | \\* | \\/ | \\%]\\s*[\\w+ | \\d+ | \\s+]\\s*([\\+ | \\- | \\* | \\/ | \\%]\\s*[\\w+ | \\d+ | \\s+])*\\s*\\)?", Pattern.CASE_INSENSITIVE);

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
        else if(exprRe.matcher(dataValue).matches()){
            // we assume the expression reduces to an int
            valType = "int";
        }
        else {
            valType = "invalid";
        }

        return valType;
    }

}
