package com.lastlight.project;

import java.util.ArrayList;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Main extends ApplicationAdapter {

    // =====================
    // HUD / Instructions
    // =====================
    private final String controlInstructions = "Controls: LMB - Flashlight | F - Interact | Shift - Sprint";
    private String objectiveInstruction = "Finish all generators!";

    // ======================
    // CORE RENDERING
    // ======================
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private OrthographicCamera camera, hudCamera;

    // ======================
    // TEXTURES
    // ======================
    private Texture wallTexture, floorTexture, trapTexture, droneTexture, playerTexture;
    private Texture generatorTexture, doorClosedTexture, doorOpenTexture;
    private Texture lightMask;
    private Texture victoryTexture, gameOverTexture;

    // ======================
    // AUDIO
    // ======================
    private Sound lightExpandSound, lightShrinkSound, generatorFixSound, doorOpenSound, victorySound, defeatSound;

    // ======================
    // ENTITIES
    // ======================
    private Player player;
    private final ArrayList<Drone> drones = new ArrayList<>();
    private final ArrayList<Trap> traps = new ArrayList<>();
    private final ArrayList<Generator> generators = new ArrayList<>();
    private final ArrayList<Door> doors = new ArrayList<>();

    // ======================
    // GAME CONSTANTS
    // ======================
    private final float tileSize = 64f;

    private final float minLightRadius = 128f;
    private final float maxLightRadius = 512f;
    private float currentLightRadius = minLightRadius;
    private final float lightGrowSpeed = 64f;
    private final float lightShrinkSpeed = 64f;

    private float battery = 100f;
    private final float batteryDrainRate = 10f;
    private final float batteryRechargeRate = 20f;
    private boolean flashlightLocked = false;
    private float flashlightLockTimer = 0f;
    private final float flashlightLockDuration = 2f;

    // ======================
    // GAME STATE
    // ======================
    private BitmapFont font;
    private boolean gameWon = false;
    private boolean gameOver = false;
    private float victoryTimer = 0f;
    private float gameOverTimer = 0f;
    private boolean wasLightHeld = false;
    private int lastCompletedGenerators = 0;

    // ======================
    // MAP
    // ======================
    private final String[][] map = {
        {"W","W","W","W","W","W","W","W","W","W","W","W","W","W","W","W","W","W","W","W"},
        {"W","F","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","F","F","W"},
        {"W","F","F","F","W","W","W","W","F","G","F","F","W","W","W","W","F","F","F","W"},
        {"W","F","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","F","G","W"},
        {"W","F","F","F","W","W","W","W","F","F","T","F","W","W","W","W","F","F","F","W"},
        {"W","F","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","F","F","W"},
        {"W","F","P","F","F","F","F","F","F","F","F","F","W","W","W","W","F","F","F","W"},
        {"W","F","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","F","F","W"},
        {"W","F","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","D","F","W"},
        {"W","F","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","F","F","W"},
        {"W","F","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","F","F","W"},
        {"W","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","W"},
        {"W","F","F","F","F","F","F","F","G","F","F","F","F","F","F","F","F","F","F","W"},
        {"W","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","W"},
        {"W","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","F","W"},
        {"W","F","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","F","F","W"},
        {"W","F","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","F","F","W"},
        {"W","F","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","F","F","W"},
        {"W","E","F","F","W","W","W","W","F","F","F","F","W","W","W","W","F","F","E","W"},
        {"W","W","W","W","W","W","W","W","W","W","W","W","W","W","W","W","W","W","W","W"}
    };

    private int totalGenerators;

    // ======================
    // CREATE
    // ======================
    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        camera = new OrthographicCamera(800, 480);
        camera.zoom = 0.75f;
        camera.update();

        hudCamera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        hudCamera.setToOrtho(false);
        hudCamera.update();

        font = new BitmapFont();
        font.setColor(1, 1, 0, 1);

        loadTextures();
        loadSounds();
        lightMask = createRadialLightMask(8192, minLightRadius);

        spawnEntities();
        totalGenerators = generators.size();

        // Add doors manually (or via map if needed)
        doors.add(new Door(new Vector2(tileSize, (map.length - 2) * tileSize), doorClosedTexture, doorOpenTexture, tileSize));
        doors.add(new Door(new Vector2(18 * tileSize, tileSize), doorClosedTexture, doorOpenTexture, tileSize));
    }

    private void loadTextures() {
        wallTexture = new Texture("Wall.png");
        floorTexture = new Texture("Floor.png");
        trapTexture = new Texture("Trap.png");
        droneTexture = new Texture("Drone.png");
        playerTexture = new Texture("Player.png");
        generatorTexture = new Texture("Generator.png");
        doorClosedTexture = new Texture("DoorClosed.png");
        doorOpenTexture = new Texture("DoorOpen.png");
        victoryTexture = new Texture("VictoryScreen.jpg");
        gameOverTexture = new Texture("GameOverScreen.jpg");
    }

    private void loadSounds() {
        lightExpandSound = Gdx.audio.newSound(Gdx.files.internal("light_on.mp3"));
        lightShrinkSound = Gdx.audio.newSound(Gdx.files.internal("light_off.mp3"));
        generatorFixSound = Gdx.audio.newSound(Gdx.files.internal("generator_fix.wav"));
        doorOpenSound = Gdx.audio.newSound(Gdx.files.internal("door_open.mp3"));
        victorySound = Gdx.audio.newSound(Gdx.files.internal("victory.wav"));
        defeatSound = Gdx.audio.newSound(Gdx.files.internal("defeat.wav"));
    }

    // ======================
    // RENDER LOOP
    // ======================
    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        if (!gameOver && !gameWon) {
            player.update(delta, map, generators, doors);
            for (Drone d : drones) d.update(delta, map, traps, trapTexture, player);

            // Check collisions
            checkPlayerCollisions();

            // Update doors
            updateDoors();

            // Check if player steps on open door
            checkPlayerOnDoors();
        }

        updateCamera();
        renderWorld();
        updateFlashlight(delta);
        renderDarkness();
        updateProgressSounds();
        renderProgressBars();
        renderHUD();
        renderEndScreens(delta);
    }

    // ======================
    // DOOR LOGIC
    // ======================
    private void updateDoors() {
        boolean allGeneratorsDone = getCompletedGenerators() == totalGenerators;
        float delta = Gdx.graphics.getDeltaTime();

        for (Door d : doors) {
            // Doors become interactable only after all generators done
            d.setOpenable(allGeneratorsDone);

            // Update progress if player is near and pressing action
            if (d.isOpenable() && !d.isOpen() && player.getInteractingDoor() == d) {
                d.progress(delta);

                // Play sound when door just opens
                if (d.isOpen()) {
                    doorOpenSound.play(0.8f);
                }
            }
        }
    }


    private void checkPlayerOnDoors() {
        for (Door d : doors) {
            if (d.isOpen() && isPlayerOnDoor(d)) {
                gameWon = true;
                victoryTimer = 0f;
                victorySound.play(0.8f);
            }
        }
    }

    private boolean isPlayerOnDoor(Door door) {
        float px = player.position.x + player.size / 2f;
        float py = player.position.y + player.size / 2f;
        float dx = door.position.x;
        float dy = door.position.y;

        return px > dx && px < dx + door.size && py > dy && py < dy + door.size;
    }

    // ======================
    // RENDER HELPERS
    // ======================
    private void renderWorld() {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                Texture t = map[y][x].equals("W") ? wallTexture : floorTexture;
                batch.draw(t, x * tileSize, (map.length - y - 1) * tileSize, tileSize, tileSize);
            }
        }
        for (Generator g : generators) g.render(batch);
        for (Door d : doors) d.render(batch);
        for (Trap t : traps) t.render(batch);
        for (Drone d : drones) d.render(batch);
        player.render(batch);
        batch.end();
    }

    private void renderProgressBars() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        boolean allGeneratorsDone = getCompletedGenerators() == totalGenerators;

        // Generators
        for (Generator g : generators) g.renderProgressBar(shapeRenderer);

        // Doors
        for (Door d : doors) d.renderProgressBar(shapeRenderer);

        shapeRenderer.end();
    }

    private void renderHUD() {
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        // --- Top Left: Controls ---
        font.setColor(1, 1, 1, 1);
        font.draw(batch, controlInstructions, 20, Gdx.graphics.getHeight() - 20);

        // --- Top Right: Generator Counter ---
        String genText = "Generators: " + getCompletedGenerators() + "/" + totalGenerators;
        GlyphLayout gl = new GlyphLayout(font, genText);
        font.draw(batch, genText, Gdx.graphics.getWidth() - gl.width - 20, Gdx.graphics.getHeight() - 20);

        // --- Top Right: Objective Instructions ---
        if (getCompletedGenerators() < totalGenerators) {
            objectiveInstruction = "Finish all generators!";
        } else {
            objectiveInstruction = "Exits can now be interacted, open them and escape!";
        }

        GlyphLayout gl2 = new GlyphLayout(font, objectiveInstruction);
        font.draw(batch, objectiveInstruction,
                Gdx.graphics.getWidth() - gl2.width - 20,
                Gdx.graphics.getHeight() - 40); // slightly below generator counter

        batch.end();

        // --- Battery bar ---
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1);
        shapeRenderer.rect(20, 20, 150, 15);
        shapeRenderer.setColor(0.2f, 1f, 0.2f, 1);
        shapeRenderer.rect(20, 20, 150 * (battery / 100f), 15);
        shapeRenderer.end();
    }

    private void renderDarkness() {
        batch.setBlendFunction(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();

        float scale = currentLightRadius / maxLightRadius;
        float w = lightMask.getWidth() * scale * 2f;
        float h = lightMask.getHeight() * scale * 2f;

        float x = player.position.x + player.size / 2f - w / 2f;
        float y = player.position.y + player.size / 2f - h / 2f;

        batch.draw(lightMask, x, y, w, h);
        batch.end();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private void renderEndScreens(float delta) {
        if (gameWon) {
            victoryTimer += delta;
            batch.begin();
            batch.draw(victoryTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.end();
            if (victoryTimer > 3f) Gdx.app.exit();
        }

        if (gameOver) {
            gameOverTimer += delta;

            batch.setProjectionMatrix(hudCamera.combined);
            batch.begin();
            // Draw background image
            batch.draw(gameOverTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            // Draw centered text
            String deathText = "YOU DIED";
            GlyphLayout layout = new GlyphLayout(font, deathText);
            float x = (Gdx.graphics.getWidth() - layout.width) / 2f;
            float y = (Gdx.graphics.getHeight() + layout.height) / 2f; 
            font.getData().setScale(2f);
            font.setColor(255, 0, 0, 1); 
            font.draw(batch, layout, x, y);

            batch.end();

            if (gameOverTimer > 3f) Gdx.app.exit();
        }
    }

    // ======================
    // FLASHLIGHT
    // ======================
    private void updateFlashlight(float delta) {
        if (flashlightLocked) {
            flashlightLockTimer -= delta;
            if (flashlightLockTimer <= 0) flashlightLocked = false;
        }

        boolean held = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        if (!flashlightLocked && held && battery > 0) {
            currentLightRadius = Math.min(maxLightRadius, currentLightRadius + lightGrowSpeed * delta);
            battery = Math.max(0, battery - batteryDrainRate * delta);

            if (battery == 0) {
                flashlightLocked = true;
                flashlightLockTimer = flashlightLockDuration;
            }
        } else {
            currentLightRadius = Math.max(minLightRadius, currentLightRadius - lightShrinkSpeed * delta);
            if (!flashlightLocked)
                battery = Math.min(100, battery + batteryRechargeRate * delta);
        }

        if (held && !wasLightHeld) lightExpandSound.play(0.5f);
        if (!held && wasLightHeld) lightShrinkSound.play(0.4f);
        wasLightHeld = held;
    }

    private void updateProgressSounds() {
        int completed = getCompletedGenerators();
        if (completed > lastCompletedGenerators) generatorFixSound.play(0.7f);
        lastCompletedGenerators = completed;
    }

    // ======================
    // CAMERA
    // ======================
    private void updateCamera() {
        camera.position.set(player.position.x + player.size / 2f, player.position.y + player.size / 2f, 0);
        float hw = camera.viewportWidth * camera.zoom / 2f;
        float hh = camera.viewportHeight * camera.zoom / 2f;
        camera.position.x = MathUtils.clamp(camera.position.x, hw, map[0].length * tileSize - hw);
        camera.position.y = MathUtils.clamp(camera.position.y, hh, map.length * tileSize - hh);
        camera.update();
    }

    // ======================
    // ENTITY SPAWN
    // ======================
    private void spawnEntities() {
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[y].length; x++) {
                String tile = map[y][x];
                Vector2 pos = new Vector2(x * tileSize, (map.length - y - 1) * tileSize);

                switch (tile) {
                    case "P":
                        player = new Player(pos, playerTexture, tileSize);
                        break;
                    case "D":
                        drones.add(new Drone(pos, droneTexture, tileSize));
                        break;
                    case "T":
                        traps.add(new Trap(pos, trapTexture, tileSize));
                        break;
                    case "G":
                        generators.add(new Generator(pos, generatorTexture, tileSize));
                        break;
                }
            }
        }
    }

    private Texture createRadialLightMask(int size, float radius) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float c = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float d = Vector2.dst(x, y, c, c);
                float a = MathUtils.clamp(d / radius, 0, 1);
                pm.setColor(0, 0, 0, a);
                pm.drawPixel(x, y);
            }
        }
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    private int getCompletedGenerators() {
        int c = 0;
        for (Generator g : generators) if (g.isCompleted()) c++;
        return c;
    }

    private void checkPlayerCollisions() {
        // --- Traps ---
        for (Trap t : traps) {
            if (player.position.x + player.size > t.position.x &&
                player.position.x < t.position.x + t.size &&
                player.position.y + player.size > t.position.y &&
                player.position.y < t.position.y + t.size) {
                triggerGameOver();
                return;
            }
        }

        // --- Drones ---
        for (Drone d : drones) {
            if (player.position.x + player.size > d.position.x &&
                player.position.x < d.position.x + d.size &&
                player.position.y + player.size > d.position.y &&
                player.position.y < d.position.y + d.size) {
                triggerGameOver();
                return;
            }
        }
    }


    public void triggerGameOver() {
        if (!gameOver) {
            gameOver = true;
            gameOverTimer = 0f;
            defeatSound.play(0.8f); 
        }
    }


    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        wallTexture.dispose();
        floorTexture.dispose();
        trapTexture.dispose();
        droneTexture.dispose();
        playerTexture.dispose();
        generatorTexture.dispose();
        doorClosedTexture.dispose();
        doorOpenTexture.dispose();
        lightMask.dispose();
        font.dispose();
        victoryTexture.dispose();
        gameOverTexture.dispose();
        lightExpandSound.dispose();
        lightShrinkSound.dispose();
        generatorFixSound.dispose();
        doorOpenSound.dispose();
        victorySound.dispose();
        defeatSound.dispose();
    }
}
