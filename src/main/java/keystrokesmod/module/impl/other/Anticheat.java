package keystrokesmod.module.impl.other;

import java.util.HashMap;
import java.util.UUID;
import keystrokesmod.event.AntiCheatFlagEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.PlayerData;
import keystrokesmod.utility.Utils;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.event.ClickEvent;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Anticheat extends Module {

    private SliderSetting interval;

    private ButtonSetting enemyAdd;

    private ButtonSetting autoReport;

    private ButtonSetting ignoreTeammates;

    private ButtonSetting atlasSuspect;

    private ButtonSetting shouldPing;

    private ButtonSetting autoBlock;

    private ButtonSetting noFall;

    private ButtonSetting noSlow;

    private ButtonSetting scaffold;

    private ButtonSetting legitScaffold;

    private HashMap<UUID, HashMap<ButtonSetting, Long>> flags = new HashMap<>();

    private HashMap<UUID, PlayerData> players = new HashMap<>();

    private long lastAlert;

    private long lastClientBoundPacket;

    public Anticheat() {
        super("Anticheat", Module.category.other);
        this.liteModule = true;
        this.registerSetting(new DescriptionSetting("Tries to detect cheaters."));
        this.registerSetting(this.interval = new SliderSetting("Flag interval", " second", 20.0, 0.0, 60.0, 1.0));
        this.registerSetting(this.enemyAdd = new ButtonSetting("Add cheaters as enemy", false));
        this.registerSetting(this.autoReport = new ButtonSetting("Auto report", false));
        this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", false));
        this.registerSetting(this.atlasSuspect = new ButtonSetting("Only atlas suspect", false));
        this.registerSetting(this.shouldPing = new ButtonSetting("Should ping", true));
        this.registerSetting(new DescriptionSetting("Detected cheats"));
        this.registerSetting(this.autoBlock = new ButtonSetting("Autoblock", true));
        this.registerSetting(this.noFall = new ButtonSetting("NoFall", true));
        this.registerSetting(this.noSlow = new ButtonSetting("NoSlow", true));
        this.registerSetting(this.scaffold = new ButtonSetting("Scaffold", true));
        this.registerSetting(this.legitScaffold = new ButtonSetting("Legit scaffold", true));
        this.closetModule = true;
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        this.lastClientBoundPacket = System.currentTimeMillis();
    }

    private void alert(EntityPlayer entityPlayer, ButtonSetting mode) {
        if (!Utils.isFriended(entityPlayer) && (!this.ignoreTeammates.isToggled() || !Utils.isTeammate(entityPlayer))) {
            if (this.atlasSuspect.isToggled()) {
                if (!entityPlayer.func_70005_c_().equals("Suspect§r")) {
                    return;
                }
            } else if (this.enemyAdd.isToggled()) {
                Utils.addEnemy(entityPlayer.func_70005_c_());
            }
            long currentTimeMillis = System.currentTimeMillis();
            if (this.interval.getInput() > 0.0) {
                HashMap<ButtonSetting, Long> hashMap = this.flags.get(entityPlayer.func_110124_au());
                if (hashMap == null) {
                    hashMap = new HashMap<>();
                } else {
                    Long lastFlag = hashMap.get(mode);
                    if (lastFlag != null && Utils.timeBetween(lastFlag, currentTimeMillis) <= this.interval.getInput() * 1000.0) {
                        return;
                    }
                }
                hashMap.put(mode, currentTimeMillis);
                this.flags.put(entityPlayer.func_110124_au(), hashMap);
            }
            ChatComponentText chatComponentText = new ChatComponentText(Utils.formatColor("&7[&dR&7]&r " + entityPlayer.func_145748_c_().func_150260_c() + " &7detected for &d" + mode.getName()));
            ChatStyle chatStyle = new ChatStyle();
            chatStyle.func_150241_a(new ClickEvent(Action.RUN_COMMAND, "/wdr " + entityPlayer.func_70005_c_()));
            chatComponentText.func_150257_a(new ChatComponentText(Utils.formatColor(" §7[§cWDR§7]")).func_150255_a(chatStyle));
            mc.field_71439_g.func_145747_a(chatComponentText);
            this.postAntiCheatFlagEvent(mode.getName(), entityPlayer);
            if (this.shouldPing.isToggled() && Utils.timeBetween(this.lastAlert, currentTimeMillis) >= 1500L) {
                mc.field_71439_g.func_85030_a("note.pling", 1.0F, 1.0F);
                this.lastAlert = currentTimeMillis;
            }
            if (this.autoReport.isToggled() && !Utils.isFriended(entityPlayer)) {
                mc.field_71439_g.func_71165_d("/wdr " + Utils.stripColor(entityPlayer.func_146103_bH().getName()));
            }
        }
    }

    @Override
    public void onUpdate() {
        if (!mc.func_71356_B()) {
            for (EntityPlayer entityPlayer : mc.field_71441_e.field_73010_i) {
                if (entityPlayer != null && entityPlayer != mc.field_71439_g && !AntiBot.isBot(entityPlayer)) {
                    PlayerData data = this.players.get(entityPlayer.func_110124_au());
                    if (data == null) {
                        data = new PlayerData();
                    }
                    data.update(entityPlayer);
                    this.performCheck(entityPlayer, data);
                    data.updateServerPos(entityPlayer);
                    data.updateSneak(entityPlayer);
                    this.players.put(entityPlayer.func_110124_au(), data);
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.field_71439_g) {
            this.players.clear();
            this.flags.clear();
        }
    }

    @Override
    public void onDisable() {
        this.players.clear();
        this.flags.clear();
        this.lastAlert = 0L;
    }

    private void performCheck(EntityPlayer entityPlayer, PlayerData playerData) {
        if (this.autoBlock.isToggled() && playerData.autoBlockTicks >= 10) {
            this.alert(entityPlayer, this.autoBlock);
        } else if (this.legitScaffold.isToggled() && playerData.sneakTicks >= 3) {
            this.alert(entityPlayer, this.legitScaffold);
        } else if (this.noSlow.isToggled() && playerData.noSlowTicks == 11 && playerData.speed >= 0.08) {
            this.alert(entityPlayer, this.noSlow);
        } else {
            if (this.scaffold.isToggled() && entityPlayer.field_82175_bq && entityPlayer.field_70125_A >= 70.0F && entityPlayer.func_70694_bm() != null && entityPlayer.func_70694_bm().func_77973_b() instanceof ItemBlock && playerData.fastTick >= 20 && entityPlayer.field_70173_aa - playerData.lastSneakTick >= 30 && entityPlayer.field_70173_aa - playerData.aboveVoidTicks >= 20) {
                boolean overAir = true;
                BlockPos blockPos = entityPlayer.func_180425_c().func_177979_c(2);
                for (int i = 0; i < 4; i++) {
                    if (!(BlockUtils.getBlock(blockPos) instanceof BlockAir)) {
                        overAir = false;
                        break;
                    }
                    blockPos = blockPos.func_177977_b();
                }
                if (overAir) {
                    this.alert(entityPlayer, this.scaffold);
                    return;
                }
            }
            if (this.noFall.isToggled() && !entityPlayer.field_71075_bZ.field_75100_b && Utils.timeBetween(System.currentTimeMillis(), this.lastClientBoundPacket) <= 150L) {
                double serverPosX = entityPlayer.field_70118_ct / 32;
                double serverPosY = entityPlayer.field_70117_cu / 32;
                double serverPosZ = entityPlayer.field_70116_cv / 32;
                double deltaX = Math.abs(playerData.serverPosX - serverPosX);
                double deltaY = playerData.serverPosY - serverPosY;
                double deltaZ = Math.abs(playerData.serverPosZ - serverPosZ);
                if (deltaY >= 5.0 && deltaX <= 10.0 && deltaZ <= 10.0 && deltaY <= 40.0 && !Utils.overVoid(serverPosX, serverPosY, serverPosZ) && Utils.distanceToGround(entityPlayer) > 3.0 && !Utils.onLadder(entityPlayer) && !entityPlayer.func_70090_H() && !entityPlayer.func_180799_ab()) {
                    this.alert(entityPlayer, this.noFall);
                }
            }
        }
    }

    public void postAntiCheatFlagEvent(String flag, Entity entity) {
        MinecraftForge.EVENT_BUS.post(new AntiCheatFlagEvent(flag, entity));
    }
}
