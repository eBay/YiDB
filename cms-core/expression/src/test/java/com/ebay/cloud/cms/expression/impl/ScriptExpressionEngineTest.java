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
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ebay.cloud.cms.expression.IExpression;
import com.ebay.cloud.cms.expression.IExpressionContext;
import com.ebay.cloud.cms.expression.IExpressionEngine;
import com.ebay.cloud.cms.expression.entity.SampleEntity;
import com.ebay.cloud.cms.expression.entity.SampleEntityExpressionContext;
import com.ebay.cloud.cms.expression.exception.ExpressionEvaluateException;
import com.ebay.cloud.cms.expression.exception.ExpressionParseException;


public class ScriptExpressionEngineTest {

    private final static IExpressionEngine engine = ScriptExpressionEngine.getInstance();
    
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test01() {
        String source = "function add(op1,op2){return op1+op2} add($f1, $f2)";
        IExpression expr = getEngine().compile(source);
        
        SampleEntity entity = new SampleEntity();
        entity.addFieldValue("f1", 1);
        entity.addFieldValue("f2", 2);
        entity.addFieldValue("f3", 3);
        IExpressionContext context = new SampleEntityExpressionContext(entity);
        
        Object result = getEngine().evaluate(expr, context);
        assertEquals(3.0, result); 
    }

    protected IExpressionEngine getEngine() {
        return engine;
    }
    
    @Test
    public void test02() {
        String source = "$f1 + $f2";
        IExpression expr = getEngine().compile(source);
        
        SampleEntity entity = new SampleEntity();
        entity.addFieldValue("f1", 1);
        entity.addFieldValue("f2", 2);
        IExpressionContext context = new SampleEntityExpressionContext(entity);
        
        Object result = getEngine().evaluate(expr, context);
        assertEquals(3.0, result); 
    }

    @Test
    public void test03() {
        String source = "$f1 == $f2";
        IExpression expr = getEngine().compile(source);
        
        SampleEntity entity = new SampleEntity();
        entity.addFieldValue("f1", new String("abc"));
        entity.addFieldValue("f2", new String("abc"));
        IExpressionContext context = new SampleEntityExpressionContext(entity);        
        Object result = getEngine().evaluate(expr, context);
        assertEquals(true, result); 
        
        entity = new SampleEntity();
        entity.addFieldValue("f1", new String("abc1"));
        entity.addFieldValue("f2", new String("abc"));
        context = new SampleEntityExpressionContext(entity);        
        result = getEngine().evaluate(expr, context);
        assertEquals(false, result); 
    }
    
    @Test
    public void test04() {
        String source = "$_f1[1] + $_f2[1]";
        IExpression expr = getEngine().compile(source);
        
        SampleEntity entity = new SampleEntity();
        entity.addFieldValue("f1", 1);
        entity.addFieldValue("f1", 10);
        entity.addFieldValue("f2", 2);
        entity.addFieldValue("f2", 20);
        IExpressionContext context = new SampleEntityExpressionContext(entity);
        
        Object result = getEngine().evaluate(expr, context);
        assertEquals("1020", result); 
    }
    
    @Test (expected=ExpressionParseException.class)
    public void testException01() {
        String source = "$f1 == $f2";
        IExpression expr = getEngine().compile(source);
        
        SampleEntity entity = new SampleEntity();
        entity.addFieldValue("f1", new String("abc"));
        entity.addFieldValue("f3", new String("abc"));
        IExpressionContext context = new SampleEntityExpressionContext(entity);        
        Object result = getEngine().evaluate(expr, context);
        assertEquals(true, result); 
    }
    
    @Test (expected=ExpressionEvaluateException.class)
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
    
    @Test
    public void testException03() {
    	String source = "function compare(x, y) {if (x === y) {return true;}if(x.length != y.length){return false;}for (key in x) {if (y.indexOf(x[key]) == -1) {return false;}} for (key in y) {if (x.indexOf(y[key]) == -1) {return false;}} return true;}compare($installedManifestsCur, $installedManifestsRef);";
    	IExpression expr = getEngine().compile(source);
    	
    	SampleEntity entity = new SampleEntity();
    	ArrayList<String> installedManifestsRefList = new ArrayList<String>();
    	installedManifestsRefList.add("test1");
    
    	ArrayList<String> installedManifestsCurList = new ArrayList<String>();
    	installedManifestsRefList.add("test2");
    	
    	entity.addFieldValue("installedManifestsRef", installedManifestsRefList);
    	entity.addFieldValue("installedManifestsCur", installedManifestsCurList);
    	IExpressionContext context = new SampleEntityExpressionContext(entity);        
    	Object result = getEngine().evaluate(expr, context);
    	assertEquals(false, result); 
    }
    
    @Test
    public void testException04() {
    	String source = "function compare(x, y) {if (x === y) {return true;} if(!x  || !y) {return false;} if(x.size() != y.size()){return false;}for (var index=0; index < x.size(); index++) {if (y.indexOf(x.get(index)) == -1) {return false;}} for (var index=0; index < y.size(); index++) {if (x.indexOf(y.get(index)) == -1) {return false;}} return true;}compare($installedManifestsCur, $installedManifestsRef);";
    	IExpression expr = getEngine().compile(source);
    	
    	SampleEntity entity = new SampleEntity();
    	ArrayList<String> installedManifestsRefList = new ArrayList<String>();
    	installedManifestsRefList.add("test1");
    	
    	installedManifestsRefList.add("test2");
    	
    	ArrayList<String> installedManifestsCurList = new ArrayList<String>();
    	installedManifestsCurList.add("test2");
    	installedManifestsCurList.add("test1");
    	
    	entity.addFieldValue("installedManifestsRef", installedManifestsRefList);
    	entity.addFieldValue("installedManifestsCur", installedManifestsCurList);
    	IExpressionContext context = new SampleEntityExpressionContext(entity);        
    	Object result = getEngine().evaluate(expr, context);
    	assertEquals(true, result); 
    }

    @Test
    public void testException05() {
    	String source = "function compare(x, y) {if (x === y) {return true;} if(x!=null || y!= null) {return false;} if(x.size() != y.size()){return false;}for (var index=0; index < x.size(); index++) {if (y.indexOf(x.get(index)) == -1) {return false;}} for (var index=0; index < y.size(); index++) {if (x.indexOf(y.get(index)) == -1) {return false;}} return true;}compare($installedManifestsCur, $installedManifestsRef);";
    	IExpression expr = getEngine().compile(source);
    	
    	SampleEntity entity = new SampleEntity();
    	
    	ArrayList<String> installedManifestsCurList = new ArrayList<String>();
    	installedManifestsCurList.add("test2");
    	installedManifestsCurList.add("test1");
    	
    	entity.addFieldValue("installedManifestsRef", null);
    	entity.addFieldValue("installedManifestsCur", installedManifestsCurList);
    	IExpressionContext context = new SampleEntityExpressionContext(entity);        
    	Object result = getEngine().evaluate(expr, context);
    	assertEquals(false, result); 
    }
    
    @Test
    public void testException06() {
    	String source = "function compare(x, y) {if (x === y) {return true;} if(x!=null || y!= null) {return false;} if(x.size() != y.size()){return false;}for (var index=0; index < x.size(); index++) {if (y.indexOf(x.get(index)) == -1) {return false;}} for (var index=0; index < y.size(); index++) {if (x.indexOf(y.get(index)) == -1) {return false;}} return true;}compare($installedManifestsCur, $installedManifestsRef);";
    	IExpression expr = getEngine().compile(source);
    	
    	SampleEntity entity = new SampleEntity();
    	
    	ArrayList<String> installedManifestsCurList = new ArrayList<String>();
    	installedManifestsCurList.add("test2");
    	installedManifestsCurList.add("test1");
    	
    	entity.addFieldValue("installedManifestsRef", null);
    	entity.addFieldValue("installedManifestsCur", installedManifestsCurList);
    	IExpressionContext context = new SampleEntityExpressionContext(entity);        
    	Object result = getEngine().evaluate(expr, context);
    	assertEquals(false, result); 
    }
    
    @Test
    public void testException07() {
    	String source = "function compare(x, y) {if (x === y) {return true;} if(x!=null || y!= null) {return false;} if(x.size() != y.size()){return false;}for (var index=0; index < x.size(); index++) {if (y.indexOf(x.get(index)) == -1) {return false;}} for (var index=0; index < y.size(); index++) {if (x.indexOf(y.get(index)) == -1) {return false;}} return true;}compare($installedManifestsCur, $installedManifestsRef);";
    	IExpression expr = getEngine().compile(source);
    	
    	SampleEntity entity = new SampleEntity();
    	
    	ArrayList<String> installedManifestsCurList = new ArrayList<String>();
    	installedManifestsCurList.add("test2");
    	installedManifestsCurList.add("test1");
    	
    	entity.addFieldValue("installedManifestsRef", null);
    	entity.addFieldValue("installedManifestsCur", null);
    	IExpressionContext context = new SampleEntityExpressionContext(entity);        
    	Object result = getEngine().evaluate(expr, context);
    	assertEquals(true, result); 
    }
    
    private class TestRunner implements Runnable{

        private IExpression expr = null;
        private CountDownLatch doneSignal = null;
        private AtomicInteger successCount = null;
        private Random rand = null;
        public TestRunner(IExpression expr, CountDownLatch signal, AtomicInteger count) {
            this.expr = expr;
            this.doneSignal = signal;
            this.rand = new Random();
            this.successCount = count;
        }
        
        @Override
        public void run() {            
            try {
                for (int i = 0; i < 100; i++) {
                    int f1 = rand.nextInt(1000);
                    int f2 = rand.nextInt(1000);
                    double sum = f1 + f2;
                    
                    SampleEntity entity = new SampleEntity();
                    entity.addFieldValue("f1", f1);
                    entity.addFieldValue("f2", f2);
                    
                    IExpressionContext context = new SampleEntityExpressionContext(entity); 
                    Object result;
                    try {
                        result = getEngine().evaluate(expr, context);
                        assertEquals(sum, result);                        
                    } catch (Throwable t) {
                        t.printStackTrace();
                        throw new RuntimeException(t);
                    }
                }
                successCount.incrementAndGet();
            } finally {
                doneSignal.countDown();
            }            
        }        
    }

    @Test
    public void testMT01() throws Exception {
        String source = "$f1 + $f2";
        IExpression expr = getEngine().compile(source);
        
        int poolSize = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch doneSignal = new CountDownLatch(poolSize);
        for (int i = 0; i < poolSize; i++) {
            Thread t = new Thread(new TestRunner(expr, doneSignal, successCount));
            t.setDaemon(false);
            t.start();
        }
        
        doneSignal.await();        
        assertEquals(poolSize, successCount.get());
    }
    
    @Test
    public void testUndefiendReturnValueVerfication() {
        String source = "if($f1 > $f2) { $f1 == $f2 } ";
        IExpression expr = getEngine().compile(source);

        SampleEntity entity = new SampleEntity();
        entity.addFieldValue("f1", new Integer(1));
        entity.addFieldValue("f2", new Integer(2));
        IExpressionContext context = new SampleEntityExpressionContext(entity);
        Object result = getEngine().evaluate(expr, context);
        Assert.assertNull(result);
    }
    
}
