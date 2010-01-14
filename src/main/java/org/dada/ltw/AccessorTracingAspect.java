/*
 * Copyright (c) 2009, Julian Gosnell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dada.ltw;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class AccessorTracingAspect {

	private static final int ACCESSOR_FIELD_LENGTH = 3;

    private static final Logger LOG = LoggerFactory.getLogger(AccessorTracingAspect.class);

	@Around("getterPointcut()")
    public Object getterAdvice(ProceedingJoinPoint pjp) throws Throwable {
		Object value = pjp.proceed();
		LOG.info("get: " + pjp.getTarget()+ "." + fieldName(pjp)+ " = " + value);
		return value;
    }

	private String fieldName(ProceedingJoinPoint pjp) {
		return pjp.getSignature().getName().substring(ACCESSOR_FIELD_LENGTH);
	}

	@Around("setterPointcut()")
    public Object setterAdvice(ProceedingJoinPoint pjp) throws Throwable {
        try {
            return pjp.proceed();
        } finally {
            LOG.info("set: " + pjp.getTarget() + "." + fieldName(pjp) + " = " + pjp.getArgs()[0]);
        }
    }

    @Pointcut("execution(public org.dada.ltw.Identifiable+ org.dada.ltw.Identifiable+.get*())")
    public void getterPointcut() {
    }

    @Pointcut("execution(public void org.dada.ltw.Identifiable+.set*(org.dada.ltw.Identifiable+))")
    public void setterPointcut() {
    }

}
