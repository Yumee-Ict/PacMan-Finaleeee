package p;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

// Loads sound clips and starts, stops, and adjusts their volume.
class SoundManager {

    // Each sound name is paired with the file that contains its audio.
    private enum Sound {
        PLAY_THE_GAME("PlayTheGame.wav"),
        FAILED("Failed (2).wav"),
        CONGRATULATIONS("CONGRATULATION.wav");

        private final String fileName;

        Sound(String fileName) {
            this.fileName = fileName;
        }
    }

    private static final String SOUND_FOLDER = "p";

    private final Map<Sound, Clip> clips = new EnumMap<>(Sound.class);

    private boolean gameMusicWanted = false;

    // Load available clips and subscribe to changes in the audio settings.
    SoundManager() {
        for (Sound sound : Sound.values()) {
            Clip clip = load(sound);
            if (clip != null) {
                clips.put(sound, clip);
            }
        }

        applyVolume();
        AudioSettings.addChangeListener(() -> {
            applyVolume();
            applyMusicSetting();
            if (!AudioSettings.isFailedSoundEnabled()) {
                stop(Sound.FAILED);
            }
            if (!AudioSettings.isCongratulationsSoundEnabled()) {
                stop(Sound.CONGRATULATIONS);
            }
        });
    }

    // Start or stop background music to match the player's music preference.
    private void applyMusicSetting() {
        Clip music = clips.get(Sound.PLAY_THE_GAME);
        if (music == null) {
            return;
        }
        if (!AudioSettings.isMusicEnabled()) {
            if (music.isRunning()) {
                music.stop();
            }
        } else if (gameMusicWanted && !music.isRunning()) {
            music.setFramePosition(0);
            music.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    // Convert the selected volume to the audio system's decibel scale.
    private void applyVolume() {
        double volume = AudioSettings.getEffectiveVolume();
        for (Clip clip : clips.values()) {
            if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                continue;
            }
            FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float decibels;
            if (volume <= 0.0) {
                decibels = gain.getMinimum();
            } else {
                decibels = (float) (20.0 * Math.log10(volume));
            }
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
        }
    }

    // Start looping game music unless it is disabled or already playing.
    void startGameMusic() {
        gameMusicWanted = true;
        Clip music = clips.get(Sound.PLAY_THE_GAME);
        if (music == null || !AudioSettings.isMusicEnabled() || music.isRunning()) {
            return;
        }
        music.setFramePosition(0);
        music.loop(Clip.LOOP_CONTINUOUSLY);
    }

    // Stop other audio, then play the game-over sound if it is enabled.
    void playFailed() {
        stopAll();
        if (AudioSettings.isFailedSoundEnabled()) {
            playOnce(Sound.FAILED);
        }
    }

    // Stop other audio, then play the win sound if it is enabled.
    void playCongratulations() {
        stopAll();
        if (AudioSettings.isCongratulationsSoundEnabled()) {
            playOnce(Sound.CONGRATULATIONS);
        }
    }

    void stopEndSounds() {
        stop(Sound.FAILED);
        stop(Sound.CONGRATULATIONS);
    }

    void stopAll() {
        gameMusicWanted = false;
        for (Sound sound : Sound.values()) {
            stop(sound);
        }
    }

    private void playOnce(Sound sound) {
        Clip clip = clips.get(sound);
        if (clip == null) {
            return;
        }
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    private void stop(Sound sound) {
        Clip clip = clips.get(sound);
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    // Load one sound file and report a useful message if it cannot be loaded.
    private Clip load(Sound sound) {
        try (AudioInputStream input = openStream(sound)) {
            if (input == null) {
                System.err.println("Sound file not found: " + SOUND_FOLDER + "/" + sound.fileName);
                return null;
            }
            Clip clip = AudioSystem.getClip();
            clip.open(input);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException
                 | IllegalArgumentException | SecurityException ex) {
            System.err.println("Could not load sound " + sound.fileName + ": " + ex.getMessage());
            return null;
        }
    }

    // Look for audio in the app resources first, then in the source folder.
    private AudioInputStream openStream(Sound sound) throws UnsupportedAudioFileException, IOException {
        URL resource = SoundManager.class.getResource("/" + SOUND_FOLDER + "/" + sound.fileName);
        if (resource != null) {
            return AudioSystem.getAudioInputStream(resource);
        }
        File file = new File("src/" + SOUND_FOLDER, sound.fileName);
        if (file.isFile()) {
            return AudioSystem.getAudioInputStream(file);
        }
        return null;
    }
}
