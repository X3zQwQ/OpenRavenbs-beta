package keystrokesmod.command.impl;

import java.math.BigDecimal;
import java.util.List;
import keystrokesmod.command.Command;
import keystrokesmod.command.CommandInput;
import keystrokesmod.module.ModuleManager;

public class Timer extends Command {

    public Timer() {
        super("timer");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() == 1 && input.getArgument(0).equalsIgnoreCase("reset")) {
            ModuleManager.timer.resetSpeed();
            this.replyWithHeader("&7Timer value reset to &b1&7.");
        } else if (input.argumentCount() == 1 && input.getArgument(0).equalsIgnoreCase("value")) {
            this.replyWithHeader("&7Timer value: &b" + this.formatValue(ModuleManager.timer.getSpeed()));
        } else if (input.argumentCount() == 2 && input.getArgument(0).equalsIgnoreCase("set")) {
            double value;
            try {
                value = Double.parseDouble(input.getArgument(1));
            } catch (NumberFormatException exception) {
                this.replyWithHeader("&7Timer value must be a number.");
                return;
            }
            if (!Double.isFinite(value)) {
                this.replyWithHeader("&7Timer value must be a finite number.");
            } else {
                ModuleManager.timer.setSpeed(value);
                this.replyWithHeader("&7Timer value set to &b" + this.formatValue(ModuleManager.timer.getSpeed()) + "&7.");
            }
        } else {
            this.replyWithHeader("&7Usage: &b" + this.prefixed("timer set <value>") + "&7, &b" + this.prefixed("timer reset") + "&7, or &b" + this.prefixed("timer value") + "&7.");
        }
    }

    @Override
    public List<String> suggest(CommandInput input) {
        return input.argumentCount() <= 1 ? this.filterSuggestions(input, "set", "reset", "value") : super.suggest(input);
    }

    private String formatValue(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
