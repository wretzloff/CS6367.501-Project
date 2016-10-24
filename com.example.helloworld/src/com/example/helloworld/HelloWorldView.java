package com.example.helloworld;
	
	import org.eclipse.swt.widgets.*;
	import org.eclipse.swt.SWT;
	import org.eclipse.ui.part.ViewPart;

	import java.io.*;
	import java.util.*;
	import org.eclipse.core.resources.*;
	import org.eclipse.core.runtime.*;
	import org.eclipse.jdt.core.*;
	import org.eclipse.jdt.core.dom.*;
	import org.eclipse.jdt.core.dom.rewrite.*;
	import org.eclipse.jface.text.*;
	import org.eclipse.text.edits.*;
	import org.eclipse.debug.core.*;

   public class HelloWorldView extends ViewPart 
   {
	   Label labelPrompt;
	   Combo comboProjectsList;
	   Button buttonStart;
	   Text textStatusArea;
	   String[] projectNamesArray;
	   
	   
	   public HelloWorldView() 
	   {
		   projectNamesArray = getProjectNames();
    	  
	   }
      
	   public void createPartControl(Composite parent) 
	   {
		   labelPrompt = new Label(parent, SWT.WRAP);
		   labelPrompt.setText("Choose project to test: ");
         
		   comboProjectsList = new Combo(parent, SWT.DROP_DOWN);
		   comboProjectsList.setItems(projectNamesArray);
    	  
		   buttonStart = new Button(parent, SWT.WRAP);
		   buttonStart.setText("Start");
		   buttonStart.addListener(SWT.Selection, new Listener() 
		   {
			   public void handleEvent(Event e) 
			   {
				   switch (e.type) 
				   {
				   		case SWT.Selection:
				   			startButtonPressed();
				   			break;
				   }
			   }
    	   	});
		   
		   	textStatusArea = new Text(parent, SWT.WRAP | SWT.BORDER);
		   	textStatusArea.setBounds(100, 50, 100, 20);
      }
      
      public void setFocus() 
      {
         // set focus to my widget.  For a label, this doesn't
         // make much sense, but for more complex sets of widgets
         // you would decide which one gets the focus.
      }
      
      public void startButtonPressed()
      {
    	  if(comboProjectsList.getSelectionIndex() >= 0)
    	  {
    		  String projectName = projectNamesArray[comboProjectsList.getSelectionIndex()];
    		  try 
    		  {
    			  //Create a location to store the results of the mutation testing on this project.
    			  String directoryPath = createFolderForResults(projectName);
    			  //Generate a mutation plan for this project.
    			  ArrayList<String> mutationPlans = createMutationPlan(projectName, directoryPath);
    			  
    			  //For each mutation in the mutation plan
    			  for (int i = 0; i < mutationPlans.size(); i++) 
    			  {
    				  //Create a copy of the target project so that it can be mutated
    				  String projectCopyName = copyProject(projectName, "_mutant" + i);
    				  
    				  //Parse the information needed from this mutation
        			  String handleId = getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 2);
    				  int startPosition = Integer.parseInt(getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 4));
    				  int length = Integer.parseInt(getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 5));
    				  String newSource = getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 7);
    				  
    				  //Modify the handle ID to point to the copy of the project instead of the original project
    				  handleId = handleId.replaceFirst("=" + projectName + "/", "=" + projectCopyName + "/");
    				  
    				  //Perform the specified mutation
    				  replaceSourceCode(startPosition, length, newSource, handleId);
    				  
    				  //Execute JUnit tests on project copy
    				  DebugPlugin dPlugin = DebugPlugin.getDefault();
    				  ILaunchManager launchManager = dPlugin.getLaunchManager();
    				  ILaunchConfiguration[] configurations = launchManager.getLaunchConfigurations();
    				  System.out.println(configurations.length);
    				  for(ILaunchConfiguration configuration : configurations)
    				  {
        				  System.out.println(configuration.toString());
    					  configuration.launch(ILaunchManager.RUN_MODE, null);  
    				  }
    				  
    				  //Delete the project copy
    				  deleteProject(projectCopyName);
    			  }  
    		  } 
    		  catch (CoreException e) 
    		  {
				//TODO Auto-generated catch block
				e.printStackTrace();
    		  }
    	  }
    	  else
    	  {
    		  textStatusArea.append("Please make a valid selection.\n");
    	  }  
      }//end startButtonPressed()
      
      private void replaceSourceCode(int startPosition, int length, String newSource, String handleId)
      {
    	  System.out.println("--------------------------------------------------------------------");
    	  System.out.println("Begin replaceSourceCode(): " + handleId + " " + startPosition);
    	  
    	  try 
    	  {
    		  //Get ahold of the ICompilationUnit represented by the handle ID
    		  ICompilationUnit iCompilationUnit = (ICompilationUnit)JavaCore.create(handleId);
    		  
    		  iCompilationUnit.becomeWorkingCopy(new NullProgressMonitor());
    		  CompilationUnit astRoot = parse_iCompilation_Unit_To_CompilationUnit(iCompilationUnit);
    		  AST ast = astRoot.getAST();
    		  ASTRewrite rewriter = ASTRewrite.create(ast);
    		  TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
    		  
    		  //Find the target node
    		  NodeFinder myNodeFinder = new NodeFinder(typeDecl, startPosition, length);
    		  ASTNode oldNode = myNodeFinder.getCoveredNode();
    		  
    		  //Create a new node using the provided source code
    		  TextElement siso = ast.newTextElement();
    		  siso.setText(newSource);
    		  
    		  //Replace the node
    		  rewriter.replace(oldNode, siso, null);
    		  TextEdit edits = rewriter.rewriteAST();
    		  //Document document = new Document(iCompilationUnit.getSource());
			  //edits.apply(document);
			  //iCompilationUnit.getBuffer().setContents(document.get());
    		  iCompilationUnit.applyTextEdit(rewriter.rewriteAST(), new NullProgressMonitor());
    		  iCompilationUnit.commitWorkingCopy(false, new NullProgressMonitor());
    	  } 
    	  catch (JavaModelException e) 
    	  {
    		  // TODO Auto-generated catch block
    		  e.printStackTrace();
    	  }
    	  
    	  System.out.println("End replaceSourceCode(): "  + handleId + " " + startPosition);
    	  System.out.println("--------------------------------------------------------------------");
      }
      
      private String getIthPieceOfDataFromMutationPlanString(String mutationPlan, int index) 
      {
    	  String[] splits = mutationPlan.split("\n");
    	  String currentSplit = splits[index];
    	  int indexOfLabel = currentSplit.indexOf(": ") + 2;
    	  String info = currentSplit.substring(indexOfLabel);
    	  return info;
      }
      
      //Given a project name, this method will create a copy of that project.
      private String copyProject(String projectName, String nameAddition) 
      {
    	  String cloneName = "";
    	  try 
    	  {
				IProgressMonitor m = new NullProgressMonitor();
	    	    //IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	    	    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	    	    IProjectDescription projectDescription;
				projectDescription = project.getDescription();
				cloneName = projectName + nameAddition;
	    	    // create clone project in workspace
	    	    IProjectDescription cloneDescription = ResourcesPlugin.getWorkspace().newProjectDescription(cloneName);
	    	    // copy project files
	    	    project.copy(cloneDescription, true, m);
	    	    IProject clone = ResourcesPlugin.getWorkspace().getRoot().getProject(cloneName);
	    	    // copy the project properties
	    	    cloneDescription.setNatureIds(projectDescription.getNatureIds());
	    	    cloneDescription.setReferencedProjects(projectDescription.getReferencedProjects());
	    	    cloneDescription.setDynamicReferences(projectDescription.getDynamicReferences());
	    	    cloneDescription.setBuildSpec(projectDescription.getBuildSpec());
	    	    cloneDescription.setReferencedProjects(projectDescription.getReferencedProjects());
	    	    clone.setDescription(cloneDescription, null);
	    	    
    	  } 
    	  catch (CoreException e) 
    	  {
				// TODO Auto-generated catch block
				e.printStackTrace();
    	  }
    	  return cloneName;
    	    
      }//end copyProject()
      
      //Given a project name, this method will create a copy of that project.
      private void deleteProject(String projectName) 
      {
    	  IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    	  try 
    	  {
    		  project.delete(true, null);
    	  } 
    	  catch (CoreException e) 
    	  {
    		  // TODO Auto-generated catch block
    		  e.printStackTrace();
    	  }
      }
      //Return an array of the names of the projects available in the workspace.
      private String[] getProjectNames()
      {
    	  //Create a temporary ArrayList to hold the names of the available projects
    	  ArrayList<String> projectsList = new ArrayList<String>();
    	  
    	  // Get all projects in the workspace
    	  IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    	  // Loop over all projects
    	  for (IProject project : projects) 
    	  {
    		  projectsList.add(project.getName());
    	  }
    	   
    	  String[] tempProjectsArray = new String[projectsList.size()];
    	  projectsList.toArray(tempProjectsArray);
    	  return tempProjectsArray;      
      }//end getProjectNames()
      
      private CompilationUnit parse_iCompilation_Unit_To_CompilationUnit(ICompilationUnit unit) 
      {
          ASTParser parser = ASTParser.newParser(AST.JLS8);
          parser.setKind(ASTParser.K_COMPILATION_UNIT);
          parser.setSource(unit);
          parser.setResolveBindings(true);
          return (CompilationUnit) parser.createAST(null); // parse
      }
      
      private String createFolderForResults(String projectName)
      {
    	  System.out.println("--------------------------------------------------------------------");
    	  System.out.println("Begin createFolderForResults(): " + projectName);
    	  String directoryPath = System.getProperty("user.dir") + "\\MutationTesting_" + projectName;
    	  File f = new File(directoryPath);
    	  try
    	  {
    		  //If the folder already exists, just wipe out the contents
    		  if(f.exists()) 
    		  { 
    			  for (File c : f.listFiles())
    			  {
    				  c.delete();
    			  }
    		  }
    		  else//Else the folder does not exist, so create it.
    		  {
    			  if(f.mkdir()) 
        	      { 
        	    	  System.out.println("Created directory: " + directoryPath);
        	      } 
        	      else 
        	      {
        	    	  System.out.println("Failed to create directory: " + directoryPath);
        	      }
    		  }
    	  } 
    	  catch(Exception e)
    	  {
    	      e.printStackTrace();
    	  } 
		  
    	  System.out.println("End createFolderForResults(): " + projectName);
    	  System.out.println("--------------------------------------------------------------------");
    	  return directoryPath;
      }
      
      private ArrayList<String> createMutationPlan(String projectName, String directoryPath) throws CoreException
      {
    	  System.out.println("--------------------------------------------------------------------");
    	  System.out.println("Begin createMutationPlan(): " + projectName);
    	  String mutationPlanPath = directoryPath + "\\MutationPlan.txt";
    	  textStatusArea.append("Building mutation plan: " + mutationPlanPath + "\n");
    	  
		  //We'll build the mutation plan as a list of Strings.
		  ArrayList<String> mutations = new ArrayList<String>();
		  
    	  try 
    	  {  
    		  IProject projectCopy = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    		  IJavaProject javaProject = JavaCore.create(projectCopy);
    		  IPackageFragment package1 = javaProject.getPackageFragments()[0];
    		  //Get the compilation units. Each ICompilationUnit represents a class.
    		  ICompilationUnit[] iCompilationUnits = package1.getCompilationUnits();
    		  
    		  for (ICompilationUnit iCompilationUnit : iCompilationUnits) 
        	  {
    			  //Parse a CompilationUnit from the ICompilationUnit
    			  CompilationUnit astRoot = parse_iCompilation_Unit_To_CompilationUnit(iCompilationUnit);
    			  AST ast = astRoot.getAST();
    			  ASTRewrite rewriter = ASTRewrite.create(ast);
    			  //Each TypeDeclaration also seems to represent a class
    			  TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
    			  //Type superclassType = typeDecl.getSuperclassType();
    			  //FieldDeclaration[] fieldDeclarations = typeDecl.getFields();
    			  //Get all methods from the class
    			  MethodDeclaration[] methodDeclarations = typeDecl.getMethods();
    			  for (MethodDeclaration methodDeclaration : methodDeclarations) 
    	    	  {
    				  Block methodBody = methodDeclaration.getBody();
    				  methodBody.accept(new ASTVisitor() 
    				  {  
    					  public boolean visit(PostfixExpression node) 
    					  {
    						  StringBuilder sb = new StringBuilder();
    						  IMethodBinding a = methodDeclaration.resolveBinding();
    						  ITypeBinding b = a.getDeclaringClass();
    						  sb.append("File Name: " + iCompilationUnit.getPath() + "\n");
    						  sb.append("Class name: " + b.getName() + "\n");
    						  sb.append("handleID: " + iCompilationUnit.getHandleIdentifier() + "\n");
    						  sb.append("Line: " + astRoot.getLineNumber(node.getStartPosition()) + "\n");
    						  sb.append("Start Position: " + node.getStartPosition() + "\n");
    						  sb.append("Length: " + node.getLength() + "\n");
    						  sb.append("Current source: " + node + "\n");
    						  node.setOperator(PostfixExpression.Operator.toOperator("--"));
    						  sb.append("New source: " + node + "\n");
    						  sb.append("\n");
    						  mutations.add(sb.toString());
    						  return true; 
    					  }//end visit()
    				  });
    	    	  }//end for loop
        	  }//end for loop
    		  
    		  //Create a file and print the plan
    		  PrintWriter writer = new PrintWriter(new File(mutationPlanPath));
    		  for(String mutation : mutations)
    		  {
        		  writer.print(mutation);    			  
    		  }
    		  
    		  writer.close();
    	  } 
    	  catch (IOException e) 
    	  {
			// TODO Auto-generated catch block
			e.printStackTrace();
    	  }
    	  
    	  System.out.println("End createMutationPlan(): " + projectName);
    	  System.out.println("--------------------------------------------------------------------");
    	  return mutations;
      }
   }//end class