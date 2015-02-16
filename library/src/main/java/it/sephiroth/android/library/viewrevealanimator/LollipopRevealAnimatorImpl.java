package it.sephiroth.android.library.viewrevealanimator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;

import static it.sephiroth.android.library.viewrevealanimator.ViewRevealAnimator.DBG;

/**
 * Created by alessandro on 01/02/15.
 */
class LollipopRevealAnimatorImpl extends RevealAnimatorImpl {
    private boolean mAnimatorAnimating;
    private Animator mAnimator;

    LollipopRevealAnimatorImpl(final ViewRevealAnimator animator) {
        super(animator);
    }

    @TargetApi (21)
    private void circularHide(final int previousIndex, final int nextIndex, @Nullable final Point origin) {
        if (DBG) {
            Log.i(TAG, "circularHide: " + previousIndex + " > " + nextIndex);
            if (null != origin) {
                Log.v(TAG, "origin: " + origin.x + "x" + origin.y);
            }
        }
        mAnimatorAnimating = true;
        final View previousView = parent.getChildAt(previousIndex);
        int finalRadius = parent.getViewRadius(previousView);
        Point newPoint = parent.getViewCenter(previousView);

        if (null != origin) {
            double distance = ViewRevealAnimator.distance(origin, newPoint);
            finalRadius += distance;
            newPoint = new Point(origin.x - previousView.getLeft(), origin.y - previousView.getTop());
        }


        Animator animator = ViewAnimationUtils.createCircularReveal(previousView, newPoint.x, newPoint.y, finalRadius, 0);
        animator.addListener(
            new AnimatorListenerAdapter() {
                boolean isCancelled;

                @Override
                public void onAnimationStart(final Animator animation) {
                    super.onAnimationStart(animation);
                    parent.onAnimationStarted(previousIndex, nextIndex);
                }

                @Override
                public void onAnimationCancel(final Animator animation) {
                    if (DBG) {
                        Log.v(TAG, "onAnimationCancel(hide)");
                    }
                    isCancelled = true;
                    mAnimatorAnimating = false;
                }

                @Override
                public void onAnimationEnd(final Animator animation) {
                    if (DBG) {
                        Log.v(TAG, "onAnimationEnd(hide), isCancelled: " + isCancelled);
                    }
                    if (!isCancelled) {
                        super.onAnimationEnd(animation);
                        circularReveal(previousIndex, nextIndex, true, origin);
                    }
                }
            });

        mAnimator = animator;
        animator.setDuration(parent.getAnimationDuration());
        animator.setInterpolator(parent.getInterpolator());
        animator.start();

    }

    @TargetApi (21)
    private void circularReveal(
        final int previousIndex, final int nextIndex, final boolean hideBeforeReveal, @Nullable final Point origin) {
        if (DBG) {
            Log.i(TAG, "circularReveal: " + previousIndex + " > " + nextIndex);
            if (null != origin) {
                Log.v(TAG, "origin: " + origin.x + "x" + origin.y);
            }
        }

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

                        if (DBG) {
                            Log.d(TAG, "onPreDraw");
                        }

                        if (targetView.getWidth() == 0 || targetView.getHeight() == 0) {
                            mAnimatorAnimating = false;
                            showOnlyNoAnimation(previousIndex, nextIndex);
                            parent.onViewChanged(previousIndex, nextIndex);
                        } else {
                            circularReveal(previousIndex, nextIndex, hideBeforeReveal, origin);
                        }
                        return true;
                    }
                });
            return;
        }

        int finalRadius = parent.getViewRadius(targetView);
        Point newPoint = parent.getViewCenter(targetView);

        if (null != origin) {
            double distance = ViewRevealAnimator.distance(origin, newPoint);
            finalRadius += distance;
            newPoint = new Point(origin.x - targetView.getLeft(), origin.y - targetView.getTop());
        }

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
                        parent.onAnimationCompleted(previousIndex, nextIndex);
                        parent.onViewChanged(previousIndex, nextIndex);
                    }
                }

                @Override
                public void onAnimationCancel(final Animator animation) {
                    if (DBG) {
                        Log.v(TAG, "onAnimationCancel(show)");
                    }
                    isCancelled = true;
                    mAnimatorAnimating = false;
                    super.onAnimationCancel(animation);
                }

                @Override
                public void onAnimationStart(final Animator animation) {
                    if (!hideBeforeReveal) {
                        parent.onAnimationStarted(previousIndex, nextIndex);
                    }
                }
            });

        mAnimator = animator;
        animator.setDuration(parent.mAnimationDuration);
        animator.setInterpolator(parent.mInterpolator);
        animator.start();
    }

    @Override
    public void showOnly(final int previousChild, final int childIndex, Point origin) {
        if (!parent.getHideBeforeReveal()) {
            circularReveal(previousChild, childIndex, parent.getHideBeforeReveal(), origin);
        } else {
            circularHide(previousChild, childIndex, origin);
        }
    }

    @Override
    public void showOnlyNoAnimation(final int previousIndex, final int childIndex) {
        if (DBG) {
            Log.i(TAG, "showOnlyNoAnimation: " + previousIndex + " > " + childIndex);
        }

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
        if (mAnimatorAnimating) {
            return false;
        }

        if (!parent.mAnimateFirstTime && parent.mFirstTime) {
            return false;
        }

        return true;
    }

}
