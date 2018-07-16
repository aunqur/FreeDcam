
#include "JniUtils.h"
#include <stdlib.h>
#include "RawStackPipeNative.h"
#include "HalideBuffer.h"
#include "CustomMatrix.h"
#include "DngProfile.h"

extern "C"
{
    JNIEXPORT jobject JNICALL Java_freed_jni_RawStack_init(JNIEnv *env, jobject thiz) {
        RawStackPipeNative * rawStackPipeNative = new RawStackPipeNative();
        return env->NewDirectByteBuffer(rawStackPipeNative, 0);
    }

    JNIEXPORT void JNICALL Java_freed_jni_RawStack_setBaseFrame(JNIEnv *env, jobject thiz, jobject javaHandler, jbyteArray input, jint width, jint height) {
        RawStackPipeNative * rawStackPipeNative =  (RawStackPipeNative*)env->GetDirectBufferAddress(javaHandler);
        rawStackPipeNative->init(width,height, (uint16_t*)copyByteArray(env, input));
    }

    JNIEXPORT void JNICALL Java_freed_jni_RawStack_setBaseFrameBuffer(JNIEnv *env, jobject thiz, jobject javaHandler, jobject input, jint width, jint height) {
        RawStackPipeNative * rawStackPipeNative =  (RawStackPipeNative*)env->GetDirectBufferAddress(javaHandler);
        rawStackPipeNative->init(width,height, (uint16_t*)env->GetDirectBufferAddress(input));
    }

    JNIEXPORT void JNICALL Java_freed_jni_RawStack_stackFrame(JNIEnv *env, jobject thiz, jobject javaHandler, jbyteArray input) {
        RawStackPipeNative * rawStackPipeNative =  (RawStackPipeNative*)env->GetDirectBufferAddress(javaHandler);
        rawStackPipeNative->stackFrame((uint16_t*)copyByteArray(env, input));
    }

    JNIEXPORT void JNICALL Java_freed_jni_RawStack_stackFrameBuffer(JNIEnv *env, jobject thiz, jobject javaHandler, jobject input) {
        RawStackPipeNative * rawStackPipeNative =  (RawStackPipeNative*)env->GetDirectBufferAddress(javaHandler);
        rawStackPipeNative->stackFrame((uint16_t*)env->GetDirectBufferAddress(input));
    }

    JNIEXPORT void JNICALL Java_freed_jni_RawStack_SetOpCode2(JNIEnv *env, jobject thiz, jbyteArray opcode,jobject javaHandler)
    {
        DngWriter* writer = (DngWriter*)env->GetDirectBufferAddress(javaHandler);
        writer->opcode2Size = env->GetArrayLength(opcode);
        writer->opcode2 = copyByteArray(env,opcode);
    }

    JNIEXPORT void JNICALL Java_freed_jni_RawStack_SetOpCode3(JNIEnv *env, jobject thiz, jbyteArray opcode,jobject javaHandler)
    {
        DngWriter* writer = (DngWriter*)env->GetDirectBufferAddress(javaHandler);
        writer->opcode3Size = env->GetArrayLength(opcode);
        writer->opcode3 = copyByteArray(env,opcode);
    }

    JNIEXPORT void JNICALL Java_freed_jni_RawStack_writeDng(JNIEnv *env, jobject thiz, jobject javaHandler, jobject dngprofile, jobject matrix,jstring fileout, jobject exifinfo) {
        RawStackPipeNative * rawStackPipeNative = (RawStackPipeNative*)env->GetDirectBufferAddress(javaHandler);
        DngProfile * profile = (DngProfile*)env->GetDirectBufferAddress(dngprofile);
        CustomMatrix * cmatrix = (CustomMatrix*)env->GetDirectBufferAddress(matrix);
        ExifInfo * exifInfo = (ExifInfo*)env->GetDirectBufferAddress(exifinfo);
        char * outfile = copyString(env,fileout);
        rawStackPipeNative->writeDng(profile, cmatrix, outfile,exifInfo);
        delete rawStackPipeNative;

    }

};
