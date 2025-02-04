package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.Arrays;
import java.util.Set;

public enum Kind {
    PROGRAM,
    CLASS_DECL,
    IMPORT_DECL,
    EXTENDED_CLASS,
    VAR_DECL,
    TYPE,
    DOTTED_STRINGS,
    METHOD_DECL,
    MAIN_METHOD_DECL,
    PARAM,
    BLOCK_STMT,
    IF_STMT,
    WHILE_STMT,
    ASSIGN_STMT,
    RETURN_STMT,
    EXPRESSION_STMT,
    ARRAY_ACCESS,
    PROPERTY_ACCESS,
    LENGTH_ACCESS,
    METHOD_CALL_ON_ASSIGN,
    METHOD_CALL,
    BOOLEAN_VALUE,
    THIS,
    VAR_REF_EXPR,
    INTEGER_LITERAL,
    PARENTHESES_EXPRESSION,
    BINARY_EXPR,
    RELATIONAL_EXPRESSION,
    LOGICAL_EXPRESSION,
    NOT_EXPRESSION,
    NEW_ARRAY,
    ARRAY_INIT,
    NEW_CLASS_INSTANCE,
    EXPRESSION_LIST,
    INTEGER_TYPE,
    BOOLEAN_TYPE,
    STRING_TYPE,
    ARRAY_TYPE,
    VAR_ARGS_TYPE,
    OTHER_TYPE,
    VOID_TYPE;

    private static final Set<Kind> STATEMENTS = Set.of(BLOCK_STMT, IF_STMT, WHILE_STMT, ASSIGN_STMT, RETURN_STMT, EXPRESSION_STMT);
    private static final Set<Kind> EXPRESSIONS = Set.of(ARRAY_ACCESS, PROPERTY_ACCESS, LENGTH_ACCESS,METHOD_CALL_ON_ASSIGN, METHOD_CALL, BOOLEAN_VALUE, THIS, VAR_REF_EXPR, INTEGER_LITERAL, PARENTHESES_EXPRESSION, BINARY_EXPR, RELATIONAL_EXPRESSION, LOGICAL_EXPRESSION, NOT_EXPRESSION, NEW_ARRAY, ARRAY_INIT, NEW_CLASS_INSTANCE, ARRAY_TYPE);

    private final String name;

    Kind(String name) {
        this.name = name;
    }

    Kind() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    public static Kind fromString(String kind) {

        for (Kind k : Kind.values()) {
            if (k.getNodeName().equals(kind)) {
                return k;
            }
        }
        throw new RuntimeException("Could not convert string '" + kind + "' to a Kind");
    }

    public String getNodeName() {
        return name;
    }

    @Override
    public String toString() {
        return getNodeName();
    }

    /**
     * @return true if this kind represents a statement, false otherwise
     */
    public boolean isStmt() {
        return STATEMENTS.contains(this);
    }

    /**
     * @return true if this kind represents an expression, false otherwise
     */
    public boolean isExpr() {
        return EXPRESSIONS.contains(this);
    }

    /**
     * Tests if the given JmmNode has the same kind as this type.
     */
    public boolean check(JmmNode node) {
        return node.isInstance(this);
    }

    /**
     * Performs a check and throws if the test fails. Otherwise, does nothing.
     */
    public void checkOrThrow(JmmNode node) {

        if (!check(node)) {
            throw new RuntimeException("Node '" + node + "' is not a '" + getNodeName() + "'");
        }
    }

    /**
     * Performs a check on all kinds to test and returns false if none matches. Otherwise, returns true.
     *
     * @param node
     * @param kindsToTest
     * @return
     */
    public static boolean check(JmmNode node, Kind... kindsToTest) {

        for (Kind k : kindsToTest) {

            // if any matches, return successfully
            if (k.check(node)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Performs a check an all kinds to test and throws if none matches. Otherwise, does nothing.
     *
     * @param node
     * @param kindsToTest
     */
    public static void checkOrThrow(JmmNode node, Kind... kindsToTest) {
        if (!check(node, kindsToTest)) {
            // throw if none matches
            throw new RuntimeException("Node '" + node + "' is not any of " + Arrays.asList(kindsToTest));
        }
    }
}
