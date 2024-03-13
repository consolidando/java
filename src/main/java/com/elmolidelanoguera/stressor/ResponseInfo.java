/*
 * Copyright (c) 2024 joanribalta@elmolidelanoguera.com
 * License: CC BY-NC-ND 4.0 (https://creativecommons.org/licenses/by-nc-nd/4.0/)
 * Blog Consolidando: https://diy.elmolidelanoguera.com/
 */
package com.elmolidelanoguera.stressor;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author joanr
 */
public class ResponseInfo         
{    
    RealTimeStatistics responseTime = new RealTimeStatistics();
    // errors & exceptions
    // all can be changed to LongAdder !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private final AtomicLong exceptionsAfterRetrying = new AtomicLong(0);
    private final AtomicLong timeoutExceptions = new AtomicLong(0);
    private final AtomicLong connexionExceptions = new AtomicLong(0);
    private final AtomicLong otherExceptions = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);

    // -------------------------------------------------------------------------
    public AtomicLong getExceptionsAfterRetrying()
    {
        return exceptionsAfterRetrying;
    }
    
    public void incrementExceptionsAfterRetrying()
    {
        exceptionsAfterRetrying.incrementAndGet();
    }

    // -------------------------------------------------------------------------
    public AtomicLong getTimeoutExceptions()
    {
        return timeoutExceptions;
    }
    
    public void incrementTimeoutExceptions()
    {
        timeoutExceptions.incrementAndGet();
    }

    // -------------------------------------------------------------------------
    public AtomicLong getConnexionExceptions()
    {
        return connexionExceptions;
    }
    
    public void incrementConnexionExceptions()
    {
        connexionExceptions.incrementAndGet();
    }

    // -------------------------------------------------------------------------
    public AtomicLong getOtherExceptions()
    {
        return otherExceptions;
    }
    
    public void incrementOtherExceptions()
    {
        otherExceptions.incrementAndGet();
    }
    

    // -------------------------------------------------------------------------
    public AtomicLong getErrors()
    {
        return errors;        
    }

    public void incrementErrors()
    {
        errors.incrementAndGet();
    }
}
