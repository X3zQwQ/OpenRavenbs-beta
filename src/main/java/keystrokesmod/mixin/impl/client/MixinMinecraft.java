package keystrokesmod.mixin.impl.client;

import keystrokesmod.event.ClickMouseEvent;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.event.GuiUpdateEvent;
import keystrokesmod.event.KeyEvent;
import keystrokesmod.event.PostMouseSelectionEvent;
import keystrokesmod.event.PostRightClickMouseEvent;
import keystrokesmod.event.PreAttackEvent;
import keystrokesmod.event.PreInputEvent;
import keystrokesmod.event.PrePlayerInteractEvent;
import keystrokesmod.event.PreSlotScrollEvent;
import keystrokesmod.event.RightClickDelayTickEvent;
import keystrokesmod.event.RightClickMouseEvent;
import keystrokesmod.event.RunGameLoopEvent;
import keystrokesmod.event.SlotUpdateEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.Timer;
import keystrokesmod.module.impl.player.BedAura;
import keystrokesmod.module.impl.player.FastMine;
import keystrokesmod.module.impl.render.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;getMouseOver(F)V", shift = At.Shift.BEFORE))
    public void onBeforeGetMouseOver(CallbackInfo ci) {
        RotationHelper.get().updateServerRotations();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;getMouseOver(F)V", shift = At.Shift.AFTER))
    public void onRunTickMouseOver(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PostMouseSelectionEvent());
    }

    @Inject(method = "runTick", at = @At(value = "FIELD", opcode = 180, target = "Lnet/minecraft/client/settings/GameSettings;chatVisibility:Lnet/minecraft/entity/player/EntityPlayer$EnumChatVisibility;"))
    private void injectBeforeChatVisibility(CallbackInfo ci) {
        Timer.flushPendingActions();
        MinecraftForge.EVENT_BUS.post(new PrePlayerInteractEvent());
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V"))
    private void injectBeforeDispatchKeypresses(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new KeyEvent(Keyboard.getKeyName(Keyboard.getEventKey()), Keyboard.getEventKey(), Keyboard.getEventKeyState(), Minecraft.func_71410_x().field_71462_r != null));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V", ordinal = 2))
    private void onRunTick(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PreInputEvent());
    }

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    public void onRunGameLoop(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new RunGameLoopEvent());
    }

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    public void injectClickMouse(CallbackInfo ci) {
        if (!Timer.allowAttack()) {
            ci.cancel();
        } else {
            Minecraft mc = (Minecraft) (Object) this;
            MovingObjectPosition mop = mc.field_71476_x;
            PreAttackEvent preAttack = new PreAttackEvent(mop);
            MinecraftForge.EVENT_BUS.post(preAttack);
            if (preAttack.isCanceled()) {
                ci.cancel();
            } else {
                MinecraftForge.EVENT_BUS.post(new ClickMouseEvent());
            }
        }
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    public void injectRightClickMouse(CallbackInfo ci) {
        if (!Timer.allowUse()) {
            ci.cancel();
        } else {
            RightClickMouseEvent event = new RightClickMouseEvent();
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    public void onRunTickStart(CallbackInfo ci) {
        Timer.beginClientTick();
        MinecraftForge.EVENT_BUS.post(new GameTickEvent());
    }

    @Inject(method = "sendClickBlockToController", at = @At("HEAD"), cancellable = true)
    private void raven$gateBlockAttack(boolean attack, CallbackInfo ci) {
        if (attack && !Timer.shouldProcessPlayerActions()) {
            ci.cancel();
        }
    }

    @Inject(method = "rightClickMouse", at = @At("RETURN"))
    public void postRightClickMouse(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PostRightClickMouseEvent());
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", ordinal = 0, shift = At.Shift.BEFORE))
    public void onRunTickAfterRightClickDelay(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new RightClickDelayTickEvent());
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;updateController()V"))
    private void raven$timerUpdateController(PlayerControllerMP controller) {
        for (int i = 0; i < Timer.getWorldUpdateCount(); i++) {
            controller.func_78765_e();
        }
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;updateRenderer()V"))
    private void raven$timerUpdateRenderer(EntityRenderer renderer) {
        for (int i = 0; i < Timer.getWorldUpdateCount(); i++) {
            renderer.func_78464_a();
        }
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;updateClouds()V"))
    private void raven$timerUpdateClouds(RenderGlobal renderer) {
        for (int i = 0; i < Timer.getWorldUpdateCount(); i++) {
            renderer.func_72734_e();
        }
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;updateEntities()V"))
    private void raven$timerUpdateEntities(WorldClient world) {
        for (int i = 0; i < Timer.getWorldUpdateCount(); i++) {
            world.func_72939_s();
        }
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;tick()V"))
    private void raven$timerTickWorld(WorldClient world) {
        for (int i = 0; i < Timer.getWorldUpdateCount(); i++) {
            world.func_72835_b();
        }
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/EffectRenderer;updateEffects()V"))
    private void raven$timerUpdateEffects(EffectRenderer renderer) {
        for (int i = 0; i < Timer.getWorldUpdateCount(); i++) {
            renderer.func_78868_a();
        }
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;sendClickBlockToController(Z)V", shift = At.Shift.AFTER))
    private void raven$fastMinePassiveBlockHitDelay(CallbackInfo ci) {
        BedAura bedAura = ModuleManager.bedAura;
        if (bedAura == null || !bedAura.shouldOverrideFastMine()) {
            FastMine fm = ModuleManager.fastMine;
            if (fm != null) {
                fm.tickPassiveBlockHitDecay((Minecraft) (Object) this);
            }
        }
    }

    @Inject(method = "displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", at = @At("HEAD"))
    public void onDisplayGuiScreen(GuiScreen guiScreen, CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;
        GuiScreen previousGui = mc.field_71462_r;
        GuiScreen setGui = guiScreen;
        boolean opened = guiScreen != null;
        if (!opened) {
            setGui = previousGui;
        }
        GuiUpdateEvent event = new GuiUpdateEvent(setGui, opened);
        MinecraftForge.EVENT_BUS.post(event);
    }

    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;changeCurrentItem(I)V"))
    public void changeCurrentItem(InventoryPlayer inventoryPlayer, int slot) {
        PreSlotScrollEvent event = new PreSlotScrollEvent(slot, inventoryPlayer.field_70461_c);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled()) {
            inventoryPlayer.func_70453_c(slot);
        }
    }

    @Redirect(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;thirdPersonView:I", opcode = 181))
    private void onSetThirdPersonView(GameSettings gameSettings, int value) {
        if (ModuleManager.freelook != null && Freelook.perspectiveToggled) {
            ModuleManager.freelook.resetPerspective();
        } else {
            gameSettings.field_74320_O = value;
        }
    }

    @Redirect(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/InventoryPlayer;currentItem:I", opcode = 181))
    private void onSetCurrentItem(InventoryPlayer inventoryPlayer, int slot) {
        SlotUpdateEvent e = new SlotUpdateEvent(slot);
        MinecraftForge.EVENT_BUS.post(e);
        if (!e.isCanceled()) {
            inventoryPlayer.field_70461_c = slot;
        }
    }
}
