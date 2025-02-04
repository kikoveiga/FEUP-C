package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(PARENTHESES_EXPRESSION, this::visitParenthesesExpr);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(LOGICAL_EXPRESSION, this::visitLogicalExpr);
        addVisit(RELATIONAL_EXPRESSION, this::visitRelationalExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(BOOLEAN_VALUE, this::visitBoolean);
        addVisit(NOT_EXPRESSION, this::visitNotExpr);
        addVisit(METHOD_CALL, this::visitMethodCall);
        addVisit(PROPERTY_ACCESS, this::visitPropertyAccess);
        addVisit(NEW_CLASS_INSTANCE, this::visitNewClassInstance);
        addVisit(NEW_ARRAY, this::visitNewArray);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(ARRAY_INIT, this::visitArrayInit);
        addVisit(THIS, this::visitThis);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);

        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        var boolType = new Type(TypeUtils.getBooleanTypeName(), false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String code = (Objects.equals(node.get("value"), "true") ? "1" : "0") + ollirBoolType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitNotExpr(JmmNode node, Void unused) {
        var child = visit(node.getChild(0));
        Type childType = TypeUtils.getExprType(node, table);
        StringBuilder computation = new StringBuilder();
        computation.append(child.getComputation());
        String code = OptUtils.getTemp() + OptUtils.toOllirType(TypeUtils.getExprType(node, table));
        computation.append(code).append(SPACE).append(ASSIGN).append(OptUtils.toOllirType(childType)).append(SPACE).append("!.bool ").append(child.getCode()).append(END_STMT);
        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitParenthesesExpr(JmmNode node, Void unused) {
        return visit(node.getChild(0));
    }

    private OllirExprResult visitLogicalExpr(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String tempVar = OptUtils.getTemp() + ".bool";

        var lhs = visit(node.getChild(0));
        var rhs = visit(node.getChild(1));
        int ifThenNum = OptUtils.getNextIfThenNum();

        computation.append(lhs.getComputation());

        computation.append("if(").append(lhs.getCode()).append(") goto true_").append(ifThenNum).append(END_STMT);
        computation.append(tempVar).append(" :=.bool 0.bool").append(END_STMT);
        computation.append("goto end_").append(ifThenNum).append(END_STMT).append("true_").append(ifThenNum).append(":\n");
        computation.append(rhs.getComputation());
        computation.append(tempVar).append(" :=.bool ").append(rhs.getCode()).append(END_STMT);
        computation.append("end_").append(ifThenNum).append(":\n");

        code.append(tempVar);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitRelationalExpr(JmmNode node, Void unused) {

        String operator = node.get("op");
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String ollirBoolType = OptUtils.toOllirType(new Type(TypeUtils.getBooleanTypeName(), false));
        String tempVar = OptUtils.getTemp() + ollirBoolType;

        var lhs = visit(node.getChild(0));
        var rhs = visit(node.getChild(1));
        int ifThenNum = OptUtils.getNextIfThenNum();

        computation.append(lhs.getComputation()).append(rhs.getComputation());

        computation.append("if(").append(lhs.getCode()).append(operator).append(ollirBoolType).append(SPACE).append(rhs.getCode()).append(") goto true_").append(ifThenNum).append(END_STMT);
        computation.append(tempVar).append(" :=.bool 0.bool").append(END_STMT);
        computation.append("goto end_").append(ifThenNum).append(END_STMT).append("true_").append(ifThenNum).append(":\n");
        computation.append(tempVar).append(" :=.bool 1.bool").append(END_STMT);
        computation.append("end_").append(ifThenNum).append(":\n");

        code.append(tempVar);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getChild(0));
        var rhs = visit(node.getChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation()).append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE).append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE).append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);

        if (type == null) {
            return new OllirExprResult(id);
        }
        String ollirType = OptUtils.toOllirType(type);

        String computation = "";
        boolean isField = true;

        // Get the current method
        JmmNode parent = node.getParent();
        while (!parent.getKind().equals("MethodDecl") && !parent.getKind().equals("MainMethodDecl")) {
            parent = parent.getParent();
        }
        String methodName = parent.get("name");

        for (var local : table.getLocalVariables(methodName)) {
            if (local.getName().equals(id)) {
                isField = false;
                break;
            }
        }

        // Check if it's a param
        if (isField) {

            for (var param : table.getParameters(methodName)) {
                if (param.getName().equals(node.get("name"))) {
                    isField = false;
                    break;
                }
            }
        }
        if (isField) {
            var temp = OptUtils.getTemp();
            computation = temp + ollirType + SPACE + ASSIGN + ollirType + " getfield(this, " + id + ollirType + ")" + ollirType + END_STMT;
            id = temp;
        }

        String code = id + ollirType;
        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        List<String> argsCode = new ArrayList<>();
        String methodName = node.get("methodName");

        // Visit the owner of the method (e.g., an instance of a class, or the class itself for static methods)
        OllirExprResult ownerExpr = visit(node.getChild(0));
        computation.append(ownerExpr.getComputation());

        boolean isVarArgs = false;
        int index = 0;

        if (table.getMethods().contains(methodName)) {
            for (var param : table.getParameters(methodName)) {
                if (param.getType().hasAttribute("isVarArgs")) {
                    isVarArgs = true;
                    break;
                }
                index++;
            }
        }

        String ollirIntType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), false));
        String tempVarForVarArgs = "";
        String arrayType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), true));
        int numArgs = node.getNumChildren() - 1;

        if (isVarArgs) {
            tempVarForVarArgs = OptUtils.getTemp();
            computation.append(tempVarForVarArgs).append(arrayType).append(SPACE).append(ASSIGN).append(arrayType).append(SPACE).append("new(array, ").append(numArgs - index).append(ollirIntType).append(")").append(arrayType).append(END_STMT);
        }

        // Handle each argument of the method
        for (int i = 1; i < node.getNumChildren(); i++) {

            if (!isVarArgs || index > i - 1) {
                OllirExprResult argExpr = visit(node.getChild(i));
                computation.append(argExpr.getComputation());
                argsCode.add(argExpr.getCode());
            }

            else if (index == i - 1) {

                int indexVarArgs = 0;
                for (int j = i; j < node.getNumChildren(); j++) {
                    OllirExprResult argExpr = visit(node.getChild(j));
                    computation.append(argExpr.getComputation());
                    computation.append(tempVarForVarArgs).append("[").append(indexVarArgs).append(ollirIntType).append("]").append(ollirIntType).append(SPACE).append(ASSIGN).append(ollirIntType).append(SPACE).append(argExpr.getCode()).append(END_STMT);
                    indexVarArgs++;
                }
            }
        }

        if (isVarArgs) argsCode.add(tempVarForVarArgs + arrayType);


        boolean isAssign = false;
        boolean isStatic = false;
        boolean isInsideMethodCall = false;
        boolean isInsideReturn = false;
        JmmNode parent = node.getParent();
        JmmNode child = node.getChild(0);
        Type childType = TypeUtils.getExprType(child, table);

        while(!parent.getKind().equals("MethodDecl") && !parent.getKind().equals("MainMethodDecl")) {
            if (parent.getKind().equals("AssignStmt")) {
                isAssign = true;
            }
            else if (parent.getKind().equals("MethodCall")) {
                isInsideMethodCall = true;
            }
            else if (parent.getKind().equals("ReturnStmt")) {
                isInsideReturn = true;
            }
            parent = parent.getParent();
        }

        if (childType == null) {
            isStatic = true;
        }

        // Get the return type
        Type returnType = TypeUtils.getExprType(node, table);
        String returnTypeString = OptUtils.toOllirType(returnType);

        // Construct the method call
        String argsList = String.join(", ", argsCode);
        String tempVar = OptUtils.getTemp() + returnTypeString;
        String methodCallComputation =  (isStatic ? "invokestatic(" : "invokevirtual(") + ownerExpr.getCode() + ", \"" + methodName + "\""
                                        + (argsCode.isEmpty() ? "" : ", " + argsList) + ")" + returnTypeString;

        // Store the result of the method call in a temporary variable
        if (isAssign || isInsideMethodCall || isInsideReturn) {
            computation.append(tempVar).append(SPACE).append(ASSIGN).append(returnTypeString).append(SPACE);
        }
        computation.append(methodCallComputation).append(END_STMT);

        return new OllirExprResult(tempVar, computation.toString());
    }

    private OllirExprResult visitPropertyAccess(JmmNode node, Void unused) {
        if (!node.get("name").equals("length")) return OllirExprResult.EMPTY; // Only length property is supported
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        String intType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), false));
        var array = node.getChild(0);
        var arrayVisit = visit(array);
        String arrayType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), true));
        String arrayName;
        if (array.getKind().equals("VarRefExpr")) {
            arrayName = array.get("name");

        } else {
            arrayName = arrayVisit.getCode();
            arrayType = "";
        }

        String tempVar = OptUtils.getTemp() + intType;

        computation.append(arrayVisit.getComputation());

        code.append(tempVar);
        computation.append(tempVar).append(ASSIGN).append(intType).append(" arraylength(").append(arrayName).append(arrayType).append(")").append(intType).append(END_STMT);

        return new OllirExprResult(code.toString(), computation.toString());

    }

    private OllirExprResult visitNewArray(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        // Visit the size of the array
        OllirExprResult sizeExpr = visit(node.getChild(1));
        computation.append(sizeExpr.getComputation());

        // Get the type of the array
        Type arrayType = TypeUtils.getExprType(node, table);
        String arrayTypeString = OptUtils.toOllirType(arrayType);
        String tempVar = OptUtils.getTemp() + arrayTypeString;

        // Construct the array creation
        String arrayCreationCode = "new(array" + ", " + sizeExpr.getCode() + ")" + arrayTypeString;

        // Store the result of the array creation in a temporary variable
        computation.append(tempVar).append(SPACE).append(ASSIGN).append(arrayTypeString).append(SPACE).append(arrayCreationCode).append(END_STMT);
        code.append(tempVar);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        JmmNode array = node.getChild(0);
        var arrayVisit = visit(array);
        OllirExprResult index = visit(node.getChild(1));
        String intType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), false));
        String tempVar = OptUtils.getTemp() + intType;

        computation.append(index.getComputation()).append(arrayVisit.getComputation());

        boolean onLeftSideOfAssign = false;

        JmmNode parent = node.getParent();
        while (!parent.getKind().equals("MethodDecl") && !parent.getKind().equals("MainMethodDecl")) {
            if (parent.getKind().equals("AssignStmt")) {
                if (parent.getChild(0).equals(node)) onLeftSideOfAssign = true;
                break;
            }
            parent = parent.getParent();
        }

        if (onLeftSideOfAssign) {
            code.append(arrayVisit.getCode()).append("[").append(index.getCode()).append("]").append(intType);
        }

        else {
            computation.append(tempVar).append(SPACE).append(ASSIGN).append(intType).append(SPACE).append(arrayVisit.getCode()).append("[").append(index.getCode()).append("]").append(intType).append(END_STMT);
            code.append(tempVar);
        }

        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitThis(JmmNode node, Void unused) {
        return new OllirExprResult("this." + table.getClassName(), "");
    }

    private OllirExprResult visitNewClassInstance(JmmNode node, Void unused) {
        String className = node.get("name");
        String tempVar = OptUtils.getTemp() + "." + className;
        String initializationCode = tempVar + SPACE + ASSIGN + "." + className + SPACE + "new(" + className + ")." + className
                + END_STMT + "invokespecial(" + tempVar + ", \"<init>\").V" + END_STMT;
        return new OllirExprResult(tempVar, initializationCode);
    }

    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

    private OllirExprResult visitArrayInit(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        String tempVar = OptUtils.getTemp();

        JmmNode child = node.getChild(0);
        int size = child.getNumChildren();
        String intType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), false));
        String arrayType = OptUtils.toOllirType(new Type(TypeUtils.getIntTypeName(), true));

        code.append(tempVar).append(arrayType);

        computation.append(tempVar).append(arrayType).append(SPACE).append(ASSIGN).append(arrayType).append(SPACE).append("new(array, ").append(size).append(intType).append(")").append(arrayType).append(END_STMT);
        for (int i = 0; i < child.getNumChildren(); i++) {
            computation.append(tempVar).append("[").append(i).append(intType).append("]").append(intType).append(SPACE).append(ASSIGN).append(intType).append(SPACE).append(visit(child.getChild(i)).getCode()).append(END_STMT);
        }

        return new OllirExprResult(code.toString(), computation.toString());
    }
}
