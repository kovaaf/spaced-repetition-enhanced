package org.company.infrastructure.data;

public class DataServiceFactory {
    public DataService create(String url) {
        return new GrpcDataService(url);
    }
}