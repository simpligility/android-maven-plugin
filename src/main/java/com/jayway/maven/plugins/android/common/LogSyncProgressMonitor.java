/*
 * Copyright (C) 2011 simpligility technologies inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jayway.maven.plugins.android.common;


import com.android.ddmlib.SyncService;
import org.apache.maven.plugin.logging.Log;

/**
 * LogSyncProgressMonitor is an implementation of the ISyncProgressMonitor
 * from the Android ddmlib that logs to the Maven Plugin log passed into it.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class LogSyncProgressMonitor implements SyncService.ISyncProgressMonitor {
    private static final String INDENT = "  ";
    private Log log;

    public LogSyncProgressMonitor(Log log) {
        this.log = log;
    }

    public void start(int totalWork) {
        log.info("Starting transfer of " + totalWork
            + ". See debug log for progress");
    }

    public void stop() {
        log.info("Stopped transfer");
    }

    public boolean isCanceled() {
        return false;
    }

    public void startSubTask(String name) {
        log.info(INDENT + "Started sub task " + name);
    }

    public void advance(int work) {
        log.debug(INDENT + "Transferred " + work);
    }
}