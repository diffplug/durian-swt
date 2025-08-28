// DeepLinkBridge.m - Handle deep links on macOS without breaking SWT
// Build as a small .dylib and load it early (see Java below).
#import <Cocoa/Cocoa.h>
#import <objc/runtime.h>
#import <jni.h>

@class DPDelegateProxy;  // Forward declaration

static JavaVM *gJVM = NULL;
static jclass gHandlerClass = NULL;         // Global ref to MacDeepLink class
static jmethodID gDeliverMID = NULL;        // Method ID for deliverURL(String)
static DPDelegateProxy *gDelegateProxy = NULL;  // Strong ref to prevent deallocation

#pragma mark - Helpers

static void abortWithMessage(NSString *message) {
    NSLog(@"[DeepLink] FATAL: %@", message);
    fflush(stdout);  // Ensure message is printed before crash
    
    // Most aggressive crash - direct null pointer dereference
    // This causes SIGSEGV which is very hard to catch
    volatile int *p = NULL;
    *p = 42;
    
    // Fallbacks in case the above somehow doesn't work
    __builtin_trap();
    abort();
}

#pragma mark - JNI bootstrap

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    gJVM = vm;
    NSLog(@"[DeepLink] JNI_OnLoad: JavaVM stored");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    NSLog(@"[DeepLink] JNI_OnUnload: Cleaning up");
    
    // Deregister Apple Event handler
    [[NSAppleEventManager sharedAppleEventManager]
        removeEventHandlerForEventClass:kInternetEventClass
        andEventID:kAEGetURL];
    NSLog(@"[DeepLink] Removed Apple Event handler");
    
    // Restore original delegate before releasing proxy
    if (gDelegateProxy && NSApp) {
        // Access the original delegate directly via ivar
        Ivar ivar = class_getInstanceVariable(object_getClass(gDelegateProxy), "_realDelegate");
        id originalDelegate = object_getIvar(gDelegateProxy, ivar);
        [NSApp setDelegate:originalDelegate];
        NSLog(@"[DeepLink] Restored original delegate");
    }
    
    // Clean up global reference
    if (gHandlerClass) {
        JNIEnv *env;
        if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_6) == JNI_OK) {
            (*env)->DeleteGlobalRef(env, gHandlerClass);
            gHandlerClass = NULL;
            NSLog(@"[DeepLink] Released global class reference");
        }
    }
    
    // Clear cached values
    gDeliverMID = NULL;
    gJVM = NULL;
    gDelegateProxy = NULL;  // Now safe to release the proxy
}

static JNIEnv* getEnv(BOOL *didAttach) {
    *didAttach = NO;
    if (!gJVM) return NULL;
    JNIEnv *env = NULL;
    jint rs = (*gJVM)->GetEnv(gJVM, (void **)&env, JNI_VERSION_1_6);
    if (rs == JNI_EDETACHED) {
        // Use daemon attachment for system threads so they don't block JVM shutdown
        if ((*gJVM)->AttachCurrentThreadAsDaemon(gJVM, (void **)&env, NULL) != 0) {
            return NULL;
        }
        *didAttach = YES;
    } else if (rs != JNI_OK) {
        return NULL;
    }
    return env;
}

static void deliverToJava(NSString *s) {
    NSLog(@"[DeepLink] deliverToJava called with URL");
    // These should never be null since we control registration timing
    if (!gHandlerClass || !gDeliverMID) {
        abortWithMessage(@"JNI handler not initialized - applicationStartBeforeSwt must be called first");
    }

    BOOL didAttach = NO;
    JNIEnv *env = getEnv(&didAttach);
    if (!env) {
        abortWithMessage(@"Cannot get JNI environment - JVM may be shutting down");
    }

    const char *utf8 = s.UTF8String;
    if (utf8) {
        jstring jstr = (*env)->NewStringUTF(env, utf8);
        if (jstr) {
            NSLog(@"[DeepLink] Calling Java deliverURL");
            (*env)->CallStaticVoidMethod(env, gHandlerClass, gDeliverMID, jstr);
            if ((*env)->ExceptionCheck(env)) {
                NSLog(@"[DeepLink] Java exception occurred!");
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
            (*env)->DeleteLocalRef(env, jstr);
        }
    }
    if (didAttach) (*gJVM)->DetachCurrentThread(gJVM);
}

#pragma mark - Apple Event handler

// Object to handle Apple Events
@interface DeepLinkAppleEventHandler : NSObject
+ (instancetype)sharedHandler;
- (void)handleGetURL:(NSAppleEventDescriptor *)event withReply:(NSAppleEventDescriptor *)reply;
@end

@implementation DeepLinkAppleEventHandler

+ (instancetype)sharedHandler {
    static DeepLinkAppleEventHandler *handler = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        handler = [[DeepLinkAppleEventHandler alloc] init];
    });
    return handler;
}

- (void)handleGetURL:(NSAppleEventDescriptor *)event withReply:(NSAppleEventDescriptor *)reply {
    NSString *urlString = [[event paramDescriptorForKeyword:keyDirectObject] stringValue];
    NSLog(@"[DeepLink] Apple Event received URL");
    if (urlString.length) {
        deliverToJava(urlString);
    }
}

@end

// Install Apple Event handler when Java is ready
static void installEarlyAEHandler(void) {
    @autoreleasepool {
        NSLog(@"[DeepLink] Installing Apple Event handler");

        // Register handler for kAEGetURL events
        [[NSAppleEventManager sharedAppleEventManager]
            setEventHandler:[DeepLinkAppleEventHandler sharedHandler]
            andSelector:@selector(handleGetURL:withReply:)
            forEventClass:kInternetEventClass
            andEventID:kAEGetURL];

        NSLog(@"[DeepLink] Apple Event handler installed");
    }
}

#pragma mark - NSApplicationDelegate proxy

// Dynamic proxy that intercepts application:openURLs: and forwards all other messages
@interface DPDelegateProxy : NSProxy <NSApplicationDelegate> {
    id _realDelegate;
}
- (instancetype)initWithDelegate:(id)delegate;
- (id)realDelegate;  // Getter to access original delegate for restoration
@end

@implementation DPDelegateProxy

- (instancetype)initWithDelegate:(id)delegate {
    _realDelegate = delegate;
    return self;
}

- (id)realDelegate {
    return _realDelegate;
}

- (id)forwardingTargetForSelector:(SEL)sel {
    // Fast-forward everything except our intercepted method
    if (sel == @selector(application:openURLs:)) {
        return nil;  // We'll handle this ourselves
    }
    return _realDelegate;
}

- (BOOL)respondsToSelector:(SEL)sel {
    if (sel == @selector(application:openURLs:)) return YES;
    return [_realDelegate respondsToSelector:sel];
}

- (void)application:(NSApplication *)app openURLs:(NSArray<NSURL *> *)urls {
    NSLog(@"[DeepLink] DPDelegateProxy application:openURLs: received %lu URLs", (unsigned long)urls.count);
    for (NSURL *u in urls) {
        if (!u) continue;
        NSString *s = u.absoluteString;
        NSLog(@"[DeepLink] DPDelegateProxy processing URL");
        if (s.length) deliverToJava(s);
    }
}
@end

#pragma mark - JNI exports

// Java calls this early, before SWT initialization
JNIEXPORT void JNICALL Java_com_diffplug_common_swt_widgets_MacDeepLink_nativeBeforeSwt
  (JNIEnv *env, jclass clazz) {
    
    NSLog(@"[DeepLink] nativeBeforeSwt called from Java");
    
    // Cache class & method (global ref so it survives)
    if (!gHandlerClass) {
        gHandlerClass = (*env)->NewGlobalRef(env, clazz);
        NSLog(@"[DeepLink] Cached Java class reference");
    }
    if (!gDeliverMID) {
        gDeliverMID = (*env)->GetStaticMethodID(env, gHandlerClass, "deliverURL", "(Ljava/lang/String;)V");
        if (!gDeliverMID) {
            NSLog(@"[DeepLink] ERROR: Could not find deliverURL method!");
            return;
        }
        NSLog(@"[DeepLink] Cached deliverURL method ID");
    }
    
    // Now that JNI is ready, register with macOS for Apple Events
    installEarlyAEHandler();
}

// Java calls this after SWT is initialized to install the delegate proxy
JNIEXPORT void JNICALL Java_com_diffplug_common_swt_widgets_MacDeepLink_nativeAfterSwt
  (JNIEnv *env, jclass clazz) {

    NSLog(@"[DeepLink] nativeAfterSwt called from Java");

    if (!NSApp) {
        abortWithMessage(@"NSApp is nil! Make sure SWT Display is created first");
    }

    // Wrap the existing delegate with our proxy
    id current = [NSApp delegate];
    NSLog(@"[DeepLink] Current NSApp delegate: %@", current);

    // Store proxy in static to prevent deallocation (NSApp.delegate is weak)
    gDelegateProxy = [[DPDelegateProxy alloc] initWithDelegate:current];
    [NSApp setDelegate:(id<NSApplicationDelegate>)gDelegateProxy];
    NSLog(@"[DeepLink] Installed delegate proxy");
}