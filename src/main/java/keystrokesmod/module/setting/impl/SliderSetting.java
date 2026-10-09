package keystrokesmod.module.setting.impl;

import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import keystrokesmod.event.PostSetSliderEvent;
import keystrokesmod.module.setting.Setting;
import net.minecraftforge.common.MinecraftForge;

public class SliderSetting extends Setting {

    private String settingName;

    private String[] options = null;

    private double defaultValue;

    private double max;

    private double min;

    private double intervals;

    public boolean isString;

    private String suffix = "";

    private String minString = null;

    public boolean canBeDisabled;

    public GroupSetting groupSetting;

    private String[] legacyProfileKeys;

    private boolean allowOutOfRangeProfileValues;

    public SliderSetting(GroupSetting groupSetting, String settingName, double defaultValue, double min, double max, double intervals) {
        super(settingName);
        this.groupSetting = groupSetting;
        this.settingName = settingName;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.intervals = intervals;
        this.isString = false;
        this.legacyProfileKeys = new String[0];
    }

    public SliderSetting(String settingName, double defaultValue, double min, double max, double intervals) {
        this((GroupSetting) null, settingName, defaultValue, min, max, intervals);
    }

    public SliderSetting(String settingName, double defaultValue, double min, double max, double intervals, String... legacyProfileKeys) {
        this(settingName, defaultValue, min, max, intervals);
        this.legacyProfileKeys = legacyProfileKeys != null ? legacyProfileKeys : new String[0];
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, String suffix, double defaultValue, double min, double max, double intervals) {
        this(groupSetting, settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
    }

    public SliderSetting(String settingName, String suffix, double defaultValue, double min, double max, double intervals) {
        this((GroupSetting) null, settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
    }

    public SliderSetting(String settingName, String suffix, double defaultValue, double min, double max, double intervals, String... legacyProfileKeys) {
        this(settingName, suffix, defaultValue, min, max, intervals);
        this.legacyProfileKeys = legacyProfileKeys != null ? legacyProfileKeys : new String[0];
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, String suffix, String minString, double defaultValue, double min, double max, double intervals) {
        this(groupSetting, settingName, suffix, defaultValue, min, max, intervals);
        this.minString = minString;
    }

    public SliderSetting(String settingName, String suffix, String minString, double defaultValue, double min, double max, double intervals) {
        this((GroupSetting) null, settingName, suffix, minString, defaultValue, min, max, intervals);
    }

    public SliderSetting(String settingName, boolean canBeDisabled, int defaultValue, String[] options) {
        this(settingName, defaultValue, options);
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(String settingName, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(settingName, defaultValue, min, max, intervals);
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(String settingName, boolean canBeDisabled, double defaultValue, double min, double max, double intervals, String... legacyProfileKeys) {
        this(settingName, canBeDisabled, defaultValue, min, max, intervals);
        this.legacyProfileKeys = legacyProfileKeys != null ? legacyProfileKeys : new String[0];
    }

    public SliderSetting(GroupSetting group, String settingName, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(group, settingName, defaultValue, min, max, intervals);
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(String settingName, String suffix, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(String settingName, String suffix, String minString, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(settingName, suffix, defaultValue, min, max, intervals);
        this.minString = minString;
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, String suffix, String minString, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(groupSetting, settingName, suffix, defaultValue, min, max, intervals);
        this.minString = minString;
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, int defaultValue, String[] options) {
        super(settingName);
        this.groupSetting = groupSetting;
        this.settingName = settingName;
        this.options = sanitizeOptions(options);
        this.defaultValue = correctValue(defaultValue, 0.0, this.options.length - 1);
        this.min = 0.0;
        this.max = this.options.length - 1;
        this.intervals = 1.0;
        this.isString = true;
        this.legacyProfileKeys = new String[0];
    }

    public SliderSetting(String settingName, int defaultValue, String[] options) {
        this((GroupSetting) null, settingName, defaultValue, options);
    }

    public SliderSetting(String settingName, int defaultValue, String[] options, String... legacyProfileKeys) {
        this((GroupSetting) null, settingName, defaultValue, options);
        this.legacyProfileKeys = legacyProfileKeys != null ? legacyProfileKeys : new String[0];
    }

    public SliderSetting(String settingName, String suffix, int defaultValue, String[] options) {
        this((GroupSetting) null, settingName, defaultValue, options);
        this.suffix = suffix;
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, String suffix, int defaultValue, String[] options) {
        this(groupSetting, settingName, defaultValue, options);
        this.suffix = suffix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public String getMinString() {
        return this.minString;
    }

    public void setMinString(String minString) {
        this.minString = minString;
    }

    public String[] getOptions() {
        return this.options;
    }

    public String getSelectedOption() {
        if (this.isString && this.options != null && this.options.length != 0) {
            int index = (int) Math.max(0L, Math.min((long) (this.options.length - 1), Math.round(this.defaultValue)));
            return this.options[index];
        } else {
            return null;
        }
    }

    public boolean setSelectedOption(String option) {
        if (this.isString && option != null && this.options != null) {
            for (int i = 0; i < this.options.length; i++) {
                if (option.equals(this.options[i])) {
                    this.setValue(i);
                    return true;
                }
            }
            for (int ix = 0; ix < this.options.length; ix++) {
                if (option.equalsIgnoreCase(this.options[ix])) {
                    this.setValue(ix);
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    public void setOptions(String[] newOptions) {
        String selectedOption = this.getSelectedOption();
        this.options = sanitizeOptions(newOptions);
        this.min = 0.0;
        this.max = this.options.length - 1;
        this.intervals = 1.0;
        this.isString = true;
        if (!this.setSelectedOption(selectedOption)) {
            this.setValue(0.0);
        }
    }

    @Override
    public String getName() {
        return this.settingName;
    }

    @Override
    public String getProfileKey() {
        return this.groupSetting == null ? this.getName() : this.groupSetting.getName() + "." + this.getName();
    }

    public double getInput() {
        return roundToInterval(this.defaultValue, 4);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    public double getInterval() {
        return this.intervals;
    }

    public double setValue(double newValue) {
        newValue = correctValue(newValue, this.min, this.max);
        newValue = Math.round(newValue * (1.0 / this.intervals)) / (1.0 / this.intervals);
        return this.defaultValue = newValue;
    }

    public void setValueWithEvent(double newValue) {
        double prev = this.defaultValue;
        MinecraftForge.EVENT_BUS.post(new PostSetSliderEvent(prev, this.setValue(newValue)));
    }

    public void setValueRaw(double n) {
        this.defaultValue = n;
    }

    public void setValueRawWithEvent(double n) {
        double prev = this.defaultValue;
        this.defaultValue = n;
        MinecraftForge.EVENT_BUS.post(new PostSetSliderEvent(prev, n));
    }

    public SliderSetting allowOutOfRangeProfileValues() {
        this.allowOutOfRangeProfileValues = true;
        return this;
    }

    public static double correctValue(double v, double i, double a) {
        v = Math.max(i, v);
        return Math.min(a, v);
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public static double roundToInterval(double v, int p) {
        if (p < 0) {
            return 0.0;
        } else {
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(p, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }

    @Override
    public void loadProfile(JsonObject data) {
        String profileKey = this.getProfileKey();
        String legacyKey = this.getName();
        String key = null;
        if (data.has(profileKey)) {
            key = profileKey;
        } else if (data.has(legacyKey)) {
            key = legacyKey;
        } else {
            for (String legacyProfileKey : this.legacyProfileKeys) {
                if (data.has(legacyProfileKey)) {
                    key = legacyProfileKey;
                    break;
                }
            }
        }
        if (key != null && data.has(key) && data.get(key).isJsonPrimitive()) {
            if (this.isString && data.getAsJsonPrimitive(key).isString()) {
                String selectedOption = data.getAsJsonPrimitive(key).getAsString();
                if (this.canBeDisabled && "Disabled".equalsIgnoreCase(selectedOption)) {
                    this.setValueRaw(-1.0);
                } else if (!this.setSelectedOption(selectedOption)) {
                    this.setValue(0.0);
                }
                return;
            }
            double newValue = this.defaultValue;
            try {
                newValue = data.getAsJsonPrimitive(key).getAsDouble();
            } catch (Exception exception) {
            }
            if (newValue == -1.0 && this.canBeDisabled) {
                this.setValueRaw(newValue);
                return;
            }
            if (this.allowOutOfRangeProfileValues) {
                this.setValueRaw(newValue);
            } else {
                this.setValue(newValue);
            }
        }
    }

    private static String[] sanitizeOptions(String[] options) {
        return options != null && options.length != 0 ? (String[]) options.clone() : new String[] { "" };
    }
}
