package it.sephiroth.android.library.viewrevealanimator;

/**
 * Created by alessandro on 01/02/15.
 */
abstract class RevealAnimatorImpl implements RevealAnimator {
    protected static final String TAG = "RevealAnimatorImpl";
    protected final ViewRevealAnimator parent;

    RevealAnimatorImpl(final ViewRevealAnimator animator) {
        this.parent = animator;
    }

}
