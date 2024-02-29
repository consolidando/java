/*
 * Copyright (c) 2024 joanribalta@elmolidelanoguera.com
 * License: CC BY-NC-ND 4.0 (https://creativecommons.org/licenses/by-nc-nd/4.0/)
 * Blog Consolidando: https://diy.elmolidelanoguera.com/
 */
package com.elmolidelanoguera.stressor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author joanr
 */
public class ServerStatistics
{

    static HttpClient client = HttpClient.newHttpClient();

    static String url = Constants.BASE_URL + "id/statistics";

    static void delete()
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("DELETE", HttpRequest.BodyPublishers.noBody())
                .build();

        try
        {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            System.out.println("DELETE Server Statistics: " + response.statusCode());
        } catch (IOException | InterruptedException ex)
        {
            Logger.getLogger(ServerStatistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void get()
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        try
        {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("\nSERVER DB ACCESS STATISTICS: --------------------------------" );
            if (response.statusCode() == 200)
            {
                ObjectMapper mapper = new ObjectMapper();
                GetIdStatistics idStatistics = mapper.readValue(response.body(), GetIdStatistics.class);

                System.out.println("Total Request: " + idStatistics.requestNumber());
                System.out.println("Maximum Concurrent Request: " + idStatistics.maximumConcurrentRequest());
                System.out.println("Minimum Request Time: " + 
                        String.format("%.2f", idStatistics.minimumRequestTime()/ 1000_000.00) + " ms");
                System.out.println("Maximum Request Time: " +
                     String.format("%.2f", idStatistics.maximumRequestTime()/ 1000_000.00) + " ms");
                System.out.println("Mean: " +
                         String.format("%.2f", idStatistics.mean() / 1000_000.00) + " ms");
                System.out.println("Deviation: " +
                         String.format("%.2f", idStatistics.deviation() / 1000_000.00) + " ms");
            } else
            {
                System.out.println("Error: " + response.statusCode());
            }
        } catch (IOException | InterruptedException ex)
        {
            Logger.getLogger(ServerStatistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
