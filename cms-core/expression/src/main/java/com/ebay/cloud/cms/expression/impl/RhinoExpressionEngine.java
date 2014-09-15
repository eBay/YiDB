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

import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

import com.ebay.cloud.cms.expression.IExpression;
import com.ebay.cloud.cms.expression.IExpressionContext;
import com.ebay.cloud.cms.expression.IExpressionEngine;
import com.ebay.cloud.cms.expression.exception.ExpressionEvaluateException;
import com.ebay.cloud.cms.expression.exception.ExpressionParseException;
import com.ebay.cloud.cms.expression.exception.ExpressionTimeoutException;
import com.ebay.cloud.cms.expression.factory.DaemonThreadFactory;

/**
 * @author liasu
 * 
 */
public class RhinoExpressionEngine implements IExpressionEngine {

	private final ScriptableObject parentScope;
	private final Long jsExpressionTimeoutInSeconds;

	public RhinoExpressionEngine(Long jsExpressionTimeoutInSeconds) {
		this.jsExpressionTimeoutInSeconds = jsExpressionTimeoutInSeconds;
		Context rhinoContext = Context.enter();
		try {
			parentScope = rhinoContext.initStandardObjects();
		} finally {
			Context.exit();
		}
	}

	@Override
	public Object evaluate(IExpression expression, IExpressionContext context) {
		final IExpression theExpression = expression;
		final IExpressionContext theContext = context;
		Callable<Object> callable = new Callable<Object>() {
			public Object call() throws ExpressionParseException {
				Object result = null;
				try {
					final Script script = ((RhinoExpression) theExpression).getCompiledExpression();
					final Context rhinoContext = Context.enter();
					final Scriptable scope = new RhinoScriptObject(theContext);
					scope.setPrototype(parentScope);
					scope.setParentScope(null);
					result = script.exec(rhinoContext, scope);
				} catch (ExpressionParseException e) {
					throw new ExpressionParseException(MessageFormat.format("" + e.getMessage()
							+ "\n Failed to parse script:{0}", theExpression.getStringExpression()), e);
				} finally {
					Context.exit();
				}
				return result;
			}
		};

		// Submit the task for execution
		ExecutorService executorService = Executors.newSingleThreadExecutor(DaemonThreadFactory.getInstance());
		Future<Object> future = executorService.submit(callable);
		Object returnVal = null;
		try {
			returnVal = future.get(jsExpressionTimeoutInSeconds, TimeUnit.SECONDS);
			if (returnVal instanceof Undefined) {
				// NOTE : when js snippet doesn't have a return value, rhino
				// return an Undefined. In this case, we treat this as null
				returnVal = null;
			}
		} catch (TimeoutException e) {
			throw new ExpressionTimeoutException(MessageFormat.format("" + e.getMessage()
					+ "\n Timeout to execute script:{0}", expression.getStringExpression()), e);
		} catch (InterruptedException e) {
            throw new ExpressionEvaluateException(MessageFormat.format("" + e.getMessage()
                    + "\n Failed to evaluate script:{0}", expression.getStringExpression()), e);
        } catch (Exception e) {
			if (e.getCause() instanceof ExpressionParseException) {
				throw new ExpressionParseException(MessageFormat.format("" + e.getMessage()
						+ "\n Failed to parse script:{0}", expression.getStringExpression()), e);
			} else {
				throw new ExpressionEvaluateException(MessageFormat.format("" + e.getMessage()
						+ "\n Failed to evaluate script:{0}", expression.getStringExpression()), e);
			}
		} finally {
            future.cancel(true);
            executorService.shutdown();
		}
		return returnVal;
	}

	@Override
	public IExpression compile(String source) {
		Context rhinoContext = Context.enter();
		try {
			Script compiledScript = rhinoContext.compileString(source, "", 0, null);
			return new RhinoExpression(source, compiledScript);
		} catch (Exception e) {
			throw new ExpressionParseException(MessageFormat.format("Failed to compile javascript code snippt: {0}",
					source), e);
		} finally {
			Context.exit();
		}
	}

}
