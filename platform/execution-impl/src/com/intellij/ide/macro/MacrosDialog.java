// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.macro;

import com.intellij.execution.ExecutionBundle;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.IdeCoreBundle;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.popup.ListItemDescriptorAdapter;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Predicates;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SeparatorFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.ui.popup.list.GroupedItemsListRenderer;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.serialization.PathMacroUtil;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class MacrosDialog extends DialogWrapper {
  private final DefaultListModel<Item> myMacrosModel = new DefaultListModel<>();
  private final JBList<Item> myMacrosList = new JBList<>(myMacrosModel);
  private final JTextArea myPreviewTextarea = new JTextArea();
  private final DataContext myDataContext;

  public MacrosDialog(@NotNull Component parent,
                      @NotNull Predicate<? super Macro> filter,
                      @Nullable Map<String, String> userMacros) {
    super(parent, true);
    myDataContext = MacroManager.getCorrectContext(DataManager.getInstance().getDataContext(parent));
    init(filter, userMacros);
  }

  public static void addTextFieldExtension(@NotNull ExtendableTextField textField) {
    addTextFieldExtension(textField, Filters.ALL, null);
  }

  public static void addTextFieldExtension(@NotNull ExtendableTextField textField,
                                           @NotNull Predicate<? super Macro> macroFilter,
                                           @Nullable Map<String, String> userMacros) {
    textField.addExtension(ExtendableTextComponent.Extension.create(
      AllIcons.General.InlineAdd, AllIcons.General.InlineAddHover, ExecutionBundle.message("insert.macros"),
      () -> show(textField, macroFilter, userMacros)));
  }

  public static void addMacroSupport(@NotNull ExtendableTextField textField,
                                     @NotNull Predicate<? super Macro> macroFilter,
                                     Computable<Boolean> hasModule) {
    textField.addExtension(ExtendableTextComponent.Extension.create(
      AllIcons.General.InlineVariables, AllIcons.General.InlineVariablesHover, ExecutionBundle.message("insert.macros"),
      () -> show(textField, macroFilter, getPathMacros(hasModule.compute()))));
  }

  public static void show(@NotNull JTextComponent textComponent) {
    show(textComponent, Filters.ALL, null);
  }

  public static void show(@NotNull JTextComponent textComponent,
                          @NotNull Predicate<? super Macro> filter,
                          @Nullable Map<String, String> userMacros) {
    MacrosDialog dialog = new MacrosDialog(textComponent, filter, userMacros);
    if (dialog.showAndGet()) {
      String macro = dialog.getSelectedMacroName();
      if (macro != null) {
        int position = textComponent.getCaretPosition();
        int selectionStart = textComponent.getSelectionStart();
        int selectionEnd = textComponent.getSelectionEnd();
        try {
          if (selectionStart < selectionEnd) {
            textComponent.getDocument().remove(selectionStart, selectionEnd - selectionStart);
            position = selectionStart;
          }
          final String nameToInsert = (macro.startsWith("$") || macro.startsWith("%")) ? macro : "$" + macro + "$";
          textComponent.getDocument().insertString(position, nameToInsert, null);
          textComponent.setCaretPosition(position + nameToInsert.length());
        }
        catch (BadLocationException ignored) {
        }
      }
    }
    IdeFocusManager.findInstance().requestFocus(textComponent, true);
  }

  public static void show(@NotNull EditorTextField textComponent,
                          @NotNull Predicate<? super Macro> filter,
                          @Nullable Map<String, String> userMacros) {
    MacrosDialog dialog = new MacrosDialog(textComponent, filter, userMacros);
    if (dialog.showAndGet()) {
      String macro = dialog.getSelectedMacroName();
      Editor editor = textComponent.getEditor();
      if (macro != null && editor != null) {
        int selectionStart = editor.getSelectionModel().getSelectionStart();
        int selectionEnd = editor.getSelectionModel().getSelectionEnd();
        final String nameToInsert = (macro.startsWith("$") || macro.startsWith("%")) ? macro : "$" + macro + "$";
        int position = selectionStart < selectionEnd ? selectionStart : editor.getCaretModel().getOffset();
        WriteCommandAction.writeCommandAction(textComponent.getProject()).run(() -> {
          if (selectionStart < selectionEnd) {
            editor.getDocument().deleteString(selectionStart, selectionEnd - selectionStart);
          }
          textComponent.getDocument().insertString(position, nameToInsert);
        });
        textComponent.setCaretPosition(position + nameToInsert.length());
      }
    }
    IdeFocusManager.findInstance().requestFocus(textComponent, true);
  }

  @Override
  protected void init() {
    throw new UnsupportedOperationException("Call init(...) overload accepting parameters");
  }

  private void init(@NotNull Predicate<? super Macro> filter, @Nullable Map<String, String> userMacros) {
    super.init();

    setTitle(IdeCoreBundle.message("title.macros"));
    setOKButtonText(IdeCoreBundle.message("button.insert"));

    List<Macro> macros = ContainerUtil.sorted(ContainerUtil.filter(MacroManager.getInstance().getMacros(),
                                              macro -> MacroFilter.GLOBAL.accept(macro) && filter.test(macro)),
    new Comparator<>() {
      @Override
      public int compare(Macro macro1, Macro macro2) {
        String name1 = macro1.getName();
        String name2 = macro2.getName();
        if (!StringUtil.startsWithChar(name1, '/')) {
          name1 = ZERO + name1;
        }
        if (!StringUtil.startsWithChar(name2, '/')) {
          name2 = ZERO + name2;
        }
        return name1.compareToIgnoreCase(name2);
      }

      private static final String ZERO = new String(new char[]{0});
    });

    if (userMacros != null && !userMacros.isEmpty()) {
      for (Map.Entry<String, String> macro : userMacros.entrySet()) {
        myMacrosModel.addElement(new EntryWrapper(macro));
      }
    }
    Item firstMacro = null;
    for (Macro macro : macros) {
      final Item element = new MacroWrapper(macro);
      if (firstMacro == null) {
        firstMacro = element;
      }
      myMacrosModel.addElement(element);
    }

    final Item finalFirstMacro = firstMacro;
    myMacrosList.setCellRenderer(new GroupedItemsListRenderer<>(new ListItemDescriptorAdapter<>() {
      @Override
      public String getTextFor(Item value) {
        return value.toString(); //NON-NLS
      }

      @Override
      public boolean hasSeparatorAboveOf(Item value) {
        return value == finalFirstMacro;
      }
    }));

    addListeners();
    if (!myMacrosModel.isEmpty()) {
      myMacrosList.setSelectedIndex(0);
    }
    else {
      setOKActionEnabled(false);
    }
  }

  @Override
  protected String getHelpId() {
    return "reference.settings.ide.settings.external.tools.macros";
  }

  @Override
  protected String getDimensionServiceKey(){
    return "#com.intellij.ide.macro.MacrosDialog";
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints constr;

    // list label
    constr = new GridBagConstraints();
    constr.gridy = 0;
    constr.anchor = GridBagConstraints.WEST;
    constr.fill = GridBagConstraints.HORIZONTAL;
    panel.add(SeparatorFactory.createSeparator(IdeCoreBundle.message("label.macros"), null), constr);

    // macros list
    constr = new GridBagConstraints();
    constr.gridy = 1;
    constr.weightx = 1;
    constr.weighty = 1;
    constr.fill = GridBagConstraints.BOTH;
    constr.anchor = GridBagConstraints.WEST;
    panel.add(ScrollPaneFactory.createScrollPane(myMacrosList), constr);
    myMacrosList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myMacrosList.setPreferredSize(null);

    // preview label
    constr = new GridBagConstraints();
    constr.gridx = 0;
    constr.gridy = 2;
    constr.anchor = GridBagConstraints.WEST;
    constr.fill = GridBagConstraints.HORIZONTAL;
    panel.add(SeparatorFactory.createSeparator(IdeCoreBundle.message("label.macro.preview"), null), constr);

    // preview
    constr = new GridBagConstraints();
    constr.gridx = 0;
    constr.gridy = 3;
    constr.weightx = 1;
    constr.weighty = 1;
    constr.fill = GridBagConstraints.BOTH;
    constr.anchor = GridBagConstraints.WEST;
    panel.add(ScrollPaneFactory.createScrollPane(myPreviewTextarea), constr);
    myPreviewTextarea.setEditable(false);
    myPreviewTextarea.setLineWrap(true);
    myPreviewTextarea.setPreferredSize(null);

    panel.setPreferredSize(JBUI.size(400, 500));

    return panel;
  }

  public static @NotNull HashMap<String, String> getPathMacros(boolean addModuleMacros) {
    final HashMap<String, String> macros = new HashMap<>(PathMacros.getInstance().getUserMacros());
    if (addModuleMacros) {
      macros.put(PathMacroUtil.MODULE_DIR_MACRO_NAME, PathMacros.getInstance().getValue(PathMacroUtil.MODULE_DIR_MACRO_NAME));
      macros.put(PathMacroUtil.MODULE_WORKING_DIR,
                 PathMacros.getInstance().getValue(PathMacroUtil.MODULE_WORKING_DIR_NAME));
    }
    return macros;
  }

  /**
   * Macro info shown in list
   */
  private interface Item {
    @NotNull String getName();

    @NotNull String getPreview();

    @Override
    @NotNull String toString();
  }

  private final class MacroWrapper implements Item {
    private final Macro myMacro;

    MacroWrapper(Macro macro) {
      myMacro = macro;
    }

    @Override
    public @NotNull String getName() {
      return myMacro.getName();
    }

    @Override
    public @NotNull String getPreview() {
      return StringUtil.notNullize(myMacro.preview(myDataContext));
    }

    @Override
    public @NotNull String toString() {
      return myMacro.getName() + " - " + myMacro.getDescription();
    }
  }

  private static final class EntryWrapper implements Item {
    private final Map.Entry<String, String> myEntry;

    EntryWrapper(Map.Entry<String, String> entry) {
      myEntry = entry;
    }

    @Override
    public @NotNull String getName() {
      return myEntry.getKey();
    }

    @Override
    public @NotNull String getPreview() {
      return StringUtil.notNullize(myEntry.getValue(), "$" + getName() + "$");
    }

    @Override
    public @NotNull String toString() {
      return myEntry.getKey();
    }
  }

  private void addListeners() {
    myMacrosList.getSelectionModel().addListSelectionListener(
      e -> {
        Item item = myMacrosList.getSelectedValue();
        if (item == null) {
          myPreviewTextarea.setText("");
          setOKActionEnabled(false);
        }
        else {
          myPreviewTextarea.setText(item.getPreview());
          setOKActionEnabled(true);
        }
      }
    );

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(@NotNull MouseEvent e) {
        if (getSelectedMacroName() != null) {
          close(OK_EXIT_CODE);
          return true;
        }
        return false;
      }
    }.installOn(myMacrosList);
  }


  public @Nullable String getSelectedMacroName() {
    final Item item = myMacrosList.getSelectedValue();
    if (item == null) return null;
    return item.getName();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myMacrosList;
  }

  public static final class Filters {
    private Filters() { }

    private static final Pattern CAMEL_HUMP_START_PATTERN = Pattern.compile("(?<=[\\p{Lower}\\p{Digit}])(?![\\p{Lower}\\p{Digit}])");

    public static final @NotNull Predicate<? super Macro> ALL = Predicates.alwaysTrue();
    public static final @NotNull Predicate<? super Macro> NONE = Predicates.alwaysFalse();

    public static final @NotNull Predicate<? super Macro> ANY_PATH =
      m -> nameContains(m, "File") ||
           nameContains(m, "Dir") ||
           m instanceof ContentRootMacro ||
           m instanceof FilePromptMacro;

    public static final @NotNull Predicate<? super Macro> DIRECTORY_PATH =
      m -> nameContains(m, "Dir") ||
           m instanceof ContentRootMacro ||
           m instanceof FilePromptMacro;

    public static final @NotNull Predicate<? super Macro> FILE_PATH =
      m -> nameContains(m, "File") && !nameContains(m, "Dir") ||
           m instanceof FilePromptMacro;

    private static boolean nameContains(@NotNull Macro m, @NotNull String part) {
      final String[] nameParts = CAMEL_HUMP_START_PATTERN.split(m.getName());
      return ArrayUtil.contains(part, nameParts);
    }
  }
}
