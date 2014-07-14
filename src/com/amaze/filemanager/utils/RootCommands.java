package com.amaze.filemanager.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class RootCommands {
//under construction dont read
	private static final String UNIX_ESCAPE_EXPRESSION = "(\\(|\\)|\\[|\\]|\\s|\'|\"|`|\\{|\\}|&|\\\\|\\?)";

	public static String getCommandLineString(String input) {
		return input.replaceAll(UNIX_ESCAPE_EXPRESSION, "\\\\$1");
	}

	
	public static ArrayList<String> listFiles(String path, boolean showhidden) {
		ArrayList<String> mDirContent = new ArrayList<String>();

		try {
			String[] cmd = new String[] { "su", "-c", "ls", "-a",
					getCommandLineString(path) };
			Process p = Runtime.getRuntime().exec(cmd);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					p.getInputStream()));

			String line;
			while ((line = in.readLine()) != null) {
				if (!showhidden) {
					if (line.toString().charAt(0) != '.')
						mDirContent.add(path + "/" + line);
				} else {
					mDirContent.add(path + "/" + line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return mDirContent;
	}
}
