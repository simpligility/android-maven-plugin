package com.jayway.maven.plugins.android.configuration;

import java.io.File;

/**
 * DeployApk is the configuration pojo for the DeployApk, UndeployApk and RedeployApk mojos.
 * 
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class DeployApk
{
    private File apkFile;
    private String packageName;
    
    public File getApkFile() 
    {
        return apkFile;
    }
    
    public void setApkFile( File apkFile ) 
    {
        this.apkFile = apkFile;
    }
    
    public String getPackageName() 
    {
        return packageName;
    }
    
    public void setPackageName( String packageName ) 
    {
        this.packageName = packageName;
    }
}
