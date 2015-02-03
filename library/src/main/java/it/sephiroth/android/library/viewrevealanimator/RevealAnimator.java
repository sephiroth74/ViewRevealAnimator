package it.sephiroth.android.library.viewrevealanimator;

/**
 * Created by alessandro on 01/02/15.
 */
interface RevealAnimator {
    void showOnly(final int previousChild, final int childIndex);

    void showOnlyNoAnimation(final int previousIndex, final int childIndex);

    boolean isAnimating();

    boolean shouldAnimate();
}
