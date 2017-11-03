package io.github.yantrashala.springcache.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheFactoryBean;
import org.springframework.cache.interceptor.DefaultKeyGenerator;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.EnableLoadTimeWeaving;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestCacheOperations.TestConfiguration.class })
@EnableLoadTimeWeaving
@EnableAspectJAutoProxy
public class TestCacheOperations {

	private static final String CACHE_NAME = "default";
	@Autowired
	BusinessService businessService;

	@Autowired
	UnstableBusinessService unstableService;

	@Autowired
	CacheOperations cacheOperations;

	@Autowired
	CacheManager cacheManager;

	/**
	 * Tests standard Spring cache to validate the setup
	 */
	@Test
	public void testStandardCacheSimple() {
		String response1 = businessService.business("param1", "param2");
		String response2 = businessService.business("param1", "param2");
		assertEquals(response2, response1);
	}

	/**
	 * Tests standard Spring cache negative case to validate the setup is good
	 * for testing reloads
	 */
	@Test
	public void testStandardCacheNegative() {
		String response1 = businessService.business("param1", "param2");
		cacheManager.getCache(CACHE_NAME).clear();
		String response2 = businessService.business("param1", "param2");
		assertNotEquals(response2, response1);
	}

	/**
	 * Tests cached value refresh across cache invocations.
	 */
	@Test
	public void testCacheReloadPositive() {
		String response1 = businessService.business("param1", "param2");
		cacheOperations.refreshCache(CACHE_NAME);
		String response2 = businessService.business("param1", "param2");
		assertNotEquals(response2, response1);
	}

	/**
	 * Tests cached value retention when business service fails during refresh.
	 */
	@Test
	public void testCacheReloadWithError() {
		String response1 = unstableService.business("paramx", "paramy");
		unstableService.setDown(true);
		cacheOperations.refreshCache(CACHE_NAME);
		String response2 = unstableService.business("paramx", "paramy");
		assertEquals(response2, response1);
	}

	/**
	 * Uses simple cache setup and default keygenerator to setup Spring cache
	 * abstraction.
	 * 
	 * @author Saiyed Zaidi
	 *
	 */
	@Configuration
	@EnableCaching
	@EnableAspectJAutoProxy
	@ComponentScan(basePackages = { "io.github.yantrashala.springcache.tools" })
	public static class TestConfiguration {

		@Bean
		public SimpleCacheManager cacheManager() {
			SimpleCacheManager cacheManager = new SimpleCacheManager();
			List<Cache> caches = new ArrayList<Cache>();
			caches.add(cacheBean().getObject());
			cacheManager.setCaches(caches);
			return cacheManager;
		}

		@Bean
		public ConcurrentMapCacheFactoryBean cacheBean() {
			ConcurrentMapCacheFactoryBean cacheFactoryBean = new ConcurrentMapCacheFactoryBean();
			cacheFactoryBean.setName(CACHE_NAME);
			return cacheFactoryBean;
		}

		/**
		 * Better to use own Keygenerator instead of default as the default
		 * ignores the method and class name in the key generation logic. Using
		 * Default for simplicity.
		 * 
		 * @return
		 */
		@Bean
		public KeyGenerator getKeyGenerator() {
			return new DefaultKeyGenerator();
		}
	}
}

/**
 * Any Business service that uses @Cacheable
 * 
 * @author Saiyed Zaidi
 *
 */
@Component("businessService")
class BusinessServiceImpl implements BusinessService {

	@Cacheable(value = "default")
	public String business(String param1, String param2) {
		return "output " + new Random().nextInt();
	}
}

interface BusinessService {
	String business(String param1, String param2);
}

@Component("unstableService")
class UnstableBusinessServiceImpl implements UnstableBusinessService {

	boolean down = false;

	public void setDown(boolean down) {
		this.down = down;
	}

	@Cacheable(value = "default")
	public String business(String param1, String param2) {
		if (down) {
			throw new RuntimeException("Service down");
		}
		return "output " + new Random().nextInt();
	}
}

/**
 * An Unstable service that may go down and start throwing exceptions.
 * 
 * @author szaidi
 *
 */
interface UnstableBusinessService {
	/**
	 * Business as usual
	 * 
	 * @param param1
	 * @param param2
	 * @return
	 */
	String business(String param1, String param2);

	/**
	 * Set service down status
	 * 
	 * @param down
	 */
	void setDown(boolean down);
}

/**
 * Advice to allow watching the Business service invocations. Passes the
 * invocations to CachingAnnotationsAspect
 * 
 * @author Saiyed Zaidi
 *
 */
@Aspect
@Component
class TestAdvice {

	@Pointcut("execution(public * io.github.yantrashala.springcache.tools.BusinessService.*(..))")
	public void methodsToBeInspected() {
	}

	@Autowired
	CachingAnnotationsAspect cachingAnnotationsAspect;

	@Around("methodsToBeInspected()")
	public Object interceptCaches(ProceedingJoinPoint joinPoint) throws Throwable {
		return cachingAnnotationsAspect.interceptCacheables(joinPoint);
	}
}