module com.tugalsan.api.servlet.local {
    requires com.tugalsan.api.log;
	requires com.tugalsan.api.union;
	requires com.tugalsan.api.thread;
	
	requires com.tugalsan.api.function;
        exports com.tugalsan.api.servlet.local.server;
}
