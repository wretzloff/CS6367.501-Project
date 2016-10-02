package com.example.helloworld;
	
	import org.eclipse.swt.widgets.*;
	import org.eclipse.swt.SWT;
	import org.eclipse.ui.part.ViewPart;
	import java.util.ArrayList;
	import org.eclipse.core.resources.*;
	import org.eclipse.core.runtime.*;
	//import org.eclipse.jdt.core.*;
	//import org.eclipse.jdt.launching.JavaRuntime;


   public class HelloWorldView extends ViewPart 
   {
	  String[] projectNamesArray;
      public HelloWorldView() 
      {
    	  projectNamesArray = getProjectNames();
    	  
      }
      
      public void createPartControl(Composite parent) 
      {
    	  Label label = new Label(parent, SWT.WRAP);
    	  label.setText("Choose project to test: ");
         
    	  Combo combo = new Combo(parent, SWT.DROP_DOWN);
    	  combo.setItems(projectNamesArray);
      }
      
      public void setFocus() 
      {
         // set focus to my widget.  For a label, this doesn't
         // make much sense, but for more complex sets of widgets
         // you would decide which one gets the focus.
      }
      
      //Given a project name, this method will create a copy of that project.
      public IProject copyProject(String projectName) throws CoreException 
      {
    	    IProgressMonitor m = new NullProgressMonitor();
    	    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    	    IProject project = workspaceRoot.getProject(projectName);
    	    IProjectDescription projectDescription = project.getDescription();
    	    String cloneName = projectName + "_copy";
    	    // create clone project in workspace
    	    IProjectDescription cloneDescription = workspaceRoot.getWorkspace().newProjectDescription(cloneName);
    	    // copy project files
    	    project.copy(cloneDescription, true, m);
    	    IProject clone = workspaceRoot.getProject(cloneName);
    	    // copy the project properties
    	    cloneDescription.setNatureIds(projectDescription.getNatureIds());
    	    cloneDescription.setReferencedProjects(projectDescription.getReferencedProjects());
    	    cloneDescription.setDynamicReferences(projectDescription.getDynamicReferences());
    	    cloneDescription.setBuildSpec(projectDescription.getBuildSpec());
    	    cloneDescription.setReferencedProjects(projectDescription.getReferencedProjects());
    	    clone.setDescription(cloneDescription, null);
    	    return clone;
      }
      
      //Return an array of the names of the projects available in the workspace.
      public String[] getProjectNames()
      {
    	  //Create a temporary ArrayList to hold the names of the available projects
    	  ArrayList<String> projectsList = new ArrayList<String>();
    	  
    	  // Get the root of the workspace
    	  IWorkspace workspace = ResourcesPlugin.getWorkspace();
    	  IWorkspaceRoot root = workspace.getRoot();
    	  // Get all projects in the workspace
    	  IProject[] projects = root.getProjects();
    	  // Loop over all projects
    	  for (IProject project : projects) 
    	  {
    		  projectsList.add(project.getName());
    	  }
    	   
    	  String[] tempProjectsArray = new String[projectsList.size()];
    	  projectsList.toArray(tempProjectsArray);
    	  return tempProjectsArray;      
      }
      
   }