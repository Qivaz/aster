package io.qivaz.aster.runtime.bundle;

/**
 * @author Qinghua Zhang @create 2017/6/5.
 */
public interface IContainer {
    void initContainer();

    String peekContainer(String target);

    String bindContainer(String target);

    boolean unbindContainer(String target);
}
