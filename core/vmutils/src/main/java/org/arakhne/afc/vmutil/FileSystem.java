/* 
 * $Id$
 * 
 * Copyright (C) 2004-2013 Stephane GALLAND.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * This program is free software; you can redistribute it and/or modify
 */

package org.arakhne.afc.vmutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** An utility class that permits to deal with filenames.
 * 
 * @author $Author: galland$
 * @version $FullVersion$
 * @mavengroupid $GroupId$
 * @mavenartifactid $ArtifactId$
 */
public class FileSystem {

	static {
		URLHandlerUtil.installArakhneHandlers();

		String validChars = "[^\\\\/:*?\"<>|]"; //$NON-NLS-1$
		String bslashChar = "\\\\"; //$NON-NLS-1$

		StringBuilder pattern = new StringBuilder();
		pattern.append("^"); //$NON-NLS-1$
		pattern.append("(([a-zA-Z]:"); //$NON-NLS-1$
		pattern.append(validChars);
		pattern.append("*)|("); //$NON-NLS-1$
		pattern.append(validChars);
		pattern.append("+"); //$NON-NLS-1$
		pattern.append(bslashChar);
		pattern.append(validChars);
		pattern.append("+)|("); //$NON-NLS-1$
		pattern.append(bslashChar);
		pattern.append("))?("); //$NON-NLS-1$
		pattern.append(bslashChar);
		pattern.append(validChars);
		pattern.append("*)*"); //$NON-NLS-1$
		pattern.append("$"); //$NON-NLS-1$
		//"^([A-Za-z]:)?([^\\\\/:*?\"<>|]*\\\\)*[^\\\\/:*?\"<>|]*$"; //$NON-NLS-1$
		WINDOW_NATIVE_FILENAME_PATTERN = pattern.toString();
	}

	/** Regular expression pattern which corresponds to Windows native filename.
	 */
	private static final String WINDOW_NATIVE_FILENAME_PATTERN;

	/** Character used to specify a file extension.
	 */
	public static final char EXTENSION_SEPARATOR_CHAR = '.';

	/** String which is representing the current directory in a relative path.
	 */
	public static final String CURRENT_DIRECTORY = "."; //$NON-NLS-1$

	/** String which is representing the parent directory in a relative path.
	 */
	public static final String PARENT_DIRECTORY = ".."; //$NON-NLS-1$

	/** Character used to separate paths on an URL.
	 */
	public static final char URL_PATH_SEPARATOR_CHAR = '/';

	/** Character used to separate paths on an URL.
	 */
	public static final String URL_PATH_SEPARATOR = "/"; //$NON-NLS-1$

	/** String used to specify a file extension.
	 */
	public static final String EXTENSION_SEPARATOR = "."; //$NON-NLS-1$

	/** Prefix used to join in a Jar URL the jar filename and the inside-jar filename.
	 */
	public static final String JAR_URL_FILE_ROOT = "!/"; //$NON-NLS-1$

	private static final Random RANDOM = new Random();
	
	private static final DeleteOnExitHook deleteOnExitHook = new DeleteOnExitHook();
	
	private static Boolean isFileCompatibleWithURL = null;

	/** Replace the HTML entities by the current charset characters.
	 * 
	 * @param s
	 * @return decoded string or <var>s/<var>.
	 */
	private static String decodeHTMLEntities(String s) {
		if (s==null) return null;
		try {
			return URLDecoder.decode(s, Charset.defaultCharset().displayName());
		}
		catch (UnsupportedEncodingException _) {
			return s;
		}
	}
	
	/** Replace the special characters by HTML entities.
	 * 
	 * @param s
	 * @return decoded string or <var>s/<var>.
	 */
	private static String encodeHTMLEntities(String s) {
		if (s==null) return null;
		try {
			return URLEncoder.encode(s, Charset.defaultCharset().displayName());
		}
		catch (UnsupportedEncodingException _) {
			return s;
		}
	}

	/** Decode the given file to obtain a string representation
	 * which is compatible with the URL standard.
	 * This function was introduced to have a work around
	 * on the '\' character on Windows operating system.
	 * 
	 * @since 6.2
	 */
	private static String getFilePath(File f) {
		if (f==null) return null;
		return getFilePath(f.getPath());
	}

	/** Decode the given file to obtain a string representation
	 * which is compatible with the URL standard.
	 * This function was introduced to have a work around
	 * on the '\' character on Windows operating system.
	 * 
	 * @since 6.2
	 */
	private static String getFilePath(String f) {
		if (f==null) return null;
		if (isFileCompatibleWithURL==null) {
			isFileCompatibleWithURL = Boolean.valueOf(
					URL_PATH_SEPARATOR.equals(File.separator));
		}
		String filePath = f;
		if (!isFileCompatibleWithURL) {
			filePath = filePath.replaceAll(
					Pattern.quote(File.separator),
					Matcher.quoteReplacement(URL_PATH_SEPARATOR));
		}
		return filePath;
	}

	/** Replies if the given URL has a jar scheme.
	 * 
	 * @param url
	 * @return <code>true</code> if the given URL uses a jar scheme.
	 */
	public static boolean isJarURL(URL url) {
		return URISchemeType.JAR.isURL(url);
	}

	/** Replies the jar part of the jar-scheme URL.
	 * 
	 * @param url
	 * @return the URL of the jar file in the given URL, or <code>null</code>
	 * if the given URL does not use jar scheme.
	 */
	public static URL getJarURL(URL url) {
		if (!isJarURL(url)) return null;
		String path = url.getPath();
		int idx = path.lastIndexOf(JAR_URL_FILE_ROOT);
		if (idx>=0) path = path.substring(0, idx);
		try {
			return new URL(path);
		}
		catch(MalformedURLException _) {
			return null;
		}
	}

	/** Replies the file part of the jar-scheme URL.
	 * 
	 * @param url
	 * @return the file in the given URL, or <code>null</code>
	 * if the given URL does not use jar scheme.
	 */
	public static File getJarFile(URL url) {
		if (isJarURL(url)) {
			String path = url.getPath();
			int idx = path.lastIndexOf(JAR_URL_FILE_ROOT);
			if (idx>=0) return new File(decodeHTMLEntities(path.substring(idx+1)));
		}
		return null;
	}

	/** Replies the jar-schemed URL composed of the two given components.
	 * 
	 * @param jarFile is the URL to the jar file.
	 * @param insideFile is the name of the file inside the jar.
	 * @return the jar-schemed URL.
	 * @throws MalformedURLException when the URL is malformed.
	 */
	public static URL toJarURL(File jarFile, File insideFile) throws MalformedURLException {
		if (jarFile==null || insideFile==null) return null;
		return toJarURL(jarFile, getFilePath(insideFile));
	}

	/** Replies the jar-schemed URL composed of the two given components.
	 * 
	 * @param jarFile is the URL to the jar file.
	 * @param insideFile is the name of the file inside the jar.
	 * @return the jar-schemed URL.
	 * @throws MalformedURLException when the URL is malformed.
	 */
	public static URL toJarURL(File jarFile, String insideFile) throws MalformedURLException {
		return toJarURL(jarFile.toURI().toURL(), insideFile);
	}

	/** Replies the jar-schemed URL composed of the two given components.
	 * 
	 * @param jarFile is the URL to the jar file.
	 * @param insideFile is the name of the file inside the jar.
	 * @return the jar-schemed URL.
	 * @throws MalformedURLException when the URL is malformed.
	 */
	public static URL toJarURL(URL jarFile, File insideFile) throws MalformedURLException {
		if (jarFile==null || insideFile==null) return null;
		return toJarURL(jarFile, getFilePath(insideFile));
	}

	/** Replies the jar-schemed URL composed of the two given components.
	 * 
	 * @param jarFile is the URL to the jar file.
	 * @param insideFile is the name of the file inside the jar.
	 * @return the jar-schemed URL.
	 * @throws MalformedURLException when the URL is malformed.
	 */
	public static URL toJarURL(URL jarFile, String insideFile) throws MalformedURLException {
		if (jarFile==null || insideFile==null) return null;
		StringBuilder buf = new StringBuilder();
		buf.append("jar:"); //$NON-NLS-1$
		buf.append(jarFile.toExternalForm());
		buf.append(JAR_URL_FILE_ROOT);
		String path = getFilePath(insideFile);
		if (path.startsWith(URL_PATH_SEPARATOR)) {
			buf.append(path.substring(URL_PATH_SEPARATOR.length()));
		}
		else {
			buf.append(path);
		}
		return new URL(buf.toString());
	}

	/** Replies if the current operating system uses case-sensitive filename.
	 * 
	 * @return <code>true</code> if the filenames on the current file system are case sensitive,
	 * otherwise <code>false</code>
	 */
	public static boolean isCaseSensitiveFilenameSystem() {
		switch(OperatingSystem.getCurrentOS()) {
		case AIX:
		case BSD:
		case FREEBSD:
		case NETBSD:
		case OPENBSD:
		case LINUX:
		case SOLARIS:
		case HPUX:
			return true;
		case MACOSX:
		case WIN:
		case OTHER:
		default:
			return false;
		}
	}

	/** Replies the character used to separate the basename and the file extension.
	 * 
	 * @return the character used to separate the basename and the file extension.
	 */
	public static char getFileExtensionCharacter() {
		return EXTENSION_SEPARATOR_CHAR;
	}

	/** Replies the dirname of the specified file.
	 *
	 * @param filename is the name to parse.
	 * @return the dirname of the specified file.
	 * @see #shortBasename(File)
	 * @see #largeBasename(File)
	 * @see #basename(File)
	 * @see #extension(File)
	 */
	public static URL dirname(File filename) {
		if (filename==null) return null;
		String parent = getFilePath(filename.getParent());
		try {
			if (parent==null || "".equals(parent)) { //$NON-NLS-1$
				if (filename.isAbsolute()) return null;
				return new URL(URISchemeType.FILE.name(), "", CURRENT_DIRECTORY); //$NON-NLS-1$
			}
			return new URL(URISchemeType.FILE.name(), "", parent); //$NON-NLS-1$
		}
		catch(MalformedURLException _) {
			return null;
		}
	}

	/** Replies the dirname of the specified file.
	 *
	 * @param filename is the name to parse.
	 * @return the dirname of the specified file.
	 * @see #shortBasename(URL)
	 * @see #largeBasename(URL)
	 * @see #basename(URL)
	 * @see #extension(URL)
	 */
	public static URL dirname(URL filename) {
		if (filename==null) return null;

		URL prefix = null;
		String path;
		if (isJarURL(filename)) {
			prefix = getJarURL(filename);
			path = getFilePath(getJarFile(filename));
		}
		else
			path = filename.getPath();

		if ("".equals(path)) return null; //$NON-NLS-1$

		int idx = path.lastIndexOf(URL_PATH_SEPARATOR_CHAR);
		if (idx==path.length()-1)
			idx = path.lastIndexOf(URL_PATH_SEPARATOR_CHAR, path.length()-2);

		if (idx<0) {
			if (URISchemeType.getSchemeType(filename).isFileBasedScheme())
				path = CURRENT_DIRECTORY;
			else
				path = URL_PATH_SEPARATOR;
		}
		else {
			path = path.substring(0, idx+1);
		}

		try {
			if (prefix!=null) {
				return toJarURL(prefix, path);
			}
			URI uri = new URI(
					filename.getProtocol(), 
					filename.getUserInfo(), 
					filename.getHost(), 
					filename.getPort(), 
					decodeHTMLEntities(path),
					null,
					null);
			return uri.toURL();
		}
		catch (Throwable _) {
			//
		}

		try {
			return new URL(
					filename.getProtocol(), 
					filename.getHost(), 
					path);
		}
		catch (Throwable _) {
			//
		}
		return null;
	}

	/** Replies the basename of the specified file with the extension.
	 * <p>
	 * Caution: This function does not support URL format.
	 *
	 * @param filename is the name to parse.
	 * @return the basename of the specified file with the extension.
	 */
	public static String largeBasename(String filename) {
		if (filename==null) return null;
		assert(!isWindowsNativeFilename(filename));
		int end = filename.length();
		int idx;
		do {
			end--;
			idx = filename.lastIndexOf(File.separatorChar, end);
		}
		while (idx>=0 && end>=0 && idx>=end);
		if (idx<0) {
			if (end<filename.length()-1)
				return filename.substring(0, end+1);
			return filename;
		}
		return filename.substring(idx+1, end+1);
	}

	/** Replies the basename of the specified file with the extension.
	 *
	 * @param filename is the name to parse.
	 * @return the basename of the specified file with the extension.
	 */
	public static String largeBasename(File filename) {
		if (filename==null) return null;
		return filename.getName();
	}

	/** Replies the basename of the specified file with the extension.
	 *
	 * @param filename is the name to parse.
	 * @return the basename of the specified file with the extension.
	 */
	public static String largeBasename(URL filename) {
		if (filename==null) return null;
		String fullPath = filename.getPath();
		assert(!isWindowsNativeFilename(fullPath));
		int idx;
		int end = fullPath.length();
		do {
			end--;
			idx = fullPath.lastIndexOf(URL_PATH_SEPARATOR_CHAR, end);
		}
		while (idx>=0 && end>=0 && idx>=end);
		String r;
		if (idx<0) {
			if (end<fullPath.length()-1)
				r = fullPath.substring(0, end+1);
			else
				r = fullPath;
		}
		else 
			r = fullPath.substring(idx+1, end+1);
		return decodeHTMLEntities(r);
	}

	/** Reply the basename of the specified file without the last extension.
	 * <p>
	 * Caution: This function does not support URL format.
	 *
	 * @param filename is the name to parse.
	 * @return the basename of the specified file without the last extension.
	 * @see #shortBasename(String)
	 * @see #largeBasename(String)
	 */
	public static String basename(String filename) {
		if (filename==null) return null;
		assert(!isWindowsNativeFilename(filename));
		int end = filename.length();
		int idx;
		do {
			end--;
			idx = filename.lastIndexOf(File.separatorChar, end);
		}
		while (idx>=0 && end>=0 && idx>=end);
		String basename;
		if (idx<0) {
			if (end<filename.length()-1)
				basename = filename.substring(0, end+1);
			else
				basename = filename;
		}
		else
			basename =filename.substring(idx+1, end+1);
		idx = basename.lastIndexOf(getFileExtensionCharacter());
		if (idx<0) return basename;
		return basename.substring(0,idx);
	}

	/** Reply the basename of the specified file without the last extension.
	 *
	 * @param filename is the name to parse.
	 * @return the basename of the specified file without the last extension.
	 * @see #shortBasename(File)
	 * @see #largeBasename(File)
	 * @see #dirname(File)
	 * @see #extension(File)
	 */
	public static String basename(File filename) {
		if (filename==null) return null;
		String largeBasename = filename.getName();
		int idx = largeBasename.lastIndexOf(getFileExtensionCharacter());
		if (idx<=0) return largeBasename;
		return largeBasename.substring(0,idx);
	}

	/** Reply the basename of the specified file without the last extension.
	 *
	 * @param filename is the name to parse.
	 * @return the basename of the specified file without the last extension.
	 * @see #shortBasename(URL)
	 * @see #largeBasename(URL)
	 * @see #dirname(URL)
	 * @see #extension(URL)
	 */
	public static String basename(URL filename) {
		if (filename==null) return null;
		String largeBasename = filename.getPath();
		assert(!isWindowsNativeFilename(largeBasename));
		int end = largeBasename.length();
		int idx;
		do {
			end--;
			idx = largeBasename.lastIndexOf(URL_PATH_SEPARATOR_CHAR, end);
		}
		while (idx>=0 && end>=0 && idx>=end);
		String basename;
		if (idx<0) {
			if (end<largeBasename.length()-1)
				basename = largeBasename.substring(0, end+1);
			else
				basename = largeBasename;
		}
		else
			basename = largeBasename.substring(idx+1, end+1);
		idx = basename.lastIndexOf(getFileExtensionCharacter());
		if (idx>=0) basename = basename.substring(0,idx);
		return decodeHTMLEntities(basename);
	}

	/** Reply the basename of the specified file without all the extensions.
	 * <p>
	 * Caution: This function does not support URL format.
	 *
	 * @param filename is the name to parse.
	 * @return the basename of the specified file without all the extensions.
	 */
	public static String shortBasename(String filename) {
		if (filename==null) return null;
		if (isWindowsNativeFilename(filename)) {
			return shortBasename(normalizeWindowsNativeFilename(filename));
		}
		String normalizedFilename = getFilePath(filename);
		int idx;
		int end = normalizedFilename.length();
		do {
			end--;
			idx = normalizedFilename.lastIndexOf(URL_PATH_SEPARATOR_CHAR, end);
		}
		while (idx>=0 && end>=0 && idx>=end);
		String basename;
		if (idx<0) {
			if (end<normalizedFilename.length()-1)
				basename = normalizedFilename.substring(0, end+1);
			else
				basename = normalizedFilename;
		}
		else
			basename = normalizedFilename.substring(idx+1, end+1);

		idx = basename.indexOf(getFileExtensionCharacter());
		if (idx<0) return basename;
		return basename.substring(0,idx);
	}

	/** Reply the basename of the specified file without all the extensions.
	 *
	 * @param filename is the name to parse.
	 * @return the basename of the specified file without all the extensions.
	 */
	public static String shortBasename(File filename) {
		if (filename==null) return null;
		String largeBasename = filename.getName();
		int idx = largeBasename.indexOf(getFileExtensionCharacter());
		if (idx<0) return largeBasename;
		return largeBasename.substring(0,idx);
	}

	/** Reply the basename of the specified file without all the extensions.
	 *
	 * @param filename is the name to parse.
	 * @return the basename of the specified file without all the extensions.
	 */
	public static String shortBasename(URL filename) {
		if (filename==null) return null;
		String largeBasename = filename.getPath();
		assert(!isWindowsNativeFilename(largeBasename));
		int idx;
		int end = largeBasename.length();
		do {
			end--;
			idx = largeBasename.lastIndexOf(URL_PATH_SEPARATOR_CHAR, end);
		}
		while (idx>=0 && end>=0 && idx>=end);
		String basename;
		if (idx<0) {
			if (end<largeBasename.length()-1)
				basename = largeBasename.substring(0, end+1);
			else
				basename = largeBasename;
		}
		else
			basename = largeBasename.substring(idx+1, end+1);

		idx = basename.indexOf(getFileExtensionCharacter());
		if (idx>=0) basename = basename.substring(0,idx);
		return decodeHTMLEntities(basename);
	}

	/** Reply the extension of the specified file.
	 *
	 * @param filename is the name to parse.
	 * @return the extension of the specified file
	 * @see #shortBasename(File)
	 * @see #largeBasename(File)
	 * @see #basename(File)
	 * @see #dirname(File)
	 * @see #extensions(File)
	 */
	public static String extension(File filename) {
		if (filename==null) return null;
		String largeBasename = largeBasename(filename);
		int idx = largeBasename.lastIndexOf(getFileExtensionCharacter());
		if (idx<=0) return ""; //$NON-NLS-1$
		return largeBasename.substring(idx);
	}

	/** Reply the extension of the specified file.
	 *
	 * @param filename is the name to parse.
	 * @return the extension of the specified file
	 * @see #shortBasename(File)
	 * @see #largeBasename(File)
	 * @see #basename(File)
	 * @see #dirname(File)
	 * @see #extensions(File)
	 * @since 7.0
	 */
	public static String extension(String filename) {
		if (filename==null) return null;
		String largeBasename = largeBasename(filename);
		int idx = largeBasename.lastIndexOf(getFileExtensionCharacter());
		if (idx<=0) return ""; //$NON-NLS-1$
		return largeBasename.substring(idx);
	}

	/** Reply the extension of the specified file.
	 *
	 * @param filename is the name to parse.
	 * @return the extension of the specified file
	 * @see #shortBasename(URL)
	 * @see #largeBasename(URL)
	 * @see #basename(URL)
	 * @see #dirname(URL)
	 * @see #extensions(URL)
	 */
	public static String extension(URL filename) {
		if (filename==null) return null;
		String largeBasename = largeBasename(filename);
		int idx = largeBasename.lastIndexOf(getFileExtensionCharacter());
		if (idx<=0) return ""; //$NON-NLS-1$
		return decodeHTMLEntities(largeBasename.substring(idx));
	}

	/** Reply all the extensions of the specified file.
	 *
	 * @param filename is the name to parse.
	 * @return the extensions of the specified file
	 */
	public static String[] extensions(File filename) {
		if (filename==null) return new String[0];
		String largeBasename = largeBasename(filename);
		String[] parts = largeBasename.split(Pattern.quote(Character.toString(getFileExtensionCharacter())));
		if (parts.length<=1) return new String[0];
		String[] r = new String[parts.length-1];
		System.arraycopy(parts, 1, r, 0, r.length);
		return r;
	}

	/** Reply all the extensions of the specified file.
	 *
	 * @param filename is the name to parse.
	 * @return the extensions of the specified file
	 * @since 7.0
	 */
	public static String[] extensions(String filename) {
		if (filename==null) return new String[0];
		String largeBasename = largeBasename(filename);
		String[] parts = largeBasename.split(Pattern.quote(Character.toString(getFileExtensionCharacter())));
		if (parts.length<=1) return new String[0];
		String[] r = new String[parts.length-1];
		System.arraycopy(parts, 1, r, 0, r.length);
		return r;
	}

	/** Reply all the extensions of the specified file.
	 *
	 * @param filename is the name to parse.
	 * @return the extensions of the specified file
	 */
	public static String[] extensions(URL filename) {
		if (filename==null) return new String[0];
		String largeBasename = largeBasename(filename);
		String[] parts = largeBasename.split(Pattern.quote(Character.toString(getFileExtensionCharacter())));
		if (parts.length<=1) return new String[0];
		String[] r = new String[parts.length-1];
		for(int i=0; i<r.length; ++i) {
			r[i] = decodeHTMLEntities(parts[i+1]);
		}
		return r;
	}

	/** Replies the parts of a path.
	 *
	 * @param filename is the name to parse.
	 * @return the parts of a path.
	 */
	public static String[] split(File filename) {
		if (filename==null) return new String[0];
		return filename.getPath().split(Pattern.quote(File.separator));
	}

	/** Replies the parts of a path.
	 *
	 * @param filename is the name to parse.
	 * @return the parts of a path.
	 */
	public static String[] split(URL filename) {
		if (filename==null) return new String[0];
		String path;
		if (isJarURL(filename))
			return split(getJarFile(filename));
		path = filename.getPath();
		String[] tab = path.split(Pattern.quote(URL_PATH_SEPARATOR));
		for(int i=0; i<tab.length; ++i) {
			tab[i] = decodeHTMLEntities(tab[i]);
		}
		return tab;
	}

	/** Join the parts of a path and append them to the given File.
	 *
	 * @param fileBase is the file to put as prefix.
	 * @param elements are the path's elements to join.
	 * @return the result of the join of the path's elements.
	 */
	public static File join(File fileBase, String... elements) {
		if (fileBase==null) return null;
		StringBuilder buf = new StringBuilder(fileBase.getPath());
		boolean empty;
		for(String elt : elements) {
			empty = (elt==null || elt.length()==0);
			if (!empty) {
				assert(elt!=null);
				if (!elt.startsWith(File.separator) 
						&& buf.length()>=0
						&& buf.charAt(buf.length()-1)!=File.separatorChar) {
					buf.append(File.separatorChar);
				}
				buf.append(elt);
			}
		}
		return new File(buf.toString());
	}

	/** Join the parts of a path and append them to the given File.
	 *
	 * @param fileBase is the file to put as prefix.
	 * @param elements are the path's elements to join.
	 * @return the result of the join of the path's elements.
	 */
	public static File join(File fileBase, File... elements) {
		if (fileBase==null) return null;
		StringBuilder buf = new StringBuilder(fileBase.getPath());
		for(File elt : elements) {
			if (!elt.isAbsolute()) {
				if (buf.length()>=0 && buf.charAt(buf.length()-1)!=File.separatorChar) {
					buf.append(File.separatorChar);
				}
			}
			buf.append(elt.getPath());
		}
		return new File(buf.toString());
	}

	/** Join the parts of a path and append them to the given URL.
	 *
	 * @param urlBase is the url to put as prefix.
	 * @param elements are the path's elements to join.
	 * @return the result of the join of the path's elements.
	 */
	public static URL join(URL urlBase, String... elements) {
		if (urlBase==null) return null;
		StringBuilder buf = new StringBuilder(urlBase.getPath());
		boolean empty;
		for(String elt : elements) {
			empty = (elt==null || elt.length()==0);
			if (!empty) {
				assert(elt!=null);
				if (!elt.startsWith(File.separator) 
						&& (buf.length()==0
								|| buf.charAt(buf.length()-1)!=URL_PATH_SEPARATOR_CHAR)) {
					buf.append(URL_PATH_SEPARATOR_CHAR);
				}
				buf.append(elt);
			}
		}
		try {
			if (isJarURL(urlBase)) {
				return new URL(
						urlBase.getProtocol(), 
						urlBase.getHost(), 
						urlBase.getPort(),
						buf.toString());
			}
			URI uri = new URI(
					urlBase.getProtocol(), 
					urlBase.getUserInfo(), 
					urlBase.getHost(), 
					urlBase.getPort(), 
					decodeHTMLEntities(buf.toString()),
					decodeHTMLEntities(urlBase.getQuery()),
					urlBase.getRef());
			return uri.toURL();
		}
		catch (Throwable _) {
			//
		}
		try {
			return new URL(
					urlBase.getProtocol(), 
					urlBase.getHost(), 
					buf.toString());
		}
		catch (Throwable _) {
			return null;
		}
	}

	/** Join the parts of a path and append them to the given URL.
	 *
	 * @param urlBase is the url to put as prefix.
	 * @param elements are the path's elements to join.
	 * @return the result of the join of the path's elements.
	 */
	public static URL join(URL urlBase, File... elements) {
		if (urlBase==null) return null;
		StringBuilder buf = new StringBuilder(urlBase.getPath());
		for(File elt : elements) {
			if (!elt.isAbsolute()) {
				if (buf.length()==0 || buf.charAt(buf.length()-1)!=URL_PATH_SEPARATOR_CHAR) {
					buf.append(URL_PATH_SEPARATOR_CHAR);
				}
			}
			buf.append(getFilePath(elt));
		}
		try {
			if (isJarURL(urlBase)) {
				return new URL(
						urlBase.getProtocol(), 
						urlBase.getHost(), 
						urlBase.getPort(),
						buf.toString());
			}
			URI uri = new URI(
					urlBase.getProtocol(), 
					urlBase.getUserInfo(), 
					urlBase.getHost(), 
					urlBase.getPort(), 
					decodeHTMLEntities(buf.toString()),
					decodeHTMLEntities(urlBase.getQuery()),
					urlBase.getRef());
			return uri.toURL();
		}
		catch (Throwable _) {
			//
		}
		try {
			return new URL(
					urlBase.getProtocol(), 
					urlBase.getHost(), 
					buf.toString());
		}
		catch (Throwable _) {
			return null;
		}
	}

	/** Replies if the specified file has the specified extension.
	 * <p>
	 * The test is dependent of the case-sensitive attribute of operating system.
	 * 
	 * @param filename is the filename to parse
	 * @param extension is the extension to test.
	 * @return <code>true</code> if the given filename has the given extension,
	 * otherwise <code>false</code>
	 */
	public static boolean hasExtension(File filename, String extension) {
		if (filename==null) return false;
		assert(extension!=null);
		String extent = extension;
		if (!"".equals(extent) && !extent.startsWith(EXTENSION_SEPARATOR)) //$NON-NLS-1$
			extent = EXTENSION_SEPARATOR+extent;
		String ext = extension(filename);
		if (ext==null) return false;
		if (isCaseSensitiveFilenameSystem())
			return ext.equals(extent);
		return ext.equalsIgnoreCase(extent);
	}

	/** Replies if the specified file has the specified extension.
	 * <p>
	 * The test is dependent of the case-sensitive attribute of operating system.
	 * 
	 * @param filename is the filename to parse
	 * @param extension is the extension to test.
	 * @return <code>true</code> if the given filename has the given extension,
	 * otherwise <code>false</code>
	 * @since 7.0
	 */
	public static boolean hasExtension(String filename, String extension) {
		if (filename==null) return false;
		assert(extension!=null);
		String extent = extension;
		if (!"".equals(extent) && !extent.startsWith(EXTENSION_SEPARATOR)) //$NON-NLS-1$
			extent = EXTENSION_SEPARATOR+extent;
		String ext = extension(filename);
		if (ext==null) return false;
		if (isCaseSensitiveFilenameSystem())
			return ext.equals(extent);
		return ext.equalsIgnoreCase(extent);
	}

	/** Replies if the specified file has the specified extension.
	 * <p>
	 * The test is dependent of the case-sensitive attribute of operating system.
	 * 
	 * @param filename is the filename to parse
	 * @param extension is the extension to test.
	 * @return <code>true</code> if the given filename has the given extension,
	 * otherwise <code>false</code>
	 */
	public static boolean hasExtension(URL filename, String extension) {
		if (filename==null) return false;
		assert(extension!=null);
		String extent = extension;
		if (!"".equals(extent) && !extent.startsWith(EXTENSION_SEPARATOR)) //$NON-NLS-1$
			extent = EXTENSION_SEPARATOR+extent;
		String ext = extension(filename);
		if (ext==null) return false;
		if (isCaseSensitiveFilenameSystem())
			return ext.equals(extent);
		return ext.equalsIgnoreCase(extent);
	}

	/** Remove the extension from the specified filename.
	 * 
	 * @param filename is the filename to parse.
	 * @return the filename without the extension.
	 */
	public static File removeExtension(File filename) {
		if (filename==null) return null;
		File dir = filename.getParentFile();
		String name = filename.getName();
		int idx = name.lastIndexOf(getFileExtensionCharacter());
		if (idx<0) return filename;
		return new File(dir, name.substring(0,idx));
	}

	/** Remove the extension from the specified filename.
	 * 
	 * @param filename is the filename to parse.
	 * @return the filename without the extension.
	 */
	public static URL removeExtension(URL filename) {
		if (filename==null) return null;
		String path = filename.getPath();
		int idx = path.lastIndexOf(URL_PATH_SEPARATOR);
		StringBuilder buf = new StringBuilder((idx<0) ? "" : decodeHTMLEntities(path.substring(0, idx+1))); //$NON-NLS-1$
		String largeBasename = decodeHTMLEntities(path.substring(idx+1));
		idx = largeBasename.lastIndexOf(getFileExtensionCharacter());
		if (idx<0) return filename;
		buf.append(largeBasename.substring(0, idx));
		try {
			if (isJarURL(filename)) {
				return new URL(
						filename.getProtocol(), 
						filename.getHost(), 
						filename.getPort(),
						buf.toString());
			}
			URI uri = new URI(
					filename.getProtocol(), 
					filename.getUserInfo(), 
					filename.getHost(), 
					filename.getPort(), 
					buf.toString(),
					decodeHTMLEntities(filename.getQuery()),
					filename.getRef());
			return uri.toURL();
		}
		catch(AssertionError e) {
			throw e;
		}
		catch(Throwable _) {
			//
		}
		try {
			return new URL(
					filename.getProtocol(), 
					filename.getHost(), 
					buf.toString());
		}
		catch(AssertionError e) {
			throw e;
		}
		catch(Throwable _) {
			return null;
		}
	}

	/** Replace the extension of the specified filename by the given extension.
	 * If the filename has no extension, the specified one will be added.
	 * 
	 * @param filename is the filename to parse.
	 * @param extension is the extension to remove if it is existing.
	 * @return the filename without the extension.
	 */
	public static File replaceExtension(File filename, String extension) {
		if (filename==null) return null;
		if (extension==null) return filename;
		File dir = filename.getParentFile();
		String name = filename.getName();
		int idx = name.lastIndexOf(getFileExtensionCharacter());
		StringBuilder n = new StringBuilder();
		if (idx<0) {
			n.append(name);
		}
		else {
			n.append(name.substring(0,idx));
		}
		if (!name.endsWith(EXTENSION_SEPARATOR) && !extension.startsWith(EXTENSION_SEPARATOR))
			n.append(EXTENSION_SEPARATOR);
		n.append(extension);
		return new File(dir, n.toString());
	}

	/** Replace the extension of the specified filename by the given extension.
	 * If the filename has no extension, the specified one will be added.
	 * 
	 * @param filename is the filename to parse.
	 * @param extension is the extension to remove if it is existing.
	 * @return the filename without the extension.
	 */
	public static URL replaceExtension(URL filename, String extension) {
		if (filename==null) return null;
		if (extension==null) return filename;
		String path = filename.getPath();
		int idx = path.lastIndexOf(URL_PATH_SEPARATOR);
		int end = path.length();
		if (idx==end-1) {
			end --;
			idx = path.lastIndexOf(URL_PATH_SEPARATOR, end-1);
		}
		StringBuilder buf = new StringBuilder((idx<0) ? "" : decodeHTMLEntities(path.substring(0, idx+1))); //$NON-NLS-1$
		String largeBasename = decodeHTMLEntities(path.substring(idx+1, end));
		idx = largeBasename.lastIndexOf(getFileExtensionCharacter());
		if (idx<0) {
			buf.append(largeBasename);
		}
		else {
			buf.append(largeBasename.substring(0, idx));
		}
		String extent = extension;
		if (!"".equals(extent) && !extent.startsWith(EXTENSION_SEPARATOR)) //$NON-NLS-1$
			extent = EXTENSION_SEPARATOR+extent;
		buf.append(extent);
		try {
			if (isJarURL(filename)) {
				return new URL(
						filename.getProtocol(), 
						filename.getHost(), 
						filename.getPort(),
						buf.toString());
			}
			URI uri = new URI(
					filename.getProtocol(), 
					filename.getUserInfo(), 
					filename.getHost(), 
					filename.getPort(), 
					buf.toString(),
					encodeHTMLEntities(filename.getQuery()),
					filename.getRef());
			return uri.toURL();
		}
		catch(AssertionError e) {
			throw e;
		}
		catch(Throwable _) {
			//
		}
		try {
			return new URL(
					filename.getProtocol(), 
					filename.getHost(), 
					buf.toString());
		}
		catch(AssertionError e) {
			throw e;
		}
		catch(Throwable _) {
			return null;
		}
	}

	/** Add the extension of to specified filename.
	 * If the filename has already the given extension, the filename is not changed.
	 * If the filename has no extension or an other extension, the specified one is added.
	 * 
	 * @param filename is the filename to parse.
	 * @param extension is the extension to remove if it is existing.
	 * @return the filename with the extension.
	 * @since 6.0
	 */
	public static File addExtension(File filename, String extension) {
		if (filename!=null && !hasExtension(filename, extension)) {
			String extent = extension;
			if (!"".equals(extent) && !extent.startsWith(EXTENSION_SEPARATOR)) //$NON-NLS-1$
				extent = EXTENSION_SEPARATOR+extent;
			return new File(filename.getParentFile(), filename.getName()+extent);
		}
		return filename;
	}

	/** Add the extension of to specified filename.
	 * If the filename has already the given extension, the filename is not changed.
	 * If the filename has no extension or an other extension, the specified one is added.
	 * 
	 * @param filename is the filename to parse.
	 * @param extension is the extension to remove if it is existing.
	 * @return the filename with the extension.
	 * @since 6.0
	 */
	public static URL addExtension(URL filename, String extension) {
		if (filename!=null && !hasExtension(filename, extension)) {
			String basename = largeBasename(filename);
			URL dirname = dirname(filename);
			String extent = extension;
			if (!"".equals(extent) && !extent.startsWith(EXTENSION_SEPARATOR)) //$NON-NLS-1$
				extent = EXTENSION_SEPARATOR+extent;
			return join(dirname, basename+extent);
		}
		return filename;
	}

	/** Delete the given directory and all its subdirectories.
	 * If the given <var>file</var> is a directory, its
	 * content and the <var>file</var> itself are recursivelly removed. 
	 * 
	 * @param file is the file to delete.
	 * @throws IOException
	 * @see File#delete() for the deletion on a file only.
	 * @see File#mkdir() to create a directory.
	 * @see File#mkdirs() to create a directory and all its parents.
	 * @since 6.0
	 */
	public static void delete(File file) throws IOException {
		if (file!=null) {
			LinkedList<File> candidates = new LinkedList<File>();
			candidates.add(file);
			File f;
			File[] children;
			while (!candidates.isEmpty()) {
				f = candidates.getFirst();
				if (f.isDirectory()) {
					children = f.listFiles();
					if (children!=null && children.length>1) {
						// Non empty directory
						for(File c : children) {
							candidates.push(c);
						}
					}
					else {
						// empty directory
						candidates.removeFirst();
						f.delete();
					}
				}
				else {
					// not a directory
					candidates.removeFirst();
					f.delete();
				}
			}
		}
	}

	/** Delete the given directory and all its subdirectories when the JVM is exiting.
	 * If the given <var>file</var> is a directory, its
	 * content and the <var>file</var> itself are recursivelly removed.
	 * <p>
	 * To cancel this action, see {@link #undeleteOnExit(File)}. 
	 * 
	 * @param file is the file to delete.
	 * @throws IOException
	 * @see File#deleteOnExit() for the deletion on a file only.
	 * @see File#mkdir() to create a directory.
	 * @see File#mkdirs() to create a directory and all its parents.
	 * @since 6.0
	 */
	public static void deleteOnExit(File file) throws IOException {
		if (file!=null) {
			deleteOnExitHook.add(file);
		}
	}

	/** Cancel the deletion of the given directory and all its subdirectories when the JVM is exiting.
	 * 
	 * @param file is the file to undelete.
	 * @throws IOException
	 * @see #deleteOnExit(File)
	 * @see File#deleteOnExit() for the deletion on a file only.
	 * @see File#mkdir() to create a directory.
	 * @see File#mkdirs() to create a directory and all its parents.
	 * @since 6.0
	 */
	public static void undeleteOnExit(File file) throws IOException {
		if (file!=null) {
			deleteOnExitHook.remove(file);
		}
	}

	/** Copy the first file into the second file.
	 * <p>
	 * The content of the second file will be lost.
	 * This copy function allows to do a copy between two different
	 * partitions.
	 * 
	 * @param in is the file to copy.
	 * @param out is the target file
	 * @throws IOException in case of error.
	 * @see #fileCopy(URL, File)
	 * @deprecated {@link #copy(File, File)}
	 */
	@Deprecated
	public static void fileCopy(File in, File out) throws IOException {
		copy(in, out);
	}

	/** Copy the first file into the second file.
	 * <p>
	 * The content of the second file will be lost.
	 * This copy function allows to do a copy between two different
	 * partitions.
	 * <p>
	 * If the <var>out</var> parameter is a directory, the output file
	 * is a file with the same basename as the input and inside
	 * the <var>ou</var> directory.
	 * 
	 * @param in is the file to copy.
	 * @param out is the target file
	 * @throws IOException in case of error.
	 * @see #copy(URL, File)
	 * @since 6.0
	 */
	public static void copy(File in, File out) throws IOException {
		assert(in!=null);
		assert(out!=null);
		
		File outFile = out;
		if (out.isDirectory()) {
			outFile = new File(out, largeBasename(in));
		}
		
		FileInputStream fis = new FileInputStream(in);
		FileOutputStream fos = new FileOutputStream(outFile);
		try {
			copy(fis, (int)in.length(), fos);
		}
		finally {
			fis.close();
			fos.close();
		}
	}

	/** Copy the first file into the second file.
	 * <p>
	 * The content of the second file will be lost.
	 * This copy function allows to do a copy between two different
	 * partitions.
	 * 
	 * @param in is the file to copy.
	 * @param out is the target file
	 * @throws IOException in case of error.
	 * @see #fileCopy(File, File)
	 * @deprecated {@link #copy(URL, File)}
	 */
	@Deprecated
	public static void fileCopy(URL in, File out) throws IOException {
		copy(in, out);
	}

	/** Copy the first file into the second file.
	 * <p>
	 * The content of the second file will be lost.
	 * This copy function allows to do a copy between two different
	 * partitions.
	 * 
	 * @param in is the file to copy.
	 * @param out is the target file
	 * @throws IOException in case of error.
	 * @see #copy(File, File)
	 * @since 6.0
	 */
	public static void copy(URL in, File out) throws IOException {
		assert(in!=null);
		assert(out!=null);

		File outFile = out;
		if (out.isDirectory()) {
			outFile = new File(out, largeBasename(in));
		}
		
		URLConnection connection = in.openConnection();
		FileOutputStream fos = new FileOutputStream(outFile);
		try {
			copy(
				connection.getInputStream(),
				connection.getContentLength(),
				fos);
		}
		finally {
			fos.close();
		}
	}

	/** Copy the first file into the second file.
	 * <p>
	 * The content of the second file will be lost.
	 * This copy function allows to do a copy between two different
	 * partitions.
	 * 
	 * @param in is the input stream to read.
	 * @param inSize is the total size of the input stream.
	 * @param out is the output stream.
	 * @throws IOException
	 * @since 6.2
	 */
	public static void copy(InputStream in, int inSize, FileOutputStream out) throws IOException {
		assert(in!=null);
		assert(out!=null);
		ReadableByteChannel inChannel = Channels.newChannel(in);
		FileChannel outChannel = out.getChannel();
		try {
			int size = inSize;
			// apparently has trouble copying large files on Windows
			if (size<0 || OperatingSystem.WIN.isCurrentOS()) {
				// magic number for Windows, 64Mb - 32Kb
				int maxCount = (64 * 1024 * 1024) - (32 * 1024);
				long position = 0;
				long copied = 1;
				while ( (size>=0 && position<size) || (size<0 && copied>0)) {
					copied = outChannel.transferFrom(inChannel, position, maxCount);
					position += copied;
				}
			}
			else {
				outChannel.transferFrom(inChannel, 0, size);
			}
		}
		finally {
			inChannel.close();
			outChannel.close();
		}
	}
	
	/** Replies the user home directory.
	 *
	 * @return the home directory of the current user.
	 * @throws FileNotFoundException
	 */
	public static File getUserHomeDirectory() throws FileNotFoundException {
		String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		if (userHome!=null && !userHome.isEmpty()) {
			File file = new File(userHome);
			if (file.isDirectory()) return file;
		}
		if (OperatingSystem.ANDROID.isCurrentOS()) {
			return join(File.listRoots()[0], Android.HOME_DIRECTORY);
		}
		throw new FileNotFoundException();
	}

	/** Replies the user home directory.
	 * 
	 * @return the home directory of the current user.
	 */
	public static String getUserHomeDirectoryName() {
		String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		if ((userHome==null || userHome.isEmpty()) && (OperatingSystem.ANDROID.isCurrentOS())) {
			return join(File.listRoots()[0], Android.HOME_DIRECTORY).toString();
		}
		return userHome;
	}

	/** Replies the user configuration directory for the specified software.
	 * <p>
	 * On Unix operating systems, the user directory for a
	 * software is by default {@code $HOME/.software} where {@code software}
	 * is the given parameter (case-sensitive). On Windows&reg; operating systems, the user
	 * directory for a software is by default
	 * {@code C:<span>\</span>Documents and Settings<span>\</span>userName<span>\</span>Local Settings<span>\</span>Application Data<span>\</span>software}
	 * where {@code userName} is the login of the current user and {@code software}
	 * is the given parameter (case-insensitive). 
	 *
	 * @param software is the name of the concerned software.
	 * @return the configuration directory of the software for the current user.
	 */
	public static File getUserConfigurationDirectoryFor(String software) {
		if (software==null || "".equals(software)) //$NON-NLS-1$
			throw new IllegalArgumentException();
		try {
			File userHome = getUserHomeDirectory();
			OperatingSystem os = OperatingSystem.getCurrentOS();
			if (os==OperatingSystem.ANDROID) {
				return join(userHome, "Android", Android.DATA_DIRECTORY,  //$NON-NLS-1$
						Android.makeAndroidApplicationName(software));
			}
			else if (os.isUnixCompliant()) {
				return new File(new File(userHome, ".config"), software); //$NON-NLS-1$
			}
			else if (os==OperatingSystem.WIN) {
				String userName = System.getProperty("user.name"); //$NON-NLS-1$
				if (userName!=null && !"".equals(userName)) { //$NON-NLS-1$
					return join(
							new File("C:"),  //$NON-NLS-1$
							"Documents and Settings", //$NON-NLS-1$
							userName,
							"Local Settings","Application Data",  //$NON-NLS-1$//$NON-NLS-2$
							software);
				}
			}
			return new File(userHome,software);
		}
		catch(FileNotFoundException _) {
			//
		}
		return null;
	}

	/** Replies the user configuration directory for the specified software.
	 * <p>
	 * On Unix operating systems, the user directory for a
	 * software is by default {@code $HOME/.software} where {@code software}
	 * is the given parameter (case-sensitive). On Windows&reg; operating systems, the user
	 * directory for a software is by default
	 * {@code C:<span>\</span>Documents and Settings<span>\</span>userName<span>\</span>Local Settings<span>\</span>Application Data<span>\</span>software}
	 * where {@code userName} is the login of the current user and {@code software}
	 * is the given parameter (case-insensitive). 
	 * 
	 * @param software is the name of the concerned software.
	 * @return the configuration directory of the software for the current user.
	 */
	public static String getUserConfigurationDirectoryNameFor(String software) {
		File directory = getUserConfigurationDirectoryFor(software);
		if (directory!=null) return directory.getAbsolutePath();
		return null;
	}

	/** Replies the system configuration directory for the specified software.
	 * <p>
	 * On Unix operating systems, the system directory for a
	 * software is by default {@code /etc/software} where {@code software}
	 * is the given parameter (case-sensitive). On Windows&reg; operating systems, the user
	 * directory for a software is by default
	 * {@code C:<span>\</span>Program Files<span>\</span>software}
	 * where {@code software} is the given parameter (case-insensitive). 
	 *
	 * @param software is the name of the concerned software.
	 * @return the configuration directory of the software for the current user.
	 */
	public static File getSystemConfigurationDirectoryFor(String software) {
		if (software==null || "".equals(software)) //$NON-NLS-1$
			throw new IllegalArgumentException();
		OperatingSystem os = OperatingSystem.getCurrentOS();
		if (os==OperatingSystem.ANDROID) {
			return join(File.listRoots()[0], Android.CONFIGURATION_DIRECTORY,
					Android.makeAndroidApplicationName(software));
		}
		else if (os.isUnixCompliant()) {
			File[] roots = File.listRoots();
			return join(roots[0],"etc", software); //$NON-NLS-1$
		}
		else if (os==OperatingSystem.WIN) {
			File pfDirectory;
			for(File root : File.listRoots()) {
				pfDirectory = new File(root, "Program Files"); //$NON-NLS-1$
				if (pfDirectory.isDirectory()) {
					return new File(root, software);
				}
			}
		}
		return null;
	}

	/** Replies the user configuration directory for the specified software.
	 * <p>
	 * On Unix operating systems, the system directory for a
	 * software is by default {@code /etc/software} where {@code software}
	 * is the given parameter (case-sensitive). On Windows&reg; operating systems, the user
	 * directory for a software is by default
	 * {@code C:<span>\</span>Program Files<span>\</span>software}
	 * where {@code software} is the given parameter (case-insensitive). 
	 * 
	 * @param software is the name of the concerned software.
	 * @return the configuration directory of the software for the current user.
	 */
	public static String getSystemConfigurationDirectoryNameFor(String software) {
		File directory = getSystemConfigurationDirectoryFor(software);
		if (directory!=null) return directory.getAbsolutePath();
		return null;
	}

	/** Replies the system shared library directory for the specified software.
	 * <p>
	 * On Unix operating systems, the system directory for a
	 * software is by default {@code /usr/lib/software} where {@code software}
	 * is the given parameter (case-sensitive). On Windows&reg; operating systems, the user
	 * directory for a software is by default
	 * {@code C:<span>\</span>Program Files<span>\</span>software}
	 * where {@code software} is the given parameter (case-insensitive). 
	 *
	 * @param software is the name of the concerned software.
	 * @return the configuration directory of the software for the current user.
	 */
	public static File getSystemSharedLibraryDirectoryFor(String software) {
		if (software==null || "".equals(software)) //$NON-NLS-1$
			throw new IllegalArgumentException();
		OperatingSystem os = OperatingSystem.getCurrentOS();
		if (os==OperatingSystem.ANDROID) {
			return join(File.listRoots()[0], Android.DATA_DIRECTORY,
					Android.makeAndroidApplicationName(software));
		}
		else if (os.isUnixCompliant()) {
			File[] roots = File.listRoots();
			return join(roots[0],"usr","lib", software); //$NON-NLS-1$ //$NON-NLS-2$
		}
		else if (os==OperatingSystem.WIN) {
			File pfDirectory;
			for(File root : File.listRoots()) {
				pfDirectory = new File(root, "Program Files"); //$NON-NLS-1$
				if (pfDirectory.isDirectory()) {
					return new File(root, software);
				}
			}
		}
		return null;
	}

	/** Replies the system shared library directory for the specified software.
	 * <p>
	 * On Unix operating systems, the system directory for a
	 * software is by default {@code /usr/lib/software} where {@code software}
	 * is the given parameter (case-sensitive). On Windows&reg; operating systems, the user
	 * directory for a software is by default
	 * {@code C:<span>\</span>Program Files<span>\</span>software}
	 * where {@code software} is the given parameter (case-insensitive). 
	 *
	 * @param software is the name of the concerned software.
	 * @return the configuration directory of the software for the current user.
	 */
	public static String getSystemSharedLibraryDirectoryNameFor(String software) {
		File f = getSystemSharedLibraryDirectoryFor(software);
		if (f==null) return null;
		return f.getAbsolutePath();
	}

	/** Convert an URL which represents a local file into a File.
	 * 
	 * @param url is the URL to convert.
	 * @return the file.
	 * @throws IllegalArgumentException is the URL was malformed.
	 * @deprecated {@link #convertURLToFile(URL)}
	 */
	@Deprecated
	public static File convertUrlToFile(URL url) {
		return convertURLToFile(url);
	}

	/** Convert an URL which represents a local file or a resource into a File.
	 * 
	 * @param url is the URL to convert.
	 * @return the file.
	 * @throws IllegalArgumentException is the URL was malformed.
	 */
	public static File convertURLToFile(URL url) {
		URL theUrl = url;
		if (theUrl==null) return null;
		if (URISchemeType.RESOURCE.isURL(theUrl)) {
			theUrl = Resources.getResource(decodeHTMLEntities(theUrl.getFile()));
			if (theUrl==null) theUrl = url;
		}
		URI uri;
		try {
			// this is the step that can fail, and so
			// it should be this step that should be fixed
			uri = theUrl.toURI();
		}
		catch (URISyntaxException e) {
			// OK if we are here, then obviously the URL did
			// not comply with RFC 2396. This can only
			// happen if we have illegal unescaped characters.
			// If we have one unescaped character, then
			// the only automated fix we can apply, is to assume
			// all characters are unescaped.
			// If we want to construct a URI from unescaped
			// characters, then we have to use the component
			// constructors:
			try {
				uri = new URI(theUrl.getProtocol(), theUrl.getUserInfo(), theUrl
						.getHost(), theUrl.getPort(), 
						decodeHTMLEntities(theUrl.getPath()),
						decodeHTMLEntities(theUrl.getQuery()),
						theUrl.getRef());
			}
			catch (URISyntaxException e1) {
				// The URL is broken beyond automatic repair
				throw new IllegalArgumentException("broken URL: " + theUrl); //$NON-NLS-1$
			}

		}
		if (uri!=null && URISchemeType.FILE.isURI(uri)) {
			String auth = uri.getAuthority();
			String path = uri.getPath();
			if (path==null) path = uri.getRawPath();
			if (path==null) path = uri.getSchemeSpecificPart();
			if (path==null) path = uri.getRawSchemeSpecificPart();
			if (path!=null) {
				if (auth==null || "".equals(auth)) { //$NON-NLS-1$
					// absolute filename in URI
					return new File(decodeHTMLEntities(path));
				}
				// relative filename in URI, extract it directly
				return new File(decodeHTMLEntities(auth+path));
			}
		}
		throw new IllegalArgumentException("not a file URL: "+theUrl); //$NON-NLS-1$
	}

	/** Convert a string to an URL according to several rules.
	 * <p>
	 * The rules are (the first succeeded is replied):
	 * <ul>
	 * <li>if <var>urlDescription</var> is <code>null</code> or empty, return <code>null</code>;</li>
	 * <li>try to build an {@link URL} with <var>urlDescription</var> as parameter;</li>
	 * <li>if <var>allowResourceSearch</var> is <code>true</code> and 
	 * <var>urlDescription</var> starts with {@code "resource:"}, call
	 * {@link Resources#getResource(String)} with the rest of the string as parameter;</li>
	 * <li>if <var>allowResourceSearch</var> is <code>true</code>, call
	 * {@link Resources#getResource(String)} with the <var>urlDescription</var> as
	 * parameter;</li>
	 * <li>assuming that the <var>urlDescription</var> is
	 * a filename, call {@link File#toURI()} to retreive an URI and then
	 * {@link URI#toURL()};</li>
	 * <li>If everything else failed, return <code>null</code>.</li>
	 * </ul>
	 * 
	 * @param urlDescription is a string which is describing an URL.
	 * @param allowResourceSearch indicates if the convertion must take into account the Java resources.
	 * @return the URL.
	 * @throws IllegalArgumentException is the string could not be formatted to URL.
	 * @see Resources#getResource(String)
	 * @deprecated see {@link #convertStringToURL(String, boolean)}
	 */
	@Deprecated
	public static URL convertStringToUrl(String urlDescription, boolean allowResourceSearch) {
		return convertStringToURL(urlDescription, allowResourceSearch, true, true);
	}

	/** Convert a string to an URL according to several rules.
	 * <p>
	 * The rules are (the first succeeded is replied):
	 * <ul>
	 * <li>if <var>urlDescription</var> is <code>null</code> or empty, return <code>null</code>;</li>
	 * <li>try to build an {@link URL} with <var>urlDescription</var> as parameter;</li>
	 * <li>if <var>allowResourceSearch</var> is <code>true</code> and 
	 * <var>urlDescription</var> starts with {@code "resource:"}, call
	 * {@link Resources#getResource(String)} with the rest of the string as parameter;</li>
	 * <li>if <var>allowResourceSearch</var> is <code>true</code>, call
	 * {@link Resources#getResource(String)} with the <var>urlDescription</var> as
	 * parameter;</li>
	 * <li>assuming that the <var>urlDescription</var> is
	 * a filename, call {@link File#toURI()} to retreive an URI and then
	 * {@link URI#toURL()};</li>
	 * <li>If everything else failed, return <code>null</code>.</li>
	 * </ul>
	 * 
	 * @param urlDescription is a string which is describing an URL.
	 * @param allowResourceSearch indicates if the convertion must take into account the Java resources.
	 * @return the URL.
	 * @throws IllegalArgumentException is the string could not be formatted to URL.
	 * @see Resources#getResource(String)
	 */
	public static URL convertStringToURL(String urlDescription, boolean allowResourceSearch) {
		return convertStringToURL(urlDescription, allowResourceSearch, true, true);
	}

	/** Convert a string to an URL according to several rules.
	 * <p>
	 * The rules are (the first succeeded is replied):
	 * <ul>
	 * <li>if <var>urlDescription</var> is <code>null</code> or empty, return <code>null</code>;</li>
	 * <li>try to build an {@link URL} with <var>urlDescription</var> as parameter;</li>
	 * <li>if <var>allowResourceSearch</var> is <code>true</code> and 
	 * <var>urlDescription</var> starts with {@code "resource:"}, call
	 * {@link Resources#getResource(String)} with the rest of the string as parameter;</li>
	 * <li>if <var>allowResourceSearch</var> is <code>true</code>, call
	 * {@link Resources#getResource(String)} with the <var>urlDescription</var> as
	 * parameter;</li>
	 * <li>if <var>repliesFileURL</var> is <code>true</code> and 
	 * assuming that the <var>urlDescription</var> is
	 * a filename, call {@link File#toURI()} to retreive an URI and then
	 * {@link URI#toURL()};</li>
	 * <li>If everything else failed, return <code>null</code>.</li>
	 * </ul>
	 * 
	 * @param urlDescription is a string which is describing an URL.
	 * @param allowResourceSearch indicates if the convertion must take into account the Java resources.
	 * @param repliesFileURL indicates if urlDescription is allowed to be a filename.
	 * @return the URL.
	 * @throws IllegalArgumentException is the string could not be formatted to URL.
	 * @see Resources#getResource(String)
	 * @deprecated {@link #convertStringToURL(String, boolean, boolean)}
	 */
	@Deprecated
	public static URL convertStringToUrl(String urlDescription, boolean allowResourceSearch, boolean repliesFileURL) {
		return convertStringToURL(urlDescription, allowResourceSearch, repliesFileURL, true);
	}

	/** Convert a string to an URL according to several rules.
	 * <p>
	 * The rules are (the first succeeded is replied):
	 * <ul>
	 * <li>if <var>urlDescription</var> is <code>null</code> or empty, return <code>null</code>;</li>
	 * <li>try to build an {@link URL} with <var>urlDescription</var> as parameter;</li>
	 * <li>if <var>allowResourceSearch</var> is <code>true</code> and 
	 * <var>urlDescription</var> starts with {@code "resource:"}, call
	 * {@link Resources#getResource(String)} with the rest of the string as parameter;</li>
	 * <li>if <var>allowResourceSearch</var> is <code>true</code>, call
	 * {@link Resources#getResource(String)} with the <var>urlDescription</var> as
	 * parameter;</li>
	 * <li>if <var>repliesFileURL</var> is <code>true</code> and 
	 * assuming that the <var>urlDescription</var> is
	 * a filename, call {@link File#toURI()} to retreive an URI and then
	 * {@link URI#toURL()};</li>
	 * <li>If everything else failed, return <code>null</code>.</li>
	 * </ul>
	 * 
	 * @param urlDescription is a string which is describing an URL.
	 * @param allowResourceSearch indicates if the convertion must take into account the Java resources.
	 * @param repliesFileURL indicates if urlDescription is allowed to be a filename.
	 * @return the URL.
	 * @throws IllegalArgumentException is the string could not be formatted to URL.
	 * @see Resources#getResource(String)
	 */
	public static URL convertStringToURL(String urlDescription, boolean allowResourceSearch, boolean repliesFileURL) {
		return convertStringToURL(urlDescription, allowResourceSearch, repliesFileURL, true);
	}		

	/** Convert a string to an URL according to several rules.
	 * <p>
	 * The rules are (the first succeeded is replied):
	 * <ul>
	 * <li>if <var>urlDescription</var> is <code>null</code> or empty, return <code>null</code>;</li>
	 * <li>try to build an {@link URL} with <var>urlDescription</var> as parameter;</li>
	 * <li>if <var>allowResourceSearch</var> is <code>true</code> and 
	 * <var>urlDescription</var> starts with {@code "resource:"}, call
	 * {@link Resources#getResource(String)} with the rest of the string as parameter;</li>
	 * <li>if <var>allowResourceSearch</var> is <code>true</code>, call
	 * {@link Resources#getResource(String)} with the <var>urlDescription</var> as
	 * parameter;</li>
	 * <li>if <var>repliesFileURL</var> is <code>true</code> and 
	 * assuming that the <var>urlDescription</var> is
	 * a filename, call {@link File#toURI()} to retreive an URI and then
	 * {@link URI#toURL()};</li>
	 * <li>If everything else failed, return <code>null</code>.</li>
	 * </ul>
	 * 
	 * @param urlDescription is a string which is describing an URL.
	 * @param allowResourceSearch indicates if the convertion must take into account the Java resources.
	 * @param repliesFileURL indicates if urlDescription is allowed to be a filename.
	 * @param supportWindowsPaths indicates if Windows paths should be treated in particular way.
	 * @return the URL.
	 * @throws IllegalArgumentException is the string could not be formatted to URL.
	 * @see Resources#getResource(String)
	 */
	static URL convertStringToURL(String urlDescription, boolean allowResourceSearch, boolean repliesFileURL, boolean supportWindowsPaths) {
		URL url = null;

		if (urlDescription!=null && urlDescription.length()>0)  {

			if (supportWindowsPaths && isWindowsNativeFilename(urlDescription)) {
				File f = normalizeWindowsNativeFilename(urlDescription);
				if (f!=null) return convertFileToURL(f);
			}

			if (URISchemeType.RESOURCE.isScheme(urlDescription)) {
				if (allowResourceSearch) {
					String resourceName = urlDescription.substring(9);
					url = Resources.getResource(resourceName);
				}
			}
			else if (URISchemeType.FILE.isScheme(urlDescription)) {
				File file = new File(URISchemeType.FILE.removeScheme(urlDescription));
				try {
					url = new URL(URISchemeType.FILE.name(), "", //$NON-NLS-1$
							getFilePath(file));
				}
				catch (MalformedURLException e) {
					//
				}
			}
			else {
				try {
					url = new URL(urlDescription);
				}
				catch (MalformedURLException _) {
					// ignore error
				}
			}

			if (url==null) {
				if (allowResourceSearch) {
					url = Resources.getResource(urlDescription);
				}

				if (url==null && URISchemeType.RESOURCE.isScheme(urlDescription)) {
					return null;
				}

				if (url==null && repliesFileURL) {
					String urlPart = URISchemeType.removeAnyScheme(urlDescription);
					// Try to parse a malformed JAR url:
					// jar:{malformed-url}!/{entry}
					if (URISchemeType.JAR.isScheme(urlDescription)) {
						int idx = urlPart.indexOf(JAR_URL_FILE_ROOT);
						if (idx>0) {
							URL jarURL = convertStringToURL(urlPart.substring(0, idx), allowResourceSearch);
							if (jarURL!=null) {
								try {
									url = toJarURL(jarURL, urlPart.substring(idx+2));
								}
								catch (MalformedURLException _) {
									//
								}
							}
						}
					}

					// Standard local file
					if (url==null) {
						try {
							File file = new File(urlPart);
							url = new URL(URISchemeType.FILE.name(), "", //$NON-NLS-1$ 
									getFilePath(file));
						}
						catch (MalformedURLException e) {
							// ignore error
						}
					}
				}
			}
		}

		return url;
	}

	/**
	 * Make the given filename absolute from the given root if it is not already absolute.
	 * <p>
	 * <table border="1" width="100%">
	 * <thead>
	 * <tr>
	 * <th><var>filename</var></th><th><var>current</var></th><th>Result</th>
	 * </tr>
	 * </thead>
	 * <tr>
	 * <td><code>null</code></td>
	 * <td><code>null</code></td>
	 * <td><code>null</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>null</code></td>
	 * <td><code>/myroot</code></td>
	 * <td><code>null</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>/path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>/path/to/file</code></td>
	 * <td><code>/myroot</code></td>
	 * <td><code>/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>path/to/file</code></td>
	 * <td><code>/myroot</code></td>
	 * <td><code>/myroot/path/to/file</code></td>
	 * </tr>
	 * </table>
	 *
	 * @param filename is the name to make absolute.
	 * @param current is the current directory which permits to make absolute.
	 * @return an absolute filename.
	 */
	public static File makeAbsolute(File filename, File current) {
		if (filename==null) return null;
		if (current!=null && !filename.isAbsolute()) {
			try {
				return new File(current.getCanonicalFile(), filename.getPath());
			}
			catch(IOException _) {
				return new File(current.getAbsoluteFile(), filename.getPath());
			}
		}
		return filename;
	}

	/** Replies if the given URL is using a protocol which could be map to files.
	 * 
	 * @param url
	 * @return <code>true</code> if the given url is a "file", "http", 
	 * "https", "ftp", "ssh", "jar" or "resource", otherwise <code>false</code>.
	 * @deprecated see {@link URISchemeType#isFileBasedScheme()} 
	 */
	@Deprecated
	public static boolean isFileBasedURL(URL url) {
		if (url!=null) {
			return isFileBasedScheme(URISchemeType.getSchemeType(url));
		}
		return false;
	}

	/** Replies if the given URL scheme is using a protocol which could be map to files.
	 * 
	 * @param scheme
	 * @return <code>true</code> if the given scheme is a "file", "http", 
	 * "https", "ftp", "ssh", "jar" or "resource", otherwise <code>false</code>.
	 * @since 5.0
	 * @deprecated see {@link URISchemeType#isFileBasedScheme()}
	 */
	@Deprecated
	public static boolean isFileBasedScheme(URISchemeType scheme) {
		if (scheme!=null) {
			return scheme.isFileBasedScheme();
		}
		return false;
	}

	/**
	 * Make the given filename absolute from the given root if it is not already absolute. 
	 * <p>
	 * <table border="1" width="100%">
	 * <thead>
	 * <tr>
	 * <th><var>filename</var></th><th><var>current</var></th><th>Result</th>
	 * </tr>
	 * </thead>
	 * <tr>
	 * <td><code>null</code></td>
	 * <td><code>null</code></td>
	 * <td><code>null</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>null</code></td>
	 * <td><code>/myroot</code></td>
	 * <td><code>null</code></td>
	 * </tr>
	 * 
	 * <tr>
	 * <td><code>file:/path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>file:/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>file:path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>file:path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>file:/path/to/file</code></td>
	 * <td><code>/myroot</code></td>
	 * <td><code>file:/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>file:path/to/file</code></td>
	 * <td><code>/myroot</code></td>
	 * <td><code>file:/myroot/path/to/file</code></td>
	 * </tr>
	 * 
	 * <tr>
	 * <td><code>http://host.com/path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>http://host.com/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>http://host.com/path/to/file</code></td>
	 * <td><code>/myroot</code></td>
	 * <td><code>http://host.com/path/to/file</code></td>
	 * </tr>
	 * 
	 * <tr>
	 * <td><code>ftp://host.com/path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>ftp://host.com/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>ftp://host.com/path/to/file</code></td>
	 * <td><code>/myroot</code></td>
	 * <td><code>ftp://host.com/path/to/file</code></td>
	 * </tr>
	 * 
	 * <tr>
	 * <td><code>ssh://host.com/path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>ssh://host.com/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>ssh://host.com/path/to/file</code></td>
	 * <td><code>/myroot</code></td>
	 * <td><code>ssh://host.com/path/to/file</code></td>
	 * </tr>
	 * </table>
	 *
	 * @param filename is the name to make absolute.
	 * @param current is the current directory which permits to make absolute.
	 * @return an absolute filename.
	 */
	public static URL makeAbsolute(URL filename, File current) {
		try {
			return makeAbsolute(filename, current==null ? null : current.toURI().toURL());
		}
		catch(MalformedURLException _) {
			//
		}
		return filename;
	}

	/**
	 * Make the given filename absolute from the given root if it is not already absolute. 
	 * <p>
	 * <table border="1" width="100%">
	 * <thead>
	 * <tr>
	 * <th><var>filename</var></th><th><var>current</var></th><th>Result</th>
	 * </tr>
	 * </thead>
	 * <tr>
	 * <td><code>null</code></td>
	 * <td><code>null</code></td>
	 * <td><code>null</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>null</code></td>
	 * <td><code>file:/myroot</code></td>
	 * <td><code>null</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>null</code></td>
	 * <td><code>http://host.com/myroot</code></td>
	 * <td><code>null</code></td>
	 * </tr>
	 * 
	 * <tr>
	 * <td><code>file:path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>file:path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>file:path/to/file</code></td>
	 * <td><code>file:/myroot</code></td>
	 * <td><code>file:/myroot/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>file:path/to/file</code></td>
	 * <td><code>http://host.com/myroot</code></td>
	 * <td><code>http://host.com/myroot/path/to/file</code></td>
	 * </tr>
	 * 
	 * <tr>
	 * <td><code>file:/path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>file:/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>file:/path/to/file</code></td>
	 * <td><code>file:/myroot</code></td>
	 * <td><code>file:/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>file:/path/to/file</code></td>
	 * <td><code>http://host.com/myroot</code></td>
	 * <td><code>file:/path/to/file</code></td>
	 * </tr>
	 * 
	 * <tr>
	 * <td><code>http://host2.com/path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>http://host2.com/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>http://host2.com/path/to/file</code></td>
	 * <td><code>file:/myroot</code></td>
	 * <td><code>http://host2.com/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>http://host2.com/path/to/file</code></td>
	 * <td><code>http://host.com/myroot</code></td>
	 * <td><code>http://host2.com/path/to/file</code></td>
	 * </tr>
	 * 
	 * <tr>
	 * <td><code>ftp://host2.com/path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>ftp://host2.com/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>ftp://host2.com/path/to/file</code></td>
	 * <td><code>file:/myroot</code></td>
	 * <td><code>ftp://host2.com/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>ftp://host2.com/path/to/file</code></td>
	 * <td><code>http://host.com/myroot</code></td>
	 * <td><code>ftp://host2.com/path/to/file</code></td>
	 * </tr>
	 * </table>
	 *
	 * @param filename is the name to make absolute.
	 * @param current is the current directory which permits to make absolute.
	 * @return an absolute filename.
	 */
	public static URL makeAbsolute(URL filename, URL current) {
		if (filename==null) return null;
		URISchemeType scheme = URISchemeType.getSchemeType(filename);
		switch(scheme) {
		case JAR:
			try {
				URL jarUrl = getJarURL(filename);
				jarUrl = makeAbsolute(jarUrl, current);
				File jarFile = getJarFile(filename);
				return toJarURL(jarUrl, jarFile);
			}
			catch(MalformedURLException _) {
				// Ignore error
			}
			break;
		case FILE:
		{
			File f = new File(filename.getFile());
			if (!f.isAbsolute()) {
				if (current!=null) {
					return join(current, f);
				}
			}
		}
		break;
		default:
			// do not change the URL
		}
		return filename;
	}

	/**
	 * Make the given filename absolute from the given root if it is not already absolute. 
	 * <p>
	 * <table border="1" width="100%">
	 * <thead>
	 * <tr>
	 * <th><var>filename</var></th><th><var>current</var></th><th>Result</th>
	 * </tr>
	 * </thead>
	 * <tr>
	 * <td><code>null</code></td>
	 * <td><code>null</code></td>
	 * <td><code>null</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>null</code></td>
	 * <td><code>file:/myroot</code></td>
	 * <td><code>null</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>null</code></td>
	 * <td><code>http://host.com/myroot</code></td>
	 * <td><code>null</code></td>
	 * </tr>
	 * 
	 * <tr>
	 * <td><code>path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>file:path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>path/to/file</code></td>
	 * <td><code>file:/myroot</code></td>
	 * <td><code>file:/myroot/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>path/to/file</code></td>
	 * <td><code>http://host.com/myroot</code></td>
	 * <td><code>http://host.com/myroot/path/to/file</code></td>
	 * </tr>
	 * 
	 * 
	 * <tr>
	 * <td><code>/path/to/file</code></td>
	 * <td><code>null</code></td>
	 * <td><code>file:/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>/path/to/file</code></td>
	 * <td><code>file:/myroot</code></td>
	 * <td><code>file:/path/to/file</code></td>
	 * </tr>
	 * <tr>
	 * <td><code>/path/to/file</code></td>
	 * <td><code>http://host.com/myroot</code></td>
	 * <td><code>file:/path/to/file</code></td>
	 * </tr>
	 * </table>
	 *
	 * @param filename is the name to make absolute.
	 * @param current is the current directory which permits to make absolute.
	 * @return an absolute filename.
	 * @since 5.0
	 */
	public static URL makeAbsolute(File filename, URL current) {
		if (filename!=null) {
			if (!filename.isAbsolute() && current!=null) {
				return join(current, filename);
			}
			try {
				return new URL(URISchemeType.FILE.toString()+
						getFilePath(filename));
			}
			catch (MalformedURLException _) {
				// ignore error
			}
		}
		return null;
	}

	/** Replies the parent URL for the given URL.
	 * 
	 * @param url
	 * @return the parent URL
	 * @throws MalformedURLException 
	 */
	public static URL getParentURL(URL url) throws MalformedURLException {
		if (url==null) return url;
		String path = url.getPath();
		String prefix, parentStr;

		switch(URISchemeType.getSchemeType(url)) {
		case JAR:
		{
			int index = path.indexOf(JAR_URL_FILE_ROOT);
			assert(index>0);
			prefix = path.substring(0,index+1);
			path = path.substring(index+1);
			parentStr = URL_PATH_SEPARATOR;
		}
		break;
		case FILE:
		{
			prefix = null;
			parentStr = ".."+URL_PATH_SEPARATOR; //$NON-NLS-1$
		}
		break;
		default:
		{
			prefix = null;
			parentStr = URL_PATH_SEPARATOR;
		}
		}

		if (path==null || "".equals(path)) path = parentStr; //$NON-NLS-1$
		int index = path.lastIndexOf(URL_PATH_SEPARATOR_CHAR);
		if (index==-1) path = parentStr;
		else if (index==path.length()-1) {
			index = path.lastIndexOf(URL_PATH_SEPARATOR_CHAR, index-1);
			if (index==-1) path = parentStr;
			else path = path.substring(0, index+1);
		}
		else path = path.substring(0, index+1);

		if (prefix!=null)  path = prefix + path;

		return new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
	}

	/** Test if the given filename is a local filename and extract
	 * the path component.
	 * 
	 * @param filename
	 * @return the path
	 */
	private static String extractLocalPath(String filename) {
		if (filename==null ) return null;
		String fn = filename.toUpperCase();
		if (fn.startsWith("FILE://")) //$NON-NLS-1$
			fn = filename.substring(7);
		else if (fn.startsWith("FILE:")) //$NON-NLS-1$
			fn = filename.substring(5);
		else
			fn = filename;
		return fn;
	}

	/** Replies if the given string contains a Windows&reg; native long filename.
	 * <p>
	 * Long filenames (LFN), spelled "long file names" by Microsoft Corporation, 
	 * are Microsoft's way of implementing filenames longer than the 8.3, 
	 * or short-filename, naming scheme used in Microsoft DOS in their modern 
	 * FAT and NTFS filesystems. Because these filenames can be longer than the 
	 * 8.3 filename, they can be more descriptive. Another advantage of this 
	 * scheme is that it allows for use of *nix files ending in (e.g. .jpeg, 
	 * .tiff, .html, and .xhtml) rather than specialized shortened names 
	 * (e.g. .jpg, .tif, .htm, .xht).
	 * <p>
	 * The long filename system allows a maximum length of 255 UTF-16 characters,
	 * including spaces and non-alphanumeric characters; excluding the following 
	 * characters, which have special meaning within the command interpreter or 
	 * the operating system kernel: <code>\</code> <code>/</code> <code>:</code>
	 * <code>*</code> <code>?</code> <code>"</code> <code>&lt;</code>
	 * <code>&gt;</code> <code>|</code>
	 * 
	 * @param filename
	 * @return <code>true</code> if the given filename is a long filename,
	 * otherwise <code>false</code>
	 * @see #normalizeWindowsNativeFilename(String)
	 */
	public static boolean isWindowsNativeFilename(String filename) {
		String fn = extractLocalPath(filename);
		if (fn==null || fn.length()==0) return false;
		Pattern pattern = Pattern.compile(WINDOW_NATIVE_FILENAME_PATTERN);
		Matcher matcher = pattern.matcher(fn);
		return matcher.matches();
	}

	/** Normalize the given string contains a Windows&reg; native long filename
	 * and replies a Java-standard version.
	 * <p>
	 * Long filenames (LFN), spelled "long file names" by Microsoft Corporation, 
	 * are Microsoft's way of implementing filenames longer than the 8.3, 
	 * or short-filename, naming scheme used in Microsoft DOS in their modern 
	 * FAT and NTFS filesystems. Because these filenames can be longer than the 
	 * 8.3 filename, they can be more descriptive. Another advantage of this 
	 * scheme is that it allows for use of *nix files ending in (e.g. .jpeg, 
	 * .tiff, .html, and .xhtml) rather than specialized shortened names 
	 * (e.g. .jpg, .tif, .htm, .xht).
	 * <p>
	 * The long filename system allows a maximum length of 255 UTF-16 characters,
	 * including spaces and non-alphanumeric characters; excluding the following 
	 * characters, which have special meaning within the command interpreter or 
	 * the operating system kernel: <code>\</code> <code>/</code> <code>:</code>
	 * <code>*</code> <code>?</code> <code>"</code> <code>&lt;</code>
	 * <code>&gt;</code> <code>|</code>
	 * 
	 * @param filename
	 * @return the normalized path or <code>null</code> if not a windows native path.
	 * @see #isWindowsNativeFilename(String)
	 */
	public static File normalizeWindowsNativeFilename(String filename) {
		String fn = extractLocalPath(filename);
		if (fn!=null && fn.length()>0) {
			Pattern pattern = Pattern.compile(WINDOW_NATIVE_FILENAME_PATTERN);
			Matcher matcher = pattern.matcher(fn);
			if (matcher.find()) {
				return new File(fn.replace('\\', File.separatorChar));
			}
		}
		return null;
	}

	/** Replies an URL for the given file and translate it into a
	 * resource URL if the given file is inside the classpath.
	 * 
	 * @param file is the filename to translate.
	 * @return the URL which is corresponding to file, or <code>null</code> if 
	 * the url cannot be computed.
	 */
	public static URL convertFileToURL(File file) {
		if (file==null) return null;
		try {
			File f = file;
			if (isWindowsNativeFilename(file.toString())) {
				f = normalizeWindowsNativeFilename(file.toString());
				if (f==null) f = file;
			}
			URL url = f.toURI().toURL();
			return toShortestURL(url);
		}
		catch (MalformedURLException e) {
			return null;
		}
	}

	/** Replies an URL for the given url and translate it into a
	 * resource URL if the given file is inside the classpath.
	 * 
	 * @param url is the URL to make shortest.
	 * @return the URL which is corresponding to file, or <code>null</code> if 
	 * the url cannot be computed.
	 * @since 4.0
	 */
	public static URL toShortestURL(URL url) {
		if (url==null) return null;
		String s = url.toExternalForm().replaceAll("/$", "");  //$NON-NLS-1$//$NON-NLS-2$
		String sp;
		Iterator<URL> classpath = ClasspathUtil.getClasspath();
		URL path;

		while (classpath.hasNext()) {
			path = classpath.next();
			sp = path.toExternalForm().replaceAll("/$", "");  //$NON-NLS-1$//$NON-NLS-2$
			if (s.startsWith(sp)) {
				StringBuilder buffer = new StringBuilder("resource:"); //$NON-NLS-1$
				buffer.append(s.substring(sp.length()).replaceAll("^/", ""));  //$NON-NLS-1$//$NON-NLS-2$
				try {
					return new URL(buffer.toString());
				}
				catch (MalformedURLException e) {
					//
				}
			}
		}

		return url;
	}

	/**
	 * Make the given filename relative to the given root path.
	 *
	 * @param filenameToMakeRelative is the name to make relative.
	 * @param rootPath is the root path from which the relative path will be set.
	 * @return a relative filename.
	 * @throws IOException when is is impossible to retreive canonical paths.
	 */
	public static File makeRelative(File filenameToMakeRelative, File rootPath) throws IOException {
		return makeRelative(filenameToMakeRelative, rootPath, true);
	}
	
	/**
	 * Make the given filename relative to the given root path.
	 *
	 * @param filenameToMakeRelative is the name to make relative.
	 * @param rootPath is the root path from which the relative path will be set.
	 * @param appendCurrentDirectorySymbol indicates if "./" should be append at the
	 * begining of the relative filename.
	 * @return a relative filename.
	 * @throws IOException when is is impossible to retreive canonical paths.
	 */
	private static File makeRelative(File filenameToMakeRelative, File rootPath, boolean appendCurrentDirectorySymbol) throws IOException {

		if (filenameToMakeRelative==null || rootPath==null)
			throw new IllegalArgumentException();

		if (!filenameToMakeRelative.isAbsolute()) return filenameToMakeRelative;
		if (!rootPath.isAbsolute()) return filenameToMakeRelative;

		File root = rootPath.getCanonicalFile();
		File dir = filenameToMakeRelative.getParentFile().getCanonicalFile();

		String[] parts1 = split(dir);
		String[] parts2 = split(root);

		String relPath = makeRelative(parts1, parts2, filenameToMakeRelative.getName());

		if (appendCurrentDirectorySymbol)
			return new File(CURRENT_DIRECTORY, relPath);
		return new File(relPath);
	}

	/**
	 * Make the given filename relative to the given root path.
	 *
	 * @param filenameToMakeRelative is the name to make relative.
	 * @param rootPath is the root path from which the relative path will be set.
	 * @return a relative filename.
	 * @throws IOException when is is impossible to retreive canonical paths.
	 * @since 6.0
	 */
	public static File makeRelative(File filenameToMakeRelative, URL rootPath) throws IOException {
		if (filenameToMakeRelative==null || rootPath==null)
			throw new IllegalArgumentException();

		if (!filenameToMakeRelative.isAbsolute()) return filenameToMakeRelative;

		File dir = filenameToMakeRelative.getParentFile().getCanonicalFile();

		String[] parts1 = split(dir);
		String[] parts2 = split(rootPath);

		String relPath = makeRelative(parts1, parts2, filenameToMakeRelative.getName());

		return new File(CURRENT_DIRECTORY, relPath);
	}

	/**
	 * Make the given filename relative to the given root path.
	 *
	 * @param filenameToMakeRelative is the name to make relative.
	 * @param rootPath is the root path from which the relative path will be set.
	 * @return a relative filename.
	 * @throws IOException when is is impossible to retreive canonical paths.
	 * @since 6.0
	 */
	public static File makeRelative(URL filenameToMakeRelative, URL rootPath) throws IOException {
		if (filenameToMakeRelative==null || rootPath==null)
			throw new IllegalArgumentException();

		String basename = largeBasename(filenameToMakeRelative);
		URL dir = dirname(filenameToMakeRelative);

		String[] parts1 = split(dir);
		String[] parts2 = split(rootPath);

		String relPath = makeRelative(parts1, parts2, basename);

		return new File(CURRENT_DIRECTORY, relPath);
	}

	private static String makeRelative(String[] parts1, String[] parts2, String basename) {
		int firstDiff = -1;

		for(int i=0; firstDiff<0 && i<parts1.length && i<parts2.length; i++) {
			if (!parts1[i].equals(parts2[i])) {
				firstDiff = i;
			}
		}

		StringBuilder result = new StringBuilder();
		if (firstDiff<0) {
			firstDiff = Math.min(parts1.length, parts2.length);
		}

		for(int i=firstDiff; i<parts2.length; i++) {
			if (result.length()>0) result.append(File.separator);
			result.append(PARENT_DIRECTORY);
		}

		for(int i=firstDiff; i<parts1.length; i++) {
			if (result.length()>0) result.append(File.separator);
			result.append(parts1[i]);
		}

		if (result.length()>0) result.append(File.separator);
		result.append(basename);

		return result.toString();
	}

	/**
	 * <p>
	 * A canonical pathname is both absolute and unique.  This method maps 
	 * the pathname to its unique form.  This typically involves removing redundant names
	 * such as <tt>"."</tt> and <tt>".."</tt> from the pathname.
	 * 
	 * @param url is the URL to make canonical
	 * @return the canonical form of the given URL.
	 * @since 6.0
	 */
	public static URL makeCanonicalURL(URL url) {
		if (url!=null) {
			String[] pathComponents = url.getPath().split(Pattern.quote(URL_PATH_SEPARATOR));

			List<String> canonicalPath = new LinkedList<String>();
			for(String component : pathComponents) {
				if (!CURRENT_DIRECTORY.equals(component)) {
					if (PARENT_DIRECTORY.equals(component)) {
						if (!canonicalPath.isEmpty()) {
							canonicalPath.remove(canonicalPath.size()-1);
						}
						else {
							canonicalPath.add(component);
						}
					}
					else {
						canonicalPath.add(component);
					}
				}
			}

			StringBuilder newPathBuffer = new StringBuilder();
			boolean isFirst = true;
			for(String component : canonicalPath) {
				if (!isFirst) {
					newPathBuffer.append(URL_PATH_SEPARATOR_CHAR);
				}
				else {
					isFirst = false;
				}
				newPathBuffer.append(component);
			}

			try {
				URI uri = new URI(
						url.getProtocol(), 
						url.getUserInfo(),
						url.getHost(),
						url.getPort(), 
						newPathBuffer.toString(),
						url.getQuery(),
						url.getRef());
				return uri.toURL();
			}
			catch (MalformedURLException _) {
				//
			}
			catch (URISyntaxException _) {
				//
			}

			try {
				return new URL(
						url.getProtocol(), 
						url.getHost(), 
						newPathBuffer.toString());
			}
			catch (Throwable _) {
				//
			}
		}
		return url;
	}
	
	private static String fileToURL(File file) {
		return file.getPath().replace(File.separatorChar, '/');
	}

	/**
	 * Create a zip file from the given input file.
	 * If the input file is a directory, the content of the directory is zipped.
	 * If the input file is a standard file, it is zipped.
	 * 
	 * @param input
	 * @param output
	 * @throws IOException
	 * @since 6.2
	 */
	public static void zipFile(File input, OutputStream output) throws IOException {
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(output);

			if (input==null) return;

			LinkedList<File> candidates = new LinkedList<File>();
			candidates.add(input);

			byte[] buffer = new byte[2048];
			int len;
			File file, relativeFile;
			String zipFilename;

			File rootDirectory = (input.isDirectory()) ? input : input.getParentFile();

			while (!candidates.isEmpty()) {
				file = candidates.removeFirst();
				assert(file!=null);
				
				if (file.getAbsoluteFile().equals(rootDirectory.getAbsoluteFile()))
					relativeFile = null;
				else
					relativeFile = makeRelative(file, rootDirectory, false);
				
				if (file.isDirectory()) {
					if (relativeFile!=null) {
						zipFilename = fileToURL(relativeFile)+"/"; //$NON-NLS-1$
						ZipEntry zipEntry = new ZipEntry(zipFilename);
						zos.putNextEntry(zipEntry);
						zos.closeEntry();
					}
					candidates.addAll(Arrays.asList(file.listFiles()));
				}
				else if (relativeFile!=null) {
					FileInputStream fis = new FileInputStream(file);
					try {
						zipFilename = fileToURL(relativeFile);
						ZipEntry zipEntry = new ZipEntry(zipFilename);
						zos.putNextEntry(zipEntry);
						while ((len=fis.read(buffer))>0) {
							zos.write(buffer, 0, len);
						}
						zos.closeEntry();
					}
					finally {
						fis.close();
					}
				}
			}
		}
		finally {
			if (zos!=null) zos.close();
		}
	}

	/**
	 * Unzip the given stream and write out the file in the output.
	 * If the input file is a directory, the content of the directory is zipped.
	 * If the input file is a standard file, it is zipped.
	 * 
	 * @param input
	 * @param output
	 * @throws IOException
	 * @since 6.2
	 */
	public static void unzipFile(InputStream input, File output) throws IOException {
		if (output==null) return;
		output.mkdirs();
		if (!output.isDirectory()) throw new IOException("not a directory: "+output); //$NON-NLS-1$
		ZipInputStream zis = null;
		try {
			byte[] buffer = new byte[2048];
			int len;
			
			zis = new ZipInputStream(input);
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry!=null) {
				String name = zipEntry.getName();
				File outFile = new File(output,name).getCanonicalFile();
				if (zipEntry.isDirectory()) {
					outFile.mkdirs();
				}
				else {
					outFile.getParentFile().mkdirs();
					FileOutputStream fos = new FileOutputStream(outFile);
					try {
						while ((len=zis.read(buffer))>0) {
							fos.write(buffer, 0, len);
						}
					}
					finally {
						fos.close();
					}
				}
				zipEntry = zis.getNextEntry();
			}
		}
		finally {
			if (zis!=null) zis.close();
		}
	}

	/**
	 * Create a zip file from the given input file.
	 * 
	 * @param input
	 * @param output
	 * @throws IOException
	 * @since 6.2
	 */
	public static void zipFile(File input, File output) throws IOException {
		FileOutputStream fos = new FileOutputStream(output);
		try {
			zipFile(input, fos);
		}
		finally {
			fos.close();
		}
	}

	/**
	 * Unzip a file into the output directory.
	 * 
	 * @param input
	 * @param output
	 * @throws IOException
	 * @since 6.2
	 */
	public static void unzipFile(File input, File output) throws IOException {
		FileInputStream fis = new FileInputStream(input);
		try {
			unzipFile(fis, output);
		}
		finally {
			fis.close();
		}
	}

	/** Create an empty directory in the default temporary-file directory, using
	 * the given prefix and suffix to generate its name.  Invoking this method
	 * is equivalent to invoking <code>{@link #createTempDirectory(java.lang.String,
	 * java.lang.String, java.io.File)
	 * createTempDirectory(prefix,&nbsp;suffix,&nbsp;null)}</code>.
	 *
	 * @param  prefix is the prefix string to be used in generating the file's
	 *                    name; must be at least three characters long
	 *
	 * @param  suffix is the suffix string to be used in generating the file's
	 *                    name; may be <code>null</code>, in which case the
	 *                    suffix <code>".tmp"</code> will be used
	 * @return  An abstract pathname denoting a newly-created empty file
	 * @throws  IllegalArgumentException
	 *          If the <code>prefix</code> argument contains fewer than three
	 *          characters
	 * @throws  IOException  If a file could not be created
	 * @throws  SecurityException
	 *          If a security manager exists and its <code>{@link
	 *          java.lang.SecurityManager#checkWrite(java.lang.String)}</code>
	 *          method does not allow a file to be created
	 * @since 6.2
	 */
	public static File createTempDirectory(String prefix, String suffix) throws IOException {
		return createTempDirectory(prefix, suffix, null);
	}

	/** Creates a new empty directory in the specified directory, using the
     * given prefix and suffix strings to generate its name.  If this method
     * returns successfully then it is guaranteed that:
     * <ol>
     * <li> The directory denoted by the returned abstract pathname did not exist
     *      before this method was invoked, and
     * <li> Neither this method nor any of its variants will return the same
     *      abstract pathname again in the current invocation of the virtual
     *      machine.
     * </ol>
     * <p>
     * This method provides only part of a temporary-file facility.  To arrange
     * for a file created by this method to be deleted automatically, use the
     * <code>{@link #deleteOnExit}</code> method.
     *
     * <p> The <code>prefix</code> argument must be at least three characters
     * long.  It is recommended that the prefix be a short, meaningful string
     * such as <code>"hjb"</code> or <code>"mail"</code>.  The
     * <code>suffix</code> argument may be <code>null</code>, in which case the
     * suffix <code>".tmp"</code> will be used.
     *
     * <p> To create the new directory, the prefix and the suffix may first be
     * adjusted to fit the limitations of the underlying platform.  If the
     * prefix is too long then it will be truncated, but its first three
     * characters will always be preserved.  If the suffix is too long then it
     * too will be truncated, but if it begins with a period character
     * (<code>'.'</code>) then the period and the first three characters
     * following it will always be preserved.  Once these adjustments have been
     * made the name of the new file will be generated by concatenating the
     * prefix, five or more internally-generated characters, and the suffix.
     *
     * <p> If the <code>directory</code> argument is <code>null</code> then the
     * system-dependent default temporary-file directory will be used.  The
     * default temporary-file directory is specified by the system property
     * <code>java.io.tmpdir</code>.  On UNIX systems the default value of this
     * property is typically <code>"/tmp"</code> or <code>"/var/tmp"</code>; on
     * Microsoft Windows systems it is typically <code>"C:\\WINNT\\TEMP"</code>.  A different
     * value may be given to this system property when the Java virtual machine
     * is invoked, but programmatic changes to this property are not guaranteed
     * to have any effect upon the temporary directory used by this method.
	 *
	 * @param  prefix is the prefix string to be used in generating the file's
	 *                    name; must be at least three characters long
	 *
	 * @param  suffix is the suffix string to be used in generating the file's
	 *                    name; may be <code>null</code>, in which case the
	 *                    suffix <code>".tmp"</code> will be used
	 * @param  directory is the directory in which the file is to be created, or
	 *                    <code>null</code> if the default temporary-file
	 *                    directory is to be used
	 * @return  An abstract pathname denoting a newly-created empty file
	 * @throws  IllegalArgumentException
	 *          If the <code>prefix</code> argument contains fewer than three
	 *          characters
	 * @throws  IOException  If a file could not be created
	 * @throws  SecurityException
	 *          If a security manager exists and its <code>{@link
	 *          java.lang.SecurityManager#checkWrite(java.lang.String)}</code>
	 *          method does not allow a file to be created
	 * @since 6.2
	 */
	public static File createTempDirectory(String prefix, String suffix, File directory) throws IOException {
		if (prefix == null) throw new NullPointerException();
		if (prefix.length() < 3)
			throw new IllegalArgumentException("Prefix string too short"); //$NON-NLS-1$
		String s = (suffix == null) ? ".tmp" : suffix; //$NON-NLS-1$
		File targetDirectory;
		if (directory == null) {
			targetDirectory = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
		}
		else {
			targetDirectory = directory;
		}
		File f;
		do {
			long n = RANDOM.nextLong();
			if (n == Long.MIN_VALUE) {
				n = 0;      // corner case
			}
			else {
				n = Math.abs(n);
			}
			StringBuilder buffer = new StringBuilder();
			buffer.append(prefix);
			buffer.append(Long.toString(n));
			buffer.append(s);
			f = new File(targetDirectory, buffer.toString());
		} 
		while (!f.mkdirs());
		return f;
	}

    /** Hook to recursively delete files on JVM exit. 
	 * 
	 * @author $Author: galland$
	 * @version $FullVersion$
	 * @mavengroupid $GroupId$
	 * @mavenartifactid $ArtifactId$
	 * @since 6.0
	 */
	private static class DeleteOnExitHook extends Thread {

		private List<File> filesToDelete = null;

		public DeleteOnExitHook() {
			setName("DeleteOnExitHook"); //$NON-NLS-1$
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			synchronized(this) {
				if (this.filesToDelete!=null) {
					for(File f : this.filesToDelete) {
						try {
							delete(f);
						}
						catch(IOException e) {
							// Ignore error
						}
					}
					this.filesToDelete.clear();
					this.filesToDelete = null;
				}
			}
		}

		/** Add a file to delete.
		 * 
		 * @param file
		 */
		public void add(File file) {
			assert(file!=null);
			synchronized(this) {
				if (this.filesToDelete==null) {
					this.filesToDelete = new LinkedList<File>();
					Runtime.getRuntime().addShutdownHook(this);
				}
				this.filesToDelete.add(file);
			}
		}

		/** Remove a file to delete.
		 * 
		 * @param file
		 */
		public void remove(File file) {
			synchronized(this) {
				if (this.filesToDelete!=null) {
					this.filesToDelete.remove(file);
					if (this.filesToDelete.isEmpty()) {
						this.filesToDelete = null;
						Runtime.getRuntime().removeShutdownHook(this);
					}
				}
			}
		}

	}

}

