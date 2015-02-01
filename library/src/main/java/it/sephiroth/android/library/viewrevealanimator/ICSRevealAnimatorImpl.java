package it.sephiroth.android.library.viewrevealanimator;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;

/**
 * Created by alessandro on 01/02/15.
 */
public class ICSRevealAnimatorImpl extends RevealAnimatorImpl {
    ICSRevealAnimatorImpl(final ViewRevealAnimator animator) {
        super(animator);
    }

    @Override
    public void showOnly(final int previousChild, final int childIndex) {
        Log.i(TAG, "showOnly: " + previousChild + " >> " + childIndex);

        parent.mInAnimation.setAnimationListener(new MyAnimationListener(previousChild, childIndex));

        final int count = parent.getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = parent.getChildAt(i);
            if (i == childIndex) {
                if (parent.mInAnimation != null) {
                    child.startAnimation(parent.mInAnimation);
                }
                child.setVisibility(View.VISIBLE);
                parent.mFirstTime = false;
            } else {
                if (parent.mOutAnimation != null && child.getVisibility() == View.VISIBLE) {
                    child.startAnimation(parent.mOutAnimation);
                } else if (child.getAnimation() == parent.mInAnimation) {
                    parent.mInAnimation.setAnimationListener(null);
                    child.clearAnimation();
                }
                child.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void showOnlyNoAnimation(final int previousIndex, final int childIndex) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            child.setVisibility(i == childIndex ? View.VISIBLE : View.GONE);
            if (child.getAnimation() == parent.mInAnimation) {
                parent.mInAnimation.setAnimationListener(null);
                child.clearAnimation();
            } else if (child.getAnimation() == parent.mOutAnimation) {
                child.clearAnimation();
            }
        }
    }

    @Override
    public boolean isAnimating() {
        return
            (parent.mInAnimation != null &&
                (parent.mInAnimation.hasStarted() && !parent.mInAnimation.hasEnded()))
                || (parent.mOutAnimation != null &&
                (parent.mOutAnimation.hasStarted() && !parent.mOutAnimation.hasEnded()));
    }

    @Override
    public boolean shouldAnimate() {
        return (!parent.mFirstTime || parent.mAnimateFirstTime)
            && !isAnimating()
            && null != parent.mInAnimation
            && null != parent.mOutAnimation;
    }

    class MyAnimationListener implements Animation.AnimationListener {
        final int prevIndex;
        final int childIndex;

        public MyAnimationListener(final int previousChild, final int childIndex) {
            this.prevIndex = previousChild;
            this.childIndex = childIndex;
        }

        @Override
        public void onAnimationStart(final Animation animation) {
        }

        @Override
        public void onAnimationEnd(final Animation animation) {
            Log.d(TAG, "onAnimationEnd");
            parent.onViewChanged(prevIndex, childIndex);
        }

        @Override
        public void onAnimationRepeat(final Animation animation) {

        }
    }
}
