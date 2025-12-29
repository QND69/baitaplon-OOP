package com.example.farmSimulation.view.assets;

import com.example.farmSimulation.config.SettingsMenuConfig;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;

@Getter
@Setter

/**
 * Quản lý âm thanh nền (Background Music) của game
 */
public class AudioManager {
    private MediaPlayer mediaPlayer;
    private double currentVolume = SettingsMenuConfig.DEFAULT_MASTER_VOLUME; // Volume mặc định từ config
    
    /**
     * Phát nhạc nền với đường dẫn tài nguyên
     * @param resourcePath Đường dẫn tài nguyên (từ AssetPaths)
     */
    public void playMusic(String resourcePath) {
        try {
            // Lấy URL từ resource path
            URL musicUrl = getClass().getResource(resourcePath);
            if (musicUrl == null) {
                System.err.println("AudioManager: Không tìm thấy file nhạc tại: " + resourcePath);
                return;
            }
            
            // Tạo Media và MediaPlayer
            Media media = new Media(musicUrl.toString());
            mediaPlayer = new MediaPlayer(media);
            
            // Cấu hình MediaPlayer: lặp lại vô hạn
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            
            // Đặt volume hiện tại
            mediaPlayer.setVolume(currentVolume);
            
            // Phát nhạc
            mediaPlayer.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Tạm dừng nhạc nền
     */
    public void pauseMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }
    
    /**
     * Tiếp tục phát nhạc nền
     */
    public void resumeMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }
    
    /**
     * Đặt volume tổng thể (0.0 - 1.0)
     * @param volume Giá trị volume từ 0.0 (tắt) đến 1.0 (tối đa)
     */
    public void setGlobalVolume(double volume) {
        // Giới hạn volume trong khoảng 0.0 - 1.0
        this.currentVolume = Math.max(0.0, Math.min(1.0, volume));
        
        // Áp dụng volume vào MediaPlayer nếu đang phát
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(this.currentVolume);
        }
    }
    
    /**
     * Dừng và giải phóng tài nguyên MediaPlayer
     */
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }
}




