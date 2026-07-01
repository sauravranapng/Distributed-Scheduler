package com.saurav.jobservice.util;

import java.util.UUID;

import static com.saurav.jobservice.util.Constants.TOTAL_SEGMENTS;

public class JobServiceUtil {
    private JobServiceUtil(){

    }
    /*
     This method calculates the segment number for a given jobId.
     */
    public static int calculateSegment(UUID jobId) {
        return Math.floorMod(jobId.hashCode(), TOTAL_SEGMENTS);
    }
}
