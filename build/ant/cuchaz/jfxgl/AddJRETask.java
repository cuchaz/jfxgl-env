package cuchaz.jfxgl;

import java.io.File;

import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.launching.StandardVM;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;

public class AddJRETask extends Task {
	
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
		
		// make our VM
		IVMInstallType standardVMType = JavaRuntime.getVMInstallType(StandardVMType.ID_STANDARD_VM_TYPE);
		StandardVM vm = (StandardVM)standardVMType.createVMInstall(name);
		vm.setName(name);
		vm.setInstallLocation(file);
		
		// save the VM config
		try {
			JavaRuntime.saveVMConfiguration();
		} catch (CoreException ex) {
			System.err.println("can't save VM configuration");
			ex.printStackTrace(System.err);
		}
		
		System.out.println("Created JRE: " + name);
	}
}
