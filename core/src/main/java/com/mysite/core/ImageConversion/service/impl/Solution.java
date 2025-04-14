package com.mysite.core.ImageConversion.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Solution {

    public static int[] sortJumbledwords(int[] mapping,int[] nums) {
        List<Integer> digits = new ArrayList<>();

        for (int num : nums) {
            String numStr = String.valueOf(num);  // Convert number to string
            for (char ch : numStr.toCharArray()) {  // Iterate through each character
                digits.add(Character.getNumericValue(ch));  // Convert character to integer
            }
        }
        int[] sort = new int[digits.size()];
        for (int i = 0; i < digits.size(); i++) {
            sort[i] = mapping[digits.get(i)];
        }
        return sort;
    }

    public static void main(String[] args)
    {
        int[] mapping={8,9,4,0,2,1,3,5,7,6};
        int[] nums={991,338,38};

        sortJumbledwords(mapping,nums);
    }
}
