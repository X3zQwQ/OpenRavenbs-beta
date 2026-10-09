package org.spongepowered.tools.obfuscation.interfaces;

import java.util.Collection;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.mapping.IMappingConsumer;

public interface IObfuscationEnvironment {

    MappingMethod getObfMethod(MemberInfo memberInfo);

    MappingMethod getObfMethod(MappingMethod mappingMethod);

    MappingMethod getObfMethod(MappingMethod mappingMethod2, boolean enabled);

    MappingField getObfField(MemberInfo memberInfo2);

    MappingField getObfField(MappingField mappingField);

    MappingField getObfField(MappingField mappingField2, boolean enabled2);

    String getObfClass(String text);

    MemberInfo remapDescriptor(MemberInfo memberInfo3);

    String remapDescriptor(String text2);

    void writeMappings(Collection<IMappingConsumer> collection);
}
