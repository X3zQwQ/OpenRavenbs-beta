package keystrokesmod.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import keystrokesmod.Raven;
import keystrokesmod.module.impl.client.ChatCommands;
import keystrokesmod.module.impl.client.CommandLine;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.impl.client.Relationships;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.combat.AimAssist;
import keystrokesmod.module.impl.combat.AntiKnockback;
import keystrokesmod.module.impl.combat.AutoClicker;
import keystrokesmod.module.impl.combat.Autoblock;
import keystrokesmod.module.impl.combat.ClickAssist;
import keystrokesmod.module.impl.combat.Displace;
import keystrokesmod.module.impl.combat.HitBox;
import keystrokesmod.module.impl.combat.HitSelect;
import keystrokesmod.module.impl.combat.JumpReset;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.combat.KnockbackDelay;
import keystrokesmod.module.impl.combat.Piercing;
import keystrokesmod.module.impl.combat.RawInput;
import keystrokesmod.module.impl.combat.Reach;
import keystrokesmod.module.impl.combat.Reduce;
import keystrokesmod.module.impl.combat.RodAimbot;
import keystrokesmod.module.impl.combat.TPAura;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.impl.combat.WTap;
import keystrokesmod.module.impl.minigames.AutoRequeue;
import keystrokesmod.module.impl.minigames.AutoWho;
import keystrokesmod.module.impl.minigames.BedWars;
import keystrokesmod.module.impl.minigames.BridgeInfo;
import keystrokesmod.module.impl.minigames.DuelsStats;
import keystrokesmod.module.impl.minigames.MurderMystery;
import keystrokesmod.module.impl.minigames.SkyWars;
import keystrokesmod.module.impl.minigames.SpeedBuilders;
import keystrokesmod.module.impl.minigames.SumoFences;
import keystrokesmod.module.impl.minigames.WoolWars;
import keystrokesmod.module.impl.movement.BHop;
import keystrokesmod.module.impl.movement.Boost;
import keystrokesmod.module.impl.movement.Dolphin;
import keystrokesmod.module.impl.movement.Fly;
import keystrokesmod.module.impl.movement.InstantStop;
import keystrokesmod.module.impl.movement.InvMove;
import keystrokesmod.module.impl.movement.Jump45;
import keystrokesmod.module.impl.movement.KeepSprint;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.movement.MovementFix;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.module.impl.movement.NullMove;
import keystrokesmod.module.impl.movement.Speed;
import keystrokesmod.module.impl.movement.Sprint;
import keystrokesmod.module.impl.movement.Stasis;
import keystrokesmod.module.impl.movement.StopMotion;
import keystrokesmod.module.impl.movement.Teleport;
import keystrokesmod.module.impl.movement.Timer;
import keystrokesmod.module.impl.movement.VClip;
import keystrokesmod.module.impl.network.Backtrack;
import keystrokesmod.module.impl.network.Blink;
import keystrokesmod.module.impl.network.FakeLag;
import keystrokesmod.module.impl.network.Freeze;
import keystrokesmod.module.impl.network.LagRange;
import keystrokesmod.module.impl.network.Limbo;
import keystrokesmod.module.impl.other.Anticheat;
import keystrokesmod.module.impl.other.ChatBypass;
import keystrokesmod.module.impl.other.Debug;
import keystrokesmod.module.impl.other.FakeChat;
import keystrokesmod.module.impl.other.HitParticles;
import keystrokesmod.module.impl.other.LatencyAlerts;
import keystrokesmod.module.impl.other.NameHider;
import keystrokesmod.module.impl.other.ViewPackets;
import keystrokesmod.module.impl.player.AntiAFK;
import keystrokesmod.module.impl.player.AntiFireball;
import keystrokesmod.module.impl.player.AutoBlockin;
import keystrokesmod.module.impl.player.AutoJump;
import keystrokesmod.module.impl.player.AutoLadder;
import keystrokesmod.module.impl.player.AutoSwap;
import keystrokesmod.module.impl.player.AutoTool;
import keystrokesmod.module.impl.player.BedAura;
import keystrokesmod.module.impl.player.BridgeAssist;
import keystrokesmod.module.impl.player.Clutch;
import keystrokesmod.module.impl.player.DelayRemover;
import keystrokesmod.module.impl.player.FastMine;
import keystrokesmod.module.impl.player.FastPlace;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.impl.player.GhostHand;
import keystrokesmod.module.impl.player.HideWindow;
import keystrokesmod.module.impl.player.InvManager;
import keystrokesmod.module.impl.player.NoFall;
import keystrokesmod.module.impl.player.NoRotate;
import keystrokesmod.module.impl.player.SafeWalk;
import keystrokesmod.module.impl.player.Scaffold;
import keystrokesmod.module.impl.player.WaterBucket;
import keystrokesmod.module.impl.render.AntiDebuff;
import keystrokesmod.module.impl.render.AntiShuffle;
import keystrokesmod.module.impl.render.Arrows;
import keystrokesmod.module.impl.render.BedESP;
import keystrokesmod.module.impl.render.BlockESP;
import keystrokesmod.module.impl.render.BlockOverlay;
import keystrokesmod.module.impl.render.BreakProgress;
import keystrokesmod.module.impl.render.Chams;
import keystrokesmod.module.impl.render.ChestESP;
import keystrokesmod.module.impl.render.DamageTags;
import keystrokesmod.module.impl.render.DamageTint;
import keystrokesmod.module.impl.render.ExtendCamera;
import keystrokesmod.module.impl.render.ExtraBobbing;
import keystrokesmod.module.impl.render.FallView;
import keystrokesmod.module.impl.render.Freelook;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.impl.render.Holdlook;
import keystrokesmod.module.impl.render.Indicators;
import keystrokesmod.module.impl.render.ItemESP;
import keystrokesmod.module.impl.render.ItemPhysics;
import keystrokesmod.module.impl.render.MobESP;
import keystrokesmod.module.impl.render.Nametags;
import keystrokesmod.module.impl.render.NoCameraClip;
import keystrokesmod.module.impl.render.NoHurtCam;
import keystrokesmod.module.impl.render.PlayerESP;
import keystrokesmod.module.impl.render.PotionHUD;
import keystrokesmod.module.impl.render.Radar;
import keystrokesmod.module.impl.render.Saturation;
import keystrokesmod.module.impl.render.TNTTimer;
import keystrokesmod.module.impl.render.TargetHUD;
import keystrokesmod.module.impl.render.Tracers;
import keystrokesmod.module.impl.render.Trajectories;
import keystrokesmod.module.impl.render.Xray;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.impl.world.Weather;
import keystrokesmod.script.Manager;
import keystrokesmod.utility.font.RavenFontRenderer;

public class ModuleManager {

    public static List<Module> modules = new ArrayList<>();

    public static List<Module> organizedModules = Collections.synchronizedList(new ArrayList<>());

    private static final Map<String, Module> modulesByName = new HashMap<>();

    private static final Map<String, Module> modulesByNormalizedName = new HashMap<>();

    private static final Map<Class<?>, Module> modulesByClass = new HashMap<>();

    public static NameHider nameHider;

    public static FastPlace fastPlace;

    public static MurderMystery murderMystery;

    public static InvMove invmove;

    public static SkyWars skyWars;

    public static AntiFireball antiFireball;

    public static BedAura bedAura;

    public static FastMine fastMine;

    public static AntiShuffle antiShuffle;

    public static AimAssist aimAssist;

    public static MovementFix movementFix;

    public static CommandLine commandLine;

    public static LongJump longJump;

    public static AntiBot antiBot;

    public static NoSlow noSlow;

    public static KillAura killAura;

    public static AutoClicker autoClicker;

    public static HitSelect hitSelect;

    public static KnockbackDelay knockbackDelay;

    public static HitBox hitBox;

    public static Reach reach;

    public static NoRotate noRotate;

    public static BlockESP blockESP;

    public static BedESP bedESP;

    public static Blink blink;

    public static Freeze freeze;

    public static Chams chams;

    public static HUD hud;

    public static PotionHUD potionHUD;

    public static Timer timer;

    public static Fly fly;

    public static WTap wTap;

    public static Velocity velocity;

    public static AntiDebuff antiDebuff;

    public static TargetHUD targetHUD;

    public static NoFall noFall;

    public static PlayerESP playerESP;

    public static MobESP mobESP;

    public static Reduce reduce;

    public static SafeWalk safeWalk;

    public static KeepSprint keepSprint;

    public static Piercing piercing;

    public static GhostHand ghostHand;

    public static AntiKnockback antiKnockback;

    public static ExtendCamera extendCamera;

    public static Freelook freelook;

    public static Manager scriptManager;

    public static InvManager inventory;

    public static NoCameraClip noCameraClip;

    public static BedWars bedwars;

    public static BHop bHop;

    public static NoHurtCam noHurtCam;

    public static AutoTool autoTool;

    public static Backtrack backtrack;

    public static AutoSwap autoSwap;

    public static Sprint sprint;

    public static Weather weather;

    public static ChatCommands chatCommands;

    public static AutoBlockin autoBlockin;

    public static Relationships relationships;

    public static HideWindow hideWindow;

    public static Displace displace;

    public static Debug debug;

    public void register() {
        this.addModule(chatCommands = new ChatCommands());
        this.addModule(commandLine = new CommandLine());
        this.addModule(new Gui());
        this.addModule(new Settings());
        this.addModule(relationships = new Relationships());
        if (Raven.playerRelationsManager == null || Raven.playerRelationsManager.isActive()) {
            relationships.enable();
        }
        this.addModule(aimAssist = new AimAssist());
        this.addModule(antiKnockback = new AntiKnockback());
        this.addModule(autoClicker = new AutoClicker());
        this.addModule(new Autoblock());
        this.addModule(autoBlockin = new AutoBlockin());
        this.addModule(new ClickAssist());
        this.addModule(displace = new Displace());
        this.addModule(hitSelect = new HitSelect());
        this.addModule(hitBox = new HitBox());
        this.addModule(new JumpReset());
        this.addModule(killAura = new KillAura());
        this.addModule(knockbackDelay = new KnockbackDelay());
        this.addModule(new LagRange());
        this.addModule(piercing = new Piercing());
        this.addModule(ghostHand = new GhostHand());
        this.addModule(new RawInput());
        this.addModule(reach = new Reach());
        this.addModule(reduce = new Reduce());
        this.addModule(new RodAimbot());
        this.addModule(new TPAura());
        this.addModule(velocity = new Velocity());
        this.addModule(wTap = new WTap());
        this.addModule(new Limbo());
        this.addModule(backtrack = new Backtrack());
        this.addModule(new ExtraBobbing());
        this.addModule(new AutoRequeue());
        this.addModule(new AutoWho());
        this.addModule(bedwars = new BedWars());
        this.addModule(new BridgeInfo());
        this.addModule(new DuelsStats());
        this.addModule(murderMystery = new MurderMystery());
        this.addModule(skyWars = new SkyWars());
        this.addModule(new SpeedBuilders());
        this.addModule(new SumoFences());
        this.addModule(new WoolWars());
        this.addModule(bHop = new BHop());
        this.addModule(movementFix = new MovementFix());
        this.addModule(new Boost());
        this.addModule(new Dolphin());
        this.addModule(fly = new Fly());
        this.addModule(invmove = new InvMove());
        this.addModule(keepSprint = new KeepSprint());
        this.addModule(longJump = new LongJump());
        this.addModule(noSlow = new NoSlow());
        this.addModule(new NullMove());
        this.addModule(new Speed());
        this.addModule(sprint = new Sprint());
        this.addModule(new Stasis());
        this.addModule(new StopMotion());
        this.addModule(new InstantStop());
        this.addModule(new Teleport());
        this.addModule(timer = new Timer());
        this.addModule(new VClip());
        this.addModule(new Anticheat());
        this.addModule(new ChatBypass());
        this.addModule(new FakeChat());
        this.addModule(new LatencyAlerts());
        this.addModule(debug = new Debug());
        this.addModule(nameHider = new NameHider());
        this.addModule(new ViewPackets());
        this.addModule(new AntiAFK());
        this.addModule(antiFireball = new AntiFireball());
        this.addModule(new AutoJump());
        this.addModule(new AutoLadder());
        this.addModule(autoSwap = new AutoSwap());
        this.addModule(new BridgeAssist());
        this.addModule(new Clutch());
        this.addModule(autoTool = new AutoTool());
        this.addModule(bedAura = new BedAura());
        this.addModule(blink = new Blink());
        this.addModule(new DelayRemover());
        this.addModule(fastMine = new FastMine());
        this.addModule(fastPlace = new FastPlace());
        this.addModule(new FakeLag());
        this.addModule(freeze = new Freeze());
        this.addModule(new Freecam());
        this.addModule(hideWindow = new HideWindow());
        this.addModule(inventory = new InvManager());
        this.addModule(noFall = new NoFall());
        this.addModule(noRotate = new NoRotate());
        this.addModule(new Jump45());
        this.addModule(safeWalk = new SafeWalk());
        this.addModule(new WaterBucket());
        this.addModule(new keystrokesmod.utility.profile.Manager());
        this.addModule(antiDebuff = new AntiDebuff());
        this.addModule(antiShuffle = new AntiShuffle());
        this.addModule(new Arrows());
        this.addModule(bedESP = new BedESP());
        this.addModule(blockESP = new BlockESP());
        this.addModule(new BlockOverlay());
        this.addModule(new BreakProgress());
        this.addModule(chams = new Chams());
        this.addModule(new DamageTint());
        this.addModule(new DamageTags());
        this.addModule(new HitParticles());
        this.addModule(new ChestESP());
        this.addModule(extendCamera = new ExtendCamera());
        this.addModule(freelook = new Freelook());
        this.addModule(new FallView());
        this.addModule(new Holdlook());
        this.addModule(hud = new HUD());
        this.addModule(new Indicators());
        this.addModule(new ItemESP());
        this.addModule(new ItemPhysics());
        this.addModule(mobESP = new MobESP());
        this.addModule(new Scaffold());
        this.addModule(new Nametags());
        this.addModule(noCameraClip = new NoCameraClip());
        this.addModule(noHurtCam = new NoHurtCam());
        this.addModule(playerESP = new PlayerESP());
        this.addModule(potionHUD = new PotionHUD());
        this.addModule(new Radar());
        this.addModule(new Saturation());
        this.addModule(targetHUD = new TargetHUD());
        this.addModule(new Trajectories());
        this.addModule(new TNTTimer());
        this.addModule(new Tracers());
        this.addModule(new Xray());
        this.addModule(antiBot = new AntiBot());
        this.addModule(weather = new Weather());
        this.addModule(scriptManager = new Manager());
        movementFix.enable();
        antiBot.enable();
        modules.sort(Comparator.comparing(Module::getName));
    }

    public void addModule(Module module) {
        modules.add(module);
        modulesByName.put(module.getName(), module);
        modulesByNormalizedName.put(normalizeModuleName(module.getName()), module);
        modulesByClass.put(module.getClass(), module);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> inCategory(Module.category category) {
        ArrayList<Module> categoryModules = new ArrayList<>();
        for (Module module : this.getModules()) {
            if (module.moduleCategory().equals(category)) {
                categoryModules.add(module);
            }
        }
        return categoryModules;
    }

    public static Module getModule(String moduleName) {
        Module module = modulesByName.get(moduleName);
        return module != null ? module : modulesByNormalizedName.get(normalizeModuleName(moduleName));
    }

    public static Module getModule(Class<?> clazz) {
        return modulesByClass.get(clazz);
    }

    public static void sort() {
        if (HUD.alphabeticalSort.isToggled()) {
            organizedModules.sort(Comparator.comparing(Module::getNameInHud));
        } else {
            RavenFontRenderer hudFont = HUD.getHudFontRenderer();
            organizedModules.sort((o1, o2) -> hudFont.getStringWidth(HUD.getHudRenderText(o2)) - hudFont.getStringWidth(HUD.getHudRenderText(o1)));
        }
    }

    private static String normalizeModuleName(String moduleName) {
        if (moduleName == null) {
            return "";
        } else {
            StringBuilder normalized = new StringBuilder(moduleName.length());
            for (int i = 0; i < moduleName.length(); i++) {
                char character = moduleName.charAt(i);
                if (Character.isLetterOrDigit(character)) {
                    normalized.append(Character.toLowerCase(character));
                }
            }
            return normalized.toString();
        }
    }
}
