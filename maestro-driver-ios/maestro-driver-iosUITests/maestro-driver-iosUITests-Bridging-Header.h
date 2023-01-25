//
//  Use this file to import your target's public headers that you would like to expose to Swift.
//

//#import <objc/runtime.h>
//#import "XCTestManager_ManagerInterface-Protocol.h"
//#import "XCTRunnerDaemonSession.h"
//
//@protocol XCTestManager_ManagerInterface;
//
//id getTestRunnerProxy() {
//    Class FBXCTRunnerDaemonSessionClass = objc_lookUpClass("XCTRunnerDaemonSession");
//    XCTRunnerDaemonSession *deamonSession = (XCTRunnerDaemonSession *)[FBXCTRunnerDaemonSessionClass sharedSession];
//    id testRunnerProxy = deamonSession.daemonProxy;
//    return testRunnerProxy;
//}
//
////void sentTextUsingPrivateHeaders(id text, int frequency, void (^completion)(NSError *)) {
////    [getTestRunnerProxy()
////        _XCT_sendString: text
////        maximumFrequency: frequency
////        completion: completion];
////}
