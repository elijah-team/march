/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.psi;

import org.jetbrains.annotations.NotNull;

/**
 * CAUTION: this is a workaround to fix java method searcher performance.
 * This can be removed in future if the searcher is fixed to obey {@link PsiReference#isReferenceTo(PsiElement)} contract.
 *
 * Can be implemented by {@link PsiReference}
 * @author Dmitry Avdeev
 */
public interface ResolvingHint {

  boolean canResolveTo(@NotNull Class<? extends PsiElement> elementClass);
}
