package p;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

// Builds the settings screen and connects its controls to AudioSettings.
public class SettingsMenuController extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String SETTINGS_FONT_FAMILY = "Monotype Corsiva";

    private final JSlider volumeSlider;
    private final JLabel volumeLabel;
    private final JCheckBox musicCheckBox;
    private final JCheckBox failedCheckBox;
    private final JCheckBox congratulationsCheckBox;
    private final JCheckBox muteCheckBox;

    // Create audio controls and keep the callback for returning to the menu.
    public SettingsMenuController(Runnable onBack) {
        setLayout(new GridBagLayout());
        setBackground(new Color(230, 214, 255));
        setPreferredSize(new Dimension(600, 400));

        JLabel title = new JLabel("Settings");
        title.setFont(settingsFont(Font.BOLD, 32));

        JLabel audioHeader = new JLabel("Audio");
        audioHeader.setFont(settingsFont(Font.BOLD, 22));

        volumeSlider = new JSlider(0, 100, (int) Math.round(AudioSettings.getVolume() * 100));
        volumeSlider.setPreferredSize(new Dimension(300, volumeSlider.getPreferredSize().height));
        volumeSlider.setOpaque(false);
        volumeSlider.setFont(settingsFont(Font.PLAIN, 18));
        volumeLabel = new JLabel();
        volumeLabel.setPreferredSize(new Dimension(60, 20));
        volumeLabel.setFont(settingsFont(Font.PLAIN, 18));

        JPanel volumeRow = new JPanel();
        volumeRow.setOpaque(false);
        JLabel volumeText = new JLabel("Volume");
        volumeText.setFont(settingsFont(Font.PLAIN, 18));
        volumeRow.add(volumeText);
        volumeRow.add(volumeSlider);
        volumeRow.add(volumeLabel);

        musicCheckBox = new JCheckBox("Music (Play the Game)", AudioSettings.isMusicEnabled());
        musicCheckBox.setOpaque(false);
        musicCheckBox.setFont(settingsFont(Font.PLAIN, 18));
        musicCheckBox.addActionListener(event -> AudioSettings.setMusicEnabled(musicCheckBox.isSelected()));

        failedCheckBox = new JCheckBox("Game over sound (Failed)", AudioSettings.isFailedSoundEnabled());
        failedCheckBox.setOpaque(false);
        failedCheckBox.setFont(settingsFont(Font.PLAIN, 18));
        failedCheckBox.addActionListener(event -> AudioSettings.setFailedSoundEnabled(failedCheckBox.isSelected()));

        congratulationsCheckBox = new JCheckBox("Win sound (CONGRATULATIONS)",
                AudioSettings.isCongratulationsSoundEnabled());
        congratulationsCheckBox.setOpaque(false);
        congratulationsCheckBox.setFont(settingsFont(Font.PLAIN, 18));
        congratulationsCheckBox.addActionListener(
                event -> AudioSettings.setCongratulationsSoundEnabled(congratulationsCheckBox.isSelected()));

        muteCheckBox = new JCheckBox("Mute", AudioSettings.isMuted());
        muteCheckBox.setOpaque(false);
        muteCheckBox.setFont(settingsFont(Font.PLAIN, 18));
        volumeSlider.setEnabled(!AudioSettings.isMuted());
        updateVolumeLabel();

        // Update the shared volume whenever the slider moves.
        volumeSlider.addChangeListener(event -> {
            AudioSettings.setVolume(volumeSlider.getValue() / 100.0);
            updateVolumeLabel();
        });

        // Muting also disables the slider until sound is turned back on.
        muteCheckBox.addActionListener(event -> {
            AudioSettings.setMuted(muteCheckBox.isSelected());
            volumeSlider.setEnabled(!muteCheckBox.isSelected());
            updateVolumeLabel();
        });

        JButton menuButton = new JButton("Back to Menu");
        menuButton.setFont(settingsFont(Font.PLAIN, 18));
        menuButton.addActionListener(event -> {
            if (onBack != null) {
                onBack.run();
            }
        });

        GridBagConstraints row = new GridBagConstraints();
        row.gridx = 0;
        row.insets = new Insets(10, 0, 10, 0);
        add(title, row);
        add(audioHeader, row);
        add(volumeRow, row);
        add(musicCheckBox, row);
        add(failedCheckBox, row);
        add(congratulationsCheckBox, row);
        add(muteCheckBox, row);
        add(menuButton, row);
    }

    // Use the same font family for all settings labels and controls.
    private Font settingsFont(int style, int size) {
        return new Font(SETTINGS_FONT_FAMILY, style, size);
    }

    // Keep the text beside the slider in sync with the current audio settings.
    private void updateVolumeLabel() {
        if (AudioSettings.isMuted()) {
            volumeLabel.setText("Muted");
        } else {
            volumeLabel.setText(Math.round(AudioSettings.getVolume() * 100) + "%");
        }
    }
}
