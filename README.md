# HTTP Server Parallel Stress Test

This Java application performs parallel stress testing on an HTTP server to measure its performance under load. It sends multiple HTTP GET requests to a specified API endpoint concurrently and collects statistics such as response times, minimum and maximum response times, and the number of exceptions and errors encountered.

## Description

The application sends requests to an HTTP server for all configured ids a specified number of times, defined by CYCLE_NUMBER, in parallel using virtual threads. These virtual threads operate within a maximum of PLATFORM_THREAD_USED platform threads. The objective is to determine the server's maximum throughput for a specific server configuration by setting the minimum time between requests in microseconds.

The PLATFORM_THREADS_USED parameter restricts the number of platform threads utilized by the stressor to prevent monopolization of the machine's resources. This limitation facilitates running both the client and the server on the same machine. For instance, the server could operate inside a Docker container, enabling the restriction of the number of CPU cores used by each container.

## Usage

Execute the following command to use the application: (You must use the version with the dependencies included that can be found at [packages](https://github.com/consolidando/stressor/packages/2093303) and it should be run on Java 21 or higher)


**java -jar stressor.jar A AtoB B BtoC C**

Where:
- Parameter **A** represents the minimum number of microseconds to wait between each request until there are **AtoB** concurrent requests.
- Parameter **B** represents the minimum number of microseconds to wait between each request between **AtoB** and **BtoC** concurrent requests.
- Parameter **C** represents the minimum number of microseconds to wait between each request after getting **BtoC** concurrent requests.

## Configuration

- `BASE_URL`: Base URL of the API endpoint.  
- `ids`: List of IDs to cycle in the requests.  
- `CYCLE_NUMBER`: Number of request cycles to perform.  
- `PLATFORM_THREADS_USED`: Number of platform threads to use. 
- `MINIMUM_CONCURRENT_REQUEST`: Minimum number of concurrent requests (AtoB).  
- `MAXIMUM_CONCURRENT_REQUEST`: Maximum number of concurrent requests (BtoC).  
- `TIME_BETWEEN_REQUESTS_IN_CONCURRENT_REQUEST_A`: Time between requests for Concurrent Request Level A in nanoseconds.  
- `TIME_BETWEEN_REQUESTS_IN_CONCURRENT_REQUEST_B`: Time between requests for Concurrent Request Level B in nanoseconds.  
- `TIME_BETWEEN_REQUESTS_IN_CONCURRENT_REQUEST_C`: Time between requests for Concurrent Request Level C in nanoseconds.  
- `MINIMUM_TIME_BETWEEN_REQUEST`: Minimum time between requests in nanoseconds.  
- `REQUEST_RETRIES_NUMBER`: Number of request retries.  
- `TIME_BETWEEN_RETRIES`: Time between request retries in milliseconds.  
- `MAXIMUM_TIME_PER_REQUEST_CONNECTION`: Maximum time per request for connection establishment in milliseconds.  
- `MAXIMUM_TIME_PER_REQUEST_READ`: Maximum time per request for read in milliseconds.  
- `MAXIMUM_TIME_ALL_THREADS`: Maximum time for all threads to complete execution in milliseconds.  

## Dependencies

- Java Development Kit (JDK) 21 or higher

## License

This project is licensed under the [CC BY-NC-ND 4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/) License. See the [LICENSE](LICENSE.md) file for details.

