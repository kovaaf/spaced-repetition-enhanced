package org.company.spacedrepetition.ui.logic;

import org.company.spacedrepetition.ui.client.AnalyticsServiceClient;
import org.company.spacedrepetitiondata.grpc.AnalyticsProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

/**
 * Fetches statistics data from the analytics service in the background.
 * Uses SwingWorker to avoid blocking the UI thread.
 * Provides conversion from period filter strings to time ranges.
 */
public class StatisticsDataFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(StatisticsDataFetcher.class);
    
    private final AnalyticsServiceClient analyticsClient;
    
    /**
     * Constructs a new StatisticsDataFetcher with the given client.
     * @param analyticsClient the gRPC client for analytics service
     */
    public StatisticsDataFetcher(AnalyticsServiceClient analyticsClient) {
        this.analyticsClient = analyticsClient;
    }

    /**
     * Updates the name cache with any names present in the analytics response.
     * @param response the analytics response containing events with optional names
     */
    private void updateNameCache(AnalyticsProto.AnalyticsResponse response) {
        NameCache cache = NameCache.getInstance();
        for (AnalyticsProto.AnswerEvent event : response.getEventsList()) {
            // Cache user name if present
            if (event.hasUserName()) {
                cache.put(String.valueOf(event.getUserId()), event.getUserName());
            }
            // Cache deck name if present
            if (event.hasDeckName()) {
                cache.put(String.valueOf(event.getDeckId()), event.getDeckName());
            }
            // Cache card title if present
            if (event.hasCardTitle()) {
                cache.put(String.valueOf(event.getCardId()), event.getCardTitle());
            }
        }
    }
    
    /**
     * Fetches analytics data for the specified parameters.
     * The network call is performed in a background thread; callbacks are invoked on the EDT.
     * 
     * @param userId the user ID (string), empty string means "all users"
     * @param startTime start of time range (inclusive), can be null for no lower bound
     * @param endTime end of time range (inclusive), can be null for no upper bound
     * @param onSuccess callback invoked with the analytics response on success (on EDT)
     * @param onError callback invoked with exception on failure (on EDT)
     */
    public void fetchData(String userId, Instant startTime, Instant endTime,
                          Consumer<AnalyticsProto.AnalyticsResponse> onSuccess,
                          Consumer<Exception> onError) {
        new SwingWorker<AnalyticsProto.AnalyticsResponse, Void>() {
            @Override
            protected AnalyticsProto.AnalyticsResponse doInBackground() throws Exception {
                LOG.debug("Fetching analytics data for user '{}' from {} to {}", 
                         userId, startTime, endTime);
                return analyticsClient.getAnalytics(userId, startTime, endTime);
            }
            
            @Override
            protected void done() {
                try {
                    AnalyticsProto.AnalyticsResponse response = get();
                    if (response != null) {
                        LOG.info("Successfully fetched {} analytics events", response.getTotalCount());
                        // Update name cache with any names from the response
                        updateNameCache(response);
                        onSuccess.accept(response);
                    } else {
                        LOG.warn("Fetched null analytics response");
                    }
                } catch (Exception e) {
                    LOG.error("Failed to fetch analytics data", e);
                    onError.accept(e);
                }
            }
        }.execute();
    }
    /**
     * Fetches all users from the analytics service.
     * The network call is performed in a background thread; callbacks are invoked on the EDT.
     * 
     * @param onSuccess callback invoked with the users response on success (on EDT)
     * @param onError callback invoked with exception on failure (on EDT)
     */

    public void fetchUsers(Consumer<AnalyticsProto.UsersResponse> onSuccess,
                           Consumer<Exception> onError) {
        new SwingWorker<AnalyticsProto.UsersResponse, Void>() {
            @Override
            protected AnalyticsProto.UsersResponse doInBackground() throws Exception {
                LOG.debug("Fetching users");
                return analyticsClient.getUsers();
            }
            
            @Override
            protected void done() {
                try {
                    AnalyticsProto.UsersResponse response = get();
                    LOG.info("Successfully fetched {} users", response.getUsersCount());
                    // Optionally cache user names? For now just pass response.
                    onSuccess.accept(response);
                } catch (Exception e) {
                    LOG.error("Failed to fetch users", e);
                    onError.accept(e);
                }
            }
        }.execute();
    }
    
    /**
     * Converts a period filter string to a time range (start and end Instant).
     * @param periodText one of "Last Day", "Last Week", "Last Month", "Last Year", "All Time"
     * @return array of two Instants: [startTime, endTime]; endTime is current time,
     *         startTime is calculated based on period. For "All Time", startTime is epoch.
     * @throws IllegalArgumentException if periodText is not recognized
     */
    public static Instant[] periodToTimeRange(String periodText) {
        Instant end = Instant.now();
        Instant start;
        
        switch (periodText) {
            case "Last Day":
                start = end.minus(1, ChronoUnit.DAYS);
                break;
            case "Last Week":
                start = end.minus(7, ChronoUnit.DAYS);
                break;
            case "Last Month":
                start = end.minus(30, ChronoUnit.DAYS); // approximate month
                break;
            case "Last Year":
                start = end.minus(365, ChronoUnit.DAYS);
                break;
            case "All Time":
                start = Instant.EPOCH;
                break;
            default:
                throw new IllegalArgumentException("Unknown period: " + periodText);
        }
        
        return new Instant[] { start, end };
    }
    
    /**
     * Converts a user filter selection to a user ID string.
     * "All Users" returns empty string; empty string returns empty string;
     * "User N" returns the numeric part as string; numeric ID returns as-is.
     * 
     * @param userSelection the user filter selection string
     * @return user ID string (empty for all users)
     */
    public static String userSelectionToUserId(String userSelection) {
        if (userSelection == null || userSelection.equals("All Users") || userSelection.isEmpty()) {
            return "";
        }
        
        // Extract numeric part from "User N"
        String[] parts = userSelection.split(" ");
        if (parts.length == 2 && parts[0].equals("User")) {
            return parts[1];
        }
        
        // If pattern doesn't match, assume it's already a numeric ID
        return userSelection;
    }
}