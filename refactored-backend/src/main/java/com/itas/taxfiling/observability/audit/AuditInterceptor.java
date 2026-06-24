package com.itas.taxfiling.observability.audit;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Logs START/SUCCESS/FAILURE for every use case execution with actor, use case name, and duration.
 * No auth annotations — security is fully owned by the API Gateway + Keycloak.
 */
@Aspect
@Component
@Slf4j
public class AuditInterceptor {

    @Around("execution(* com.itas.taxfiling.application.usecase..*(..))")
    public Object auditUseCase(ProceedingJoinPoint joinPoint) throws Throwable {
        String useCaseName = joinPoint.getSignature().getDeclaringType().getSimpleName()
                + "." + joinPoint.getSignature().getName();
        String actor = resolveActor();
        long start = System.currentTimeMillis();

        log.info("[AUDIT] START useCaseName={} actor={}", useCaseName, actor);
        try {
            Object result = joinPoint.proceed();
            long durationMs = System.currentTimeMillis() - start;
            log.info("[AUDIT] SUCCESS useCaseName={} actor={} durationMs={}", useCaseName, actor, durationMs);
            return result;
        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.warn("[AUDIT] FAILURE useCaseName={} actor={} durationMs={} error={}",
                    useCaseName, actor, durationMs, ex.getMessage());
            throw ex;
        }
    }

    private String resolveActor() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String id = attrs.getRequest().getHeader("X-Actor-Id");
                return id != null ? id : "anonymous";
            }
        } catch (Exception ignored) {
            // non-HTTP context
        }
        return "system";
    }
}
