/*
 * @(#) View.java
 *
 */
package view;

import java.util.EventObject;
import java.util.List;

import javax.annotation.PostConstruct;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.RadialLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;

import analysis.CloneAnalyzer;
import analysis.MoveMethodAnalyzer;
import analysis.ProjectAnalyzer;
import analysis.ViewNodeAnalyzer;
import graph.model.GClassNode;
import graph.model.GMethodNode;
import graph.model.GNode;
import graph.model.GNodeType;
import graph.model.GPackageNode;
import graph.provider.GLabelProvider;
import graph.provider.GModelProvider;
import graph.provider.GNodeContentProvider;
import util.UtilMsg;
import util.UtilNode;

public class MyGraphView {
   public static final String VIEW_ID = "CDProjectOrange.partdescriptor.simplezestview";

   private GraphViewer gViewer;
   private int layout = 0;
   private Menu mPopupMenu = null;
   private MenuItem menuItemMoveMethod = null, menuItemRefresh = null, menuOpenNodeView = null, menuClone = null;
   private GraphNode selectedSrcGraphNode = null, selectedDstGraphNode = null, lastSelectedGraphNode = null;
   private GraphNode prevSelectedDstGraphNode = null;

   private GNode selectedGMethodNode = null, selectedGClassNode = null, selectedGPackageNode = null, lastSelectedNode = null;
   private GNode prevSelectedGClassNode = null, prevSelectedGPackageNode = null;

   @PostConstruct
   public void createControls(Composite parent) {
      gViewer = new GraphViewer(parent, SWT.BORDER);
      gViewer.setContentProvider(new GNodeContentProvider());
      gViewer.setLabelProvider(new GLabelProvider());
      gViewer.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
      gViewer.applyLayout();

      addPoupMenu();
      addMouseListenerGraphViewer();
   }

   private void addPoupMenu() {
      mPopupMenu = new Menu(gViewer.getControl());
      gViewer.getControl().setMenu(mPopupMenu);

      menuItemMoveMethod = new MenuItem(mPopupMenu, SWT.CASCADE);
      menuItemMoveMethod.setText("Move Method");
      addSelectionListenerMenuItemMoveMethod();

      menuItemRefresh = new MenuItem(mPopupMenu, SWT.CASCADE);
      menuItemRefresh.setText("Refresh");
      addSelectionListenerMenuItemRefresh();      
      
      menuOpenNodeView = new MenuItem(mPopupMenu, SWT.CASCADE);
      menuOpenNodeView.setText("Open Node in New View");
      addSelectionListenerNewNodeView();
      
      menuClone = new MenuItem(mPopupMenu, SWT.CASCADE);
      menuClone.setText("Clone");
      addSelectionListenerMenuItemClone();
   }

   private void addMouseListenerGraphViewer() {
      MouseAdapter mouseAdapter = new MouseAdapter() {
         public void mouseDown(MouseEvent e) {
            menuItemMoveMethod.setEnabled(false);
            resetSelectedSrcGraphNode();

            if (UtilNode.isMethodNode(e)) {
               System.out.println("single clicked");
               menuItemMoveMethod.setEnabled(true);

               selectedSrcGraphNode = (GraphNode) ((Graph) e.getSource()).getSelection().get(0);
               selectedSrcGraphNode.setBorderWidth(1);

               selectedGMethodNode = (GMethodNode) selectedSrcGraphNode.getData();
               selectedGMethodNode.setNodeType(GNodeType.UserSelection);
               
               lastSelectedNode = (GMethodNode) selectedSrcGraphNode.getData();
               lastSelectedNode.setNodeType(GNodeType.UserSelection);
            }
            else if(UtilNode.isClassNode(e)) {
            	lastSelectedGraphNode = (GraphNode) ((Graph) e.getSource()).getSelection().get(0);
            	
            	lastSelectedNode = (GClassNode) lastSelectedGraphNode.getData();
                lastSelectedNode.setNodeType(GNodeType.UserSelection);
            }
            else if(UtilNode.isPackageNode(e)) {
            	lastSelectedGraphNode = (GraphNode) ((Graph) e.getSource()).getSelection().get(0);
            	
            	lastSelectedNode = (GPackageNode) lastSelectedGraphNode.getData();
                lastSelectedNode.setNodeType(GNodeType.UserSelection);
            }
         }

         @Override
         public void mouseDoubleClick(MouseEvent e) {
            if (UtilNode.isClassNode(e)) {
               System.out.println("double clicked");
               prevSelectedDstGraphNode = selectedDstGraphNode;
               selectedDstGraphNode = (GraphNode) ((Graph) e.getSource()).getSelection().get(0);

               prevSelectedGClassNode = selectedGClassNode;
               selectedGClassNode = (GClassNode) selectedDstGraphNode.getData();

               if (selectedGClassNode.eq(prevSelectedGClassNode)) {
                  // same node => marked => unmarked
                  if (selectedGClassNode.getNodeType().equals(GNodeType.UserDoubleClicked)) {
                     UtilNode.resetDstNode(selectedDstGraphNode, selectedGClassNode);
                  }
                  // same node => unmarked => marked
                  else if (selectedGClassNode.getNodeType().equals(GNodeType.InValid)) {
                     changeColorDDClikedNode(selectedGClassNode);
                  }
               } else {
                  // different node => marked && unmarked previous marked node
                  changeColorDDClikedNode(selectedGClassNode);
                  UtilNode.resetDstNode(prevSelectedDstGraphNode, prevSelectedGClassNode);
               }
            } else if (UtilNode.isPackageNode(e)) {
               /* 
                * TODO: Class Exercise
                */
               prevSelectedDstGraphNode = selectedDstGraphNode;
               selectedDstGraphNode = (GraphNode) ((Graph) e.getSource()).getSelection().get(0);
            
               prevSelectedGPackageNode = selectedGPackageNode;
               selectedGPackageNode = (GPackageNode) selectedDstGraphNode.getData();
            
               if (selectedGPackageNode.eq(prevSelectedGPackageNode)) {
                  if (selectedGPackageNode.getNodeType().equals(GNodeType.UserDoubleClicked)) {
                     UtilNode.resetPackageNode(selectedDstGraphNode, selectedGPackageNode);//
                  } else if (selectedGPackageNode.getNodeType().equals(GNodeType.InValid)) {
                     changeColorDDClikedNode(selectedGPackageNode);//
                  }
               } else { 
                  changeColorDDClikedNode(selectedGPackageNode);//
                  UtilNode.resetPackageNode(prevSelectedDstGraphNode, prevSelectedGPackageNode);//
               } 
            }
         }
      };
      gViewer.getControl().addMouseListener(mouseAdapter);
   }

   private void changeColorDDClikedNode(GNode node) {
      if (this.selectedDstGraphNode == null)
         return;
      selectedDstGraphNode.setForegroundColor(ColorConstants.red);
      selectedDstGraphNode.setBackgroundColor(ColorConstants.blue);
      selectedDstGraphNode.setBorderColor(ColorConstants.blue);
      selectedDstGraphNode.setHighlightColor(ColorConstants.blue);
      selectedDstGraphNode.setBorderHighlightColor(ColorConstants.black);
      node.setNodeType(GNodeType.UserDoubleClicked);
   }

   private void addSelectionListenerMenuItemMoveMethod() {
      SelectionListener menuItemListenerMoveMethod = new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            if (!isNodesSelected()) {
               String msg = "Please select class and method nodes. " //
                     + "Select a class node by double-click and select a method node by single-click";
               UtilMsg.openWarning(msg);
               System.out.println("[DBG] " + msg);
               return;
            }
            if (((GMethodNode) selectedGMethodNode).isParent((GClassNode) selectedGClassNode)) {
               String msg = "Please select a different class as move destination.";
               UtilMsg.openWarning(msg);
               System.out.println("[DBG] " + msg);
               return;
            }
            System.out.println("[DBG] MenuItem MoveMethod");
            MoveMethodAnalyzer moveMethodAnalyzer = new MoveMethodAnalyzer();
            moveMethodAnalyzer.setMethodToBeMoved((GMethodNode) selectedGMethodNode);
            moveMethodAnalyzer.setClassMoveDestination((GClassNode) selectedGClassNode);
            moveMethodAnalyzer.analyze();
            moveMethodAnalyzer.moveMethod();
            resetSelectedSrcGraphNode();
            UtilNode.resetDstNode(selectedDstGraphNode, selectedGClassNode);
            syncZestViewAndJavaEditor();
         }

         private boolean isNodesSelected() {
            return selectedGMethodNode != null && selectedGMethodNode.getNodeType().equals(GNodeType.UserSelection) && //
            selectedGClassNode != null && selectedGClassNode.getNodeType().equals(GNodeType.UserDoubleClicked);
         }

         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      };
      menuItemMoveMethod.addSelectionListener(menuItemListenerMoveMethod);
   }

   private void addSelectionListenerMenuItemRefresh() {
      SelectionListener menuItemListenerRefresh = new SelectionListener() {
         @Override
         public void widgetSelected(SelectionEvent e) {
            System.out.println("[DBG] MenuItem Refresh");
            syncZestViewAndJavaEditor();
         }

         @Override
         public void widgetDefaultSelected(SelectionEvent e) {
         }
      };
      menuItemRefresh.addSelectionListener(menuItemListenerRefresh);
   }

   private void addSelectionListenerNewNodeView() {
	      SelectionListener menuItemListenerNodeView = new SelectionListener() {
	         @Override
	         public void widgetSelected(SelectionEvent e) {
	            System.out.println("[DBG] MenuItem View Node");
	            if(selectedGMethodNode != null) {
	            	System.out.println(((GMethodNode) selectedGMethodNode).getClassName());
	            	System.out.println(((GMethodNode) selectedGMethodNode).getPkgName());
	            	ViewNodeAnalyzer nodeAnalyzer = new ViewNodeAnalyzer();
	            	nodeAnalyzer.setPkgNode(((GMethodNode) selectedGMethodNode).getPkgName());
	            	nodeAnalyzer.setClassNode(((GMethodNode) selectedGMethodNode).getClassName());
	            	nodeAnalyzer.setMethodNode(((GMethodNode) selectedGMethodNode).getName());
	            	nodeAnalyzer.analyze();
	            	gViewer.setInput(GModelProvider.instance().getNodes());
	            }
	         }

	         @Override
	         public void widgetDefaultSelected(SelectionEvent e) {
	         }
	      };
	      menuOpenNodeView.addSelectionListener(menuItemListenerNodeView);
	   }
   private void addSelectionListenerMenuItemClone() {
	      SelectionListener menuItemListenerClone = new SelectionListener() {
	         @Override
	         public void widgetSelected(SelectionEvent e) {
	            CloneAnalyzer cloneAnalyzer = new CloneAnalyzer();
	            if (lastSelectedNode instanceof GMethodNode) {
	            	System.out.println("[DBG] MenuItem Clone Method");
	            	cloneAnalyzer.setMethodToBeCloned((GMethodNode) selectedGMethodNode);
	            }
	            cloneAnalyzer.setMethodToBeCloned((GMethodNode) selectedGMethodNode);
	            cloneAnalyzer.analyze();
	            cloneAnalyzer.cloneMethod();
	            resetSelectedSrcGraphNode();
	            UtilNode.resetDstNode(selectedDstGraphNode, selectedGClassNode);
	            syncZestViewAndJavaEditor();
	         }

	         private boolean isNodesSelected() {
	            return selectedGMethodNode != null && selectedGMethodNode.getNodeType().equals(GNodeType.UserSelection) && //
	            selectedGClassNode != null && selectedGClassNode.getNodeType().equals(GNodeType.UserDoubleClicked);
	         }

	         @Override
	         public void widgetDefaultSelected(SelectionEvent e) {
	         }
	      };
	      menuClone.addSelectionListener(menuItemListenerClone);
	   }
 
   private void resetSelectedSrcGraphNode() {
      if (selectedSrcGraphNode != null && selectedSrcGraphNode.isDisposed() == false) {
         selectedSrcGraphNode.setBorderWidth(0);
         selectedGMethodNode.setNodeType(GNodeType.InValid);
      }
   }

   public void syncZestViewAndJavaEditor() {
      ProjectAnalyzer analyzer = new ProjectAnalyzer();
      analyzer.analyze();
      gViewer.setInput(GModelProvider.instance().getNodes());
   }

   public void update() {
      gViewer.setInput(GModelProvider.instance().getNodes());
      if (layout % 2 == 0)
         gViewer.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
      else
         gViewer.setLayoutAlgorithm(new RadialLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
      layout++;
   }

   @Focus
   public void setFocus() {
      this.gViewer.getGraphControl().setFocus();
   }
}
