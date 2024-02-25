module com.tugalsan.api.servlet.local {
    requires com.tugalsan.api.log;
	requires com.tugalsan.api.unsafe;
	requires com.tugalsan.api.thread;
	requires com.tugalsan.api.optional;
	requires com.tugalsan.api.runnable;
	requires com.tugalsan.api.callable;
        exports com.tugalsan.api.servlet.local.server;
}
