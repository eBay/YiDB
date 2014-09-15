package com.ebay.cloud.cms.typsafe.entity.internal;

import java.util.Date;

import com.ebay.cloud.cms.typsafe.entity.ICMSEntity;

public abstract class QueryFilterBuilder {

    public static <T extends ICMSEntity> String convertQueryValue(Class<T> entityClass, String fieldName,
            Object fieldValue, String... includeFieldNames) {
        if (fieldValue == null) {
            return convertNotExistingCriteria(fieldName);
        }

        Class<?> returnType = ClassUtil.getGetterReturnType(entityClass, fieldName);
        if (returnType == null) {
            returnType = fieldValue.getClass();
        }

        return convertFilter(fieldName, fieldValue, returnType, includeFieldNames);
    }

    private static String convertNotExistingCriteria(String fieldName) {
        // TODO: add domain/env
        return "[ not exists @" + fieldName + " ]";
    }

    private static String convertFilter(String fieldName, Object fieldValue, Class<?> returnType, String... includeFieldNames) {
        StringBuilder sb = new StringBuilder("[@").append(fieldName).append(" = ");
        Class<?> valueClass = returnType;
        if (valueClass == Boolean.class) {
            sb.append(fieldValue);
        } else if (valueClass == Integer.class) {
            sb.append(fieldValue);
        } else if (valueClass == Double.class) {
            sb.append(fieldValue);
        } else if (valueClass == Long.class) {
            sb.append(fieldValue);
        } else if (valueClass.isEnum()) {
            // assert type validity?
            sb.append("\"").append(fieldValue.toString()).append("\"");
        } else if (valueClass == String.class) {
            sb.append("\"").append(fieldValue.toString()).append("\"");
        } else if (valueClass == Date.class) {
            // assert type validity?
            sb.append("date(").append(((Date) fieldValue).getTime()).append(")");
        } else {
            // for other case like json/reference, simply as _oid
            sb.append("\"").append(fieldValue.toString()).append("\"");
        }
//        sb.append(" and @domain=\"ebay.com\" ");
        sb.append(']');

        if (includeFieldNames != null && includeFieldNames.length > 0) {
            sb.append("{");
            for (String field : includeFieldNames) {
                sb.append("@").append(field).append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append("}");
        }

        return sb.toString();
    }

    public static String convertQueryValue(String fieldName, Object fieldValue, String... includeFieldNames) {
        if (fieldValue == null) {
            return convertNotExistingCriteria(fieldName);
        }
        Class<?> type = fieldValue.getClass();
        return convertFilter(fieldName, fieldValue, type, includeFieldNames);
    }
}
