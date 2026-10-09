package keystrokesmod.script;

import keystrokesmod.Raven;
import keystrokesmod.event.AntiCheatFlagEvent;
import keystrokesmod.event.AttackEvent;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.DispatchPacketEvent;
import keystrokesmod.event.GuiUpdateEvent;
import keystrokesmod.event.KeyEvent;
import keystrokesmod.event.KeyPressEvent;
import keystrokesmod.event.PlayerMoveEvent;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.event.PreAttackEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.script.model.Entity;
import keystrokesmod.script.model.MovementInput;
import keystrokesmod.script.model.PlayerState;
import keystrokesmod.script.model.Vec3;
import keystrokesmod.script.packet.clientbound.SPacket;
import keystrokesmod.script.packet.serverbound.CPacket;
import keystrokesmod.script.packet.serverbound.PacketHandler;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import org.lwjgl.opengl.GL11;

public class ScriptEvents {

    public Module module;

    public ScriptEvents(Module module) {
        this.module = module;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ClientChatReceivedEvent e) {
        if (Utils.nullCheck()) {
            if (!Utils.stripColor(e.message.func_150260_c()).isEmpty()) {
                if (Raven.scriptManager.invokeBoolean("onChat", this.module, e.message.func_150260_c(), e.type) == 0) {
                    e.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onSendPacket(SendPacketEvent e) {
        if (!e.isCanceled() && e.getPacket() != null) {
            if (!e.getPacket().getClass().getSimpleName().startsWith("S")) {
                CPacket packet = PacketHandler.convertServerBound(e.getPacket());
                if (packet != null && Raven.scriptManager.invokeBoolean("onPacketSent", this.module, packet) == 0) {
                    e.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onDispatchPacket(DispatchPacketEvent e) {
        if (e.getPacket() != null) {
            if (!e.getPacket().getClass().getSimpleName().startsWith("S")) {
                CPacket packet = PacketHandler.convertServerBound(e.getPacket());
                Raven.scriptManager.invoke("onDispatchPacket", this.module, packet);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!e.isCanceled() && e.getPacket() != null) {
            SPacket packet = PacketHandler.convertClientBound(e.getPacket());
            if (packet != null && Raven.scriptManager.invokeBoolean("onPacketReceived", this.module, packet) == 0) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreAttack(PreAttackEvent e) {
        if (!e.isCanceled()) {
            if (Raven.scriptManager.invokeBoolean("onPreAttack", this.module) == 0) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(AttackEvent e) {
        if (!e.isCanceled()) {
            Entity target = Entity.convert(e.target);
            Entity attacker = Entity.convert(e.attacker);
            if (Raven.scriptManager.invokeBoolean("onAttackEntity", this.module, target, attacker) == 0) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientRotations(ClientRotationEvent e) {
        Float[] rotations = Raven.scriptManager.invokeFloatArray("getRotations", this.module);
        if (rotations != null && rotations.length != 0 && rotations.length <= 2) {
            if (rotations[0] != null) {
                e.yaw = rotations[0];
                e.scriptRotations = true;
            }
            if (rotations.length == 2 && rotations[1] != null) {
                e.pitch = rotations[1];
                e.scriptRotations = true;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPrePlayerMovementInput(PrePlayerInputEvent e) {
        MovementInput input = new MovementInput(e, (byte) 0);
        Raven.scriptManager.invoke("onPrePlayerInput", this.module, input);
        if (!e.isEquals(input)) {
            e.setForward(input.forward);
            e.setSneak(input.sneak);
            e.setJump(input.jump);
            e.setStrafe(input.strafe);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyTyped(KeyPressEvent e) {
        if (!e.isCanceled()) {
            if (Raven.scriptManager.invokeBoolean("onKeyPress", this.module, e.typedChar, e.keyCode) == 0) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onKeyTyped(KeyEvent e) {
        if (!e.isCanceled()) {
            Raven.scriptManager.invoke("onKey", this.module, e.keyName, e.keyCode, e.state, e.inGui);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        if (Utils.nullCheck()) {
            Minecraft mc = Minecraft.func_71410_x();
            ((IAccessorEntityRenderer) mc.field_71460_t).callSetupCameraTransform(e.partialTicks, 0);
            ScriptDefaults.render.beginWorldToScreen(new ScaledResolution(mc).func_78325_e());
            try {
                Raven.scriptManager.invoke("onRenderWorld", this.module, e.partialTicks);
            } finally {
                ScriptDefaults.render.endWorldToScreen();
                this.restoreWorldRenderState(mc, e.partialTicks);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreUpdate(PreUpdateEvent e) {
        Raven.scriptManager.invoke("onPreUpdate", this.module);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostUpdate(PostUpdateEvent e) {
        Raven.scriptManager.invoke("onPostUpdate", this.module);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderTick(RenderTickEvent e) {
        if (e.phase == Phase.END && Utils.nullCheck()) {
            Minecraft mc = Minecraft.func_71410_x();
            mc.field_71460_t.func_78478_c();
            try {
                Raven.scriptManager.invoke("onRenderTick", this.module, e.renderTickTime);
            } finally {
                this.restoreOverlayRenderState(mc);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAntiCheatFlag(AntiCheatFlagEvent e) {
        Raven.scriptManager.invoke("onAntiCheatFlag", this.module, e.flag, Entity.convert(e.entity));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGuiUpdate(GuiUpdateEvent e) {
        if (e.guiScreen != null) {
            Raven.scriptManager.invoke("onGuiUpdate", this.module, e.guiScreen.getClass().getSimpleName(), e.opened);
        }
    }

    @SubscribeEvent
    public void onDisconnect(ClientDisconnectionFromServerEvent e) {
        Minecraft.func_71410_x().func_152344_a(() -> Raven.scriptManager.invoke("onDisconnect", this.module));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreMotion(PreMotionEvent e) {
        PlayerState playerState = new PlayerState(e, (byte) 0);
        Raven.scriptManager.invoke("onPreMotion", this.module, playerState);
        if (!e.isEquals(playerState)) {
            if (e.getYaw() != playerState.yaw) {
                e.setYaw(playerState.yaw);
            }
            e.setPitch(playerState.pitch);
            e.setPosX(playerState.x);
            e.setPosY(playerState.y);
            e.setPosZ(playerState.z);
            e.setOnGround(playerState.onGround);
            e.setSprinting(playerState.isSprinting);
            e.setSneaking(playerState.isSneaking);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPrePlayerInteract(PrePlayerInteractEvent e) {
        Raven.scriptManager.invoke("onPrePlayerInteract", this.module);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity != null) {
            Raven.scriptManager.invoke("onWorldJoin", this.module, Entity.convert(e.entity));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostInput(PostPlayerInputEvent e) {
        Raven.scriptManager.invoke("onPostPlayerInput", this.module);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostMotion(PostMotionEvent e) {
        Raven.scriptManager.invoke("onPostMotion", this.module);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent e) {
        if (e.button != -1 || e.dwheel != 0) {
            boolean state = e.buttonstate;
            if (e.button == -1) {
                if (e.dwheel > 0) {
                    state = true;
                } else {
                    state = false;
                }
            }
            if (Raven.scriptManager.invokeBoolean("onMouse", this.module, e.button, state, e.x, e.y) == 0) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPreRenderModel(Pre e) {
        if (Utils.nullCheck()) {
            if (Raven.scriptManager.invokeBoolean("onPreRenderModel", this.module, Entity.convert(e.entity)) == 0) {
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPostRenderModel(Post e) {
        if (Utils.nullCheck()) {
            Raven.scriptManager.invoke("onPostRenderModel", this.module, Entity.convert(e.entity));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onScreenRender(net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post event) {
        Raven.scriptManager.invoke("onScreenRender", this.module, event.gui == null ? "" : event.gui.getClass().getSimpleName(), event.mouseX, event.mouseY, event.renderPartialTicks);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent e) {
        Raven.scriptManager.invoke("onPlayerMove", this.module, new Vec3(e.x, e.y, e.z));
    }

    private void restoreWorldRenderState(Minecraft mc, float partialTicks) {
        ((IAccessorEntityRenderer) mc.field_71460_t).callSetupCameraTransform(((IAccessorMinecraft) mc).getTimer().field_74281_c, 0);
        this.restoreCommonRenderState();
        GlStateManager.func_179103_j(7424);
        GlStateManager.func_179126_j();
        GlStateManager.func_179132_a(true);
        GlStateManager.func_179089_o();
        GlStateManager.func_179084_k();
        GlStateManager.func_179106_n();
        RenderHelper.func_74518_a();
    }

    private void restoreOverlayRenderState(Minecraft mc) {
        mc.field_71460_t.func_78478_c();
        this.restoreCommonRenderState();
        GlStateManager.func_179097_i();
        GlStateManager.func_179132_a(true);
        GlStateManager.func_179140_f();
        GlStateManager.func_179084_k();
        RenderHelper.func_74518_a();
    }

    private void restoreCommonRenderState() {
        GlStateManager.func_179117_G();
        GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.func_179135_a(true, true, true, true);
        GlStateManager.func_179098_w();
        GlStateManager.func_179141_d();
        GlStateManager.func_179092_a(516, 0.1F);
        GlStateManager.func_179120_a(770, 771, 1, 0);
        GL11.glLineWidth(1.0F);
        GL11.glDisable(2848);
        GL11.glDisable(3089);
    }
}
