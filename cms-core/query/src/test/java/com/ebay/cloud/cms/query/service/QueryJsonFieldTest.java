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

package com.ebay.cloud.cms.query.service;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.ebay.cloud.cms.dal.entity.IEntity;
import com.ebay.cloud.cms.query.MongoBaseTest;
import com.ebay.cloud.cms.query.exception.QueryException.QueryErrCodeEnum;
import com.ebay.cloud.cms.query.parser.QueryParseException;
import com.mongodb.BasicDBObject;

/**
 * @author liasu
 *
 */
public class QueryJsonFieldTest extends MongoBaseTest {

    @Test
    public void testJsonField00() {
        IQueryResult result = queryService.query("ServiceInstance[@properties.$f3 = 1]", raptorContext);
        Assert.assertEquals(result.getEntities().size(), 2);
        result = queryService.query("ServiceInstance[@properties.$f1 = 1]", raptorContext);
        Assert.assertEquals(result.getEntities().size(), 1);
        result = queryService.query("ServiceInstance[@properties.$f10 = 1]", raptorContext);
        Assert.assertTrue(result.getEntities().isEmpty());
    }

    @Test
    public void testJsonField01Array() {
        IQueryResult result1 = queryService.query("ServiceInstance[@properties.$f1 = 1234563]", raptorContext);
        Assert.assertNotNull(result1);
        Assert.assertNotNull(result1.getEntities());
        Assert.assertEquals(1, result1.getEntities().size());
    }
    
    public void testJsonField02Array() {
        IQueryResult result0 = queryService.query("ServiceInstance[@properties.$nestF2Array.$ff1 = 1234563]", raptorContext);
        Assert.assertNotNull(result0);
        Assert.assertNotNull(result0.getEntities());
        Assert.assertEquals(1, result0.getEntities().size());
    }

    @Test
    public void testJsonField03MultiLevelNest() {
        IQueryResult result0 = queryService.query("ServiceInstance[@properties.$nestF1.$nestFF1.$fff1 = 1337667919420]", raptorContext);
        Assert.assertNotNull(result0);
        Assert.assertNotNull(result0.getEntities());
        Assert.assertEquals(1, result0.getEntities().size());
        
        // rely on mongo db support array element query. Here the "nested-field-1" and "nested-field-2" are array element in fff2
        IQueryResult result1 = queryService.query("ServiceInstance[@properties.$nestF1.$nestFF1.$fff2 = \"nested-field-1\"]", raptorContext);
        Assert.assertNotNull(result1);
        Assert.assertNotNull(result1.getEntities());
        Assert.assertEquals(1, result1.getEntities().size());
        IQueryResult result2 = queryService.query("ServiceInstance[@properties.$nestF1.$nestFF1.$fff2 = \"nested-field-2\"]", raptorContext);
        Assert.assertNotNull(result2);
        Assert.assertNotNull(result2.getEntities());
        Assert.assertEquals(1, result2.getEntities().size());
        
        Assert.assertEquals(result1.getEntities().get(0).getId(), result2.getEntities().get(0).getId());
    }

    @Test
    public void testJsonField04Projection() {
        IQueryResult result = queryService.query("ServiceInstance[@properties.$f3 = 1]{@properties}", raptorContext);
        Assert.assertEquals(result.getEntities().size(), 2);
        for (IEntity entity : result.getEntities()) {
            Assert.assertTrue(entity.getFieldValues("properties").size() > 0);
            BasicDBObject propertyObj = (BasicDBObject)entity.getFieldValues("properties").get(0);
            Assert.assertNotNull(propertyObj);
            Assert.assertEquals(1, propertyObj.get("f3"));
        }
    }

    @Test
    public void testJsonField_regexp() {
        // case 0:
        IQueryResult result = queryService.query("ServiceInstance[@properties.$f4 =~ \"a\"]{@properties}", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        for (IEntity entity : result.getEntities()) {
            Assert.assertTrue(entity.getFieldValues("properties").size() > 0);
            BasicDBObject propertyObj = (BasicDBObject)entity.getFieldValues("properties").get(0);
            Assert.assertNotNull(propertyObj);
            Assert.assertEquals("a", propertyObj.get("f4"));
        }
        // case 1:
        result = queryService.query("ServiceInstance[@properties.$f4 =~ \"^a$\"]{@properties}", raptorContext);
        Assert.assertEquals(1, result.getEntities().size());
        // case 2:
        result = queryService.query("ServiceInstance[@properties.$f4 =~ \"aa\"]{@properties}", raptorContext);
        Assert.assertEquals(0, result.getEntities().size());
    }
    
    @Test
    public void testJsonField05Projection() {
        IQueryResult result = queryService.query("ServiceInstance[@properties.$f1 = 1] {@properties.$f1, @properties.$f3}", raptorContext);
        Assert.assertEquals(result.getEntities().size(), 1);
        for (IEntity entity : result.getEntities()) {
            Assert.assertTrue(entity.getFieldValues("properties").size() > 0);
            BasicDBObject propertyObj = (BasicDBObject) entity.getFieldValues("properties").get(0);
            Assert.assertNotNull(propertyObj);
            Assert.assertEquals(1, propertyObj.get("f3"));

            // not projected field shouldn't be fetched
            Assert.assertNull(propertyObj.get("nestF1"));
        }
    }

    @Test
    public void testJsonField06ArrayProjection() {
        // properties itself is an array 
        IQueryResult result = queryService.query("ServiceInstance[@properties.$f1 = 1234563 ]{@properties.$f2}", raptorContext);
        Assert.assertEquals(result.getEntities().size(), 1);
        for (IEntity entity : result.getEntities()) {
            Assert.assertTrue(entity.getFieldValues("properties").size() > 0);
            BasicDBObject propertyObj = (BasicDBObject) entity.getFieldValues("properties").get(0);
            Assert.assertNotNull(propertyObj);

            Object f2Object = propertyObj.get("f2");
            Assert.assertNotNull(f2Object);
            Assert.assertTrue(f2Object instanceof Collection);
            Assert.assertEquals(2, ((Collection<?>) f2Object).size());

            // not projected field shouldn't be fetched
            Assert.assertNull(propertyObj.get("f1"));
        }
    }

    @Test
    public void testJsonField07MultiJsonProjection() {
        // In object of properties array, some objects has f2 only, some objects has f3 only
        IQueryResult result = queryService.query("ServiceInstance[@properties.$f1 = 1234563 or @properties.$f3 = 1234563 ]{@properties.$f2, @properties.$f3}", raptorContext);
        Assert.assertEquals(result.getEntities().size(), 1);
        for (IEntity entity : result.getEntities()) {
            Assert.assertTrue(entity.getFieldValues("properties").size() > 0);
            BasicDBObject propertyObj =  (BasicDBObject)entity.getFieldValues("properties").get(0);
            Assert.assertNotNull(propertyObj);

            Object f2Object = propertyObj.get("f2");
            Assert.assertNotNull(f2Object);
            Assert.assertTrue(f2Object instanceof Collection);
            Assert.assertEquals(2, ((Collection<?>) f2Object).size());
            Assert.assertNotNull(propertyObj.get("f3"));

            // not projected field shouldn't be fetched
            Assert.assertNull(propertyObj.get("f1"));

        }
    }

    @Test
    public void testJsonField08InEmbedEntity() {
        IQueryResult result = queryService.query("ResourceContainer.resources[@userData.$admin=\"cms-dev-admin@ebay.com\"]", stratusContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(3, result.getEntities().size());
    }

    @Test
    public void testJsonField09InEmbedEntityMultiLevel() {
        IQueryResult result = queryService.query("ResourceContainer.resources[@userData.$useOrgnization.$dev.$team=\"cloud-dev\"]", stratusContext);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getEntities());
        Assert.assertEquals(1, result.getEntities().size());
    }

    @Test
    public void testJsonField10_escaped() {
        raptorContext.setAllowFullTableScan(true);
        {
            String str = "cd c:\\temp; wget --quiet --no-check-certificate -O fp.pl https://source.com/pages/SCM/puppet/fp/fp.pl; perl fp.pl";
            System.out.println(str);
            IQueryResult result = queryService.query("ServiceInstance[@properties.$script = \"" + str
                    + "\"]{@properties}", raptorContext);
            Assert.assertEquals(1, result.getEntities().size());
            for (IEntity entity : result.getEntities()) {
                Assert.assertTrue(entity.getFieldValues("properties").size() > 0);
                BasicDBObject propertyObj = (BasicDBObject) entity.getFieldValues("properties").get(0);
                Assert.assertNotNull(propertyObj);
                Assert.assertEquals(str, propertyObj.get("script"));
            }
        }

        {
            String str = "\\\\";
            System.out.println(str);
            IQueryResult result = queryService.query("ServiceInstance[@properties.$newname = \"" + str
                    + "\"]{@properties}", raptorContext);
            Assert.assertEquals(1, result.getEntities().size());
            for (IEntity entity : result.getEntities()) {
                Assert.assertTrue(entity.getFieldValues("properties").size() > 0);
                BasicDBObject propertyObj = (BasicDBObject) entity.getFieldValues("properties").get(0);
                Assert.assertNotNull(propertyObj);
            }
        }
    }

    //
    // This is current limtation of CMS query
    //
    @Test
    public void testJsonField10_escaped_singleBackSlash() {
        raptorContext.setAllowFullTableScan(true);
        String str = "\\";
        System.out.println(str);
        try {
            queryService.query("ServiceInstance[@properties.$name = \"" + str + "\"]{@properties}", raptorContext);
            Assert.fail();
        } catch (QueryParseException exception) {
            Assert.assertEquals(QueryErrCodeEnum.PARSE_GRAMMER_ERROR.getErrorCode(), exception.getErrorCode());
        }
    }

}
