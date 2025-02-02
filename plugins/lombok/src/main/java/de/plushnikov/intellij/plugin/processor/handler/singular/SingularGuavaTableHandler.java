package de.plushnikov.intellij.plugin.processor.handler.singular;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import de.plushnikov.intellij.plugin.processor.handler.BuilderInfo;
import de.plushnikov.intellij.plugin.psi.LombokLightFieldBuilder;
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder;
import de.plushnikov.intellij.plugin.util.PsiTypeUtil;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

class SingularGuavaTableHandler extends SingularMapHandler {
  private static final String COM_GOOGLE_COMMON_COLLECT_TABLE = "com.google.common.collect.Table";

  private static final String LOMBOK_ROW_KEY = "rowKey";
  private static final String LOMBOK_COLUMN_KEY = "columnKey";
  private static final String LOMBOK_VALUE = "value";

  private final boolean sortedCollection;

  SingularGuavaTableHandler(String guavaQualifiedName, boolean sortedCollection) {
    super(guavaQualifiedName);
    this.sortedCollection = sortedCollection;
  }

  @Override
  public Collection<PsiField> renderBuilderFields(@NotNull BuilderInfo info) {
    final PsiType builderFieldKeyType = getBuilderFieldType(info.getFieldType(), info.getProject());
    return Collections.singleton(
      new LombokLightFieldBuilder(info.getManager(), info.getFieldName(), builderFieldKeyType)
        .withContainingClass(info.getBuilderClass())
        .withModifier(PsiModifier.PRIVATE)
        .withNavigationElement(info.getVariable()));
  }


  private static @NotNull PsiType getRowKeyType(@NotNull PsiType psiFieldType, PsiManager psiManager) {
    return PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, COM_GOOGLE_COMMON_COLLECT_TABLE, 0);
  }

  private static @NotNull PsiType getColumnKeyType(@NotNull PsiType psiFieldType, PsiManager psiManager) {
    return PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, COM_GOOGLE_COMMON_COLLECT_TABLE, 1);
  }

  @Override
  protected @NotNull PsiType getValueType(@NotNull PsiType psiFieldType, PsiManager psiManager) {
    return PsiTypeUtil.extractOneElementType(psiFieldType, psiManager, COM_GOOGLE_COMMON_COLLECT_TABLE, 2);
  }

  private static @NotNull PsiType getCollectionType(@NotNull PsiType psiFieldType, PsiManager psiManager) {
    final PsiType rowKeyType = PsiTypeUtil.extractAllElementType(psiFieldType, psiManager, COM_GOOGLE_COMMON_COLLECT_TABLE, 0);
    final PsiType columnKeyType = PsiTypeUtil.extractAllElementType(psiFieldType, psiManager, COM_GOOGLE_COMMON_COLLECT_TABLE, 1);
    final PsiType valueType = PsiTypeUtil.extractAllElementType(psiFieldType, psiManager, COM_GOOGLE_COMMON_COLLECT_TABLE, 2);

    return PsiTypeUtil.createCollectionType(psiManager, COM_GOOGLE_COMMON_COLLECT_TABLE, rowKeyType, columnKeyType, valueType);
  }

  @Override
  protected @NotNull PsiType getBuilderFieldType(@NotNull PsiType psiFieldType, @NotNull Project project) {
    final PsiManager psiManager = PsiManager.getInstance(project);
    final PsiType rowKeyType = getRowKeyType(psiFieldType, psiManager);
    final PsiType columnKeyType = getColumnKeyType(psiFieldType, psiManager);
    final PsiType valueType = getValueType(psiFieldType, psiManager);

    return PsiTypeUtil.createCollectionType(psiManager, collectionQualifiedName + ".Builder", rowKeyType, columnKeyType, valueType);
  }

  @Override
  protected List<PsiType> getOneMethodParameterTypes(@NotNull BuilderInfo info) {
    return List.of(getKeyType(info.getFieldType(), info.getManager()),
                   getColumnKeyType(info.getFieldType(), info.getManager()),
                   getValueType(info.getFieldType(), info.getManager()));
  }

  @Override
  protected List<PsiType> getAllMethodParameterTypes(@NotNull BuilderInfo info) {
    final PsiType collectionType = getCollectionType(info.getFieldType(), info.getManager());
    return List.of(collectionType);
  }

  @Override
  protected void addOneMethodParameter(@NotNull LombokLightMethodBuilder methodBuilder,
                                       @NotNull PsiType psiFieldType,
                                       @NotNull String singularName) {
    final PsiManager psiManager = methodBuilder.getManager();
    final PsiType rowKeyType = getRowKeyType(psiFieldType, psiManager);
    final PsiType columnKeyType = getColumnKeyType(psiFieldType, psiManager);
    final PsiType valueType = getValueType(psiFieldType, psiManager);

    methodBuilder.withParameter(LOMBOK_ROW_KEY, rowKeyType);
    methodBuilder.withParameter(LOMBOK_COLUMN_KEY, columnKeyType);
    methodBuilder.withParameter(LOMBOK_VALUE, valueType);
  }


  @Override
  protected void addAllMethodParameter(@NotNull LombokLightMethodBuilder methodBuilder,
                                       @NotNull PsiType psiFieldType,
                                       @NotNull String singularName) {
    final PsiType collectionType = getCollectionType(psiFieldType, methodBuilder.getManager());

    methodBuilder.withParameter(singularName, collectionType);
  }

  @Override
  protected String getClearMethodBody(@NotNull BuilderInfo info) {
    final String codeBlockFormat = "this.{0} = null;\n" +
                                   "return {1};";
    return MessageFormat.format(codeBlockFormat, info.getFieldName(), info.getBuilderChainResult());
  }

  @Override
  protected String getOneMethodBody(@NotNull String singularName, @NotNull BuilderInfo info) {
    final String codeBlockTemplate = "if (this.{0} == null) this.{0} = {1}.{2}; \n" +
                                     "this.{0}.put(" + LOMBOK_ROW_KEY + ", " + LOMBOK_COLUMN_KEY + ", " + LOMBOK_VALUE + ");\n" +
                                     "return {3};";

    return MessageFormat.format(codeBlockTemplate, info.getFieldName(), collectionQualifiedName,
                                sortedCollection ? "naturalOrder()" : "builder()", info.getBuilderChainResult());
  }

  @Override
  protected String getAllMethodBody(@NotNull String singularName, @NotNull BuilderInfo info) {
    final String codeBlockTemplate = """
      if({0}==null)'{'throw new NullPointerException("{0} cannot be null");'}'
      if (this.{0} == null) this.{0} = {1}.{2};\s
      this.{0}.putAll({0});
      return {3};""";

    return MessageFormat.format(codeBlockTemplate, singularName, collectionQualifiedName,
                                sortedCollection ? "naturalOrder()" : "builder()", info.getBuilderChainResult());
  }

  @Override
  protected String renderBuildCode(@NotNull PsiVariable psiVariable, @NotNull String fieldName, @NotNull String builderVariable) {
    final PsiManager psiManager = psiVariable.getManager();
    final PsiType psiFieldType = psiVariable.getType();

    final PsiType rowKeyType = getRowKeyType(psiFieldType, psiManager);
    final PsiType columnKeyType = getColumnKeyType(psiFieldType, psiManager);
    final PsiType valueType = getValueType(psiFieldType, psiManager);

    return MessageFormat.format(
      "{4}<{1}, {2}, {3}> {0} = " +
      "{5}.{0} == null ? " +
      "{4}.<{1}, {2}, {3}>of() : " +
      "{5}.{0}.build();\n",
      fieldName, rowKeyType.getCanonicalText(false), columnKeyType.getCanonicalText(false),
      valueType.getCanonicalText(false), collectionQualifiedName, builderVariable);
  }

  @Override
  protected String getEmptyCollectionCall(@NotNull BuilderInfo info) {
    return collectionQualifiedName + '.' + "builder()";
  }
}
