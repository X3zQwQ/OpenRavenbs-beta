package keystrokesmod.module.impl.other;

import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class Debug extends Module {

    private ButtonSetting debugBlock;

    public ButtonSetting debugBPS;

    public ButtonSetting debugEntities;

    private ButtonSetting debugAttacked;

    private ButtonSetting debugHeld;

    private ButtonSetting alertPost;

    private ButtonSetting spawnDummy;

    private static final int HELD_MAX_LINE_WIDTH = 300;

    private static final int HELD_LINE_HEIGHT = 10;

    private final ArrayDeque<String> postQueue = new ArrayDeque<>();

    public EntityOtherPlayerMP fakeEntity = null;

    private boolean sentFlying;

    public Debug() {
        super("Debug", Module.category.other);
        this.registerSetting(this.spawnDummy = new ButtonSetting("Spawn dummy", true));
        this.registerSetting(new DescriptionSetting("Visuals"));
        this.registerSetting(this.alertPost = new ButtonSetting("Alert post", false));
        this.registerSetting(this.debugBlock = new ButtonSetting("Debug block", true));
        this.registerSetting(this.debugHeld = new ButtonSetting("Debug held", false));
        this.registerSetting(this.debugBPS = new ButtonSetting("Show speed", true));
        this.registerSetting(this.debugEntities = new ButtonSetting("Render all entities", false));
        this.registerSetting(this.debugAttacked = new ButtonSetting("Debug attacked", true));
    }

    @Override
    public void onDisable() {
        this.resetState();
        if (this.fakeEntity != null) {
            mc.field_71441_e.func_72900_e(this.fakeEntity);
            this.fakeEntity = null;
        }
    }

    @Override
    public void onEnable() {
        if (this.spawnDummy.isToggled()) {
            this.fakeEntity = new EntityOtherPlayerMP(mc.field_71441_e, new GameProfile(mc.field_71439_g.func_110124_au(), "Dummy"));
            this.fakeEntity.func_82149_j(mc.field_71439_g);
            mc.field_71441_e.func_73027_a(-8008, this.fakeEntity);
            this.fakeEntity.field_71071_by.field_70460_b[0] = new ItemStack(Items.field_151169_ag);
            this.fakeEntity.func_70062_b(0, new ItemStack(Blocks.field_150325_L));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSendPacket(SendPacketEvent e) {
        if (!this.alertPost.isToggled()) {
            this.resetState();
        } else if (!Utils.nullCheck()) {
            this.resetState();
        } else if (!e.isCanceled()) {
            Packet<?> packet = e.getPacket();
            if (packet != null) {
                if (isTickPacket(packet)) {
                    this.postQueue.clear();
                    this.sentFlying = true;
                } else if (isTransactionPacket(packet)) {
                    if (this.sentFlying && !this.postQueue.isEmpty()) {
                        Utils.sendMessage("&7Post packet: &b" + this.postQueue.peekFirst());
                    }
                    this.postQueue.clear();
                    this.sentFlying = false;
                } else {
                    if (this.sentFlying && isPostCheckPacket(packet)) {
                        this.postQueue.add(packet.getClass().getSimpleName());
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent ev) {
        if (ev.phase == Phase.END && Utils.nullCheck()) {
            if (this.debugBlock.isToggled()) {
                this.renderBlockDebug();
            }
            if (this.debugHeld.isToggled()) {
                this.renderHeldDebug();
            }
        }
    }

    private void renderBlockDebug() {
        MovingObjectPosition mouse = RotationUtils.rayCast(mc.field_71442_b.func_78757_d(), mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A, false);
        if (mouse != null && mouse.field_72313_a == MovingObjectType.BLOCK && mouse.func_178782_a() != null) {
            BlockPos pos = mouse.func_178782_a();
            Block block = BlockUtils.getBlock(pos);
            if (block != null && block != Blocks.field_150350_a) {
                IBlockState state = mc.field_71441_e.func_180495_p(pos);
                mc.field_71466_p.func_175063_a("§7BlockPos: §b" + pos.func_177958_n() + "§7, §b" + pos.func_177956_o() + "§7, §b" + pos.func_177952_p(), 30.0F, 20.0F, -1);
                mc.field_71466_p.func_175063_a("§7HitVec: §b" + Utils.round(mouse.field_72307_f.field_72450_a, 3) + "§7, §b" + Utils.round(mouse.field_72307_f.field_72448_b, 3) + "§7, §b" + Utils.round(mouse.field_72307_f.field_72449_c, 3), 30.0F, 30.0F, -1);
                mc.field_71466_p.func_175063_a("§7Face: §b" + mouse.field_178784_b.name(), 30.0F, 40.0F, -1);
                mc.field_71466_p.func_175063_a("§7Unlocalized Name: §b" + block.func_149739_a(), 30.0F, 50.0F, -1);
                mc.field_71466_p.func_175063_a("§7Registry Name: §b" + block.getRegistryName(), 30.0F, 60.0F, -1);
                int y = 70;
                for (IProperty<?> property : block.func_176194_O().func_177623_d()) {
                    Class<?> valueClass = property.func_177699_b();
                    String propName = property.func_177701_a();
                    Object currentValue = state.func_177229_b(property);
                    Collection<?> allowedValues = property.func_177700_c();
                    mc.field_71466_p.func_175063_a("§7Property Name: §b" + propName, 30.0F, y, -1);
                    y += 10;
                    mc.field_71466_p.func_175063_a("§7Value Type: §b" + valueClass.getName(), 30.0F, y, -1);
                    y += 10;
                    if (currentValue != null) {
                        mc.field_71466_p.func_175063_a("§7Current Value: §b" + currentValue, 30.0F, y, -1);
                        y += 10;
                        if (valueClass.isEnum()) {
                            Enum<?> enumValue = (Enum<?>) currentValue;
                            mc.field_71466_p.func_175063_a("§7Current Enum Name: §b" + enumValue.name(), 30.0F, y, -1);
                            y += 10;
                            mc.field_71466_p.func_175063_a("§7Current Enum Ordinal: §b" + enumValue.ordinal(), 30.0F, y, -1);
                            y += 10;
                        }
                    }
                    mc.field_71466_p.func_175063_a("§7Allowed Values: §b" + allowedValues, 30.0F, y, -1);
                    y += 10;
                    if (valueClass.isEnum()) {
                        Object[] enumConstants = valueClass.getEnumConstants();
                        if (Arrays.toString(enumConstants).equals(allowedValues.toString())) {
                            break;
                        }
                        mc.field_71466_p.func_175063_a("§7Enum Constants: §b" + Arrays.toString(enumConstants), 30.0F, y, -1);
                        y += 10;
                    }
                    y += 5;
                }
            }
        }
    }

    private void renderHeldDebug() {
        ItemStack stack = mc.field_71439_g.func_70694_bm();
        if (stack != null) {
            List<String> lines = this.buildHeldDebugLines(stack);
            ScaledResolution resolution = new ScaledResolution(mc);
            int width = 0;
            for (String line : lines) {
                width = Math.max(width, mc.field_71466_p.func_78256_a(line));
            }
            int x = Math.max(30, resolution.func_78326_a() - width - 30);
            int y = 20;
            for (String line : lines) {
                mc.field_71466_p.func_175063_a(line, x, y, -1);
                y += 10;
            }
        }
    }

    private List<String> buildHeldDebugLines(ItemStack stack) {
        List<String> lines = new ArrayList<>();
        Item item = stack.func_77973_b();
        this.addHeldLine(lines, "§7Display Name: §b" + stack.func_82833_r());
        this.addHeldLine(lines, "§7Registry Name: §b" + Item.field_150901_e.func_177774_c(item));
        this.addHeldLine(lines, "§7Numeric ID: §b" + Item.func_150891_b(item));
        this.addHeldLine(lines, "§7Metadata: §b" + stack.func_77960_j());
        this.addHeldLine(lines, "§7Unlocalized Name: §b" + stack.func_77977_a());
        this.addHeldLine(lines, "§7Class: §b" + item.getClass().getName());
        this.addHeldLine(lines, "§7Held Slot: §b" + (mc.field_71439_g.field_71071_by.field_70461_c + 1));
        this.addHeldLine(lines, "§7Stack Size: §b" + stack.field_77994_a + "§7/§b" + stack.func_77976_d());
        this.addHeldLine(lines, "§7Rarity: §b" + stack.func_77953_t().field_77934_f);
        this.addHeldLine(lines, "§7Damage: §b" + stack.func_77952_i() + "§7/§b" + stack.func_77958_k());
        this.addHeldLine(lines, "§7Damageable: §b" + stack.func_77984_f());
        if (stack.func_77984_f()) {
            this.addHeldLine(lines, "§7Durability Remaining: §b" + (stack.func_77958_k() - stack.func_77952_i()));
        }
        this.addHeldLine(lines, "§7Repair Cost: §b" + stack.func_82838_A());
        this.addHeldLine(lines, "§7Enchanted: §b" + stack.func_77948_v());
        this.addHeldLine(lines, "§7Enchantability: §b" + item.getItemEnchantability(stack));
        this.addHeldLine(lines, "§7Has Effect: §b" + stack.func_77962_s());
        this.addHeldLine(lines, "§7Use Action: §b" + stack.func_77975_n());
        this.addHeldLine(lines, "§7Max Use Duration: §b" + stack.func_77988_m());
        Multimap<String, AttributeModifier> modifiers = stack.func_111283_C();
        this.addHeldLine(lines, "§7Attribute Modifiers: §b" + modifiers.size());
        if (modifiers.isEmpty()) {
            this.addHeldLine(lines, "§7Attributes: §bNone");
        } else {
            for (Entry<String, AttributeModifier> entry : modifiers.entries()) {
                AttributeModifier modifier = entry.getValue();
                this.addHeldLine(lines, "§7Attribute Name: §b" + entry.getKey());
                this.addHeldLine(lines, "§7Modifier Name: §b" + modifier.func_111166_b());
                this.addHeldLine(lines, "§7Amount: §b" + Utils.round(modifier.func_111164_d(), 6));
                this.addHeldLine(lines, "§7Operation: §b" + modifier.func_111169_c());
                this.addHeldLine(lines, "§7UUID: §b" + modifier.func_111167_a());
            }
        }
        this.addHeldLine(lines, "§7NBT: §b" + (stack.func_77942_o() ? stack.func_77978_p() : "None"));
        return lines;
    }

    private void addHeldLine(List<String> lines, String line) {
        List<String> wrapped = mc.field_71466_p.func_78271_c(line, 300);
        if (wrapped.isEmpty()) {
            lines.add("");
        } else {
            lines.addAll(wrapped);
        }
    }

    public boolean shouldDebugAttacked() {
        return this.debugAttacked.isToggled();
    }

    private static boolean isPostCheckPacket(Packet<?> packet) {
        return packet instanceof C13PacketPlayerAbilities || packet instanceof C09PacketHeldItemChange || packet instanceof C02PacketUseEntity || packet instanceof C08PacketPlayerBlockPlacement || packet instanceof C07PacketPlayerDigging || packet instanceof C0APacketAnimation || packet instanceof C0EPacketClickWindow || packet instanceof C0BPacketEntityAction;
    }

    private static boolean isTickPacket(Packet<?> packet) {
        return packet instanceof C03PacketPlayer;
    }

    private static boolean isTransactionPacket(Packet<?> packet) {
        return packet instanceof C0FPacketConfirmTransaction;
    }

    private void resetState() {
        this.postQueue.clear();
        this.sentFlying = false;
    }
}
