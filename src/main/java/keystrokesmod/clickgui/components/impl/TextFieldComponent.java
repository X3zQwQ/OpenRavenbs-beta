package keystrokesmod.clickgui.components.impl;

import keystrokesmod.module.setting.impl.TextSetting;

public class TextFieldComponent extends AbstractTextInputComponent {

    public final TextSetting textSetting;

    private String valueWhenFocused;

    public TextFieldComponent(TextSetting textSetting, ModuleComponent moduleComponent, float o) {
        super(moduleComponent, o, textSetting.getPlaceholder(), textSetting.getMaxLength());
        this.textSetting = textSetting;
        this.getTextField().setText(textSetting.getText());
    }

    @Override
    public void render() {
        if (!this.isTextFieldFocused()) {
            if (this.valueWhenFocused != null) {
                this.revertToSaved();
            }
            if (!this.getTextField().getText().equals(this.textSetting.getText())) {
                this.getTextField().setText(this.textSetting.getText());
            }
        }
        this.renderTextInput(this.textSetting.getName());
    }

    @Override
    public boolean onClick(int mouseX, int mouseY, int button) {
        if (this.moduleComponent.isOpened && this.moduleComponent.isVisible(this) && button == 0) {
            AbstractTextInputComponent.Layout layout = this.layout(true);
            if (this.isTextFieldClicked(mouseX, mouseY, layout)) {
                if (!this.isTextFieldFocused()) {
                    this.valueWhenFocused = this.textSetting.getText();
                }
                this.setTextFieldFocused(true);
                return true;
            } else {
                if (this.isTextFieldFocused()) {
                    this.valueWhenFocused = null;
                    this.setTextFieldFocused(false);
                }
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.moduleComponent.isOpened && this.isTextFieldFocused()) {
            if (keyCode == 1) {
                this.revertToSaved();
                this.setTextFieldFocused(false);
            } else if (keyCode != 28 && keyCode != 156) {
                if (this.getTextField().textboxKeyTyped(typedChar, keyCode)) {
                    this.textSetting.setText(this.getTextField().getText());
                }
            } else {
                this.textSetting.submit();
                this.valueWhenFocused = null;
                this.getTextField().setText(this.textSetting.getText());
                this.setTextFieldFocused(false);
            }
        }
    }

    @Override
    public boolean isBaseVisible() {
        return this.textSetting.visible;
    }

    @Override
    public String getGroupName() {
        return this.textSetting.group != null ? this.textSetting.group.getName() : "";
    }

    @Override
    public boolean containsClick(int mouseX, int mouseY) {
        return this.isTextFieldClicked(mouseX, mouseY, this.layout(true));
    }

    private void revertToSaved() {
        if (this.valueWhenFocused != null) {
            this.getTextField().setText(this.valueWhenFocused);
            this.textSetting.setText(this.valueWhenFocused);
            this.valueWhenFocused = null;
        }
    }
}
