package org.digitalillusion.droid.iching.anim;

import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import org.digitalillusion.droid.iching.R;
import org.digitalillusion.droid.iching.utils.Consts;

/**
 * A class to ease the animation of the coins
 *
 * @author digitalillusion
 */
public class AnimCoin {

  protected int value;
  protected ImageView imageView;
  protected AnimationDrawable animation;
  private boolean running = false;
  private View.OnTouchListener touchListener;

  public AnimCoin(final ImageView imageView, Resources res) {
    animation = new AnimationDrawable();
    animation.addFrame(res.getDrawable(R.drawable.ic_yangcoin), (int) (50 + 100 * Math.random()));
    animation.addFrame(res.getDrawable(R.drawable.ic_halfcoin), (int) (25 + 25 * Math.random()));
    animation.addFrame(res.getDrawable(R.drawable.ic_yincoin), (int) (50 + 100 * Math.random()));
    animation.addFrame(res.getDrawable(R.drawable.ic_halfcoin), (int) (25 + 25 * Math.random()));
    animation.setOneShot(false);
    this.imageView = imageView;
    runAnimation();
  }

  public AnimCoin(final ImageView imageView, final Resources res, int value) {
    this.imageView = imageView;
    this.value = value;
    flip(res);

    imageView.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (running) {
          return true;
        }
        boolean flip = flip(res);
        if (touchListener != null) {
          return flip | touchListener.onTouch(v, event);
        }
        return flip;
      }
    });
  }

  public AnimationDrawable getAnimation() {
    return animation;
  }

  public int getCoinValue() {
    return value;
  }

  public int getDuration() {
    int duration = 0;
    for (int i = 0; animation != null && i < animation.getNumberOfFrames(); i++) {
      duration += animation.getDuration(i);
    }
    return duration;
  }

  public void setOnTouchListener(View.OnTouchListener touchListener) {
    this.touchListener = touchListener;
  }

  private void runAnimation() {
    imageView.setBackgroundDrawable(animation);
    running = true;
    animation.start();
    imageView.postDelayed(new Runnable() {
      public void run() {
        running = false;
      }
    }, getDuration());
  }

  private boolean flip(Resources res) {
    if (value == Consts.ICHING_COIN_YIN) {
      value = Consts.ICHING_COIN_YANG;
      animation = new AnimationDrawable();
      animation.addFrame(res.getDrawable(R.drawable.ic_yincoin), (int) (50 + 100 * Math.random()));
      animation.addFrame(res.getDrawable(R.drawable.ic_halfcoin), (int) (25 + 25 * Math.random()));
      animation.addFrame(res.getDrawable(R.drawable.ic_yangcoin), (int) (50 + 100 * Math.random()));
      runAnimation();
      return true;
    } else if (value == Consts.ICHING_COIN_YANG) {
      value = Consts.ICHING_COIN_YIN;
      animation = new AnimationDrawable();
      animation.addFrame(res.getDrawable(R.drawable.ic_yangcoin), (int) (50 + 100 * Math.random()));
      animation.addFrame(res.getDrawable(R.drawable.ic_halfcoin), (int) (25 + 25 * Math.random()));
      animation.addFrame(res.getDrawable(R.drawable.ic_yincoin), (int) (50 + 100 * Math.random()));
      runAnimation();
      return true;
    }
    return false;
  }
}
