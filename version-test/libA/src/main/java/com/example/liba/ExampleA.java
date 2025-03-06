package com.example.liba;

import com.example.libb.ExampleB;

public class ExampleA {
    public void printExampleBVersion() {
        ExampleB b = new ExampleB();
        System.out.println("ExampleB says: " + b.getVersion());
    }
}
