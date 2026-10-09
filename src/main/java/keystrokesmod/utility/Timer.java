package keystrokesmod.utility;

public class Timer {

    public float updates;

    public long last;

    public float cached = Float.NaN;

    public Timer(float updates) {
        this.updates = updates;
    }

    public float getValueFloat(float begin, float end, int type) {
        if (!Float.isNaN(this.cached) && this.cached == end) {
            return this.cached;
        } else {
            float t = (float) (System.currentTimeMillis() - this.last) / this.updates;
            switch(type) {
                case 1:
                    t = t < 0.5F ? 4.0F * t * t * t : (t - 1.0F) * (2.0F * t - 2.0F) * (2.0F * t - 2.0F) + 1.0F;
                    break;
                case 2:
                    t = (float) (1.0 - Math.pow(1.0F - t, 5.0));
                    break;
                case 3:
                    t = this.bounce(t);
                    break;
                case 4:
                    t = this.quadInOut(t);
            }
            float value = begin + t * (end - begin);
            if (end > begin && value > end || end < begin && value < end) {
                value = end;
            }
            if (value == end) {
                this.cached = value;
            }
            return value;
        }
    }

    public int getValueInt(int begin, int end, int type) {
        return Math.round(this.getValueFloat(begin, end, type));
    }

    public void start() {
        this.cached = Float.NaN;
        this.last = System.currentTimeMillis();
    }

    private float bounce(float t) {
        double i2 = 7.5625;
        double i3 = 2.75;
        float i;
        if (t < 1.0 / i3) {
            i = (float) (i2 * t * t);
        } else if (t < 2.0 / i3) {
            float floatValue;
            i = (float) (i2 * (floatValue = (float) (t - 1.5 / i3)) * floatValue + 0.75);
        } else if (t < 2.5 / i3) {
            float floatValue2;
            i = (float) (i2 * (floatValue2 = (float) (t - 2.25 / i3)) * floatValue2 + 0.9375);
        } else {
            float floatValue3;
            i = (float) (i2 * (floatValue3 = (float) (t - 2.625 / i3)) * floatValue3 + 0.984375);
        }
        return i;
    }

    float quadInOut(float t) {
        return t < 0.5F ? 2.0F * t * t : -1.0F + (4.0F - 2.0F * t) * t;
    }
}
