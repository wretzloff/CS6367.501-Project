package com.example.helloworld;
	
	import org.eclipse.swt.widgets.*;
	import org.eclipse.swt.SWT;
	import org.eclipse.swt.layout.*;
	import org.eclipse.ui.part.ViewPart;

	import java.io.*;
	import java.util.*;
	import org.eclipse.core.resources.*;
	import org.eclipse.core.runtime.*;
	import org.eclipse.jdt.core.*;
	import org.eclipse.jdt.core.dom.*;
	import org.eclipse.jdt.core.dom.rewrite.*;
	import org.eclipse.jdt.junit.*;
	import org.eclipse.jdt.junit.model.*;
	import org.eclipse.jdt.junit.model.ITestElement.FailureTrace;
	import org.eclipse.jface.text.*;
	import org.eclipse.text.edits.*;
	import org.eclipse.debug.core.*;
	import org.eclipse.debug.core.model.IProcess;

   public class HelloWorldView extends ViewPart 
   {
	   Label labelChooseProject;
	   Label labelChooseTimeout;
	   Combo comboProjectsList;
	   Button buttonStart;
	   Text textStatusArea;
	   Text textTimeoutArea;
	   String[] projectNamesArray;
	   
	   
	   public HelloWorldView() 
	   {
		   projectNamesArray = getProjectNames();
    	  
	   }
      
	   public void createPartControl(Composite parent) 
	   {
		   Composite top = new Composite(parent, SWT.NONE);
		   GridLayout layout = new GridLayout();
		   layout.numColumns = 2;
		   top.setLayout(layout);

		   Composite left = new Composite(top, SWT.BORDER);
		   GridLayout leftLayout = new GridLayout();
		   leftLayout.numColumns = 1;
		   left.setLayout(leftLayout);
		   
		   labelChooseProject = new Label(left, SWT.WRAP);
		   labelChooseProject.setText("Choose project to test: ");
		   
		   comboProjectsList = new Combo(left, SWT.DROP_DOWN);
		   comboProjectsList.setItems(projectNamesArray);

		   labelChooseTimeout = new Label(left, SWT.WRAP);
		   labelChooseTimeout.setText("Specify timeout: ");
		   
		   textTimeoutArea = new Text(left, SWT.BORDER);
		   textTimeoutArea.setText("10000");
		   
		   buttonStart = new Button(left, SWT.WRAP);
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
		   
		   textStatusArea = new Text(top, SWT.MULTI | SWT.BORDER );//| SWT.WRAP);
		   
		   GridData data = new GridData();
		   data.verticalAlignment = SWT.RIGHT;
		   data.grabExcessHorizontalSpace = true;
		   data.grabExcessVerticalSpace = true;
		   data.heightHint = 500;
		   data.widthHint = 1000;
		   textStatusArea.setLayoutData(data);
		   
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
    		  int timeout = Integer.parseInt(textTimeoutArea.getText());
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
    				  String projectCopyName = projectName + "_mutant" + i;
    				  deleteProject(projectCopyName);
    				  copyProject(projectName, projectCopyName);
    				
    				  //Error check: check that project copy exists
    				  IProject projectCopy = ResourcesPlugin.getWorkspace().getRoot().getProject(projectCopyName);
    				  if(projectCopy.exists() == false)
    				  {
    					  System.out.println("Failure to create mutant " + projectCopyName + ". Moving to next iteration.");
    					  String filePath = directoryPath + "/" + projectCopyName + " - failure_to_create_mutant.txt";
    		    		  printArrayListOfStringsToFile(filePath, new ArrayList<String>());
    		    		  
    				  }
    				  else
    				  {
    					//Parse the information needed from this mutation
            			  String handleId = getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 2);
        				  int startPosition = Integer.parseInt(getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 4));
        				  int length = Integer.parseInt(getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 5));
        				  String newSource = getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 7);
        				  
        				  //Modify the handle ID to point to the copy of the project instead of the original project
        				  handleId = handleId.replaceFirst("=" + projectName + "/", "=" + projectCopyName + "/");
        				  
        				  //Perform the specified mutation
        				  replaceSourceCode(startPosition, length, newSource, handleId);
        				  
        				  //Set up a listener that will be notified when a test launch finishes.
        				  setUpTestRunListener(projectCopyName, directoryPath);
        				  
        				  //Add JUnit to the project copy's build path
        				  addJUnitToBuildPath(projectCopyName);
        				  
        				  //Create a new launch configuration that will launch all JUnit tests.
        				  ILaunchConfiguration launchConfiguration = createJUnitRunConfiguration(projectCopyName);
        				  
        				  //Error check: check for build errors
        				  boolean foundErrors = false;
        				  projectCopy = ResourcesPlugin.getWorkspace().getRoot().getProject(projectCopyName);
        				  IMarker[] markers = projectCopy.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        				  for (IMarker marker: markers)
        				  {
        					  Integer severityType = (Integer) marker.getAttribute(IMarker.SEVERITY);
        					  if (severityType.intValue() == IMarker.SEVERITY_ERROR)
        					  {
        						  System.out.println("Marker: " + marker.getResource());
        						  foundErrors = true;
        					  }
        				  }
        				  if(foundErrors == true)
        				  {
    						  System.out.println("Build errors in project " + projectCopyName);
    						  String filePath = directoryPath + "/" + projectCopyName + " - build_errors.txt";
        		    		  printArrayListOfStringsToFile(filePath, new ArrayList<String>());
        		    		  continue;
        				  }
      
        				  //Execute JUnit tests on project copy.
        				  executeTests(launchConfiguration, directoryPath, timeout);
        				  
        				  //Delete launch configuration now that we're done with it
        				  launchConfiguration.delete();
        				  
        				  //Clean up the project copy now that we're done with it
        				  deleteProject(projectCopyName);
    				  }//end else
    				 
    			  	}//end for loop  
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
      
      private void addJUnitToBuildPath(String projectName)
      {
    	  System.out.println("--------------------------------------------------------------------");
    	  System.out.println("Begin addJUnitToBuildPath(): ");
    	  IProject projectCopy = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		  IJavaProject javaProject = JavaCore.create(projectCopy);
		  IClasspathEntry[] rawClasspath;
		  try 
		  {
			rawClasspath = javaProject.getRawClasspath();
			IClasspathEntry[] newClasspath = new IClasspathEntry[rawClasspath.length + 1];
			System.arraycopy(rawClasspath, 0, newClasspath, 0, rawClasspath.length);
			Path junitPath = new Path("org.eclipse.jdt.junit.JUNIT_CONTAINER/4"); 
			IClasspathEntry junitEntry = JavaCore.newContainerEntry(junitPath);
			newClasspath[rawClasspath.length] =junitEntry;
			javaProject.setRawClasspath(newClasspath,null);
		  } 
		  catch (JavaModelException e) 
		  {
			// TODO Auto-generated catch block
			e.printStackTrace();
		  }
		  System.out.println("end addJUnitToBuildPath(): ");
		  System.out.println("--------------------------------------------------------------------");
      }//end addJUnitToBuildPath()
      
      private void setUpTestRunListener(String projectCopyName, String directoryPath)
      {
    	  System.out.println("--------------------------------------------------------------------");
    	  System.out.println("Begin setUpTestRunListener(): ");
    	  
    	  //Create a listener to listen for the completion of the tests, so the results can be logged.
		  JUnitCore.addTestRunListener(new TestRunListener() {
			  public void sessionFinished(ITestRunSession session) 
	    	  {
	        	  System.out.println("--------------------------------------------------------------------");
	        	  System.out.println("Begin sessionFinished(): ");
	        	
	        	  //Create a temporary ArrayList to hold each line of the test results 
		    	  ArrayList<String> results = new ArrayList<String>();
	    		  
		    	  //Loop through the test suites.
	    		  ITestElement[] children = session.getChildren();
	    		  for(ITestElement child : children)
	    		  {
	    			  extractResultsFromITestElement(child, results, "");
	    		  }
	    		  
	    		  //Create a file and print the results
	    		  String resultsFilePath = directoryPath + "/" + session.getLaunchedProject().getElementName() + " - " + session.getTestResult(false) + ".txt";
	    		  printArrayListOfStringsToFile(resultsFilePath, results);
	    		  
	        	  System.out.println("End sessionFinished(): ");
	        	  System.out.println("--------------------------------------------------------------------");
	    	  }
		  });
		  System.out.println("--------------------------------------------------------------------");
    	  System.out.println("End setUpTestRunListener(): ");
      }
      
      private void extractResultsFromITestElement(ITestElement element, ArrayList<String> results, String tabs)
      {
    	  if(element instanceof ITestCaseElement)
    	  {
    		  ITestCaseElement testcase = (ITestCaseElement) element;
    		  String printstring = tabs + testcase.getTestMethodName() + " " + testcase.getTestResult(true);
    		  FailureTrace failureTrace = testcase.getFailureTrace();
    		  if(failureTrace != null)
    		  {
    			  int firstTab = failureTrace.getTrace().indexOf('\t');
    			  printstring = printstring + " " + failureTrace.getTrace().substring(0, firstTab);
    		  }
    		  else
    		  {
    			  printstring = printstring + "\n";
    		  }
    		  results.add(printstring);
    	  }
    	  else if (element instanceof ITestSuiteElement)
		  {
    		  results.add(tabs + element.toString() + "\n");
    		  
    		  ITestSuiteElement testsuite = (ITestSuiteElement) element;
			  ITestElement[] children = testsuite.getChildren();
			  for(ITestElement child : children)
    		  {
				  extractResultsFromITestElement(child, results, tabs + "\t");
    		  }
		  }
      }
      
      private void executeTests(ILaunchConfiguration launchConfiguration, String directoryPath, int timeout)
      {
    	  try
    	  {
    		  String projectCopyName = launchConfiguration.getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", "");
    		  System.out.println("--------------------------------------------------------------------");
        	  System.out.println("Begin executeTests(): " + projectCopyName);
    		  System.out.println("Run configuration name: " + launchConfiguration.getName());
			  System.out.println("Project: " + projectCopyName);
			  System.out.println("Run configuration type: " + launchConfiguration.getType().getName());
			  
			  //Launch the tests.
			  ILaunch launch = launchConfiguration.launch(ILaunchManager.RUN_MODE, null);
			  
			  //Wait some time for the launch to complete, and if it hasn't completed, terminate it.
			  Thread.sleep(timeout);
			  IProcess[] processes = launch.getProcesses();
			  boolean processNotTerminated = false;
			  for(IProcess process : processes)
			  {
				  if(process.isTerminated() == false)
				  {
					  processNotTerminated = true;  
				  }
			  }
			    
			  if(processNotTerminated)
			  {
				  for(IProcess process : processes)
				  {
					  process.terminate();
				  }  
				  
				  System.out.println("Tests for " + launchConfiguration.getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", "") + " exceded timeout: " + timeout);
				  String filePath = directoryPath + "/" + projectCopyName + " - tests_timed_out.txt";
	    		  printArrayListOfStringsToFile(filePath, new ArrayList<String>());
			  }
				  
			  
			  System.out.println("End executeTests(): " + projectCopyName);
	    	  System.out.println("--------------------------------------------------------------------");
    	  }
    	  catch (CoreException e) 
    	  {
  			// TODO Auto-generated catch block
  			e.printStackTrace();
    	  } catch (InterruptedException e) 
    	  {
			// TODO Auto-generated catch block
			e.printStackTrace();
    	  }
    	  
      }//end executeTests()
      
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
      private String copyProject(String projectName, String cloneName) 
      {
    	  System.out.println("--------------------------------------------------------------------");
    	  System.out.println("Begin copyProject(): " + projectName);

    	  try 
    	  {
				IProgressMonitor m = new NullProgressMonitor();
	    	    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	    	    IProjectDescription projectDescription;
				projectDescription = project.getDescription();
				
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
	    	    
	    	    //Check that the new project exists before exiting.
	    	    while(clone.exists() == false);
	    	    {
	    	    	System.out.println("Project " + clone.getName() + " not finished being created yet.");
	    	    	Thread.sleep(5000);
	    	    }
    	  } 
    	  catch (CoreException e) 
    	  {
    		  // TODO Auto-generated catch block
    		  //e.printStackTrace();
    		  System.out.println("CoreException: " + e.getMessage());
    	  } 
    	  catch (InterruptedException e) 
    	  {
    		  // TODO Auto-generated catch block
    		  e.printStackTrace();
    	  }
    	  
    	  System.out.println("End copyProject(): " + projectName);
    	  System.out.println("--------------------------------------------------------------------");
    	  return cloneName;
    	    
      }//end copyProject()
      
      //Given a project name, this method will create a copy of that project.
      private void deleteProject(String projectName) 
      {
    	  System.out.println("--------------------------------------------------------------------");
    	  System.out.println("Begin deleteProject(): " + projectName);
    	  IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    	  if(project.exists())
    	  {
    		  System.out.println("Project " + projectName + " exists. It is being deleted.");
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
    	  System.out.println("End deleteProject(): " + projectName);
    	  System.out.println("--------------------------------------------------------------------");
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
    	  String directoryPath = getResultsDirectory() + "\\MutationTesting_" + projectName;
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
      
      private String getResultsDirectory()
      {
    	  return System.getProperty("user.dir");
      }
      
      private void printArrayListOfStringsToFile(String fileName, ArrayList<String> strings)
      {
		  try 
		  {
			  PrintWriter writer = new PrintWriter(new File(fileName));
			  for(String string : strings)
			  {
	    		  writer.print(string);    			  
			  }
			  
			  writer.close();
		  } 
		  catch (FileNotFoundException e) 
		  {
			  // TODO Auto-generated catch block
			  e.printStackTrace();
		  }
      }//End printArrayListOfStringsToFile
      
      private ArrayList<String> createMutationPlan(String projectName, String directoryPath) throws CoreException
      {
    	  System.out.println("--------------------------------------------------------------------");
    	  System.out.println("Begin createMutationPlan(): " + projectName);
    	  String mutationPlanPath = directoryPath + "\\MutationPlan.txt";
    	  textStatusArea.append("Building mutation plan: " + mutationPlanPath + "\n");
    	  
		  //We'll build the mutation plan as a list of Strings.
		  ArrayList<String> mutations = new ArrayList<String>();
		  
    	  IProject projectCopy = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		  IJavaProject javaProject = JavaCore.create(projectCopy);
		  IPackageFragment[] packageFragments = javaProject.getPackageFragments();
		  for(IPackageFragment packageFragment : packageFragments)
		  {
			  //Get the compilation units. Each ICompilationUnit represents a class.
			  ICompilationUnit[] iCompilationUnits = packageFragment.getCompilationUnits();
			  
			  for (ICompilationUnit iCompilationUnit : iCompilationUnits) 
			  {
				  //Parse a CompilationUnit from the ICompilationUnit
				  CompilationUnit astRoot = parse_iCompilation_Unit_To_CompilationUnit(iCompilationUnit);
				  AST ast = astRoot.getAST();
				  ASTRewrite rewriter = ASTRewrite.create(ast);
				  //Each TypeDeclaration also seems to represent a class
				  if(!(astRoot.types().get(0) instanceof TypeDeclaration))
				  {
					  continue;
				  }
				  TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
				  //Type superclassType = typeDecl.getSuperclassType();
				  //FieldDeclaration[] fieldDeclarations = typeDecl.getFields();
				  //Get all methods from the class
				  MethodDeclaration[] methodDeclarations = typeDecl.getMethods();
				  for (MethodDeclaration methodDeclaration : methodDeclarations) 
				  {
					  Block methodBody = methodDeclaration.getBody();
					  if(methodBody != null)
					  {
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
					  }
					  
				  }//end for loop
			  }//end for loop
		  }
		  
		  if(mutations.size() == 0)
		  {
			  mutationPlanPath = mutationPlanPath + " - no mutations";
		  }
		  
		  //Create a file and print the plan
		  printArrayListOfStringsToFile(mutationPlanPath, mutations);
		  
    	  System.out.println("End createMutationPlan(): " + projectName);
    	  System.out.println("--------------------------------------------------------------------");
    	  return mutations;
      }
      
      private ILaunchConfigurationWorkingCopy createJUnitRunConfiguration(String projectName) 
      {
    	  System.out.println("--------------------------------------------------------------------");
    	  System.out.println("Begin createJUnitRunConfiguration(): " + projectName);
    	  IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    	  ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
    	  ILaunchConfigurationType launchType = manager.getLaunchConfigurationType("org.eclipse.jdt.junit.launchconfig");
    	  ILaunchConfigurationWorkingCopy workingCopy = null;
    	  try 
    	  {
    	        workingCopy = launchType.newInstance(null, project.getName());
    	        IResource[] resourcesArray = {project};
    	        workingCopy.setMappedResources(resourcesArray);
    	        workingCopy.setAttribute("org.eclipse.jdt.junit.CONTAINER", "=" + project.getName().replace("#", "\\#"));
    	        workingCopy.setAttribute("org.eclipse.jdt.junit.KEEPRUNNING_ATTR", false);
    	        workingCopy.setAttribute("org.eclipse.jdt.junit.TESTNAME", "");
    	        workingCopy.setAttribute("org.eclipse.jdt.junit.TEST_KIND", "org.eclipse.jdt.junit.loader.junit4");
    	        workingCopy.setAttribute("org.eclipse.jdt.launching.MAIN_TYPE", "");
    	        workingCopy.setAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", projectName);
    	        workingCopy.doSave();
    	        
    	    } 
    	    catch (CoreException e) {
    	        
    	    }
    	  
    	  System.out.println("End createJUnitRunConfiguration(): " + projectName);
    	  System.out.println("--------------------------------------------------------------------");
    	  return workingCopy;
      }
      
      
   }//end class