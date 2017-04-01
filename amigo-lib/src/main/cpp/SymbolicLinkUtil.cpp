//
// Created by wwm on 3/10/17.
//

#include <unistd.h>
#include <stdio.h>
#include <errno.h>
#include "SymbolicLinkUtil.h"

jint throwException(JNIEnv *env, const char *message) {
    jclass exClass;
    const char *className = "java/io/IOException";
    exClass = env->FindClass(className);

    if(!exClass) {
      return -1; // ignore
    }

    return env->ThrowNew(exClass, message);
}


JNIEXPORT jint JNICALL Java_me_ele_amigo_utils_SymbolicLinkUtil_makeSymbolicLink
        (JNIEnv *env, jclass clazz, jstring linkPath, jstring targetFilePath) {
    const char *inputFilePathC = env->GetStringUTFChars(targetFilePath, 0);
    const char *inputSymLinkPathC = env->GetStringUTFChars(linkPath, 0);

    int result = symlink(inputFilePathC, inputSymLinkPathC);

    //release resources when done
    env->ReleaseStringUTFChars(targetFilePath, inputFilePathC);
    env->ReleaseStringUTFChars(linkPath, inputSymLinkPathC);

    if (result == -1) {
        char errorStr[100];
        sprintf(errorStr, "failed to create link, errno=%d", errno);
        throwException(env, errorStr);
    }

    return result;
}