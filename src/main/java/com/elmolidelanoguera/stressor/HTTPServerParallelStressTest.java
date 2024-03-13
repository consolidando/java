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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.locks.ReentrantLock;

public class HTTPServerParallelStressTest
{

    enum ConcurrencyLevel
    {
        A,
        B,
        C
    }

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

    // statistics in request and response
    // Times are saved as microseconds because using nanoseconds causes overflow 
    // in the standard deviation calculation.
    private static final RealTimeStatistics timeBetweenRequestLevelA = new RealTimeStatistics();
    private static final RealTimeStatistics timeBetweenRequestLevelB = new RealTimeStatistics();
    private static final RealTimeStatistics timeBetweenRequestLevelC = new RealTimeStatistics();
    private static final ResponseInfo responseInfoLevelA = new ResponseInfo();
    private static final ResponseInfo responseInfoLevelB = new ResponseInfo();
    private static final ResponseInfo responseInfoLevelC = new ResponseInfo();

    private static PrintStream fileout = null;
    private static String fileoutNameSuffix = null;

    // 
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
                        fileoutNameSuffix = args[5];
                    }

                } catch (NumberFormatException e)
                {
                    System.out.println("Invalid parameter format. Please provide numeric values.");
                    System.exit(1); // Terminate the program
                }
            } else
            {
                System.out.println("java -jar stressor.jar A AtoB B BtoC C");
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

        if (fileoutNameSuffix != null)
        {
            commandBuilder.append(" ").append(fileoutNameSuffix);
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
            executor.submit((Callable<Void>) () ->
            {
                masterTask(executor, latch);
                return null;
            });
            

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

    private static void masterTask(ExecutorService executor, CountDownLatch latch) throws InterruptedException
    {
        for (int cycleNumber = 0; cycleNumber < Constants.CYCLE_NUMBER; cycleNumber++)
        {
            for (int cycleRequestNumber = 0; cycleRequestNumber < numRequestsPerCycle; cycleRequestNumber++)
            {
                final int __cycleRequestNumber = cycleRequestNumber;
                final int __requestNumber = cycleNumber * numRequestsPerCycle + cycleRequestNumber + 1;

                executor.submit(() ->
                {
                    // minimum time between request according to the  current concurrency
                    long __diff = controlTimeBetweenRequest();

                    //  
                    long __concurrentRequests = concurrentRequests.incrementAndGet();
                    maximumConcurrentRequestsAchieve.updateAndGet(current -> Math.max(current, __concurrentRequests));

                    // statistics of real time between request  
                    ConcurrencyLevel concurrencyLevel = getConcurrencyLevel(__concurrentRequests);

                    RealTimeStatistics timeBetweenRequest
                            = timeBetweenRequestOnConcurrencyLevel(concurrencyLevel);

                    if (__diff != 0)
                    {
                        timeBetweenRequest.addValue(__diff / 1000L);
                    }

                    makeRequestWith3Retries(concurrencyLevel,
                            __requestNumber, Constants.ids.get(__cycleRequestNumber), latch);
                });

                // gives access to new tasks
                taskSwitchAndDelayInNanos(Constants.MINIMUM_TIME_BETWEEN_REQUEST);
            }
        }

        System.out.println("-------> Master ends in "
                + (System.currentTimeMillis() - startTime) + " ms");
    }

    private static long controlTimeBetweenRequest()
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

    static ConcurrencyLevel getConcurrencyLevel(long concurrentRequests)
    {
        ConcurrencyLevel concurrencyLevel = ConcurrencyLevel.A;

        if (concurrentRequests >= minimumConcurrentRequest)
        {
            if (concurrentRequests > maximumConcurrentRequest)
            {
                concurrencyLevel = ConcurrencyLevel.C;
            } else
            {
                concurrencyLevel = ConcurrencyLevel.B;
            }
        }

        return (concurrencyLevel);
    }

    static RealTimeStatistics timeBetweenRequestOnConcurrencyLevel(ConcurrencyLevel concurrencyLevel)
    {
        return switch (concurrencyLevel)
        {
            case B ->
                timeBetweenRequestLevelB;
            case C ->
                timeBetweenRequestLevelC;
            default ->
                timeBetweenRequestLevelA;
        };
    }

    static ResponseInfo responseInfoOnConcurrencyLevel(ConcurrencyLevel concurrencyLevel)
    {
        return switch (concurrencyLevel)
        {
            case B ->
                responseInfoLevelB;
            case C ->
                responseInfoLevelC;
            default ->
                responseInfoLevelA;
        };

    }

    private static void makeRequestWith3Retries(ConcurrencyLevel concurrencyLevel,
            int requestNumber, int id, CountDownLatch latch)
    {
        try
        {
            ResponseInfo responseInfo
                    = responseInfoOnConcurrencyLevel(concurrencyLevel);
            int retries = 0;
            while (retries < Constants.REQUEST_RETRIES_NUMBER)
            {
                try
                {
                    makeRequest(responseInfo, requestNumber, id);
                    break;
                } catch (IOException e)
                {
                    switch (e)
                    {
                        case SocketTimeoutException s ->
                            responseInfo.incrementTimeoutExceptions();
                        case ConnectException s ->
                            responseInfo.incrementConnexionExceptions();
                        default ->
                            responseInfo.incrementOtherExceptions();
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
                responseInfo.incrementExceptionsAfterRetrying();
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

    private static void makeRequest(ResponseInfo responseInfo, int requestNumber, int id)
            throws URISyntaxException, MalformedURLException, IOException
    {
        HttpURLConnection connection = null;
        try
        {
            URL url = new URI(Constants.BASE_URL + id).toURL();

            connection = (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(Constants.MAXIMUM_TIME_PER_REQUEST_CONNECTION);
            connection.setReadTimeout(Constants.MAXIMUM_TIME_PER_REQUEST_READ);

            long startGet = System.nanoTime();

            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK)
            {
                long endGet = System.nanoTime();
                long reponseTime = endGet - startGet;
                responseInfo.responseTime.addValue(reponseTime / 1000L);

                long localResponseNumber = responseNumber.incrementAndGet();
                String difference = (localResponseNumber == requestNumber) ? "= " : "* ";

                TRACE_MAKE_REQUEST(difference + localResponseNumber
                        + " - Response " + requestNumber + ": OK, in "
                        + String.format("%.2f", (double) reponseTime / 1000_000.00)
                        + " ms");
            } else
            {
                responseInfo.incrementErrors();
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
        double responseTimeMean = (responseInfoLevelA.responseTime.calculateMean()
                + responseInfoLevelB.responseTime.calculateMean()
                + responseInfoLevelC.responseTime.calculateMean()) / 3;
        double averageConcurrentRequests = (responseTimeMean * requestNumber) / (totalTime * 1000L);

        long requestPerSecondWithoutAllResponses = (long) (requestNumber * 1000L) / timeToSendAllRequests;
        long microsPerRequest = (timeToSendAllRequests * 1000L) / requestNumber;

        RESULT_OUTPUT("*** Without waiting all responses ***");
        RESULT_OUTPUT("Requests per second: " + requestPerSecondWithoutAllResponses);
        RESULT_OUTPUT("Microseconds per request: " + microsPerRequest + " \u00B5s/request");

        RESULT_OUTPUT("\n*** Waiting all responses ***");
        RESULT_OUTPUT("Requests per second: " + requestPerSecond);
        RESULT_OUTPUT("Maximum concurrent requests: " + maximumConcurrentRequestsAchieve);
        RESULT_OUTPUT("Average of concurrent requests: "
                + String.format("%.2f", averageConcurrentRequests));
        RESULT_OUTPUT("Total time of all requests: " + totalTime + " ms");

        RESULT_OUTPUT("\nRESPONSE ACCORDING TO CONCURRENCY ---------------------------");

        for (ConcurrencyLevel concurrencyLevel : ConcurrencyLevel.values())
        {
            ResponseInfo responseInfo = responseInfoOnConcurrencyLevel(concurrencyLevel);
            String title
                    = "************************* Concurrency level %s ***************".formatted(concurrencyLevel);
            RESULT_OUTPUT(title);
            //        
            RESULT_OUTPUT("Request Number: " + responseInfo.responseTime.getCount());
            RESULT_OUTPUT("Minimum time response: "
                    + String.format("%.2f", (double) (responseInfo.responseTime.getMinimum()) / 1000.00) + " ms");
            RESULT_OUTPUT("Maximum time response: "
                    + String.format("%.2f", (double) responseInfo.responseTime.getMaximum() / 1000.00) + " ms");
            RESULT_OUTPUT("Mean: "
                    + String.format("%.2f", responseInfo.responseTime.calculateMean() / 1000.00) + " ms");
            RESULT_OUTPUT("Standard Deviation: "
                    + String.format("%.2f", responseInfo.responseTime.calculateStandardDeviation() / 1000.00) + " ms");
            //
            RESULT_OUTPUT("Requests with timeout exceptions: " + responseInfo.getTimeoutExceptions());
            RESULT_OUTPUT("Requests with connexion exceptions: " + responseInfo.getConnexionExceptions());
            RESULT_OUTPUT("Requests with other exceptions: " + responseInfo.getOtherExceptions());
            RESULT_OUTPUT("Requests with exceptions after retrying: " + responseInfo.getExceptionsAfterRetrying());
            RESULT_OUTPUT("Requests with error: " + responseInfo.getErrors());

        }

        // print server statistics ---------------------------------------------
        RESULT_OUTPUT("\nSERVER DB ACCESS STATISTICS: --------------------------------");
        GetIdStatistics getIdStatistics = ServerStatistics.get();

        RESULT_OUTPUT("Total Request: " + getIdStatistics.requestNumber());
        RESULT_OUTPUT("Maximum Concurrent Requests: " + getIdStatistics.maximumConcurrentRequest());
        RESULT_OUTPUT("Minimum Request Time: "
                + String.format("%.2f", getIdStatistics.minimumRequestTime() / 1000_000.00) + " ms");
        RESULT_OUTPUT("Maximum Request Time: "
                + String.format("%.2f", getIdStatistics.maximumRequestTime() / 1000_000.00) + " ms");
        RESULT_OUTPUT("Mean: "
                + String.format("%.2f", getIdStatistics.mean() / 1000_000.00) + " ms");
        RESULT_OUTPUT("Deviation: "
                + String.format("%.2f", getIdStatistics.deviation() / 1000_000.00) + " ms");

        // ---------------------------------------------------------------------
        RESULT_OUTPUT("\nEQUIVALENT CALL ---------------------------------------------");
        RESULT_OUTPUT(commandString());

        RESULT_OUTPUT("\nCLIENT SETTINGS ---------------------------------------------");
        RESULT_OUTPUT("Platform threads used: " + platformThreadsUsed);
        RESULT_OUTPUT("A: Until " + minimumConcurrentRequest
                + " concurrent requests, new request each "
                + minimumTimeBetweenRequestsA / 1000L + " \u00B5s");
        RESULT_OUTPUT("B: From " + minimumConcurrentRequest + " to " + maximumConcurrentRequest
                + " concurrent requests, new requests each "
                + minimumTimeBetweenRequestsB / 1000L + " \u00B5s");
        RESULT_OUTPUT("C: After " + maximumConcurrentRequest
                + " concurrent requests, new requests each "
                + minimumTimeBetweenRequestsC / 1000L + " \u00B5s");
        RESULT_OUTPUT("Total Request Number: " + requestNumber);

        //  --------------------------------------------------------------------
        RESULT_OUTPUT("\nTIME BETWEEN REQUEST ACCORDING TO CONCURRENCY ---------------");
        for (ConcurrencyLevel concurrencyLevel : ConcurrencyLevel.values())
        {
            RealTimeStatistics timeBetweenRequest = timeBetweenRequestOnConcurrencyLevel(concurrencyLevel);
            String title
                    = "************************* Concurrency level %s ***************".formatted(concurrencyLevel);
            RESULT_OUTPUT(title);
            RESULT_OUTPUT("Request Number: " + timeBetweenRequest.getCount());
            RESULT_OUTPUT("Minimum time between request: "
                    + String.format("%.2f", (double) (timeBetweenRequest.getMinimum())) + " \u00B5s");
            RESULT_OUTPUT("Maximum time between request: "
                    + String.format("%.2f", (double) timeBetweenRequest.getMaximum()) + " \u00B5s");
            RESULT_OUTPUT("Mean: "
                    + String.format("%.2f", timeBetweenRequest.calculateMean()) + " \u00B5s");
            RESULT_OUTPUT("Standard Deviation: "
                    + String.format("%.2f", timeBetweenRequest.calculateStandardDeviation()) + " \u00B5s");
        }

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
