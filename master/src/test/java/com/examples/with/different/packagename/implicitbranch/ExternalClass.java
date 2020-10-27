package com.examples.with.different.packagename.implicitbranch;

public class ExternalClass {

    public void throwNullPointer(String str1,String str2) {
        if (str2 == null){
            throw new NullPointerException();
        }

        if(str2.length() == str1.length()+5){
            System.out.println("Wow! this is Cool!");
        }else{
            throw new NullPointerException();
        }

    }

    public void throwIllegalState(String str) {
        if(! str.contains("[")){
            throw new IllegalStateException();
        }
    }
}
