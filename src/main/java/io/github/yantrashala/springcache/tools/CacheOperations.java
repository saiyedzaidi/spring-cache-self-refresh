package io.github.yantrashala.springcache.tools;

import java.util.Collection;

/**
 * Provides methods to refresh cached objects.
 * 
 * @author szaidi
 * @copyright @2016 http://yantrashala.github.io
 * @version 1.0
 */
public interface CacheOperations {

	/**
	 * Returns all the configured cache names.
	 * @return
	 */
	Collection<String> getCacheNames();

	/**
	 * Refreshes caches corresponding to the supplied cache names array.
	 * 
	 * @param cacheNames
	 */
	void refreshCaches(String... cacheNames);

	/**
	 * Refreshes caches corresponding to the supplied cache name.
	 * 
	 * @param cacheName
	 */
	void refreshCache(String cacheName);

	/**
	 * Refreshes all caches configured in the application
	 * 
	 * @param cacheName
	 */
	void refreshAllCaches();

	/**
	 * Clears all values from the named caches
	 * 
	 * @param cacheNames
	 */
	void evictCache(String... cacheNames);

	/**
	 * Clears all values from the named cache
	 * 
	 * @param cacheName
	 */
	void evictCache(String cacheName);
}