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
            //System.out.println("DELETE Server Statistics: " + response.statusCode());
        } catch (IOException | InterruptedException ex)
        {
            Logger.getLogger(ServerStatistics.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static GetIdStatistics get()
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        GetIdStatistics getIdStatistics = null;
        try
        {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200)
            {
                ObjectMapper mapper = new ObjectMapper();
                getIdStatistics = mapper.readValue(response.body(), GetIdStatistics.class);

            } else
            {
                System.out.println("Error: " + response.statusCode());
            }
        } catch (IOException | InterruptedException ex)
        {
            Logger.getLogger(ServerStatistics.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return(getIdStatistics);
    }

}
