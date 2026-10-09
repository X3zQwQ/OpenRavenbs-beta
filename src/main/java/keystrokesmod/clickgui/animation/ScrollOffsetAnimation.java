package keystrokesmod.clickgui.animation;

public class ScrollOffsetAnimation {

    private float from;

    private float to;

    private long startMs;

    private final long durationMs;

    public ScrollOffsetAnimation(long durationMs) {
        this.durationMs = durationMs;
        this.from = 0.0F;
        this.to = 0.0F;
        this.startMs = 0L;
    }

    public void reset(float value) {
        this.from = value;
        this.to = value;
        this.startMs = 0L;
    }

    public void setTarget(float newTarget) {
        this.from = this.getValue();
        this.to = newTarget;
        this.startMs = System.currentTimeMillis();
    }

    public void extend(float delta) {
        this.from = this.getValue();
        this.to += delta;
        this.startMs = System.currentTimeMillis();
    }

    public void clampTarget(float min, float max) {
        this.to = Math.max(min, Math.min(max, this.to));
    }

    public float getValue() {
        if (this.startMs == 0L) {
            return this.to;
        } else {
            long elapsed = System.currentTimeMillis() - this.startMs;
            if (elapsed >= this.durationMs) {
                this.startMs = 0L;
                this.from = this.to;
                return this.to;
            } else {
                float t = (float) elapsed / (float) this.durationMs;
                float ease = expoOut(t);
                return this.from + (this.to - this.from) * ease;
            }
        }
    }

    public boolean isAnimating() {
        return this.startMs == 0L ? false : System.currentTimeMillis() - this.startMs < this.durationMs;
    }

    public float getTarget() {
        return this.to;
    }

    private static float expoOut(float t) {
        if (t <= 0.0F) {
            return 0.0F;
        } else {
            return t >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0, -10.0 * t);
        }
    }
}
