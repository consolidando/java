# HTTP Server Parallel Stress Test

This Java application performs parallel stress testing on an HTTP server to measure its performance under load. It sends multiple HTTP GET requests to a specified API endpoint concurrently and collects statistics such as response times, minimum and maximum response times, and the number of exceptions and errors encountered.

## Description

The application sends requests to an HTTP server for all configured `ids` a specified number of times defined in `CYCLE_NUMBER` in parallel using virtual threads. These virtual threads run within a maximum of `PLATFORM_THREAD_USED` platform threads. The goal is to determine the server's maximum throughput for a specific server configuration by setting `DEFAULT_MINIMUM_TIME_BETWEEN_REQUESTS_IN_MICROSECONDS`, which represents the minimum time between requests in microseconds.

The `PLATFORM_THREADS_USED` parameter limits the number of platform threads used by the stressor to prevent monopolization of the machine's resources and allows for running both the client and the server on the same machine. For example, the server could be running inside a Docker container, which allows limiting the number of CPU cores used by each container.

## Usage

Execute the following command to use the application:

java -jar stressor.jar 2000

The parameter `2000` is optional and represents the minimum number of microseconds to wait between each request. By default, it is set to 2000 microseconds.


## Configuration

- `CYCLE_NUMBER`: Number of request cycles to perform.
- `PLATFORM_THREADS_USED`: Number of platform threads to use (0 for the number of CPU cores).
- `DEFAULT_MINIMUM_TIME_BETWEEN_REQUESTS_IN_MICROSECONDS`: Default minimum time between requests in microseconds.
- `MAXIMUM_TIME_ALL_THREADS`: Maximum time for all threads to complete execution.
- `BASE_URL`: Base URL of the API endpoint.
- `ids`: List of IDs to cycle in the requests.

## Dependencies

- Java Development Kit (JDK) 21 or higher

## License

This project is licensed under the [CC BY-NC-ND 4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/) License. See the [LICENSE](LICENSE.md) file for details.

