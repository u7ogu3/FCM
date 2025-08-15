// syshook.cpp
/*
 *	SysHook - 7/17/05
 *	Jacob Gohlke
 *	
 *	JNI Interface for setting a Keyboard Hook and monitoring
 *	it Java-side
 *
 *	(c) Copyright 2005 Jacob Gohlke
 *	
 *	Feel free to use and learn from this code, royalty-free!
 *	I only ask you acknkowlege what library you are using
 *	and who made it. Thanks, and happy hooking!
 */
 
#include <windows.h>
#include <winuser.h>
#include "jni.h"
#include "syshook.h"
 
#pragma data_seg(".HOOKDATA") //Shared data among all instances.
static HHOOK hkb = NULL;
static HANDLE g_hModule = NULL;
static WPARAM g_wParam = NULL;
static LPARAM g_lParam = NULL;

 
JNIEXPORT void NotifyJava(JNIEnv *env, jobject obj, WPARAM wParam, LPARAM lParam)
{		
	jclass cls = env->GetObjectClass(obj);
	jmethodID mid;
 
	mid = env->GetMethodID(cls, "Callback", "(ZIZZ)V");
	if (mid == NULL) 
			return;
 
	if( (HIWORD( lParam ) & KF_UP) )
		env->CallVoidMethod(obj, mid, (jboolean)FALSE, (jint)(wParam), (jboolean)(HIWORD( lParam ) & KF_ALTDOWN), (jboolean)(HIWORD( lParam ) & KF_EXTENDED));
	else
		env->CallVoidMethod(obj, mid, (jboolean)TRUE, (jint)(wParam), (jboolean)(HIWORD( lParam ) & KF_ALTDOWN), (jboolean)(HIWORD( lParam ) & KF_EXTENDED));
}
 
#pragma data_seg() 
 
#pragma comment(linker, "/SECTION:.HOOKDATA,RWS")
 
JNIEXPORT LRESULT CALLBACK HookKeyboardProc(INT nCode, WPARAM wParam, LPARAM lParam) 
{
    if (nCode < 0)  // do not process message 
		return CallNextHookEx(hkb, nCode, wParam, lParam); 

	g_wParam = wParam;
	g_lParam = lParam;

	return CallNextHookEx(hkb, nCode, wParam, lParam);
} 
 
JNIEXPORT void JNICALL Java_com_acm_keyboard_win_KeyboardHook_00024PollThread_checkKeyboardChanges(JNIEnv *env, jobject obj)
{
	if(g_wParam != NULL && g_lParam != NULL)
	{
		NotifyJava(env, obj, g_wParam, g_lParam);
		g_wParam = NULL;
		g_lParam = NULL;
	}
}

static void Cleanup()
{
	if( hkb != NULL )
		UnhookWindowsHookEx( hkb );
 
	hkb = NULL;
}

JNIEXPORT void JNICALL Java_com_acm_keyboard_win_KeyboardHook_00024PollThread_cleanup (JNIEnv *env, jobject obj)
{
	Cleanup();
}
 
static void Init()
{
	hkb = SetWindowsHookEx( WH_KEYBOARD, (HOOKPROC)HookKeyboardProc, (HINSTANCE)g_hModule, 0 );
}
 
BOOL APIENTRY DllMain( HANDLE hModule, DWORD  ul_reason_for_call, LPVOID lpReserved )
{
	switch(ul_reason_for_call)
	{
		case DLL_PROCESS_ATTACH:
			g_hModule = hModule;
			Init();
			return TRUE;
 
		case DLL_PROCESS_DETACH:
			Cleanup();
			return TRUE;
	}
 
    return TRUE;
}
