package com.rojsn.searchengine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author sbt-rodionov-oy
 */
public class EngineTimer {    
    
    private static final Logger LOG = LogManager.getLogger(EngineTimer.class);
    private static long interval = 0;
    
    public static void start() {
        LOG.info("Started at: " + System.currentTimeMillis());
        interval = System.currentTimeMillis();
    }

    public static void end() {
        LOG.info("Completed at: " + System.currentTimeMillis());
        LOG.info("Spent time: " + (System.currentTimeMillis() - interval));
    }    
}