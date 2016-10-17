package io.github.yantrashala.springcache.tools;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A simple logging aspect to log the arguments and return values from the configured methods.
 * 
 * @author Saiyed Zaidi
 * @copyright @2016 http://yantrashala.github.io
 * @version 1.0
 */
@Component
public class LoggingAspect {

	private static final String METHOD_ARGS_RETURN = "method={} args={} return={}";
	private static final String METHOD_ARGS = "method={} args={}";
	private static final String EXCEPTION_CAUGHT_SOURCE_METHOD_ARGS_EXCEPTION_TRACE = "exception.logged source=[{}] \nmethod=[{}] \nargs=[{}] \nexception=[{}] \ntrace={}";
	private static final int CHAR_LENGTH_120 = 120;
	private static final Map<Class<?>, Logger> CLASS_LOGGERS = new ConcurrentHashMap<Class<?>, Logger>();
	private static final Logger ASPECTLOGGER = LoggingAspect.getLogger(LoggingAspect.class);

	/**
	 * Gets the logger for the targeted class name.
	 * 
	 * @param className
	 *            Class for which logger is to be created or fetched
	 * @return
	 */
	protected static Logger getLogger(final Class<?> className) {
		Logger logger = LoggingAspect.CLASS_LOGGERS.get(className);
		if (logger == null) {
			logger = LoggerFactory.getLogger(className);
			LoggingAspect.CLASS_LOGGERS.put(className, logger);
		}
		return logger;
	}

	/**
	 * Logs the entry to and exit from any method under the configured package.
	 * 
	 * @param joinPoint
	 * @return
	 */
	public Object logAround(final ProceedingJoinPoint joinPoint) throws Throwable { // NOSONAR
		// No sonar comment is to skip sonar Throwable violation, which can not
		// be avoided
		Object ret = joinPoint.proceed();
		final Logger targetLogger = LoggingAspect.getLogger(joinPoint.getSourceLocation().getWithinType());
		if (targetLogger.isDebugEnabled()) {
			targetLogger.debug(LoggingAspect.METHOD_ARGS_RETURN, new String[] { joinPoint.getSignature().getName(),
					Arrays.toString(joinPoint.getArgs()), ret != null ? ret.toString() : null });
		}
		return ret;

	}

	/**
	 * Logs the entry to any method under configured package.
	 * 
	 * @param joinPoint
	 * @return
	 */
	public void logBefore(final JoinPoint joinPoint) throws Throwable { // NOSONAR
		// No sonar comment is to skip sonar violation at this line
		final Logger targetLogger = LoggingAspect.getLogger(joinPoint.getSourceLocation().getWithinType());
		if (targetLogger.isDebugEnabled()) {
			targetLogger.debug(LoggingAspect.METHOD_ARGS, joinPoint.getSignature().getName(), joinPoint.getArgs());
		}
	}

	/**
	 * Logs all exception thrown under the configured package.
	 * 
	 * @param joinPoint
	 *            Execution point at which exception arose
	 * @param throwable
	 *            Exception to log
	 */
	public void logGeneralExceptions(final JoinPoint joinPoint, final Throwable throwable) {
		buildAndLogStackTraceBuffer(joinPoint.getSourceLocation().getWithinType(), joinPoint.getSignature().getName(),
				joinPoint.getArgs(), throwable);
	}

	/**
	 * Creates stacktrace from the supplied throwable and logs it along with the
	 * Class name, method name and arguments
	 * 
	 * @param withInType
	 * @param signatureName
	 * @param args
	 * @param throwable
	 */
	protected void buildAndLogStackTraceBuffer(final Class<?> withInType, final String signatureName,
			final Object[] args, final Throwable throwable) {
		StringBuilder traceBuilder = buildTrace(throwable);

		logStackTrace(withInType, signatureName, args, throwable, traceBuilder);

		if (throwable.getCause() != null) {
			buildAndLogStackTraceBuffer(withInType, signatureName, args, throwable.getCause());
		}
	}

	/**
	 * Extracts the stacktrace from the Throwable
	 * 
	 * @param throwable
	 * @return
	 */
	protected StringBuilder buildTrace(Throwable throwable) {
		StringBuilder traceBuilder = null;
		if (throwable != null) {
			final StackTraceElement[] causeTrace = throwable.getStackTrace();
			if (causeTrace != null) {
				traceBuilder = new StringBuilder(causeTrace.length * LoggingAspect.CHAR_LENGTH_120);
				for (final StackTraceElement element : causeTrace) {
					traceBuilder.append(element).append('\n');
				}
			}
		}
		return traceBuilder;
	}

	/**
	 * Logs the class name, method signature name and arguments and the
	 * exception stacktrace
	 * 
	 * @param withInType
	 * @param signatureName
	 * @param args
	 * @param throwable
	 * @param stackTraceBufer
	 */
	protected void logStackTrace(final Class<?> withInType, final String signatureName, final Object[] args,
			final Throwable throwable, StringBuilder stackTraceBufer) {
		LoggingAspect.getLogger(withInType).error(LoggingAspect.EXCEPTION_CAUGHT_SOURCE_METHOD_ARGS_EXCEPTION_TRACE,
				new String[] { withInType.getName(), signatureName, Arrays.toString(args), throwable.toString(),
						stackTraceBufer != null ? stackTraceBufer.toString() : null });
	}
}
