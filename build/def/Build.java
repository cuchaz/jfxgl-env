/*************************************************************************
 * Copyright (C) 2017, Jeffrey W. Martin "Cuchaz"
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License Version 2 with
 * the classpath exception, as published by the Free Software Foundation.
 * 
 * See LICENSE.txt in the project root folder for the full license.
 *************************************************************************/


import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;

public class Build extends JkJavaBuild {
	
	// by default, we'll omit absolute paths and let the operating system find the paths for these tools
	// if for some reason, the OS doesn't get it right, feel free to edit this script and add absolute paths
	private static final String HG = "hg";
	private static final String GRADLE = "gradle";
	private static final String ECLIPSE = "eclipse";
	
	// by default, we'll assume the JDK is at this relative path
	// if it's not, feel free to edit this script and add the absolute path here
	private static final File JDKDir = new File("openjdk-8u121");
	
	private static final String OpenJFXCommit = "149fdbc41c8f5ab43c0414b970d9133e1f4e9cbd";
	
	public void clean() {
		File cwd = new File("").getAbsoluteFile();
		JkUtilsFile.tryDeleteDir(new File(cwd, "openjdk-8u121-noFX"));
		JkUtilsFile.tryDeleteDir(new File(cwd, "openjfx"));
		JkUtilsFile.tryDeleteDir(new File(cwd, "JFXGL"));
		JkUtilsFile.tryDeleteDir(new File(cwd, "JFXGL-demos"));
		JkUtilsFile.tryDeleteDir(new File(cwd, ".metadata"));
	}
	
	public void setup() {
		
		header("Setting up JFXGL development environment...");
			
		File cwd = new File("").getAbsoluteFile();
		
		// is the JDK already downloaded?
		if (!JDKDir.exists()) {
			error("no JDK found at " + JDKDir);
		}
		
		// make the noFX copy if needed
		File jdkNoFXDir = new File(cwd, "openjdk-8u121-noFX");
		if (!jdkNoFXDir.exists()) {
			log("Copying JDK...");
			copyDirAndAttributes(JDKDir, jdkNoFXDir);
			log("JDK copied");
		}
		
		// delete the JavaFX jar if needed
		File javafxJar = new File(jdkNoFXDir, "jre/lib/ext/jfxrt.jar");
		if (javafxJar.exists()) {
			javafxJar.delete();
			log("Deleted JavaFX Jar");
		}
		
		// clone OpenJFX
		File openjfxDir = new File(cwd, "openjfx");
		if (!openjfxDir.exists()) {
			openjfxDir.mkdirs();
			
			log("Downloading OpenJFX... (It's ~690 MiB, so this can take a while)");
			hg(openjfxDir, "clone", "-r", OpenJFXCommit, "http://hg.openjdk.java.net/openjfx/8u-dev/rt", ".");
			log("Downloaded OpenJFX");
			
			// build OpenJFX
			log("Configuring OpenJFX build...");
			File gradlePropsTemplate = new File(openjfxDir, "gradle.properties.template");
			File gradleProps = new File(openjfxDir, "gradle.properties");
			JkUtilsFile.copyFile(gradlePropsTemplate, gradleProps);
			JkUtilsFile.writeString(gradleProps, "\nJDK_HOME = " + jdkNoFXDir.getAbsolutePath(), true);
			log("Building OpenJFX...");
			run(openjfxDir, GRADLE);
			
			// copy the compiled classes to (what will be) the eclipse build dir
			for (String module : Arrays.asList("base", "controls", "fxml", "graphics")) {
				File src = new File(openjfxDir, "modules/" + module + "/build/classes/main");
				File dst = new File(openjfxDir, "modules/" + module + "/bin");
				dst.mkdirs();
				JkUtilsFile.copyDirContent(src, dst, true);
			}
		}
		
		// clone JFXGL
		File jfxglDir = new File(cwd, "JFXGL");
		if (!jfxglDir.exists()) {
			jfxglDir.mkdirs();
			
			log("Downloading JFXGL...");
			hg(jfxglDir, "clone", "https://cuchaz@bitbucket.org/cuchaz/jfxgl", ".");
			jerkar(jfxglDir, "eclipse#generateFiles");
			
			// patch OpenJFX
			hg(openjfxDir, "revert", "--all");
			hg(openjfxDir, "patch", "--no-commit", "../JFXGL/openjfx.8u121.patch");
		}
		
		// clone JFXGL-demos
		File demosDir = new File(cwd, "JFXGL-demos");
		if (!demosDir.exists()) {
			demosDir.mkdirs();
			hg(demosDir, "clone", "https://cuchaz@bitbucket.org/cuchaz/jfxgl-demos", ".");
			jerkar(demosDir, "eclipse#generateFiles");
		}
		
		// config eclipse workspace
		File eclipseDir = new File(cwd, ".metadata");
		if (!eclipseDir.exists()) {
			log("Creating Eclipse workspace...");
			eclipseDir.mkdirs();
			
			// fix the buildSrc classpath
			File classpathFile = new File(cwd, "openjfx/buildSrc/.classpath");
			File libsDir = new File(cwd, "openjfx/build/libs");
			String classpathText = JkUtilsFile.read(classpathFile);
			classpathText = classpathText.replaceAll("\\.\\./build/libs", libsDir.getAbsolutePath());
			JkUtilsFile.writeString(classpathFile, classpathText, false);
			
			run(cwd, ECLIPSE,
				"-nosplash",
				"-data", cwd.getAbsolutePath(),
				"-application", "org.eclipse.ant.core.antRunner",
				"-buildfile", new File(cwd, "build/def/setupEclipse.xml").getAbsolutePath()
			);
		}
		
		// we're done!
		log("");
		log("");
		log("And we're all done!");
		log("");
		log("");
	}
	
	private void copyDirAndAttributes(File src, File dst) {
		
		// use the new Java File API so we preserve file attributes (like executable permissions)
		Path srcPath = src.toPath();
		Path dstPath = dst.toPath();
		
		try {
			Files.walkFileTree(srcPath, new SimpleFileVisitor<Path>() {
				
				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
				throws IOException {
					Files.createDirectories(dstPath.resolve(srcPath.relativize(dir)));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.copy(file, dstPath.resolve(srcPath.relativize(file)));
					return FileVisitResult.CONTINUE;
				}
			});
			
		} catch (IOException ex) {
			error(ex.getMessage());
		}
	}

	private static void hg(File cwd, String ... args) {
		run(cwd, HG, concat("--noninteractive", args));
	}
	
	private static void jerkar(File cwd, String task) {
		// NOTE: we're using jerkar embedded mode, so always use the jerkar in the cwd
		run(cwd, "./jerkar", task);
	}
	
	private static String[] concat(String val, String[] vals) {
		String[] newvals = new String[vals.length + 1];
		newvals[0] = val;
		System.arraycopy(vals, 0, newvals, 1, vals.length);
		return newvals;
	}
	
	private static void run(File cwd, String cmd, String ... args) {
		int result = JkProcess.of(cmd, args)
			.withWorkingDir(cwd)
			.failOnError(false)
			.runSync();
		if (result != 0) {
			error("Failed to run command: %s", cmd);
		}
	}
	
	private static void header(String msg, Object ... args) {
		msg = String.format(msg, args);
		System.out.println();
		System.out.println(msg.toUpperCase());
		System.out.println();
	}
	
	private static void log(String msg, Object ... args) {
		msg = String.format(msg, args);
		System.out.println(msg);
	}
	
	private static void error(String msg, Object ... args) {
		msg = String.format(msg, args);
		System.out.println();
		System.out.println("/=============");
		System.out.println("|   ERRROR:");
		System.out.println("|-------------");
		System.out.println("|   " + msg);
		System.out.println("\\=============");
		System.out.println();
		System.out.println();
		throw new Error(msg);
	}
	
	
	// build script bits needed to compile the ant tasks
	
	@Override
	public JkDependencies dependencies() {
		return JkDependencies.builder()
			.on("org.apache.ant:ant:1.10.1")
			.on("org.eclipse.core:runtime:3.10.0-v20140318-2214")
			.on("org.eclipse.core:resources:3.3.0-v20070604")
			.on("org.eclipse.jdt:org.eclipse.jdt.core:3.12.2")
			.on("org.eclipse.jdt:org.eclipse.jdt.launching:3.8.101")
			.build();
	}
	
	@Override
	public JkFileTreeSet editedSources() {
		return JkFileTreeSet.of(file("build/ant"));
	}
}
