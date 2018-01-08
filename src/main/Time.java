package main;

import javafx.util.Pair;

import java.util.HashMap;

/**
 * Used to record times during running
 */
public class Time {

    private long launchStartTime;
    private long playStartTime;
    private long readFileStartTime;
    private long parserStartTime;
    private long tempPostingStartTime;
    private long finalPostingStartTime;
    private static Pair<Long, Long> queryTimes; //start time, end time
    private static HashMap<String, Pair<Long, Long>> querysTimes = new HashMap<>(); //query id -> start time, end time

    /**
     * adds the start time of a single query id by the current system time
     */
    public void addQueryStartTime() {
        queryTimes = new Pair<>(System.currentTimeMillis(), 0L);
    }

    /**
     * adds the end time of a single query id by the current system time
     */
    public void addQueryEndTime() {
        queryTimes = new Pair<>(queryTimes.getKey(), System.currentTimeMillis());
    }

    /**
     * returns the duration between start and end of single query in ms
     *
     * @return duration in ms
     */
    public long getQueryDuration() {
        return queryTimes.getValue() - queryTimes.getKey();
    }

    /**
     * adds the start time of a query id by the current system time
     *
     * @param queryId query id
     */
    public void addQueryStartTime(String queryId) {
        querysTimes.put(queryId, new Pair<>(System.currentTimeMillis(), 0L));
    }

    /**
     * adds the end time of a query if by the current system time
     *
     * @param queryId query id
     */
    public void addQueryEndtime(String queryId) {
        querysTimes.put(queryId, new Pair<>(querysTimes.get(queryId).getKey(), System.currentTimeMillis()));
    }

    /**
     * returns the duration between start and end of query in ms
     *
     * @param queryId query id
     * @return duration in ms
     */
    public long getQueryDuration(String queryId) {
        return querysTimes.get(queryId).getValue() - querysTimes.get(queryId).getKey();
    }

    public long getLaunchStartTime() {
        return launchStartTime;
    }

    public void setLaunchStartTime(long launchStartTime) {
        this.launchStartTime = launchStartTime;
    }

    public long getPlayStartTime() {
        return playStartTime;
    }

    public void setPlayStartTime(long playStartTime) {
        this.playStartTime = playStartTime;
    }

    public long getReadFileStartTime() {
        return readFileStartTime;
    }

    public void setReadFileStartTime(long readFileStartTime) {
        this.readFileStartTime = readFileStartTime;
    }

    public long getParserStartTime() {
        return parserStartTime;
    }

    public void setParserStartTime(long parserStartTime) {
        this.parserStartTime = parserStartTime;
    }

    public long getTempPostingStartTime() {
        return tempPostingStartTime;
    }

    public void setTempPostingStartTime(long tempPostingStartTime) {
        this.tempPostingStartTime = tempPostingStartTime;
    }

    public long getFinalPostingStartTime() {
        return finalPostingStartTime;
    }

    public void setFinalPostingStartTime(long finalPostingStartTime) {
        this.finalPostingStartTime = finalPostingStartTime;
    }
}
