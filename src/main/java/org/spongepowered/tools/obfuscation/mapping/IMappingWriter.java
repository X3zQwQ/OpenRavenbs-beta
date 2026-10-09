package org.spongepowered.tools.obfuscation.mapping;

import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationType;

public interface IMappingWriter {

    void write(String text, ObfuscationType obfuscationType, IMappingConsumer.MappingSet<MappingField> mappingSet, IMappingConsumer.MappingSet<MappingMethod> mappingSet2);
}
