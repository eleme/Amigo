// IAmigoService.aidl
package android.app;


interface IAmigoService {
    void join(IBinder token, int name);
    void leave(IBinder token, int name);
}
