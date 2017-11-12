package com.yiqiniu.easytrans.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yiqiniu.easytrans.protocol.BusinessIdentifer;
import com.yiqiniu.easytrans.protocol.BusinessProvider;
import com.yiqiniu.easytrans.protocol.EasyTransRequest;

public final class ReflectUtil {
	private ReflectUtil(){
	}
	
	@SuppressWarnings("unchecked")
	public static <P extends EasyTransRequest<R, ?>, R extends Serializable> Class<R> getResultClass(Class<P> paramsClass){
		List<Class<?>> pType = getTypeArguments(EasyTransRequest.class,paramsClass);
		return (Class<R>) pType.get(0);
	}
	
	@SuppressWarnings("unchecked")
	public static Class<? extends EasyTransRequest<?, ?>> getRequestClass(Class<? extends BusinessProvider<?>> providerClass){
		List<Class<?>> pType = getTypeArguments(BusinessProvider.class, providerClass);
		return (Class<? extends EasyTransRequest<?, ?>>) pType.get(0);
	}
	
	public static <P extends EasyTransRequest<?,?>> BusinessIdentifer getBusinessIdentifer(Class<P> o){
		if(o == null){
			return null;
		}
		return o.getAnnotation(BusinessIdentifer.class);
	}
	
	/**
	 * Get the underlying class for a type, or null if the type is a variable
	 * type.
	 * 
	 * @param type
	 *            the type
	 * @return the underlying class
	 */
	private static Class<?> getClass(Type type) {
		if (type instanceof Class) {
			return (Class<?>) type;
		} else if (type instanceof ParameterizedType) {
			return getClass(((ParameterizedType) type).getRawType());
		} else if (type instanceof GenericArrayType) {
			Type componentType = ((GenericArrayType) type)
					.getGenericComponentType();
			Class<?> componentClass = getClass(componentType);
			if (componentClass != null) {
				return Array.newInstance(componentClass, 0).getClass();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	
	private static <T> Type getNecessaryResolvedTypes(Class<T> baseClass, Class<? extends T> childClass,Map<Type, Type> resolvedTypes){
		Type targetType = innerGetMap(baseClass, childClass, resolvedTypes);
		if(targetType == null){
			throw new RuntimeException("programe error!");
		}
		return targetType;
	}


	private static <T> Type innerGetMap(Class<T> baseClass, Type childClass, Map<Type, Type> resolvedTypes) {
		Class<?> rawClass = getClass(childClass);
		
		if(rawClass == null){
			return null;
		}
		
		if(rawClass.equals(baseClass)){
			return childClass;
		}

		if (childClass instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) childClass;
			Class<?> rawType = (Class<?>) parameterizedType.getRawType();

			Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
			TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
			for (int i = 0; i < actualTypeArguments.length; i++) {
				resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
			}
		}
		
		{
			Type genericSuperclass = rawClass.getGenericSuperclass();
			if(genericSuperclass != null){
				Class<?> superClass = null;
				if (genericSuperclass instanceof ParameterizedType){
					superClass = (Class<?>) ((ParameterizedType)genericSuperclass).getRawType();
				}else{
					superClass = (Class<?>) genericSuperclass;
				}
				
				if(baseClass.isAssignableFrom(superClass)){
					return innerGetMap(baseClass, genericSuperclass, resolvedTypes);
				}
			}
		}
		
		{
			Type[] genericInterfaces = rawClass.getGenericInterfaces();
			Class<?> superInterface = null;
			for(Type interfaceType :genericInterfaces){
				if(interfaceType instanceof ParameterizedType){
					superInterface = (Class<?>) ((ParameterizedType)interfaceType).getRawType();
				}else{
					superInterface = (Class<?>) interfaceType;
				}
				
				if(baseClass.isAssignableFrom(superInterface)){
					return innerGetMap(baseClass, interfaceType, resolvedTypes);
				}
			}
		}
		
		return null;
	}

	/**
	 * Get the actual type arguments a child class has used to extend a generic
	 * base class.
	 *
	 * @param baseClass
	 *            the base class
	 * @param childClass
	 *            the child class
	 * @return a list of the raw classes for the actual type arguments.
	 */
	public static <T> List<Class<?>> getTypeArguments(Class<T> baseClass, Class<? extends T> childClass) {
		
		Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
		Type type = getNecessaryResolvedTypes(baseClass, childClass, resolvedTypes);

		// finally, for each actual type argument provided to baseClass,
		// determine (if possible)
		// the raw class for that type argument.
		Type[] actualTypeArguments;
		if (type instanceof Class) {
			actualTypeArguments = ((Class<?>) type).getTypeParameters();
		} else {
			actualTypeArguments = ((ParameterizedType) type)
					.getActualTypeArguments();
		}
		List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
		// resolve types by chasing down type variables.
		for (Type baseType : actualTypeArguments) {
			while (resolvedTypes.containsKey(baseType)) {
				baseType = resolvedTypes.get(baseType);
			}
			typeArgumentsAsClasses.add(getClass(baseType));
		}
		return typeArgumentsAsClasses;
	}
	
}
