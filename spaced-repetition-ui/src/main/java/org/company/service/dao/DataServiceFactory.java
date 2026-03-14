package org.company.service.dao;

/**
 * Factory that creates {@link DataService} instances for a given server URL.
 * Currently always returns a {@link GrpcDataService}.
 */
public class DataServiceFactory {
    /**
     * Creates a new data service connected to the specified URL.
     *
     * @param url the gRPC server address
     * @return a new {@link GrpcDataService} instance
     */
    public DataService create(String url) {
        return new GrpcDataService(url);
    }
}