// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.ide.util.frameworkSupport;

import com.intellij.facet.ui.libraries.LibraryInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.SimpleListCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
*/
public class FrameworkSupportConfigurableBase extends FrameworkSupportConfigurable {
  private JComboBox<FrameworkVersion> myVersionComboBox;
  private final FrameworkSupportProviderBase myFrameworkSupportProvider;
  protected final FrameworkSupportModel myFrameworkSupportModel;
  private final List<FrameworkVersion> myVersions;
  private JPanel myMainPanel;
  private JLabel myDescriptionLabel;

  public FrameworkSupportConfigurableBase(FrameworkSupportProviderBase frameworkSupportProvider, FrameworkSupportModel model) {
    this(frameworkSupportProvider, model, Collections.emptyList(), null);
  }

  public FrameworkSupportConfigurableBase(FrameworkSupportProviderBase frameworkSupportProvider, FrameworkSupportModel model,
                                          @NotNull List<FrameworkVersion> versions, @NlsContexts.Label @Nullable String versionLabelText) {
    myFrameworkSupportProvider = frameworkSupportProvider;
    myFrameworkSupportModel = model;
    myVersions = versions;
    myDescriptionLabel.setText(versionLabelText);
    myVersionComboBox.setRenderer(SimpleListCellRenderer.create("", FrameworkVersion::getVersionName));
    updateAvailableVersions(versions);
    myVersionComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        fireFrameworkVersionChanged();
      }
    });
  }

  protected void updateAvailableVersions(List<? extends FrameworkVersion> versions) {
    if (!versions.isEmpty()) {
      String maxValue = "";
      ((DefaultComboBoxModel<?>)myVersionComboBox.getModel()).removeAllElements();
      FrameworkVersion defaultVersion = versions.get(versions.size() - 1);
      for (FrameworkVersion version : versions) {
        myVersionComboBox.addItem(version);
        FontMetrics fontMetrics = myVersionComboBox.getFontMetrics(myVersionComboBox.getFont());
        if (fontMetrics.stringWidth(version.getVersionName()) > fontMetrics.stringWidth(maxValue)) {
          maxValue = version.getVersionName();
        }
        if (version.isDefault()) {
          defaultVersion = version;
        }
      }
      myVersionComboBox.setSelectedItem(defaultVersion);
      myVersionComboBox.setPrototypeDisplayValue(new FrameworkVersion(maxValue + "_"));
    }

    final boolean hasMoreThanOneVersion = versions.size() >= 2;
    myDescriptionLabel.setVisible(hasMoreThanOneVersion);
    myVersionComboBox.setVisible(hasMoreThanOneVersion);
  }

  @Override
  public JComponent getComponent() {
    return myMainPanel;
  }

  protected void reloadVersions(List<? extends FrameworkVersion> frameworkVersions) {
    myVersions.clear();
    myVersions.addAll(frameworkVersions);
  }

  @Override
  public @NotNull List<? extends FrameworkVersion> getVersions() {
    return myVersions;
  }

  public LibraryInfo @NotNull [] getLibraries() {
    return getSelectedVersion().getLibraries();
  }

  @Override
  public void addSupport(final @NotNull Module module, final @NotNull ModifiableRootModel rootModel, final @Nullable Library library) {
    myFrameworkSupportProvider.addSupport(module, rootModel, getSelectedVersion(), library);
  }

  @Override
  public FrameworkVersion getSelectedVersion() {
    return (FrameworkVersion)myVersionComboBox.getSelectedItem();
  }
}
