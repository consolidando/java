/*
 * Copyright (c) 2024 joanribalta@elmolidelanoguera.com
 * License: CC BY-NC-ND 4.0 (https://creativecommons.org/licenses/by-nc-nd/4.0/)
 * Blog Consolidando: https://diy.elmolidelanoguera.com/
 */

package com.elmolidelanoguera.stressor;

/**
 *
 * @author joanr
 */

import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

public class RealTimeStatistics
{
    private final DoubleAdder sum = new DoubleAdder(); // Sum of all values
    private final DoubleAdder sumOfSquares = new DoubleAdder(); // Sum of squares of all values
    private final LongAdder count = new LongAdder(); // Count of values

    // Method to register a value during a request concurrently
    public void addValue(double value)
    {
        sum.add(value);
        sumOfSquares.add(value * value);
        count.increment();
    }

    // Method to calculate the mean
    public double calculateMean()
    {
        long totalCount = count.sum(); 
        if (totalCount == 0)
        {            
            return Double.NaN; 
        }

        return sum.sum() / totalCount; 
    }

    // Method to calculate the standard deviation
    public double calculateStandardDeviation()
    {
        long totalCount = count.sum(); 
        if (totalCount == 0)
        {         
            return Double.NaN; 
        }

        double mean = sum.sum() / totalCount; 
        double variance = sumOfSquares.sum() / totalCount - mean * mean; 
        return Math.sqrt(variance); 
    }
}
