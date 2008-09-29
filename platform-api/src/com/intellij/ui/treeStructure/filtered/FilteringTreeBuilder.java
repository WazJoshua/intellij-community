package com.intellij.ui.treeStructure.filtered;

import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.speedSearch.ElementFilter;
import com.intellij.ui.treeStructure.PatchedDefaultMutableTreeNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleNodeVisitor;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.util.Comparator;

public class FilteringTreeBuilder extends AbstractTreeBuilder {

  private Object myLastSuccessfulSelect;
  private SimpleTree myTree;

  public FilteringTreeBuilder(Project project, SimpleTree tree, ElementFilter filter, AbstractTreeStructure structure, Comparator<NodeDescriptor> comparator) {
    super(tree, (DefaultTreeModel) tree.getModel(), new FilteringTreeStructure(project, filter, structure), comparator);
    myTree = tree;
    initRootNode();
  }

  public boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
    return false;
  }

  public boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
    return true;
  }

  protected final DefaultMutableTreeNode createChildNode(final NodeDescriptor childDescr) {
    return new PatchedDefaultMutableTreeNode(childDescr);
  }

  protected boolean isSelectable(Object nodeObject) {
    return true;
  }

  public void refilter() {
    Object selectedObject = getSelected();
    final Object toSelect = isSelectable(selectedObject) ? selectedObject : null;

    ((FilteringTreeStructure) getTreeStructure()).refilter();
    updateFromRoot();

    boolean wasSelected = false;
    if (selectedObject != null) {
      wasSelected = myTree.select(this, new SimpleNodeVisitor() {
        public boolean accept(SimpleNode simpleNode) {
          if (simpleNode instanceof FilteringTreeStructure.Node) {
            FilteringTreeStructure.Node node = (FilteringTreeStructure.Node)simpleNode;
            return node.getDelegate().equals(toSelect);
          }
          else {
            return false;
          }
        }
      }, true);
    }

    if (!wasSelected) {
      myTree.select(this, new SimpleNodeVisitor() {
        public boolean accept(SimpleNode simpleNode) {
          if (simpleNode instanceof FilteringTreeStructure.Node) {
            FilteringTreeStructure.Node node = (FilteringTreeStructure.Node)simpleNode;
            if (isSelectable(node.getDelegate())) {
              return true;
            }
          }
          else {
            return false;
          }
          return false;
        }
      }, true);
    }

    if (!wasSelected && myLastSuccessfulSelect != null) {
      wasSelected = myTree.select(this, new SimpleNodeVisitor() {
        public boolean accept(SimpleNode simpleNode) {
          if (simpleNode instanceof FilteringTreeStructure.Node) {
            Object object = ((FilteringTreeStructure.Node) simpleNode).getDelegate();
            return myLastSuccessfulSelect.equals(object);
          }
          return false;
        }
      }, true);
      if (wasSelected) {
        myLastSuccessfulSelect = getSelected();
      }
    } else if (wasSelected) {
      myLastSuccessfulSelect = getSelected();
    }

  }

  @Nullable
  private Object getSelected() {
    FilteringTreeStructure.Node selected = (FilteringTreeStructure.Node) myTree.getSelectedNode();
    return selected != null ? selected.getDelegate() : null;
  }

  public FilteringTreeStructure.Node getVisibleNodeFor(Object nodeObject) {
    return ((FilteringTreeStructure)getTreeStructure()).getVisibleNodeFor(nodeObject);
  }


}