/*
Copyright [2013-2014] eBay Software Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/


/**
 * 
 */
/* 
Copyright 2012 eBay Software Foundation 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/ 

package com.ebay.cloud.cms.expression.impl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ebay.cloud.cms.expression.IExpression;
import com.ebay.cloud.cms.expression.IExpressionContext;
import com.ebay.cloud.cms.expression.IExpressionEngine;
import com.ebay.cloud.cms.expression.entity.SampleEntity;
import com.ebay.cloud.cms.expression.entity.SampleEntityExpressionContext;
import com.ebay.cloud.cms.expression.exception.ExpressionParseException;
import com.ebay.cloud.cms.expression.factory.ScriptEngineProvider;

/**
 * @author liasu
 * 
 */
public class RhinoExpressionTest extends ScriptExpressionEngineTest {

    private static final IExpressionEngine engine = ScriptEngineProvider.getEngine();

    @Override
    protected IExpressionEngine getEngine() {
        return engine;
    }

    @Test(expected = ExpressionParseException.class)
    public void testException02() {
        String source = "func($f1, $f2)";
        IExpression expr = getEngine().compile(source);

        SampleEntity entity = new SampleEntity();
        entity.addFieldValue("f1", new String("abc"));
        entity.addFieldValue("f3", new String("abc"));
        IExpressionContext context = new SampleEntityExpressionContext(entity);
        Object result = getEngine().evaluate(expr, context);
        assertEquals(true, result);
    }

    public static class ManifestSampleEntity {
        private final String id;

        public ManifestSampleEntity(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }
    

    @Test
    public void testForeach_null() {
        String source = "if ( $installedManifestsRef ==null || $installedManifestsCur == null) { false; } else {curCount = 0; $installedManifestsRef.forEach(function check(x){if ($installedManifestsCur.indexOf(x) == -1){curCount++}});refCount=0;$installedManifestsRef.forEach(function check(x){if ($installedManifestsCur.indexOf(x) == -1){refCount++}}); curCount==0 && refCount==0;}";

        IExpression expression = getEngine().compile(source);

        SampleEntity entity = new SampleEntity(){
            public void addFieldValue(String fieldName, Object value) {
                List<Object> values = fields.get(fieldName);
                if (value == null) {
                    return;
                }
                if (values == null) {
                    values = new ArrayList<Object>();
                    fields.put(fieldName, values);
                }
                values.add(value);
            }
        };

        entity.addFieldValue("installedManifestsCur", new ManifestSampleEntity[] { new ManifestSampleEntity("abc") });
        IExpressionContext context = new SampleEntityExpressionContext(entity) {
            @Override
            public boolean containsParameter(String name) {
                if ("installedManifestsRef".equals(name)) {
                    return true;
                }
                return super.containsParameter(name);
            }
            @Override
            public Object getParamter(String name) {
                String fieldName = extractEntityField(name);
                if (fieldName != null) {
                    List<?> values = entity.getFieldValues(fieldName);
                    if (values == null) {
                        return null;
                    }
                    if (isCardinalityMany(name)) {
                        return values.toArray();
                    } else {
                        return values.get(0);
                    }
                }
                return null;
            }
        };
        Object result = getEngine().evaluate(expression, context);
        assertEquals(false, result);
    }

    @Test
    public void testForeach00() {
        String source = "var installedManifestsRefIds=[ref.getId() for each (ref in $installedManifestsRef)];"
                + "var installedManifestsCurIds=[cur.getId() for each (cur in $installedManifestsCur)];"
                + "[x for each (x in installedManifestsRefIds) if (installedManifestsCurIds.indexOf(x)==-1)].length==0 "
                + "&& [x for each (x in installedManifestsCurIds) if (installedManifestsRefIds.indexOf(x)==-1)].length==0";

        IExpression expression = getEngine().compile(source);

        SampleEntity entity = new SampleEntity();

        entity.addFieldValue("installedManifestsRef", new ManifestSampleEntity[] { new ManifestSampleEntity("abc"),
                new ManifestSampleEntity("def") });
        entity.addFieldValue("installedManifestsCur", new ManifestSampleEntity[] { new ManifestSampleEntity("abc") });
        IExpressionContext context = new SampleEntityExpressionContext(entity);
        Object result = getEngine().evaluate(expression, context);
        assertEquals(false, result);
    }

    @Test
    public void testForeach01() {
        String source = "if ($installedManifestsRef == $installedManifestsCur) {true;}else {var installedManifestsRefIds=[ref.getId() for each (ref in $installedManifestsRef)];"
                + "var installedManifestsCurIds=[cur.getId() for each (cur in $installedManifestsCur)];"
                + "[x for each (x in installedManifestsRefIds) if (installedManifestsCurIds.indexOf(x)==-1)].length !=0 "
                + "|| [x for each (x in installedManifestsCurIds) if (installedManifestsRefIds.indexOf(x)==-1)].length !=0}";

        IExpression expression = getEngine().compile(source);

        SampleEntity entity = new SampleEntity();

        entity.addFieldValue("installedManifestsRef", new ManifestSampleEntity[] { new ManifestSampleEntity("abc"),
                new ManifestSampleEntity("def") });
        entity.addFieldValue("installedManifestsCur", new ManifestSampleEntity[] { new ManifestSampleEntity("abc") });
        IExpressionContext context = new SampleEntityExpressionContext(entity);
        Object result = getEngine().evaluate(expression, context);
        assertEquals(true, result);
    }

    @Test
    public void testForeach02() {
        String source = "if ($installedManifestsRef == $installedManifestsCur) {true;}else {var installedManifestsRefIds=[ref.getId() for each (ref in $installedManifestsRef)];"
                + "var installedManifestsCurIds=[cur.getId() for each (cur in $installedManifestsCur)];"
                + "[x for each (x in installedManifestsRefIds) if (installedManifestsCurIds.indexOf(x)==-1)].length !=0 "
                + "|| [x for each (x in installedManifestsCurIds) if (installedManifestsRefIds.indexOf(x)==-1)].length !=0}";

        IExpression expression = getEngine().compile(source);

        SampleEntity entity = new SampleEntity();

        entity.addFieldValue("installedManifestsRef", new ManifestSampleEntity[] { new ManifestSampleEntity("abc") });
        entity.addFieldValue("installedManifestsCur", new ManifestSampleEntity[] { new ManifestSampleEntity("abc") });
        IExpressionContext context = new SampleEntityExpressionContext(entity);
        Object result = getEngine().evaluate(expression, context);
        assertEquals(false, result);
    }
    
    @Test(expected=AssertionError.class)
    public void testForeach03() {
        String source = "if ($installedManifestsRef == $installedManifestsCur) {true;}else {var installedManifestsRefIds=[ref.getId() for each (ref in $installedManifestsRef)];"
                + "var installedManifestsCurIds=[cur.getId() for each (cur in $installedManifestsCur)];"
                + "[x for each (x in installedManifestsRefIds) if (installedManifestsCurIds.indexOf(x)==-1)].length !=0 "
                + "|| [x for each (x in installedManifestsCurIds) if (installedManifestsRefIds.indexOf(x)==-1)].length !=0}";

        IExpression expression = getEngine().compile(source);

        SampleEntity entity = new SampleEntity();

        entity.addFieldValue("installedManifestsRef", new ManifestSampleEntity[] { new ManifestSampleEntity(new String(
                "abc")) });// here the rhino use the == not equals to compare
                           // the string. when array indexOf() is called. So the assertion will fail later.
        entity.addFieldValue("installedManifestsCur", new ManifestSampleEntity[] { new ManifestSampleEntity("abc") });
        IExpressionContext context = new SampleEntityExpressionContext(entity);
        Object result = getEngine().evaluate(expression, context);
        assertEquals(false, result);
    }
    
    @Test
    public void testForeach04() {
        String source = "function contains(array, obj){" +
                        "   if(null!=array){"+
                        "   var length = array.length;"+
                        "   for(var index=0; index< length;index++){" +
                        "       if(null != array[index].getId() && array[index].getId().equals(obj.getId())){" +
                        "           return true;"+
                        "       }" +
                        "   }" +
                        "}" +
                        "return false;" +
                        "}" +
 "[x for each (x in $installedManifestsCur) if (!contains($installedManifestsRef, x))].length!=0 || [x for each (x in $installedManifestsRef) if (!contains($installedManifestsCur, x))].length!=0;";

        IExpression expression = getEngine().compile(source);

        //case 0
        SampleEntity entity = new SampleEntity();

        entity.addFieldValue("installedManifestsRef", new ManifestSampleEntity[] { new ManifestSampleEntity(new String("abc")) });
        entity.addFieldValue("installedManifestsCur", new ManifestSampleEntity[] { new ManifestSampleEntity("abc") });
        IExpressionContext context = new SampleEntityExpressionContext(entity);
        Object result = getEngine().evaluate(expression, context);
        assertEquals(false, result);
        
        //case 1
        SampleEntity entity1 = new SampleEntity();

        entity1.addFieldValue("installedManifestsRef", new ManifestSampleEntity[] { new ManifestSampleEntity(new String("abc")), new ManifestSampleEntity("bcd")});
        entity1.addFieldValue("installedManifestsCur", new ManifestSampleEntity[] { new ManifestSampleEntity("abc") });
        IExpressionContext context1 = new SampleEntityExpressionContext(entity1);
        Object result1 = getEngine().evaluate(expression, context1);
        assertEquals(true, result1);
    }

}
