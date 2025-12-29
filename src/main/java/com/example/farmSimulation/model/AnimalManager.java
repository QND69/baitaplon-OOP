package com.example.farmSimulation.model;

import com.example.farmSimulation.config.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Class quản lý hệ thống động vật trong game.
 * Chịu trách nhiệm xử lý trí tuệ nhân tạo (AI), di chuyển, chỉ số đói,
 * tạo sản phẩm, sinh sản và quá trình lớn lên của động vật.
 */
public class AnimalManager {
    // -------------------------------------------------------------------------
    // 1. KHAI BÁO BIẾN VÀ CẤU HÌNH
    // -------------------------------------------------------------------------
    private final WorldMap worldMap;
    private final CollisionManager collisionManager;
    private final List<Animal> animals;
    private final Random random;

    // Thời gian cập nhật lần cuối cho các logic xử lý chậm (không chạy theo từng khung hình)
    private long lastLogicUpdateTimeMs = 0;

    // Thời gian cập nhật di chuyển lần cuối (tính bằng nano giây) để tính toán delta time
    private long lastMovementUpdateTime = 0;

    public AnimalManager(WorldMap worldMap, CollisionManager collisionManager) {
        this.worldMap = worldMap;
        this.collisionManager = collisionManager;
        this.animals = new ArrayList<>();
        this.random = new Random();
    }

    // -------------------------------------------------------------------------
    // 2. CÁC PHƯƠNG THỨC QUẢN LÝ LIST (THÊM, XÓA, TÌM KIẾM)
    // -------------------------------------------------------------------------

    /**
     * Thêm động vật mới vào danh sách quản lý.
     */
    public void addAnimal(Animal animal) {
        if (animal != null) {
            animals.add(animal);
        }
    }

    /**
     * Xóa động vật khỏi danh sách.
     */
    public void removeAnimal(Animal animal) {
        animals.remove(animal);
    }

    /**
     * Lấy danh sách tất cả động vật hiện có.
     * Trả về một bản sao để tránh lỗi thay đổi dữ liệu đồng thời (concurrent modification).
     */
    public List<Animal> getAnimals() {
        return new ArrayList<>(animals);
    }

    /**
     * Tìm động vật tại một vị trí cụ thể trong bán kính cho trước.
     * Dùng để xử lý tương tác của người chơi (ví dụ: click chuột vào con vật).
     */
    public Animal getAnimalAt(double worldX, double worldY, double range) {
        for (Animal animal : animals) {
            if (animal.isDead()) continue;

            double dx = animal.getX() - worldX;
            double dy = animal.getY() - worldY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance <= range) {
                return animal;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // 3. VÒNG LẶP CHÍNH (UPDATE LOOP)
    // -------------------------------------------------------------------------

    /**
     * Hàm cập nhật chính cho toàn bộ hệ thống động vật.
     * Xử lý logic theo hai nhóm:
     * 1. Logic nặng (Logic Loop): Đói, lớn, sinh sản... chạy ngắt quãng để tối ưu hiệu năng.
     * 2. Logic di chuyển (Movement Loop): Chạy liên tục mỗi khung hình để chuyển động mượt mà.
     */
    public boolean updateAnimals(long currentTime, Player player) {
        boolean needsRedraw = false;
        long currentTimeMs = currentTime / 1_000_000;

        // Kiểm tra xem đã đến lúc chạy các logic nặng chưa
        boolean shouldUpdateLogic = (currentTimeMs - lastLogicUpdateTimeMs >= AnimalConfig.ANIMAL_UPDATE_INTERVAL_MS);

        if (shouldUpdateLogic) {
            lastLogicUpdateTimeMs = currentTimeMs;
        }

        // Tính toán khoảng thời gian giữa 2 frame (deltaTime) để di chuyển mượt mà
        // Cap deltaTime tối đa 0.1s để tránh hiện tượng "teleport" khi game bị giật/lag
        double deltaTime = 0.0;
        if (lastMovementUpdateTime > 0) {
            long deltaNanos = currentTime - lastMovementUpdateTime;
            deltaTime = deltaNanos / 1_000_000_000.0;
            if (deltaTime > 0.1) deltaTime = 0.1;
        }
        lastMovementUpdateTime = currentTime;

        long now = System.currentTimeMillis();

        // Danh sách tạm chứa các động vật mới sinh ra (trứng, con non)
        // để thêm vào danh sách chính sau khi vòng lặp kết thúc
        List<Animal> newAnimals = new ArrayList<>();

        Iterator<Animal> iterator = animals.iterator();
        while (iterator.hasNext()) {
            Animal animal = iterator.next();

            // Loại bỏ động vật đã chết khỏi danh sách
            if (animal.isDead()) {
                iterator.remove();
                needsRedraw = true;
                continue;
            }

            // --- NHÓM 1: CẬP NHẬT TRẠNG THÁI LOGIC (Chạy ngắt quãng) ---
            if (shouldUpdateLogic) {
                // 1. Cập nhật chỉ số đói
                updateHunger(animal, currentTime);

                // 2. Kiểm tra xem có bị chết đói không
                if (checkStarvation(animal, currentTime)) {
                    // Xử lý rơi thịt và đánh dấu đã chết
                    handleStarvationDrop(animal);
                    animal.setDead(true);
                    needsRedraw = true;
                    continue;
                }

                // 3. Xử lý sự sinh trưởng (Trứng -> Con non -> Trưởng thành)
                if (updateGrowth(animal, currentTime)) {
                    needsRedraw = true;
                }

                // 4. Xử lý sinh sản
                // Chỉ áp dụng cho thú trưởng thành, no bụng và không phải là Gà (Gà đẻ trứng riêng)
                if (!animal.isBaby() && animal.getType() != AnimalType.CHICKEN && animal.getHunger() >= AnimalConfig.MIN_HUNGER_FOR_BREEDING) {
                    if (updateBreeding(animal, currentTime, newAnimals)) {
                        needsRedraw = true;
                    }
                } else {
                    // Nếu điều kiện không thỏa mãn (đói/chưa lớn), hủy bỏ trạng thái ghép đôi
                    animal.setBreedingPartner(null);
                    animal.setMatingStartTime(0);
                }

                // 5. Xử lý tạo sản phẩm (Sữa, Len, Trứng)
                if (updateProduction(animal, currentTime, newAnimals)) {
                    needsRedraw = true;
                }

                // 6. Cập nhật tuổi thọ tính theo giây
                animal.setAge((int) animal.getAgeInSeconds());
            }

            // --- NHÓM 2: DI CHUYỂN & TRÍ TUỆ NHÂN TẠO (Chạy mỗi khung hình) ---
            // Cần chạy liên tục để đảm bảo vị trí được cập nhật mượt mà và timer AI chính xác
            if (updateMovement(animal, now, deltaTime, player)) {
                needsRedraw = true;
            }
        }

        // Thêm các động vật mới sinh vào danh sách quản lý chính
        if (!newAnimals.isEmpty()) {
            animals.addAll(newAnimals);
            needsRedraw = true;
        }

        return needsRedraw;
    }

    // -------------------------------------------------------------------------
    // 4. CÁC PHƯƠNG THỨC XỬ LÝ LOGIC TRẠNG THÁI (PRIVATE HELPERS)
    // -------------------------------------------------------------------------

    /**
     * Cập nhật mức độ đói của động vật dựa trên thời gian trôi qua.
     */
    private void updateHunger(Animal animal, long currentTime) {
        long deltaTimeMs = (currentTime - animal.getLastHungerUpdateTime()) / 1_000_000;
        if (deltaTimeMs > 0) {
            // Giảm độ no theo thời gian
            double hungerDecrease = (deltaTimeMs / 1000.0) * AnimalConfig.HUNGER_DECREASE_RATE;
            double newHunger = Math.max(0, animal.getHunger() - hungerDecrease);
            animal.setHunger(newHunger);
            animal.setLastHungerUpdateTime(currentTime);

            // Bắt đầu đếm giờ chết đói nếu chỉ số đói về 0
            if (animal.isHungry() && animal.getStarvationStartTime() == 0) {
                animal.setStarvationStartTime(currentTime);
            } else if (!animal.isHungry()) {
                animal.setStarvationStartTime(0); // Reset nếu đã được cho ăn
            }
        }
    }

    /**
     * Kiểm tra xem động vật đã bị đói quá lâu chưa. Nếu quá thời gian cho phép sẽ chết.
     */
    private boolean checkStarvation(Animal animal, long currentTime) {
        if (animal.getStarvationStartTime() > 0) {
            long starvationDuration = (currentTime - animal.getStarvationStartTime()) / 1_000_000;
            if (starvationDuration >= AnimalConfig.STARVATION_TIME_MS) {
                return true; // Đã chết đói
            }
        }
        return false;
    }

    /**
     * Xử lý logic rơi thịt khi động vật chết đói.
     * Tìm vị trí thích hợp trên bản đồ để đặt item thịt rơi ra.
     */
    private void handleStarvationDrop(Animal animal) {
        int meatAmount = animal.calculateMeatDrop();
        if (meatAmount > 0 && worldMap != null) {
            ItemType meatType = animal.getMeatType();
            if (meatType != null) {
                // Tính toán vị trí hiển thị trung tâm của động vật để item rơi ra từ chính giữa
                double visualCenterY = animal.getY() - (animal.getType().getSpriteSize() / 2.0);

                int idealTileCol = (int) Math.floor(animal.getX() / WorldConfig.TILE_SIZE);
                int idealTileRow = (int) Math.floor(visualCenterY / WorldConfig.TILE_SIZE);

                // Tính toán độ lệch (offset) để vẽ item đúng vị trí
                double targetItemX = animal.getX() - (ItemSpriteConfig.ITEM_SPRITE_WIDTH / 2.0);
                double targetItemY = visualCenterY - (ItemSpriteConfig.ITEM_SPRITE_HEIGHT / 2.0);

                double originalOffsetX = targetItemX - (idealTileCol * WorldConfig.TILE_SIZE);
                double originalOffsetY = targetItemY - (idealTileRow * WorldConfig.TILE_SIZE);

                // Tìm ô đất trống xung quanh để đặt thịt, tránh ghi đè lên vật phẩm khác
                int searchRadius = GameLogicConfig.ITEM_DROP_SEARCH_RADIUS;
                int finalCol = -1;
                int finalRow = -1;
                boolean foundSpot = false;

                // Ưu tiên 1: Kiểm tra ô đất ngay tại vị trí động vật đứng
                TileData idealTile = worldMap.getTileData(idealTileCol, idealTileRow);
                if (idealTile.getGroundItem() == null) {
                    finalCol = idealTileCol;
                    finalRow = idealTileRow;
                    foundSpot = true;
                } else if (idealTile.getGroundItem() == meatType) {
                    finalCol = idealTileCol;
                    finalRow = idealTileRow;
                    foundSpot = true;
                } else {
                    // Ưu tiên 2: Tìm các ô xung quanh
                    for (int r = idealTileRow - searchRadius; r <= idealTileRow + searchRadius; r++) {
                        for (int c = idealTileCol - searchRadius; c <= idealTileCol + searchRadius; c++) {
                            if (r == idealTileRow && c == idealTileCol) continue;
                            TileData checkTile = worldMap.getTileData(c, r);
                            if (checkTile.getGroundItem() == null) {
                                finalCol = c;
                                finalRow = r;
                                foundSpot = true;
                                break;
                            }
                        }
                        if (foundSpot) break;
                    }
                }

                // Nếu không tìm thấy chỗ trống, buộc phải đặt tại chỗ cũ (fallback)
                if (!foundSpot) {
                    finalCol = idealTileCol;
                    finalRow = idealTileRow;
                }

                // Cập nhật dữ liệu vào bản đồ
                TileData finalTile = worldMap.getTileData(finalCol, finalRow);

                if (finalTile.getGroundItem() == meatType) {
                    // Cộng dồn nếu đã có cùng loại thịt
                    finalTile.setGroundItemAmount(finalTile.getGroundItemAmount() + meatAmount);
                } else {
                    // Đặt mới
                    finalTile.setGroundItem(meatType);
                    finalTile.setGroundItemAmount(meatAmount);

                    if (finalCol == idealTileCol && finalRow == idealTileRow) {
                        finalTile.setGroundItemOffsetX(originalOffsetX);
                        finalTile.setGroundItemOffsetY(originalOffsetY);
                    } else {
                        // Nếu rơi sang ô bên cạnh, tạo một chút lệch ngẫu nhiên cho tự nhiên
                        finalTile.setDefaultItemOffset();
                        double scatter = GameLogicConfig.ITEM_DROP_SCATTER_RANGE;
                        double jitterX = (random.nextDouble() - 0.5) * scatter;
                        double jitterY = (random.nextDouble() - 0.5) * scatter;
                        finalTile.setGroundItemOffsetX(finalTile.getGroundItemOffsetX() + jitterX);
                        finalTile.setGroundItemOffsetY(finalTile.getGroundItemOffsetY() + jitterY);
                    }
                }

                worldMap.setTileData(finalCol, finalRow, finalTile);
            }
        }
    }

    /**
     * Cập nhật quá trình lớn lên của động vật.
     * Ví dụ: Trứng nở thành gà con, gà con lớn thành gà trưởng thành.
     */
    private boolean updateGrowth(Animal animal, long currentTime) {
        if (!animal.canGrow()) {
            return false;
        }

        long ageInSeconds = animal.getAgeInSeconds();
        long growthTimeSeconds = animal.getType().getGrowthTimeMs() / 1000;

        if (ageInSeconds >= growthTimeSeconds) {
            // Đã đủ tuổi trưởng thành, tiến hành biến đổi loại (Type)
            if (animal.getType() == AnimalType.EGG_ENTITY) {
                animal.setType(AnimalType.BABY_CHICKEN);
                animal.setSpawnTime(currentTime); // Reset thời gian sinh
                return true;
            } else if (animal.getType() == AnimalType.BABY_CHICKEN) {
                animal.setType(AnimalType.CHICKEN);
                animal.setSpawnTime(currentTime);
                return true;
            } else if (animal.getType() == AnimalType.BABY_COW) {
                animal.setType(AnimalType.COW);
                animal.setSpawnTime(currentTime);
                return true;
            } else if (animal.getType() == AnimalType.BABY_PIG) {
                animal.setType(AnimalType.PIG);
                animal.setSpawnTime(currentTime);
                return true;
            } else if (animal.getType() == AnimalType.BABY_SHEEP) {
                animal.setType(AnimalType.SHEEP);
                animal.setSpawnTime(currentTime);
                return true;
            }
        }

        return false;
    }

    /**
     * Cập nhật logic tạo sản phẩm (Sữa, Len, Trứng).
     */
    private boolean updateProduction(Animal animal, long currentTime, List<Animal> newAnimals) {
        if (!animal.canProduce() || animal.isHasProduct()) {
            return false; // Không thể sản xuất hoặc chưa thu hoạch sản phẩm cũ
        }

        // Nếu đói thì thời gian sản xuất sẽ lâu hơn
        long productionTime = animal.isHungry() ?
                AnimalConfig.HUNGRY_PRODUCTION_TIME_MS :
                animal.getType().getProductionTimeMs();

        // Khởi tạo bộ đếm thời gian nếu chưa có
        if (animal.getProductionTimer() == 0) {
            animal.setProductionTimer(currentTime);
        }

        // Kiểm tra xem đã đủ thời gian chưa
        long elapsed = (currentTime - animal.getProductionTimer()) / 1_000_000;
        if (elapsed >= productionTime) {
            // Đối với Gà: Đẻ trứng xuống đất (tạo một thực thể trứng mới)
            if (animal.getType() == AnimalType.CHICKEN) {
                animal.setProductionTimer(0); // Reset timer ngay

                Animal egg = new Animal(AnimalType.EGG_ENTITY, animal.getX(), animal.getY());
                newAnimals.add(egg);

                return true;
            } else {
                // Các con vật khác: Hiện icon sản phẩm trên đầu để người chơi thu hoạch
                animal.setHasProduct(true);
                animal.setProductionTimer(0); // Reset timer
                return true;
            }
        }

        return false;
    }

    /**
     * Cập nhật logic sinh sản (Breeding).
     * Bao gồm các bước: Tìm bạn tình -> Di chuyển lại gần -> Hoạt cảnh sinh sản -> Sinh con.
     */
    private boolean updateBreeding(Animal animal, long currentTime, List<Animal> newAnimals) {
        // Kiểm tra thời gian hồi chiêu (Cooldown) sau lần sinh trước
        long timeSinceBreed = (currentTime - animal.getBreedingCooldownTimer()) / 1_000_000;
        if (animal.getBreedingCooldownTimer() > 0 && timeSinceBreed < AnimalConfig.BREEDING_COOLDOWN_MS) {
            return false;
        }

        // --- TRƯỜNG HỢP 1: ĐÃ CÓ ĐỐI TƯỢNG VÀ ĐANG THỰC HIỆN HOẠT CẢNH ---
        if (animal.getMatingStartTime() > 0 && animal.getBreedingPartner() != null) {
            long matingDuration = System.currentTimeMillis() - animal.getMatingStartTime();

            // Nếu đã hết thời gian hoạt cảnh -> Sinh con
            if (matingDuration >= AnimalConfig.BREEDING_ANIMATION_DURATION_MS) {
                AnimalType babyType = animal.getType().getBabyType();
                if (babyType != null) {
                    double midX = (animal.getX() + animal.getBreedingPartner().getX()) / 2.0;
                    double midY = (animal.getY() + animal.getBreedingPartner().getY()) / 2.0;

                    // Tạo con non tại vị trí giữa bố và mẹ
                    Animal baby = new Animal(babyType, midX, midY);
                    newAnimals.add(baby);

                    // Reset trạng thái cho cả bố và mẹ sau khi sinh
                    Animal partner = animal.getBreedingPartner();
                    applyPostBreedingState(animal, currentTime);
                    applyPostBreedingState(partner, currentTime);

                    return true;
                }
            }
            return false; // Đang trong quá trình chờ sinh
        }

        // --- TRƯỜNG HỢP 2: CHƯA CÓ ĐỐI TƯỢNG, BẮT ĐẦU TÌM KIẾM ---
        if (animal.getBreedingPartner() == null) {
            // Quét danh sách động vật để tìm bạn tình phù hợp
            for (Animal partner : animals) {
                if (partner == animal) continue; // Không tự sinh sản
                if (partner.isDead()) continue;

                // Điều kiện: Cùng loại, trưởng thành, không đói, và đang độc thân
                if (partner.getType() == animal.getType() &&
                        !partner.isBaby() &&
                        !partner.isHungry() &&
                        partner.getBreedingPartner() == null) {

                    // Đối phương cũng phải hết thời gian hồi chiêu
                    long partnerTimeSince = (currentTime - partner.getBreedingCooldownTimer()) / 1_000_000;
                    if (partner.getBreedingCooldownTimer() > 0 && partnerTimeSince < AnimalConfig.BREEDING_COOLDOWN_MS) {
                        continue;
                    }

                    // Kiểm tra khoảng cách
                    double distSq = Math.pow(animal.getX() - partner.getX(), 2) + Math.pow(animal.getY() - partner.getY(), 2);

                    // Nếu trong phạm vi phát hiện -> Thiết lập quan hệ ghép đôi
                    if (distSq <= AnimalConfig.BREEDING_DETECTION_RANGE * AnimalConfig.BREEDING_DETECTION_RANGE) {
                        animal.setBreedingPartner(partner);
                        partner.setBreedingPartner(animal);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Hàm hỗ trợ để đặt lại các chỉ số sau khi sinh sản thành công.
     */
    private void applyPostBreedingState(Animal animal, long currentTime) {
        animal.setBreedingCooldownTimer(currentTime); // Bắt đầu tính thời gian hồi chiêu

        // Trừ đi một lượng đói (chi phí sinh sản)
        double cost = AnimalConfig.BREEDING_HUNGER_COST;
        animal.setHunger(Math.max(0, animal.getHunger() - cost));
        animal.setStarvationStartTime(0);

        animal.setBreedingPartner(null); // Xóa liên kết bạn tình
        animal.setMatingStartTime(0); // Xóa timer hoạt cảnh
        animal.setCurrentAction(Animal.Action.IDLE); // Về trạng thái đứng yên
    }

    // -------------------------------------------------------------------------
    // 5. LOGIC DI CHUYỂN VÀ AI (PRIVATE HELPERS)
    // -------------------------------------------------------------------------

    /**
     * Cập nhật di chuyển của động vật.
     * Sử dụng hệ thống ưu tiên (Priority System) để quyết định hành động:
     * 1. Ưu tiên cao nhất: Dàn hàng để sinh sản (khi đã đứng cạnh nhau).
     * 2. Ưu tiên nhì: Đi tìm bạn tình (nếu đã xác định được đối tượng).
     * 3. Ưu tiên ba: Đi theo người chơi (nếu người chơi cầm thức ăn).
     * 4. Ưu tiên thấp nhất: Đi lang thang ngẫu nhiên.
     */
    private boolean updateMovement(Animal animal, long nowMs, double deltaTime, Player player) {
        // Trứng không di chuyển
        if (animal.getType() == AnimalType.EGG_ENTITY) {
            return false;
        }

        // Nếu delta time bằng 0 (khung hình đầu tiên), không xử lý
        if (deltaTime <= 0) {
            return false;
        }

        // --- ƯU TIÊN 1: LOGIC SẮP XẾP VỊ TRÍ KHI SINH SẢN ---
        // Khi hai con vật đã ở cạnh nhau, chúng cần di chuyển nhẹ để đối mặt nhau
        if (animal.getMatingStartTime() > 0) {
            Animal partner = animal.getBreedingPartner();
            if (partner != null && !partner.isDead()) {
                double dx = Math.abs(animal.getX() - partner.getX());
                double dy = Math.abs(animal.getY() - partner.getY());

                double targetX, targetY;
                int newDir;

                // Xác định cách dàn hàng: Ngang hay Dọc tùy theo khoảng cách nào lớn hơn
                if (dx > dy) {
                    // Dàn hàng NGANG: Y bằng nhau, X cách nhau một khoảng
                    targetY = (animal.getY() + partner.getY()) / 2.0;

                    double midX = (animal.getX() + partner.getX()) / 2.0;
                    boolean amILeft = animal.getX() <= partner.getX();

                    // Xử lý trường hợp tọa độ trùng nhau
                    if (Math.abs(animal.getX() - partner.getX()) < 1.0) {
                        amILeft = animal.hashCode() < partner.hashCode();
                    }

                    targetX = midX + (amILeft ? -AnimalConfig.BREEDING_ALIGNMENT_OFFSET : AnimalConfig.BREEDING_ALIGNMENT_OFFSET);
                    newDir = amILeft ? 1 : 2; // Quay mặt vào nhau

                } else {
                    // Dàn hàng DỌC: X bằng nhau, Y cách nhau một khoảng
                    targetX = (animal.getX() + partner.getX()) / 2.0;

                    double midY = (animal.getY() + partner.getY()) / 2.0;
                    boolean amIUp = animal.getY() <= partner.getY();

                    if (Math.abs(animal.getY() - partner.getY()) < 1.0) {
                        amIUp = animal.hashCode() < partner.hashCode();
                    }

                    targetY = midY + (amIUp ? -AnimalConfig.BREEDING_ALIGNMENT_OFFSET : AnimalConfig.BREEDING_ALIGNMENT_OFFSET);
                    newDir = amIUp ? 0 : 3; // Quay mặt vào nhau
                }

                // Di chuyển mượt mà về vị trí mục tiêu (Linear Interpolation - Lerp)
                double lerpSpeed = AnimalConfig.BREEDING_ALIGNMENT_SPEED;
                double newX = animal.getX() + (targetX - animal.getX()) * lerpSpeed;
                double newY = animal.getY() + (targetY - animal.getY()) * lerpSpeed;

                animal.setX(newX);
                animal.setY(newY);
                animal.setDirection(newDir);
            }

            // Trong lúc này luôn giữ trạng thái IDLE để hiển thị icon trái tim nếu có
            animal.setCurrentAction(Animal.Action.IDLE);
            return true;
        }

        // --- ƯU TIÊN 2: DI CHUYỂN ĐẾN BẠN TÌNH ---
        if (animal.getBreedingPartner() != null) {
            Animal partner = animal.getBreedingPartner();

            // Nếu đối tác không còn hợp lệ, hủy bỏ
            if (partner.isDead() || partner.isHungry()) {
                animal.setBreedingPartner(null);
            } else {
                double dx = partner.getX() - animal.getX();
                double dy = partner.getY() - animal.getY();
                double distSq = dx*dx + dy*dy;

                // Nếu đã đến đủ gần -> Bắt đầu timer sinh sản
                if (distSq <= AnimalConfig.BREEDING_RANGE * AnimalConfig.BREEDING_RANGE) {
                    if (animal.getMatingStartTime() == 0) {
                        animal.setMatingStartTime(System.currentTimeMillis());
                    }
                    animal.setCurrentAction(Animal.Action.IDLE);
                    return true;
                }

                // Nếu chưa đến -> Tiếp tục đi tới
                animal.setCurrentAction(Animal.Action.WALK);
                animal.setActionEndTime(nowMs + 100); // Gia hạn timer hành động

                // Xác định hướng di chuyển
                if (Math.abs(dx) > Math.abs(dy)) {
                    animal.setDirection(dx > 0 ? 1 : 2);
                } else {
                    animal.setDirection(dy > 0 ? 0 : 3);
                }

                // Tính toán vị trí tiếp theo
                double speed = animal.getType().getMovementSpeed();
                double moveDist = speed * deltaTime;
                double dist = Math.sqrt(distSq);
                double moveX = (dx / dist) * moveDist;
                double moveY = (dy / dist) * moveDist;

                double nextX = animal.getX() + moveX;
                double nextY = animal.getY() + moveY;

                if (canAnimalMoveTo(animal, nextX, nextY)) {
                    animal.setX(nextX);
                    animal.setY(nextY);
                    return true;
                }
            }
        }

        // --- ƯU TIÊN 3: ĐI THEO NGƯỜI CHƠI (NẾU CẦM THỨC ĂN) ---
        boolean isAttracted = false;
        if (player != null && player.getCurrentItem() != null) {
            ItemType heldItem = player.getCurrentItem().getItemType();
            if (animal.getType().acceptsFood(heldItem)) {
                // Lấy tọa độ thực tế của người chơi
                double pX = player.getTileX() + (PlayerSpriteConfig.BASE_PLAYER_FRAME_WIDTH / 2.0);
                double pY = player.getTileY() + PlayerSpriteConfig.BASE_PLAYER_FRAME_HEIGHT;

                double dx = pX - animal.getX();
                double dy = pY - animal.getY();
                double distSq = dx*dx + dy*dy;

                // Nếu trong tầm nhìn của động vật
                if (distSq < AnimalConfig.PLAYER_FOLLOW_DETECTION_RANGE * AnimalConfig.PLAYER_FOLLOW_DETECTION_RANGE) {
                    // Nếu chưa đến quá gần (khoảng cách an toàn)
                    if (distSq > AnimalConfig.PLAYER_FOLLOW_STOP_DISTANCE * AnimalConfig.PLAYER_FOLLOW_STOP_DISTANCE) {
                        isAttracted = true;

                        animal.setCurrentAction(Animal.Action.WALK);
                        animal.setActionEndTime(nowMs + 100);

                        // Hướng mặt về phía người chơi
                        if (Math.abs(dx) > Math.abs(dy)) {
                            animal.setDirection(dx > 0 ? 1 : 2);
                        } else {
                            animal.setDirection(dy > 0 ? 0 : 3);
                        }

                        // Di chuyển trực tiếp về phía người chơi
                        double speed = animal.getType().getMovementSpeed();
                        double moveDist = speed * deltaTime;

                        double dist = Math.sqrt(distSq);
                        double moveX = (dx / dist) * moveDist;
                        double moveY = (dy / dist) * moveDist;

                        double nextX = animal.getX() + moveX;
                        double nextY = animal.getY() + moveY;

                        if (canAnimalMoveTo(animal, nextX, nextY)) {
                            animal.setX(nextX);
                            animal.setY(nextY);
                            return true;
                        }
                    } else {
                        // Nếu đã đến gần -> Đứng lại nhìn
                        animal.setCurrentAction(Animal.Action.IDLE);
                        if (Math.abs(dx) > Math.abs(dy)) {
                            animal.setDirection(dx > 0 ? 1 : 2);
                        } else {
                            animal.setDirection(dy > 0 ? 0 : 3);
                        }
                        return true;
                    }
                }
            }
        }

        if (isAttracted) return true; // Nếu đang bị thu hút thì bỏ qua logic đi lang thang

        // --- ƯU TIÊN 4: ĐI LANG THANG NGẪU NHIÊN ---
        // Thiết lập thông số ngẫu nhiên dựa trên loại động vật (Gà ít đi hơn thú khác)
        boolean isChickenType = (animal.getType() == AnimalType.CHICKEN || animal.getType() == AnimalType.BABY_CHICKEN);

        double walkChance;
        int minIdle, maxIdle;
        int minWalk, maxWalk;

        if (isChickenType) {
            walkChance = AnimalConfig.CHICKEN_WALK_CHANCE;
            minIdle = AnimalConfig.CHICKEN_MIN_IDLE_TIME_MS;
            maxIdle = AnimalConfig.CHICKEN_MAX_IDLE_TIME_MS;
            minWalk = AnimalConfig.CHICKEN_MIN_WALK_TIME_MS;
            maxWalk = AnimalConfig.CHICKEN_MAX_WALK_TIME_MS;
        } else {
            walkChance = AnimalConfig.STANDARD_WALK_CHANCE;
            minIdle = AnimalConfig.STANDARD_MIN_IDLE_TIME_MS;
            maxIdle = AnimalConfig.STANDARD_MAX_IDLE_TIME_MS;
            minWalk = AnimalConfig.STANDARD_MIN_WALK_TIME_MS;
            maxWalk = AnimalConfig.STANDARD_MAX_WALK_TIME_MS;
        }

        // 1. Quyết định hành động mới nếu hành động cũ đã hết thời gian
        if (nowMs >= animal.getActionEndTime()) {
            double roll = random.nextDouble();

            if (roll < walkChance) {
                // Chuyển sang đi bộ
                animal.setCurrentAction(Animal.Action.WALK);
                animal.setDirection(random.nextInt(4)); // Hướng ngẫu nhiên
                long duration = minWalk + (long)(random.nextDouble() * (maxWalk - minWalk));
                animal.setActionEndTime(nowMs + duration);
            } else {
                // Chuyển sang đứng yên
                animal.setCurrentAction(Animal.Action.IDLE);
                long duration = minIdle + (long)(random.nextDouble() * (maxIdle - minIdle));
                animal.setActionEndTime(nowMs + duration);
            }
        }

        // 2. Thực thi hành động di chuyển
        if (animal.getCurrentAction() == Animal.Action.WALK) {
            double speedPerSecond = animal.getType().getMovementSpeed();
            double movementDistance = speedPerSecond * deltaTime;
            double newX = animal.getX();
            double newY = animal.getY();

            switch (animal.getDirection()) {
                case 0: newY += movementDistance; break; // Xuống
                case 1: newX += movementDistance; break; // Phải
                case 2: newX -= movementDistance; break; // Trái
                case 3: newY -= movementDistance; break; // Lên
            }

            // Kiểm tra va chạm trước khi cập nhật vị trí
            if (canAnimalMoveTo(animal, newX, newY)) {
                animal.setX(newX);
                animal.setY(newY);
                return true;
            } else {
                // Nếu bị kẹt, dừng lại và chuyển sang đứng yên ngay lập tức để chờ quyết định hướng mới
                animal.setCurrentAction(Animal.Action.IDLE);
                animal.setActionEndTime(nowMs);
            }
        }

        return false;
    }

    /**
     * Kiểm tra xem động vật có thể di chuyển đến vị trí (newX, newY) hay không.
     * Dựa trên hệ thống va chạm (Collision System) của game.
     */
    private boolean canAnimalMoveTo(Animal animal, double newX, double newY) {
        return !collisionManager.checkCollision(newX, newY,
                animal.getType().getHitboxWidth(),
                animal.getType().getHitboxHeight());
    }


    // -------------------------------------------------------------------------
    // 6. CÁC PHƯƠNG THỨC TƯƠNG TÁC (PUBLIC INTERFACE)
    // -------------------------------------------------------------------------

    /**
     * Cho động vật ăn, giúp hồi phục chỉ số đói.
     */
    public void feedAnimal(Animal animal) {
        if (animal != null && !animal.isDead()) {
            double current = animal.getHunger();
            double recover = AnimalConfig.HUNGER_RECOVER_PER_FEED;
            // Cộng thêm lượng hồi phục nhưng không vượt quá giới hạn
            animal.setHunger(Math.min(AnimalConfig.MAX_HUNGER, current + recover));

            animal.setStarvationStartTime(0); // Xóa trạng thái chết đói
        }
    }

    /**
     * Thu hoạch sản phẩm từ động vật (Sữa, Len).
     * Sau khi thu hoạch sẽ reset lại thời gian sản xuất.
     */
    public void harvestProduct(Animal animal) {
        if (animal != null && animal.isHasProduct()) {
            animal.setHasProduct(false);
            animal.setProductionTimer(System.nanoTime()); // Bắt đầu chu kỳ sản xuất mới
        }
    }

    /**
     * Xử lý khi động vật bị giết (bởi người chơi).
     * Trả về số lượng thịt thu được.
     */
    public int killAnimal(Animal animal) {
        if (animal != null && !animal.isDead()) {
            animal.setDead(true);
            return animal.calculateMeatDrop();
        }
        return 0;
    }
}