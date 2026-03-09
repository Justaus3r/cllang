package CLLang;
import java.util.HashMap;

public class SymbolTable {
     
    public static HashMap<String, String> symTable;
    public static String lKey;
    public static String lValue;

    public SymbolTable(){
        this.symTable = new HashMap<String, String>();
    }
    
    public boolean checkSymbolExists(String symbol){
        return this.symTable.containsKey(symbol);
    }

    public boolean put(String varName, String varType){
        if(this.symTable.containsKey(varName)){
            return false;
        }
        
        this.lKey = varName;
        this.lValue = varType;

        this.symTable.put(varName, varType);
        return true;
    }

    public String get(String varName){
        String ret = "";
        
        if(this.symTable.containsKey(varName)){
            ret = this.symTable.get(varName);
        }

        return ret;
    }

    public String lastKey(){
        return this.lKey; 
    }

    public String lastValue(){
        return this.lValue; 
    }
    
    public int getLength(){
        return this.symTable.size();
    }

    public void printSymbolTable(){
        System.out.println("************Symbol Table***************");
        this.symTable.forEach((k,v) -> {
            System.out.println(k + ": " + v);
        });
        System.out.println("***************************************");
    }
}

