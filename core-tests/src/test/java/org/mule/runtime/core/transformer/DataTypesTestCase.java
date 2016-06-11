/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Ignore;

@SmallTest
@Ignore
public class DataTypesTestCase extends AbstractMuleTestCase
{
    // //Just used for testing
    // private List<Exception> listOfExceptions;
    //
    // @Test
    // public void testSimpleTypes() throws Exception
    // {
    // DataType dt = dataTypeBuilder(Exception.class).build();
    // DataType dt2 = dataTypeBuilder(Exception.class).build();
    //
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertEquals(dt, dt2);
    //
    // dt2 = dataTypeBuilder(IOException.class).build();
    //
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // //Check mime type matching
    // dt2 = dataTypeBuilder(IOException.class).forMimeType("application/exception+java").build();
    //
    // //Will match because the default mime type is '*/*'
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // dt = dataTypeBuilder(Exception.class).forMimeType(BINARY).build();
    //
    // assertFalse(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // dt = dataTypeBuilder(Exception.class).build();
    // dt2 = STRING_DATA_TYPE;
    //
    // assertFalse(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    // }
    //
    // @Test
    // public void testCollectionTypes() throws Exception
    // {
    // DataType dt = dataTypeBuilder(List.class).build();
    // DataType dt2 = dataTypeBuilder(List.class).build();
    //
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertEquals(dt, dt2);
    //
    // dt2 = dataTypeBuilder(ArrayList.class).build();
    //
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // //Check mime type matching
    // dt2 = dataTypeBuilder(ArrayList.class).forMimeType("application/list+java").build();
    //
    // //Will match because the default mime type is '*/*'
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // dt = dataTypeBuilder(List.class).forMimeType(BINARY).build();
    //
    // assertFalse(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // dt = dataTypeBuilder(List.class).build();
    // dt2 = dataTypeBuilder(Set.class).build();
    //
    // assertFalse(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // }
    //
    // @Test
    // public void testGenericCollectionTypes() throws Exception
    // {
    // DataType dt = dataTypeBuilder().forCollectionType(List.class, Exception.class).build();
    // DataType dt2 = dataTypeBuilder().forCollectionType(List.class, Exception.class).build();
    //
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertEquals(dt, dt2);
    //
    // dt2 = dataTypeBuilder().forCollectionType(ArrayList.class, IOException.class).build();
    //
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // //Check mime type matching
    // dt2 = dataTypeBuilder().forCollectionType(ArrayList.class,
    // IOException.class).forMimeType("application/list+java").build();
    //
    // //Will match because the default mime type is '*/*'
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // dt = dataTypeBuilder().forCollectionType(List.class,
    // Exception.class).forMimeType(BINARY).build();
    //
    // assertFalse(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // //Test Generic Item types don't match
    // dt = dataTypeBuilder().forCollectionType(List.class, Exception.class).build();
    // dt2 = dataTypeBuilder().forCollectionType(List.class, String.class).build();
    //
    // assertFalse(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    // }
    //
    //
    // @Test
    // public void testGenericCollectionTypesFromMethodReturn() throws Exception
    // {
    // DataType dt = createFromReturnType(getClass().getDeclaredMethod("listOfExceptionsMethod",
    // String.class));
    // assertTrue(dt instanceof CollectionDataType);
    //
    // assertEquals(List.class, dt.getType());
    // assertEquals(Exception.class, ((CollectionDataType) dt).getItemType());
    //
    // DataType dt2 = createFromReturnType(getClass().getDeclaredMethod("listOfExceptionsMethod",
    // String.class));
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertEquals(dt, dt2);
    //
    // dt2 = createFromReturnType(getClass().getDeclaredMethod("listOfExceptionsMethod",
    // Integer.class));
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    //
    // }
    //
    // @Test
    // public void testGenericCollectionTypesFromMethodParam() throws Exception
    // {
    // DataType dt = createFromParameterType(getClass().getDeclaredMethod("listOfExceptionsMethod",
    // Collection.class), 0);
    // assertTrue(dt instanceof CollectionDataType);
    //
    // assertEquals(Collection.class, dt.getType());
    // assertEquals(Exception.class, ((CollectionDataType) dt).getItemType());
    //
    // DataType dt2 = createFromParameterType(getClass().getDeclaredMethod("listOfExceptionsMethod",
    // Collection.class), 0);
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertEquals(dt, dt2);
    //
    // dt2 = createFromParameterType(getClass().getDeclaredMethod("listOfExceptionsMethod",
    // List.class), 0);
    // assertTrue(dt.isCompatibleWith(dt2));
    // assertFalse(dt.equals(dt2));
    // }
    //
    // @Test
    // public void testGenericCollectionTypesFromField() throws Exception
    // {
    // DataType dt = createFromField(getClass().getDeclaredField("listOfExceptions"));
    // assertTrue(dt instanceof CollectionDataType);
    //
    // assertEquals(List.class, dt.getType());
    // assertEquals(Exception.class, ((CollectionDataType) dt).getItemType());
    // }
    //
    // private List<Exception> listOfExceptionsMethod(String s)
    // {
    // return null;
    // }
    //
    // private ArrayList<IOException> listOfExceptionsMethod(Integer i)
    // {
    // return null;
    // }
    //
    // private String listOfExceptionsMethod(Collection<Exception> exceptions)
    // {
    // return null;
    // }
    //
    // private Integer listOfExceptionsMethod(List<IOException> ioExceptions)
    // {
    // return null;
    // }
}
