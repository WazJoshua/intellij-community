/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.ide.util.treeView.TreeState;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.CheckedTreeNode;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;

public class PostfixTemplatesCheckboxTree extends CheckboxTree {

  private static final class PostfixTemplateCheckedTreeNode extends CheckedTreeNode {

    @NotNull
    private final String myLang;
    @NotNull
    private final PostfixTemplate myTemplate;
    @NotNull
    private final PostfixTemplatesSettings mySettings;

    @NotNull
    public PostfixTemplate getTemplate() {
      return myTemplate;
    }

    @NotNull
    public String getLang() {
      return myLang;
    }

    PostfixTemplateCheckedTreeNode(@NotNull PostfixTemplate template, @NotNull String lang) {
      super(template.getKey().replaceFirst("\\.", ""));
      PostfixTemplatesSettings templatesSettings = PostfixTemplatesSettings.getInstance();
      assert templatesSettings != null;
      mySettings = templatesSettings;

      setChecked(mySettings.isTemplateEnabled(template, lang));
      myLang = lang;
      myTemplate = template;
    }
  }

  @NotNull
  private final CheckedTreeNode myRoot;
  @NotNull
  private final DefaultTreeModel myModel;

  public PostfixTemplatesCheckboxTree() {
    super(new CheckboxTreeCellRenderer() {
      @Override
      public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if (!(value instanceof CheckedTreeNode)) return;
        CheckedTreeNode node = (CheckedTreeNode)value;
        SimpleTextAttributes attributes = node instanceof PostfixTemplateCheckedTreeNode
                                          ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
        final String text = value.toString();
        final Color background = selected ? UIUtil.getTreeSelectionBackground() : UIUtil.getTreeTextBackground();

        if (text != null) {
          getTextRenderer().append(text, new SimpleTextAttributes(background, attributes.getFgColor(), JBColor.RED, attributes.getStyle()));
        }
      }
    }, new CheckedTreeNode(null));


    myModel = (DefaultTreeModel)getModel();
    myRoot = (CheckedTreeNode)myModel.getRoot();

    getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent event) {
        selectionChanged();
      }
    });
    setRootVisible(false);
    setShowsRootHandles(true);
  }

  protected void selectionChanged() {

  }

  public void initTree(@NotNull MultiMap<String, PostfixTemplate> langToTemplates) {
    myRoot.removeAllChildren();
    for (Map.Entry<String, Collection<PostfixTemplate>> entry : langToTemplates.entrySet()) {
      CheckedTreeNode langNode = new CheckedTreeNode(entry.getKey());
      myRoot.add(langNode);
      for (PostfixTemplate template : entry.getValue()) {
        CheckedTreeNode templateNode = new PostfixTemplateCheckedTreeNode(template, entry.getKey());
        langNode.add(templateNode);
      }
    }

    myModel.nodeStructureChanged(myRoot);
    TreeUtil.expandAll(this);
    setSelectionRow(0);
  }

  public PostfixTemplate getTemplate() {
    TreePath path = getSelectionModel().getSelectionPath();
    if (path == null || !(path.getLastPathComponent() instanceof PostfixTemplateCheckedTreeNode)) {
      return null;
    }
    return ((PostfixTemplateCheckedTreeNode)path.getLastPathComponent()).getTemplate();
  }

  public Map<String, Set<String>> getState() {
    final Map<String, Set<String>> result = ContainerUtil.newHashMap();
    Consumer<PostfixTemplateCheckedTreeNode> consumer = new Consumer<PostfixTemplateCheckedTreeNode>() {
      @Override
      public void consume(PostfixTemplateCheckedTreeNode template) {
        if (!template.isChecked()) {
          Set<String> templatesForLanguage =
            ContainerUtil.getOrCreate(result, template.getLang(), PostfixTemplatesSettings.SET_FACTORY);
          templatesForLanguage.add(template.getTemplate().getKey());
        }
      }
    };
    visit(consumer);

    return result;
  }

  private void visit(@NotNull Consumer<PostfixTemplateCheckedTreeNode> consumer) {
    Enumeration languages = myRoot.children();
    while (languages.hasMoreElements()) {
      final CheckedTreeNode langNode = (CheckedTreeNode)languages.nextElement();
      Enumeration templates = langNode.children();
      while (templates.hasMoreElements()) {
        final PostfixTemplateCheckedTreeNode template = (PostfixTemplateCheckedTreeNode)templates.nextElement();
        consumer.consume(template);
      }
    }
  }


  public void setState(@NotNull final Map<String, Set<String>> langToDisabledTemplates) {
    final TreeState treeState = TreeState.createOn(this, myRoot);
    Consumer<PostfixTemplateCheckedTreeNode> consumer = new Consumer<PostfixTemplateCheckedTreeNode>() {
      @Override
      public void consume(PostfixTemplateCheckedTreeNode template) {
        Set<String> disabledTemplates = langToDisabledTemplates.get(template.getLang());
        String key = template.getTemplate().getKey();
        if (disabledTemplates != null && disabledTemplates.contains(key)) {
          template.setChecked(false);
          return;
        }

        template.setChecked(true);
      }
    };

    visit(consumer);

    myModel.nodeStructureChanged(myRoot);
    treeState.applyTo(this);
    TreeUtil.expandAll(this);
  }

  public void selectTemplate(@NotNull final PostfixTemplate postfixTemplate, @NotNull final String lang) {
    Consumer<PostfixTemplateCheckedTreeNode> consumer = new Consumer<PostfixTemplateCheckedTreeNode>() {
      @Override
      public void consume(PostfixTemplateCheckedTreeNode template) {
        if (lang.equals(template.getLang()) && postfixTemplate.getKey().equals(template.getTemplate().getKey())) {
          TreeUtil.selectInTree(template, true, PostfixTemplatesCheckboxTree.this, true);
        }
      }
    };
    visit(consumer);
  }
}
