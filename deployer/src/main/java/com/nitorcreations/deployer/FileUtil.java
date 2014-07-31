package com.nitorcreations.deployer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.codehaus.swizzle.stream.ReplaceStringsInputStream;

public class FileUtil {

	public static String getFileName(String name) {
		int lastSeparator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
		return name.substring(lastSeparator + 1);
	}
	public static long copy(InputStream in, File target) throws IOException {
		OutputStream out = null;
		try {
			out = new FileOutputStream(target);
			byte[] buffer = new byte[1025 * 4];
			long count = 0;
			int n = 0;
			while (-1 != (n = in.read(buffer))) {
				out.write(buffer, 0, n);
				count += n;
			}
			return count;
		} finally {
			try {
				out.flush();
			} catch (IOException e0) {
				throw e0;
			} finally {
				try {
					if (in != null) in.close();
				} catch (IOException e1) {
					throw e1;
				} finally {
					if (out != null) out.close();
				}
			}
		}
	}

	public static long filterStream(InputStream original, File target, Map<String, String> replaceTokens) throws IOException {
		InputStream in = new ReplaceStringsInputStream(original, replaceTokens);
		OutputStream out = new FileOutputStream(target);
		return copyByteByByte(in, out);
	}

	public static long filterStream(InputStream original, OutputStream out, Map<String, String> replaceTokens) throws IOException {
		InputStream in = new ReplaceStringsInputStream(original, replaceTokens);
		return copyByteByByte(in, out);
	}

	public static long copyByteByByte(InputStream in, OutputStream out) throws IOException {
		try {
			long i = 0;
			int b;
			while ((b = in.read()) != -1) {
				out.write(b);
				i++;
			}
			return i;
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				out.flush();
			} catch (IOException e0) {
				throw e0;
			} finally {
				try {
					if (in != null) in.close();
				} catch (IOException e1) {
					throw e1;
				} finally {
					if (out != null) out.close();
				}
			}
		}
	}


}
