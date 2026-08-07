package tester;

public class testingVariablesPile {
    private static String[] list1;
    private static String[] list2;
    private static String[] list3;

    // Environment configuration. Values are resolved (in order) from a JVM system
    // property, then an environment variable, then a sensible default. This lets
    // the same harness target the local synthetic stand-in, the hosted service,
    // or a future Tryg environment without editing code:
    //
    //   mvn test -Dsaepiox.host=https://staging.saepiox.com/
    //   SAEPIOX_HOST=http://127.0.0.1:5000/ mvn test
    //
    // Default host points at the local stand-in (testenv/server.py on :5000).
    private static String host = cfg("saepiox.host", "SAEPIOX_HOST", "http://127.0.0.1:5000/");
    private static String adminlogin = cfg("saepiox.admin.login", "SAEPIOX_ADMIN_LOGIN", "tj@saepiox.com");
    private static String adminPass = cfg("saepiox.admin.pass", "SAEPIOX_ADMIN_PASS", "pass");

    // Dedicated Tryg integration test login (synthetic account on the stand-in).
    private static String trygLogin = cfg("tryg.login", "TRYG_LOGIN", "tryg-tester");
    private static String trygPass = cfg("tryg.pass", "TRYG_PASS", "tryg-test-01");

    /** Resolve a config value: -Dprop, then $ENV, then default. */
    private static String cfg(String prop, String env, String def) {
        String v = System.getProperty(prop);
        if (v == null || v.isEmpty()) {
            v = System.getenv(env);
        }
        return (v == null || v.isEmpty()) ? def : v;
    }

    public static void setList1(String[] list1) {
        testingVariablesPile.list1 = list1;
    }

    public static void setList2(String[] list2) {
        testingVariablesPile.list2 = list2;
    }

    public static void setList3(String[] list3) {
        testingVariablesPile.list3 = list3;
    }

    public static String[] getList1() {
        return list1;
    }


    public static String[] getList2() {
        return list2;
    }

    public static String[] getList3() {
        return list3;
    }

    public static String getAdminlogin(){
        return adminlogin;
    }

    public static void setAdminPass(String adminPass) {
        testingVariablesPile.adminPass = adminPass;
    }

    public static void setAdminlogin(String adminlogin) {
        testingVariablesPile.adminlogin = adminlogin;
    }

    public static String getAdminPass() {
        return adminPass;
    }

    public static String getHost() {
        return host;
    }
    public static void setHost(String toSet){

        testingVariablesPile.host = toSet;
    }

    // -- Tryg integration test profile --------------------------------------
    public static String getTrygLogin() {
        return trygLogin;
    }

    public static void setTrygLogin(String toSet) {
        testingVariablesPile.trygLogin = toSet;
    }

    public static String getTrygPass() {
        return trygPass;
    }

    public static void setTrygPass(String toSet) {
        testingVariablesPile.trygPass = toSet;
    }
}
