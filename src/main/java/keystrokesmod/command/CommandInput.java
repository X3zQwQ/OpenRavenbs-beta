package keystrokesmod.command;

public final class CommandInput {

    private final String rawMessage;

    private final String label;

    private final String[] arguments;

    public CommandInput(String rawMessage, String label, String[] arguments) {
        this.rawMessage = rawMessage == null ? "" : rawMessage;
        this.label = label == null ? "" : label;
        this.arguments = arguments == null ? new String[0] : (String[]) arguments.clone();
    }

    public String getRawMessage() {
        return this.rawMessage;
    }

    public String getLabel() {
        return this.label;
    }

    public int argumentCount() {
        return this.arguments.length;
    }

    public String[] getArguments() {
        return (String[]) this.arguments.clone();
    }

    public String getArgument(int index) {
        return index >= 0 && index < this.arguments.length ? this.arguments[index] : null;
    }

    public String joinArguments(int intValue) {
        if (intValue >= 0 && intValue < this.arguments.length) {
            StringBuilder builder = new StringBuilder();
            for (int i = intValue; i < this.arguments.length; i++) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(this.arguments[i]);
            }
            return builder.toString();
        } else {
            return "";
        }
    }
}
