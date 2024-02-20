/*
 * Copyright (c) 2024 joanribalta@elmolidelanoguera.com
 * License: CC BY-NC-ND 4.0 (https://creativecommons.org/licenses/by-nc-nd/4.0/)
 * Blog Consolidando: https://diy.elmolidelanoguera.com/
 */

package com.elmolidelanoguera.stressor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HTTPServerParallelStressTest
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
    static final int PLATFORM_THREAD_USED = 6; // 0 => number of cores
    static final long DEFAULT_MINIMUM_TIME_BETWEEN_REQUESTS_IN_MICROSECONDS = 2_000; // .µs
    static final long MAXIMUM_TIME_ALL_THREADS = 60_000; // .ms

    private static final AtomicLong minimum = new AtomicLong(1000);
    private static final AtomicLong maximum = new AtomicLong(0);
    private static final AtomicLong exception = new AtomicLong(0);
    private static final AtomicLong error = new AtomicLong(0);
    private static final AtomicLong responseNumber = new AtomicLong(0);
    private static long delayedRequests = 0;
    private static long previousRequestTimeInNs = 0;
    private static final RealTimeStatistics realTimeStatistics = new RealTimeStatistics();

    
    //
    public static void main(String[] args) throws InterruptedException
    {
        int numRequestsPerCycle = ids.size();
        int platformThreadsUsed = (PLATFORM_THREAD_USED != 0) ? PLATFORM_THREAD_USED  
                : Runtime.getRuntime().availableProcessors();
        
        long minimumTimeBetweenRequestsInNs = (args.length > 0 ? 
                Long.parseLong(args[0]) : DEFAULT_MINIMUM_TIME_BETWEEN_REQUESTS_IN_MICROSECONDS)*1000;

        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", 
                Integer.toString(platformThreadsUsed));

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch latch = new CountDownLatch(numRequestsPerCycle * CYCLE_NUMBER);

        long startTime = System.currentTimeMillis();

        for (int cycleNumber = 0; cycleNumber < CYCLE_NUMBER; cycleNumber++)
        {
            for (int cycleRequestNumber = 0; cycleRequestNumber < numRequestsPerCycle; cycleRequestNumber++)
            {
                final int finalCycleRequestNumber = cycleRequestNumber;
                final int counter = cycleNumber * numRequestsPerCycle + cycleRequestNumber + 1;

                long requestTimeInNs = System.nanoTime();
                if ((requestTimeInNs - previousRequestTimeInNs) < minimumTimeBetweenRequestsInNs)
                {
                    delayedRequests++;
                    long nanos = (minimumTimeBetweenRequestsInNs - (requestTimeInNs - previousRequestTimeInNs));
                    
                    Thread.sleep((nanos / 1_000_000), (int)(nanos%1_000_000));
                }
                previousRequestTimeInNs = System.nanoTime();

                executor.submit(() -> makeRequest(counter, ids.get(finalCycleRequestNumber), latch));
            }
        }

        // Wait for all requests to complete
        if (latch.await(MAXIMUM_TIME_ALL_THREADS, TimeUnit.MILLISECONDS) != true)
        {
            System.out.println("Timeout");
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("\nRESULTS --------------------------------------------");
        
//
        long requestNumber = CYCLE_NUMBER * numRequestsPerCycle;
        long requestPerSecond = (long)(CYCLE_NUMBER * numRequestsPerCycle*1000L) / totalTime;
        double averageConcurrentRequests = (realTimeStatistics.calculateMean() * requestNumber)/ totalTime;        
        
        System.out.println("Request per second: " + requestPerSecond);        
        System.out.println("Average of concurrent requests: " + averageConcurrentRequests);
        System.out.println("Total time of all requests: " + totalTime + " ms");
        //
        System.out.println("Minimum time requests: " + minimum + " ms");
        System.out.println("Maximum time requests: " + maximum + " ms");
        System.out.println("Mean: " + 
                String.format("%.2f", realTimeStatistics.calculateMean()) + " ms");        
        System.out.println("Standard Deviation: " + 
                String.format("%.2f",realTimeStatistics.calculateStandardDeviation()) + " ms");
        //
        System.out.println("Delayed Requests: " + delayedRequests);
        System.out.println("Requests with exception: " + exception);
        System.out.println("Requests with error: " + error);

        System.out.println("\nCLIENT SETTINGS -------------------------------------------");
        System.out.println("Platform threads used: " + platformThreadsUsed);
        System.out.println("Minimum time between request: " + minimumTimeBetweenRequestsInNs/1000 + " µs");
        System.out.println("Request Number: " + requestNumber);

        executor.shutdown();
    }

    private static void makeRequest(int requestNumber, int id, CountDownLatch latch) 
    {
        HttpURLConnection connection = null;
        try
        {
            URL url = new URI(BASE_URL+id).toURL();

            connection = (HttpURLConnection) url.openConnection();

            long startTime = System.currentTimeMillis();

            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                long endTime = System.currentTimeMillis();
                long requestTime = endTime - startTime;
                minimum.updateAndGet(current -> Math.min(current, requestTime));
                maximum.updateAndGet(current -> Math.max(current, requestTime));
                realTimeStatistics.addValue(requestTime);

                long localResponseNumber = responseNumber.incrementAndGet();
                String difference = (localResponseNumber == requestNumber) ? "= " : "* ";

                System.out.println(difference + localResponseNumber
                        + " - Request " + requestNumber + ": OK, in " + requestTime + "ms");
            } else
            {
                error.incrementAndGet();
                System.out.println("Request " + requestNumber + ": Error - Response Code: " + responseCode);
            }

            connection.disconnect();
        } catch (IOException | URISyntaxException e)
        {
            exception.getAndIncrement();
            System.out.println("Request " + requestNumber + ": Error - Exception: " + e.getMessage());

        } finally
        {
            if (connection != null) connection.disconnect();
            latch.countDown();
        }
    }
}
