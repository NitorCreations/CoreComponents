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
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

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

	private void extractTar(InputStream in, File destFolder) throws Exception {
		// Create a TarInputStream
		TarArchiveInputStream tis = new TarArchiveInputStream(new BufferedInputStream(in));
		TarArchiveEntry entry;

		while((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
			int count;
			byte data[] = new byte[2048];
			FileOutputStream fos = new FileOutputStream(new File(destFolder, entry.getName()));
			BufferedOutputStream dest = new BufferedOutputStream(fos);
			Set<PosixFilePermission> perms = getPermissions(entry.getMode());

			while((count = tis.read(data)) != -1) {
				dest.write(data, 0, count);
			}

			dest.flush();
			dest.close();
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
				extractTar(new FileInputStream(target), new File(root));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private boolean downloadArtifact(String propertySuffix, Properties properties, Map<String, String> replaceTokens) {
		String artifact = properties.getProperty(PROPERTY_KEY_PREFIX_DOWNLOAD_ARTIFACT);
		if (artifact == null) return false;
		return true;
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
