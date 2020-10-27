package com.examples.with.different.packagename.implicitbranch;

public class ExternalImplicitException {

    public void method0(String str1, String str2){
        if(str1 == null){
            throw new NullPointerException();
        }

        ExternalClass externalClass = new ExternalClass();
        externalClass.throwNullPointer(str1,str2);
        externalClass.throwIllegalState(str1);
        externalClass.throwIllegalState(str2);
        System.out.println("We have reached to the end of the method");

    }
}
