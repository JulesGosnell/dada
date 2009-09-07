package org.omo.jms;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleMethodMapper implements MethodMapper<Integer> {

	protected /* final */ Class<?> interfaze;
	protected /* final */ transient List<Method> indexToMethod;
	protected /* final */ transient Map<Method, Integer> methodToIndex;
	
	
	public SimpleMethodMapper(Class<?> interfaze) {
		this.interfaze = interfaze;
		initialise();
	}
	
	private SimpleMethodMapper() {
		// assumes interfaze is already set up...
		initialise();
	}
	
	public void initialise() {
//		Map<String, Method> order = new TreeMap<String, Method>();
//		for (Method method : interfaze.getMethods()) {
//			order.put(makeKey(method), method);
//		}

		indexToMethod = new ArrayList<Method>();
		methodToIndex = new HashMap<Method, Integer>();
		for (Method method : interfaze.getMethods()) {
			methodToIndex.put(method, indexToMethod.size());
			indexToMethod.add(method);
		}
	}
	
	protected String makeKey(Method method) {
		String key = method.getGenericReturnType().toString()+" "+method.getName()+"("+arrayToString(method.getParameterTypes())+") "+arrayToString(method.getExceptionTypes());
		return key;
	}
	
	protected String arrayToString(Object[] array) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i<array.length; i++) {
			if (i!=0)
				buffer.append(", ");
			buffer.append(array[i].toString());
		}
		return buffer.toString();
	}
	
	@Override
	public Integer getKey(Method method) {
		return methodToIndex.get(method);
	}

	@Override
	public Method getMethod(Integer key) {
		return indexToMethod.get(key);
	}

}
