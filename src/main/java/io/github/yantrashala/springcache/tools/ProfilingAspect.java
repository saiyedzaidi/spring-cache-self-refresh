package io.github.yantrashala.springcache.tools;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * Aspect to profile all methods and log execution times
 * 
 * @author Saiyed Zaidi
 * @copyright @2016 http://yantrashala.github.io
 * @version 1.0
 */
@Component
public class ProfilingAspect {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProfilingAspect.class);

	/**
	 * Applies the advice to a specific package as per the pointcut
	 * 
	 * @param joinPoint
	 * @return
	 * @throws Throwable
	 */
	public Object profileIntegrations(final ProceedingJoinPoint joinPoint) throws Throwable { // NOSONAR
		// No sonar comment is to skip sonar violation at this line
		return logExecutionTime(joinPoint);
	}

	/**
	 * Logs execution time of the joinpoint
	 * 
	 * @param joinPoint
	 * @return Result of joinpoint execution
	 * @throws Throwable
	 *             Can not avoid the mandatory throwable thrown by
	 *             joinpoint.proceed()
	 */
	public Object logExecutionTime(final ProceedingJoinPoint joinPoint) throws Throwable { // NOSONAR
		// No sonar comment is to skip sonar violation at this line
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		Object ret = joinPoint.proceed();
		stopWatch.stop();
		LOGGER.info("class={} method={} args={} execTime={}",
				new String[] { joinPoint.getTarget().getClass().getName(), joinPoint.getSignature().getName(),
						(joinPoint.getArgs() == null || joinPoint.getArgs().length == 0) ? "None"
								: Arrays.toString(joinPoint.getArgs()),
						stopWatch.getTotalTimeMillis() + " millis" });
		return ret;
	}
}
