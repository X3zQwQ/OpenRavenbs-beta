package keystrokesmod.script;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ScriptSourceTransformer {

    private ScriptSourceTransformer() {
    }

    static String supportNestedTypes(String source) {
        if (source != null && !source.isEmpty()) {
            List<ScriptSourceTransformer.Token> tokens = tokenize(source);
            Map<Integer, Integer> matchingBraces = findMatchingBraces(tokens);
            List<ScriptSourceTransformer.TypeDeclaration> declarations = findTypeDeclarations(tokens, matchingBraces);
            attachMemberParents(declarations);
            List<Integer> insertionPoints = new ArrayList<>();
            for (ScriptSourceTransformer.TypeDeclaration declaration : declarations) {
                if (declaration.eligibleMember && declaration.kind == ScriptSourceTransformer.TypeKind.CLASS && becomesStatic(declaration) && !declaration.declaredStatic) {
                    insertionPoints.add(declaration.keywordPosition);
                }
            }
            if (insertionPoints.isEmpty()) {
                return source;
            } else {
                insertionPoints.sort(Collections.reverseOrder());
                StringBuilder transformed = new StringBuilder(source);
                for (int insertionPoint : insertionPoints) {
                    transformed.insert(insertionPoint, "static ");
                }
                return transformed.toString();
            }
        } else {
            return source;
        }
    }

    private static boolean becomesStatic(ScriptSourceTransformer.TypeDeclaration declaration) {
        if (declaration.kind != ScriptSourceTransformer.TypeKind.CLASS) {
            return true;
        } else if (declaration.staticState != null) {
            return declaration.staticState;
        } else {
            declaration.staticState = declaration.declaredStatic;
            if (!declaration.staticState) {
                for (ScriptSourceTransformer.TypeDeclaration child : declaration.memberChildren) {
                    if (child.eligibleMember && becomesStatic(child)) {
                        declaration.staticState = true;
                        break;
                    }
                }
            }
            return declaration.staticState;
        }
    }

    private static void attachMemberParents(List<ScriptSourceTransformer.TypeDeclaration> declarations) {
        declarations.sort(Comparator.comparingInt(value -> value.keywordToken));
        for (ScriptSourceTransformer.TypeDeclaration declaration : declarations) {
            ScriptSourceTransformer.TypeDeclaration enclosing = null;
            for (ScriptSourceTransformer.TypeDeclaration candidate : declarations) {
                if (candidate.keywordToken >= declaration.keywordToken) {
                    break;
                }
                if (candidate.bodyOpenToken < declaration.keywordToken && candidate.bodyCloseToken > declaration.keywordToken && (enclosing == null || candidate.bodyOpenToken > enclosing.bodyOpenToken)) {
                    enclosing = candidate;
                }
            }
            if (enclosing == null) {
                declaration.eligibleMember = declaration.depth == 0;
            } else {
                boolean directMember = declaration.depth == enclosing.depth + 1;
                declaration.eligibleMember = directMember && enclosing.eligibleMember;
                if (directMember) {
                    enclosing.memberChildren.add(declaration);
                }
            }
        }
    }

    private static List<ScriptSourceTransformer.TypeDeclaration> findTypeDeclarations(List<ScriptSourceTransformer.Token> tokens, Map<Integer, Integer> matchingBraces) {
        List<ScriptSourceTransformer.TypeDeclaration> declarations = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            ScriptSourceTransformer.Token token = tokens.get(i);
            ScriptSourceTransformer.TypeKind kind = typeKind(token.text);
            if (kind != null && !isClassLiteral(tokens, i)) {
                int bodyOpen = findTypeBody(tokens, i);
                Integer bodyClose = matchingBraces.get(bodyOpen);
                if (bodyOpen != -1 && bodyClose != null) {
                    ScriptSourceTransformer.TypeDeclaration declaration = new ScriptSourceTransformer.TypeDeclaration();
                    declaration.kind = kind;
                    declaration.keywordToken = i;
                    declaration.keywordPosition = token.position;
                    declaration.bodyOpenToken = bodyOpen;
                    declaration.bodyCloseToken = bodyClose;
                    declaration.depth = token.depth;
                    declaration.declaredStatic = kind != ScriptSourceTransformer.TypeKind.CLASS || hasStaticModifier(tokens, i);
                    declarations.add(declaration);
                }
            }
        }
        return declarations;
    }

    private static int findTypeBody(List<ScriptSourceTransformer.Token> tokens, int keywordToken) {
        int declarationDepth = tokens.get(keywordToken).depth;
        int parentheses = 0;
        int brackets = 0;
        for (int i = keywordToken + 1; i < tokens.size(); i++) {
            ScriptSourceTransformer.Token token = tokens.get(i);
            if (token.depth < declarationDepth) {
                return -1;
            }
            if (token.depth == declarationDepth) {
                if ("(".equals(token.text)) {
                    parentheses++;
                } else if (")".equals(token.text) && parentheses > 0) {
                    parentheses--;
                } else if ("[".equals(token.text)) {
                    brackets++;
                } else if ("]".equals(token.text) && brackets > 0) {
                    brackets--;
                } else {
                    if ("{".equals(token.text) && parentheses == 0 && brackets == 0) {
                        return i;
                    }
                    if (";".equals(token.text) && parentheses == 0 && brackets == 0) {
                        return -1;
                    }
                }
            }
        }
        return -1;
    }

    private static boolean hasStaticModifier(List<ScriptSourceTransformer.Token> tokens, int keywordToken) {
        int declarationDepth = tokens.get(keywordToken).depth;
        for (int i = keywordToken - 1; i >= 0; i--) {
            ScriptSourceTransformer.Token token = tokens.get(i);
            if (token.depth < declarationDepth) {
                break;
            }
            if (token.depth <= declarationDepth) {
                if (";".equals(token.text) || "{".equals(token.text) || "}".equals(token.text)) {
                    break;
                }
                if ("static".equals(token.text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isClassLiteral(List<ScriptSourceTransformer.Token> tokens, int intValue) {
        return intValue > 0 && "class".equals(tokens.get(intValue).text) && ".".equals(tokens.get(intValue - 1).text);
    }

    private static ScriptSourceTransformer.TypeKind typeKind(String token) {
        if ("class".equals(token)) {
            return ScriptSourceTransformer.TypeKind.CLASS;
        } else if ("enum".equals(token)) {
            return ScriptSourceTransformer.TypeKind.ENUM;
        } else {
            return "interface".equals(token) ? ScriptSourceTransformer.TypeKind.INTERFACE : null;
        }
    }

    private static Map<Integer, Integer> findMatchingBraces(List<ScriptSourceTransformer.Token> tokens) {
        Map<Integer, Integer> matching = new HashMap<>();
        Deque<Integer> openings = new ArrayDeque<>();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i).text;
            if ("{".equals(token)) {
                openings.push(i);
            } else if ("}".equals(token) && !openings.isEmpty()) {
                matching.put(openings.pop(), i);
            }
        }
        return matching;
    }

    private static List<ScriptSourceTransformer.Token> tokenize(String source) {
        List<ScriptSourceTransformer.Token> tokens = new ArrayList<>();
        int depth = 0;
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
            } else {
                if (current == '/' && index + 1 < source.length()) {
                    char next = source.charAt(index + 1);
                    if (next == '/') {
                        index = skipLineComment(source, index + 2);
                        continue;
                    }
                    if (next == '*') {
                        index = skipBlockComment(source, index + 2);
                        continue;
                    }
                }
                if (current != '"' && current != '\'') {
                    if (!Character.isJavaIdentifierStart(current)) {
                        if (current == '}') {
                            depth = Math.max(0, depth - 1);
                        }
                        tokens.add(new ScriptSourceTransformer.Token(String.valueOf(current), index, depth));
                        if (current == '{') {
                            depth++;
                        }
                        index++;
                    } else {
                        int start = index++;
                        while (index < source.length() && Character.isJavaIdentifierPart(source.charAt(index))) {
                            index++;
                        }
                        tokens.add(new ScriptSourceTransformer.Token(source.substring(start, index), start, depth));
                    }
                } else {
                    index = skipQuotedValue(source, index + 1, current);
                }
            }
        }
        return tokens;
    }

    private static int skipLineComment(String source, int index) {
        while (index < source.length() && source.charAt(index) != '\n' && source.charAt(index) != '\r') {
            index++;
        }
        return index;
    }

    private static int skipBlockComment(String source, int index) {
        while (index + 1 < source.length()) {
            if (source.charAt(index) == '*' && source.charAt(index + 1) == '/') {
                return index + 2;
            }
            index++;
        }
        return source.length();
    }

    private static int skipQuotedValue(String source, int index, char quote) {
        boolean escaped = false;
        while (index < source.length()) {
            char current = source.charAt(index++);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == quote) {
                break;
            }
        }
        return index;
    }

    private static final class Token {

        private final String text;

        private final int position;

        private final int depth;

        private Token(String text, int position, int depth) {
            this.text = text;
            this.position = position;
            this.depth = depth;
        }
    }

    private static final class TypeDeclaration {

        private ScriptSourceTransformer.TypeKind kind;

        private int keywordToken;

        private int keywordPosition;

        private int bodyOpenToken;

        private int bodyCloseToken;

        private int depth;

        private boolean declaredStatic;

        private boolean eligibleMember;

        private Boolean staticState;

        private final List<ScriptSourceTransformer.TypeDeclaration> memberChildren = new ArrayList<>();

        private TypeDeclaration() {
        }
    }

    private static enum TypeKind {

        CLASS, ENUM, INTERFACE
    }
}
