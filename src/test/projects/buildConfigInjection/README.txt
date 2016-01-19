Based on the Hello Flash Light app, this project shows you how to inject custom variables into the Android BuildConfig class, enabling you to do some nifty things in terms of tracking stuff like
 - what branch was this build made on?
 - when was this build made?


Basically any maven parameter can be injected. Since a lot of people use git, this project includes uses some other open source maven plugins (that are in central) to inject the current time stamp, username and git information into BuildConfig.java.
