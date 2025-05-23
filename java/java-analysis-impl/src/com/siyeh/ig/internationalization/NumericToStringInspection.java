/*
 * Copyright 2003-2021 Dave Griffith, Bas Leijdekkers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.siyeh.ig.internationalization;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiMethodCallExpression;
import com.siyeh.HardcodedMethodConstants;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.callMatcher.CallMatcher;
import org.jetbrains.annotations.NotNull;

public final class NumericToStringInspection extends BaseInspection {

  @Override
  public @NotNull String getID() {
    return "CallToNumericToString";
  }

  @Override
  public @NotNull String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message("call.to.numeric.tostring.problem.descriptor");
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new NumericToStringVisitor();
  }

  private static class NumericToStringVisitor extends BaseInspectionVisitor {

    private static final CallMatcher.Simple MATCHER =
      CallMatcher.instanceCall(CommonClassNames.JAVA_LANG_NUMBER, HardcodedMethodConstants.TO_STRING).parameterCount(0);

    @Override
    public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
      super.visitMethodCallExpression(expression);
      if (!MATCHER.matches(expression) || NonNlsUtils.isNonNlsAnnotatedUse(expression)) {
        return;
      }
      registerMethodCallError(expression);
    }
  }
}