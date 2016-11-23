package com.example.helloworld;
	
	import org.eclipse.swt.widgets.*;
	import org.eclipse.swt.SWT;
	import org.eclipse.swt.layout.*;
	import org.eclipse.ui.part.ViewPart;

	import java.io.*;
	import java.util.*;
	import java.util.List;
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
      
      public void startButtonPressed()
      {  
    		  String projectName = projectNamesArray[comboProjectsList.getSelectionIndex()];
    		  int timeout = Integer.parseInt(textTimeoutArea.getText());
    		  
    		  MutationTestingLogic runnable = new MutationTestingLogic(projectName, timeout, textStatusArea);
    	      Thread t = new Thread(runnable);
    	      t.start();
      }//end startButtonPressed()
      
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
      
   }//end class