# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := native-code-including-static-lib
LOCAL_SRC_FILES := hello-jni.c

# Include the static libraries pulled in via Android Maven plugin makefile (see include below)
LOCAL_STATIC_LIBRARIES := $(ANDROID_MAVEN_PLUGIN_LOCAL_STATIC_LIBRARIES)

# Build the shared libary
include $(BUILD_SHARED_LIBRARY)

# Include the Android Maven plugin generated makefile
# Important: Must be the last import in order for Android Maven Plugins paths to work
include $(ANDROID_MAVEN_PLUGIN_MAKEFILE)
