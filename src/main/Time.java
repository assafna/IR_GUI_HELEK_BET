package main;

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
