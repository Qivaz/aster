package io.qivaz.aster.runtime;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public class BundleNotInstalledException extends Exception {

    public BundleNotInstalledException() {
        super();
    }

    public BundleNotInstalledException(String message) {
        super(message);
    }
}
