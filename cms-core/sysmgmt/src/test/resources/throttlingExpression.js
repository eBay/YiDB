var throttlingLevel = 0;

var moderator = 0;
var threshold1 = 0.80;
var threshold2 = 0.85;
var threshold3 = 0.90;
var threshold4 = 0.95;

if ( $currentTimestamp - $fullGCTimestamp < 20 * 60000 ) {
    if ( $currentThrottlingLevel > 0 ) {
        moderator = 0.05;
    }
    
	if ( $currentPercent > threshold4 - moderator ) {
	    throttlingLevel = 10000;
    } else if ( $currentPercent > threshold3 -moderator ) {
        throttlingLevel = 1000;
    } else if ( $currentPercent > threshold2 - moderator ) {
        throttlingLevel = 100;
    } else if ( $currentPercent > threshold1 - moderator ) {
        throttlingLevel = 10;
    }
}

throttlingLevel;