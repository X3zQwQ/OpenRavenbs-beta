package org.spongepowered.tools.obfuscation.interfaces;

import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import org.spongepowered.asm.obfuscation.mapping.common.MappingField;
import org.spongepowered.asm.obfuscation.mapping.common.MappingMethod;
import org.spongepowered.tools.obfuscation.ObfuscationData;

public interface IReferenceManager {

    void setAllowConflicts(boolean allowConflicts);

    boolean getAllowConflicts();

    void write();

    ReferenceMapper getMapper();

    void addMethodMapping(String text, String text2, ObfuscationData<MappingMethod> obfuscationData);

    void addMethodMapping(String text3, String text4, MemberInfo memberInfo, ObfuscationData<MappingMethod> obfuscationData2);

    void addFieldMapping(String text5, String text6, MemberInfo memberInfo2, ObfuscationData<MappingField> obfuscationData3);

    void addClassMapping(String text7, String text8, ObfuscationData<String> obfuscationData4);
}
