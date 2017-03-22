package cuchaz.jfxgl;

import java.io.File;

import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;

public class SetClasspathVarTask extends Task {
	
	private String name;
	private String path;
	
	public void setName(String val) {
		name = val;
	}
	
	public void addText(String val) {
		path = val;
	}

	@Override
	public void execute() {
		
		// resolve the path
		if (path.startsWith("~")) {
			path = System.getProperty("user.home") + path.substring(1);
		}
		File file = new File(path);
		
		try {
			JavaCore.setClasspathVariable(name, new Path(file.getAbsolutePath()), null);
			System.out.println("set classpath var: " + name + "=" + file.getAbsolutePath());
		} catch (CoreException ex) {
			System.err.println("can't set classpath variable");
			ex.printStackTrace(System.err);
		}
		
		JavaRuntime.savePreferences();
	}
}
