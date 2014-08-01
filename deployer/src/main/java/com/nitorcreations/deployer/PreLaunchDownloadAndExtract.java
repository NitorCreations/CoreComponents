package com.nitorcreations.deployer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.velocity.runtime.parser.node.GetExecutor;

import static com.nitorcreations.deployer.PropertyKeys.*;

public class PreLaunchDownloadAndExtract {
	private static Map<Integer, PosixFilePermission> perms = new HashMap<Integer, PosixFilePermission>();

	static {
		perms.put(0001, PosixFilePermission.OTHERS_EXECUTE);
		perms.put(0002, PosixFilePermission.OTHERS_WRITE);
		perms.put(0004, PosixFilePermission.OTHERS_READ);
		perms.put(0010, PosixFilePermission.GROUP_EXECUTE);
		perms.put(0020, PosixFilePermission.GROUP_WRITE);
		perms.put(0040, PosixFilePermission.GROUP_READ);
		perms.put(0100, PosixFilePermission.OWNER_EXECUTE);
		perms.put(0200, PosixFilePermission.OWNER_WRITE);
		perms.put(0400, PosixFilePermission.OWNER_READ);
	}

	private void extractTar(InputStream in, File destFolder, Set<PathMatcher> skipMatchers, Set<PathMatcher> filterMatchers) throws Exception {
		// Create a TarInputStream
		TarArchiveInputStream tis = new TarArchiveInputStream(new BufferedInputStream(in));
		TarArchiveEntry entry;

		while((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
			FileOutputStream fos = new FileOutputStream(new File(destFolder, entry.getName()));
			if (!globMatches(entry.getName(), skipMatchers)) {
				BufferedOutputStream dest = new BufferedOutputStream(fos);
				Set<PosixFilePermission> perms = getPermissions(entry.getMode());
			}
		}
		tis.close();
	}

	public void execute(Properties properties) {
		Map<String, String> replaceTokens = new HashMap<>();
		for (Entry<Object,Object> nextEntry : properties.entrySet()) {
			replaceTokens.put("${" + nextEntry.getKey() + "}", (String)nextEntry.getValue());
			replaceTokens.put("@" + nextEntry.getKey() + "@", (String)nextEntry.getValue());
		}
		if (downloadUrl("", properties, replaceTokens)) {
			int i = 1;
			while (downloadUrl("" + i, properties, replaceTokens)) {}
		}
		if (downloadArtifact("", properties, replaceTokens)) {
			int i = 1;
			while (downloadArtifact("" + i, properties, replaceTokens)) {}
		}
	}
	
	private boolean downloadUrl(String propertySuffix, Properties properties, Map<String, String> replaceTokens) {
		String url = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_URL + propertySuffix);
		if (url == null) return false;
		String root = properties.getProperty(PROPERTY_KEY_PREFIX_EXTRACT_ROOT + propertySuffix, ".");
		try {
			URL source = new URL(url);
			String fileName = FileUtil.getFileName(source.getPath());
			String lcFileName = fileName.toLowerCase();
			File target = File.createTempFile(fileName, "download");
			URLConnection conn = new URL(url).openConnection();
			FileUtil.copy(conn.getInputStream(), target);
			if (lcFileName.endsWith("tar.gz") || lcFileName.endsWith(".tgz")) {
				extractTar(new FileInputStream(target), new File(root),
						getGlobMatchers(properties.getProperty(PROPERTY_KEY_PREFIX_EXTRACT_SKIP_GLOB +propertySuffix)),
						getGlobMatchers(properties.getProperty(PROPERTY_KEY_PREFIX_EXTRACT_INTERPOLATE_GLOB +propertySuffix)));
				target.delete();
			} else if (lcFileName.endsWith(".zip") || lcFileName.endsWith(".jar")) {
				extractZip(new FileInputStream(target), new File(root),
						getGlobMatchers(properties.getProperty(PROPERTY_KEY_PREFIX_EXTRACT_SKIP_GLOB +propertySuffix)),
						getGlobMatchers(properties.getProperty(PROPERTY_KEY_PREFIX_EXTRACT_INTERPOLATE_GLOB +propertySuffix)));
				target.delete();
			} else if (lcFileName.endsWith(".cpio")) {
				extractCpio(new FileInputStream(target), new File(root),
						getGlobMatchers(properties.getProperty(PROPERTY_KEY_PREFIX_EXTRACT_SKIP_GLOB +propertySuffix)),
						getGlobMatchers(properties.getProperty(PROPERTY_KEY_PREFIX_EXTRACT_INTERPOLATE_GLOB +propertySuffix)));
				target.delete();
			} else {
				target.renameTo(new File(new File(root), fileName));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private void extractCpio(FileInputStream fileInputStream, File file,
			Set<PathMatcher> globMatchers, Set<PathMatcher> globMatchers2) {
		// TODO Auto-generated method stub
		
	}
	private void extractZip(FileInputStream fileInputStream, File file,
			Set<PathMatcher> globMatchers, Set<PathMatcher> globMatchers2) {
		// TODO Auto-generated method stub
		
	}
	private boolean downloadArtifact(String propertySuffix, Properties properties, Map<String, String> replaceTokens) {
		String artifact = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT);
		if (artifact == null) return false;
		return true;
	}
	
	private boolean globMatches(String path, Set<PathMatcher> matchers) {
		for (PathMatcher next : matchers) {
			if (next.matches(Paths.get(path))) return true;
		}
		return false;
	}
	private Set<PathMatcher> getGlobMatchers(String expressions) {
		Set<PathMatcher> matchers = new LinkedHashSet<>();
		if (expressions == null || expressions.isEmpty()) return matchers;
		FileSystem def = FileSystems.getDefault();
		for (String next : expressions.split("|")) {
			matchers.add(def.getPathMatcher("glob:"  + expressions));
		}
		return matchers;
	}
	
	public Set<PosixFilePermission> getPermissions(int mode) {
		Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>();
		for (int mask : perms.keySet()) {
			if (mask == (mode & mask)) {
				permissions.add(perms.get(mask));
			}
		}
		return permissions;
	}
}
