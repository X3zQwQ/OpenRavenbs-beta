package org.spongepowered.tools.obfuscation.mapping;

import com.google.common.base.Objects;
import java.util.LinkedHashSet;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationType;

public interface IMappingConsumer {

    void clear();

    void addFieldMapping(ObfuscationType obfuscationType, MappingField mappingField, MappingField mappingField2);

    void addMethodMapping(ObfuscationType obfuscationType2, MappingMethod mappingMethod, MappingMethod mappingMethod2);

    IMappingConsumer.MappingSet<MappingField> getFieldMappings(ObfuscationType obfuscationType3);

    IMappingConsumer.MappingSet<MappingMethod> getMethodMappings(ObfuscationType obfuscationType4);

    public static class MappingSet<TMapping extends IMapping<TMapping>> extends LinkedHashSet<IMappingConsumer.MappingSet.Pair<TMapping>> {

        private static final long serialVersionUID = 1L;

        public static class Pair<TMapping extends IMapping<TMapping>> {

            public final TMapping from;

            public final TMapping to;

            public Pair(TMapping from, TMapping to) {
                this.from = from;
                this.to = to;
            }

            @Override
            public boolean equals(Object obj) {
                if (!(obj instanceof IMappingConsumer.MappingSet.Pair)) {
                    return false;
                } else {
                    IMappingConsumer.MappingSet.Pair<TMapping> other = (IMappingConsumer.MappingSet.Pair<TMapping>) obj;
                    return Objects.equal(this.from, other.from) && Objects.equal(this.to, other.to);
                }
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(new Object[] { this.from, this.to });
            }

            @Override
            public String toString() {
                return String.format("%s -> %s", this.from, this.to);
            }
        }
    }
}
