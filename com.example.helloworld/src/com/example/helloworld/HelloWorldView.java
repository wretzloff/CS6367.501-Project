package com.example.helloworld;
	
	import org.eclipse.swt.widgets.*;
	import org.eclipse.swt.SWT;
	import org.eclipse.swt.layout.*;
	import org.eclipse.ui.part.ViewPart;

	import java.io.*;
	import java.util.*;
	import java.text.*;
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
		   
		   textStatusArea = new Text(top, SWT.MULTI | SWT.BORDER |SWT.H_SCROLL | SWT.V_SCROLL);//| SWT.WRAP);
		   
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
      
      private void displayStatusMessage(String message)
      {
    	  textStatusArea.append(message + "\n");
    	  //int a = ((GridData)textStatusArea.getLayoutData()).heightHint;
    	  //System.out.println("aaa  " + a);
    	  //a++;
    	  //((GridData)textStatusArea.getLayoutData()).heightHint = a;
      }
      
      private void printStatusMessageToSTDOut(String message)
      {
    	  Date date = new Date();
    	  DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
    	  String dateFormatted = formatter.format(date);
    	  System.out.println("[MUTANTDEBUG] [" + dateFormatted + "] " + message);
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
    			
    			  //Set up a listener that will be notified when a test launch finishes.
				  setUpTestRunListener(directoryPath);
    			  
    			  //Generate a mutation plan for this project.
    			  ArrayList<String> mutationPlans = createMutationPlan(projectName, directoryPath);
    			  
    			  //For each mutation in the mutation plan
    			  for (int i = 0; i < mutationPlans.size(); i++) 
    			  {
    				  //Create mutant project name.
    				  String projectCopyName = projectName + "_mutant" + i;
    				  
    				  //Parse the information needed from this mutation
        			  String handleId = getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 2);
    				  int startPosition = Integer.parseInt(getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 4));
    				  int length = Integer.parseInt(getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 5));
    				  String newSource = getIthPieceOfDataFromMutationPlanString(mutationPlans.get(i), 7);
    				  
    				  //Modify the handle ID to point to the copy of the project instead of the original project
    				  handleId = handleId.replaceFirst("=" + projectName + "/", "=" + projectCopyName + "/");
    				  
    				  //Delete any existing project with that name.
    				  deleteProject(projectCopyName);
    				  if(checkIfProjectExists(projectCopyName))
    				  {
    					  printStatusMessageToSTDOut(projectCopyName + ": Project already exists. Moving to next mutant.");
    					  displayStatusMessage(projectCopyName + ": Project already exists. Moving to next mutant.");
    					  String filePath = directoryPath + "/" + projectCopyName + " - project_already_exists.txt";
    					  printArrayListOfStringsToFile(filePath, new ArrayList<String>());
    					  continue;
    				  }
    				  
    				  //Create a copy of the target project so that it can be mutated
    				  copyProject(projectName, projectCopyName);
    				  if(!checkIfProjectExists(projectCopyName))
    				  {
    					  printStatusMessageToSTDOut(projectCopyName + ": Project could not be created. Moving to next mutant.");
    					  displayStatusMessage(projectCopyName + ": Project could not be created. Moving to next mutant.");
    					  String filePath = directoryPath + "/" + projectCopyName + " - failure_to_create_mutant.txt";
    					  printArrayListOfStringsToFile(filePath, new ArrayList<String>());
    					  continue;
    				  }
    				  
    				  //Perform the specified mutation
    				  replaceSourceCode(projectCopyName, startPosition, length, newSource, handleId);
    				  if(!verifySourceCode(projectCopyName, startPosition, newSource, handleId))
    				  {
    					  printStatusMessageToSTDOut(projectCopyName + ": Failed to modify source code. Moving to next mutant.");
    					  displayStatusMessage(projectCopyName + ": Failed to modify source code. Moving to next mutant.");
    					  String filePath = directoryPath + "/" + projectCopyName + " - failure_to_modify_source.txt";
    					  printArrayListOfStringsToFile(filePath, new ArrayList<String>());
    					  continue;
    				  }
    				  
    				  //Add JUnit to the project copy's build path
    				  addJUnitToBuildPath(projectCopyName);
    				  if(!buildPathContainsJUnit(projectCopyName))
    				  {
    					  printStatusMessageToSTDOut(projectCopyName + ": JUnit not available. Moving to next mutant.");
    					  displayStatusMessage(projectCopyName + ": JUnit not available. Moving to next mutant.");
    					  String filePath = directoryPath + "/" + projectCopyName + " - JUnit_not_available.txt";
    					  printArrayListOfStringsToFile(filePath, new ArrayList<String>());
    					  continue;
    				  }
    				  
    				  //Create a new launch configuration that will launch all JUnit tests.
    				  ILaunchConfiguration launchConfiguration = createJUnitRunConfiguration(projectCopyName);
    				  
    				  //Error check: check for build errors
    				  if(hasBuildErrors(projectCopyName))
    				  {
						  printStatusMessageToSTDOut(projectCopyName + ": Build errors. Moving to next mutant.");
						  displayStatusMessage(projectCopyName + ": Build errors. Moving to next mutant.");
						  String filePath = directoryPath + "/" + projectCopyName + " - build_errors.txt";
    		    		  printArrayListOfStringsToFile(filePath, new ArrayList<String>());
    		    		  continue;
    				  }
  
    				  //Execute JUnit tests on project copy.
    				  executeTests(projectCopyName, launchConfiguration, directoryPath, timeout);
    				  
    				  //Delete launch configuration now that we're done with it
    				  deleteJUnitRunConfiguration(launchConfiguration);
    				  
    				  //Clean up the project copy now that we're done with it
    				  deleteProject(projectCopyName);
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
    		  displayStatusMessage("Please make a valid selection.");
    	  }  
      }//end startButtonPressed()
      
      private void addJUnitToBuildPath(String projectName)
      {
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  printStatusMessageToSTDOut("Begin addJUnitToBuildPath(): " + projectName);
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
			
			for(int i=0; i<5; i++)
			{
  			  if(buildPathContainsJUnit(projectName))
  			  {
  				  break;
  			  }
  			  else
  			  {
  				  printStatusMessageToSTDOut(projectName + " addJUnitToBuildPath(): pending JUnit add.");
  				  Thread.sleep(3000);
  			  }
			}
		  } 
		  catch (JavaModelException | InterruptedException e) 
		  {
			  // TODO Auto-generated catch block
			  //e.printStackTrace();
    		  displayStatusMessage(projectName + ": Error adding JUnit to build path.");
    		  printStatusMessageToSTDOut(projectName + " Exception: " + e.getMessage());
		  }
		  printStatusMessageToSTDOut("end addJUnitToBuildPath(): " + projectName);
		  printStatusMessageToSTDOut("--------------------------------------------------------------------");
      }//end addJUnitToBuildPath()
      
      private boolean hasBuildErrors(String projectName)
      {
    	  boolean returnValue = false;
    	  IProject projectCopy = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		  IMarker[] markers;
		  try 
		  {
			  markers = projectCopy.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
			  for (IMarker marker: markers)
			  {
				  Integer severityType = (Integer) marker.getAttribute(IMarker.SEVERITY);
				  if (severityType.intValue() == IMarker.SEVERITY_ERROR)
				  {
					  printStatusMessageToSTDOut("Marker: " + marker.getResource());
					  returnValue = true;
				  }
			  }
		  } 
		  catch (CoreException e) 
		  {
			  // TODO Auto-generated catch block
			  //e.printStackTrace();
			  printStatusMessageToSTDOut(projectName + " Exception: " + e.getMessage());
		  }
		  
    	  return returnValue;
      }
      
      private boolean buildPathContainsJUnit(String projectName)
      {
    	  boolean returnValue = false;
    	  IProject projectCopy = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		  IJavaProject javaProject = JavaCore.create(projectCopy);
		  try 
		  {
			  IClasspathEntry[] rawClasspath = javaProject.getRawClasspath();
			  for(IClasspathEntry item : rawClasspath)
			  {
				  if(item.getPath().toString().contains("org.eclipse.jdt.junit.JUNIT_CONTAINER"))
				  {
					  returnValue = true;
				  }
			  }
		  }
		  catch (JavaModelException e) 
		  {
			  // TODO Auto-generated catch block
			  //e.printStackTrace();
    		  printStatusMessageToSTDOut(projectName + " Exception: " + e.getMessage());
		  }
		  
		  return returnValue;
      }
      
      private void setUpTestRunListener(String directoryPath)
      {
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  printStatusMessageToSTDOut("Begin setUpTestRunListener(): ");
    	  
    	  //Create a listener to listen for the completion of the tests, so the results can be logged.
		  JUnitCore.addTestRunListener(new TestRunListener() {
			  public void sessionFinished(ITestRunSession session) 
	    	  {
	        	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
	        	  printStatusMessageToSTDOut("Begin sessionFinished(): ");
	        	
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
	    		  
	        	  printStatusMessageToSTDOut("End sessionFinished(): ");
	        	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
	    	  }
		  });
    	  printStatusMessageToSTDOut("End setUpTestRunListener(): ");
		  printStatusMessageToSTDOut("--------------------------------------------------------------------");
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
      
      private void executeTests(String projectCopyName, ILaunchConfiguration launchConfiguration, String directoryPath, int timeout)
      {
    	  try
    	  {
    		  printStatusMessageToSTDOut("--------------------------------------------------------------------");
        	  printStatusMessageToSTDOut("Begin executeTests(): " + projectCopyName);
    		  printStatusMessageToSTDOut("Run configuration name: " + launchConfiguration.getName());
			  printStatusMessageToSTDOut("Project: " + projectCopyName);
			  printStatusMessageToSTDOut("Run configuration type: " + launchConfiguration.getType().getName());
			  displayStatusMessage(projectCopyName + ": Executing test cases.");
			  
			  //Launch the tests.
			  ILaunch launch = launchConfiguration.launch(ILaunchManager.RUN_MODE, null);
			  
			  //Wait some time for the launch to complete, and if it hasn't completed, terminate it.
			  long beginTime = System.currentTimeMillis();
			  while(!launch.isTerminated())
			  {
				  displayStatusMessage(projectCopyName + ": Tests not yet completed.");
				  printStatusMessageToSTDOut("Tests for " + projectCopyName + " not yet completed.");
				  
				  if(System.currentTimeMillis() - beginTime > timeout)
				  {
					  launch.terminate();
					  displayStatusMessage(projectCopyName + ": Tests exceded timeout: " + timeout);
					  printStatusMessageToSTDOut("Tests for " + launchConfiguration.getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", "") + " exceded timeout: " + timeout);
					  String filePath = directoryPath + "/" + projectCopyName + " - tests_timed_out.txt";
		    		  printArrayListOfStringsToFile(filePath, new ArrayList<String>());
		    		  break;
				  }
				  
				  Thread.sleep(10000);
			  }
				  
			  /*Thread.sleep(timeout);
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
				  
				  displayStatusMessage(projectCopyName + ": Tests exceded timeout: " + timeout);
				  printStatusMessageToSTDOut("Tests for " + launchConfiguration.getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", "") + " exceded timeout: " + timeout);
				  String filePath = directoryPath + "/" + projectCopyName + " - tests_timed_out.txt";
	    		  printArrayListOfStringsToFile(filePath, new ArrayList<String>());
			  }*/
				  
			  displayStatusMessage(projectCopyName + ": Finished executing test cases.");
			  printStatusMessageToSTDOut("End executeTests(): " + projectCopyName);
	    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  }
    	  catch (CoreException | InterruptedException e ) 
    	  {
  			// TODO Auto-generated catch block
    		  printStatusMessageToSTDOut("ExecuteTests() exception: " + projectCopyName + " " + e.getMessage());
    	  } 
    	  
      }//end executeTests()
      
      private void replaceSourceCode(String projectName, int startPosition, int length, String newSource, String handleId)
      {
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  printStatusMessageToSTDOut("Begin replaceSourceCode(): " + handleId + " " + startPosition);
    	  displayStatusMessage(projectName + ": Replacing source code.");
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
    		  //e.printStackTrace();
    		  displayStatusMessage(projectName + ": Error modifing source code.");
    		  printStatusMessageToSTDOut("Exception: " + e.getMessage());
    	  }
    	  
    	  displayStatusMessage(projectName + ": Finished replacing source code.");
    	  printStatusMessageToSTDOut("End replaceSourceCode(): "  + handleId + " " + startPosition);
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
      }
      
      private boolean verifySourceCode(String projectName, int startPosition, String newSource, String handleId)
      {
    	  boolean returnValue = false;
    	  
		  try 
    	  {
			  int length = newSource.length();
	    	  
	    	  //Get ahold of the ICompilationUnit represented by the handle ID
	    	  ICompilationUnit iCompilationUnit = (ICompilationUnit)JavaCore.create(handleId);
	    	  CompilationUnit astRoot = parse_iCompilation_Unit_To_CompilationUnit(iCompilationUnit);
			  AST ast = astRoot.getAST();
			  TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);
			
			  //Find the target node
			  NodeFinder myNodeFinder = new NodeFinder(typeDecl, startPosition, length);
			  ASTNode oldNode = myNodeFinder.getCoveredNode();
			  
			  //Check for equivalency
			  if(oldNode.toString().equals(newSource))
			  {
				  returnValue = true;
			  }
    	  }
		  catch(Exception e)
		  {
			  printStatusMessageToSTDOut(projectName + " verifySourceCode() exception: " + e.getMessage());
		  }
    	  
		  
    	  return returnValue;
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
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  printStatusMessageToSTDOut("Begin copyProject(): " + projectName);
    	  displayStatusMessage(cloneName + ": Creating mutant.");
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
	    	    	printStatusMessageToSTDOut("Project " + clone.getName() + " not finished being created yet.");
	    	    	Thread.sleep(5000);
	    	    }
    	  } 
    	  catch (CoreException | InterruptedException e) 
    	  {
    		  // TODO Auto-generated catch block
    		  //e.printStackTrace();
    		  displayStatusMessage(cloneName + ": Error creating mutant.");
    		  printStatusMessageToSTDOut("CoreException: " + e.getMessage());
    	  } 
    	  
    	  displayStatusMessage(cloneName + ": Finished creating mutant.");
    	  printStatusMessageToSTDOut("End copyProject(): " + projectName);
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  return cloneName;  
      }//end copyProject()
      
      //Given a project name, this method will create a copy of that project.
      private void deleteProject(String projectName) 
      {
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  printStatusMessageToSTDOut("Begin deleteProject(): " + projectName);
    	  
    	  if(checkIfProjectExists(projectName))
    	  {
    		  printStatusMessageToSTDOut("" + projectName + ": Deleting project.");
    		  displayStatusMessage(projectName + ": Deleting project.");
    		  try 
    		  {
    			  IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    			  project.delete(true, null);
    			  
    			  //Do not proceed until the we ensure that the project no longer exists.
    	    	  while(project.exists())
    	    	  {
    	    		  printStatusMessageToSTDOut("" + projectName + ": pending deletion.");
    	    		  displayStatusMessage(projectName + ": pending deletion.");
    	    		  Thread.sleep(10000);
    	    		  
    	    	  }
    		  } 
    		  catch (CoreException | InterruptedException e) 
    		  {
    			  // TODO Auto-generated catch block
    			  //e.printStackTrace();
    			  printStatusMessageToSTDOut("deleteProject(): " + projectName + " " + e.getMessage());
    		  } 
    		  
    	  }
    	  
    	  displayStatusMessage(projectName + ": Finished deleting project.");
    	  printStatusMessageToSTDOut("End deleteProject(): " + projectName);
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
      }
      
      private boolean checkIfProjectExists(String projectName)
      {
    	  IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    	  if(project.exists())
    	  {
    		  return true;
    	  }
    	  else
    	  {
    		  return false;
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
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  printStatusMessageToSTDOut("Begin createFolderForResults(): " + projectName);
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
        	    	  printStatusMessageToSTDOut("Created directory: " + directoryPath);
        	      } 
        	      else 
        	      {
        	    	  printStatusMessageToSTDOut("Failed to create directory: " + directoryPath);
        	      }
    		  }
    	  } 
    	  catch(Exception e)
    	  {
    	      e.printStackTrace();
    	  } 
		  
    	  printStatusMessageToSTDOut("End createFolderForResults(): " + projectName);
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
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
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  printStatusMessageToSTDOut("Begin createMutationPlan(): " + projectName);
    	  String mutationPlanPath = directoryPath + "\\MutationPlan.txt";
    	  displayStatusMessage(projectName + ": Building mutation plan: " + mutationPlanPath);
    	  
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
		  
		  displayStatusMessage(projectName + ": Finished building mutation plan: " + mutationPlanPath);
    	  printStatusMessageToSTDOut("End createMutationPlan(): " + projectName);
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  return mutations;
      }
      
      private void deleteJUnitRunConfiguration(ILaunchConfiguration launchConfiguration) 
      {
    	  try 
    	  {
    		  launchConfiguration.delete();
    		  for(int i=0; i<5; i++)
    		  {
    			  if(!launchConfiguration.exists())
    			  {
    				  break;
    			  }
    			  else
    			  {
    				  printStatusMessageToSTDOut("deleteJUnitRunConfiguration(): not finished deleting.");
    				  Thread.sleep(3000);
    			  }
    		  }
    	  } 
    	  catch (CoreException | InterruptedException e) 
    	  {
    		  // TODO Auto-generated catch block
    		  e.printStackTrace();
    		  printStatusMessageToSTDOut("deleteJUnitRunConfiguration() exception: " + e.getMessage());
    	  }
      }
      
      private ILaunchConfigurationWorkingCopy createJUnitRunConfiguration(String projectName) 
      {
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  printStatusMessageToSTDOut("Begin createJUnitRunConfiguration(): " + projectName);
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
    	  catch (CoreException e) 
    	  {
    		  printStatusMessageToSTDOut("createJUnitRunConfiguration(): " + projectName + " " + e.getMessage());
    	  }
    	  
    	  printStatusMessageToSTDOut("End createJUnitRunConfiguration(): " + projectName);
    	  printStatusMessageToSTDOut("--------------------------------------------------------------------");
    	  return workingCopy;
      }
      
      
   }//end class