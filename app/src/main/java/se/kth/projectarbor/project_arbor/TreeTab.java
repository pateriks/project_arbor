package se.kth.projectarbor.project_arbor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.*;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import se.kth.projectarbor.project_arbor.view_objects.CloudView;
import se.kth.projectarbor.project_arbor.view_objects.RainView;
import se.kth.projectarbor.project_arbor.view_objects.SunView;
import se.kth.projectarbor.project_arbor.weather.Environment;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Fredrik Pihlqvist on 2017-04-28.
 */

public class TreeTab extends Fragment {

    private final static String TAG = "ARBOR_TREE_TAB";

    // Declaring all views and buttons
    private ToggleButton walkBtn;
    private TextView treeView;
    private View view;

    private SunView sunView;
    private RainView rainView;
    private CloudView cloudView;
    private TextView distanceTextView;
    private TextView stepTextView;
    private ImageView ivTree;
    private View sessionView;

    private Animation animAppear;
    private Animation animDisappear;


    private RelativeLayout weatherLayout;
    private Environment.Weather weather;
    private SharedPreferences sharedPreferences;

    private int currentPhase;
    private int newPhase;

    /* // TODO: See so it works after integrish
    //TODO:Fix messages (Ramcin)
    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            Log.d(TAG, "onReceive()");
            if (intent.getAction().equals(Pedometer.DISTANCE_BROADCAST)) {
                stepTextView.setText(String.format("%d", extras.getInt("STEPCOUNT")));
                distanceTextView.setText(String.format("%.2f km",extras.getDouble("DISTANCE")/1000));
            } else if (intent.getAction().equals(MainService.TREE_DATA)) {
                Log.d(TAG, "TREE_DATA");
                newPhase = ((Tree.Phase) extras.get("PHASE")).getPhaseNumber();
                if (newPhase != currentPhase) {
                    setTreePhase(newPhase);
                }
            } else if (intent.getAction().equals(MainService.WEATHER_DATA)) {
                Log.d("ARBOR_WEATHER", "Broadcast received");
                // Build new weather layout depending on weather
                // TODO: Here only becuse WEATHER_DATA is not done (Fredrik)
                Environment.Weather newWeather = (Environment.Weather) extras.get("WEATHER");
                if (true) { // TODO: change to goodie (Fredrik)
                    weather = newWeather;
                    RelativeLayout layout = (RelativeLayout) view;
                    layout.removeView(weatherLayout);
                    setWeatherLayout();
                    layout.addView(weatherLayout);
                    view = layout;
                }
            }


        }
    }
    */

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.view = inflater.inflate(R.layout.fragment_tree_tab, container, false);

        /* // TODO: See if it works after integration
        // Setup a filter for views
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainService.WEATHER_DATA);
        filter.addAction(Pedometer.DISTANCE_BROADCAST);
        filter.addAction(MainService.TREE_DATA);
        getActivity().registerReceiver(this.new Receiver(), filter);
        */

        sharedPreferences = getActivity().getSharedPreferences("se.kth.projectarbor.project_arbor"
        , MODE_PRIVATE);

        // looks for the last used phase number
        if (sharedPreferences.contains("CURRENT_TREE_PHASE")) {
            currentPhase = sharedPreferences.getInt("CURRENT_TREE_PHASE", 1);
        } else {
            sharedPreferences.edit().putInt("CURRENT_TREE_PHASE", 1).apply();
            currentPhase = 1;
        }

        // Setup Views
        treeView = (TextView) view.findViewById(R.id.tvTree);
        ivTree = (ImageView) view.findViewById(R.id.treeButton);
        distanceTextView = (TextView) view.findViewById(R.id.tvDistance);
        stepTextView = (TextView) view.findViewById(R.id.tvStepCount);
        sessionView = view.findViewById(R.id.sessionView);
        sessionView.setVisibility(View.GONE);

        // Pick the right tree depending on the current Phase
        setTreePhase(currentPhase);

        // Get first information about weather
        Intent intent = getActivity().getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            treeView.setText("Tree, Phase: " + ((Tree.Phase) extras.get("PHASE")).getPhaseName());
            newPhase = ((Tree.Phase) extras.get("PHASE")).getPhaseNumber();
        }

        // If the tree's phase changed it will start an animation if you press it
        if (currentPhase < newPhase) {
            //treePhaseChange();
        }

        // Change weather view depending on "weather"
        weatherLayout = new RelativeLayout(getContext());
        RelativeLayout currentLayout = (RelativeLayout) view.findViewById(R.id.treefragmentlayout);
        currentLayout.addView(weatherLayout);
        view = currentLayout;

        // Sends message to MainService and asks for weather
        if (weather == null) {
            getActivity().startService(new Intent(getActivity(), MainService.class)
                    .putExtra("MESSAGE_TYPE", MainService.MSG_UPDATE_WEATHER_VIEW));
        }


        // Animations for session
        animAppear = AnimationUtils.loadAnimation(getContext(), R.anim.session_appear);
        animDisappear = AnimationUtils.loadAnimation(getContext(), R.anim.session_disappear);
        animDisappear.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                sessionView.setVisibility(View.GONE);
                distanceTextView.setText("0 km");
                stepTextView.setText("0");
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });


        // The user can toggle to either collect "distance" or not
        walkBtn = (ToggleButton) view.findViewById(R.id.toggleButton);
        walkBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled

                    // Animate session View
                    sessionView.setVisibility(View.VISIBLE);
                    sessionView.startAnimation(animAppear);

                    Intent intent = new Intent(getActivity(), MainService.class);
                    intent.putExtra("MESSAGE_TYPE", MainService.MSG_START);
                    getActivity().startService(intent);
                } else {
                    // The toggle is disabled

                    // Animate session View
                    sessionView.startAnimation(animDisappear);

                    Intent intent = new Intent(getActivity(), MainService.class);
                    intent.putExtra("MESSAGE_TYPE", MainService.MSG_STOP);
                    getActivity().startService(intent);
                }

                sharedPreferences.edit().putBoolean("TOGGLE", isChecked).apply();

            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "RESUME");


        // Remember toggle button state
        if (sharedPreferences.contains("TOGGLE")) {
            walkBtn.setChecked(sharedPreferences.getBoolean("TOGGLE", false));
            if(sharedPreferences.getBoolean("TOGGLE", false)) {
                Intent intent2 = new Intent(getActivity(), MainService.class);
                // TODO: Was "MSG_RESUME_HEAVY" before. Dint update stats tab correctly (Fredrik)
                intent2.putExtra("MESSAGE_TYPE", MainService.MSG_RESUME_LIGHT);
                getActivity().startService(intent2);
            }else{
                Intent intent3 = new Intent(getActivity(), MainService.class);
                intent3.putExtra("MESSAGE_TYPE", MainService.MSG_RESUME_LIGHT);
                getActivity().startService(intent3);
            }
        }

    }

    // Applying the right weather layout depending on IRL weather
    void setWeatherLayout() {
        RelativeLayout layout = new RelativeLayout(getContext());

        switch (weather) {
            case CLOUDY:
                cloudView = new CloudView(getContext());
                layout = cloudView.addViews(layout);
                break;
            case SUN:
                sunView = new SunView(getActivity());
                layout = (RelativeLayout) sunView.addViews(layout);
                break;
            case RAIN:
                rainView = new RainView(getActivity());
                layout = (RelativeLayout) rainView.addViews(layout);
                break;

            // TODO: Fix later when its implemented in Environment (Fredrik)
            // TODO: Does it work as intended?
            case PARTLY_CLOUDY:
                SunView sunView = new SunView(getActivity());
                CloudView cloudView = new CloudView(getContext());
                layout = cloudView.addViews((RelativeLayout) sunView.addViews(layout));
                break;
            default:
                Log.d(TAG, "no case in weather switch");
        }

        weatherLayout = layout;
    }

    // TODO: Fix the names
    void setWeather(Environment.Weather newWeather) {
        weather = newWeather;
    }

    ViewGroup getWeatherLayout() {
        return weatherLayout;
    }

    View getTabView() {
        return view;
    }

    void setTabView(View newView) {
        view = newView;
    }

    TextView getDistanceView() {
        return distanceTextView;
    }

    TextView getStepView() {
        return stepTextView;
    }



    // Shows the right tree
    private void setTreePhase(int phaseNumber) {
        Log.d(TAG, "setTreePhase");
        switch (phaseNumber) {
            case 1:
                ivTree = (ImageView) view.findViewById(R.id.treeButton);
                ivTree.setImageResource(R.drawable.seed_to_sprout_01);
                Log.d(TAG, "ivTree seed");
                break;
            case 2:
                ivTree = (ImageView) view.findViewById(R.id.treeButton);
                ivTree.setImageResource(R.drawable.sprout_to_sapling_01);
                Log.d(TAG, "ivTree sprout");
                break;
            case 3:
                ivTree = (ImageView) view.findViewById(R.id.treeButton);
                ivTree.setImageResource(R.drawable.sprout_to_sapling_29);
                Log.d(TAG, "ivTree sapling");
                break;
        }
    }

    // TODO: FIX BACKGROUND RESOURCE, CURRENTLY USING ALOT OF MEMORY
    private void treePhaseChange() {
        switch (newPhase) {
            case 2:
                ivTree.setBackgroundResource(R.drawable.anim_seed_to_sprout);
                ivTree.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AnimationDrawable frameAnim = (AnimationDrawable) ivTree.getBackground();
                        frameAnim.start();
                        currentPhase = newPhase;
                    }
                });
                break;

            case 3:
                ivTree.setBackgroundResource(R.drawable.grow_sprout_to_sapling);
                ivTree.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AnimationDrawable frameAnim = (AnimationDrawable) ivTree.getBackground();
                        frameAnim.start();
                        currentPhase = newPhase;
                    }
                });
                break;
        }
    }

}

