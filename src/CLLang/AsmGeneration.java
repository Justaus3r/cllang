package CLLang;

import java.util.ArrayList;
import java.util.HashMap;
// dunno why java keeps removing the import of IRGeneration class, if i don't do the import, it complains, idk what the fuck is wrong with it
import CLLang.IRGeneration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// pseudo asm generation
class AsmGeneration {

  String reg;
  ArrayList<String> asmList;
  HashMap<String, String> opInstMap;

  Pattern binaryAssignmentRe, unaryAssignmentRe, stringAssignmentRe, simpleBinaryExprRe;

  public AsmGeneration() {
    asmList = new ArrayList<String>();
    // we are using a naive approach, of resusing the same register
    reg = "R0";
    opInstMap = new HashMap<String, String>();
    // populate operator-instruction map
    opInstMap.put("+", "ADD");
    opInstMap.put("-", "SUB");
    opInstMap.put("*", "MUL");
    opInstMap.put("/", "DIV");
    opInstMap.put("%", "MOD");

    binaryAssignmentRe =
        Pattern.compile(
            "\\w+\\s*=\\s*([\\d|\\w|\\d\\.\\d]+\\s*[\\+\\-\\*\\/]\\s*[\\d|\\w|\\d\\.\\d]+)");
    unaryAssignmentRe = Pattern.compile("\\w+\\s*=\\s*[\\d|\\w|\\d\\.\\d]+");
    stringAssignmentRe = Pattern.compile("\\w+\\s*=\\s*\\\"\\s*\\w+\\s*\\\"");
    simpleBinaryExprRe = Pattern.compile("(\\w+)([+\\-*/])(\\w+)");
  }

  public void generateAsm(ArrayList<Quadruple> irList) {
    for (Quadruple q : irList) {
      String exprStr = q.toString();
      Matcher match;
      if (binaryAssignmentRe.matcher(exprStr).matches()) {
        String nameExpr[] = exprStr.split("=");
        String varName = nameExpr[0].strip();
        String expr = nameExpr[1].strip();
        match = simpleBinaryExprRe.matcher(expr);
        if (!match.find()) {
          throw new ArithmeticException(
              "Expresion declared of binary nature, but pattern is not matched. Panicing...");
        }
        String operand1 = match.group(1).strip();
        String op = match.group(2).strip();
        String operand2 = match.group(3).strip();

        String loadI = "LD" + " " + reg + " ," + " " + operand1;

        asmList.add(loadI);
        String instruction = opInstMap.get(op);

        String operationI = instruction + " " + reg + " ," + " " + operand2;
        asmList.add(operationI);

        String resultStoreI = "ST" + " " + varName + " ," + " " + reg;
        asmList.add(resultStoreI);

      } else if (unaryAssignmentRe.matcher(q.toString()).matches()
          || stringAssignmentRe
              .matcher(q.toString())
              .matches()) { // both unary and string assignments produce the same assembly

        String nameExpr[] = exprStr.split("=");
        String varName = nameExpr[0].strip();
        String expr = nameExpr[1].strip();

        String loadI = "LD" + " " + reg + " ," + " " + expr;
        asmList.add(loadI);

        String storeI = "ST" + " " + varName + " ," + " " + reg;
        asmList.add(storeI);
      } else {
        // if its not an assignment stmt, we just add it as it is
        asmList.add(exprStr);
      }
    }
  }
}
