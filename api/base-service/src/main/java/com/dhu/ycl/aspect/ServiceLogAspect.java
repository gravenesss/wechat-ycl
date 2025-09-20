package com.dhu.ycl.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;


@Slf4j
@Aspect
@Component
public class ServiceLogAspect {
    @Around("execution(* com.dhu.ycl.service..*.*(..))")
    public Object recordTimeCostLog(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        // 目标对象的类名（全限定名）+ 被拦截方法的方法名
        String pointName = joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName();
        stopWatch.start("执行主业务：" + pointName);
        Object proceed = joinPoint.proceed();
        stopWatch.stop();

        log.info(stopWatch.prettyPrint());                  // 表格形式展示各个任务的执行时间、占比
        log.info(stopWatch.shortSummary());                 // 包含总任务数和总耗时的简单总结
        log.info("任务总数：{}", stopWatch.getTaskCount());   // 总任务数
        log.info("任务执行总时间：{}ms", stopWatch.getTotalTimeMillis()); // 总耗时
        long takeTimes = stopWatch.getTotalTimeMillis();    // 总耗时
        if (takeTimes > 3000) {
            log.error("执行位置{}，执行时间太长了，耗费了{}毫秒", pointName, takeTimes);
        } else if (takeTimes > 2000) {
            log.warn("执行位置{}，执行时间稍微有点长，耗费了{}毫秒", pointName, takeTimes);
        } else {
            log.info("执行位置{}，执行时间正常，耗费了{}毫秒", pointName, takeTimes);
        }

        return proceed;
    }
}
