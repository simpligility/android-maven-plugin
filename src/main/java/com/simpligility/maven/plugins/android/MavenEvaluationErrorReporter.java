/*
 * Copyright (C) 2015 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

package com.simpligility.maven.plugins.android;

import com.android.annotations.NonNull;
import com.android.builder.core.EvaluationErrorReporter;
import com.android.builder.model.SyncIssue;
import com.android.utils.ILogger;

/**
 * @author kedzie
 */
public class MavenEvaluationErrorReporter extends EvaluationErrorReporter
{

   private ILogger logger;

   public MavenEvaluationErrorReporter( ILogger logger, @NonNull EvaluationMode mode )
   {
      super( mode );
      this.logger = logger;
   }

   @Override
   public SyncIssue handleSyncError( @NonNull String data, int type, @NonNull String msg )
   {
      logger.info( "Sync Error.  Data: " + data + "\tmsg: " + msg );
      return new SyncIssueImpl( 0, type, data, msg );
   }
}

class SyncIssueImpl implements SyncIssue
{
   private int severity;
   private int type;
   private String data;
   private String message;

   SyncIssueImpl( int severity, int type, String data, String message )
   {
      this.severity = severity;
      this.type = type;
      this.data = data;
      this.message = message;
   }

   @Override
   public int getSeverity()
   {
      return severity;
   }

   @Override
   public int getType()
   {
      return type;
   }

   @Override
   public String getData()
   {
      return data;
   }

   @Override
   public String getMessage()
   {
      return message;
   }
}
