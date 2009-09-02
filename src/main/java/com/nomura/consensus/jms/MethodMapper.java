package com.nomura.consensus.jms;

import java.io.Serializable;
import java.lang.reflect.Method;

public interface MethodMapper<T> extends Serializable {

	T getKey(Method method);
	Method getMethod(T key);
	
}
