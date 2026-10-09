package org.spongepowered.tools.obfuscation.interfaces;

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.obfuscation.mapping.IMapping;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationData;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;

public interface IObfuscationDataProvider {

    <T> ObfuscationData<T> getObfEntryRecursive(MemberInfo memberInfo);

    <T> ObfuscationData<T> getObfEntry(MemberInfo memberInfo2);

    <T> ObfuscationData<T> getObfEntry(IMapping<T> iMapping);

    ObfuscationData<MappingMethod> getObfMethodRecursive(MemberInfo memberInfo3);

    ObfuscationData<MappingMethod> getObfMethod(MemberInfo memberInfo4);

    ObfuscationData<MappingMethod> getRemappedMethod(MemberInfo memberInfo5);

    ObfuscationData<MappingMethod> getObfMethod(MappingMethod mappingMethod);

    ObfuscationData<MappingMethod> getRemappedMethod(MappingMethod mappingMethod2);

    ObfuscationData<MappingField> getObfFieldRecursive(MemberInfo memberInfo6);

    ObfuscationData<MappingField> getObfField(MemberInfo memberInfo7);

    ObfuscationData<MappingField> getObfField(MappingField mappingField);

    ObfuscationData<String> getObfClass(TypeHandle typeHandle);

    ObfuscationData<String> getObfClass(String text);
}
