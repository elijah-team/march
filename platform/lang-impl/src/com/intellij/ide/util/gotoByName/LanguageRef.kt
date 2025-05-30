// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.util.gotoByName

import com.intellij.lang.DependentLanguage
import com.intellij.lang.Language
import com.intellij.lang.LanguageUtil
import com.intellij.navigation.NavigationItem
import com.intellij.navigation.PsiElementNavigationItem
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

data class LanguageRef(val id: String, @field:Nls val displayName: String, val icon: Icon?) {
  companion object {
    @JvmStatic
    fun forLanguage(lang: Language): LanguageRef =
      (nonDependentLanguage(lang) ?: lang).let {
        LanguageRef(it.id, it.displayName, it.associatedFileType?.icon)
      }

    private fun nonDependentLanguage(lang: Language): Language? =
      if (lang is DependentLanguage) lang.baseLanguage else lang

    @JvmStatic
    fun forNavigationitem(item: NavigationItem): LanguageRef? = when (item) {
      is PsiElement -> forLanguage(item.language)
      is PsiElementNavigationItem -> item.targetElement?.language?.let { forLanguage(it) }
      else -> null
    }

    @JvmStatic
    fun forAllLanguages(): List<LanguageRef> {
      return Language.getRegisteredLanguages()
        .filter { it !== Language.ANY && it !is DependentLanguage }
        .sortedWith(LanguageUtil.LANGUAGE_COMPARATOR)
        .map { forLanguage(it) }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LanguageRef

    return id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}

data class FileTypeRef(val name: @NonNls String, val displayName: @Nls String, val icon: Icon?) {
  companion object {
    @JvmStatic
    fun forFileType(fileType: FileType): FileTypeRef = FileTypeRef(fileType.name, fileType.displayName, fileType.icon)

    @JvmStatic
    fun forAllFileTypes(): List<FileTypeRef> {
      return FileTypeManager.getInstance().registeredFileTypes
        .sortedWith(FileTypeComparator.INSTANCE)
        .map { forFileType(it) }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FileTypeRef

    return name == other.name
  }

  override fun hashCode(): Int {
    return name.hashCode()
  }

}
