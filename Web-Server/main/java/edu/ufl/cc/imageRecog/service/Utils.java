package edu.ufl.cc.imageRecog.service;

public class Utils {
	
	public static String formatResultString(String imageUrl, String result) {
		String[] split = imageUrl.split("/");
		String imageName = split[split.length-1];
		return "[" + imageName + "," + result + "]\n";
	}

}
