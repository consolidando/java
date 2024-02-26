/*
 * Copyright (c) 2024 joanribalta@elmolidelanoguera.com
 * License: CC BY-NC-ND 4.0 (https://creativecommons.org/licenses/by-nc-nd/4.0/)
 * Blog Consolidando: https://diy.elmolidelanoguera.com/
 */
package com.elmolidelanoguera.stressor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class HTTPServerParallelStressTest
{

    private static int numRequestsPerCycle = 0;
    private static int platformThreadsUsed = 0;
    private static long minimumConcurrentRequest = 0;
    private static long maximumConcurrentRequest = 0;
    private static long minimumTimeBetweenRequestsInNs = 0;
    //
    private static final AtomicLong concurrentRequest = new AtomicLong(0L);
    private static final AtomicLong maximumConcurrentRequestAchieve = new AtomicLong(0L);
    private static final AtomicLong exception = new AtomicLong(0);
    //private static final AtomicLong noIOexception = new AtomicLong(0);
    private static final AtomicLong error = new AtomicLong(0);
    private static final AtomicLong responseNumber = new AtomicLong(0);
    private static final AtomicLong exceptionRequestRetries = new AtomicLong(0);
    private static long previousRequestTimeInNs = 0;
    private static final RealTimeStatistics realTimeStatistics = new RealTimeStatistics();

    public static void main(String[] args) throws InterruptedException
    {
        // Delete previous server statistics
        ServerStatistics.delete();

        // Set test parameters based on command-line arguments
        setTestParameters(args);

        // Prompt the user to press Enter to continue
        pressEnterToContinue();

        // Set the maximum pool size for virtual threads
        System.setProperty("jdk.virtualThreadScheduler.parallelism",
                Integer.toString(platformThreadsUsed));
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize",
                Integer.toString(platformThreadsUsed));

        // Create an executor service with virtual threads
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor())
        {
            // Initialize a latch to wait for completion of all tasks
            CountDownLatch latch = new CountDownLatch(numRequestsPerCycle * Constants.CYCLE_NUMBER);

            // Record the start time of the test
            long startTime = System.currentTimeMillis();

            // Submit a task to make concurrent requests as virtual thread
            // => this task does not have its own platform thread
            executor.submit(() -> concurrentRequests(executor, latch));

            // Wait for all requests to complete or timeout
            if (!latch.await(Constants.MAXIMUM_TIME_ALL_THREADS, TimeUnit.MILLISECONDS))
            {
                System.out.println("! Timeout");
            }

            // Calculate total time taken for the test
            long totalTime = System.currentTimeMillis() - startTime;
            // Print the results of the test
            printResults(totalTime);

        }
    }

    private static void setTestParameters(String[] args)
    {
        //
        numRequestsPerCycle = Constants.ids.size();
        platformThreadsUsed = (Constants.PLATFORM_THREADS_USED != 0) ? Constants.PLATFORM_THREADS_USED
                : Runtime.getRuntime().availableProcessors();

        minimumConcurrentRequest = Constants.MINIMUM_CONCURRENT_REQUEST;
        maximumConcurrentRequest = Constants.MAXIMUM_CONCURRENT_REQUEST;
        minimumTimeBetweenRequestsInNs = Constants.DEFAULT_TIME_BETWEEN_REQUESTS_IN_MICROSECONDS * 1000;

        if (args.length > 0)
        {
            if (args.length == 3)
            {
                try
                {
                    minimumConcurrentRequest = Long.parseLong(args[0]);
                    minimumTimeBetweenRequestsInNs = Long.parseLong(args[1]) * 1000;
                    maximumConcurrentRequest = Long.parseLong(args[2]);
                } catch (NumberFormatException e)
                {
                    System.out.println("Invalid parameter format. Please provide numeric values.");
                    System.exit(1); // Terminate the program
                }
            } else
            {
                System.out.println("java -jar stressor.jar minimumConcurrentRequest delayBetweenRequestAfterThat maximumConcurrentRequest");
                System.exit(1); // Terminate the program
            }
        }
    }

    private static void virtualThreadSwitchAndDelay()
    {
        try
        {
            if (concurrentRequest.get() > minimumConcurrentRequest)
            {
                if (concurrentRequest.get() > maximumConcurrentRequest)
                {
                    Thread.sleep(Constants.TIME_BETWEEN_REQUESTS_AFTER_MAXIMUM_CONCURRENT_REQUEST);
                } else
                {
                    long requestTimeInNs = System.nanoTime();
                    if ((requestTimeInNs - previousRequestTimeInNs) < minimumTimeBetweenRequestsInNs)
                    {
                        long nanos = (minimumTimeBetweenRequestsInNs - (requestTimeInNs - previousRequestTimeInNs));

                        Thread.sleep((nanos / 1_000_000), (int) (nanos % 1_000_000));
                    }
                }
            } else
            {
                Thread.sleep(0, Constants.TIME_BETWEEN_REQUESTS_BEFORE_MINIMUM_CONCURRENT_REQUEST_IN_NS);
            }
            previousRequestTimeInNs = System.nanoTime();

        } catch (InterruptedException e)
        {
        }

    }

    public static void pressEnterToContinue()
    {
        //
        System.out.println("java -jar stressor.jar "
                + minimumConcurrentRequest + " " + minimumTimeBetweenRequestsInNs / 1000L
                + " " + maximumConcurrentRequest);

        System.out.println("Press Enter to continue...");
        try(Scanner scanner = new Scanner(System.in))
        {
            scanner.nextLine();
        }
   
    }

    /**
     * Makes concurrent requests using virtual threads.
     *
     * @param executor The executor service for running tasks
     * @param latch The count down latch to synchronize threads
     */
    public static void concurrentRequests(ExecutorService executor, CountDownLatch latch)
    {
        for (int cycleNumber = 0; cycleNumber < Constants.CYCLE_NUMBER; cycleNumber++)
        {
            for (int cycleRequestNumber = 0; cycleRequestNumber < numRequestsPerCycle; cycleRequestNumber++)
            {
                final int threadCycleRequestNumber = cycleRequestNumber;
                final int threadRequestNumber = cycleNumber * numRequestsPerCycle + cycleRequestNumber + 1;

                // Introduce configurable delay between requests ---------------
                virtualThreadSwitchAndDelay();

                // asynchronous request ----------------------------------------               
                long concurrent = concurrentRequest.incrementAndGet();
                maximumConcurrentRequestAchieve.updateAndGet(current -> Math.max(current, concurrent));
                System.out.println("Request: " + threadRequestNumber
                        + " - Concurrent requests: " + concurrent);

                executor.submit(
                        () -> makeRequestWith3Retry(threadRequestNumber,
                                Constants.ids.get(threadCycleRequestNumber), latch)
                );
            }
        }

    }

    private static void makeRequestWith3Retry(int requestNumber, int id, CountDownLatch latch)
    {
        try
        {
            int retries = 0;
            while (retries < Constants.REQUEST_RETRIES_NUMBER)
            {
                try
                {                    
                    makeRequest(requestNumber, id);
                    break;
                } catch (IOException e)
                {
                    exceptionRequestRetries.incrementAndGet();
                    retries++;
                    try
                    {
                        TimeUnit.MILLISECONDS.sleep(Constants.TIME_BETWEEN_RETRIES*retries);
                    } catch (InterruptedException ignored)
                    {
                    }

                    System.out.println("! - Response " + requestNumber + ": Exception: " + e.getMessage() + " - retry: " + retries);
                    //e.printStackTrace();        
                }
            }
            if (retries >= Constants.REQUEST_RETRIES_NUMBER)
            {
                System.out.println("! - Response " + requestNumber + " MAXIMUM OF RETRIES !!! ");
                exception.getAndIncrement();
            }
        } catch (URISyntaxException e)
        {
            System.out.println("! - Response " + requestNumber + ": Exception: " + e.getMessage());
        } finally
        {
            latch.countDown();
            concurrentRequest.getAndDecrement();
        }
    }

    private static void makeRequest(int requestNumber, int id) throws URISyntaxException, MalformedURLException, IOException
    {
        HttpURLConnection connection = null;
        try
        {
            URL url = new URI(Constants.BASE_URL + id).toURL();

            connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(Constants.MAXIMUM_TIME_PER_REQUEST_CONNECTION);
            connection.setReadTimeout(Constants.MAXIMUM_TIME_PER_REQUEST_READ);

            long startTime = System.nanoTime();

            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                long endTime = System.nanoTime();
                long requestTime = endTime - startTime;
                realTimeStatistics.addValue(requestTime);

                long localResponseNumber = responseNumber.incrementAndGet();
                String difference = (localResponseNumber == requestNumber) ? "= " : "* ";

                System.out.println(difference + localResponseNumber
                        + " - Response " + requestNumber + ": OK, in "
                        + String.format("%.2f", (double) requestTime / 1000_000.00)
                        + " ms");
            } else
            {
                error.incrementAndGet();
                System.out.println("! - Response " + requestNumber + ": Error Code: " + responseCode);
            }
        } finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

    static private void printResults(long totalTime)
    {

        System.out.println("\nRESULTS -----------------------------------------------------");

//
        long requestNumber = Constants.CYCLE_NUMBER * numRequestsPerCycle;
        long requestPerSecond = (long) (Constants.CYCLE_NUMBER * numRequestsPerCycle * 1000L) / totalTime;
        double averageConcurrentRequests = (realTimeStatistics.calculateMean() * requestNumber) / (totalTime * 1000_000L);

        System.out.println("Request per second: " + requestPerSecond);
        System.out.println("Maximum concurrent request: " + maximumConcurrentRequestAchieve);
        System.out.println("Average of concurrent requests: "
                + String.format("%.2f", averageConcurrentRequests));
        System.out.println("Total time of all requests: " + totalTime + " ms");
        //        
        System.out.println("Minimum time requests: "
                + String.format("%.2f", (double) (realTimeStatistics.getMinimumRequestTime()) / 1000_000.00) + " ms");
        System.out.println("Maximum time requests: "
                + String.format("%.2f", (double) realTimeStatistics.getMaximumRequestTime() / 1000_000.00) + " ms");
        System.out.println("Mean: "
                + String.format("%.2f", realTimeStatistics.calculateMean() / 1000_000.00) + " ms");
        System.out.println("Standard Deviation: "
                + String.format("%.2f", realTimeStatistics.calculateStandardDeviation() / 1000_000.00) + " ms");
        //
        System.out.println("Requests with exception retries: " + exceptionRequestRetries);
        System.out.println("Requests with exception after retrying: " + exception);
        //System.out.println("Requests with no IO exception: " + noIOexception);
        System.out.println("Requests with error: " + error);

        System.out.println("\nCLIENT SETTINGS ---------------------------------------------");
        System.out.println("Platform threads used: " + platformThreadsUsed);
        System.out.println("Until " + minimumConcurrentRequest
                + " concurrent request, new request each "
                + Constants.TIME_BETWEEN_REQUESTS_BEFORE_MINIMUM_CONCURRENT_REQUEST_IN_NS + " ns.");
        System.out.println("From " + minimumConcurrentRequest + " to " + maximumConcurrentRequest
                + " concurrent requests, new requests each "
                + minimumTimeBetweenRequestsInNs / 1000L + " µs");
        System.out.println("After " + maximumConcurrentRequest
                + " concurrent requests, new requests each "
                + Constants.TIME_BETWEEN_REQUESTS_AFTER_MAXIMUM_CONCURRENT_REQUEST + " ms");
        System.out.println("Total Request Number: " + requestNumber);

        // print server statistics 
        ServerStatistics.get();

        System.out.println("\nEQUIVALENT CALL ---------------------------------------------");
        System.out.println("java -jar stressor.jar "
                + minimumConcurrentRequest + " " + minimumTimeBetweenRequestsInNs / 1000L
                + " " + maximumConcurrentRequest);

        System.out.println("\nThis is the end. --------------------------------------------\n ");
    }

}
