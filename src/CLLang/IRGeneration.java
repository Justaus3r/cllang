package CLLang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Pattern;

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
    if (instruction.matches("[\\+\\-\\* \\/\\%]")) {
      // binary expression assignment
      strOp = result + " = " + operand1 + instruction + operand2;
    } else if (operand1.matches("\\d+|\\d+\\.\\d+") && operand2.equals("")) {
      // unary expression assignment
      strOp = result + " = " + operand1;
    } else if (instruction.equals("outString")) {
      // outString
      strOp = instruction + "(" + operand1 + ")";
    } else if (instruction.equals("\"")) {
      // string assigment
      strOp = result + " = " + operand2;
    } else if (instruction.matches("L\\d+")) {
      strOp = instruction + ":";
    } else if(instruction.equals("if") || instruction.equals("goto")){
        strOp = instruction + " " + operand1;
    }

    return strOp;
  }
}

class IRGeneration {

  public Vector<Quadruple> irList;
  private HashMap<String, String> varTempMap;
  private int tempVarCount;
  private int loopLabelCount;
  private static Pattern unaryExprRe, binaryExprRe;
  private String loopInLabel, loopOutLabel;
  private ArrayList<String> processedConditionalsList;

  public IRGeneration() {
    irList = new Vector<>();
    varTempMap = new HashMap<String, String>();
    tempVarCount = 0;
    loopLabelCount = 0;
    loopInLabel = "";
    loopOutLabel = "";
    binaryExprRe =
        Pattern.compile(
            "\\(?\\s*[\\w+ | \\d+ | \\s+]\\s*[\\+ | \\- | \\* | \\/ | \\%]\\s*[\\w+ | \\d+ |"
                + " \\s+]\\s*([\\+ | \\- | \\* | \\/ | \\%]\\s*[\\w+ | \\d+ | \\s+])*\\s*\\)?",
            Pattern.CASE_INSENSITIVE);
    unaryExprRe = Pattern.compile("\\d+|\\d+\\.\\d+|\\w+");
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

  private String[] generateLoopLabels() {
    String loopIn = "L" + loopLabelCount;
    loopLabelCount += 1;
    String loopOut = "L" + loopLabelCount;
    return new String[] {loopIn, loopOut};
  }

  private void visitNode(Node n) {
    Quadruple q;
    String tempVar;
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
          // if assignment is a string:
          if (firstChar.equals("\"")) {
            op = "\"";
          }
          // if assignment is an binary expression
          else if (binaryExprRe.matcher(value).matches()) {
            value = value.replaceAll("[\\( | \\)]", "");
            String elementList[] = value.split("[\\+\\-\\* \\/\\%]");
            ArrayList<String> operatorList = new ArrayList<String>();
            for (char c : value.toCharArray()) {
              if (c == '+' || c == '-' || c == '*' || c == '/' || c == '%') {
                operatorList.add(String.valueOf(c));
              }
            }
            if (operatorList.size() != elementList.length - 1) {
              throw new ArithmeticException(
                  "Mismatch between operators and operands count. operators should be operands - 1"
                      + " in count");
            }

            ArrayList<String> expressionList = new ArrayList<String>();
            String operatorListCharArray[] = operatorList.toArray(new String[0]);
            int operatorCount = operatorListCharArray.length;
            for (int i = 0; i < elementList.length; ++i) {
              expressionList.add(elementList[i]);
              if (!(i == elementList.length - 1)) {
                expressionList.add(operatorListCharArray[i]);
              }
            }
            int jumpIdx = 0;
            for (int i = 0; i < operatorCount; ++i) {
              if (expressionList
                  .get(jumpIdx)
                  .matches("[\\+\\-\\* \\/\\%]")) { // curr token is operator
                Quadruple lastQ = irList.getLast();
                String lastT = lastQ.result;
                String operator = expressionList.get(jumpIdx);
                String operand2 = expressionList.get(jumpIdx + 1);
                tempVar = generateTempVar(key);
                q = new Quadruple(operator, lastT, operand2, tempVar);
                irList.add(q);
                jumpIdx += 3;
                continue;
              }
              String operand1 = expressionList.get(jumpIdx);
              String operator = expressionList.get(jumpIdx + 1);
              String operand2 = expressionList.get(jumpIdx + 2);
              // ******* SINGLE BINARY EXPRESSION IR GENERATION AND PRE-OPTIMIZATION PHASE *********
              if ((operand1.equals("0") || operand2.equals("0")) && operator.equals("*")) {
                tempVar = generateTempVar(key);
                q = new Quadruple("", "0", "", tempVar);
              } else if (((operand1.equals("1") || operand2.equals("1")) && operator.equals("*"))) {
                String res = (operand1.equals("1")) ? operand1 : operand2;
                tempVar = generateTempVar(key);
                q = new Quadruple("", res, "", tempVar);
              } else if (operand1.matches("\\d+(.\\d+)?") && operand2.matches("\\d+(.\\d+)?")) {
                int n1 = Integer.valueOf(operand1);
                int n2 = Integer.valueOf(operand2);
                int res = 0;
                if (operator.equals("+")) {
                  res = n1 + n2;
                } else if (operator.equals("-")) {
                  res = n1 - n2;
                } else if (operator.equals("*")) {
                  res = n1 * n2;
                } else if (operator.equals("/")) {
                  res = n1 / n2;
                }
                tempVar = generateTempVar(key);
                q = new Quadruple("", String.valueOf(res), "", tempVar);
              } else {
                tempVar = generateTempVar(key);
                q = new Quadruple(operator, operand1, operand2, tempVar);
              }
              irList.add(q);
              jumpIdx += 3;
            }
          }
          // unary expression else below
          else if (unaryExprRe.matcher(value).matches()) {
            tempVar = generateTempVar(key);
            q = new Quadruple("", value, "", tempVar);
            irList.add(q);
          } else {
            System.out.println("OTHER:" + value);
          }
          break;
        }
      case "OutStmt":
        {
          String stringOut = (String) node.jjtGetValue();
          q = new Quadruple("outString", stringOut, "", "");
          irList.add(q);
          break;
        }
      case "LoopStmt":
        {
          // no need to do anything
          break;
        }
      case "LoopEnd":
        {
          q = new Quadruple(loopOutLabel, "", "", "");
          irList.add(q);

          loopInLabel = "";
          loopOutLabel = "";

          break;
        }
      case "ConditionalStmt":
        {
          String loopLabels[] = generateLoopLabels();
          String loopIn = loopLabels[0];
          String loopOut = loopLabels[1];
          loopInLabel = loopIn;
          loopOutLabel = loopOut;
          
          String cond = (String) node.jjtGetValue();

          String condIfStmt = cond + " " + "goto" + " " + loopInLabel;
          String condElseStmt = loopOutLabel;
          // if
          q = new Quadruple("if", condIfStmt, "", "");
          irList.add(q);
          // else
          q = new Quadruple("goto", condElseStmt, "", "");
          irList.add(q);
          
          // now add loopIn label
          q = new Quadruple(loopInLabel, "", "", "");
          irList.add(q);
          break;
        }
    }
  }
}
