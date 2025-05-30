// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.editorconfig.language.codeinsight.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.editorconfig.common.EditorConfigBundle
import com.intellij.editorconfig.common.syntax.psi.EditorConfigHeader
import com.intellij.editorconfig.common.syntax.psi.EditorConfigVisitor
import org.editorconfig.language.codeinsight.quickfixes.EditorConfigInsertStarQuickFix
import org.editorconfig.language.codeinsight.quickfixes.EditorConfigRemoveSectionQuickFix

internal fun EditorConfigHeader.isEmptyHeader(): Boolean = this.textMatches("[]")

internal class EditorConfigEmptyHeaderInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : EditorConfigVisitor() {
    override fun visitHeader(header: EditorConfigHeader) {
      if (!header.isEmptyHeader()) return
      val message = EditorConfigBundle["inspection.header.empty.message"]
      holder.registerProblem(header, message, EditorConfigRemoveSectionQuickFix(), EditorConfigInsertStarQuickFix())
    }
  }

}