package it.sephiroth.android.library.viewrevealanimator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;

/**
 * Created by alessandro on 01/02/15.
 */
public class LollipopRevealAnimatorImpl extends RevealAnimatorImpl {
    private boolean mAnimatorAnimating;
    private Animator mAnimator;

    LollipopRevealAnimatorImpl(final ViewRevealAnimator animator) {
        super(animator);
    }

    @TargetApi (21)
    private void circularHide(final int previousIndex, final int nextIndex) {
        Log.i(TAG, "circularHide: " + previousIndex + " > " + nextIndex);
        mAnimatorAnimating = true;
        final View previousView = parent.getChildAt(previousIndex);

        Point newPoint = parent.getViewCenter(previousView);
        int finalRadius = parent.getViewRadius(previousView);

        Animator animator = ViewAnimationUtils.createCircularReveal(previousView, newPoint.x, newPoint.y, finalRadius, 0);
        animator.addListener(
            new AnimatorListenerAdapter() {
                boolean isCancelled;

                @Override
                public void onAnimationStart(final Animator animation) {
                    super.onAnimationStart(animation);
                }

                @Override
                public void onAnimationCancel(final Animator animation) {
                    Log.v(TAG, "onAnimationCancel(hide)");
                    isCancelled = true;
                    mAnimatorAnimating = false;
                }

                @Override
                public void onAnimationEnd(final Animator animation) {
                    if (!isCancelled) {
                        super.onAnimationEnd(animation);
                        circularReveal(previousIndex, nextIndex, true);
                    }
                }
            });

        mAnimator = animator;
        animator.setDuration(parent.getAnimationDuration());
        animator.setInterpolator(parent.getInterpolator());
        animator.start();

    }

    @TargetApi (21)
    private void circularReveal(final int previousIndex, final int nextIndex, final boolean hideBeforeReveal) {
        Log.i(TAG, "circularReveal: " + previousIndex + " > " + nextIndex);

        mAnimatorAnimating = true;
        final boolean isReveal = !hideBeforeReveal ? (nextIndex > previousIndex ? true : false) : true;
        final View nextView = parent.getChildAt(nextIndex);
        final View previousView = parent.getChildAt(previousIndex);
        final View targetView = isReveal ? nextView : previousView;

        if (hideBeforeReveal) {
            showOnlyNoAnimation(previousIndex, nextIndex);
        } else {
            nextView.setVisibility(View.VISIBLE);
        }

        if (targetView.getWidth() == 0 || targetView.getHeight() == 0) {
            targetView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        targetView.getViewTreeObserver().removeOnPreDrawListener(this);

                        if (targetView.getWidth() == 0 || targetView.getHeight() == 0) {
                            mAnimatorAnimating = false;
                            showOnlyNoAnimation(previousIndex, nextIndex);
                            parent.onViewChanged(previousIndex, nextIndex);
                        } else {
                            circularReveal(previousIndex, nextIndex, hideBeforeReveal);
                        }
                        return true;
                    }
                });
            return;
        }

        Point newPoint = parent.getViewCenter(targetView);
        int finalRadius = parent.getViewRadius(targetView);

        Animator animator = ViewAnimationUtils
            .createCircularReveal(targetView, newPoint.x, newPoint.y, isReveal ? 0 : finalRadius, isReveal ? finalRadius : 0);

        animator.addListener(
            new AnimatorListenerAdapter() {
                boolean isCancelled;

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mAnimatorAnimating = false;

                    if (!hideBeforeReveal) {
                        previousView.setVisibility(View.GONE);
                    }

                    if (!isCancelled) {
                        parent.onViewChanged(previousIndex, nextIndex);
                    }
                }

                @Override
                public void onAnimationCancel(final Animator animation) {
                    Log.v(TAG, "onAnimationCancel(show)");
                    isCancelled = true;
                    mAnimatorAnimating = false;
                    super.onAnimationCancel(animation);
                }
            });

        mAnimator = animator;
        animator.setDuration(parent.mAnimationDuration);
        animator.setInterpolator(parent.mInterpolator);
        animator.start();
    }

    @Override
    public void showOnly(final int previousChild, final int childIndex) {
        if (!parent.getHideBeforeReveal()) {
            circularReveal(previousChild, childIndex, parent.getHideBeforeReveal());
        } else {
            circularHide(previousChild, childIndex);
        }
    }

    @Override
    public void showOnlyNoAnimation(final int previousIndex, final int childIndex) {
        Log.i(TAG, "showOnlyNoAnimation: " + previousIndex + " > " + childIndex);

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            child.setVisibility(i == childIndex ? View.VISIBLE : View.GONE);
        }

        if (null != mAnimator) {
            mAnimator.cancel();
        }
    }

    @Override
    public boolean isAnimating() {
        return mAnimatorAnimating;
    }

    @Override
    public boolean shouldAnimate() {
        Log.d(TAG, "shouldAnimate: " + mAnimatorAnimating);
        if (mAnimatorAnimating) {
            return false;
        }

        if (!parent.mAnimateFirstTime && parent.mFirstTime) {
            return false;
        }

        return true;
    }

}
