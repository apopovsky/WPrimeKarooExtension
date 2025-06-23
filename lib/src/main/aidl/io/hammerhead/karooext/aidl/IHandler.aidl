package io.hammerhead.karooext.aidl;

interface IHandler {
     void onNext(in Bundle bundle);
     oneway void onError(String msg);
     oneway void onComplete();
}