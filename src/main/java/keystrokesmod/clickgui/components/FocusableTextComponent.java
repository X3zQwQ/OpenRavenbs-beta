package keystrokesmod.clickgui.components;

public interface FocusableTextComponent {

    boolean isTextInputFocused();

    void unfocusTextInput();

    boolean containsClick(int mouseX, int mouseY);
}
