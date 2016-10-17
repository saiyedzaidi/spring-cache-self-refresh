package io.github.yantrashala.springcache.tools;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.MethodInvoker;

/**
 * Registers invocations of methods with @Cacheable annotations.
 * 
 * @author Saiyed Zaidi
 * @copyright @2016 http://yantrashala.github.io
 * @version 1.0
 */
@Component("cacheSupport")
public class CacheSupportImpl implements CacheOperations, InvocationRegistry {

	/**
	 * Maintains Sets of CachedInvocation objects corresponding to each cache
	 * configured in the application. At initialization, this map gets populated
	 * with the cache name as the key and a hashSet as the value, for every
	 * configured cache.
	 */
	private Map<String, Set<CachedInvocation>> cacheToInvocationsMap;

	/**
	 * Avoid concurrent modification issues by using CopyOnWriteArraySet that
	 * copies the internal array on every modification
	 */
	private final Set<CachedInvocation> allInvocations = new CopyOnWriteArraySet<CacheSupportImpl.CachedInvocation>();

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private KeyGenerator keyGenerator;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerInvocation(Object targetBean, Method targetMethod, Object[] arguments,
			Set<String> annotatedCacheNames) {
		Object key = keyGenerator.generate(targetBean, targetMethod, arguments);
		final CachedInvocation invocation = new CachedInvocation(key, targetBean, targetMethod, arguments);
		allInvocations.add(invocation);
		for (final String cacheName : annotatedCacheNames) {
			cacheToInvocationsMap.get(cacheName).add(invocation);
		}
	}

	/**
	 * Creates a MethodInvoker instance from the cached invocation object and
	 * invokes it to get the return value
	 * 
	 * @param invocation
	 * @return Return value resulted from the method invocation
	 * @throws NoSuchMethodException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	private Object execute(CachedInvocation invocation)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		final MethodInvoker invoker = new MethodInvoker();
		invoker.setTargetObject(invocation.getTargetBean());
		invoker.setArguments(invocation.getArguments());
		invoker.setTargetMethod(invocation.getTargetMethod().getName());
		invoker.prepare();
		return invoker.invoke();
	}

	/**
	 * Initializes the storage objects in a optimum way based upon the number of
	 * configured caches. Helps avoid creating Set objects on the fly and
	 * related concurrency issues. Populates the cacheToInvocationsMap with the
	 * cache name as the key and a hashSet as the value, for every configured
	 * cache. Depends on CacheManager to get the configured cache names.
	 */
	@PostConstruct
	public void initialize() {
		cacheToInvocationsMap = new ConcurrentHashMap<String, Set<CachedInvocation>>(
				cacheManager.getCacheNames().size());
		for (final String cacheName : cacheManager.getCacheNames()) {
			cacheToInvocationsMap.put(cacheName, new CopyOnWriteArraySet<CachedInvocation>());
		}
	}

	/**
	 * Uses the supplied cached invocation details to invoke the target method
	 * with appropriate arguments and update the relevant caches. Updates all
	 * caches if the cacheName argument is null.
	 * 
	 * @param invocation
	 * @param cacheNames
	 */
	private void updateCache(CachedInvocation invocation, String... cacheNames) {
		String[] cacheNamesArray = cacheNames;
		boolean invocationSuccess;
		Object computed = null;
		try {
			computed = execute(invocation);
			invocationSuccess = true;
		} catch (final IllegalAccessException | ClassNotFoundException | NoSuchMethodException
				| InvocationTargetException e) {
			invocationSuccess = false;
			//TODO Invocation failed, log the issue, cache can not be updated
		}
		if (invocationSuccess) {
			if (cacheNamesArray == null) {
				cacheNamesArray = cacheToInvocationsMap.keySet().toArray(new String[cacheToInvocationsMap.size()]);
			}
			for (final String cacheName : cacheNamesArray) {
				if (cacheToInvocationsMap.get(cacheName) != null) {
					cacheManager.getCache(cacheName).put(invocation.getKey(), computed);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void refreshAllCaches() {
		for (final CachedInvocation invocation : allInvocations) {
			updateCache(invocation, (String) null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void refreshCache(String cacheName) {
		if (cacheToInvocationsMap.get(cacheName) != null) {
			for (final CachedInvocation invocation : cacheToInvocationsMap.get(cacheName)) {
				updateCache(invocation, cacheName);
			}
		}
		// Otherwise Wrong cache name, missing spring configuration for the
		// cache name used in annotations
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void refreshCaches(String... cacheNames) {
		for (final String cacheName : cacheNames) {
			refreshCache(cacheName);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void evictCache(String... cacheNames) {
		if (cacheNames != null) {
			for (final String cacheName : cacheNames) {
				evictCache(cacheName);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void evictCache(String cacheName) {
		if (cacheName != null) {
			Cache cache = cacheManager.getCache(cacheName);
			if (cache != null) {
				cache.clear();
			}
		}
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	protected Map<String, Set<CachedInvocation>> getCacheGrid() {
		return cacheToInvocationsMap;
	}

	public void setCacheGrid(Map<String, Set<CachedInvocation>> cacheGrid) {
		this.cacheToInvocationsMap = cacheGrid;
	}

	protected Set<CachedInvocation> getInvocations() {
		return allInvocations;
	}

	/**
	 * Holds the method invocation information to use while refreshing the
	 * cache.
	 * 
	 * @author szaidi
	 * @copyright @2016 Sapient Consulting
	 * @see CacheSupportImpl.java
	 * @version 1.0
	 */
	protected static final class CachedInvocation {
		private Object key;
		private final Object targetBean;
		private final Method targetMethod;
		private Object[] arguments;

		protected CachedInvocation(Object key, Object targetBean, Method targetMethod, Object[] arguments) {
			this.key = key;
			this.targetBean = targetBean;
			this.targetMethod = targetMethod;
			if (arguments != null && arguments.length != 0) {
				this.arguments = Arrays.copyOf(arguments, arguments.length);
				// TODO check if deep cloning is needed and implement
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof CachedInvocation)) {
				return false;
			}
			final CachedInvocation other = (CachedInvocation) obj;
			return key.equals(other.getKey());
		}

		/**
		 * @return the arguments
		 */
		private Object[] getArguments() {
			return arguments;
		}

		/**
		 * @return the targetBean
		 */
		private Object getTargetBean() {
			return targetBean;
		}

		/**
		 * @return the targetMethod
		 */
		private Method getTargetMethod() {
			return targetMethod;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return key.hashCode();
		}

		public Object getKey() {
			return key;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "CachedInvocation [Key=" + key + ", targetBean=" + targetBean + ", targetMethod=" + targetMethod
					+ ", arguments=" + (arguments != null ? arguments.length : "none") + " ]";
		}

	}

	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<String> getCacheNames() {
		return cacheManager.getCacheNames();
	}
}
