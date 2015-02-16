package it.sephiroth.android.library.viewrevealanimator;

import android.graphics.Point;
import android.support.annotation.Nullable;

/**
 * Created by alessandro on 01/02/15.
 */
interface RevealAnimator {
    void showOnly(final int previousChild, final int childIndex, @Nullable Point origin);

    void showOnlyNoAnimation(final int previousIndex, final int childIndex);

    boolean isAnimating();

    boolean shouldAnimate();
}
