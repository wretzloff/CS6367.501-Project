package com.example.helloworld;

   import org.eclipse.swt.widgets.Composite;
   import org.eclipse.swt.widgets.Label;
   import org.eclipse.swt.SWT;
   import org.eclipse.ui.part.ViewPart;
   import org.eclipse.core.resources.*;
   import org.eclipse.core.runtime.*;
   import org.eclipse.jdt.core.*;
   import org.eclipse.jdt.launching.JavaRuntime;


   public class HelloWorldView extends ViewPart 
   {
      Label label;
      public HelloWorldView() 
      {
    	
    	// Get the root of the workspace
  		IWorkspace workspace = ResourcesPlugin.getWorkspace();
  		IWorkspaceRoot root = workspace.getRoot();
  		// Get all projects in the workspace
  		IProject[] projects = root.getProjects();
  		// Loop over all projects
  		for (IProject project : projects) {
  			System.out.println(project.getName());
  			try {
				copyProject(project.getName());
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
  		}
		  
    	  
      }
      public void createPartControl(Composite parent) 
      {
         label = new Label(parent, SWT.WRAP);
         label.setText("Hello World!");
         
      }
      public void setFocus() 
      {
         // set focus to my widget.  For a label, this doesn't
         // make much sense, but for more complex sets of widgets
         // you would decide which one gets the focus.
      }
      
      public void createProject()
      {
    	  /*
		  //create a project with name "TESTJDT"
		  IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		  IProject project = root.getProject("TESTJDT");
		  project.create(null);
		  project.open(null);
		  
		  //set the Java nature
		  IProjectDescription description = project.getDescription();
		  description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		  
		  //create the project
		  project.setDescription(description, null);
		  IJavaProject javaProject = JavaCore.create(project);
		  
		  //set the build path
		  IClasspathEntry[] buildPath = {
		  		JavaCore.newSourceEntry(project.getFullPath().append("src")),
		  				JavaRuntime.getDefaultJREContainerEntry() }; 
		  /*javaProject.setRawClasspath(buildPath, project.getFullPath().append(
		  				"bin"), null);
		  
		  //create folder by using resources package
		  IFolder folder = project.getFolder("src");
		  folder.create(true, true, null);
		   
		  //Add folder to Java element
		  IPackageFragmentRoot srcFolder = javaProject
		  				.getPackageFragmentRoot(folder);
		   
		  //create package fragment
		  IPackageFragment fragment = srcFolder.createPackageFragment(
		  		"com.programcreek", true, null);
		  
		  //init code string and create compilation unit
		  String str = "package com.programcreek;" + "\n"
		  	+ "public class Test  {" + "\n" + " private String name;"
		  	+ "\n" + "}";
		   
		  		ICompilationUnit cu = fragment.createCompilationUnit("Test.java", str,
		  				false, null);
		   
		  //create a field
		  IType type = cu.getType("Test");
		  type.createField("private String age;", null, true, null);
		  */  
      }
      
      public IProject copyProject(String projectName) throws CoreException {
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
   }