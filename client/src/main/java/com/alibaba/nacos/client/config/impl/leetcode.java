package com.alibaba.nacos.client.config.impl;

/**
 * @Author 萨拉丁 (email: lujf@akulaku.com)
 * @Date 19:53 2021/6/2
 * @Description
 **/
public class leetcode {

    public static void main(String[] args) {
        System.out.println(isPalindromeNumber(1223));
    }

    public static boolean isPalindromeNumber(int x){
        if (x < 0 || (x % 10 == 0 && x != 0)) {
            return false;
        }

        int revIn =0;
        while(x>revIn){
            int digit = x%10;
            revIn = revIn*10+digit;
            x=x/10;
        }

        return x==revIn || x==revIn/10;
    }
}
