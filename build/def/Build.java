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
import java.util.HashSet;
import java.util.Set;

import org.jerkar.api.depmanagement.JkDependencies;
import org.jerkar.api.file.JkFileTreeSet;
import org.jerkar.api.system.JkProcess;
import org.jerkar.api.utils.JkUtilsFile;
import org.jerkar.api.utils.JkUtilsSystem;
import org.jerkar.tool.builtins.javabuild.JkJavaBuild;
import org.jerkar.tool.JkDoc;
import org.jerkar.tool.JkOptions;

public class Build extends JkJavaBuild {

	@JkDoc("path to Mercurial executable")
	private String pathHg = "hg";
	
	@JkDoc("path to Gradle executable")
	private String pathGradle = "gradle";
	
	@JkDoc("path to Eclipse executable")
	private String pathEclipse = "eclipse";
	
	@JkDoc("path to OpenJDK folder")
	private File pathJDK = new File("openjdk");
	
	private static final String OpenJFXCommit = "149fdbc41c8f5ab43c0414b970d9133e1f4e9cbd";
	
	private void checkExecutables(String ... paths) {
		Set<String> missingPaths = new HashSet<>();
		for (String path : paths) {
			if (!isExecutable(path)) {
				missingPaths.add(path);
			}
		}
		if (!missingPaths.isEmpty()) {
			throw new Error("can't find executables: " + missingPaths
				+ "\nSee README.md for info on how to configure executable paths."
			);
		}
	}
	
	private boolean isExecutable(String path) {
		
		// check for absolute path first
		File file = new File(path);
		if (file.exists() && file.canExecute()) {
			return true;
		}
		
		// then ask the OS to resolve the executable
		if (JkUtilsSystem.IS_WINDOWS) {
			int result = JkProcess.of("where", "/q", path)
				.failOnError(false)
				.runSync();
			return result == 0;
		} else {
			int result = JkProcess.of("which", path)
				.failOnError(false)
				.runSync();
			return result == 0;
		}
	}
	
	public void clean() {
		File cwd = new File("").getAbsoluteFile();
		JkUtilsFile.tryDeleteDir(new File(cwd, "openjdk-noFX"));
		JkUtilsFile.tryDeleteDir(new File(cwd, "openjfx"));
		JkUtilsFile.tryDeleteDir(new File(cwd, "JFXGL"));
		JkUtilsFile.tryDeleteDir(new File(cwd, "JFXGL-demos"));
		JkUtilsFile.tryDeleteDir(new File(cwd, ".metadata"));
	}
	
	public void setup() {
		
		header("Checking options...");
		checkExecutables(pathHg, pathGradle, pathEclipse);
		log("Options look good");
		
		header("Setting up JFXGL development environment...");
		
		File cwd = new File("").getAbsoluteFile();
		
		// is the JDK already downloaded?
		if (!pathJDK.exists()) {
			error("no JDK found at " + pathJDK);
		}
		
		// make the noFX copy if needed			
		File jdkNoFXDir = new File(cwd, "openjdk-noFX");
		if (!jdkNoFXDir.exists()) {
			log("Copying JDK...");
			copyDirAndAttributes(pathJDK, jdkNoFXDir);
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
		}
			
		// build OpenJFX
		File gradleProps = new File(openjfxDir, "gradle.properties");
		if (!gradleProps.exists()) {
			try {
				
				log("Configuring OpenJFX build...");
				File gradlePropsTemplate = new File(openjfxDir, "gradle.properties.template");
				JkUtilsFile.copyFile(gradlePropsTemplate, gradleProps);
				JkUtilsFile.writeString(gradleProps, "\nJDK_HOME = " + jdkNoFXDir.getAbsolutePath(), true);
				
				log("Building OpenJFX...");
				run(openjfxDir, pathGradle);
				
				// copy the compiled classes to (what will be) the eclipse build dir
				for (String module : Arrays.asList("base", "controls", "fxml", "graphics")) {
					File src = new File(openjfxDir, "modules/" + module + "/build/classes/main");
					File dst = new File(openjfxDir, "modules/" + module + "/bin");
					dst.mkdirs();
					JkUtilsFile.copyDirContent(src, dst, true);
				}
				
			} catch (Throwable t) {
				
				// remove the gradle props file so we re-do this step next time
				gradleProps.delete();
				
				throw t;
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
			hg(openjfxDir, "patch", "--no-commit", "../JFXGL/openjfx.patch");
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
			
			run(cwd, pathEclipse,
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

	public void compileJfxrt() {
		File cwd = new File("").getAbsoluteFile();
		File openjfxDir = new File(cwd, "openjfx");
		run(openjfxDir, pathGradle);
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

	private void hg(File cwd, String ... args) {
		run(cwd, pathHg, concat("--noninteractive", args));
	}
	
	private void jerkar(File cwd, String task) {
		// NOTE: we're using jerkar embedded mode, so always use the jerkar in the cwd
		String cmd;
		if (JkUtilsSystem.IS_WINDOWS) {
			cmd = "./jerkar.bat";
		} else {
			cmd = "./jerkar";
		}
		run(cwd, cmd, task);
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
