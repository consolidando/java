/*
 * Copyright (c) 2024 joanribalta@elmolidelanoguera.com
 * License: CC BY-NC-ND 4.0 (https://creativecommons.org/licenses/by-nc-nd/4.0/)
 * Blog Consolidando: https://diy.elmolidelanoguera.com/
 */
package com.elmolidelanoguera.stressor;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @author joanr
 */
public class Constants
{
    static String BASE_URL = "http://localhost:8080/apis/characters/";

    static List<Integer> ids = Arrays.asList(
            2, 14, 21, 18, 1, 27, 53, 3, 5, 77,
            78, 79, 85, 103, 164, 177, 215, 232, 7, 274,
            294, 298, 15, 299, 329, 22, 330, 39, 339, 19,
            389, 424, 425, 381, 83, 427, 82, 430, 431, 84,
            433, 86, 663, 113, 118, 119, 152, 209, 242, 290,
            295, 349, 359, 405, 428, 426, 429, 432, 434, 4
    );

    static final int CYCLE_NUMBER = 100;
    static final int PLATFORM_THREADS_USED = 7; // 0 => equal to number of cores
    
    //
    static final long MINIMUM_CONCURRENT_REQUEST = 100;
    static final long MAXIMUM_CONCURRENT_REQUEST = 1000;
    
    // time between request ----------------------------------------------------
    static final int  TIME_BETWEEN_REQUESTS_BEFORE_MINIMUM_CONCURRENT_REQUEST_IN_NS = 1000; // ns
    static final long DEFAULT_TIME_BETWEEN_REQUESTS_IN_MICROSECONDS = 2_000; // .µs
    static final int  TIME_BETWEEN_REQUESTS_AFTER_MAXIMUM_CONCURRENT_REQUEST = 1000; // .ms
    
    // request retries ---------------------------------------------------------
    static final int REQUEST_RETRIES_NUMBER = 3;                                    
    static final int TIME_BETWEEN_RETRIES = 6;  // .ms
    
    //
    static final int  MAXIMUM_TIME_PER_REQUEST_CONNECTION = 200; // .ms
    static final int  MAXIMUM_TIME_PER_REQUEST_READ = 10_000; // .ms
    static final long MAXIMUM_TIME_ALL_THREADS = 60_000; // .ms

}
