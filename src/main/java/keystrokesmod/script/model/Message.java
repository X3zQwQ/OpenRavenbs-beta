package keystrokesmod.script.model;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.utility.Utils;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent.Action;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

public class Message {

    public ChatComponentText component;

    public Message(String message) {
        this.component = new ChatComponentText(message);
    }

    public void appendStyle(String style, String action, String styleMessage, String message) {
        ChatStyle chatStyle = new ChatStyle();
        if (style.equals("HOVER")) {
            chatStyle.func_150209_a(new HoverEvent(Utils.getEnum(Action.class, action), new ChatComponentText(styleMessage)));
        } else if (style.equals("CLICK")) {
            chatStyle.func_150241_a(new ClickEvent(Utils.getEnum(net.minecraft.event.ClickEvent.Action.class, action), styleMessage));
        }
        this.component.func_150257_a(new ChatComponentText(message).func_150255_a(chatStyle));
    }

    public void append(String append) {
        this.component.func_150257_a(new ChatComponentText(append));
    }

    public List<Message> getSiblings() {
        List<Message> siblings = new ArrayList<>();
        for (IChatComponent sibling : this.component.func_150253_a()) {
            siblings.add(new Message(sibling.func_150261_e()));
        }
        return siblings;
    }

    public String getStyle() {
        return this.component.func_150256_b().toString();
    }

    public String getText() {
        return this.component.func_150261_e();
    }

    @Override
    public String toString() {
        return "TextComponent{text='" + this.component.func_150261_e() + '\'' + ", siblings=" + this.component.func_150253_a() + ", style=" + this.component.func_150256_b() + '}';
    }
}
