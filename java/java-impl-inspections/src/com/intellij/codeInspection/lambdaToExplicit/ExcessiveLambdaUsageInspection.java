// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInspection.lambdaToExplicit;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.java.JavaBundle;
import com.intellij.modcommand.ModPsiUpdater;
import com.intellij.modcommand.PsiUpdateModCommandQuickFix;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.pom.java.JavaFeature;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ObjectUtils;
import com.intellij.util.containers.ContainerUtil;
import com.siyeh.ig.callMatcher.CallMatcher;
import com.siyeh.ig.psiutils.CommentTracker;
import com.siyeh.ig.psiutils.ExpressionUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static com.siyeh.ig.callMatcher.CallMatcher.instanceCall;

public final class ExcessiveLambdaUsageInspection extends AbstractBaseJavaLocalInspectionTool {
  private static final CallMatcher LIST_REPLACE_ALL =
    instanceCall(CommonClassNames.JAVA_UTIL_LIST, "replaceAll").parameterTypes("java.util.function.UnaryOperator");

    @Override
  public @NotNull Set<@NotNull JavaFeature> requiredFeatures() {
    return Set.of(JavaFeature.LAMBDA_EXPRESSIONS);
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      public void visitLambdaExpression(@NotNull PsiLambdaExpression lambda) {
        PsiElement parent = lambda.getParent();
        if (!(parent instanceof PsiExpressionList)) return;
        PsiMethodCallExpression call = ObjectUtils.tryCast(parent.getParent(), PsiMethodCallExpression.class);
        if (call == null) return;
        if (!(lambda.getBody() instanceof PsiExpression expr)) return;
        if (!ExpressionUtils.isSafelyRecomputableExpression(expr)) return;
        if (ContainerUtil.or(lambda.getParameterList().getParameters(),
                             param -> ExpressionUtils.isReferenceTo(expr, param))) {
          return;
        }
        if (LIST_REPLACE_ALL.test(call)) {
          registerProblem(lambda, expr, new ReplaceWithCollectionsFillFix());
          return;
        }
        for (LambdaAndExplicitMethodPair info : LambdaAndExplicitMethodPair.INFOS) {
          if (info.isLambdaCall(call, lambda)) {
            registerProblem(lambda, expr, new RemoveExcessiveLambdaFix(info, info.getExplicitMethodName(call)));
          }
        }
      }

      private void registerProblem(PsiLambdaExpression lambda, PsiExpression expr, LocalQuickFix fix) {
        holder.registerProblem(lambda, JavaBundle.message("inspection.excessive.lambda.message"),
                               ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                               new TextRange(0, expr.getStartOffsetInParent()),
                               fix);
      }
    };
  }

  static class RemoveExcessiveLambdaFix extends PsiUpdateModCommandQuickFix {
    private final LambdaAndExplicitMethodPair myInfo;
    private final String myName;

    RemoveExcessiveLambdaFix(LambdaAndExplicitMethodPair info, String name) {
      myInfo = info;
      myName = name;
    }

    @Override
    public @Nls @NotNull String getName() {
      return JavaBundle.message("inspection.excessive.lambda.fix.name", myName);
    }

    @Override
    public @Nls @NotNull String getFamilyName() {
      return JavaBundle.message("inspection.excessive.lambda.fix.family.name");
    }

    @Override
    protected void applyFix(@NotNull Project project, @NotNull PsiElement element, @NotNull ModPsiUpdater updater) {
      Context context = Context.from(element);
      if (context == null) return;
      ExpressionUtils.bindCallTo(context.myCall, myInfo.getExplicitMethodName(context.myCall));
      CommentTracker ct = new CommentTracker();
      ct.replaceAndRestoreComments(context.myLambda, ct.text(context.myBody));
    }
  }

  static class ReplaceWithCollectionsFillFix extends PsiUpdateModCommandQuickFix {
    @Override
    public @NotNull String getFamilyName() {
      return JavaBundle.message("inspection.excessive.lambda.fix.name", "Collections.fill()");
    }

    @Override
    protected void applyFix(@NotNull Project project, @NotNull PsiElement element, @NotNull ModPsiUpdater updater) {
      Context context = Context.from(element);
      if (context == null) return;
      PsiExpression expression = ExpressionUtils.getEffectiveQualifier(context.myCall.getMethodExpression());
      if (expression == null) return;
      CommentTracker ct = new CommentTracker();
      String firstArg = expression instanceof PsiSuperExpression ? "this" : ct.text(expression);
      PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
      String text = "java.util.Collections.fill(" + firstArg + ", " + ct.text(context.myBody) + ")";
      PsiExpression replacement = factory.createExpressionFromText(text, context.myCall);
      JavaCodeStyleManager.getInstance(project).shortenClassReferences(replacement);
      ct.replaceAndRestoreComments(context.myCall, replacement);
    }
  }

  private static class Context {
    private final @NotNull PsiMethodCallExpression myCall;
    private final @NotNull PsiLambdaExpression myLambda;
    private final @NotNull PsiElement myBody;

    private Context(@NotNull PsiMethodCallExpression call,
                    @NotNull PsiLambdaExpression lambda,
                    @NotNull PsiElement body) {
      myCall = call;
      myLambda = lambda;
      myBody = body;
    }

    static @Nullable Context from(@Nullable PsiElement element) {
      if (!(element instanceof PsiLambdaExpression lambda)) return null;
      PsiElement body = lambda.getBody();
      if (body == null) return null;
      PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(lambda, PsiMethodCallExpression.class);
      if (call == null) return null;
      return new Context(call, lambda, body);
    }
  }
}
