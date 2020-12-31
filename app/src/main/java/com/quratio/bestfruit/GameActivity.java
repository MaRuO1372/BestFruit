package com.quratio.bestfruit;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameActivity extends Activity {
    Handler h = new Handler();
    List<ImageView> items;
    List<Integer> crashed_array;
    SharedPreferences sp;
    Editor ed;
    boolean isForeground = true;
    MediaPlayer mp;
    SoundPool sndpool;
    int snd_move;
    int snd_crush;
    int snd_result;
    int snd_info;
    float start_x;
    float start_y;
    boolean items_enabled;
    float x_pos;
    float y_pos;
    View current_item;
    View near_item;
    int t_effect;
    boolean crash_exist;
    boolean first_check;
    int score;
    AnimatorSet anim;
    int num_rows;
    int num_cols;
    int screen_width;
    int screen_height;
    int current_section = R.id.game;
    boolean showLeaders, isSigned;
    int item_size;
    final int move_step = 5; // move step
    final int t_move = 100; // items change speed
    final int time = 120; // total time in seconds
    final int need_crashed = 3; // minimum number of items need to crush


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // preferences
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        ed = sp.edit();



        // bg sound
        mp = new MediaPlayer();


        // SoundPool
        sndpool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        try {
            snd_move = sndpool.load(getAssets().openFd("snd_move.mp3"), 1);
            snd_crush = sndpool.load(getAssets().openFd("snd_crush.mp3"), 1);
            snd_result = sndpool.load(getAssets().openFd("snd_result.mp3"), 1);
            snd_info = sndpool.load(getAssets().openFd("snd_info.mp3"), 1);
        } catch (IOException e) {
        }

        // hide navigation bar listener
        findViewById(R.id.all).setOnSystemUiVisibilityChangeListener(new OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                hide_navigation_bar();
            }
        });

        START();
    }


    // START
    @SuppressLint("ClickableViewAccessibility")
    void START() {
        items_enabled = false;
        first_check = true;
        t_effect = 0;
        score = 0;
        current_item = null;
        near_item = null;
        items = new ArrayList<ImageView>();
        crashed_array = new ArrayList<Integer>();
        ((ViewGroup) findViewById(R.id.game)).removeAllViews();

        // num_rows & num_cols
            num_rows = 6;
            num_cols = 10;

        // screen size
        screen_width = Math.max(findViewById(R.id.all).getWidth(), findViewById(R.id.all).getHeight());
        screen_height = Math.min(findViewById(R.id.all).getWidth(), findViewById(R.id.all).getHeight());

        // item size
        item_size = (int) Math.floor(Math.min(screen_width / num_cols, screen_height / num_rows) / move_step) * move_step;

        // num rows for screen 4:3
        for (int i = 1; i <= 5; i++) {
            if (item_size * (num_rows + i) > screen_height) {
                num_rows += (i - 1);
                break;
            }
        }

        // start position
        start_x = (screen_width - item_size * num_cols) / 2;
        start_y = (screen_height - item_size * num_rows) / 2;

        // add items
        x_pos = 0;
        y_pos = 0;
        for (int i = 0; i < num_rows * num_cols; i++) {
            ImageView item = new ImageView(this);
            item.setClickable(true);
            item.setLayoutParams(new LayoutParams(item_size, item_size));
            item.setImageResource(getResources().getIdentifier("item" + (int) Math.round(Math.random() * 4), "drawable",
                    getPackageName()));
            item.setX(start_x + x_pos * item_size);
            item.setY(start_y + y_pos * item_size);
            ((ViewGroup) findViewById(R.id.game)).addView(item);
            items.add(item);
            crashed_array.add(0);

            x_pos++;
            if (x_pos == num_cols) {
                x_pos = 0;
                y_pos++;
            }

            // touch listener
            item.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (items_enabled) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                if (current_item == null) {
                                    current_item = v;
                                    near_item = null;

                                    // get touch coordinates
                                    x_pos = event.getX(0);
                                    y_pos = event.getY(0);
                                }
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if (current_item == v) {
                                    // get move delta
                                    float deltaX = event.getX(0) - x_pos;
                                    float deltaY = event.getY(0) - y_pos;

                                    // swipe
                                    if (deltaY < -DpToPx(5))
                                        near_item = get_near(v.getX(), v.getY() - item_size);
                                    else if (deltaX > DpToPx(5))
                                        near_item = get_near(v.getX() + item_size, v.getY());
                                    else if (deltaY > DpToPx(5))
                                        near_item = get_near(v.getX(), v.getY() + item_size);
                                    else if (deltaX < -DpToPx(5))
                                        near_item = get_near(v.getX() - item_size, v.getY());

                                    // change_items[
                                    if (near_item != null) {
                                        current_item.bringToFront();
                                        items_enabled = false;

                                        // create animation
                                        List<Animator> anim_list = new ArrayList<Animator>();
                                        anim_list.add(ObjectAnimator.ofFloat(near_item, "x", current_item.getX()));
                                        anim_list.add(ObjectAnimator.ofFloat(near_item, "y", current_item.getY()));
                                        anim_list.add(ObjectAnimator.ofFloat(current_item, "x", near_item.getX()));
                                        anim_list.add(ObjectAnimator.ofFloat(current_item, "y", near_item.getY()));

                                        // animation move
                                        anim = new AnimatorSet();
                                        anim.playTogether(anim_list);
                                        anim.setDuration(t_move);
                                        anim.addListener(new AnimatorListener() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                check_items();
                                            }

                                            @Override
                                            public void onAnimationCancel(Animator animation) {
                                            }

                                            @Override
                                            public void onAnimationRepeat(Animator animation) {
                                            }

                                            @Override
                                            public void onAnimationStart(Animator animation) {
                                                // sound
                                                if (!sp.getBoolean("mute", false) && isForeground)
                                                    sndpool.play(snd_move, 0.2f, 0.2f, 0, 0, 1);
                                            }
                                        });

                                        anim.start();
                                    }
                                }
                                 break;
                        }
                    }
                    return true;
                }

                class TouchableButton extends androidx.appcompat.widget.AppCompatButton {
                    public TouchableButton(Context context) {
                        super(context);
                    }

                    @SuppressLint("ClickableViewAccessibility")
                    @Override
                    public boolean performClick() {
                        // do what you want
                        return true;
                    }
                }

            });

        }


        check_items();
    }



    // check_items
    void check_items() {
        boolean another_item;
        List<Integer> temp_array;

        crash_exist = false;

        // gorizontal
        for (int i = 0; i < items.size(); i++) {
            temp_array = new ArrayList<Integer>();

            // before
            another_item = false;
            for (int j = -1; j >= -(num_cols - 1); j--) {
                for (int n = 0; n < items.size(); n++)
                    if (items.get(n).getX() == items.get(i).getX() + j * item_size && items.get(n).getY() == items.get(i).getY())
                        if (items.get(n).getDrawable().getConstantState().equals(items.get(i).getDrawable().getConstantState()))
                            temp_array.add(n);
                        else {
                            another_item = true;
                            break;
                        }

                if (another_item)
                    break;
            }

            temp_array.add(i);

            // after
            another_item = false;
            for (int j = 1; j <= (num_cols - 1); j++) {
                for (int n = 0; n < items.size(); n++)
                    if (items.get(n).getX() == items.get(i).getX() + j * item_size && items.get(n).getY() == items.get(i).getY())
                        if (items.get(n).getDrawable().getConstantState().equals(items.get(i).getDrawable().getConstantState()))
                            temp_array.add(n);
                        else {
                            another_item = true;
                            break;
                        }

                if (another_item)
                    break;
            }

            // crash exist
            if (temp_array.size() >= need_crashed) {
                crash_exist = true;
                if (!first_check)
                    score += 5;
                for (int n = 0; n < temp_array.size(); n++)
                    crashed_array.set(temp_array.get(n), 1);
            }
        }

        // vertical
        for (int i = 0; i < items.size(); i++) {
            temp_array = new ArrayList<Integer>();

            // before
            another_item = false;
            for (int j = -1; j >= -(num_rows - 1); j--) {
                for (int n = 0; n < items.size(); n++)
                    if (items.get(n).getX() == items.get(i).getX() && items.get(n).getY() == items.get(i).getY() + j * item_size)
                        if (items.get(n).getDrawable().getConstantState().equals(items.get(i).getDrawable().getConstantState()))
                            temp_array.add(n);
                        else {
                            another_item = true;
                            break;
                        }

                if (another_item)
                    break;
            }

            temp_array.add(i);

            // after
            another_item = false;
            for (int j = 1; j <= (num_rows - 1); j++) {
                for (int n = 0; n < items.size(); n++)
                    if (items.get(n).getX() == items.get(i).getX() && items.get(n).getY() == items.get(i).getY() + j * item_size)
                        if (items.get(n).getDrawable().getConstantState().equals(items.get(i).getDrawable().getConstantState()))
                            temp_array.add(n);
                        else {
                            another_item = true;
                            break;
                        }

                if (another_item)
                    break;
            }

            // crash exist
            if (temp_array.size() >= need_crashed) {
                crash_exist = true;
                if (!first_check)
                    score += 5;
                for (int n = 0; n < temp_array.size(); n++)
                    crashed_array.set(temp_array.get(n), 1);
            }
        }

        // crash exist
        if (crash_exist) {
            // create animation
            List<Animator> anim_list = new ArrayList<Animator>();
            for (int i = 0; i < items.size(); i++)
                if (crashed_array.get(i) == 1) {
                    anim_list.add(ObjectAnimator.ofFloat(items.get(i), "scaleX", 0f));
                    anim_list.add(ObjectAnimator.ofFloat(items.get(i), "scaleY", 0f));
                }

            // animation crush
            anim = new AnimatorSet();
            anim.playTogether(anim_list);
            anim.setDuration(t_effect);
            anim.addListener(new AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    update_items();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    // sound
                    if (!first_check && !sp.getBoolean("mute", false))
                        sndpool.play(snd_crush, 0.8f, 0.8f, 0, 0, 1);
                }
            });
            anim.start();
        } else if (current_item != null) { // move items back
            // animation move
            anim = new AnimatorSet();
            anim.playTogether(ObjectAnimator.ofFloat(near_item, "x", current_item.getX()),
                    ObjectAnimator.ofFloat(near_item, "y", current_item.getY()),
                    ObjectAnimator.ofFloat(current_item, "x", near_item.getX()),
                    ObjectAnimator.ofFloat(current_item, "y", near_item.getY()));
            anim.setDuration(t_move);
            anim.addListener(new AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    items_enabled = true;
                    current_item = null;
                    near_item = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    // sound
                    if (!sp.getBoolean("mute", false) && isForeground)
                        sndpool.play(snd_move, 0.2f, 0.2f, 0, 0, 1);
                }
            });
            anim.start();
        } else if (first_check) {
            // START
            show_section(R.id.game);
            first_check = false;
            t_effect = 200; // items scale crush speed
            items_enabled = true;
            h.postDelayed(TIMER, time * 1000);
        } else {
            crash_exist = false;
            items_enabled = true;
            current_item = null;
            near_item = null;
        }
    }

    // update_items
    void update_items() {
        // restart items
        for (int i = 0; i < items.size(); i++)
            if (crashed_array.get(i) == 1) {
                items.get(i).setScaleX(1f);
                items.get(i).setScaleY(1f);
                items.get(i).setImageResource(
                        getResources().getIdentifier("item" + (int) Math.round(Math.random() * 4), "drawable", getPackageName()));
                crashed_array.set(i, 0);

                // position
                for (int j = 1; j <= num_rows; j++) {
                    boolean can_move = true;

                    // all
                    for (int n = 0; n < items.size(); n++)
                        if (items.get(i).getX() == items.get(n).getX()
                                && items.get(n).getY() == start_y - j * item_size - item_size) {
                            can_move = false;
                            break;
                        }

                    // can_move
                    if (can_move) {
                        items.get(i).setY(start_y - j * item_size - item_size);
                        break;
                    }
                }
            }

        h.post(MOVE);
    }

    // MOVE
    Runnable MOVE = new Runnable() {
        @Override
        public void run() {
            boolean item_moved = false;

            // move items down
            for (int i = 0; i < items.size(); i++) {
                boolean can_move = true;

                // all
                for (int j = 0; j < items.size(); j++)
                    if (i != j)
                        if ((items.get(i).getX() == items.get(j).getX() && items.get(i).getY() + item_size == items.get(j).getY())
                                || items.get(i).getY() == start_y + item_size * (num_rows - 1)) {
                            can_move = false;
                            break;
                        }

                // can_move
                if (can_move) {
                    items.get(i).setY(items.get(i).getY() + item_size / move_step);
                    item_moved = true;
                }
            }

            if (item_moved)
                h.postDelayed(MOVE, 10);
            else {
                // all items moved
                current_item = null;
                check_items();
            }
        }
    };

    // TIMER
    Runnable TIMER = new Runnable() {
        @Override
        public void run() {
            anim.cancel();
            h.removeCallbacks(MOVE);
            items_enabled = false;
            current_item = null;
            near_item = null;

            // sound
            if (!sp.getBoolean("mute", false) && isForeground)
                sndpool.play(snd_info, 1f, 1f, 0, 0, 1);

            //h.postDelayed(STOP, 3000);
        }
    };

    // get_near
    View get_near(float x, float y) {
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).getX() == x && items.get(i).getY() == y)
                return items.get(i);

        return null;
    }

     //onClick
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
                START();
                break;
            case R.id.btn_exit:
                finishAffinity();
                break;
            case R.id.btn_privacy:
                Intent intent = new Intent(this, PrivacyPolicy.class);
                intent.putExtra("privpo", "privpol");
                startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        switch (current_section) {
            case R.id.main:
                super.onBackPressed();
                break;
            case R.id.result:
                show_section(R.id.main);
                break;
            case R.id.game:
                show_section(R.id.game);
                h.removeCallbacks(TIMER);
                h.removeCallbacks(MOVE);
                if (anim != null)
                    anim.cancel();
                break;
        }
    }

    // show_section
    void show_section(int section) {
        current_section = section;
        findViewById(R.id.main).setVisibility(View.GONE);
        findViewById(R.id.game).setVisibility(View.GONE);
        findViewById(R.id.result).setVisibility(View.GONE);
        findViewById(current_section).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(TIMER);
        h.removeCallbacks(MOVE);
        mp.release();
        sndpool.release();

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        isForeground = false;
        mp.setVolume(0, 0);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;

        if (!sp.getBoolean("mute", false) && isForeground)
            mp.setVolume(0.5f, 0.5f);
    }

    // DpToPx
    float DpToPx(float dp) {
        return (dp * Math.max(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels) / 540f);
    }

    // hide_navigation_bar
    void hide_navigation_bar() {
        // fullscreen mode
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            hide_navigation_bar();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100)
            if (resultCode == RESULT_OK) {
            }
                else { // sign fail

                }
    }
}

