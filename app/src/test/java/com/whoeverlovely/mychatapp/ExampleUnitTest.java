package com.whoeverlovely.mychatapp;

import android.util.Log;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {

        String s = "4_AES";
        s = s.substring(0,s.indexOf("_AES"));
        System.out.println(s);
    }
}