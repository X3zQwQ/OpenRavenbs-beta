package keystrokesmod.module.impl.combat;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.utility.Utils;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import net.java.games.input.Mouse;
import net.minecraft.util.MouseHelper;

public class RawInput extends Module {

    private MouseHelper originalHelper;

    private RawInput.RawMouseHelper rawHelper;

    private RawInput.RawMouseThread pollThread;

    public RawInput() {
        super("Raw Input", Module.category.combat, 0);
        this.registerSetting(new DescriptionSetting("Bypasses OS mouse acceleration."));
    }

    @Override
    public void onEnable() {
        if (!isRawInputSupported()) {
            Utils.sendMessage("&cRaw input libraries not found.");
            this.disable();
        } else {
            this.originalHelper = mc.field_71417_B;
            this.pollThread = new RawInput.RawMouseThread();
            this.pollThread.start();
            this.rawHelper = new RawInput.RawMouseHelper(this.pollThread);
            mc.field_71417_B = this.rawHelper;
        }
    }

    @Override
    public void onDisable() {
        if (this.originalHelper != null) {
            mc.field_71417_B = this.originalHelper;
            this.originalHelper = null;
        }
        if (this.pollThread != null) {
            this.pollThread.running = false;
            this.pollThread = null;
        }
        this.rawHelper = null;
    }

    private static boolean isRawInputSupported() {
        String path = System.getProperty("java.library.path");
        if (path == null) {
            return false;
        } else {
            String separator = File.pathSeparator;
            String mapped = System.mapLibraryName("jinput-raw");
            for (String dir : path.split(separator.isEmpty() ? ";" : separator)) {
                if (new File(dir, mapped).exists()) {
                    return true;
                }
            }
            String text = System.mapLibraryName("jinput-dx8");
            for (String text2 : path.split(separator.isEmpty() ? ";" : separator)) {
                if (new File(text2, text).exists()) {
                    return true;
                }
            }
            return false;
        }
    }

    private static String getEnvironmentClass() {
        String path = System.getProperty("java.library.path");
        if (path == null) {
            return null;
        } else {
            String separator = File.pathSeparator;
            boolean enabled = false;
            boolean hasRaw = false;
            String text3 = System.mapLibraryName("jinput-dx8");
            String mappedRaw = System.mapLibraryName("jinput-raw");
            for (String dir : path.split(separator.isEmpty() ? ";" : separator)) {
                if (new File(dir, text3).exists()) {
                    enabled = true;
                }
                if (new File(dir, mappedRaw).exists()) {
                    hasRaw = true;
                }
            }
            if (enabled && !hasRaw) {
                return "net.java.games.input.DirectInputEnvironmentPlugin";
            } else {
                return hasRaw ? "net.java.games.input.DirectAndRawInputEnvironmentPlugin" : null;
            }
        }
    }

    static class RawMouseHelper extends MouseHelper {

        private final RawInput.RawMouseThread thread;

        private int fails;

        RawMouseHelper(RawInput.RawMouseThread thread) {
            this.thread = thread;
        }

        public void func_74372_a() {
            this.thread.reset();
            super.func_74372_a();
        }

        public void func_74374_c() {
            if (!this.thread.mice.isEmpty() && this.thread.isAlive()) {
                boolean movement = false;
                this.field_74377_a = this.thread.dx.getAndSet(0);
                this.field_74375_b = this.thread.dy.getAndSet(0);
                movement = this.field_74377_a != 0 || this.field_74375_b != 0;
                if ((Math.abs(org.lwjgl.input.Mouse.getDX()) > 5 || Math.abs(org.lwjgl.input.Mouse.getDY()) > 5) && !movement) {
                    if (this.fails++ > 5) {
                        this.thread.rescan();
                    }
                } else if (movement) {
                    this.fails = 0;
                }
            } else {
                super.func_74374_c();
            }
        }
    }

    static class RawMouseThread extends Thread {

        final AtomicInteger dx = new AtomicInteger(0);

        final AtomicInteger dy = new AtomicInteger(0);

        volatile boolean running = true;

        volatile List<Mouse> mice = new ArrayList<>();

        RawMouseThread() {
            super("Raven Raw Mouse Input");
            this.setDaemon(true);
        }

        @Override
        public void run() {
            this.rescan();
            while (this.running) {
                try {
                    if (this.mice.isEmpty()) {
                        Thread.sleep(50L);
                    } else {
                        Iterator ignored = this.mice.iterator();
                        while (true) {
                            if (ignored.hasNext()) {
                                Mouse m = (Mouse) ignored.next();
                                if (m.poll()) {
                                    this.dx.addAndGet((int) m.getX().getPollData());
                                    this.dy.addAndGet(-((int) m.getY().getPollData()));
                                    continue;
                                }
                                this.rescan();
                            }
                            Thread.sleep(1L);
                            break;
                        }
                    }
                } catch (InterruptedException exception) {
                    break;
                } catch (Exception exception2) {
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException exception3) {
                        break;
                    }
                }
            }
        }

        void rescan() {
            try {
                String envClass = RawInput.getEnvironmentClass();
                if (envClass == null) {
                    return;
                }
                Constructor<?> ctor = Class.forName(envClass).getDeclaredConstructor();
                ctor.setAccessible(true);
                ControllerEnvironment env = (ControllerEnvironment) ctor.newInstance();
                List<Mouse> found = new ArrayList<>();
                for (Controller c : env.getControllers()) {
                    if (c instanceof Mouse) {
                        found.add((Mouse) c);
                    }
                }
                this.mice = found;
            } catch (Exception exception4) {
            }
        }

        void reset() {
            this.dx.set(0);
            this.dy.set(0);
        }
    }
}
