package analysis;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.util.HashMap;
import java.util.Map;

public class MemoryURLClassLoader extends URLClassLoader {

	private final Map<String, byte[]> definitions = new HashMap<String, byte[]>();

	@SuppressWarnings("unused")
	private AccessControlContext acc;

	public MemoryURLClassLoader(URL[] urls) {
		super(urls);
	}

	public MemoryURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public void addDefinition(final String name, final byte[] bytes) {
		definitions.put(name, bytes);
	}

	@Override
	public Class<?> loadClass(final String name, final boolean resolve)
			throws ClassNotFoundException {
		final byte[] bytes = definitions.get(name);
		if (bytes != null) {
			Class<?> c = findLoadedClass(name);
			return (c != null) ? c : defineClass(name, bytes, 0, bytes.length);
		}
		return super.loadClass(name, resolve);
	}

}
