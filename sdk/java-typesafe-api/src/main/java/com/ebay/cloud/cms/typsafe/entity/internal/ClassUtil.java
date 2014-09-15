package com.ebay.cloud.cms.typsafe.entity.internal;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.commons.lang3.StringUtils;

import com.ebay.cloud.cms.typsafe.entity.GenericCMSEntity;
import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;

public class ClassUtil {

//    private static final Logger logger = LoggerFactory.getLogger(ClassUtil.class);
    @SuppressWarnings("unchecked")
    static Class<? extends ICMSEntity> getFieldClass(Class<? extends ICMSEntity> parentClass,
            String fieldName) {
        Class<?> clz = getGetterReturnType(parentClass, fieldName);
        if (clz == null || !ICMSEntity.class.isAssignableFrom(clz)) {
            // cause too much info for non-type-safe usage
//            logger.info("Get reference field class is not a sub class of ICMSEntity, use GenericCMSEntity instead");
            clz = GenericCMSEntity.class;
        }
        return (Class<? extends ICMSEntity>) clz;
    }

    private static Map<String, Map<String, Method>> classMethodMaps = Collections
                                                                            .synchronizedMap(new WeakHashMap<String, Map<String, Method>>());

    static Class<?> getGetterReturnType(Class<?> parentEntityClass, String fieldName) {
        Class<?> clz;
        String getterName = getBeanGetterMethod(fieldName);
        String isName = getBeanIsMethod(fieldName);
        try {
            Map<String, Method> methods = loadMethods(parentEntityClass);
            Method getter = methods.get(getterName);
            if (getter == null) {
                getter = methods.get(isName);
            }

            Class<?> type = getter.getReturnType();
            if (type == List.class) {
                ParameterizedType genericType = (ParameterizedType) getter.getGenericReturnType();
                clz = (Class<?>) genericType.getActualTypeArguments()[0];
            } else {
                clz = type;
            }
        } catch (Exception e) {
//            String str = MessageFormat.format("Can not find field getter {0} or {1} in metaclass {2}", getterName, isName, parentEntityClass.getSimpleName());
//            logger.debug(str);
            clz = null;
        }
        return clz;
    }

    private static Map<String, Method> loadMethods(Class<?> parentEntityClass) {
        Map<String, Method> methodMaps = classMethodMaps.get(parentEntityClass.getCanonicalName());
        if (methodMaps == null) {
            Method[] methods = parentEntityClass.getMethods();
            Map<String, Method> map = new HashMap<String, Method>();
            for (Method m : methods) {
                map.put(m.getName(), m);
            }

            classMethodMaps.put(parentEntityClass.getCanonicalName(), map);
            methodMaps = map;
        }
        return methodMaps;
    }

    static String getBeanGetterMethod(String fieldName) {
        if ("_oid".equals(fieldName)) {
            return "get_id";
        }
        return "get" + StringUtils.capitalize(fieldName);
    }
    
    static String getBeanIsMethod(String fieldName) {
        return "is" + StringUtils.capitalize(fieldName);
    }
}
