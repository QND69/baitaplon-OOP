package com.example.farmSimulation.config;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

// Chứa các hằng số cấu hình cho Menu Cài đặt
public class SettingsMenuConfig {
    // --- Cấu hình Settings Menu ---
    public static final double SETTINGS_MENU_SPACING = 15.0;
    public static final String SETTINGS_MENU_STYLE_CSS = "-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 30; -fx-border-radius: 10; -fx-background-radius: 10;";
    public static final double SETTINGS_MENU_WIDTH = 400.0;
    public static final double SETTINGS_MENU_HEIGHT = 500.0;
    public static final String SETTINGS_MENU_TITLE = "⚙ Game Menu";
    public static final Paint SETTINGS_MENU_FONT_COLOR = Color.WHITESMOKE;
    public static final String SETTINGS_MENU_FONT_FAMILY = "Arial";
    public static final double SETTINGS_MENU_TITLE_FONT_SIZE = 28.0;
    public static final double SETTINGS_PLAYER_INFO_SPACING = 5.0;
    public static final double SETTINGS_MENU_BODY_FONT_SIZE = 18.0;
    public static final double SETTINGS_MENU_BUTTON_WIDTH = 200.0;
    public static final String SETTINGS_RESUME_BUTTON_TEXT = "Resume";
    public static final String SETTINGS_SAVE_BUTTON_TEXT = "Save Game";
    public static final String SETTINGS_EXIT_BUTTON_TEXT = "Exit Game";
    public static final String SETTINGS_TEXT_ON = "ON";
    public static final String SETTINGS_TEXT_OFF = "OFF";
    public static final String SETTINGS_MUTE_BUTTON_TEXT_PREFIX = "Mute: ";
    public static final String SETTINGS_MASTER_VOLUME_LABEL = "Master Volume:";
    public static final double SLIDER_MIN_VALUE = 0.0;
    public static final double SLIDER_MAX_VALUE = 1.0;
    // Audio Settings (Đơn giản hóa: chỉ Master Volume + Mute)
    public static final boolean DEFAULT_MUTE = false; // Mặc định không tắt tiếng
    public static final double DEFAULT_MASTER_VOLUME = 0.5; // Âm lượng tổng mặc định (50%)

    // --- Cấu hình Brightness ---
    public static final double DEFAULT_BRIGHTNESS = GameLogicConfig.DEFAULT_BRIGHTNESS; // 100%
    public static final double BRIGHTNESS_MIN = GameLogicConfig.MIN_BRIGHTNESS; // 0%
    public static final double BRIGHTNESS_MAX = GameLogicConfig.MAX_BRIGHTNESS; // 100%
    public static final String BRIGHTNESS_LABEL = "Brightness:";

    // --- Cấu hình Layout ---
    public static final double SETTINGS_MENU_WIDTH_NEW = 500.0; // Tăng width cho GridPane
    public static final double SETTINGS_MENU_HEIGHT_NEW = 600.0; // Tăng height cho GridPane
    public static final String GAME_MENU_LABEL_TEXT = "Game Menu";
    public static final String PLAYER_LABEL_PREFIX = "Player: ";
    public static final String LEVEL_LABEL_PREFIX = "Level: ";
    public static final double SLIDER_WIDTH = 200.0;
    public static final double BUTTON_BOX_SPACING = 10.0;
    public static final double CONTENT_AREA_WIDTH_PADDING = 60.0;
    public static final double CONTENT_AREA_HEIGHT = 400.0;
    public static final double NAV_BAR_SPACING = 20.0;

    // --- Tab Names ---
    public static final String TAB_GENERAL = "General";
    public static final String TAB_CONTROLS = "Controls";
    public static final String TAB_TUTORIAL = "Tutorial";

    // --- Controls Strings ---
    public static final String CONTROLS_TITLE = "Key Bindings:";
    public static final String ACTION_MOVE_UP = "Move Up";
    public static final String ACTION_MOVE_LEFT = "Move Left";
    public static final String ACTION_MOVE_DOWN = "Move Down";
    public static final String ACTION_MOVE_RIGHT = "Move Right";
    public static final String ACTION_SHOP = "Open / Close Shop";
    public static final String ACTION_DROP = "Drop Item";
    public static final String ACTION_QUEST = "Open / Close Quest Board";
    public static final String ACTION_PAUSE = "Pause / Resume Game";
    public static final String ACTION_HOTBAR = "Select Hotbar Slot";
    public static final String ACTION_PRIMARY = "Use Tool / Interact";
    public static final String ACTION_SECONDARY = "Toggle Fence / Eat Food";
    public static final String KEY_NUMS = "1 - 9";
    public static final String KEY_MOUSE_LEFT = "Mouse Left";
    public static final String KEY_MOUSE_RIGHT = "Mouse Right";

    // --- CSS Styles ---
    public static final String NAV_BUTTON_ACTIVE_STYLE =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: white;" +
                    "-fx-font-weight: bold;" +
                    "-fx-border-color: transparent transparent white transparent;" +
                    "-fx-border-width: 0 0 2 0;" +
                    "-fx-padding: 5px;";

    public static final String NAV_BUTTON_INACTIVE_STYLE =
            "-fx-background-color: transparent;" +
                    "-fx-text-fill: #aaaaaa;" +
                    "-fx-font-weight: normal;" +
                    "-fx-border-width: 0;" +
                    "-fx-padding: 5px;";

    // Text cho nút Save/Load (Tránh hardcode)
    public static final String SAVE_BUTTON_TEXT_NEW = "SAVE GAME";
    public static final String LOAD_BUTTON_TEXT = "LOAD GAME";

    // Text cho màn hình chờ
    public static final String START_NEW_GAME_TEXT = "START NEW GAME";

    // Nội dung hướng dẫn chơi game chi tiết (Tutorial)
    public static final String TUTORIAL_TEXT =
            "WELCOME TO FARM SIMULATION!\n\n" +
                    "GOAL: Manage your farm, grow crops, raise animals, and complete daily quests.\n\n" +
                    "1. BASIC CONTROLS:\n" +
                    "- Move: W, A, S, D\n" +
                    "- Select Item: Keys 1-9 or Scroll Mouse\n" +
                    "- Primary Action (Use Tool/Place Item): Left Click\n" +
                    "- Secondary Action (Eat/Toggle Fence): Right Click\n" +
                    "- Drop Item: Q Key\n" +
                    "- Open/Close Shop: B Key\n" +
                    "- Open/Close Quest Board: J Key\n" +
                    "- Settings/Pause: ESC Key\n\n" +
                    "2. FARMING GUIDE:\n" +
                    "- Hoe: Use on grass to till soil.\n" +
                    "- Seeds: Hold seeds and Left Click on tilled soil.\n" +
                    "- Watering Can: Water crops daily. Refill at the river when empty.\n" +
                    "- Fertilizer: Use on growing crops to speed up growth.\n" +
                    "- Harvest: Use Hand (or Scythe) when crops are fully grown.\n" +
                    "- Shovel: Remove plants/crops to clear the soil.\n" +
                    "- Axe: Chop trees for wood (requires multiple hits).\n" +
                    "- Wood: Hold wood and Left Click to build fences.\n\n" +
                    "3. ANIMAL GUIDE:\n" +
                    "- Buy animals at the Shop (B Key) and place them on the ground.\n" +
                    "- Feed: Hold food and click on the animal.\n" +
                    "- Harvest Products:\n" +
                    "  + Cow: Use Milk Bucket to gather milk.\n" +
                    "  + Sheep: Use Shears to gather wool.\n" +
                    "  + Chicken: Lays eggs automatically, click to collect.\n" +
                    "- Slaughter: Use Axe or Sword (drops corresponding meat).\n\n" +
                    "TIP: Watch your Stamina bar. Eat food or drink Energy Drinks to recover!";

    private SettingsMenuConfig() {}
}