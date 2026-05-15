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
    } else if (instruction.matches("L\\d+")
        || instruction.matches("C\\d+")
        || instruction.matches("LS\\d+")) {
      // loop, swtichcase labels
      strOp = instruction + ":";
    } else if (instruction.equals("if")
        || instruction.equals("else if")
        || instruction.equals("goto")) {
      // loop if else
      strOp = instruction + " " + operand1;
    } else if (instruction.equals("==")) {
      // switch case == condition
      strOp = "if" + " " + operand1 + instruction + operand2 + " " + result;
    }

    return strOp;
  }
}

class IRGeneration {

  public Vector<Quadruple> irList;
  private HashMap<String, String> varTempMap;
  //             id       {idCount, idPos}
  public HashMap<String, int[]> identifierReferenceMap;

  private int tempVarCount;
  private int loopLabelCount;
  private int switchcaseLabelCount;
  private int loopStartLabelCount;

  private static Pattern unaryExprRe, binaryExprRe;
  private String loopInLabel, loopOutLabel;
  private String switchcaseIdentifier;
  private int processedSwitchCase;

  public IRGeneration() {
    irList = new Vector<>();
    varTempMap = new HashMap<String, String>();
    identifierReferenceMap = new HashMap<String, int[]>();

    tempVarCount = 0;
    loopLabelCount = 0;
    switchcaseLabelCount = 0;
    loopStartLabelCount = 0;
    loopInLabel = "";
    loopOutLabel = "";
    switchcaseIdentifier = "";
    processedSwitchCase = 0;
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
    loopLabelCount += 1;
    return new String[] {loopIn, loopOut};
  }

  private String generateLoopStartLabel() {
    String loopStartLabel = "LS" + loopStartLabelCount;
    loopStartLabelCount += 1;

    return loopStartLabel;
  }

  private String generateSwitchCaseLabel() {
    String switchCaseLabel = "C" + switchcaseLabelCount;
    switchcaseLabelCount += 1;

    return switchCaseLabel;
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
          int prevLoopStartLabelCount = loopStartLabelCount - 1;
          String prevLoopLabel = "LS" + prevLoopStartLabelCount;
          // add goto that jumps to the condition
          q = new Quadruple("goto", prevLoopLabel, "", "");
          irList.add(q);

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

          // generate a loop start label to which we can jump back to recheck condition after an
          // iteration
          String loopStartLabel = generateLoopStartLabel();
          q = new Quadruple(loopStartLabel, "", "", "");
          irList.add(q);
          // if else strings
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
      case "SwitchCaseStmt":
        {
          processedSwitchCase = 0;
        }
      case "SwitchCaseStmtIdentifier":
        {
          String id = (String) node.jjtGetValue();
          switchcaseIdentifier = id;
          break;
        }
      case "SwitchCaseMatch":
        {
          String match = (String) node.jjtGetValue();
          String caseLabel = generateSwitchCaseLabel();
          String ifCond = switchcaseIdentifier + "==" + match + " " + "goto" + " " + caseLabel;
          String condType = "if";
          if (processedSwitchCase > 0) {
            condType = "else if";
          }
          q = new Quadruple(condType, ifCond, "", "");
          irList.add(q);
          q = new Quadruple(caseLabel, "", "", "");
          irList.add(q);

          processedSwitchCase += 1;
          break;
        }
      case "EndSwitchFor":
        {
          // reset the processed switch-case since we have exited the block
          processedSwitchCase = 0;
        }
    }
  }

  public void postIROptimization() {
    // perform dead code elimination as post optimizatoin
    int[] idCountPos = new int[2];
    int count = 0;
    for (Quadruple q : irList) {
      String id = q.result;
      if (identifierReferenceMap.containsKey(id)) {
        // increment the reference
        idCountPos = new int[] {identifierReferenceMap.get(id)[0] + 1, count};
        System.out.println(idCountPos);
        identifierReferenceMap.put(id, idCountPos);
      } else if (id.matches("t\\d+")) {
        // reference not in our map, but the id is still a valid temp var
        idCountPos = new int[] {0, count};
        identifierReferenceMap.put(id, idCountPos);
      }
      count += 1;
    }

    int normal = 0;
    for (HashMap.Entry<String, int[]> entry : identifierReferenceMap.entrySet()) {
      String k = entry.getKey();
      int[] v = entry.getValue(); // count, post
      if (v[0] == 0) {
        // remove the entry
        irList.remove(v[1] - normal);
        // each time an element is removed, the array is shortened
        // so we need to apply normal to fix the indices
        normal += 1;
      }
    }
  }
}
