package org.spongepowered.asm.util;

public final class Counter {

    public int value;

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == Counter.class && ((Counter) obj).value == this.value;
    }

    @Override
    public int hashCode() {
        return this.value;
    }
}
