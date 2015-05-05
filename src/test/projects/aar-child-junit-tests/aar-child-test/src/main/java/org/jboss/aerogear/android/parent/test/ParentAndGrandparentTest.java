package org.jboss.aerogear.android.parent.test;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.content.Context;
import org.jboss.aerogear.android.parent.ParentStoryTeller;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ParentAndGrandparentTest {
    

    @Test
    public void checkParent() {
        Context context = InstrumentationRegistry.getTargetContext();
        ParentStoryTeller teller = new ParentStoryTeller(context);
        teller.tellStory();
    }
}
