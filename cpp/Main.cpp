#include "../Demo.h"
#include "../../wiimote.h"
#include <Main.h>

JNIEXPORT void JNICALL Java_Main_connect
(JNIEnv * env, jobject caller, jobject handler) {
	wiimote remote;

	jclass handlerCls = env->GetObjectClass(handler);
	jmethodID listener = env->GetMethodID(handlerCls, "onDataReceived", "(DDDDD)V");

reconnect:

	unsigned count = 0;
	while (!remote.Connect(wiimote::FIRST_AVAILABLE)) {
		count++;
	}
	
	// display the wiimote state data until 'Home' is pressed:
	while (!remote.Button.Home())// && !GetAsyncKeyState(VK_ESCAPE))
	{
		// IMPORTANT: the wiimote state needs to be refreshed each pass
		while (remote.RefreshState() == NO_CHANGE)
			Sleep(1); // // don't hog the CPU if nothing changed
		
		// did we loose the connection?
		if (remote.ConnectionLost())
		{
			Sleep(2000);
			goto reconnect;
		}

		env->CallVoidMethod(handler, listener, remote.BalanceBoard.Kg.TopL, remote.BalanceBoard.Kg.TopR,
			remote.BalanceBoard.Kg.BottomL, remote.BalanceBoard.Kg.BottomR, remote.BalanceBoard.Kg.Total);


	}

	remote.Disconnect();

}