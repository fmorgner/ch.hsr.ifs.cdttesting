/*******************************************************************************
 * Copyright (c) 2010 Institute for Software, HSR Hochschule fuer Technik  
 * Rapperswil, University of applied sciences and others
 * All rights reserved.
 * 
 * Contributors:
 *     Institute for Software - initial API and implementation
 ******************************************************************************/
package ch.hsr.ifs.cdttesting.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import ch.hsr.ifs.cdttesting.cdttest.CDTTestingTest;
import ch.hsr.ifs.cdttesting.rts.junit4.RtsFileInfo;

public class ExternalResourceHelper {

	private static boolean isLoaded = false;
	private static String externalTextResourceAbsolutePath;
	public static final String NL = System.getProperty("line.separator");
	public static final char PATH_SEGMENT_SEPARATOR = File.separatorChar;

	public static void copyPluginResourcesToTestingWorkspace(Class<? extends CDTTestingTest> testClass) {
		if (!isLoaded) {
			try {
				RtsFileInfo testInfo = new RtsFileInfo(testClass);
				URI rootUri = ResourcesPlugin.getWorkspace().getRoot().getLocationURI();
				IPath rootPath = new Path(rootUri.getPath());
				String externalTextResourceRelativePath = testInfo.getexternalTextResourcePath();
				externalTextResourceAbsolutePath = getTargetFilePath(externalTextResourceRelativePath, rootPath);
				deleteFolder(externalTextResourceAbsolutePath);
				createFolder(externalTextResourceAbsolutePath);
				try {
					Enumeration<?> externalFilesEnumeration = testInfo.getBundle().findEntries(externalTextResourceRelativePath, "*", true);
					createFiles(rootPath, externalFilesEnumeration, externalTextResourceAbsolutePath);
				} finally {
					testInfo.closeReaderStream();
				}
				isLoaded = true;
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (CoreException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private static void createFiles(IPath rootPath, Enumeration<?> externalFilesEnumeration, String externalTextResourceAbsolutePath) {
		if (externalFilesEnumeration == null) {
			System.err.println("missing resource '" + externalTextResourceAbsolutePath + "'");
			return;
		}
		while (externalFilesEnumeration.hasMoreElements()) {
			URL url = (URL) externalFilesEnumeration.nextElement();
			String targetFilePath = getTargetFilePath(url.getPath(), rootPath);
			if (isFolderURL(targetFilePath)) {
				createFolder(targetFilePath);
			} else {
				createFile(url, targetFilePath);
			}
		}
	}

	private static void deleteFolder(String folderPath) {
		File file = new File(folderPath);
		if (!file.exists()) {
			return;
		}
		if (!recursiveDeleteDirContenteleteDir(file)) {
			System.err.println("Failed to clean up old resources in " + folderPath + " while setting up additional test resources.");
		}
	}

	public static boolean recursiveDeleteDirContenteleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = recursiveDeleteDirContenteleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

	private static void createFile(URL url, String targetFilePath) {
		File file = new File(targetFilePath);
		try {
			createFolder(file.getParentFile());
			if (!file.createNewFile()) {
				System.err.println("Failed to create file " + targetFilePath + " while setting up additional test resources.");
				return;
			}
			addFileContent(file, url);
		} catch (IOException e) {
			System.err.println("Failed to create file " + targetFilePath + " while setting up additional test resources.");
		}
	}

	private static void addFileContent(File fileToWrite, URL sourceUrl) {
		try {
			BufferedReader sourceReader = new BufferedReader(new InputStreamReader(sourceUrl.openStream()));
			BufferedWriter targetWriter = new BufferedWriter(new FileWriter(fileToWrite));
			String line;
			while ((line = sourceReader.readLine()) != null) {
				targetWriter.write(line);
				targetWriter.write(NL);
			}
			targetWriter.close();
		} catch (IOException e) {
			System.err.println("Failed to read plugin resource stream " + sourceUrl + " while setting up additional test resources.");
		}
	}

	private static boolean isFolderURL(String targetFilePath) {
		return targetFilePath.endsWith(Character.toString(PATH_SEGMENT_SEPARATOR));
	}

	private static void createFolder(String targetFilePath) {
		createFolder(new File(targetFilePath));
	}

	private static void createFolder(File file) {
		if (!file.exists() && !file.mkdirs()) {
			System.err.println("Failed to create folder " + file.getAbsolutePath() + " while setting up additional test resources.");
		}
	}

	private static String getTargetFilePath(String postfix, IPath prefix) {
		return prefix.append(postfix).toOSString();
	}

	public static String makeExternalResourceAbsolutePath(String relativePath) {
		return getTargetFilePath(relativePath, new Path(externalTextResourceAbsolutePath));
	}

}
