package com.nitorcreations.deployer;

import static com.nitorcreations.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT;
import static com.nitorcreations.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_DOWNLOAD_URL;
import static com.nitorcreations.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_EXTRACT_INTERPOLATE_GLOB;
import static com.nitorcreations.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_EXTRACT_ROOT;
import static com.nitorcreations.deployer.PropertyKeys.PROPERTY_KEY_PREFIX_EXTRACT_SKIP_GLOB;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

public class PreLaunchDownloadAndExtract {
	private final ArchiveStreamFactory factory = new ArchiveStreamFactory();
	private final CompressorStreamFactory cfactory = new CompressorStreamFactory();
	private static Map<Integer, PosixFilePermission> perms = new HashMap<Integer, PosixFilePermission>();
	private Logger logger = Logger.getLogger(this.getClass().getName());
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
			String fileName = FileUtil.getFileName(url);
			String lcFileName = fileName.toLowerCase();
			File target = File.createTempFile(fileName, "download");
			URLConnection conn = new URL(url).openConnection();
			FileUtil.copy(conn.getInputStream(), target);
			Set<PathMatcher> skipMatchers = getGlobMatchers(properties.getProperty(PROPERTY_KEY_PREFIX_EXTRACT_SKIP_GLOB +propertySuffix));
			Set<PathMatcher> filterMatchers = getGlobMatchers(properties.getProperty(PROPERTY_KEY_PREFIX_EXTRACT_INTERPOLATE_GLOB +propertySuffix));
			InputStream in = new BufferedInputStream(new FileInputStream(target), 8 * 1024);
			if (lcFileName.endsWith("z") ||	lcFileName.endsWith("bz2") || lcFileName.endsWith("lzma") ||
					lcFileName.endsWith("arj") || lcFileName.endsWith("deflate")) {
				in = cfactory.createCompressorInputStream(in);
			}
			extractArchive(factory.createArchiveInputStream(in), new File(root), replaceTokens, skipMatchers, filterMatchers);
		} catch (Exception e) {
			LogRecord rec = new LogRecord(Level.WARNING, "Failed to download and extract " + url);
			rec.setThrown(e);
			logger.log(rec);
		}
		return true;
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
			String trimmed = next.trim();
			if (!trimmed.isEmpty()) {
				matchers.add(def.getPathMatcher("glob:"  + trimmed));
			}
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
	
	private void extractArchive(ArchiveInputStream is, File destFolder, Map<String, String> replaceTokens, Set<PathMatcher> skipMatchers, Set<PathMatcher> filterMatchers) throws IOException {
		try {
			ArchiveEntry entry;
			while((entry =  is.getNextEntry()) != null) {
				File dest = new File(destFolder, entry.getName());
				if (!globMatches(entry.getName(), skipMatchers)) {
					if (entry.isDirectory()) {
						dest.mkdirs();
					} else {
						if (globMatches(entry.getName(), filterMatchers)) {
							FileUtil.filterStream(is, dest, replaceTokens);
						} else {
							FileUtil.copy(is, dest);
						}
					}
					Set<PosixFilePermission> permissions = getPermissions(getMode(entry));
					Path destPath = Paths.get(dest.getAbsolutePath());
					Files.setPosixFilePermissions(destPath, permissions);
				}
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				throw e;
			}
		}
	}

	private int getMode(ArchiveEntry entry) {
		Method m = null;
		try {
			m = entry.getClass().getMethod("getMode"); 
		} catch (NoSuchMethodException | SecurityException e) {
		}
		if (m == null) {
			if (entry instanceof ZipArchiveEntry) {
				return (int) ((((ZipArchiveEntry)entry).getExternalAttributes() >> 16) & 0xFFF);
			} else return 0664;
		} else {
			try {
				return (int)((Number)m.invoke(entry)).longValue() & 0xFFF;
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				return 0664;
			}
		}
	}
}
