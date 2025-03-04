package de.plushnikov.intellij.plugin.language;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import de.plushnikov.intellij.plugin.language.parser.LombokConfigParser;
import de.plushnikov.intellij.plugin.language.psi.LombokConfigFile;
import de.plushnikov.intellij.plugin.language.psi.LombokConfigTypes;
import org.jetbrains.annotations.NotNull;

public final class LombokConfigParserDefinition implements ParserDefinition {

  private static class LombokConfigParserTokenSets {
    private static final TokenSet COMMENTS = TokenSet.create(LombokConfigTypes.COMMENT);
  }

  private static final IFileElementType FILE = new IFileElementType(LombokConfigLanguage.INSTANCE);

  @Override
  public @NotNull Lexer createLexer(Project project) {
    return new LombokConfigLexerAdapter();
  }

  @Override
  public @NotNull TokenSet getCommentTokens() {
    return LombokConfigParserTokenSets.COMMENTS;
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }

  @Override
  public @NotNull PsiParser createParser(final Project project) {
    return new LombokConfigParser();
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return FILE;
  }

  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new LombokConfigFile(viewProvider);
  }

  @Override
  public @NotNull PsiElement createElement(ASTNode node) {
    return LombokConfigTypes.Factory.createElement(node);
  }
}
