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

package com.ebay.cloud.cms.metadata.model;

import org.junit.Assert;
import org.junit.Test;

import com.ebay.cloud.cms.metadata.exception.IllegalMetaClassException;
import com.ebay.cloud.cms.metadata.model.MetaField.DataTypeEnum;

public class MetaAttributeTest {

    @Test
    public void testGetValidatedDefaultValue() {
        MetaAttribute ma1 = new MetaAttribute();
        expectValidateException(ma1);
        
        Assert.assertEquals("MetaAttribute".hashCode(), ma1.hashCode());
        ma1.setName("attribute1");
        Assert.assertEquals("attribute1".hashCode(), ma1.hashCode());

        //0. string
        ma1.setDataType(DataTypeEnum.STRING);
        ma1.setDefaultValue("defaultvalue");
        noValidateException(ma1);
        
        //1. json type can not have default value
        ma1.setDataType(DataTypeEnum.JSON);
        ma1.setDefaultValue("json default value");
        expectValidateException(ma1);
        
        //2. integer can only have integer default value
        ma1.setDataType(DataTypeEnum.INTEGER);
        ma1.setDefaultValue("non-integer");
        expectValidateException(ma1);
        ma1.setDefaultValue("1");
        noValidateException(ma1);
        
        //3. long can only have integer default value
        ma1.setDataType(DataTypeEnum.LONG);
        ma1.setDefaultValue("non-long");
        expectValidateException(ma1);
        ma1.setDefaultValue("3");
        noValidateException(ma1);
        
        //4. boolean 
        ma1.setDataType(DataTypeEnum.BOOLEAN);
        ma1.setDefaultValue("non-boolean");
        expectValidateException(ma1);
        
        ma1.setDefaultValue("true");
        noValidateException(ma1);
        ma1.setDefaultValue("false");
        noValidateException(ma1);
        
        //5. date
        ma1.setDataType(DataTypeEnum.DATE);
        ma1.setDefaultValue("non-date");
        expectValidateException(ma1);
        
        ma1.setDefaultValue(MetaAttribute.DATE_DEFAULT_VALUE);
        noValidateException(ma1);
        
        //6. double
        ma1.setDataType(DataTypeEnum.DOUBLE);
        ma1.setDefaultValue("non-double");
        expectValidateException(ma1);
        
        ma1.setDefaultValue("2.3");
        noValidateException(ma1);
        
        //7. enum
        ma1.setDataType(DataTypeEnum.ENUM);
        expectValidateException(ma1);
        
        ma1.addEnumValue("enumvalue1");
        ma1.addEnumValue("enumvalue2");
        ma1.setDefaultValue("non-enum");
        expectValidateException(ma1);

        ma1.setDefaultValue("enumvalue1");
        noValidateException(ma1);
        
        ma1.addEnumValue("");
        expectValidateException(ma1);
        
        ma1.addEnumValue("enumvalue1");
        expectValidateException(ma1);
        
        //8. relationship
        ma1.setDataType(DataTypeEnum.RELATIONSHIP);
        expectValidateException(ma1);

        // Object.equals() and Object.hashCode()
        ma1.setName("ma1");
        ma1.setDefaultValue("dv");
        ma1.setDataType(DataTypeEnum.STRING);

        MetaAttribute ma2 = new MetaAttribute();
        ma2.setName("ma2");
        ma2.setDefaultValue("dv");
        ma2.setDataType(DataTypeEnum.STRING);
        
        Assert.assertTrue(!ma1.equals(ma2));
        
        ma2.setName("ma1");
        ma1.setEnumValues(null);
        Assert.assertEquals(ma1.equals(ma2), ma2.equals(ma1));
        
        MetaAttribute ma3 = new MetaAttribute();
        ma3.setName("testattr");
        ma3.setDbName("dbname");
        expectValidateException(ma3);
        
        MetaAttribute ma4 = new MetaAttribute();
        ma4.setName("testattr");
        ma4.setValueDbName("valuedbname");
        expectValidateException(ma4);
    }
    
    private void noValidateException(MetaAttribute ma1) {
        ma1.validate(false);
    }

    private void expectValidateException(MetaAttribute ma1) {
        try {
            ma1.validate(false);
        } catch (IllegalMetaClassException e) {
            //expected exception
//            e.printStackTrace();
        }
    }

}
