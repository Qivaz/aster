package io.qivaz.aster.runtime.bundle.serialize;

import java.io.Serializable;

/**
 * @author Qinghua Zhang @create 2017/7/6.
 */

public class ActivityInfo implements Serializable {
    /**
     * The name of the process this component should run in.
     * From the "android:process" attribute or, if not set, the same
     * as <var>applicationInfo.processName</var>.
     */
    public String processName;

    /**
     * A string resource identifier (in the package's resources) containing
     * a user-readable description of the component.  From the "description"
     * attribute or, if not set, 0.
     */
    public int descriptionRes;

    /**
     * Indicates whether or not this component may be instantiated.  Note that this value can be
     * overriden by the one in its parent {@link ApplicationInfo}.
     */
    public boolean enabled = true;

    /**
     * Set to true if this component is available for use by other applications.
     * Comes from {@link android.R.attr#exported android:exported} of the
     * &lt;activity&gt;, &lt;receiver&gt;, &lt;service&gt;, or
     * &lt;provider&gt; tag.
     */
    public boolean exported = false;

    /**
     * Indicates if this component is aware of direct boot lifecycle, and can be
     * safely run before the user has entered their credentials (such as a lock
     * pattern or PIN).
     */
    public boolean directBootAware = false;

    /**
     * @removed
     */
    @Deprecated
    public boolean encryptionAware = false;


    /**
     * Bit mask of kinds of configuration changes that this activity
     * can handle itself (without being restarted by the system).
     * Contains any combination of {@link #CONFIG_FONT_SCALE},
     * {@link #CONFIG_MCC}, {@link #CONFIG_MNC},
     * {@link #CONFIG_LOCALE}, {@link #CONFIG_TOUCHSCREEN},
     * {@link #CONFIG_KEYBOARD}, {@link #CONFIG_NAVIGATION},
     * {@link #CONFIG_ORIENTATION}, {@link #CONFIG_SCREEN_LAYOUT},
     * {@link #CONFIG_DENSITY}, and {@link #CONFIG_LAYOUT_DIRECTION}.
     * Set from the {@link android.R.attr#configChanges} attribute.
     */
    public int configChanges;

    /**
     * The document launch mode style requested by the activity. From the
     * {@link android.R.attr#documentLaunchMode} attribute, one of
     * {@link #DOCUMENT_LAUNCH_NONE}, {@link #DOCUMENT_LAUNCH_INTO_EXISTING},
     * {@link #DOCUMENT_LAUNCH_ALWAYS}.
     * <p>
     * <p>Modes DOCUMENT_LAUNCH_ALWAYS
     * and DOCUMENT_LAUNCH_INTO_EXISTING are equivalent to {@link
     * android.content.Intent#FLAG_ACTIVITY_NEW_DOCUMENT
     * Intent.FLAG_ACTIVITY_NEW_DOCUMENT} with and without {@link
     * android.content.Intent#FLAG_ACTIVITY_MULTIPLE_TASK
     * Intent.FLAG_ACTIVITY_MULTIPLE_TASK} respectively.
     */
    public int documentLaunchMode;

    /**
     * Options that have been set in the activity declaration in the
     * manifest.
     * These include:
     * {@link #FLAG_MULTIPROCESS},
     * {@link #FLAG_FINISH_ON_TASK_LAUNCH}, {@link #FLAG_CLEAR_TASK_ON_LAUNCH},
     * {@link #FLAG_ALWAYS_RETAIN_TASK_STATE},
     * {@link #FLAG_STATE_NOT_NEEDED}, {@link #FLAG_EXCLUDE_FROM_RECENTS},
     * {@link #FLAG_ALLOW_TASK_REPARENTING}, {@link #FLAG_NO_HISTORY},
     * {@link #FLAG_FINISH_ON_CLOSE_SYSTEM_DIALOGS},
     * {@link #FLAG_HARDWARE_ACCELERATED}, {@link #FLAG_SINGLE_USER}.
     */
    public int flags;

    /**
     * The launch mode style requested by the activity.  From the
     * {@link android.R.attr#launchMode} attribute, one of
     * {@link #LAUNCH_MULTIPLE},
     * {@link #LAUNCH_SINGLE_TOP}, {@link #LAUNCH_SINGLE_TASK}, or
     * {@link #LAUNCH_SINGLE_INSTANCE}.
     */
    public int launchMode;

    /**
     * Value indicating if the activity is to be locked at startup. Takes on the values from
     * {@link android.R.attr#lockTaskMode}.
     *
     * @hide
     */
    public int lockTaskLaunchMode;

    /**
     * The maximum number of tasks rooted at this activity that can be in the recent task list.
     * Refer to {@link android.R.attr#maxRecents}.
     */
    public int maxRecents;

    /**
     * Optional name of a permission required to be able to access this
     * Activity.  From the "permission" attribute.
     */
    public String permission;

    /**
     * Value indicating how this activity is to be persisted across
     * reboots for restoring in the Recents list.
     * {@link android.R.attr#persistableMode}
     */
    public int persistableMode;

    /**
     * The preferred screen orientation this activity would like to run in.
     * From the {@link android.R.attr#screenOrientation} attribute, one of
     * {@link #SCREEN_ORIENTATION_UNSPECIFIED},
     * {@link #SCREEN_ORIENTATION_LANDSCAPE},
     * {@link #SCREEN_ORIENTATION_PORTRAIT},
     * {@link #SCREEN_ORIENTATION_USER},
     * {@link #SCREEN_ORIENTATION_BEHIND},
     * {@link #SCREEN_ORIENTATION_SENSOR},
     * {@link #SCREEN_ORIENTATION_NOSENSOR},
     * {@link #SCREEN_ORIENTATION_SENSOR_LANDSCAPE},
     * {@link #SCREEN_ORIENTATION_SENSOR_PORTRAIT},
     * {@link #SCREEN_ORIENTATION_REVERSE_LANDSCAPE},
     * {@link #SCREEN_ORIENTATION_REVERSE_PORTRAIT},
     * {@link #SCREEN_ORIENTATION_FULL_SENSOR},
     * {@link #SCREEN_ORIENTATION_USER_LANDSCAPE},
     * {@link #SCREEN_ORIENTATION_USER_PORTRAIT},
     * {@link #SCREEN_ORIENTATION_FULL_USER},
     * {@link #SCREEN_ORIENTATION_LOCKED},
     */
    public int screenOrientation;

    /**
     * The desired soft input mode for this activity's main window.
     * Set from the {@link android.R.attr#windowSoftInputMode} attribute
     * in the activity's manifest.  May be any of the same values allowed
     * for {@link android.view.WindowManager.LayoutParams#softInputMode
     * WindowManager.LayoutParams.softInputMode}.  If 0 (unspecified),
     * the mode from the theme will be used.
     */
    public int softInputMode;

    /**
     * The desired extra UI options for this activity and its main window.
     * Set from the {@link android.R.attr#uiOptions} attribute in the
     * activity's manifest.
     */
    public int uiOptions = 0;

    /**
     * If this is an activity alias, this is the real activity class to run
     * for it.  Otherwise, this is null.
     */
    public String targetActivity;

    /**
     * The affinity this activity has for another task in the system.  The
     * string here is the name of the task, often the package name of the
     * overall package.  If null, the activity has no affinity.  Set from the
     * {@link android.R.attr#taskAffinity} attribute.
     */
    public String taskAffinity;

    /**
     * A style resource identifier (in the package's resources) of this
     * activity's theme.  From the "theme" attribute or, if not set, 0.
     */
    public int theme;
}
