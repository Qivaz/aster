package android.util;

public abstract class Singleton {

    private Object mInstance;

    protected abstract Object create();

    public Object get() {
        synchronized (this) {
            if (this.mInstance == null) {
                this.mInstance = this.create();
            }

            return this.mInstance;
        }
    }
}
