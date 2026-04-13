package com.obar.bll;

import com.obar.config.HibernateUtil;

/**
 * BLL facade for core runtime lifecycle so UI layers do not depend on
 * infrastructure classes.
 */
public final class CoreLifecycleService {

    public void warmUp() {
        HibernateUtil.warmUp();
    }

    public void shutdown() {
        HibernateUtil.shutdown();
    }
}
