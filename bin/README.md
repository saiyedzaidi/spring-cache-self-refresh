# spring-cache-self-refresh
Given one or more methods annotated with the Spring's @Cacheable annotation. Allow the cached data to be refreshed behind the scene, while maintaining the already cached data. 
A use could be to shield your system against an unstable remote, using a scheduler to get any updates if the remote is available any time.

Requirements:
Already configured Spring cache abstraction, with or without any actual provider.

To use:
Create a pointcut to intercept the cacheable packages/classes/methods with io.github.yantrashala.springcache.tools.CachingAnnotationsAspect.interceptCacheables(ProceedingJoinPoint)
This class register the invocations, keeping a copy of all arguments used for the invocation.
CacheOperations class provides the refresh cache method that causes all cached invocations to be re-triggered, resulting in update of the cached values.

Since the utility uses AOP, to run the Test case, please add a javaagent entry to your command line like -javaagent:${user.home}/.m2/repository/org/springframework/spring-agent/2.5.6/spring-agent-2.5.6.jar
or simply
-javaagent:spring-agent-2.5.6.jar   if you have the jar in the same directory.


Extra files:
General purpose LoggingAspect and ProfilingAspect

