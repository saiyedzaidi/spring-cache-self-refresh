# spring-cache-self-refresh
Given one or more methods annotated with the Spring's @Cacheable annotation. Allow the cached data to be refreshed behind the scene, while maintaining the already cached data. 

Requirements:
Already configured Spring cache abstraction, with ot without any actual provider.

To use:
Create a pointcut to intercept the cacheable packages/classes/methods with com.sapient.engineering.tools.cache.CachingAnnotationsAspect.interceptCacheables(ProceedingJoinPoint)
This class register the invocations, keeping a copy of all arguments used for the invocation.
CacheOperations class provides the refresh cache method that causes all cached invocations to be re-triggered, resulting in update of the cached values.

Extra files:
General purpose LoggingAspect and ProfilingAspect

