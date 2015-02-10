package org.digitalillusion.droid.iching.anim;

import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.widget.ImageView;

import org.digitalillusion.droid.iching.R;

/**
 * A class to ease the animation of the coins
 *
 * @author digitalillusion
 */
public class AnimCoin {

  protected AnimationDrawable animation;

  public AnimCoin(final ImageView imageView, Resources res) {
    animation = new AnimationDrawable();
    animation.addFrame(res.getDrawable(R.drawable.ic_yangcoin), (int) (50 + 100 * Math.random()));
    animation.addFrame(res.getDrawable(R.drawable.ic_halfcoin), (int) (25 + 25 * Math.random()));
    animation.addFrame(res.getDrawable(R.drawable.ic_yincoin), (int) (50 + 100 * Math.random()));
    animation.addFrame(res.getDrawable(R.drawable.ic_halfcoin), (int) (25 + 25 * Math.random()));
    animation.setOneShot(false);

    imageView.post(new Runnable() {
      public void run() {
        imageView.setBackgroundDrawable(animation);
        animation.start();
      }
    });
  }

  public AnimationDrawable getAnimation() {
    return animation;
  }

}
