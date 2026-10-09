package keystrokesmod.module.impl.render;

import java.util.HashSet;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderPlayerEvent.Post;
import net.minecraftforge.client.event.RenderPlayerEvent.Pre;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class Chams extends Module {

    private ButtonSetting ignoreBots;

    private ButtonSetting renderSelf;

    private ButtonSetting hidePlayers;

    private HashSet<Entity> bots = new HashSet<>();

    public Chams() {
        super("Chams", Module.category.render, 0);
        this.liteModule = true;
        this.registerSetting(this.ignoreBots = new ButtonSetting("Ignore bots", false));
        this.registerSetting(this.hidePlayers = new ButtonSetting("Hide players", false));
        this.registerSetting(this.renderSelf = new ButtonSetting("Render self", false));
    }

    @SubscribeEvent
    public void onPreRender(Pre e) {
        Entity entity = e.entity;
        if (entity != mc.field_71439_g || this.renderSelf.isToggled() && mc.field_71462_r == null) {
            if (!this.hidePlayers.isToggled() || entity == mc.field_71439_g && this.renderSelf.isToggled()) {
                if (this.ignoreBots.isToggled()) {
                    if (AntiBot.isBot(entity)) {
                        return;
                    }
                    this.bots.add(entity);
                }
                GL11.glEnable(32823);
                GL11.glPolygonOffset(1.0F, -4000000.0F);
            } else {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onPostRender(Post e) {
        Entity entity = e.entity;
        if (entity != mc.field_71439_g || this.renderSelf.isToggled() && mc.field_71462_r == null) {
            if (!this.hidePlayers.isToggled() || entity == mc.field_71439_g && this.renderSelf.isToggled()) {
                if (this.ignoreBots.isToggled()) {
                    if (!this.bots.contains(entity)) {
                        return;
                    }
                    this.bots.remove(entity);
                }
                GL11.glDisable(32823);
                GL11.glPolygonOffset(1.0F, 4000000.0F);
            }
        }
    }

    @Override
    public void onDisable() {
        this.bots.clear();
    }
}
