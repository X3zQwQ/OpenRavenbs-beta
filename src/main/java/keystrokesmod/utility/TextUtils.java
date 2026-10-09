package keystrokesmod.utility;

public final class TextUtils {

    private static final char COLOR_CHAR = '§';

    private TextUtils() {
    }

    public static String replaceAllKeepingFormatting(String input, Iterable<String> targets, String replacement) {
        if (targets == null) {
            return input;
        } else {
            String replaced = input;
            for (String target : targets) {
                replaced = replaceKeepingFormatting(replaced, target, replacement);
            }
            return replaced;
        }
    }

    public static String replaceKeepingFormatting(String input, String target, String replacement) {
        if (input != null && !input.isEmpty() && target != null && !target.isEmpty() && replacement != null) {
            StringBuilder output = new StringBuilder(input.length() + replacement.length());
            TextUtils.FormattingState formatting = new TextUtils.FormattingState();
            int index = 0;
            while (index < input.length()) {
                char current = input.charAt(index);
                if (isFormattingCode(input, index)) {
                    char code = input.charAt(index + 1);
                    formatting.apply(code);
                    output.append(current).append(code);
                    index += 2;
                } else if (matchesAtIgnoreCase(input, target, index) && hasNameBoundaries(input, index, target.length())) {
                    output.append(replacement);
                    output.append(formatting.asString());
                    index += target.length();
                } else {
                    output.append(current);
                    index++;
                }
            }
            return output.toString();
        } else {
            return input;
        }
    }

    private static boolean matchesAtIgnoreCase(String input, String target, int index) {
        return index >= 0 && index + target.length() <= input.length() && input.regionMatches(true, index, target, 0, target.length());
    }

    public static String extractTrailingWord(String value) {
        if (value == null) {
            return null;
        } else {
            String normalized = Utils.stripColor(value).trim();
            if (normalized.isEmpty()) {
                return null;
            } else {
                int intValue2 = normalized.lastIndexOf(32);
                return intValue2 >= 0 ? normalized.substring(intValue2 + 1) : normalized;
            }
        }
    }

    private static boolean hasNameBoundaries(String input, int intValue, int targetLength) {
        char previous = previousVisibleChar(input, intValue - 1);
        char next = nextVisibleChar(input, intValue + targetLength);
        return isNameBoundary(previous) && isNameBoundary(next);
    }

    private static char previousVisibleChar(String input, int index) {
        int cursor = index;
        while (cursor >= 0) {
            if (cursor > 0 && input.charAt(cursor - 1) == 167) {
                cursor -= 2;
            } else {
                if (input.charAt(cursor) != 167) {
                    return input.charAt(cursor);
                }
                cursor--;
            }
        }
        return '\u0000';
    }

    private static char nextVisibleChar(String input, int index) {
        for (int cursor = index; cursor < input.length(); cursor += 2) {
            if (!isFormattingCode(input, cursor)) {
                return input.charAt(cursor);
            }
        }
        return '\u0000';
    }

    private static boolean isNameBoundary(char character) {
        return character == 0 || !isNameCharacter(character);
    }

    private static boolean isNameCharacter(char character) {
        return Character.isLetterOrDigit(character) || character == '_';
    }

    private static boolean isFormattingCode(String input, int index) {
        return index + 1 < input.length() && input.charAt(index) == 167;
    }

    private static final class FormattingState {

        private Character color;

        private boolean obfuscated;

        private boolean bold;

        private boolean strikethrough;

        private boolean underline;

        private boolean italic;

        private FormattingState() {
        }

        private void apply(char code) {
            char normalized = Character.toLowerCase(code);
            if ((normalized < '0' || normalized > '9') && (normalized < 'a' || normalized > 'f')) {
                switch(normalized) {
                    case 'k':
                        this.obfuscated = true;
                        break;
                    case 'l':
                        this.bold = true;
                        break;
                    case 'm':
                        this.strikethrough = true;
                        break;
                    case 'n':
                        this.underline = true;
                        break;
                    case 'o':
                        this.italic = true;
                    case 'p':
                    case 'q':
                    default:
                        break;
                    case 'r':
                        this.color = null;
                        this.obfuscated = false;
                        this.bold = false;
                        this.strikethrough = false;
                        this.underline = false;
                        this.italic = false;
                }
            } else {
                this.color = normalized;
                this.obfuscated = false;
                this.bold = false;
                this.strikethrough = false;
                this.underline = false;
                this.italic = false;
            }
        }

        private String asString() {
            StringBuilder formatting = new StringBuilder();
            if (this.color != null) {
                formatting.append('§').append(this.color);
            }
            if (this.obfuscated) {
                formatting.append('§').append('k');
            }
            if (this.bold) {
                formatting.append('§').append('l');
            }
            if (this.strikethrough) {
                formatting.append('§').append('m');
            }
            if (this.underline) {
                formatting.append('§').append('n');
            }
            if (this.italic) {
                formatting.append('§').append('o');
            }
            return formatting.toString();
        }
    }
}
