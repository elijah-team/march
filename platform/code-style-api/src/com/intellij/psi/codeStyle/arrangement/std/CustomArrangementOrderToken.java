// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.codeStyle.arrangement.std;

import com.intellij.psi.codeStyle.arrangement.ArrangementEntry;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementEntryMatcher;
import com.intellij.psi.codeStyle.arrangement.match.ArrangementMatchRule;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

/**
 * Allows to define custom ordering rules for entries if none of standard rules can be used.
 *
 * @see StdArrangementTokens.Order
 * @see ArrangementMatchRule#ArrangementMatchRule(ArrangementEntryMatcher, ArrangementSettingsToken)
 * @see ArrangementMatchRule#getOrderType()
 */
public abstract class CustomArrangementOrderToken extends ArrangementSettingsToken {
  protected CustomArrangementOrderToken(@NotNull String id, @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String name) {
    super(id, name);
  }

  public abstract @NotNull Comparator<ArrangementEntry> getEntryComparator();
}
