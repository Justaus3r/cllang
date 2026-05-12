package CLLang;

import java.util.HashMap;
import java.util.Vector;

class Quadruple {
  public String instruction;
  public String operand1;
  public String operand2;
  public String result;

  public Quadruple(String instruction, String operand1, String operand2, String result) {
    this.instruction = instruction;
    this.operand1 = operand1;
    this.operand2 = operand2;
    this.result = result;
  }

  public String toString() {
    String strOp = "";
    if (instruction.equals("-") || instruction.equals("+")) {
      strOp = result + " = " + operand1 + instruction + operand2;
    } else if (instruction.equals("outString")) {
      strOp = instruction + "(" + operand1 + ")";
    }

    else if(instruction.equals("\"")){
        strOp = result + " = " + operand2;
    }

    return strOp;
  }
}

class IRGeneration {

  public Vector<Quadruple> irList;
  private HashMap<String, String> varTempMap;
  private int tempVarCount;

  public IRGeneration() {
    irList = new Vector<>();
    varTempMap = new HashMap<String,String>(); 
    tempVarCount = 0;
  }

  public void generateIR(Node root) {
    SimpleNode rootNode = (SimpleNode) root;
    String rootNodeName = rootNode.toString();
    this.visitNode(rootNode);
  }

  private String generateTempVar(String identifier) {
    if (varTempMap.containsKey(identifier)) {
      return varTempMap.get(identifier);
    }
    String tempVar = "t" + tempVarCount;
    varTempMap.put(identifier, tempVar);
    tempVarCount += 1;

    return tempVar;
  }

  private void visitNode(Node n) {
    Quadruple q;
    if (n.jjtGetNumChildren() > 0) {
      for (int i = 0; i < n.jjtGetNumChildren(); ++i) {
        this.visitNode(n.jjtGetChild(i));
      }
    }
    SimpleNode node = (SimpleNode) n;
    String nodeName = n.toString();
    switch (nodeName) {
      case "AssignmentStmt":
        {
          HashMap<String, String> kV = (HashMap<String, String>) node.jjtGetValue();
          String key = kV.keySet().iterator().next();
          String value = kV.values().iterator().next();
          String op = "";
          String firstChar = "" + value.charAt(0);
          if(firstChar.equals("\"")){
              op = "\"";
          } 
          else if (firstChar.equals("-")) {
            op = "-";
          } else {
            op = "+";
          }
          String tempvar = this.generateTempVar(key);
          q = new Quadruple(op, "0", value, tempvar);
          irList.add(q);
          break;
        }
      case "OutStmt":
        String stringOut = (String) node.jjtGetValue();
        q = new Quadruple("outString", stringOut, "", "");
        irList.add(q);
    }
  }
}
