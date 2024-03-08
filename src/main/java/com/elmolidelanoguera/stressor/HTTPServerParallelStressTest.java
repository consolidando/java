/*
 * Copyright (c) 2024 joanribalta@elmolidelanoguera.com
 * License: CC BY-NC-ND 4.0 (https://creativecommons.org/licenses/by-nc-nd/4.0/)
 * Blog Consolidando: https://diy.elmolidelanoguera.com/
 */
package com.elmolidelanoguera.stressor;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
//v1 import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class HTTPServerParallelStressTest
{

    // settings
    private static int numRequestsPerCycle = 0;
    private static int platformThreadsUsed = 0;
    private static long minimumConcurrentRequest = 0;
    private static long maximumConcurrentRequest = 0;
    private static long minimumTimeBetweenRequestsA = 0;
    private static long minimumTimeBetweenRequestsB = 0;
    private static long minimumTimeBetweenRequestsC = 0;

    //
    private static final AtomicLong concurrentRequests = new AtomicLong(0L);
    private static final AtomicLong maximumConcurrentRequestsAchieve = new AtomicLong(0L);
    private static final AtomicLong responseNumber = new AtomicLong(0);

    // errors & exceptions
    private static final AtomicLong exceptionAfterRetrying = new AtomicLong(0);
    private static final AtomicLong timeoutException = new AtomicLong(0);
    private static final AtomicLong connexionException = new AtomicLong(0);
    private static final AtomicLong otherException = new AtomicLong(0);
    private static final AtomicLong responseError = new AtomicLong(0);

    // statistics
    private static final RealTimeStatistics responseStatistics = new RealTimeStatistics();
    private static final RealTimeStatistics concurrentAStatistics = new RealTimeStatistics();
    private static final RealTimeStatistics concurrentBStatistics = new RealTimeStatistics();
    private static final RealTimeStatistics concurrentCStatistics = new RealTimeStatistics();

    private static PrintStream fileout = null;
    private static String parameter_6 = null;

    //v1 private static final AtomicLong previousRequestTime = new AtomicLong(0);
    private static final ReentrantLock previousRequestTime2Lock = new ReentrantLock();
    private static volatile long timeToSendAllRequests = 0;
    private static long sendRequests = 0;
    private static long previousRequestTime;
    private static long requestNumber;

    private static long startTime;
    private static long totalTime;

    public static void RESULT_OUTPUT(String message)
    {
        System.out.println(message);
        fileWrite(message);
    }

    public static void TRACE_MASTER(String message)
    {
        // System.out.println(message);
    }

    public static void TRACE_MAKE_REQUEST(String message)
    {
        //System.out.println(message);
    }

    private static void setTestParameters(String[] args)
    {
        // default values 
        numRequestsPerCycle = Constants.ids.size();
        platformThreadsUsed = (Constants.PLATFORM_THREADS_USED != 0) ? Constants.PLATFORM_THREADS_USED
                : Runtime.getRuntime().availableProcessors();
        minimumConcurrentRequest = Constants.MINIMUM_CONCURRENT_REQUEST;
        maximumConcurrentRequest = Constants.MAXIMUM_CONCURRENT_REQUEST;
        minimumTimeBetweenRequestsA = Constants.TIME_BETWEEN_REQUESTS_IN_CONCURRENT_REQUEST_A;
        minimumTimeBetweenRequestsB = Constants.TIME_BETWEEN_REQUESTS_IN_CONCURRENT_REQUEST_B;
        minimumTimeBetweenRequestsC = Constants.TIME_BETWEEN_REQUESTS_IN_CONCURRENT_REQUEST_C;
        
        requestNumber = Constants.CYCLE_NUMBER * numRequestsPerCycle;

        // main parameters
        if (args.length > 0)
        {
            if ((args.length == 5) || (args.length == 6))
            {
                try
                {
                    minimumTimeBetweenRequestsA = Long.parseLong(args[0]) * 1000;
                    minimumConcurrentRequest = Long.parseLong(args[1]);
                    minimumTimeBetweenRequestsB = Long.parseLong(args[2]) * 1000;
                    maximumConcurrentRequest = Long.parseLong(args[3]);
                    minimumTimeBetweenRequestsC = Long.parseLong(args[4]) * 1000;

                    if (args.length == 6)
                    {
                        parameter_6 = args[5];
                    }

                } catch (NumberFormatException e)
                {
                    System.out.println("Invalid parameter format. Please provide numeric values.");
                    System.exit(1); // Terminate the program
                }
            } else
            {
                System.out.println("java -jar stressor.jar A A_B B B_C C");
                System.exit(1); // Terminate the program
            }
        }
    }

    private static void pressEnterToContinue()
    {
        //
        System.out.println(commandString());

        System.out.println("Press Enter to continue...");
        try (Scanner scanner = new Scanner(System.in))
        {
            scanner.nextLine();
        }

    }

    private static String commandString()
    {
        StringBuilder commandBuilder = new StringBuilder();
        commandBuilder.append("java -jar stressor.jar ")
                .append(minimumTimeBetweenRequestsA / 1000L).append("\u00B5s ")
                .append(minimumConcurrentRequest).append(" ")
                .append(minimumTimeBetweenRequestsB / 1000L).append("\u00B5s ")
                .append(maximumConcurrentRequest).append(" ")
                .append(minimumTimeBetweenRequestsC / 1000L).append("\u00B5s");

        if (parameter_6 != null)
        {
            commandBuilder.append(" ").append(parameter_6);
        }

        return commandBuilder.toString();
    }

    private static String getFileName(String[] args)
    {
        String fileName = String.format("%d-%d-%d-%d-%d-%s.txt",
                minimumTimeBetweenRequestsA / 1000L,
                minimumConcurrentRequest,
                minimumTimeBetweenRequestsB / 1000L,
                maximumConcurrentRequest,
                minimumTimeBetweenRequestsC / 1000L,
                args[5]);

        return fileName;
    }

    public static void main(String[] args) throws InterruptedException
    {
        configureStandardOutputCharset();
        Logger.getGlobal().setLevel(Level.ALL);

        // Delete previous server statistics
        ServerStatistics.delete();

        // Set test parameters based on command-line arguments
        setTestParameters(args);

        // Prompt the user to press Enter to continue
        pressEnterToContinue();

        // result output by a text file
        if (args.length == 6)
        {
            fileCreate(getFileName(args));
        }

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
            startTime = System.currentTimeMillis();

            // Submit a task to make concurrent requests as virtual thread
            // => this task does not have its own platform thread
            executor.submit(() -> masterTaskPlus(executor, latch));

            // Wait for all requests to complete or timeout
            if (!latch.await(Constants.MAXIMUM_TIME_ALL_THREADS, TimeUnit.MILLISECONDS))
            {
                System.out.println("! - Timeout ..............................");
            }

            // Calculate total time taken for the test
            totalTime = System.currentTimeMillis() - startTime;

            System.out.println("-------> Main ends in " + totalTime + " ms");

            // Print the results of the test
            printResults();

        }

        fileClose();

    }

    private static void masterTaskPlus(ExecutorService executor, CountDownLatch latch)
    {
        try
        {
            masterTask(executor, latch);
        } catch (InterruptedException ex)
        {
            Logger.getLogger(HTTPServerParallelStressTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void masterTask(ExecutorService executor, CountDownLatch latch) throws InterruptedException
    {
//V1        final Semaphore semaphore = new Semaphore(Constants.PLATFORM_THREADS_USED + 1);
        long taskSwitchDelay;
//V1         long startBlockTime;

//V1         startBlockTime = System.nanoTime();
        for (int cycleNumber = 0; cycleNumber < Constants.CYCLE_NUMBER; cycleNumber++)
        {
            for (int cycleRequestNumber = 0; cycleRequestNumber < numRequestsPerCycle; cycleRequestNumber++)
            {
                final int __cycleRequestNumber = cycleRequestNumber;
                final int __requestNumber = cycleNumber * numRequestsPerCycle + cycleRequestNumber + 1;

                //V1                 semaphore.acquire();
                executor.submit(() ->
                {
                    // minimum time between request according to the  current concurrency
                    long __diff = controlTimeBetweenRequestV2();

                    //  
                    long __concurrentRequests = concurrentRequests.incrementAndGet();
                    maximumConcurrentRequestsAchieve.updateAndGet(current -> Math.max(current, __concurrentRequests));

                    // statistics of real time between request  
                    concurrencyStatistics(__concurrentRequests, __diff);

                    //V1                   semaphore.release();
                    makeRequestWith3Retries(__requestNumber, Constants.ids.get(__cycleRequestNumber), latch);
                });

                //V1                taskSwitchDelay = 0;
                // ensures that each block meets the minimum time between requests
                //V1                if ((__requestNumber % (Constants.PLATFORM_THREADS_USED + 1)) == 0)
                //V1                 {
                //V1                     long blockTime = System.nanoTime() - startBlockTime;
                //V1                     taskSwitchDelay = (getDelayBasedOnConcurrency() * Constants.PLATFORM_THREADS_USED) - blockTime;
                //V1                     startBlockTime = System.nanoTime();
                //V1                 }
                //V1                 if (taskSwitchDelay <= 0)
                //V1                 {
                taskSwitchDelay = Constants.MINIMUM_TIME_BETWEEN_REQUEST;
                //V1                 }
                // gives access to new tasks
                taskSwitchAndDelayInNanos(taskSwitchDelay);
            }
        }

        System.out.println("-------> Master ends in "
                + (System.currentTimeMillis() - startTime) + " ms");
    }

//v1     private static long controlTimeBetweenRequestV1()
//v1    {
//v1        long diff;
//v1        long previous;
//v1        previous = previousRequestTime.get();
//v1        diff = (previous != 0) ? (System.nanoTime() - previous) : 0;
//v1        previousRequestTime.set(System.nanoTime());
//v1        return (diff);
//v1    }
    private static long controlTimeBetweenRequestV2()
    {
        long diff = 0;

        previousRequestTime2Lock.lock();
        try
        {
            if (previousRequestTime != 0)
            {
                long delay = getDelayBasedOnConcurrency();
                do
                {
                    diff = System.nanoTime() - previousRequestTime;
                } while (diff < delay);
            }

            previousRequestTime = System.nanoTime();

            sendRequests++;

            if (sendRequests >= requestNumber)
            {
                timeToSendAllRequests = System.currentTimeMillis() - startTime;
            }

        } finally
        {
            previousRequestTime2Lock.unlock();
        }
        return (diff);
    }

    private static long getDelayBasedOnConcurrency()
    {
        long minimumDelayInNs;

        long __concurrentRequests = concurrentRequests.get();

        if (__concurrentRequests > minimumConcurrentRequest)
        {
            if (__concurrentRequests > maximumConcurrentRequest)
            {
                minimumDelayInNs = minimumTimeBetweenRequestsC;
            } else
            {
                minimumDelayInNs = minimumTimeBetweenRequestsB;
            }
        } else
        {
            minimumDelayInNs = minimumTimeBetweenRequestsA;
        }

        return (minimumDelayInNs);
    }

    static void concurrencyStatistics(long concurrentRequests, long requestPeriode)
    {
        if (requestPeriode > 0)
        {
            if (concurrentRequests >= minimumConcurrentRequest)
            {
                if (concurrentRequests > maximumConcurrentRequest)
                {
                    concurrentCStatistics.addValue(requestPeriode);
                } else
                {
                    concurrentBStatistics.addValue(requestPeriode);
                }
            } else
            {
                concurrentAStatistics.addValue(requestPeriode);
            }
        }
    }

    private static void makeRequestWith3Retries(int requestNumber, int id, CountDownLatch latch)
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
                    switch (e)
                    {
                        case SocketTimeoutException s ->
                            timeoutException.incrementAndGet();
                        case ConnectException s ->
                            connexionException.incrementAndGet();
                        default ->
                            otherException.incrementAndGet();
                    }

                    retries++;

                    taskSwitchAndDelayInMillis(Constants.TIME_BETWEEN_RETRIES * retries);

                    TRACE_MAKE_REQUEST("! - Response " + requestNumber + ": Exception: " + e.getMessage() + " - retry: " + retries);
                    //e.printStackTrace();        
                }
            }
            if (retries >= Constants.REQUEST_RETRIES_NUMBER)
            {
                TRACE_MAKE_REQUEST("! - Response " + requestNumber + " MAXIMUM OF RETRIES !!! ");
                exceptionAfterRetrying.incrementAndGet();
            }
        } catch (URISyntaxException e)
        {
            TRACE_MAKE_REQUEST("! - Response " + requestNumber + ": Exception: " + e.getMessage());
        } finally
        {
            latch.countDown();
            concurrentRequests.decrementAndGet();
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
                responseStatistics.addValue(requestTime);

                long localResponseNumber = responseNumber.incrementAndGet();
                String difference = (localResponseNumber == requestNumber) ? "= " : "* ";

                TRACE_MAKE_REQUEST(difference + localResponseNumber
                        + " - Response " + requestNumber + ": OK, in "
                        + String.format("%.2f", (double) requestTime / 1000_000.00)
                        + " ms");
            } else
            {
                responseError.incrementAndGet();
                TRACE_MAKE_REQUEST("! - Response " + requestNumber + ": Error Code: " + responseCode);
            }
        } finally
        {
            if (connection != null)
            {
                connection.disconnect();
            }
        }
    }

    private static void taskSwitchAndDelayInNanos(long nanos)
    {
        try
        {
            Thread.sleep((nanos / 1_000_000), (int) (nanos % 1_000_000));
        } catch (InterruptedException ex)
        {
            Logger.getLogger(HTTPServerParallelStressTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void taskSwitchAndDelayInMillis(long millis)
    {
        try
        {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException ex)
        {
            Logger.getLogger(HTTPServerParallelStressTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static private void printResults()
    {

        RESULT_OUTPUT("\nRESULTS -----------------------------------------------------");

//        
        long requestPerSecond = (long) (requestNumber * 1000L) / totalTime;
        double averageConcurrentRequests = (responseStatistics.calculateMean() * requestNumber) / (totalTime * 1000_000L);

        long requestPerSecondWithoutAllResponses = (long) (requestNumber * 1000L) / timeToSendAllRequests;
        long microsPerRequest = (timeToSendAllRequests * 1000L) / requestNumber;

        RESULT_OUTPUT("*** Without waiting all responses ***");
        RESULT_OUTPUT("Request per second: " + requestPerSecondWithoutAllResponses);
        RESULT_OUTPUT("Microseconds per request: " + microsPerRequest + " \u00B5s/request");

        RESULT_OUTPUT("\n*** Waiting all responses ***");
        RESULT_OUTPUT("Request per second: " + requestPerSecond);
        RESULT_OUTPUT("Maximum concurrent request: " + maximumConcurrentRequestsAchieve);
        RESULT_OUTPUT("Average of concurrent requests: "
                + String.format("%.2f", averageConcurrentRequests));
        RESULT_OUTPUT("Total time of all requests: " + totalTime + " ms");
        //        
        RESULT_OUTPUT("Minimum time response: "
                + String.format("%.2f", (double) (responseStatistics.getMinimumRequestTime()) / 1000_000.00) + " ms");
        RESULT_OUTPUT("Maximum time response: "
                + String.format("%.2f", (double) responseStatistics.getMaximumRequestTime() / 1000_000.00) + " ms");
        RESULT_OUTPUT("Mean: "
                + String.format("%.2f", responseStatistics.calculateMean() / 1000_000.00) + " ms");
        RESULT_OUTPUT("Standard Deviation: "
                + String.format("%.2f", responseStatistics.calculateStandardDeviation() / 1000_000.00) + " ms");
        //
        RESULT_OUTPUT("Requests with timeout exception: " + timeoutException);
        RESULT_OUTPUT("Requests with connexion exception: " + connexionException);
        RESULT_OUTPUT("Requests with other exceptions: " + otherException);
        RESULT_OUTPUT("Requests with exception after retrying: " + exceptionAfterRetrying);
        RESULT_OUTPUT("Requests with error: " + responseError);

        // print server statistics 
        ServerStatistics.get();

        RESULT_OUTPUT("\nEQUIVALENT CALL ---------------------------------------------");
        RESULT_OUTPUT(commandString());

        RESULT_OUTPUT("\nCLIENT SETTINGS ---------------------------------------------");
        RESULT_OUTPUT("Platform threads used: " + platformThreadsUsed);
        RESULT_OUTPUT("A: Until " + minimumConcurrentRequest
                + " concurrent request, new request each "
                + minimumTimeBetweenRequestsA / 1000L + " \u00B5s");
        RESULT_OUTPUT("B: From " + minimumConcurrentRequest + " to " + maximumConcurrentRequest
                + " concurrent requests, new requests each "
                + minimumTimeBetweenRequestsB / 1000L + " \u00B5s");
        RESULT_OUTPUT("C: After " + maximumConcurrentRequest
                + " concurrent requests, new requests each "
                + minimumTimeBetweenRequestsC / 1000L + " \u00B5s");
        RESULT_OUTPUT("Total Request Number: " + requestNumber);

        // real request in client time
        RESULT_OUTPUT("\nREAL TIME BETWEEN REQUEST ACCORDING TO CONCURRENCY ----------");
        RESULT_OUTPUT("*** Concurrence level A ***");
        RESULT_OUTPUT("Request Number: " + concurrentAStatistics.getCount());
        RESULT_OUTPUT("Minimum time between request: "
                + String.format("%.2f", (double) (concurrentAStatistics.getMinimumRequestTime()) / 1000.00) + " \u00B5s");
        RESULT_OUTPUT("Maximum time between request: "
                + String.format("%.2f", (double) concurrentAStatistics.getMaximumRequestTime() / 1000.00) + " \u00B5s");
        RESULT_OUTPUT("Mean: "
                + String.format("%.2f", concurrentAStatistics.calculateMean() / 1000.00) + " \u00B5s");
        RESULT_OUTPUT("Standard Deviation: "
                + String.format("%.2f", concurrentAStatistics.calculateStandardDeviation() / 1000.00) + " \u00B5s");

        RESULT_OUTPUT("*** Concurrence level B ***");
        RESULT_OUTPUT("Request Number: " + concurrentBStatistics.getCount());
        RESULT_OUTPUT("Minimum time between request: "
                + String.format("%.2f", (double) (concurrentBStatistics.getMinimumRequestTime()) / 1000.00) + " \u00B5s");
        RESULT_OUTPUT("Maximum time between request: "
                + String.format("%.2f", (double) concurrentBStatistics.getMaximumRequestTime() / 1000.00) + " \u00B5s");
        RESULT_OUTPUT("Mean: "
                + String.format("%.2f", concurrentBStatistics.calculateMean() / 1000.00) + " \u00B5s");
        RESULT_OUTPUT("Standard Deviation: "
                + String.format("%.2f", concurrentBStatistics.calculateStandardDeviation() / 1000.00) + " \u00B5s");

        RESULT_OUTPUT("*** Concurrence level C ***");
        RESULT_OUTPUT("Request Number: " + concurrentCStatistics.getCount());
        RESULT_OUTPUT("Minimum time between request: "
                + String.format("%.2f", (double) (concurrentCStatistics.getMinimumRequestTime()) / 1000.00) + " \u00B5s");
        RESULT_OUTPUT("Maximum time between request: "
                + String.format("%.2f", (double) concurrentCStatistics.getMaximumRequestTime() / 1000.00) + " \u00B5s");
        RESULT_OUTPUT("Mean: "
                + String.format("%.2f", concurrentCStatistics.calculateMean() / 1000.00) + " \u00B5s");
        RESULT_OUTPUT("Standard Deviation: "
                + String.format("%.2f", concurrentCStatistics.calculateStandardDeviation() / 1000.00) + " \u00B5s");

        RESULT_OUTPUT("\nThis is the end. --------------------------------------------\n ");

    }

    static private void configureStandardOutputCharset()
    {
        String aux = System.getProperty("file.encoding");
        if (!"UTF-8".equals(aux))
        {
            try
            {
                System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
            } catch (UnsupportedEncodingException e)
            {
                System.err.println("UTF-8 encoding is not supported. Unable to change output stream encoding.");
            }
        }
    }

    static void fileCreate(String fileName)
    {
        try
        {
            fileout = new PrintStream(new FileOutputStream(fileName, true));
        } catch (FileNotFoundException ex)
        {
            Logger.getLogger(HTTPServerParallelStressTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void fileWrite(String message)
    {
        if (fileout != null)
        {
            fileout.println(message);
        }
    }

    static void fileClose()
    {
        try
        {
            if (fileout != null)
            {
                fileout.close();
            }
        } catch (Exception ex)
        {
            Logger.getLogger(HTTPServerParallelStressTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
