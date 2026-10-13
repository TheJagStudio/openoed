package gov.openoed.api.audit;

import gov.openoed.api.outbox.AuditRecorder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** AOP interceptor: every annotated mutation (and its denial) is streamed to the audit ledger. */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

	private final AuditRecorder recorder;

	@Around("@annotation(audited)")
	public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
		HttpServletRequest request = currentRequest();
		try {
			Object result = joinPoint.proceed();
			recorder.success(audited.action(), request);
			return result;
		} catch (RuntimeException ex) {
			recorder.failure(audited.action(), ex.getClass().getSimpleName(), request);
			throw ex;
		}
	}

	private static HttpServletRequest currentRequest() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) return attrs.getRequest();
		return null;
	}
}
