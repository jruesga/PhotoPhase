#include <jni.h>
#include <GLES2/gl2.h>

/**
 * Binds a ByteBuffer image to opengl glTexSubImage2D
 */
JNIEXPORT void JNICALL
Java_com_ruesga_android_wallpapers_photophase_utils_GLESUtil_nativeGlTexImage2D
        (JNIEnv *env, jclass clazz, jobject image, jint width, jint height) {
//	jbyte *pixels = (*env)->GetDirectBufferAddress(env, image);
//    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
    //free(imageData);
//    (*env)->DeleteGlobalRef(env, pixels);

    jboolean copiedBytes;
    jbyte *nativeData = (*env)->GetByteArrayElements(env, image, &copiedBytes);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nativeData);
    (*env)->ReleaseByteArrayElements(env, image, nativeData, JNI_ABORT);
}
