/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.gltf;

import static java.util.concurrent.TimeUnit.SECONDS;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.ColorSpace;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.filament.gltfio.Animator;
import com.google.android.filament.gltfio.FilamentAsset;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.widget.TextView;
import android.view.View;
import android.graphics.Color;
import android.support.design.widget.FloatingActionButton;
import android.content.res.ColorStateList;
import android.support.v4.content.ContextCompat;

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
public class GltfActivity extends AppCompatActivity {
    private static final String TAG = GltfActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    private ModelRenderable bearRenderable, tigerRenderable, alligatorRenderable, penguinRenderable, pandaRenderable, ponyRenderable, wolfRenderable, raccoonRenderable;
    private TextView bear, tiger, alligator, penguin, panda, pony, wolf, raccoon;
    private View[] arrayView;
    private int selected = 1;
    private FloatingActionButton animationButton;
    private boolean hasAnimation = false;
    private MediaPlayer player;

    private static class AnimationInstance {
        Animator animator;
        Long startTime;
        float duration;
        int index;

        AnimationInstance(Animator animator, int index, Long startTime) {
            this.animator = animator;
            this.startTime = startTime;
            this.duration = animator.getAnimationDuration(index);
            this.index = index;
        }
    }

    private final Set<AnimationInstance> animators = new ArraySet<>();

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_ux);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        // Model
        setupModel();

        // View
        bear = (TextView) findViewById(R.id.bear);
        tiger = (TextView) findViewById(R.id.tiger);
        alligator = (TextView) findViewById(R.id.alligator);
        penguin = (TextView) findViewById(R.id.penguin);
        panda = (TextView) findViewById(R.id.panda);
        pony = (TextView) findViewById(R.id.pony);
        wolf = (TextView) findViewById(R.id.wolf);
        raccoon = (TextView) findViewById(R.id.raccoon);

        arrayView = new View[]{
                bear, tiger, alligator, penguin, panda, pony, wolf, raccoon
        };
        setClickListener();

        // Animation Button
        animationButton = findViewById(R.id.animate);
        animationButton.setOnClickListener(this::onPlayAnimation);

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    // Create the transformable model and add it to the anchor.
                    TransformableNode model = new TransformableNode(arFragment.getTransformationSystem());
                    model.setParent(anchorNode);
                    model.getScaleController().setMinScale(0.4f);
                    model.getScaleController().setMaxScale(0.7f);

                    if (selected == 1) {
                        model.setRenderable(bearRenderable);
                        addName(anchorNode, model, "Bear\n곰");
                    } else if (selected == 2) {
                        model.setRenderable(tigerRenderable);
                        addName(anchorNode, model, "Tiger\n호랑이");
                    } else if (selected == 3) {
                        model.setRenderable(alligatorRenderable);
                        addName(anchorNode, model, "Alligator\n악어");
                    } else if (selected == 4) {
                        model.setRenderable(penguinRenderable);
                        addName(anchorNode, model, "Penguin\n펭귄");
                    } else if (selected == 5) {
                        model.setRenderable(pandaRenderable);
                        addName(anchorNode, model, "Panda\n판다");
                    } else if (selected == 6) {
                        model.setRenderable(ponyRenderable);
                        addName(anchorNode, model, "Pony\n조랑말");
                    } else if (selected == 7) {
                        model.setRenderable(wolfRenderable);
                        addName(anchorNode, model, "Wolf\n늑대");
                    } else if (selected == 8) {
                        model.setRenderable(raccoonRenderable);
                        addName(anchorNode, model, "Raccoon\n라쿤");
                    }

                    model.select();

                    FilamentAsset filamentAsset = model.getRenderableInstance().getFilamentAsset();
                    if (filamentAsset.getAnimator().getAnimationCount() > 0) {
                        animators.add(new AnimationInstance(filamentAsset.getAnimator(), 0, System.nanoTime()));
                    }

                });
    }

    private void setupModel() {
        WeakReference<GltfActivity> weakActivity = new WeakReference<>(this);

        ModelRenderable.builder()
                .setSource(
                        this,
                        Uri.parse(
                                "https://storage.googleapis.com/ar-answers-in-search-models/static/BrownBear/model.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            GltfActivity activity = weakActivity.get();
                            if (activity != null) {
                                activity.bearRenderable = modelRenderable;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Bear renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        ModelRenderable.builder()
                .setSource(
                        this,
                        Uri.parse(
                                "https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            GltfActivity activity = weakActivity.get();
                            if (activity != null) {
                                activity.tigerRenderable = modelRenderable;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Tiger renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        ModelRenderable.builder()
                .setSource(
                        this,
                        Uri.parse(
                                "https://storage.googleapis.com/ar-answers-in-search-models/static/Alligator/model.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            GltfActivity activity = weakActivity.get();
                            if (activity != null) {
                                activity.alligatorRenderable = modelRenderable;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Alligator renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        ModelRenderable.builder()
                .setSource(
                        this,
                        Uri.parse(
                                "https://storage.googleapis.com/ar-answers-in-search-models/static/EmperorPenguin/model.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            GltfActivity activity = weakActivity.get();
                            if (activity != null) {
                                activity.penguinRenderable = modelRenderable;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Penguin renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        ModelRenderable.builder()
                .setSource(
                        this,
                        Uri.parse(
                                "https://storage.googleapis.com/ar-answers-in-search-models/static/GiantPanda/model.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            GltfActivity activity = weakActivity.get();
                            if (activity != null) {
                                activity.pandaRenderable = modelRenderable;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Panda renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        ModelRenderable.builder()
                .setSource(
                        this,
                        Uri.parse(
                                "https://storage.googleapis.com/ar-answers-in-search-models/static/Pony/model.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            GltfActivity activity = weakActivity.get();
                            if (activity != null) {
                                activity.ponyRenderable = modelRenderable;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Pony renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        ModelRenderable.builder()
                .setSource(
                        this,
                        Uri.parse(
                                "https://storage.googleapis.com/ar-answers-in-search-models/static/TimberWolf/model.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            GltfActivity activity = weakActivity.get();
                            if (activity != null) {
                                activity.wolfRenderable = modelRenderable;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load wolf renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        ModelRenderable.builder()
                .setSource(
                        this,
                        Uri.parse(
                                "https://storage.googleapis.com/ar-answers-in-search-models/static/Raccoon/model.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            GltfActivity activity = weakActivity.get();
                            if (activity != null) {
                                activity.raccoonRenderable = modelRenderable;
                            }
                        })
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load Raccoon renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });
    }

    private void setClickListener() {
        for (int i = 0; i < arrayView.length; i++) {
            arrayView[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view.getId() == R.id.bear) {
                        selected = 1;
                    } else if (view.getId() == R.id.tiger) {
                        selected = 2;
                    } else if (view.getId() == R.id.alligator) {
                        selected = 3;
                    } else if (view.getId() == R.id.penguin) {
                        selected = 4;
                    } else if (view.getId() == R.id.panda) {
                        selected = 5;
                    } else if (view.getId() == R.id.pony) {
                        selected = 6;
                    } else if (view.getId() == R.id.wolf) {
                        selected = 7;
                    } else if (view.getId() == R.id.raccoon) {
                        selected = 8;
                    }

                    setBackground(view.getId());
                }
            });
        }
    }

    private void setBackground(int id) {
        for (int i = 0; i < arrayView.length; i++) {
            if (arrayView[i].getId() == id) {
                arrayView[i].setBackgroundColor(Color.parseColor("#80333639"));
            } else {
                arrayView[i].setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    private void addName(AnchorNode anchorNode, TransformableNode model, String name) {
        Node titleNode = new Node();
        titleNode.setParent(model);
        titleNode.setEnabled(false);

        if (selected == 1) {
            titleNode.setLocalPosition(new Vector3(0.0f, 1.6f, 0.0f));
        } else if (selected == 2) {
            titleNode.setLocalPosition(new Vector3(0.0f, 1.1f, 0.0f));
        } else if (selected == 3) {
            titleNode.setLocalPosition(new Vector3(0.0f, 0.7f, 0.0f));
        } else if (selected == 4) {
            titleNode.setLocalPosition(new Vector3(0.0f, 1.2f, 0.0f));
        } else if (selected == 5) {
            titleNode.setLocalPosition(new Vector3(0.0f, 1.4f, 0.0f));
        } else if (selected == 6) {
            titleNode.setLocalPosition(new Vector3(0.0f, 1.2f, 0.0f));
        } else if (selected == 7) {
            titleNode.setLocalPosition(new Vector3(0.0f, 1.3f, 0.0f));
        } else if (selected == 8) {
            titleNode.setLocalPosition(new Vector3(0.0f, 0.5f, 0.0f));
        }

        ViewRenderable.builder()
                .setView(this, R.layout.tiger_card_view)
                .build()
                .thenAccept(
                        (renderable) -> {
                            titleNode.setRenderable(renderable);
                            titleNode.setEnabled(true);

                            // Set Text
                            TextView txt_name = (TextView) renderable.getView();
                            txt_name.setText(name);

                            // Click to text view to remove animal
                            txt_name.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    anchorNode.setParent(null);
                                }
                            });

                        })
                .exceptionally(
                        (throwable) -> {
                            throw new AssertionError("Could not load card view.", throwable);
                        }
                );


    }

    private void onPlayAnimation(View unusedView) {
        if (hasAnimation) {
            // Stop Music
            if (player != null) {
                player.pause();
            }

            // Stop Animation
            arFragment
                    .getArSceneView()
                    .getScene()
                    .addOnUpdateListener(
                            frameTime -> {
                                Long time = System.nanoTime();
                                for (AnimationInstance animator : animators) {
                                    animator.animator.applyAnimation(
                                            animator.index, 0);
                                    animator.animator.updateBoneMatrices();
                                }
                            });

            animationButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimaryDark)));

            hasAnimation = false;
        } else {
            // Start Music
            if (player == null) {
                player = MediaPlayer.create(this, R.raw.daftpunk);

            }
            player.start();

            // Start Animation
            arFragment
                    .getArSceneView()
                    .getScene()
                    .addOnUpdateListener(
                            frameTime -> {
                                Long time = System.nanoTime();
                                for (AnimationInstance animator : animators) {
                                    animator.animator.applyAnimation(
                                            animator.index,
                                            (float) ((time - animator.startTime) / (double) SECONDS.toNanos(1))
                                                    % animator.duration);
                                    animator.animator.updateBoneMatrices();
                                }
                            });

            animationButton.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimary)));


            hasAnimation = true;
        }
    }

    private void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
            Toast.makeText(this, "MediaPlayer released", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayer();
    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
            Log.e(TAG, "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
