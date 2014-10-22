package org.jboss.aerogear.android.parent;



import android.content.Context;

public class ParentStoryTeller  {

    private final Context mContext;

    public ParentStoryTeller(Context mContext) {
        this.mContext = mContext;
    }
    
    public String tellStory() {
        return mContext.getString(R.string.story);
    }
    
    
    
}
