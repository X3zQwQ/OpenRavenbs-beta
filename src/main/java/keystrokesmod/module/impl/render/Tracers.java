package keystrokesmod.module.impl.render;

import java.util.ArrayList;
import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.ColorSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class Tracers extends Module {

    public ButtonSetting showInvis;

    public ColorSetting color;

    public ButtonSetting rainbow;

    public SliderSetting lineWidth;

    private boolean viewBobbingEnabled;

    private final ArrayList<Entity> trackedEntities = new ArrayList<>();

    private int trackedEntityCount = 0;

    public Tracers() {
        super("Tracers", Module.category.render);
        this.registerSetting(this.showInvis = new ButtonSetting("Show invis", true));
        this.registerSetting(this.lineWidth = new SliderSetting("Line Width", 1.0, 1.0, 5.0, 1.0));
        this.registerSetting(this.color = new ColorSetting("Color", 0, 255, 0));
        this.registerSetting(this.rainbow = new ButtonSetting("Rainbow", false));
    }

    @Override
    public void onEnable() {
        this.viewBobbingEnabled = mc.field_71474_y.field_74336_f;
        if (this.viewBobbingEnabled) {
            mc.field_71474_y.field_74336_f = false;
        }
    }

    @Override
    public void onDisable() {
        mc.field_71474_y.field_74336_f = this.viewBobbingEnabled;
    }

    @Override
    public void onUpdate() {
        if (mc.field_71474_y.field_74336_f) {
            mc.field_71474_y.field_74336_f = false;
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            this.updateTrackedEntities();
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        if (Utils.nullCheck() && this.trackedEntityCount != 0) {
            int rgb = this.rainbow.isToggled() ? Utils.getChroma(2L, 0L) : this.color.getColor();
            for (int i = 0; i < this.trackedEntityCount; i++) {
                Entity entity = this.trackedEntities.get(i);
                if (entity != null) {
                    RenderUtils.drawTracerLine(entity, rgb, (float) this.lineWidth.getInput(), e.partialTicks);
                }
            }
        }
    }

    private void updateTrackedEntities() {
        this.trackedEntityCount = 0;
        if (Utils.nullCheck() && mc.field_71441_e != null) {
            if (Raven.DEBUG) {
                for (Entity entity : mc.field_71441_e.field_72996_f) {
                    if (entity instanceof EntityLivingBase && entity != mc.field_71439_g) {
                        this.addTrackedEntity(entity);
                    }
                }
            } else {
                for (EntityPlayer player : mc.field_71441_e.field_73010_i) {
                    if (player != mc.field_71439_g && player.field_70725_aQ == 0 && (this.showInvis.isToggled() || !player.func_82150_aj()) && !AntiBot.isBot(player)) {
                        this.addTrackedEntity(player);
                    }
                }
            }
        }
    }

    private void addTrackedEntity(Entity entity) {
        if (this.trackedEntityCount >= this.trackedEntities.size()) {
            this.trackedEntities.add(entity);
        } else {
            this.trackedEntities.set(this.trackedEntityCount, entity);
        }
        this.trackedEntityCount++;
    }
}
