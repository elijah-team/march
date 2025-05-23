// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.impl.java.stubs.impl;

import com.intellij.psi.*;
import com.intellij.psi.impl.cache.TypeAnnotationContainer;
import com.intellij.psi.impl.cache.TypeInfo;
import com.intellij.psi.impl.compiled.ClsJavaCodeReferenceElementImpl;
import com.intellij.psi.impl.java.stubs.JavaClassReferenceListElementType;
import com.intellij.psi.impl.java.stubs.JavaStubElementType;
import com.intellij.psi.impl.java.stubs.PsiClassReferenceListStub;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.PsiJavaCodeReferenceElementImpl;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.java.IJavaElementType;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiClassReferenceListStubImpl extends StubBase<PsiReferenceList> implements PsiClassReferenceListStub {
  private final TypeInfo @NotNull [] myInfos;
  private volatile PsiClassType [] myTypes;

  public PsiClassReferenceListStubImpl(@NotNull IJavaElementType type, StubElement parent, String @NotNull [] names) {
    this(type, parent, ContainerUtil.map2Array(
      ContainerUtil.filter(names, PsiClassReferenceListStubImpl::isCorrectGenericSequence),
      TypeInfo.class,
      TypeInfo::fromString)
    );
  }

  public PsiClassReferenceListStubImpl(@NotNull IJavaElementType type, StubElement parent,
                                       TypeInfo @NotNull [] infos) {
    super(parent, type);
    for (TypeInfo info : infos) {
      if (info == null) throw new IllegalArgumentException();
      if (info.text() == null) throw new IllegalArgumentException();
    }
    myInfos = infos.length == 0 ? TypeInfo.EMPTY_ARRAY : infos;
  }

  @Override
  public PsiClassType @NotNull [] getReferencedTypes() {
    PsiClassType[] types = myTypes;
    if (types == null) {
      myTypes = types = createTypes();
    }
    return types.length == 0 ? PsiClassType.EMPTY_ARRAY : types.clone();
  }
  
  private boolean shouldSkipSoleObject() {
    final boolean compiled = JavaStubElementType.isCompiled(this);
    return compiled && myInfos.length == 1 && myInfos[0].getKind() == TypeInfo.TypeKind.JAVA_LANG_OBJECT &&
           myInfos[0].getTypeAnnotations() == TypeAnnotationContainer.EMPTY;
  }

  private PsiClassType @NotNull [] createTypes() {
    PsiClassType[] types = myInfos.length == 0 ? PsiClassType.EMPTY_ARRAY : new PsiClassType[myInfos.length];

    final boolean compiled = JavaStubElementType.isCompiled(this);
    if (compiled) {
      if (shouldSkipSoleObject()) return PsiClassType.EMPTY_ARRAY;
      for (int i = 0; i < types.length; i++) {
        TypeInfo info = myInfos[i];
        TypeAnnotationContainer annotations = info.getTypeAnnotations();
        ClsJavaCodeReferenceElementImpl reference = new ClsJavaCodeReferenceElementImpl(getPsi(), info.text(), annotations);
        types[i] = new PsiClassReferenceType(reference, null).annotate(annotations.getProvider(reference));
      }
    }
    else {
      final PsiElementFactory factory = JavaPsiFacade.getElementFactory(getProject());

      int nullCount = 0;
      final PsiReferenceList psi = getPsi();
      for (int i = 0; i < types.length; i++) {
        try {
          final PsiJavaCodeReferenceElement ref = factory.createReferenceFromText(myInfos[i].text(), psi);
          ((PsiJavaCodeReferenceElementImpl)ref).setKindWhenDummy(PsiJavaCodeReferenceElementImpl.Kind.CLASS_NAME_KIND);
          types[i] = factory.createType(ref);
        }
        catch (IncorrectOperationException e) {
          types[i] = null;
          nullCount++;
        }
      }

      if (nullCount > 0) {
        PsiClassType[] newTypes = new PsiClassType[types.length - nullCount];
        int cnt = 0;
        for (PsiClassType type : types) {
          if (type != null) newTypes[cnt++] = type;
        }
        types = newTypes;
      }
    }
    return types;
  }

  private static boolean isCorrectGenericSequence(@Nullable String text) {
    if (text == null) return true;
    int depth = 0;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '<') depth++;
      else if (ch == '>') depth--;
      if (depth < 0) return false;
    }
    return depth == 0;
  }

  @Override
  public String @NotNull [] getReferencedNames() {
    if (myInfos.length == 0 || shouldSkipSoleObject()) return ArrayUtil.EMPTY_STRING_ARRAY;
    return ContainerUtil.map2Array(myInfos, String.class, info -> info.text());
  }

  @Override
  public @NotNull TypeInfo @NotNull [] getTypes() {
    if (myInfos.length == 0 || shouldSkipSoleObject()) return TypeInfo.EMPTY_ARRAY;
    return myInfos.clone();
  }

  @Override
  public @NotNull PsiReferenceList.Role getRole() {
    return JavaClassReferenceListElementType.elementTypeToRole(getElementType());
  }

  @Override
  public String toString() {
    return "PsiRefListStub[" + getRole() + ':' + String.join(", ", getReferencedNames()) + ']';
  }
}
