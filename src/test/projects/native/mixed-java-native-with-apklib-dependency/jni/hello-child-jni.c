/*
 * Copyright (C) 2009 The Android Open Source Project
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
 *
 */
#include <string.h>
#include <jni.h>
#include <hello-jni.h>

/* This trivial JNI example is taken directly from the Android samples.
 *
 * apps/samples/hello-jni/project/src/com/example/mixed/HelloJni/HelloJni.java
 *
*/
jstring
Java_com_acme_hellojni_HelloChildJni_stringFromJNI( JNIEnv* env,
                                                  jobject thiz )
{
    return Java_com_acme_hellojni_HelloJni_stringFromJNI( env, thiz );
}
