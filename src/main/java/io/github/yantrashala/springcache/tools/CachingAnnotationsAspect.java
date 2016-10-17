package io.github.yantrashala.springcache.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Aspect to intercept invocation of methods annotated with @Cacheable.
 * 
 * @author Saiyed Zaidi
 * @copyright @2016 http://yantrashala.github.io
 * @version 1.0
 */
@Component
public class CachingAnnotationsAspect {

	@Autowired
	private InvocationRegistry cacheRefreshSupport;

	/**
	 * Intercepts invocations of methods annotated with @Cacheable and
	 * invokes cacheRefreshSupport with the execution information. Pointcut
	 * configured externally using XML config to keep the application flexible.
	 * 
	 * Configure this aspect to intercept the classes where refreshing caches are needed.
	 * 
	 * @param joinPoint
	 * @return
	 * @throws Throwable
	 */
	public Object interceptCacheables(ProceedingJoinPoint joinPoint) throws Throwable {// NOSONAR
		// No sonar comment is to avoid "throws Throwable" sonar violation
		Method annotatedElement = getSpecificmethod(joinPoint);
		List<Cacheable> annotations = getMethodAnnotations(annotatedElement, Cacheable.class);
		Set<String> cacheSet = new HashSet<String>();
		for (Cacheable cacheables : annotations) {
			cacheSet.addAll(Arrays.asList(cacheables.value()));
		}
		cacheRefreshSupport.registerInvocation(joinPoint.getTarget(), annotatedElement, joinPoint.getArgs(), cacheSet);
		return joinPoint.proceed();
	}

	/**
	 * Finds out the most specific method when the execution reference is an
	 * interface or a method with generic parameters
	 * 
	 * @param pjp
	 * @return
	 */
	private Method getSpecificmethod(ProceedingJoinPoint pjp) {
		MethodSignature methodSignature = (MethodSignature) pjp.getSignature();
		Method method = methodSignature.getMethod();
		// The method may be on an interface, but we need attributes from the
		// target class. If the target class is null, the method will be
		// unchanged.
		Class<?> targetClass = AopProxyUtils.ultimateTargetClass(pjp.getTarget());
		if (targetClass == null && pjp.getTarget() != null) {
			targetClass = pjp.getTarget().getClass();
		}
		Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
		// If we are dealing with method with generic parameters, find the
		// original method.
		specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
		return specificMethod;
	}

	/**
	 * Parses all annotations declared on the Method
	 * 
	 * @param ae
	 * @param annotationType
	 *            Annotation type to look for
	 * @return
	 */
	private static <T extends Annotation> List<T> getMethodAnnotations(AnnotatedElement ae, Class<T> annotationType) {
		List<T> anns = new ArrayList<T>(2);
		// look for raw annotation
		T ann = ae.getAnnotation(annotationType);
		if (ann != null) {
			anns.add(ann);
		}
		// look for meta-annotations
		for (Annotation metaAnn : ae.getAnnotations()) {
			ann = metaAnn.annotationType().getAnnotation(annotationType);
			if (ann != null) {
				anns.add(ann);
			}
		}
		return (anns.isEmpty() ? null : anns);
	}
}