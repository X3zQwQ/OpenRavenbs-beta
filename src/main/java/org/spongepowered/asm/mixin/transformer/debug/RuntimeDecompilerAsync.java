package org.spongepowered.asm.mixin.transformer.debug;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class RuntimeDecompilerAsync extends RuntimeDecompiler implements Runnable, UncaughtExceptionHandler {

    private final BlockingQueue<File> queue = new LinkedBlockingQueue<>();

    private final Thread thread;

    private boolean run = true;

    public RuntimeDecompilerAsync(File outputPath) {
        super(outputPath);
        this.thread = new Thread(this, "Decompiler thread");
        this.thread.setDaemon(true);
        this.thread.setPriority(1);
        this.thread.setUncaughtExceptionHandler(this);
        this.thread.start();
    }

    @Override
    public void decompile(File file) {
        if (this.run) {
            this.queue.offer(file);
        } else {
            super.decompile(file);
        }
    }

    @Override
    public void run() {
        while (this.run) {
            try {
                File file = this.queue.take();
                super.decompile(file);
            } catch (InterruptedException exception) {
                this.run = false;
            } catch (Exception exception2) {
                exception2.printStackTrace();
            }
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        this.logger.error("Async decompiler encountered an error and will terminate. Further decompile requests will be handled synchronously. {} {}", new Object[] { ex.getClass().getName(), ex.getMessage() });
        this.flush();
    }

    private void flush() {
        this.run = false;
        File file;
        while ((file = this.queue.poll()) != null) {
            this.decompile(file);
        }
    }
}
