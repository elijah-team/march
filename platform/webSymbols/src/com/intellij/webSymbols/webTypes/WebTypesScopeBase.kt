// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.webSymbols.webTypes

import com.intellij.markdown.utils.doc.DocMarkdownToHtmlConverter
import com.intellij.model.Pointer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ClearableLazyValue
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import com.intellij.util.ui.EmptyIcon
import com.intellij.webSymbols.ContextKind
import com.intellij.webSymbols.ContextName
import com.intellij.webSymbols.FrameworkId
import com.intellij.webSymbols.WebSymbolTypeSupport
import com.intellij.webSymbols.context.WebSymbolsContext
import com.intellij.webSymbols.context.WebSymbolsContext.Companion.KIND_FRAMEWORK
import com.intellij.webSymbols.context.WebSymbolsContextKindRules
import com.intellij.webSymbols.context.WebSymbolsContextKindRules.DisablementRules
import com.intellij.webSymbols.context.WebSymbolsContextKindRules.EnablementRules
import com.intellij.webSymbols.context.WebSymbolsContextRulesProvider
import com.intellij.webSymbols.impl.StaticWebSymbolsScopeBase
import com.intellij.webSymbols.query.WebSymbolNameConversionRules
import com.intellij.webSymbols.query.WebSymbolNameConversionRulesProvider
import com.intellij.webSymbols.webTypes.impl.WebTypesJsonContributionAdapter
import com.intellij.webSymbols.webTypes.impl.WebTypesJsonContributionAdapter.Companion.wrap
import com.intellij.webSymbols.webTypes.json.*
import org.jetbrains.annotations.ApiStatus.Internal
import javax.swing.Icon

@Internal
abstract class WebTypesScopeBase :
  StaticWebSymbolsScopeBase<Contributions, GenericContributionsHost, WebTypesJsonOrigin>(),
  WebSymbolsContextRulesProvider {

  private val frameworkConfigs = mutableMapOf<WebTypes, FrameworkConfig>()
  private val contextsConfigs = mutableMapOf<WebTypes, ContextsConfig>()

  private val contextRulesCache: ClearableLazyValue<MultiMap<ContextKind, WebSymbolsContextKindRules>> = createContextRulesCache()

  private val nameConversionRulesCache = createNameConversionRulesCache()

  abstract override fun createPointer(): Pointer<out WebTypesScopeBase>

  override fun getNameConversionRulesProvider(framework: FrameworkId): WebSymbolNameConversionRulesProvider {
    return WebTypesSymbolNameConversionRulesProvider(framework, this, nameConversionRulesCache)
  }

  override fun getContextRules(): MultiMap<ContextKind, WebSymbolsContextKindRules> = contextRulesCache.value

  protected open fun addWebTypes(webTypes: WebTypes, context: WebTypesJsonOrigin) {
    addRoot(webTypes.contributions, context)

    val framework = context.framework
    var dropCaches = false
    if (framework != null) {
      webTypes.frameworkConfig?.let {
        frameworkConfigs[webTypes] = it
        dropCaches = true
      }
    }
    webTypes.contextsConfig?.let {
      contextsConfigs[webTypes] = it
      dropCaches = true
    }
    if (dropCaches) dropCaches()
  }

  protected open fun removeWebTypes(webTypes: WebTypes) {
    removeRoot(webTypes.contributions)

    var dropCaches = false
    if (frameworkConfigs.remove(webTypes) != null) {
      dropCaches = true
    }
    if (contextsConfigs.remove(webTypes) != null) {
      dropCaches = true
    }
    if (dropCaches) {
      dropCaches()
    }
  }

  override fun dropCaches() {
    super.dropCaches()
    contextRulesCache.drop()
    nameConversionRulesCache.drop()
  }

  override fun matchContext(origin: WebTypesJsonOrigin, context: WebSymbolsContext): Boolean =
    origin.matchContext(context)

  override fun adaptAllRootContributions(root: Contributions,
                                         framework: FrameworkId?,
                                         origin: WebTypesJsonOrigin): Sequence<WebTypesJsonContributionAdapter> =
    root.getAllContributions(framework)
      .flatMap { (namespace, kind, list) ->
        list.map { it.wrap(origin, this@WebTypesScopeBase, namespace, kind) }
      }

  override fun adaptAllContributions(contribution: GenericContributionsHost,
                                     framework: FrameworkId?,
                                     origin: WebTypesJsonOrigin): Sequence<WebTypesJsonContributionAdapter> =
    contribution.getAllContributions(framework)
      .flatMap { (namespace, kind, list) ->
        list.map { it.wrap(origin, this@WebTypesScopeBase, namespace, kind) }
      }

  private fun createContextRulesCache(): ClearableLazyValue<MultiMap<ContextKind, WebSymbolsContextKindRules>> {
    return ClearableLazyValue.create {
      val deprecatedContextConfigRules = contextsConfigs.values.asSequence()
        .flatMap { it.additionalProperties.entries }
        .filter { (name, config) -> name != null && config.kind != null }
        .map { (name, config) -> RulesEntry(config.kind, name, config.enableWhen?.wrap(), config.disableWhen?.wrap()) }

      val contextConfigRules = contextsConfigs.values.asSequence()
        .flatMap { it.additionalProperties.entries }
        .filter { (kind, kindConfig) -> kind != null && kindConfig.kind == null }
        .flatMap { (kind, kindConfig) ->
          kindConfig.additionalProperties.asSequence()
            .filter { it.key != null }
            .map { (name, config) ->
              RulesEntry(kind, name, config.enableWhen?.wrap(), config.disableWhen?.wrap())
            }
        }
      val frameworkConfigRules = frameworkConfigs
        .filter { (webTypes, _) -> webTypes.framework != null }
        .map { (webTypes, config) ->
          RulesEntry(KIND_FRAMEWORK, webTypes.framework, config.enableWhen?.wrap(), config.disableWhen?.wrap())
        }


      val rulesPerKind = deprecatedContextConfigRules
        .plus(contextConfigRules)
        .plus(frameworkConfigRules)
        .groupBy { it.kind }

      val result = MultiMap.create<ContextKind, WebSymbolsContextKindRules>()
      rulesPerKind.forEach { (kind, rules) ->
        val rulesPerName = rules.groupBy { it.name }
        val enablementRules = rulesPerName.mapValues { (_, entries) -> entries.mapNotNull { it.enablementRules } }
        val disablementRules = rulesPerName.mapValues { (_, entries) -> entries.mapNotNull { it.disablementRules } }
        result.putValue(kind, WebSymbolsContextKindRules.create(enablementRules, disablementRules))
      }
      result
    }
  }

  private fun createNameConversionRulesCache(): ClearableLazyValue<Map<FrameworkId, WebSymbolNameConversionRules>> =
    ClearableLazyValue.create {
      frameworkConfigs
        .asSequence()
        .mapNotNull { (webTypes, config) ->
          val framework = webTypes.framework ?: return@mapNotNull null
          val builder = WebSymbolNameConversionRules.builder()

          buildNameConverters(config.canonicalNames?.additionalProperties, { mergeConverters(listOf(it)) }, builder::addCanonicalNamesRule)
          buildNameConverters(config.matchNames?.additionalProperties, { mergeConverters(it) }, builder::addMatchNamesRule)
          buildNameConverters(config.nameVariants?.additionalProperties, { mergeConverters(it) }, builder::addCompletionVariantsRule)

          Pair(framework, builder.build())
        }
        .toMap()
    }

  protected class WebTypesJsonOriginImpl(
    webTypes: WebTypes,
    override val typeSupport: WebSymbolTypeSupport,
    private val project: Project,
    private val symbolLocationResolver: (source: SourceBase) -> WebTypesSymbol.Location? = { null },
    private val sourceSymbolResolver: (location: WebTypesSymbol.Location, cacheHolder: UserDataHolderEx) -> PsiElement? = { _, _ -> null },
    private val iconLoader: (path: String) -> Icon? = { null },
    override val version: String? = webTypes.version
  ) : WebTypesJsonOrigin {
    override val framework: FrameworkId? = webTypes.framework

    override val library: String? = webTypes.name

    private val contextExpr = webTypes.requiredContext ?: webTypes.context

    private val descriptionRenderer: (String) -> String =
      when (webTypes.descriptionMarkupWithLegacy) {
        WebTypes.DescriptionMarkup.HTML -> { doc -> doc }
        WebTypes.DescriptionMarkup.MARKDOWN -> { doc -> DocMarkdownToHtmlConverter.convert(project, doc) }
        else -> { doc -> "<p>" + StringUtil.escapeXmlEntities(doc).replace(EOL_PATTERN, "<br>") }
      }

    override val defaultIcon: Icon? = webTypes.defaultIcon?.let { IconLoader.createLazy { loadIcon(it) ?: EmptyIcon.ICON_0 } }

    override fun renderDescription(description: String): String = descriptionRenderer(description)

    override fun resolveSourceSymbol(source: SourceBase, cacheHolder: UserDataHolderEx): PsiElement? {
      return resolveSourceLocation(source)?.let { sourceSymbolResolver(it, cacheHolder) }
    }

    override fun resolveSourceLocation(source: SourceBase): WebTypesSymbol.Location? = symbolLocationResolver(source)

    override fun loadIcon(path: String): Icon? {
      return if (path.startsWith("<svg")) WebTypesSvgStringIconLoader.loadIcon(path) else iconLoader(path)
    }

    override fun matchContext(context: WebSymbolsContext): Boolean {
      return ((framework == null || context.framework == framework) && contextExpr?.evaluate(context) != false)
    }

    override fun toString(): String = "$library@$version ($framework)"
  }
}

private class WebTypesSymbolNameConversionRulesProvider(
  private val framework: FrameworkId,
  private val scope: WebTypesScopeBase,
  private val nameConversionRulesCache: ClearableLazyValue<Map<FrameworkId, WebSymbolNameConversionRules>>,
) : WebSymbolNameConversionRulesProvider {
  override fun getNameConversionRules(): WebSymbolNameConversionRules {
    return nameConversionRulesCache.value[framework] ?: WebSymbolNameConversionRules.empty()
  }

  override fun createPointer(): Pointer<out WebSymbolNameConversionRulesProvider> {
    val framework = framework
    val scopePtr = scope.createPointer()
    return Pointer {
      scopePtr.dereference()?.let {
        WebTypesSymbolNameConversionRulesProvider(
          framework = framework,
          scope = scope,
          nameConversionRulesCache = nameConversionRulesCache,
        )
      }
    }
  }

  override fun getModificationCount(): Long = scope.modificationCount
}

private val EOL_PATTERN: Regex = Regex("\n|\r\n")

private data class RulesEntry(
  val kind: ContextKind,
  val name: ContextName,
  val enablementRules: EnablementRules?,
  val disablementRules: DisablementRules?,
)