/*
 * @(#) ASTAnalyzer.java
 *
 * Copyright 2015-2018 The Software Analysis Laboratory
 * Computer Science, The University of Nebraska at Omaha
 * 6001 Dodge Street, Omaha, NE 68182.
 */
package analysis;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import graph.model.GClassNode;
import graph.model.GMethodNode;
import graph.provider.GModelProvider;
import visitor.DeclarationVisitor;
import visitor.ViewNodeVisitor;

public class ViewNodeAnalyzer {
   private static final String JAVANATURE = "org.eclipse.jdt.core.javanature";
   protected String prjName, pkgName;
private String packageName;
private String className;
private String methodName;

   public void analyze() {
      GModelProvider.instance().reset();

      // =============================================================
      // 1st step: Project
      // =============================================================
      try {
         IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
         for (IProject project : projects) {
            if (!project.isOpen() || !project.isNatureEnabled(JAVANATURE)) { // Check if we have a Java project.
               continue;
            }
            prjName = project.getName();
            analyzePackages(JavaCore.create(project).getPackageFragments());
         }
      } catch (JavaModelException e) {
         e.printStackTrace();
      } catch (CoreException e) {
         e.printStackTrace();
      }
   }

   protected void analyzePackages(IPackageFragment[] packages) throws CoreException, JavaModelException {
      // =============================================================
      // 2nd step: Packages
      // =============================================================
      for (IPackageFragment iPackage : packages) {
         if (iPackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
            if (iPackage.getCompilationUnits().length < 1) {
               continue;
            }
            pkgName = iPackage.getElementName();
            if(pkgName.equals(packageName)) {
            	analyzeCompilationUnit(iPackage.getCompilationUnits());
            }
         }
      }
   }

   private void analyzeCompilationUnit(ICompilationUnit[] iCompilationUnits) throws JavaModelException {
      // =============================================================
      // 3rd step: ICompilationUnits
      // =============================================================
      for (ICompilationUnit iUnit : iCompilationUnits) {
         CompilationUnit compilationUnit = parse(iUnit);
         ViewNodeVisitor viewNodeVisitor = new ViewNodeVisitor();
         viewNodeVisitor.setClass(className);
         viewNodeVisitor.setMethod(methodName);
         compilationUnit.accept(viewNodeVisitor);
      }
   }

   private static CompilationUnit parse(ICompilationUnit unit) {
      ASTParser parser = ASTParser.newParser(AST.JLS10);
      parser.setKind(ASTParser.K_COMPILATION_UNIT);
      parser.setSource(unit);
      parser.setResolveBindings(true);
      return (CompilationUnit) parser.createAST(null); // parse
   }
   
   public void setMethodNode(String mNode) {
	      this.methodName = mNode;
   }

   public void setClassNode(String cNode) {
	      this.className = cNode;
   }
   
   public void setPkgNode(String cNode) {
	      this.packageName = cNode;
}
	   
}