package org.jboss.aerogear.android.parent.test;

import android.content.Context;
import org.jboss.aerogear.android.parent.ParentStoryTeller;


public class ParentAndGrandparentTest extends android.test.AndroidTestCase{
    
    
    public void testParent() {
        Context context = getContext();
        ParentStoryTeller teller = new ParentStoryTeller(context);
        teller.tellStory();
    }
}
