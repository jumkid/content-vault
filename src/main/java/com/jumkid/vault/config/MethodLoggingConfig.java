package com.jumkid.vault.config;

import com.jumkid.vault.config.custom.CustomPerformanceMonitorInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@Slf4j
@Configuration
@EnableAspectJAutoProxy
@Aspect
public class MethodLoggingConfig {

    public static final String JOURNEY_ID = "journey_id";

    @Pointcut("execution(* com.jumkid.vault.repository.FileMetadata.*(..))" +
            "|| execution(* com.jumkid.vault.repository.FileStorage.*(..))")
    public void monitor() { }

    @Before("execution(* com.jumkid.vault.controller.*Controller.*(..))")
    public void log4AllControllers(JoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();

        //Get intercepted method details
        String className = methodSignature.getDeclaringType().getSimpleName();
        String methodName = methodSignature.getName();

        StringBuilder sb = new StringBuilder();
        String[] argNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i=0;i<argNames.length;i++) {
            sb.append(argNames[i]).append("=").append(args[i]).append(", ");
        }

        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String journeyId = request.getHeader(JOURNEY_ID);
        MDC.put(JOURNEY_ID, journeyId);

        log.trace("<<< Handshake >>> {}:{} >> End point {}.{} [{}]", JOURNEY_ID, journeyId, className, methodName, sb);
    }

    @Bean
    public CustomPerformanceMonitorInterceptor performanceMonitorInterceptor() {
        return new CustomPerformanceMonitorInterceptor(false);
    }

    @Bean
    public Advisor performanceMonitorAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("com.jumkid.vault.config.MethodLoggingConfig.monitor()");
        return new DefaultPointcutAdvisor(pointcut, performanceMonitorInterceptor());
    }

}
