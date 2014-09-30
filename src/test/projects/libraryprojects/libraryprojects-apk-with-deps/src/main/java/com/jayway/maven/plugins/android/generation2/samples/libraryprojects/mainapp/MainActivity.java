package com.jayway.maven.plugins.android.generation2.samples.libraryprojects.mainapp;

import android.os.Bundle;
import android.widget.TextView;
import com.jayway.maven.plugins.android.generation2.samples.libraryprojects.aar1.Aar1Class;
import com.jayway.maven.plugins.android.generation2.samples.libraryprojects.aar1.AbstractActivityUsingResources;
import com.jayway.maven.plugins.android.generation2.samples.libraryprojects.aarFromAar.AarFromAarClass;
import com.jayway.maven.plugins.android.generation2.samples.libraryprojects.apklibFromApklib.ApklibFromApklibClass;
import com.jayway.maven.plugins.android.generation2.samples.libraryprojects.apklib1.Lib1Class;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AbstractActivityUsingResources {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setTextFromAsset(R.id.apkAsset, "apkAsset.txt");
        setTextFromAsset(R.id.aar1Asset, "aar1Asset.txt");
        setTextFromAsset(R.id.lib1Asset, "lib1Asset.txt");
        setTextFromAsset(R.id.aarFromAarAsset, "aarFromAarAsset.txt");
        setTextFromAsset(R.id.apklibFromApklibAsset, "apklibFromApklibAsset.txt");

        setTextFromClass(R.id.apkJavaclass, ApkClass.getApkWithDepsString());
        setTextFromClass(R.id.aar1Javaclass, Aar1Class.getAar1String());
        setTextFromClass(R.id.lib1Javaclass, Lib1Class.getApklib1String());
        setTextFromClass(R.id.aarFromAarJavaclass, AarFromAarClass.getAarFromAarString());
        setTextFromClass(R.id.apklibFromApklibJavaclass, ApklibFromApklibClass.getApklibFromApklibString());

        setTextFromResource(R.id.apkJavaResource, "/apkJavaResource.txt");
        setTextFromResource(R.id.aar1JavaResource, "/aar1JavaResource.txt");
        setTextFromResource(R.id.lib1JavaResource, "/lib1JavaResource.txt");
        setTextFromResource(R.id.aarFromAarJavaResource, "/aarFromAarJavaResource.txt");
        setTextFromResource(R.id.apklibFromApklibJavaResource, "/apklibFromApklibJavaResource.txt");
    }

    private void setTextFromClass(int id, String text) {
        final TextView view = (TextView) findViewById(id);
        view.setText(text);
    }

    private void setTextFromAsset(int id, String assetfilename) {
        final TextView view = (TextView) findViewById(id);
        try {
            final InputStream inputStream = getAssets().open(assetfilename);
            view.setText(IOUtils.toString(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTextFromResource(int id, String resourcePath) {
        final TextView view = (TextView) findViewById(id);
        try {
            final InputStream inputStream = this.getClass().getResourceAsStream(resourcePath);
            view.setText(IOUtils.toString(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
