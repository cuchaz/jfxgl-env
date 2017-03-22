package cuchaz.jfxgl;

import java.io.File;

import org.apache.tools.ant.Task;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

public class ImportProjectTask extends Task {
	
	private String path;
	
	public void addText(String val) {
		path = val;
	}
	
	@Override
	public void execute() {
		
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		File projectFile = new File(path + "/.project");
		try {
			IProjectDescription description = workspace.loadProjectDescription(new Path(projectFile.getAbsolutePath()));
			IProject project = workspace.getRoot().getProject(description.getName());
			project.create(description, null);
			project.open(null);
			System.out.println("imported: " + description.getName());
		} catch (CoreException ex) {
			System.err.println("can't import project: " + projectFile.getAbsolutePath());
			ex.printStackTrace(System.err);
		}
	}
}
